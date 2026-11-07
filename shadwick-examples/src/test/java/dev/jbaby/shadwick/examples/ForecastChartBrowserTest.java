package dev.jbaby.shadwick.examples;

import com.deque.html.axecore.playwright.AxeBuilder;
import com.microsoft.playwright.*;
import com.microsoft.playwright.options.*;
import dev.jbaby.shadwick.*;
import org.apache.wicket.protocol.http.WebApplication;
import org.junit.jupiter.api.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import java.nio.file.*;
import java.util.*;
import java.util.stream.Stream;
import static com.microsoft.playwright.assertions.PlaywrightAssertions.assertThat;
import static org.assertj.core.api.Assertions.assertThat;

@Tag("browser")
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class ForecastChartBrowserTest {
    static Playwright playwright;
    static Browser chromium, firefox;
    static final Path SHOTS = Path.of("target/forecast-chart-screenshots");
    @LocalServerPort int port;
    @Autowired WebApplication app;
    @BeforeAll static void start() throws Exception {
        Files.createDirectories(SHOTS); playwright = Playwright.create();
        chromium = playwright.chromium().launch(); firefox = playwright.firefox().launch();
    }
    @AfterAll static void stop() { playwright.close(); }
    @BeforeEach void markup() { app.getMarkupSettings().setStripWicketTags(true); }
    @AfterEach void restoreMarkup() { app.getMarkupSettings().setStripWicketTags(false); }
    Browser browser(String engine) { return engine.equals("chromium") ? chromium : firefox; }
    String url(String skin, String mode) { return "http://localhost:" + port + "/charts?skin=" + skin + "&mode=" + mode; }
    Locator chart(Page p, String title) { return p.locator(".sw-chart").filter(new Locator.FilterOptions().setHasText(title)); }
    List<String> errors(Page p) {
        List<String> errors = new ArrayList<>(); p.onPageError(errors::add);
        p.onConsoleMessage(message -> { if (message.type().equals("error")) errors.add(message.text()); }); return errors;
    }
    void anchor(Locator chart, int row) {
        var source = chart.locator("g[data-series='0'] .sw-chart-dot").nth(row);
        var forecast = chart.locator("g[data-series='1']");
        assertThat(forecast).hasAttribute("data-join-index", String.valueOf(row));
        assertThat(forecast.locator(".sw-chart-line").first().getAttribute("d"))
                .startsWith("M" + source.getAttribute("cx") + "," + source.getAttribute("cy"));
    }
    void audit(Page p) {
        assertThat(p.evaluate("document.documentElement.scrollWidth <= innerWidth + 1")).isEqualTo(true);
        assertThat(new AxeBuilder(p).withTags(List.of("wcag2a", "wcag2aa", "wcag21a", "wcag21aa", "wcag22aa"))
                .analyze().getViolations()).isEmpty();
    }
    static Stream<Arguments> appearances() {
        return Stream.of("chromium", "firefox").flatMap(engine -> Stream.of("vega", "nova", "sketch").flatMap(skin ->
                Stream.of(Arguments.of(engine, skin, "light", 1440), Arguments.of(engine, skin, "dark", 390))));
    }
    @ParameterizedTest @MethodSource("appearances")
    void lineAndAreaHandoffsUseTheActualPointWithoutAddingAReportedValue(String engine, String skin, String mode, int width) {
        try (var context = browser(engine).newContext(new Browser.NewContextOptions().setViewportSize(width, 1000)
                .setReducedMotion(ReducedMotion.REDUCE)); var p = context.newPage()) {
            var errors = errors(p); p.navigate(url(skin, mode));
            for (String title : List.of("Actual into forecast", "A forecast with context")) {
                var c = chart(p, title); anchor(c, 2);
                assertThat(c.locator("g[data-series='1'] .sw-chart-dot")).hasCount(3);
                c.locator(".sw-chart-hit[data-index='2']").hover();
                assertThat(c.locator(".sw-chart-tooltip-content")).containsText("March");
                assertThat(c.locator(".sw-chart-tooltip-name")).hasText("Actual");
                assertThat(c.locator(".sw-chart-tooltip-value")).hasText("410");
                c.locator(".sw-chart-hit[data-index='3']").hover();
                assertThat(c.locator(".sw-chart-tooltip-name")).hasText("Forecast");
                assertThat(c.locator(".sw-chart-tooltip-indicator[data-dashed]")).hasCount(1);
                c.locator("summary").click();
                assertThat(c.locator("tbody tr").nth(2).locator("td").last()).isEmpty();
                assertThat(c.locator("tbody tr").nth(3)).containsText("560");
                audit(p); c.locator("summary").click();
            }
            var gaps = chart(p, "A forecast with a missing report");
            assertThat(gaps.locator("g[data-series='1'] .sw-chart-line")).hasCount(2);
            assertThat(gaps.locator("g[data-series='1'] .sw-chart-dot")).hasCount(2);
            p.locator("section.ex-section").filter(new Locator.FilterOptions().setHasText("Where actual becomes forecast"))
                    .screenshot(new Locator.ScreenshotOptions().setPath(SHOTS.resolve(engine + "-" + skin + "-" + mode + ".png")));
            assertThat(errors).isEmpty();
        }
    }
    @ParameterizedTest @ValueSource(strings={"chromium", "firefox"})
    void keyboardStatusAndAjaxMoveTheHandoffWithoutNavigationOrUnrelatedReplacement(String engine) {
        var settings = ShadwickSettings.get(app); settings.setBundle(Bundle.EMBEDDED);
        try (var context = browser(engine).newContext(new Browser.NewContextOptions().setViewportSize(320, 950)
                .setReducedMotion(ReducedMotion.REDUCE)); var p = context.newPage()) {
            var errors = errors(p); p.navigate(url("nova", "dark")); p.locator("main").evaluate("n=>n.dir='rtl'");
            var live = chart(p, "The handoff moves forward"); anchor(live, 2);
            var plot = live.locator(".sw-chart-plot"); plot.focus(); plot.press("Home"); plot.press("ArrowLeft"); plot.press("ArrowLeft");
            assertThat(live.locator("[data-chart-status]")).containsText("Actual: 410");
            assertThat(live.locator("[data-chart-status]")).not().containsText("Forecast");
            var original = chart(p, "Actual into forecast"); original.evaluate("n=>n.dataset.untouched='true'");
            List<String> navigation = new ArrayList<>(); p.onRequest(request -> { if (request.isNavigationRequest()) navigation.add(request.url()); });
            p.getByText("Refresh report", new Page.GetByTextOptions().setExact(true)).click();
            assertThat(live.locator("g[data-series='1']")).hasAttribute("data-join-index", "3"); anchor(live, 3);
            plot.focus(); plot.press("Home"); for (int i = 0; i < 3; i++) plot.press("ArrowLeft");
            assertThat(live.locator(".sw-chart-tooltip-name")).hasText("Actual");
            assertThat(live.locator(".sw-chart-tooltip-value")).hasText("560");
            plot.press("ArrowLeft"); assertThat(live.locator(".sw-chart-tooltip-name")).hasText("Forecast");
            assertThat(live.locator(".sw-chart-tooltip-value")).hasText("700");
            assertThat(live.locator("[data-chart-status]")).hasCount(1);
            assertThat(original).hasAttribute("data-untouched", "true"); assertThat(navigation).isEmpty(); audit(p); assertThat(errors).isEmpty();
        } finally { settings.setBundle(Bundle.STANDALONE); }
    }
    @ParameterizedTest @ValueSource(strings={"chromium", "firefox"})
    void nativePlotTitlesAndTableKeepTheBorrowedAnchorOutOfReportedData(String engine) {
        try (var context = browser(engine).newContext(new Browser.NewContextOptions().setViewportSize(320, 950).setJavaScriptEnabled(false)); var p = context.newPage()) {
            p.navigate(url("vega", "light")); p.locator("main").evaluate("n=>n.dir='rtl'");
            var c = chart(p, "Actual into forecast"); anchor(c, 2);
            assertThat(c.locator(".sw-chart-hit[data-index='2'] title")).hasText("March; Actual: 410");
            c.locator("summary").click(); assertThat(c.locator("table")).isVisible();
            assertThat(c.locator("tbody tr").nth(2).locator("td").last()).isEmpty();
            assertThat(c.locator("tbody tr").nth(3).locator("td").last()).hasText("560");
            assertThat(c.locator(".sw-chart-plot")).not().hasAttribute("tabindex", "0");
            assertThat(p.evaluate("document.documentElement.scrollWidth <= innerWidth + 1")).isEqualTo(true);
        }
    }
}
