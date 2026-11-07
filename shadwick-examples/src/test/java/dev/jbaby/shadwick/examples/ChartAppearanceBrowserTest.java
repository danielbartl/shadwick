package dev.jbaby.shadwick.examples;

import com.deque.html.axecore.playwright.AxeBuilder;
import com.microsoft.playwright.*;
import com.microsoft.playwright.options.*;
import dev.jbaby.shadwick.*;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import javax.imageio.ImageIO;
import java.nio.file.*;
import java.util.*;
import java.util.stream.Stream;
import org.apache.wicket.protocol.http.WebApplication;
import org.junit.jupiter.api.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import static com.microsoft.playwright.assertions.PlaywrightAssertions.assertThat;
import static org.assertj.core.api.Assertions.assertThat;

@Tag("browser")
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class ChartAppearanceBrowserTest {
    static Playwright playwright;
    static Browser chromium, firefox;
    static final Path SHOTS = Path.of("target/chart-appearance-screenshots");
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
    String url(String skin, String mode) { return "http://localhost:" + port + "/charts?skin=" + skin + "&mode=" + mode + "&lang=en"; }
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
    void corners(Locator bar, int color, boolean leftRounded, boolean rightRounded) throws Exception {
        BufferedImage pixels = ImageIO.read(new ByteArrayInputStream(bar.screenshot(new Locator.ScreenshotOptions()
                .setPath(SHOTS.resolve("corners-" + Integer.toHexString(color) + "-" + leftRounded + "-" + rightRounded + ".png")))));
        int left = pixels.getRGB(2, 2) & 0xffffff, right = pixels.getRGB(pixels.getWidth() - 3, 2) & 0xffffff;
        assertThat(left == color).as("Physical top-left corner: pixel #%s, %s", Integer.toHexString(left), bar.evaluate("""
                n=>({fill:getComputedStyle(n).fill,rootRtl:n.closest('.sw-chart-native-bars').matches(':dir(rtl)'),corners:[...n.parentNode.querySelectorAll('.sw-chart-corner')].map(c=>({corner:c.dataset.corner,rtl:c.matches(':dir(rtl)'),r:getComputedStyle(c).getPropertyValue('--r'),x:getComputedStyle(c).x}))})
                """)).isEqualTo(!leftRounded);
        assertThat(right == color).as("Physical top-right corner").isEqualTo(!rightRounded);
        assertThat(pixels.getRGB(pixels.getWidth() / 2, pixels.getHeight() / 2) & 0xffffff).isEqualTo(color);
    }
    static final String CLEAR_TICKS = """
        root=>{
            const axis=root.querySelector('.sw-chart-x-axis'),bounds=axis.getBoundingClientRect();
            const boxes=[...axis.children].filter(n=>getComputedStyle(n).visibility!=='hidden').map(n=>n.firstElementChild.getBoundingClientRect()).filter(r=>r.width>0).sort((a,b)=>a.left-b.left);
            return boxes.length>=2 && boxes.length<=6 && boxes.every((r,i)=>r.left>=bounds.left-1 && r.right<=bounds.right+1 && (!i || r.left>=boxes[i-1].right+11));
        }
        """;
    static Stream<Arguments> appearances() {
        return Stream.of("chromium", "firefox").flatMap(engine -> Stream.of("vega", "nova", "sketch").flatMap(skin ->
                Stream.of(Arguments.of(engine, skin, "light", 1440), Arguments.of(engine, skin, "dark", 390))));
    }
    @ParameterizedTest @MethodSource("appearances")
    void brandColorsNaturalCurvesAndNativePixelCornersFitTheSkins(String engine, String skin, String mode, int width) throws Exception {
        try (var context = browser(engine).newContext(new Browser.NewContextOptions().setViewportSize(width, 1000)
                .setReducedMotion(ReducedMotion.REDUCE)); var p = context.newPage()) {
            var errors = errors(p); p.navigate(url(skin, mode));
            var brand = chart(p, "A forecast in your colors");
            assertThat(brand.locator(".sw-chart-line").first()).hasCSS("stroke", mode.equals("dark") ? "rgb(52, 211, 153)" : "rgb(4, 120, 87)");
            assertThat(brand.locator(".sw-chart-line").first()).hasAttribute("stroke-width", "3");
            assertThat(brand.locator(".sw-chart-line").first()).hasCSS("stroke-width", "3px");
            assertThat(brand.locator(".sw-chart-line").first().getAttribute("d")).contains(" C");
            assertThat(brand.locator(".sw-chart-line").first().getAttribute("clip-path")).startsWith("url(#");
            assertThat(brand.locator(".sw-chart-legend-content")).hasAttribute("data-position", "top");
            assertThat(brand.locator(".sw-chart-legend-content").boundingBox().y).isLessThan(brand.locator(".sw-chart-plot").boundingBox().y);
            assertThat(p.locator("style[id^='sw-chart-custom-']").first().evaluate("n=>Boolean(n.nonce)")).isEqualTo(true);
            assertThat(p.locator("style[id^='sw-chart-custom-']").evaluateAll("nodes=>new Set(nodes.map(n=>n.id)).size===nodes.length")).isEqualTo(true);
            assertThat(p.locator(".sw-chart-native-bars mask[id]").evaluateAll("nodes=>new Set(nodes.map(n=>n.id)).size===nodes.length")).isEqualTo(true);
            var legacy = chart(p, "Steady progress").locator(".sw-chart-line").first();
            legacy.evaluate("n=>n.removeAttribute('stroke-width')");
            assertThat(legacy).hasCSS("stroke-width", "2px");
            chart(p, "Steady progress").evaluate("""
                root=>{
                    const clone=root.cloneNode(true),axis=clone.querySelector('.sw-chart-x-axis');
                    ['data-max-ticks','data-tick-gap','data-axis-enhanced'].forEach(name=>axis.removeAttribute(name));
                    [...axis.children].forEach(tick=>{tick.removeAttribute('data-tick-hidden');tick.replaceChildren(document.createTextNode(tick.textContent));});
                    root.replaceWith(clone);Shadwick.enhanceCharts();
                }
                """);
            var legacyChart = chart(p, "Steady progress");
            legacyChart.locator(".sw-chart-plot").focus(); legacyChart.locator(".sw-chart-plot").press("End");
            assertThat(legacyChart.locator(".sw-chart-tooltip-content")).containsText("June");
            assertThat(legacyChart.locator(".sw-chart-x-axis")).not().hasAttribute("data-axis-enhanced", "true");
            legacyChart.locator(".sw-chart-plot").press("Escape");
            var rounded = chart(p, "A softer monthly report");
            assertThat(rounded.locator(".sw-chart-native-bars .sw-chart-bar:visible")).hasCount(6);
            var first = rounded.locator(".sw-chart-native-bars .sw-chart-bar:visible").first();
            corners(first, mode.equals("dark") ? 0xa5b4fc : 0x4338ca, true, true);
            rounded.locator(".sw-chart-hit").first().hover();
            assertThat(rounded.locator(".sw-chart-tooltip-indicator")).hasAttribute("data-indicator", "line");
            assertThat(rounded.locator(".sw-chart-tooltip-value")).hasText("186");
            var horizontal = chart(p, "Rounded ends, exact totals"); horizontal.locator(".sw-chart-hit").first().hover();
            assertThat(horizontal.locator(".sw-chart-tooltip-indicator").first()).hasAttribute("data-indicator", "dashed");
            var activity = chart(p, "Daily studio activity");
            assertThat(activity.locator(".sw-chart-x-axis")).hasAttribute("data-axis-enhanced", "true");
            p.waitForCondition(() -> (Boolean) activity.evaluate(CLEAR_TICKS));
            activity.locator(".sw-chart-plot").focus();
            assertThat(activity.locator(".sw-chart-tooltip-label,.sw-chart-tooltip-indicator")).hasCount(0);
            assertThat(activity.locator("[data-chart-status]")).containsText("Nov 1, 2026; Activity: 24");
            assertThat(activity.locator(".sw-chart-hit[data-active]")).hasCSS("fill", "rgba(0, 0, 0, 0)");
            activity.locator(".sw-chart-plot").press("Escape");
            rounded.locator("summary").click(); audit(p); rounded.locator("summary").click();
            assertThat(p.locator(".sw-chart [style]")).hasCount(0); assertThat(errors).isEmpty();
            p.locator("section.ex-section").filter(new Locator.FilterOptions().setHasText("Make the report your own"))
                    .screenshot(new Locator.ScreenshotOptions().setPath(SHOTS.resolve(engine + "-" + skin + "-" + mode + ".png")));
        }
    }
    @ParameterizedTest @ValueSource(strings={"chromium", "firefox"})
    void themesRtlResizeAndPartialAjaxKeepColorsCornersAndObserversCurrent(String engine) throws Exception {
        var settings = ShadwickSettings.get(app); settings.setBundle(Bundle.EMBEDDED);
        try (var context = browser(engine).newContext(new Browser.NewContextOptions().setViewportSize(320, 950)
                .setHasTouch(true).setReducedMotion(ReducedMotion.REDUCE)); var p = context.newPage()) {
            var errors = errors(p); p.navigate(url("nova", "dark")); p.locator("main").evaluate("n=>n.dir='rtl'");
            var live = chart(p, "An adaptable report");
            var first = live.locator(".sw-chart-native-bars .sw-chart-bar:visible").first();
            corners(first, 0x34d399, false, true);
            p.locator("html").evaluate("n=>n.classList.remove('dark')");
            assertThat(first).hasCSS("fill", "rgb(4, 120, 87)");
            p.locator("html").evaluate("n=>n.classList.add('dark')");
            assertThat(first).hasCSS("fill", "rgb(52, 211, 153)");
            var activity = chart(p, "Daily studio activity"); p.waitForCondition(() -> (Boolean) activity.evaluate(CLEAR_TICKS));
            p.setViewportSize(1440, 1000); p.waitForCondition(() -> (Boolean) activity.evaluate(CLEAR_TICKS));
            p.setViewportSize(320, 950); p.waitForCondition(() -> (Boolean) activity.evaluate(CLEAR_TICKS));
            activity.evaluate("n=>n.dataset.untouched='true'");
            List<String> navigation = new ArrayList<>(); p.onRequest(request -> { if (request.isNavigationRequest()) navigation.add(request.url()); });
            int styles = p.locator("style[id^='sw-chart-custom-']").count();
            p.getByText("Refresh report", new Page.GetByTextOptions().setExact(true)).click();
            assertThat(live.locator("tbody tr")).hasCount(3); first = live.locator(".sw-chart-native-bars .sw-chart-bar:visible").first();
            assertThat(first).hasCSS("fill", "rgb(196, 181, 253)"); corners(first, 0xc4b5fd, true, false);
            assertThat(p.locator("style[id^='sw-chart-custom-']")).hasCount(styles + 1);
            p.getByText("Refresh report", new Page.GetByTextOptions().setExact(true)).click();
            assertThat(live.locator(".sw-chart-native-bars svg[data-series]:visible").first()).hasClass(java.util.regex.Pattern.compile(".*sw-chart-series-3.*"));
            assertThat(first.evaluate("n=>getComputedStyle(n).getPropertyValue('--sw-series-color').trim()===getComputedStyle(n).getPropertyValue('--chart-3').trim()")).isEqualTo(true);
            int finalStyles = p.locator("style[id^='sw-chart-custom-']").count();
            p.getByText("Refresh report", new Page.GetByTextOptions().setExact(true)).click();
            assertThat(p.locator("style[id^='sw-chart-custom-']")).hasCount(finalStyles);
            assertThat(activity).hasAttribute("data-untouched", "true"); assertThat(navigation).isEmpty();
            activity.evaluate("""
                root=>{
                    const clone=root.cloneNode(true),data=clone.querySelector('.sw-chart-data');data.remove();clone.pendingData=data;
                    delete clone.querySelector('.sw-chart-x-axis').dataset.axisEnhanced;
                    clone.querySelectorAll('[data-tick-hidden],[data-chart-status]').forEach(n=>n.hasAttribute('data-chart-status')?n.remove():n.removeAttribute('data-tick-hidden'));
                    clone.querySelector('.sw-chart-plot').dataset.tooltip='false';root.replaceWith(clone);Shadwick.enhanceCharts();
                }
                """);
            assertThat(activity.locator(".sw-chart-x-axis")).not().hasAttribute("data-axis-enhanced", "true");
            activity.evaluate("root=>{root.append(root.pendingData);delete root.pendingData;Shadwick.enhanceCharts();Shadwick.enhanceCharts();}");
            assertThat(activity.locator(".sw-chart-x-axis")).hasAttribute("data-axis-enhanced", "true");
            p.waitForCondition(() -> (Boolean) activity.evaluate(CLEAR_TICKS));
            assertThat(activity.locator("[data-chart-status]")).hasCount(0);
            p.emulateMedia(new Page.EmulateMediaOptions().setForcedColors(ForcedColors.ACTIVE));
            assertThat(live.locator(".sw-chart-native-bars").first()).hasCSS("forced-color-adjust", "none");
            p.emulateMedia(new Page.EmulateMediaOptions().setForcedColors(ForcedColors.NONE)); audit(p); assertThat(errors).isEmpty();
        } finally { settings.setBundle(Bundle.STANDALONE); }
    }
    @ParameterizedTest @ValueSource(strings={"chromium", "firefox"})
    void colorsAndPixelCornersRemainNativeWithoutScripts(String engine) throws Exception {
        try (var context = browser(engine).newContext(new Browser.NewContextOptions().setViewportSize(320, 950).setJavaScriptEnabled(false)); var p = context.newPage()) {
            var errors = errors(p); p.navigate(url("nova", "dark")); p.locator("main").evaluate("n=>n.dir='rtl'");
            var live = chart(p, "An adaptable report"); corners(live.locator(".sw-chart-native-bars .sw-chart-bar:visible").first(), 0x34d399, false, true);
            assertThat(chart(p, "A forecast in your colors").locator(".sw-chart-line").first()).hasCSS("stroke", "rgb(52, 211, 153)");
            live.locator("summary").click(); assertThat(live.locator("table")).isVisible();
            assertThat(live.locator("tbody tr").first()).containsText("186");
            assertThat(live.locator(".sw-chart-hit").first().locator("title")).containsText("January; Delivered: 186");
            assertThat(p.locator(".sw-chart [style]")).hasCount(0); assertThat(errors).isEmpty();
            assertThat(p.evaluate("document.documentElement.scrollWidth <= innerWidth + 1")).isEqualTo(true);
        }
    }
}
