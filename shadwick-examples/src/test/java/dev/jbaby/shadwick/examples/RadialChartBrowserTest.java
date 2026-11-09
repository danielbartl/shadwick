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
class RadialChartBrowserTest {
    static Playwright playwright;
    static Browser chromium, firefox;
    static final Path SHOTS = Path.of("target/radial-chart-screenshots");
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
        return page.locator(".sw-radial-chart").filter(new Locator.FilterOptions()
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
    static Stream<Arguments> appearances() {
        return Stream.of("chromium", "firefox").flatMap(engine -> Arrays.stream(Skin.values()).flatMap(skin ->
                Stream.of(Arguments.of(engine, skin.id(), "light", 1440), Arguments.of(engine, skin.id(), "dark", 390))));
    }

    void hoverRing(Page page, Locator radial, int index) {
        radial.locator(".sw-radial-plot").scrollIntoViewIfNeeded();
        var position = (Map<?, ?>) radial.locator(".sw-radial-hit[data-index='" + index + "']").evaluate("""
            path=>{const point=path.getPointAtLength(path.getTotalLength()/4);
              const screen=new DOMPoint(point.x,point.y).matrixTransform(path.getScreenCTM());return {x:screen.x,y:screen.y};}
            """);
        page.mouse().move(((Number) position.get("x")).doubleValue(), ((Number) position.get("y")).doubleValue());
    }

    @ParameterizedTest @MethodSource("appearances")
    void sharedScalesFullAndPartialArcsFitEverySkin(String engine, String skin, String mode, int width) {
        try (var context = browser(engine).newContext(new Browser.NewContextOptions().setViewportSize(width, 1000)
                .setReducedMotion(ReducedMotion.REDUCE)); var page = context.newPage()) {
            var errors = errors(page);
            page.navigate(url(skin, mode));
            var radial = chart(page, "Delivery goals");
            assertThat(radial.locator(".sw-radial-bar")).hasCount(4);
            assertThat(radial.locator(".sw-radial-track")).hasCount(4);
            assertThat(radial.locator(".sw-radial-hit")).hasCount(4);
            assertThat(radial.locator(".sw-radial-name")).hasText(new String[]{"Delivery", "Quality", "Planning", "Research"});
            assertThat(radial.locator(".sw-radial-value")).hasText(new String[]{"78", "92", "64", "86"});
            assertThat(radial.locator(".sw-radial-scale")).hasText("Scale: 0 – 100");
            var plot = radial.locator(".sw-radial-plot");
            assertThat(plot.boundingBox().height).isCloseTo(plot.boundingBox().width, within(1.0));
            radial.locator("[data-chart-index='1']").hover();
            assertThat(radial.locator(".sw-chart-tooltip-label")).hasText("Quality");
            assertThat(radial.locator(".sw-chart-tooltip-value")).hasText("92");
            assertThat(radial.locator(".sw-chart-tooltip-item")).hasClass("sw-chart-tooltip-item sw-chart-series-2");
            hoverRing(page, radial, 0);
            assertThat(radial.locator(".sw-chart-tooltip-label")).hasText("Delivery");
            var sweep = chart(page, "A three-quarter comparison");
            assertThat(sweep.locator(".sw-radial-bar").first()).hasAttribute("stroke-linecap", "round");
            assertThat(chart(page, "Monthly support hours").locator(".sw-radial-scale")).hasText("Scale: 0 – 100");
            var mixed = chart(page, "Measured and pending");
            assertThat(mixed.locator(".sw-radial-bar")).hasCount(2);
            assertThat(mixed.locator(".sw-radial-hit")).hasCount(4);
            mixed.locator("[data-chart-index='1']").hover();
            assertThat(mixed.locator(".sw-chart-tooltip-value")).hasText("0");
            mixed.locator("[data-chart-index='2']").hover();
            assertThat(mixed.locator(".sw-chart-tooltip-value")).hasText("Not measured");
            mixed.locator("[data-chart-index='3']").hover();
            assertThat(mixed.locator(".sw-chart-tooltip-value")).hasText("1E-30");
            mixed.locator("summary").click();
            assertThat(mixed.locator("tbody tr")).hasCount(4);
            assertThat(mixed.locator("tbody tr").nth(2).locator("td")).isEmpty();
            mixed.locator("summary").click();
            assertThat(chart(page, "One launch target").locator(".sw-radial-bar")).hasCount(1);
            var zero = chart(page, "A measured zero report");
            assertThat(zero.locator(".sw-radial-bar")).hasCount(0);
            assertThat(zero.locator(".sw-radial-track")).hasCount(2);
            assertThat(zero.locator(".sw-radial-scale")).hasText("Scale: 0 – 1");
            assertThat(chart(page, "Waiting for measurements").locator("[role=status]")).containsText("No measurements");
            assertThat(chart(page, "The next radial report").locator("[role=status]")).containsText("No data");
            var quiet = chart(page, "A quiet half-circle");
            assertThat(quiet.locator(".sw-radial-plot")).not().hasAttribute("tabindex", "0");
            assertThat(quiet.locator(".sw-radial-legend")).hasCount(0);
            assertThat(quiet.locator(".sw-radial-hit title")).hasCount(4);
            audit(page);
            page.mouse().move(0, 0);
            page.locator("section.ex-section").filter(new Locator.FilterOptions().setHasText("Each ring, the same scale"))
                    .screenshot(new Locator.ScreenshotOptions().setPath(SHOTS.resolve(engine + "-" + skin + "-" + mode + ".png")));
            assertThat(errors).isEmpty();
        }
    }

    @ParameterizedTest @ValueSource(strings={"chromium", "firefox"})
    void keyboardTouchRtlResizeAndAjaxKeepEveryRingReachable(String engine) {
        var settings = ShadwickSettings.get(app);
        settings.setBundle(Bundle.EMBEDDED);
        try (var context = browser(engine).newContext(new Browser.NewContextOptions().setViewportSize(320, 950)
                .setHasTouch(true).setReducedMotion(ReducedMotion.REDUCE)); var page = context.newPage()) {
            var errors = errors(page);
            page.navigate(url("nova", "dark"));
            page.locator("main").evaluate("n=>n.dir='rtl'");
            var radial = chart(page, "Delivery goals");
            var plot = radial.locator(".sw-radial-plot");
            plot.focus();
            assertThat(radial.locator(".sw-chart-tooltip-label")).hasText("Delivery");
            plot.press("ArrowDown");
            assertThat(radial.locator(".sw-chart-tooltip-label")).hasText("Quality");
            plot.press("ArrowUp");
            assertThat(radial.locator(".sw-chart-tooltip-label")).hasText("Delivery");
            plot.press("ArrowLeft");
            assertThat(radial.locator(".sw-chart-tooltip-label")).hasText("Quality");
            plot.press("End");
            assertThat(radial.locator(".sw-chart-tooltip-label")).hasText("Research");
            assertThat(radial.locator(".sw-radial-svg").evaluate("n=>getComputedStyle(n).transform")).isNotEqualTo("none");
            plot.press("Escape");
            assertThat(radial.locator(".sw-chart-tooltip-content")).isHidden();
            assertThat(plot).isFocused();
            radial.locator("[data-chart-index='2']").tap();
            assertThat(radial.locator(".sw-chart-tooltip-label")).hasText("Planning");
            assertThat(radial.locator(".sw-chart-tooltip-value")).hasText("64");
            assertThat(radial.locator("[data-chart-index][data-active]")).hasAttribute("data-chart-index", "2");
            assertThat(radial.locator("[tabindex='0']")).hasCount(2);
            hoverRing(page, radial, 0);
            assertThat(radial.locator(".sw-chart-tooltip-label")).hasText("Delivery");
            var mixed = chart(page, "Measured and pending");
            mixed.locator(".sw-radial-plot").focus();
            mixed.locator(".sw-radial-plot").press("End");
            assertThat(mixed.locator(".sw-chart-tooltip-value")).hasText("1E-30");
            mixed.locator(".sw-radial-plot").press("ArrowUp");
            assertThat(mixed.locator(".sw-chart-tooltip-value")).hasText("Not measured");
            var arc = radial.locator(".sw-radial-bar").first().getAttribute("d");
            page.setViewportSize(700, 950);
            assertThat(radial.locator(".sw-radial-bar").first()).hasAttribute("d", arc);
            assertThat(plot.boundingBox().height).isCloseTo(plot.boundingBox().width, within(1.0));
            radial.evaluate("n=>n.dataset.untouched='true'");
            List<String> navigations = new ArrayList<>();
            page.onRequest(request -> { if (request.isNavigationRequest()) navigations.add(request.url()); });
            page.getByText("Refresh radial report", new Page.GetByTextOptions().setExact(true)).click();
            var live = chart(page, "A changing delivery report");
            assertThat(live.locator(".sw-radial-hit")).hasCount(3);
            assertThat(live.locator("[data-chart-index]")).hasCount(3);
            assertThat(live.locator(".sw-radial-scale")).hasText("Scale: 0 – 120");
            assertThat(live.locator("[data-ring='0']")).hasAttribute("class", "sw-chart-series-3");
            assertThat(live.locator(".sw-radial-bar").first()).hasAttribute("stroke-linecap", "round");
            live.locator(".sw-radial-plot").focus();
            live.locator(".sw-radial-plot").press("Home");
            assertThat(live.locator(".sw-chart-tooltip-name")).hasText("Latest");
            assertThat(live.locator(".sw-chart-tooltip-value")).hasText("96");
            assertThat(live.locator(".sw-chart-tooltip-item")).hasClass("sw-chart-tooltip-item sw-chart-series-3");
            assertThat(radial).hasAttribute("data-untouched", "true");
            assertThat(navigations).isEmpty();
            live.evaluate("n=>n.replaceWith(n.cloneNode(true))");
            page.evaluate("()=>{Shadwick.enhanceCharts();Shadwick.enhanceCharts();}");
            live.locator("[data-chart-index='2']").tap();
            assertThat(live.locator(".sw-chart-tooltip-label")).hasText("Planning");
            assertThat(live.locator(".sw-chart-tooltip-value")).hasText("90");
            assertThat(live.locator("[data-chart-status]")).hasCount(1);
            page.emulateMedia(new Page.EmulateMediaOptions().setForcedColors(ForcedColors.ACTIVE));
            live.locator(".sw-radial-plot").press("Tab"); page.keyboard().press("Shift+Tab");
            assertThat(live.locator(".sw-radial-plot")).hasCSS("outline-style", "solid");
            page.emulateMedia(new Page.EmulateMediaOptions().setForcedColors(ForcedColors.NONE));
            audit(page);
            assertThat(errors).isEmpty();
        } finally { settings.setBundle(Bundle.STANDALONE); }
    }

    @ParameterizedTest @ValueSource(strings={"chromium", "firefox"})
    void localizedTitlesScalesAndOriginalValuesRemainNative(String engine) {
        try (var context = browser(engine).newContext(new Browser.NewContextOptions().setViewportSize(320, 950)
                .setJavaScriptEnabled(false)); var page = context.newPage()) {
            page.navigate(url("vega", "light") + "&lang=de");
            page.locator("main").evaluate("n=>n.dir='rtl'");
            var radial = chart(page, "Delivery goals");
            assertThat(radial.locator(".sw-radial-hit title")).hasCount(4);
            assertThat(radial.locator(".sw-radial-scale")).hasText("Skala: 0 – 100");
            assertThat(radial.locator(".sw-radial-legend")).hasAttribute("aria-label", "Kategorien");
            assertThat(radial.locator(".sw-radial-plot")).not().hasAttribute("tabindex", "0");
            assertThat(radial.locator("[data-chart-key]")).not().hasAttribute("data-chart-key-enhanced", "true");
            radial.locator("summary").click();
            assertThat(radial.locator("thead")).containsText("Kategorie");
            assertThat(radial.locator("tbody tr")).hasCount(4);
            assertThat(radial.locator("tbody tr").first().locator("td")).hasText("78");
            var mixed = chart(page, "Measured and pending");
            assertThat(mixed.locator(".sw-radial-hit[data-index='2'] title")).containsText("Nicht gemessen");
            mixed.locator("summary").click();
            assertThat(mixed.locator("tbody tr").nth(2).locator("td")).isEmpty();
            assertThat(mixed.locator("tbody tr").last().locator("td")).hasText("1E-30");
            assertThat(chart(page, "Waiting for measurements").locator("[role=status]")).hasText("Keine Messwerte vorhanden.");
            assertThat(chart(page, "Waiting for measurements").locator("tbody tr")).hasCount(2);
            assertThat(chart(page, "The next radial report").locator("[role=status]")).hasText("Keine Daten vorhanden.");
            assertThat(page.evaluate("document.documentElement.scrollWidth <= innerWidth + 1")).isEqualTo(true);
        }
    }
}
