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
class ScatterChartBrowserTest {
    static Playwright playwright;
    static Browser chromium, firefox;
    static final Path SHOTS = Path.of("target/scatter-chart-screenshots");
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
        return page.locator(".sw-scatter-chart").filter(new Locator.FilterOptions()
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
    void numericAxesIndependentSeriesAndNativeMarkersFitEverySkin(String engine, String skin, String mode, int width) {
        try (var context = browser(engine).newContext(new Browser.NewContextOptions().setViewportSize(width, 1000)
                .setReducedMotion(ReducedMotion.REDUCE)); var page = context.newPage()) {
            var errors = errors(page);
            page.navigate(url(skin, mode));
            var scatter = chart(page, "Effort and revenue");
            assertThat(scatter.locator(".sw-scatter-hit")).hasCount(7);
            assertThat(scatter.locator(".sw-scatter-mark circle")).hasCount(4);
            assertThat(scatter.locator(".sw-scatter-mark rect")).hasCount(3);
            assertThat(scatter.locator(".sw-scatter-x-axis")).containsText("50");
            assertThat(scatter.locator(".sw-scatter-x-axis")).not().containsText("1,000");
            var point = scatter.locator(".sw-scatter-mark[data-index='0'] circle");
            assertThat(markerWidth(point)).isCloseTo(10, within(.5));
            scatter.locator(".sw-scatter-hit[data-index='0']").hover();
            assertThat(scatter.locator(".sw-chart-tooltip-label")).hasText("Aster");
            assertThat(scatter.locator(".sw-chart-tooltip-value")).hasText(new String[]{"12", "€2,400.00"});
            assertThat(scatter.locator(".sw-scatter-mark[data-active]")).hasAttribute("data-index", "0");
            scatter.locator("summary").click();
            assertThat(scatter.locator("tbody tr")).hasCount(8);
            assertThat(scatter.locator("tbody tr").nth(4)).containsText("Awaiting invoice");
            assertThat(scatter.locator("tbody tr").nth(4).locator("td").last()).isEmpty();
            var single = chart(page, "One completed project");
            assertThat(single.locator(".sw-scatter-hit")).hasCount(1);
            assertThat(single.locator(".sw-scatter-mark")).hasAttribute("x", "50%");
            assertThat(single.locator(".sw-scatter-mark")).hasAttribute("y", "120");
            assertThat(chart(page, "Waiting for complete reports").locator(".sw-scatter-hit")).hasCount(0);
            assertThat(chart(page, "The next relationship").locator("[role=status]")).containsText("No data");
            audit(page);
            scatter.locator("summary").click();
            page.mouse().move(0, 0);
            page.locator("section.ex-section").filter(new Locator.FilterOptions().setHasText("How two measurements relate"))
                    .screenshot(new Locator.ScreenshotOptions().setPath(SHOTS.resolve(engine + "-" + skin + "-" + mode + ".png")));
            assertThat(errors).isEmpty();
        }
    }

    @ParameterizedTest @ValueSource(strings={"chromium", "firefox"})
    void keyboardOverlapRtlTouchResizeAndAjaxKeepPointsReachable(String engine) {
        var settings = ShadwickSettings.get(app);
        settings.setBundle(Bundle.EMBEDDED);
        try (var context = browser(engine).newContext(new Browser.NewContextOptions().setViewportSize(320, 950)
                .setHasTouch(true).setReducedMotion(ReducedMotion.REDUCE)); var page = context.newPage()) {
            var errors = errors(page);
            page.navigate(url("nova", "dark"));
            page.locator("main").evaluate("n=>n.dir='rtl'");
            var scatter = chart(page, "Effort and revenue");
            var plot = scatter.locator(".sw-scatter-plot");
            plot.focus();
            assertThat(scatter.locator(".sw-chart-tooltip-label")).hasText("Aster");
            plot.press("ArrowLeft");
            assertThat(scatter.locator(".sw-chart-tooltip-label")).hasText("Birch");
            plot.press("End");
            assertThat(scatter.locator(".sw-chart-tooltip-label")).hasText("Cirrus");
            assertThat(scatter.locator(".sw-scatter-points").evaluate("n=>getComputedStyle(n).transform")).isNotEqualTo("none");
            plot.press("Escape");
            assertThat(scatter.locator(".sw-chart-tooltip-content")).isHidden();
            assertThat(plot).isFocused();
            scatter.locator(".sw-scatter-hit[data-index='0']").tap();
            assertThat(scatter.locator(".sw-chart-tooltip-label")).hasText("Aster");
            var overlap = chart(page, "Overlapping measurements");
            overlap.locator(".sw-scatter-plot").focus();
            overlap.locator(".sw-scatter-plot").press("Home");
            assertThat(overlap.locator(".sw-chart-tooltip-label")).hasText("Same coordinate A");
            overlap.locator(".sw-scatter-plot").press("ArrowDown");
            assertThat(overlap.locator(".sw-chart-tooltip-label")).hasText("Same coordinate B");
            assertThat(overlap.locator(".sw-scatter-points > svg").last()).hasAttribute("data-index", "1");
            var quiet = chart(page, "A quiet relationship");
            assertThat(quiet.locator(".sw-scatter-plot")).not().hasAttribute("tabindex", "0");
            assertThat(quiet.locator(".sw-scatter-x-axis")).hasAttribute("data-axis-enhanced", "true");
            assertThat(quiet.locator(".sw-scatter-x-axis").evaluate("""
                axis=>{const bounds=axis.getBoundingClientRect();const boxes=[...axis.children]
                  .filter(n=>getComputedStyle(n).visibility!=='hidden').map(n=>n.firstElementChild.getBoundingClientRect()).sort((a,b)=>a.left-b.left);
                  return boxes.every((box,i)=>box.left>=bounds.left-1&&box.right<=bounds.right+1&&(!i||boxes[i-1].right<=box.left+1));}
                """)).isEqualTo(true);
            page.setViewportSize(700, 950);
            assertThat(markerWidth(scatter.locator(".sw-scatter-mark[data-index='0'] circle"))).isCloseTo(10, within(.5));
            scatter.evaluate("n=>n.dataset.untouched='true'");
            List<String> navigations = new ArrayList<>();
            page.onRequest(request -> { if (request.isNavigationRequest()) navigations.add(request.url()); });
            page.getByText("Refresh measurements", new Page.GetByTextOptions().setExact(true)).click();
            var live = chart(page, "Fresh delivery relationships");
            assertThat(live.locator(".sw-scatter-hit")).hasCount(3);
            assertThat(live.locator(".sw-scatter-mark path")).hasCount(1);
            live.locator(".sw-scatter-plot").focus();
            live.locator(".sw-scatter-plot").press("Home");
            assertThat(live.locator(".sw-chart-tooltip-label")).hasText("Clover launch");
            assertThat(live.locator(".sw-chart-tooltip-value")).hasText(new String[]{"48", "€7,200.00"});
            assertThat(scatter).hasAttribute("data-untouched", "true");
            assertThat(navigations).isEmpty();
            live.evaluate("n=>n.replaceWith(n.cloneNode(true))");
            page.evaluate("()=>{Shadwick.enhanceScatterCharts();Shadwick.enhanceScatterCharts();}");
            live.locator(".sw-scatter-plot").focus();
            live.locator(".sw-scatter-plot").press("End");
            assertThat(live.locator(".sw-chart-tooltip-label")).hasText("Summit launch");
            assertThat(live.locator("[data-scatter-status]")).hasCount(1);
            page.emulateMedia(new Page.EmulateMediaOptions().setForcedColors(ForcedColors.ACTIVE));
            live.locator(".sw-scatter-plot").press("Tab"); page.keyboard().press("Shift+Tab");
            assertThat(live.locator(".sw-scatter-plot")).hasCSS("outline-style", "solid");
            page.emulateMedia(new Page.EmulateMediaOptions().setForcedColors(ForcedColors.NONE));
            audit(page);
            assertThat(errors).isEmpty();
        } finally { settings.setBundle(Bundle.STANDALONE); }
    }

    @ParameterizedTest @ValueSource(strings={"chromium", "firefox"})
    void localizedTitlesShapesAndAllOriginalMeasurementsRemainNative(String engine) {
        try (var context = browser(engine).newContext(new Browser.NewContextOptions().setViewportSize(320, 950)
                .setJavaScriptEnabled(false)); var page = context.newPage()) {
            page.navigate(url("vega", "light") + "&lang=de");
            page.locator("main").evaluate("n=>n.dir='rtl'");
            var scatter = chart(page, "Effort and revenue");
            assertThat(scatter.locator(".sw-scatter-hit title")).hasCount(7);
            assertThat(scatter.locator(".sw-scatter-hit[data-index='0'] title")).containsText("Aster");
            assertThat(scatter.locator(".sw-scatter-plot")).not().hasAttribute("tabindex", "0");
            scatter.locator("summary").click();
            assertThat(scatter.locator("thead")).containsText("Datenreihe");
            assertThat(scatter.locator("tbody tr")).hasCount(8);
            assertThat(scatter.locator("tbody tr").first().locator("td").last()).containsText("2.400,00");
            assertThat(scatter.locator("tbody tr").nth(4).locator("td").last()).isEmpty();
            assertThat(chart(page, "Waiting for complete reports").locator("[role=status]")).containsText("Koordinatenpaare");
            assertThat(chart(page, "Waiting for complete reports").locator("tbody tr")).hasCount(2);
            assertThat(chart(page, "The next relationship").locator("[role=status]")).hasText("Keine Daten vorhanden.");
            assertThat(page.evaluate("document.documentElement.scrollWidth <= innerWidth + 1")).isEqualTo(true);
        }
    }
}
