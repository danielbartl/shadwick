package dev.jbaby.shadwick.examples;

import com.deque.html.axecore.playwright.AxeBuilder;
import com.microsoft.playwright.*;
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
import java.util.stream.Stream;

import static com.microsoft.playwright.assertions.PlaywrightAssertions.assertThat;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;

/** Category grid lines, tick margins and the plot margin of SwChart, in real browsers. */
@Tag("browser")
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class ChartLayoutBrowserTest {
    static Playwright playwright;
    static Browser chromium, firefox;
    @LocalServerPort int port;
    @Autowired WebApplication app;

    @BeforeAll static void start() throws Exception {
        Files.createDirectories(Path.of("target/chart-layout-screenshots"));
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

    Locator card(Page p, String title) {
        return p.locator(".ex-chart-card").filter(new Locator.FilterOptions().setHas(p.locator("figcaption", new Page.LocatorOptions().setHasText(title))));
    }
    double number(Locator element, String script) { return ((Number) element.evaluate(script)).doubleValue(); }

    @ParameterizedTest @MethodSource("skins")
    void categoryLinesTickMarginsAndThePlotMarginFollowTheSettings(String engine, String skin, String mode, int width) {
        try (var context = (engine.equals("chromium") ? chromium : firefox).newContext(new Browser.NewContextOptions().setViewportSize(width, 1000));
             var p = context.newPage()) {
            var errors = new ArrayList<String>();
            p.onPageError(errors::add);
            p.onConsoleMessage(m -> { if (m.type().equals("error")) errors.add(m.text()); });
            p.navigate("http://localhost:" + port + "/charts?skin=" + skin + "&mode=" + mode);

            Locator vertical = card(p, "Grid lines and spacing").locator(".sw-chart-container");
            vertical.scrollIntoViewIfNeeded();
            // The layout rule is a nonce-carrying header stylesheet: the strict CSP lets it apply
            String layout = (String) vertical.evaluate("c => [...c.classList].find(n => n.startsWith('sw-chart-layout-'))");
            assertThat(p.evaluate("cls => !!document.getElementById(cls) && document.getElementById(cls).nonce !== ''", layout)).isEqualTo(true);
            assertThat(vertical.evaluate("c => getComputedStyle(c).columnGap")).isEqualTo("8px");
            assertThat(vertical.evaluate("c => getComputedStyle(c).rowGap")).isEqualTo("4px");
            assertThat(vertical.locator(".sw-chart-plot").evaluate("p => getComputedStyle(p).marginInlineStart")).isEqualTo("16px");
            // The x-axis moves with the plot: the labels stay under the category lines
            Locator lines = vertical.locator("[data-category-grid]");
            assertThat(lines).hasCount(6);
            Locator ticks = vertical.locator(".sw-chart-x-axis > span");
            for (int i : new int[]{0, 5}) {
                double line = number(lines.nth(i), "l => { const r = l.getBoundingClientRect(); return r.left + r.width / 2; }");
                double tick = number(ticks.nth(i), "t => { const r = t.getBoundingClientRect(); return r.left + r.width / 2; }");
                assertThat(line).as("category line %d under its label", i).isCloseTo(tick, within(2.0));
            }
            assertThat(number(vertical, "c => c.querySelector('.sw-chart-x-axis').getBoundingClientRect().top - c.querySelector('.sw-chart-plot').getBoundingClientRect().bottom"))
                    .isCloseTo(4.0, within(1.0));

            Locator horizontal = card(p, "A line through every row");
            assertThat(horizontal.locator("[data-category-grid]")).hasCount(4);
            assertThat(horizontal.locator(".sw-chart-plot").evaluate("p => getComputedStyle(p).columnGap")).isEqualTo("8px");
            assertThat(horizontal.locator(".sw-chart-row").first().evaluate("r => getComputedStyle(r).marginInlineEnd")).isEqualTo("8px");
            assertThat(horizontal.locator(".sw-chart-value-axis").evaluate("a => getComputedStyle(a).marginTop")).isEqualTo("6px");

            // A chart without the settings keeps the defaults
            Locator plain = card(p, "A softer monthly report").locator(".sw-chart-container");
            assertThat(plain.locator("[data-category-grid]")).hasCount(0);
            assertThat(plain.locator(".sw-chart-plot").evaluate("p => getComputedStyle(p).marginInlineStart")).isEqualTo("0px");
            assertThat(plain.evaluate("c => getComputedStyle(c).rowGap")).isEqualTo("12px");

            // Right to left: the value axis stands right of the plot, the margins still apply
            p.evaluate("document.documentElement.dir = 'rtl'");
            assertThat(number(vertical, "c => c.querySelector('.sw-chart-y-axis').getBoundingClientRect().left - c.querySelector('.sw-chart-plot').getBoundingClientRect().right"))
                    .isCloseTo(24.0, within(1.0));
            p.evaluate("document.documentElement.dir = 'ltr'");

            assertThat(p.locator("[style]")).hasCount(0);
            assertThat(p.evaluate("document.documentElement.scrollWidth <= innerWidth + 1")).isEqualTo(true);
            assertThat(new AxeBuilder(p).include(List.of(".ex-chart-card")).withTags(List.of("wcag2a", "wcag2aa", "wcag21a", "wcag21aa", "wcag22aa"))
                    .analyze().getViolations()).isEmpty();
            card(p, "Grid lines and spacing").locator("xpath=../..").screenshot(new Locator.ScreenshotOptions()
                    .setPath(Path.of("target/chart-layout-screenshots", engine + "-" + skin + "-" + mode + ".png")));
            assertThat(errors).isEmpty();
        }
    }
}
