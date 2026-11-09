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
import static org.assertj.core.api.Assertions.within;

@Tag("browser")
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class RadarChartBrowserTest {
    static Playwright playwright;
    static Browser chromium, firefox;
    static final Path SHOTS = Path.of("target/radar-chart-screenshots");
    @LocalServerPort int port;
    @Autowired WebApplication app;

    @BeforeAll static void start() throws Exception {
        Files.createDirectories(SHOTS);
        playwright = Playwright.create();
        chromium = playwright.chromium().launch();
        firefox = playwright.firefox().launch();
    }
    @AfterAll static void stop() { playwright.close(); }
    @BeforeEach void markup() { app.getMarkupSettings().setStripWicketTags(true); }
    @AfterEach void restoreMarkup() { app.getMarkupSettings().setStripWicketTags(false); }
    Browser browser(String engine) { return engine.equals("chromium") ? chromium : firefox; }
    String url(String skin, String mode) { return "http://localhost:" + port + "/charts?skin=" + skin + "&mode=" + mode; }
    Locator chart(Page page, String title) {
        return page.locator(".sw-radar-chart").filter(new Locator.FilterOptions()
                .setHas(page.getByText(title, new Page.GetByTextOptions().setExact(true))));
    }
    List<String> errors(Page page) {
        List<String> errors = new ArrayList<>();
        page.onPageError(errors::add);
        page.onConsoleMessage(message -> { if (message.type().equals("error")) errors.add(message.text()); });
        return errors;
    }
    void audit(Page page) {
        assertThat(page.evaluate("document.documentElement.scrollWidth <= innerWidth + 1")).as("page overflow").isEqualTo(true);
        assertThat(new AxeBuilder(page).withTags(List.of("wcag2a", "wcag2aa", "wcag21a", "wcag21aa", "wcag22aa"))
                .analyze().getViolations()).isEmpty();
    }
    double markerWidth(Locator marker) {
        return ((Number) marker.evaluate("n=>{const transform=n.getScreenCTM();return n.getBBox().width*Math.hypot(transform.a,transform.b);}")).doubleValue();
    }
    static Stream<Arguments> appearances() {
        return Stream.of("chromium", "firefox").flatMap(engine -> Arrays.stream(Skin.values()).flatMap(skin ->
                Stream.of(Arguments.of(engine, skin.id(), "light", 1440), Arguments.of(engine, skin.id(), "dark", 390))));
    }

    @ParameterizedTest @MethodSource("appearances")
    void sharedScalesMissingValuesAndDimensionKeysFitEverySkin(String engine, String skin, String mode, int width) {
        try (var context = browser(engine).newContext(new Browser.NewContextOptions().setViewportSize(width, 1000)
                .setReducedMotion(ReducedMotion.REDUCE)); var page = context.newPage()) {
            var errors = errors(page);
            page.navigate(url(skin, mode));
            var radar = chart(page, "Team capability profile");
            assertThat(radar.locator(".sw-chart-hit")).hasCount(6);
            assertThat(radar.locator(".sw-radar-area")).hasCount(2);
            assertThat(radar.locator(".sw-radar-dot")).hasCount(6);
            assertThat(radar.locator(".sw-radar-grid")).hasCount(4);
            assertThat(radar.locator(".sw-radar-scale")).hasText("Scale: 0 – 100");
            assertThat(radar.locator(".sw-radar-name")).hasText(new String[]{"Delivery", "Quality", "Communication", "Planning", "Research", "Support"});
            assertThat(markerWidth(radar.locator(".sw-radar-dot").first())).isCloseTo(6, within(.5));
            var plot = radar.locator(".sw-radar-plot");
            assertThat(plot.boundingBox().height).isCloseTo(plot.boundingBox().width, within(1.0));
            radar.locator("[data-chart-index='1']").hover();
            assertThat(radar.locator(".sw-chart-tooltip-label")).hasText("Quality");
            assertThat(radar.locator(".sw-chart-tooltip-value")).hasText(new String[]{"88", "92"});
            assertThat(radar.locator("[data-chart-index][data-active]")).hasAttribute("data-chart-index", "1");
            plot.scrollIntoViewIfNeeded();
            var box = plot.boundingBox();
            page.mouse().move(box.x + box.width / 2, box.y + box.height / 3);
            assertThat(radar.locator(".sw-chart-tooltip-label")).hasText("Delivery");
            var missing = chart(page, "An incomplete assessment");
            assertThat(missing.locator("[data-series='0'] .sw-radar-area")).hasCount(0);
            assertThat(missing.locator("[data-series='0'] .sw-radar-line")).hasCount(4);
            missing.locator("[data-chart-index='3']").hover();
            assertThat(missing.locator(".sw-chart-tooltip-value")).hasText(new String[]{"Not measured", "82"});
            missing.locator("summary").click();
            assertThat(missing.locator("tbody tr")).hasCount(6);
            assertThat(missing.locator("tbody tr").nth(3).locator("td").first()).isEmpty();
            missing.locator("summary").click();
            assertThat(chart(page, "An automatic workload scale").locator(".sw-radar-scale")).hasText("Scale: 0 – 200");
            assertThat(chart(page, "A measured zero").locator(".sw-radar-scale")).hasText("Scale: 0 – 1");
            assertThat(chart(page, "First assessment").locator(".sw-radar-plot")).hasCount(0);
            assertThat(chart(page, "First assessment").locator("[role=status]")).containsText("At least three dimensions");
            assertThat(chart(page, "No measurements yet").locator("[role=status]")).containsText("No measurements");
            assertThat(chart(page, "The next team profile").locator("[role=status]")).containsText("No data");
            audit(page);
            page.mouse().move(0, 0);
            page.locator("section.ex-section").filter(new Locator.FilterOptions().setHasText("A profile across dimensions"))
                    .screenshot(new Locator.ScreenshotOptions().setPath(SHOTS.resolve(engine + "-" + skin + "-" + mode + ".png")));
            assertThat(errors).isEmpty();
        }
    }

    @ParameterizedTest @ValueSource(strings={"chromium", "firefox"})
    void keyboardTouchRtlResizeAndAjaxKeepTheProfileReachable(String engine) {
        var settings = ShadwickSettings.get(app);
        settings.setBundle(Bundle.EMBEDDED);
        try (var context = browser(engine).newContext(new Browser.NewContextOptions().setViewportSize(320, 950)
                .setHasTouch(true).setReducedMotion(ReducedMotion.REDUCE)); var page = context.newPage()) {
            var errors = errors(page);
            page.navigate(url("nova", "dark"));
            page.locator("main").evaluate("n=>n.dir='rtl'");
            var radar = chart(page, "Team capability profile");
            var plot = radar.locator(".sw-radar-plot");
            plot.focus();
            assertThat(radar.locator(".sw-chart-tooltip-label")).hasText("Delivery");
            plot.press("ArrowLeft");
            assertThat(radar.locator(".sw-chart-tooltip-label")).hasText("Quality");
            plot.press("End");
            assertThat(radar.locator(".sw-chart-tooltip-label")).hasText("Support");
            assertThat(radar.locator(".sw-radar-svg").evaluate("n=>getComputedStyle(n).transform")).isNotEqualTo("none");
            assertThat(radar.locator("[data-radar-dir='rtl']")).hasCSS("display", "inline");
            assertThat(radar.locator("[data-radar-dir='ltr']")).hasCSS("display", "none");
            plot.press("Escape");
            assertThat(radar.locator(".sw-chart-tooltip-content")).isHidden();
            assertThat(plot).isFocused();
            radar.locator("[data-chart-index='3']").tap();
            assertThat(radar.locator(".sw-chart-tooltip-label")).hasText("Planning");
            assertThat(radar.locator(".sw-chart-tooltip-value")).hasText(new String[]{"65", "82"});
            assertThat(radar.locator("[tabindex='0']")).hasCount(2); // Plot and native table scroll region.
            assertThat(markerWidth(radar.locator(".sw-radar-dot").first())).isCloseTo(6, within(.5));
            page.setViewportSize(700, 950);
            assertThat(markerWidth(radar.locator(".sw-radar-dot").first())).isCloseTo(6, within(.5));
            radar.evaluate("n=>n.dataset.untouched='true'");
            List<String> navigations = new ArrayList<>();
            page.onRequest(request -> { if (request.isNavigationRequest()) navigations.add(request.url()); });
            page.getByText("Refresh assessment", new Page.GetByTextOptions().setExact(true)).click();
            var live = chart(page, "A changing capability profile");
            assertThat(live.locator(".sw-chart-hit")).hasCount(4);
            assertThat(live.locator("[data-chart-index]")).hasCount(4);
            assertThat(live.locator(".sw-radar-scale")).hasText("Scale: 0 – 120");
            assertThat(live.locator(".sw-radar-svg [data-series='0']")).hasAttribute("class", "sw-chart-series-3");
            live.locator(".sw-radar-plot").focus();
            live.locator(".sw-radar-plot").press("Home");
            assertThat(live.locator(".sw-chart-tooltip-name")).hasText(new String[]{"Latest", "Target"});
            assertThat(live.locator(".sw-chart-tooltip-value")).hasText(new String[]{"97", "110"});
            assertThat(radar).hasAttribute("data-untouched", "true");
            assertThat(navigations).isEmpty();
            live.evaluate("n=>n.replaceWith(n.cloneNode(true))");
            page.evaluate("()=>{Shadwick.enhanceCharts();Shadwick.enhanceCharts();}");
            live.locator("[data-chart-index='3']").tap();
            assertThat(live.locator(".sw-chart-tooltip-label")).hasText("Planning");
            assertThat(live.locator(".sw-chart-tooltip-value")).hasText(new String[]{"88", "100"});
            assertThat(live.locator("[data-chart-status]")).hasCount(1);
            page.emulateMedia(new Page.EmulateMediaOptions().setForcedColors(ForcedColors.ACTIVE));
            live.locator(".sw-radar-plot").press("Tab"); page.keyboard().press("Shift+Tab");
            assertThat(live.locator(".sw-radar-plot")).hasCSS("outline-style", "solid");
            page.emulateMedia(new Page.EmulateMediaOptions().setForcedColors(ForcedColors.NONE));
            audit(page);
            assertThat(errors).isEmpty();
        } finally { settings.setBundle(Bundle.STANDALONE); }
    }

    @ParameterizedTest @ValueSource(strings={"chromium", "firefox"})
    void localizedScaleTitlesAndAllMeasurementsRemainNative(String engine) {
        try (var context = browser(engine).newContext(new Browser.NewContextOptions().setViewportSize(320, 950)
                .setJavaScriptEnabled(false)); var page = context.newPage()) {
            page.navigate(url("vega", "light") + "&lang=de");
            page.locator("main").evaluate("n=>n.dir='rtl'");
            var radar = chart(page, "Team capability profile");
            assertThat(radar.locator(".sw-chart-hit title")).hasCount(6);
            assertThat(radar.locator(".sw-radar-scale")).hasText("Skala: 0 – 100");
            assertThat(radar.locator(".sw-radar-dimensions")).hasAttribute("aria-label", "Dimensionen");
            assertThat(radar.locator(".sw-radar-plot")).not().hasAttribute("tabindex", "0");
            radar.locator("summary").click();
            assertThat(radar.locator("thead")).containsText("Dimension");
            assertThat(radar.locator("tbody tr")).hasCount(6);
            assertThat(radar.locator("tbody tr").first().locator("td").first()).hasText("72");
            var missing = chart(page, "An incomplete assessment");
            assertThat(missing.locator(".sw-chart-hit[data-index='3'] title")).containsText("Nicht gemessen");
            missing.locator("summary").click();
            assertThat(missing.locator("tbody tr").nth(3).locator("td").first()).isEmpty();
            assertThat(chart(page, "First assessment").locator("[role=status]")).containsText("mindestens drei Dimensionen");
            assertThat(chart(page, "First assessment").locator("tbody tr")).hasCount(2);
            assertThat(chart(page, "No measurements yet").locator("[role=status]")).hasText("Keine Messwerte vorhanden.");
            assertThat(chart(page, "The next team profile").locator("[role=status]")).hasText("Keine Daten vorhanden.");
            assertThat(page.evaluate("document.documentElement.scrollWidth <= innerWidth + 1")).isEqualTo(true);
        }
    }
}
