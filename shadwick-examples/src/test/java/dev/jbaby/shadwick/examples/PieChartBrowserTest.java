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
class PieChartBrowserTest {
    static Playwright playwright;
    static Browser chromium, firefox;
    static final Path SHOTS = Path.of("target/pie-chart-screenshots");
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
        return page.locator(".sw-donut-chart").filter(new Locator.FilterOptions()
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
    void filledPiesAndExactSharesFitEverySkin(String engine, String skin, String mode, int width) {
        try (var context = browser(engine).newContext(new Browser.NewContextOptions().setViewportSize(width, 1000)
                .setReducedMotion(ReducedMotion.REDUCE)); var page = context.newPage()) {
            var errors = errors(page);
            page.navigate(url(skin, mode));
            var pie = chart(page, "Revenue shares");
            assertThat(pie.locator("path.sw-pie-segment")).hasCount(5);
            assertThat(pie.locator(".sw-donut-share")).hasText(new String[]{"40%", "25%", "15%", "10%", "10%"});
            assertThat(pie.locator(".sw-donut-center")).hasCount(0);
            assertThat(pie.locator(".sw-pie-segment").first().evaluate("n=>{const s=getComputedStyle(n);return s.fill!=='none'&&s.strokeWidth==='1.5px';}")).isEqualTo(true);
            pie.locator("svg").scrollIntoViewIfNeeded();
            var svg = pie.locator("svg").boundingBox();
            page.mouse().move(svg.x + svg.width * .75, svg.y + svg.height * .3);
            assertThat(pie.locator(".sw-chart-tooltip-content")).containsText("North");
            pie.locator(".sw-donut-legend-item").last().hover();
            assertThat(pie.locator(".sw-chart-tooltip-content")).containsText("Other");
            pie.locator("summary").click();
            assertThat(pie.locator("tbody tr")).hasCount(7);
            assertThat(pie.locator("tfoot")).containsText("100%");
            assertThat(chart(page, "Every slice in view").locator("path.sw-pie-segment")).hasCount(7);
            assertThat(chart(page, "One active region").locator("path.sw-pie-segment")).hasCount(1);
            assertThat(chart(page, "Before the first slice").locator("path.sw-pie-segment")).hasCount(0);
            audit(page);
            pie.locator("summary").click();
            page.locator("section.ex-section").filter(new Locator.FilterOptions().setHasText("Each slice in context"))
                    .screenshot(new Locator.ScreenshotOptions().setPath(SHOTS.resolve(engine + "-" + skin + "-" + mode + ".png")));
            assertThat(errors).isEmpty();
        }
    }

    @ParameterizedTest @ValueSource(strings={"chromium", "firefox"})
    void keyboardTouchRtlAndAjaxReplacementUseTheSharedInteractionLifecycle(String engine) {
        var settings = ShadwickSettings.get(app);
        settings.setBundle(Bundle.EMBEDDED);
        try (var context = browser(engine).newContext(new Browser.NewContextOptions().setViewportSize(320, 950)
                .setHasTouch(true).setReducedMotion(ReducedMotion.REDUCE)); var page = context.newPage()) {
            var errors = errors(page);
            page.navigate(url("nova", "dark"));
            page.locator("main").evaluate("n=>n.dir='rtl'");
            var pie = chart(page, "Revenue shares");
            var plot = pie.locator(".sw-donut-plot");
            var tip = pie.locator(".sw-chart-tooltip-content");
            plot.focus();
            assertThat(tip).containsText("North");
            plot.press("ArrowLeft");
            assertThat(tip).containsText("South");
            plot.press("End");
            assertThat(tip).containsText("Other");
            plot.press("Escape");
            assertThat(tip).isHidden();
            assertThat(plot).isFocused();
            pie.locator(".sw-donut-legend-item").nth(2).tap();
            assertThat(tip).containsText("East");
            assertThat(pie.locator(".sw-pie-segment[data-active]")).hasAttribute("data-index", "2");
            pie.evaluate("n=>n.dataset.untouched='true'");
            List<String> navigations = new ArrayList<>();
            page.onRequest(request -> { if (request.isNavigationRequest()) navigations.add(request.url()); });
            page.getByText("Refresh report", new Page.GetByTextOptions().setExact(true)).click();
            var live = chart(page, "Live revenue shares");
            assertThat(live.locator(".sw-pie-segment")).hasCount(2);
            live.locator(".sw-donut-plot").focus();
            live.locator(".sw-donut-plot").press("End");
            assertThat(live.locator(".sw-chart-tooltip-content")).containsText("Americas");
            assertThat(live.locator(".sw-chart-tooltip-content")).containsText("25%");
            assertThat(pie).hasAttribute("data-untouched", "true");
            assertThat(navigations).isEmpty();
            live.evaluate("n=>n.replaceWith(n.cloneNode(true))");
            page.evaluate("()=>{Shadwick.enhanceDonutCharts();Shadwick.enhanceDonutCharts();}");
            live.locator(".sw-donut-plot").focus();
            assertThat(live.locator("[data-donut-status]")).hasCount(1);
            page.emulateMedia(new Page.EmulateMediaOptions().setForcedColors(ForcedColors.ACTIVE));
            live.locator(".sw-donut-plot").press("Tab");
            page.keyboard().press("Shift+Tab");
            assertThat(live.locator(".sw-donut-plot")).hasCSS("outline-style", "solid");
            page.emulateMedia(new Page.EmulateMediaOptions().setForcedColors(ForcedColors.NONE));
            audit(page);
            assertThat(errors).isEmpty();
        } finally { settings.setBundle(Bundle.STANDALONE); }
    }

    @ParameterizedTest @ValueSource(strings={"chromium", "firefox"})
    void nativeSectorsAndLocalizedExactDataWorkWithoutJavaScript(String engine) {
        try (var context = browser(engine).newContext(new Browser.NewContextOptions().setViewportSize(320, 950)
                .setJavaScriptEnabled(false)); var page = context.newPage()) {
            page.navigate(url("vega", "light") + "&lang=de");
            var pie = chart(page, "Revenue shares");
            assertThat(pie.locator("path.sw-pie-segment title")).hasCount(5);
            assertThat(pie.locator(".sw-donut-name").last()).hasText("Sonstige");
            assertThat(pie.locator(".sw-donut-plot")).not().hasAttribute("tabindex", "0");
            pie.locator("summary").click();
            assertThat(pie.locator("tbody tr")).hasCount(7);
            assertThat(pie.locator("thead")).containsText("Anteil");
            assertThat(pie.locator("tfoot th")).hasText("Gesamt");
            assertThat(chart(page, "Before the first slice").locator("[role=status]")).hasText("Keine positiven Werte vorhanden.");
            assertThat(page.evaluate("document.documentElement.scrollWidth <= innerWidth + 1")).isEqualTo(true);
        }
    }
}
