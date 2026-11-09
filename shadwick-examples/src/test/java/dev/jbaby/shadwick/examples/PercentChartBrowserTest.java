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
class PercentChartBrowserTest {
    static Playwright playwright;
    static Browser chromium, firefox;
    static final Path SHOTS = Path.of("target/percent-chart-screenshots");
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
        return page.locator(".sw-chart").filter(new Locator.FilterOptions()
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
    Locator nativeBars(Locator chart) {
        return chart.locator(".sw-chart-native-bars [data-native-dir='ltr'] > svg[data-series]");
    }
    static Stream<Arguments> appearances() {
        return Stream.of("chromium", "firefox").flatMap(engine -> Arrays.stream(Skin.values()).flatMap(skin ->
                Stream.of(Arguments.of(engine, skin.id(), "light", 1440), Arguments.of(engine, skin.id(), "dark", 390))));
    }

    @ParameterizedTest @MethodSource("appearances")
    void percentageGeometryAndOriginalCountsFitEverySkin(String engine, String skin, String mode, int width) {
        try (var context = browser(engine).newContext(new Browser.NewContextOptions().setViewportSize(width, 1000)
                .setReducedMotion(ReducedMotion.REDUCE)); var page = context.newPage()) {
            var errors = errors(page);
            page.navigate(url(skin, mode));
            var bars = chart(page, "Device share by month");
            assertThat(bars.locator(".sw-chart-plot")).hasAttribute("data-stack-mode", "percent");
            assertThat(bars.locator(".sw-chart-y-axis")).containsText("100%");
            assertThat(nativeBars(bars)).hasCount(9);
            assertThat(nativeBars(bars).first()).hasAttribute("height", "72");
            assertThat(bars.locator(".sw-chart-native-bars [data-native-dir='ltr'] > svg[data-index='2']")).hasCount(0);
            bars.locator(".sw-chart-hit[data-index='1']").hover();
            var tip = bars.locator(".sw-chart-tooltip-content");
            assertThat(tip).containsText("February");
            assertThat(tip.locator(".sw-chart-tooltip-value")).hasText(new String[]{"120", "80"});
            bars.locator("summary").click();
            assertThat(bars.locator("tbody tr")).hasCount(6);
            assertThat(bars.locator("tbody tr").nth(2).locator("td")).hasText(new String[]{"0", "0"});
            assertThat(bars.locator("tbody tr").nth(3).locator("td").first()).isEmpty();
            var horizontal = chart(page, "Device share across rows");
            assertThat(horizontal.locator(".sw-chart-row")).hasCount(6);
            assertThat(horizontal.locator(".sw-chart-value-axis")).containsText("100%");
            assertThat(nativeBars(horizontal).first()).hasAttribute("width", "30%");
            assertThat(horizontal.locator(".sw-chart-row").nth(2).locator(".sw-chart-bar")).hasCount(0);
            assertThat(chart(page, "A changing device mix").locator(".sw-chart-area")).hasCount(4);
            audit(page);
            bars.locator("summary").click();
            page.locator("section.ex-section").filter(new Locator.FilterOptions().setHasText("Compare the mix, keep the numbers"))
                    .screenshot(new Locator.ScreenshotOptions().setPath(SHOTS.resolve(engine + "-" + skin + "-" + mode + ".png")));
            assertThat(errors).isEmpty();
        }
    }

    @ParameterizedTest @ValueSource(strings={"chromium", "firefox"})
    void keyboardTouchRtlAndAjaxRecomputeEachCategoryWithoutReplacingOtherCharts(String engine) {
        var settings = ShadwickSettings.get(app);
        settings.setBundle(Bundle.EMBEDDED);
        try (var context = browser(engine).newContext(new Browser.NewContextOptions().setViewportSize(320, 950)
                .setHasTouch(true).setReducedMotion(ReducedMotion.REDUCE)); var page = context.newPage()) {
            var errors = errors(page);
            page.navigate(url("nova", "dark"));
            page.locator("main").evaluate("n=>n.dir='rtl'");
            var bars = chart(page, "Device share by month");
            var plot = bars.locator(".sw-chart-plot");
            plot.focus();
            plot.press("ArrowLeft");
            assertThat(bars.locator(".sw-chart-tooltip-content")).containsText("February");
            assertThat(bars.locator(".sw-chart-tooltip-value")).hasText(new String[]{"120", "80"});
            plot.press("Escape");
            assertThat(bars.locator(".sw-chart-tooltip-content")).isHidden();
            assertThat(plot).isFocused();
            var horizontal = chart(page, "Device share across rows");
            horizontal.locator(".sw-chart-hit[data-index='4']").tap();
            assertThat(horizontal.locator(".sw-chart-tooltip-content")).containsText("May");
            horizontal.locator(".sw-chart-plot").press("ArrowDown");
            assertThat(horizontal.locator(".sw-chart-tooltip-content")).containsText("June");
            bars.evaluate("n=>n.dataset.untouched='true'");
            List<String> navigations = new ArrayList<>();
            page.onRequest(request -> { if (request.isNavigationRequest()) navigations.add(request.url()); });
            page.getByText("Refresh report", new Page.GetByTextOptions().setExact(true)).click();
            for (String title : List.of("Live device proportions", "Live device proportions by row", "Live device proportions over time")) {
                var live = chart(page, title);
                var livePlot = live.locator(".sw-chart-plot");
                livePlot.focus();
                livePlot.press("Home");
                assertThat(live.locator(".sw-chart-tooltip-content")).containsText("July");
                assertThat(live.locator(".sw-chart-tooltip-value")).hasText(new String[]{"250", "100"});
                assertThat(live.locator(".sw-chart-hit")).hasCount(3);
            }
            var liveHorizontal = chart(page, "Live device proportions by row");
            assertThat(nativeBars(liveHorizontal).first()).hasAttribute("width", "71.42857%");
            assertThat(bars).hasAttribute("data-untouched", "true");
            assertThat(navigations).isEmpty();
            var liveArea = chart(page, "Live device proportions over time");
            liveArea.evaluate("n=>n.replaceWith(n.cloneNode(true))");
            page.evaluate("()=>{Shadwick.enhanceCharts();Shadwick.enhanceCharts();}");
            liveArea.locator(".sw-chart-plot").focus();
            liveArea.locator(".sw-chart-plot").press("Home");
            assertThat(liveArea.locator("[data-chart-status]")).hasCount(1);
            assertThat(liveArea.locator(".sw-chart-tooltip-content")).containsText("July");
            page.emulateMedia(new Page.EmulateMediaOptions().setForcedColors(ForcedColors.ACTIVE));
            liveArea.locator(".sw-chart-plot").press("Tab");
            page.keyboard().press("Shift+Tab");
            assertThat(liveArea.locator(".sw-chart-plot")).hasCSS("outline-style", "solid");
            page.emulateMedia(new Page.EmulateMediaOptions().setForcedColors(ForcedColors.NONE));
            audit(page);
            assertThat(errors).isEmpty();
        } finally { settings.setBundle(Bundle.STANDALONE); }
    }

    @ParameterizedTest @ValueSource(strings={"chromium", "firefox"})
    void localizedPercentAxesAndOriginalZeroMissingValuesRemainNative(String engine) {
        try (var context = browser(engine).newContext(new Browser.NewContextOptions().setViewportSize(320, 950)
                .setJavaScriptEnabled(false)); var page = context.newPage()) {
            page.navigate(url("vega", "light") + "&lang=de");
            page.locator("main").evaluate("n=>n.dir='rtl'");
            for (String title : List.of("Device share by month", "Device share across rows", "A changing device mix")) {
                var chart = chart(page, title);
                assertThat(chart.locator(".sw-chart-y-axis, .sw-chart-value-axis").textContent().replace('\u00a0', ' ')).contains("100 %");
                assertThat(chart.locator(".sw-chart-hit title")).hasCount(6);
                assertThat(chart.locator(".sw-chart-hit[data-index='1'] title")).containsText("120");
                assertThat(chart.locator(".sw-chart-plot")).not().hasAttribute("tabindex", "0");
                chart.locator("summary").click();
                assertThat(chart.locator("tbody tr").nth(2).locator("td")).hasText(new String[]{"0", "0"});
                assertThat(chart.locator("tbody tr").nth(3).locator("td").first()).isEmpty();
                chart.locator("summary").click();
            }
            assertThat(page.evaluate("document.documentElement.scrollWidth <= innerWidth + 1")).isEqualTo(true);
        }
    }
}
