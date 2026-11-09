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
class SparklineBrowserTest {
    static Playwright playwright;
    static Browser chromium, firefox;
    static final Path SHOTS = Path.of("target/sparkline-screenshots");
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
    void audit(Page p) {
        assertThat(p.evaluate("document.documentElement.scrollWidth <= innerWidth + 1")).isEqualTo(true);
        assertThat(new AxeBuilder(p).withTags(List.of("wcag2a", "wcag2aa", "wcag21a", "wcag21aa", "wcag22aa"))
                .analyze().getViolations()).isEmpty();
    }
    static Stream<Arguments> appearances() {
        return Stream.of("chromium", "firefox").flatMap(engine -> Arrays.stream(Skin.values()).flatMap(skin ->
                Stream.of(Arguments.of(engine, skin.id(), "light", 1440), Arguments.of(engine, skin.id(), "dark", 390))));
    }
    @ParameterizedTest @MethodSource("appearances")
    void trendsAndNativeSizeLabelsFitEverySkin(String engine, String skin, String mode, int width) {
        try (var context = browser(engine).newContext(new Browser.NewContextOptions().setViewportSize(width, 1000)
                .setReducedMotion(ReducedMotion.REDUCE)); var p = context.newPage()) {
            var errors = errors(p); p.navigate(url(skin, mode));
            assertThat(p.locator(".sw-sparkline")).hasCount(8);
            assertThat(p.locator(".sw-sparkline").first()).hasAttribute("aria-hidden", "true");
            assertThat(p.locator(".sw-sparkline").first()).not().hasAttribute("tabindex", "0");
            assertThat(p.locator(".ex-spark-metric").first()).containsText("84 h");
            assertThat(p.locator(".sw-sparkline").first().evaluate("n=>{const a=n.getBoundingClientRect(),b=n.querySelector('.sw-sparkline-last').getBoundingClientRect();return b.left>=a.left-1 && b.right<=a.right+1 && b.top>=a.top-1 && b.bottom<=a.bottom+1;}")).isEqualTo(true);
            var bars = chart(p, "Highest and latest months");
            assertThat(bars.locator("[data-label-dir=ltr] text")).hasText(new String[]{"214", "305"});
            assertThat(bars.locator("[data-label-dir=ltr] text").first()).hasCSS("font-size", "11px");
            var horizontal = chart(p, "Hours without hovering");
            assertThat(horizontal.locator("[data-label-dir=ltr] text")).hasText(new String[]{"84", "68", "52", "36"});
            assertThat(horizontal.locator(".sw-chart-plot")).hasAttribute("data-labels-enhanced", "true");
            assertThat(horizontal.locator("[data-label-dir=ltr] text").first()).isVisible();
            audit(p);
            p.locator(".ex-spark-section").screenshot(new Locator.ScreenshotOptions().setPath(SHOTS.resolve(engine + "-" + skin + "-" + mode + "-trends.png")));
            p.locator("section.ex-section").filter(new Locator.FilterOptions().setHasText("The values that matter"))
                    .screenshot(new Locator.ScreenshotOptions().setPath(SHOTS.resolve(engine + "-" + skin + "-" + mode + "-labels.png")));
            assertThat(errors).isEmpty();
        }
    }
    static final String CLEAR_LABELS = """
        root => {
            const visible=[...root.querySelectorAll('.sw-chart-value-label')].filter(n=>getComputedStyle(n).visibility!=='hidden')
                .map(n=>n.getBoundingClientRect()).filter(r=>r.width>0 && r.height>0);
            return visible.length>0 && visible.length<24 && visible.every((a,i)=>visible.slice(i+1).every(b=>a.right<=b.left || a.left>=b.right || a.bottom<=b.top || a.top>=b.bottom));
        }
        """;
    @ParameterizedTest @ValueSource(strings={"chromium", "firefox"})
    void labelLayoutResizesMirrorsAndRefreshesWithoutReplacingOtherRegions(String engine) {
        var settings = ShadwickSettings.get(app); settings.setBundle(Bundle.EMBEDDED);
        try (var context = browser(engine).newContext(new Browser.NewContextOptions().setViewportSize(320, 950)
                .setReducedMotion(ReducedMotion.REDUCE)); var p = context.newPage()) {
            var errors = errors(p); p.navigate(url("nova", "dark"));
            var dense = chart(p, "Labels that make room");
            assertThat(dense.locator(".sw-chart-plot")).hasAttribute("data-labels-enhanced", "true");
            p.waitForCondition(() -> (Boolean) dense.evaluate(CLEAR_LABELS));
            dense.evaluate("""
                root=>{
                    const clone=root.cloneNode(true),data=clone.querySelector('.sw-chart-data');
                    data.remove();clone.pendingData=data;
                    clone.querySelector('.sw-chart-plot').removeAttribute('data-labels-enhanced');
                    clone.querySelectorAll('[data-collided]').forEach(n=>n.removeAttribute('data-collided'));
                    root.replaceWith(clone);Shadwick.enhanceCharts();
                }
                """);
            assertThat(dense.locator(".sw-chart-plot")).not().hasAttribute("data-labels-enhanced", "true");
            dense.evaluate("root=>{root.append(root.pendingData);delete root.pendingData;Shadwick.enhanceCharts();}");
            assertThat(dense.locator(".sw-chart-plot")).hasAttribute("data-labels-enhanced", "true");
            p.waitForCondition(() -> (Boolean) dense.evaluate(CLEAR_LABELS));
            p.setViewportSize(1440, 1000); p.waitForCondition(() -> (Boolean) dense.evaluate(CLEAR_LABELS));
            p.setViewportSize(320, 950); p.locator("main").evaluate("n=>n.dir='rtl'");
            p.waitForCondition(() -> (Boolean) dense.evaluate(CLEAR_LABELS));
            var horizontal = chart(p, "Hours without hovering");
            p.waitForCondition(() -> (Boolean) horizontal.evaluate(CLEAR_LABELS));
            // Collision handling may hide the first label on a narrow plot. Check the visible direction.
            assertThat(horizontal.locator("[data-label-dir=rtl] text:visible").first()).isVisible();
            assertThat(horizontal.locator("[data-label-dir=ltr] text").first()).isHidden();
            assertThat(p.locator(".sw-sparkline").first().locator(".sw-sparkline-last").evaluate("n=>{const dot=n.getBoundingClientRect(),root=n.closest('.sw-sparkline').getBoundingClientRect();return dot.left<root.left+10;}")).isEqualTo(true);
            dense.evaluate("n=>n.dataset.untouched='true'"); List<String> navigations = new ArrayList<>();
            String oldTrend = p.locator(".sw-sparkline").first().locator(".sw-sparkline-line").getAttribute("points");
            p.onRequest(request -> { if (request.isNavigationRequest()) navigations.add(request.url()); });
            p.getByText("Refresh report", new Page.GetByTextOptions().setExact(true)).click();
            assertThat(p.locator(".ex-spark-metric").first()).containsText("96 h");
            assertThat(p.locator(".sw-sparkline").first().locator(".sw-sparkline-line")).not().hasAttribute("points", oldTrend);
            var live = chart(p, "Live report");
            assertThat(live.locator("[data-label-dir=rtl] text")).hasText("280");
            assertThat(live.locator(".sw-chart-plot")).hasAttribute("data-labels-enhanced", "true");
            assertThat(dense).hasAttribute("data-untouched", "true"); assertThat(navigations).isEmpty();
            live.evaluate("n=>n.replaceWith(n.cloneNode(true))"); p.evaluate("()=>{Shadwick.enhanceCharts();Shadwick.enhanceCharts();}");
            assertThat(live.locator("[data-chart-status]")).hasCount(1);
            live.locator(".sw-chart-plot").focus(); live.locator(".sw-chart-plot").press("End");
            assertThat(live.locator(".sw-chart-tooltip-content")).containsText("280");
            p.emulateMedia(new Page.EmulateMediaOptions().setForcedColors(ForcedColors.ACTIVE));
            assertThat(p.locator(".sw-sparkline").first().locator(".sw-sparkline-line")).hasCSS("stroke-width", "1.5px");
            assertThat(live.locator("[data-label-dir=rtl] text").evaluate("n=>{const s=getComputedStyle(n);return s.fill!==s.stroke;}")).isEqualTo(true);
            p.emulateMedia(new Page.EmulateMediaOptions().setForcedColors(ForcedColors.NONE)); audit(p); assertThat(errors).isEmpty();
            p.locator(".ex-spark-section").screenshot(new Locator.ScreenshotOptions().setPath(SHOTS.resolve(engine + "-final-rtl-trends.png")));
            p.locator("section.ex-section").filter(new Locator.FilterOptions().setHasText("The values that matter"))
                    .screenshot(new Locator.ScreenshotOptions().setPath(SHOTS.resolve(engine + "-final-rtl-labels.png")));
        } finally { settings.setBundle(Bundle.STANDALONE); }
    }
    @ParameterizedTest @ValueSource(strings={"chromium", "firefox"})
    void nativeTrendsAndSelectedValuesNeedNoJavascript(String engine) {
        try (var context = browser(engine).newContext(new Browser.NewContextOptions().setViewportSize(320, 950).setJavaScriptEnabled(false)); var p = context.newPage()) {
            p.navigate(url("vega", "light")); p.locator("main").evaluate("n=>n.dir='rtl'");
            assertThat(p.locator(".sw-sparkline").first().locator("svg")).hasCount(2);
            assertThat(p.locator(".sw-sparkline").first()).hasAttribute("aria-hidden", "true");
            assertThat(chart(p, "Highest and latest months").locator("[data-label-dir=rtl] text").first()).isVisible();
            assertThat(chart(p, "Hours without hovering").locator("[data-label-dir=rtl] text")).hasCount(4);
            var dense = chart(p, "Labels that make room");
            assertThat(dense.locator("[data-label-dir=rtl] text:visible")).hasCount(2);
            dense.locator("summary").click(); assertThat(dense.locator("tbody tr")).hasCount(24);
            assertThat(p.evaluate("document.documentElement.scrollWidth <= innerWidth + 1")).isEqualTo(true);
        }
    }
}
