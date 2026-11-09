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
class ReferenceChartBrowserTest {
    static Playwright playwright;
    static Browser chromium, firefox;
    static final Path SHOTS = Path.of("target/reference-chart-screenshots");
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
    static Stream<Arguments> appearances() {
        return Stream.of("chromium", "firefox").flatMap(engine -> Arrays.stream(Skin.values()).flatMap(skin ->
                Stream.of(Arguments.of(engine, skin.id(), "light", 1440), Arguments.of(engine, skin.id(), "dark", 390))));
    }

    @ParameterizedTest @MethodSource("appearances")
    void referenceLinesBandsAndCaptionsFitEverySkin(String engine, String skin, String mode, int width) {
        try (var context = browser(engine).newContext(new Browser.NewContextOptions().setViewportSize(width, 1000)
                .setReducedMotion(ReducedMotion.REDUCE)); var page = context.newPage()) {
            var errors = errors(page);
            page.navigate(url(skin, mode));
            var revenue = chart(page, "Revenue against a target");
            assertThat(revenue.locator(".sw-chart-reference-line")).hasCount(1);
            assertThat(revenue.locator(".sw-chart-reference-line")).hasAttribute("y1", "30");
            assertThat(revenue.locator(".sw-chart-reference-band")).hasCount(1);
            assertThat(revenue.locator(".sw-chart-reference-value")).hasText(new String[]{"€150.00 – €250.00", "€350.00"});
            assertThat(revenue.locator(".sw-chart-references")).hasAttribute("aria-label", "References");
            assertThat(revenue.locator(".sw-chart-reference-overlay")).hasCSS("pointer-events", "none");
            assertThat(revenue.locator(".sw-chart-plot").getAttribute("aria-describedby"))
                    .contains(revenue.locator(".sw-chart-references").getAttribute("id"));
            var hours = chart(page, "Hours with a delivery goal");
            assertThat(hours.locator(".sw-chart-reference-line")).hasCount(4);
            assertThat(hours.locator(".sw-chart-reference-line").first()).hasAttribute("x1", "700");
            assertThat(hours.locator(".sw-chart-reference-line").first()).not().hasAttribute("stroke-dasharray", "5 5");
            var hit = hours.locator(".sw-chart-hit[data-index='0']");
            hit.scrollIntoViewIfNeeded();
            var box = hit.boundingBox();
            page.mouse().move(box.x + box.width * .7, box.y + box.height / 2);
            assertThat(hours.locator(".sw-chart-tooltip-content")).containsText("Northstar");
            var percent = chart(page, "A balanced device mix");
            assertThat(percent.locator(".sw-chart-reference-line")).hasAttribute("y1", "120");
            assertThat(percent.locator(".sw-chart-reference-band")).hasAttribute("height", "48");
            assertThat(percent.locator(".sw-chart-reference-value")).hasText(new String[]{"40% – 60%", "50%"});
            audit(page);
            page.locator("section.ex-section").filter(new Locator.FilterOptions().setHasText("Give the numbers a reference"))
                    .screenshot(new Locator.ScreenshotOptions().setPath(SHOTS.resolve(engine + "-" + skin + "-" + mode + ".png")));
            assertThat(errors).isEmpty();
        }
    }

