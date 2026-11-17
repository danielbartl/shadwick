package dev.jbaby.shadwick.examples;

import com.deque.html.axecore.playwright.AxeBuilder;
import com.microsoft.playwright.*;
import com.microsoft.playwright.options.*;
import dev.jbaby.shadwick.Skin;
import org.apache.wicket.protocol.http.WebApplication;
import org.junit.jupiter.api.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;

import java.nio.file.*;
import java.util.*;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import static com.microsoft.playwright.assertions.PlaywrightAssertions.assertThat;
import static org.assertj.core.api.Assertions.assertThat;

/** The calendar heatmaps on /charts: one cell per day, keyboard by day and week, days without data, Ajax, every skin. */
@Tag("browser")
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class CalendarHeatmapBrowserTest {
    static Playwright playwright;
    static Browser chromium, firefox;
    @LocalServerPort int port;
    @Autowired WebApplication app;

    @BeforeAll static void start() throws Exception {
        Files.createDirectories(Path.of("target/calendar-heatmap-screenshots"));
        playwright = Playwright.create();
        chromium = playwright.chromium().launch();
        firefox = playwright.firefox().launch();
    }
    @AfterAll static void stop() { playwright.close(); }
    @BeforeEach void markup() { app.getMarkupSettings().setStripWicketTags(true); }
    @AfterEach void restore() { app.getMarkupSettings().setStripWicketTags(false); }

    static Stream<Arguments> skins() {
        List<String> selected = Arrays.asList(System.getProperty("shadwick.browser.skins", "vega,lyra,nova,maia,mira,luma,rhea,sera,sketch").split(","));
        return Stream.of("chromium", "firefox").flatMap(engine -> Arrays.stream(Skin.values())
                .map(skin -> skin.name().toLowerCase(Locale.ROOT)).filter(selected::contains)
                .flatMap(skin -> Stream.of(Arguments.of(engine, skin, "light", 1440), Arguments.of(engine, skin, "dark", 320))));
    }

    Browser browser(String engine) { return engine.equals("chromium") ? chromium : firefox; }
    String url(String query) { return "http://localhost:" + port + "/charts?" + query; }
    Locator calendar(Page p, String title) {
        return p.locator(".sw-calendar-heatmap").filter(new Locator.FilterOptions().setHas(p.locator("figcaption", new Page.LocatorOptions().setHasText(title))));
    }
    List<String> errors(Page p) {
        var result = new ArrayList<String>();
        p.onPageError(result::add);
        p.onConsoleMessage(m -> { if (m.type().equals("error")) result.add(m.text()); });
        return result;
    }

    @ParameterizedTest @MethodSource("skins")
    void daysAreExploredByWeekAndDayAndTheGridScrollsOnAPhone(String engine, String skin, String mode, int width) {
        try (var context = browser(engine).newContext(new Browser.NewContextOptions().setViewportSize(width, 1000)); var p = context.newPage()) {
            var errors = errors(p);
            p.navigate(url("skin=" + skin + "&mode=" + mode + "&lang=en-US"));
            Locator year = calendar(p, "A year of contributions"), plot = year.locator(".sw-heatmap-plot");
            year.scrollIntoViewIfNeeded();
            assertThat(plot).hasAttribute("data-heatmap-enhanced", "true");
            assertThat(year.locator("td.sw-heatmap-cell:not([data-outside])")).hasCount(365);
            // The page never scrolls sideways; on a phone the grid does
            assertThat(p.evaluate("document.documentElement.scrollWidth <= innerWidth + 1")).as("No page overflow").isEqualTo(true);
            if (width < 1000) assertThat(plot.evaluate("p => p.scrollWidth > p.clientWidth")).isEqualTo(true);
            assertThat(p.locator("[style]")).hasCount(0);
            assertThat(new AxeBuilder(p).include(List.of(".sw-calendar-heatmap")).withTags(List.of("wcag2a", "wcag2aa", "wcag21a", "wcag21aa", "wcag22aa"))
                    .analyze().getViolations()).isEmpty();

            // The keyboard starts on the first day; Right and Left move by week, Down and Up by day
            Locator tooltip = year.locator(".sw-heatmap-tooltip"), status = plot.locator("[data-heatmap-status]");
            plot.focus();
            assertThat(tooltip.locator(".sw-chart-tooltip-label")).hasText("Monday, November 17, 2025");
            assertThat(status).hasText(Pattern.compile("^Monday, November 17, 2025; Commits: \\d+$"));
            p.keyboard().press("ArrowRight");
            assertThat(tooltip.locator(".sw-chart-tooltip-label")).hasText("Monday, November 24, 2025");
            p.keyboard().press("ArrowDown");
            assertThat(tooltip.locator(".sw-chart-tooltip-label")).hasText("Tuesday, November 25, 2025");
            // The last Tuesday is the week before the period ends on a Monday: End passes over the padding day
            p.keyboard().press("End");
            assertThat(tooltip.locator(".sw-chart-tooltip-label")).hasText("Tuesday, November 10, 2026");
            p.keyboard().press("Control+End");
            assertThat(tooltip.locator(".sw-chart-tooltip-label")).hasText("Monday, November 16, 2026");
            p.keyboard().press("ArrowRight");
            assertThat(tooltip.locator(".sw-chart-tooltip-label")).hasText("Monday, November 16, 2026");
            p.keyboard().press("Control+Home");
            assertThat(tooltip.locator(".sw-chart-tooltip-label")).hasText("Monday, November 17, 2025");
            p.keyboard().press("Escape");
            assertThat(tooltip).isHidden();
            p.screenshot(new Page.ScreenshotOptions().setPath(Path.of("target/calendar-heatmap-screenshots", engine + "-" + skin + "-" + mode + ".png")));
            assertThat(errors).isEmpty();
        }
    }

    @ParameterizedTest @ValueSource(strings = {"chromium", "firefox"})
    void daysWithoutAReportAreNotMeasuredAndTheQuarterMovesForwardOverAjax(String engine) {
        try (var context = browser(engine).newContext(new Browser.NewContextOptions().setViewportSize(1280, 1000)); var p = context.newPage()) {
            var errors = errors(p);
            p.navigate(url("skin=vega&mode=light&lang=en-US"));
            Locator sales = calendar(p, "Daily sales, third quarter");
            assertThat(sales.locator("td.sw-heatmap-cell:not([data-outside])")).hasCount(92);
            assertThat(sales.locator("td[data-tooltip-value='Not measured']")).hasCount(2);
            assertThat(sales.locator(".sw-heatmap-missing-key")).hasText("× Not measured");
            // Hover names the day and its exact value
            Locator day = sales.locator("td[data-tooltip-label='Wednesday, July 1, 2026']");
            day.hover();
            assertThat(sales.locator(".sw-heatmap-tooltip .sw-chart-tooltip-label")).hasText("Wednesday, July 1, 2026");
            assertThat(sales.locator(".sw-heatmap-tooltip .sw-chart-tooltip-value")).hasText(Pattern.compile("^€[\\d,]+\\.00$"));

            p.getByRole(AriaRole.LINK, new Page.GetByRoleOptions().setName("Show the fourth quarter")).click();
            Locator next = calendar(p, "Daily sales, fourth quarter so far");
            assertThat(next.locator("td.sw-heatmap-cell:not([data-outside])")).hasCount(92);
            assertThat(next.locator(".sw-heatmap-plot")).hasAttribute("data-heatmap-enhanced", "true");
            // Reported up to November 16: the rest of the quarter is not measured yet
            assertThat(next.locator("td[data-tooltip-label='Thursday, December 31, 2026']")).hasAttribute("data-tooltip-value", "Not measured");
            assertThat(next.locator(".sw-heatmap-plot [data-heatmap-status]")).hasCount(1);
            assertThat(errors).isEmpty();
        }
    }

    @ParameterizedTest @ValueSource(strings = {"chromium", "firefox"})
    void weeksStartOnTheLocalesDayAndRightToLeftReversesTheWeeks(String engine) {
        try (var context = browser(engine).newContext(new Browser.NewContextOptions().setViewportSize(1280, 1000)); var p = context.newPage()) {
            p.navigate(url("skin=vega&mode=light&lang=de-DE"));
            Locator year = calendar(p, "A year of contributions");
            assertThat(year.locator("tbody th").first()).containsText("Montag");
            assertThat(year.locator(".sw-calendar-heatmap-month").first()).hasText("Dez 2025");
            // Right to left: the weeks run from the right, and ArrowLeft moves to the next week
            p.evaluate("document.documentElement.dir = 'rtl'");
            Locator plot = year.locator(".sw-heatmap-plot");
            plot.focus();
            p.keyboard().press("ArrowLeft");
            assertThat(year.locator(".sw-heatmap-tooltip .sw-chart-tooltip-label")).hasText("Montag, 24. November 2025");
            assertThat(year.locator("td[data-active]").evaluate("c => c.getBoundingClientRect().left < c.closest('tr').querySelector('th').getBoundingClientRect().left"))
                    .isEqualTo(true);
        }
    }

    @Test
    void withoutJavaScriptEveryDayIsInTheTable() {
        try (var context = chromium.newContext(new Browser.NewContextOptions().setJavaScriptEnabled(false)); var p = context.newPage()) {
            p.navigate(url("skin=vega&mode=light&lang=en-US"));
            Locator year = calendar(p, "A year of contributions");
            assertThat(year.locator("td[title^='Monday, November 17, 2025; Commits: ']")).hasCount(1);
            assertThat(year.locator("thead th .sw-chart-sr-only").first()).hasText("Week of Nov 16, 2025");
            assertThat(year.locator("td.sw-heatmap-cell:not([data-outside]) .sw-heatmap-value")).hasCount(365);
        }
    }
}