    @ParameterizedTest @ValueSource(strings={"chromium", "firefox"})
    void keyboardTouchRtlAndAjaxUpdateGuidesWithoutChangingOtherCharts(String engine) {
        var settings = ShadwickSettings.get(app);
        settings.setBundle(Bundle.EMBEDDED);
        try (var context = browser(engine).newContext(new Browser.NewContextOptions().setViewportSize(320, 950)
                .setHasTouch(true).setReducedMotion(ReducedMotion.REDUCE)); var page = context.newPage()) {
            var errors = errors(page);
            page.navigate(url("nova", "dark"));
            page.locator("main").evaluate("n=>n.dir='rtl'");
            var staticChart = chart(page, "Revenue against a target");
            staticChart.evaluate("n=>n.dataset.untouched='true'");
            var hours = chart(page, "Hours with a delivery goal");
            hours.locator(".sw-chart-hit[data-index='1']").tap();
            assertThat(hours.locator(".sw-chart-tooltip-content")).containsText("Harbor");
            hours.locator(".sw-chart-plot").press("ArrowDown");
            assertThat(hours.locator(".sw-chart-tooltip-content")).containsText("Clover");
            assertThat(hours.locator(".sw-chart-reference-overlay").first().evaluate("n=>getComputedStyle(n).transform")).isNotEqualTo("none");
            List<String> navigations = new ArrayList<>();
            page.onRequest(request -> { if (request.isNavigationRequest()) navigations.add(request.url()); });
            page.getByText("Refresh targets", new Page.GetByTextOptions().setExact(true)).click();
            for (String title : List.of("An updated revenue target", "An updated revenue target by row")) {
                var live = chart(page, title);
                assertThat(live.locator(".sw-chart-reference-value")).hasText(new String[]{"€350.00", "€200.00 – €300.00"});
                assertThat(live.locator(".sw-chart-hit")).hasCount(3);
                var plot = live.locator(".sw-chart-plot");
                plot.focus(); plot.press("Home");
                assertThat(live.locator(".sw-chart-tooltip-content")).containsText("July");
                assertThat(live.locator(".sw-chart-tooltip-value")).hasText("€250.00");
                plot.press("Escape");
                assertThat(live.locator(".sw-chart-tooltip-content")).isHidden();
                assertThat(plot).isFocused();
            }
            var vertical = chart(page, "An updated revenue target");
            assertThat(vertical.locator(".sw-chart-reference-line")).hasAttribute("y1", "30");
            var horizontal = chart(page, "An updated revenue target by row");
            assertThat(horizontal.locator(".sw-chart-reference-line").first()).hasAttribute("x1", "875");
            assertThat(staticChart).hasAttribute("data-untouched", "true");
            assertThat(navigations).isEmpty();
            vertical.evaluate("n=>n.replaceWith(n.cloneNode(true))");
            page.evaluate("()=>{Shadwick.enhanceCharts();Shadwick.enhanceCharts();}");
            vertical.locator(".sw-chart-plot").focus();
            assertThat(vertical.locator("[data-chart-status]")).hasCount(1);
            page.emulateMedia(new Page.EmulateMediaOptions().setForcedColors(ForcedColors.ACTIVE));
            vertical.locator(".sw-chart-plot").press("Tab"); page.keyboard().press("Shift+Tab");
            assertThat(vertical.locator(".sw-chart-plot")).hasCSS("outline-style", "solid");
            page.emulateMedia(new Page.EmulateMediaOptions().setForcedColors(ForcedColors.NONE));
            audit(page);
            assertThat(errors).isEmpty();
        } finally { settings.setBundle(Bundle.STANDALONE); }
    }

    @ParameterizedTest @ValueSource(strings={"chromium", "firefox"})
    void referencesAndOriginalDataRemainReadableWithoutJavaScript(String engine) {
        try (var context = browser(engine).newContext(new Browser.NewContextOptions().setViewportSize(320, 950)
                .setJavaScriptEnabled(false)); var page = context.newPage()) {
            page.navigate(url("vega", "light") + "&lang=de");
            page.locator("main").evaluate("n=>n.dir='rtl'");
            var revenue = chart(page, "Revenue against a target");
            assertThat(revenue.locator(".sw-chart-references")).hasAttribute("aria-label", "Referenzwerte");
            assertThat(revenue.locator(".sw-chart-reference-value").last().textContent()).contains("350,00", "€");
            assertThat(revenue.locator(".sw-chart-reference-line")).hasAttribute("y1", "30");
            assertThat(revenue.locator(".sw-chart-plot")).not().hasAttribute("tabindex", "0");
            revenue.locator("summary").click();
            assertThat(revenue.locator("tbody tr")).hasCount(6);
            assertThat(revenue.locator("tbody tr").first().locator("td")).containsText("186,00");
            assertThat(revenue.locator("thead th")).hasCount(2);
            assertThat(chart(page, "Hours with a delivery goal").locator(".sw-chart-reference-line")).hasCount(4);
            assertThat(chart(page, "A balanced device mix").locator(".sw-chart-reference-value").last().textContent().replace('\u00a0', ' ')).isEqualTo("50 %");
            assertThat(page.evaluate("document.documentElement.scrollWidth <= innerWidth + 1")).isEqualTo(true);
        }
    }
}
