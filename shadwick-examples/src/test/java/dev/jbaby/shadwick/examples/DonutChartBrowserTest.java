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
class DonutChartBrowserTest {
    static Playwright playwright;
    static Browser chromium, firefox;
    static final Path SHOTS = Path.of("target/donut-chart-screenshots");
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
    Locator chart(Page page, String title) { return page.locator(".sw-donut-chart").filter(new Locator.FilterOptions().setHasText(title)); }
    List<String> errors(Page page) {
        List<String> errors = new ArrayList<>(); page.onPageError(errors::add);
        page.onConsoleMessage(message -> { if (message.type().equals("error")) errors.add(message.text()); }); return errors;
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
    void nativeDonutsAndLegendsFitEverySkin(String engine, String skin, String mode, int width) {
        try (var context = browser(engine).newContext(new Browser.NewContextOptions().setViewportSize(width, 1000)
                .setReducedMotion(ReducedMotion.REDUCE)); var page = context.newPage()) {
            var errors = errors(page); page.navigate(url(skin, mode));
            var grouped = chart(page, "Revenue by region");
            assertThat(grouped.locator(".sw-donut-segment")).hasCount(5);
            assertThat(grouped.locator(".sw-donut-name")).hasText(new String[]{"North", "South", "East", "West", "Other"});
            assertThat(grouped.locator(".sw-donut-share")).hasText(new String[]{"40%", "25%", "15%", "10%", "10%"});
            assertThat(grouped.locator(".sw-donut-total")).containsText("100");
            assertThat(grouped.locator(".sw-donut-svg").evaluate("n=>{const r=n.getBoundingClientRect();return Math.abs(r.width-r.height)<1;}")).isEqualTo(true);
            assertThat(chart(page, "Delivered hours by client").locator(".sw-donut-legend").evaluate("n=>n.scrollWidth<=n.clientWidth+1")).isEqualTo(true);
            grouped.locator(".sw-donut-legend-item").last().hover();
            assertThat(grouped.locator(".sw-chart-tooltip-content")).containsText("Other");
            assertThat(grouped.locator(".sw-chart-tooltip-content")).containsText("10%");
            grouped.locator("summary").click();
            assertThat(grouped.locator("tbody tr")).hasCount(7);
            assertThat(grouped.locator("tbody tr").last()).containsText("Overseas");
            assertThat(grouped.locator("tfoot")).containsText("100%");
            audit(page);
            grouped.locator("summary").click();
            page.locator("section.ex-section").filter(new Locator.FilterOptions().setHasText("A share of the whole"))
                    .screenshot(new Locator.ScreenshotOptions().setPath(SHOTS.resolve(engine + "-" + skin + "-" + mode + ".png")));
            assertThat(errors).isEmpty();
        }
    }
    @ParameterizedTest @ValueSource(strings={"chromium", "firefox"})
    void keyboardRtlTouchReplacementAndNativeDialogEscapeRemainLocal(String engine) {
        var settings = ShadwickSettings.get(app); settings.setBundle(Bundle.EMBEDDED);
        try (var context = browser(engine).newContext(new Browser.NewContextOptions().setViewportSize(320, 950)
                .setHasTouch(true).setReducedMotion(ReducedMotion.REDUCE)); var page = context.newPage()) {
            var errors = errors(page); page.navigate(url("nova", "dark")); page.locator("main").evaluate("n=>n.dir='rtl'");
            var grouped = chart(page, "Revenue by region"); var plot = grouped.locator(".sw-donut-plot"); var tip = grouped.locator(".sw-chart-tooltip-content");
            plot.focus(); assertThat(tip).containsText("North"); plot.press("ArrowDown"); assertThat(tip).containsText("South");
            plot.press("Home"); plot.press("ArrowLeft"); assertThat(tip).containsText("South");
            plot.press("End"); assertThat(tip).containsText("Other"); plot.press("Escape"); assertThat(tip).isHidden(); assertThat(plot).isFocused();
            grouped.locator(".sw-donut-legend-item").nth(2).tap(); assertThat(tip).containsText("East");
            assertThat(grouped.locator(".sw-donut-segment[data-active]")).hasAttribute("data-index", "2");
            var live = chart(page, "Live regional breakdown");
            List<String> navigations = new ArrayList<>(); page.onRequest(request -> { if (request.isNavigationRequest()) navigations.add(request.url()); });
            grouped.evaluate("n=>n.dataset.untouched='true'");
            page.getByText("Refresh report", new Page.GetByTextOptions().setExact(true)).click();
            assertThat(live.locator(".sw-donut-segment")).hasCount(2);
            live.locator(".sw-donut-plot").focus(); live.locator(".sw-donut-plot").press("End");
            assertThat(live.locator(".sw-chart-tooltip-content")).containsText("Americas");
            assertThat(live.locator(".sw-chart-tooltip-content")).containsText("25%");
            assertThat(grouped).hasAttribute("data-untouched", "true"); assertThat(navigations).isEmpty();
            live.evaluate("n=>n.replaceWith(n.cloneNode(true))"); page.evaluate("()=>{Shadwick.enhanceDonutCharts();Shadwick.enhanceDonutCharts();}");
            live.locator(".sw-donut-plot").focus(); assertThat(live.locator("[data-donut-status]")).hasCount(1);
            page.emulateMedia(new Page.EmulateMediaOptions().setForcedColors(ForcedColors.ACTIVE));
            live.locator(".sw-donut-plot").press("Tab"); page.keyboard().press("Shift+Tab");
            assertThat(live.locator(".sw-donut-plot")).isFocused(); assertThat(live.locator(".sw-donut-plot")).hasCSS("outline-style", "solid");
            page.emulateMedia(new Page.EmulateMediaOptions().setForcedColors(ForcedColors.NONE)); audit(page);
            page.evaluate("()=>{const d=document.createElement('dialog');d.id='donut-dialog';d.setAttribute('aria-label','Donut dialog');d.append(document.querySelector('.sw-donut-chart'));document.body.append(d);d.showModal();}");
            page.locator("#donut-dialog .sw-donut-plot").focus(); page.keyboard().press("Escape");
            assertThat(page.locator("#donut-dialog")).isVisible(); assertThat(page.locator("#donut-dialog .sw-chart-tooltip-content")).isHidden();
            page.keyboard().press("Escape"); assertThat(page.locator("#donut-dialog")).isHidden(); assertThat(errors).isEmpty();
        } finally { settings.setBundle(Bundle.STANDALONE); }
    }
    @ParameterizedTest @ValueSource(strings={"chromium", "firefox"})
    void exactValuesAndEmptyStatesRemainUsefulWithoutScript(String engine) {
        try (var context = browser(engine).newContext(new Browser.NewContextOptions().setViewportSize(320, 950).setJavaScriptEnabled(false)); var page = context.newPage()) {
            page.navigate(url("vega", "light") + "&lang=de"); page.locator("main").evaluate("n=>n.dir='rtl'");
            var grouped = chart(page, "Revenue by region");
            assertThat(grouped.locator(".sw-donut-segment title")).hasCount(5);
            assertThat(grouped.locator(".sw-donut-name").last()).hasText("Sonstige");
            assertThat(grouped.locator(".sw-donut-total-label")).hasText("Gesamt");
            grouped.locator("summary").click(); assertThat(grouped.locator("tbody tr")).hasCount(7);
            assertThat(grouped.locator("thead")).containsText("Anteil"); assertThat(grouped.locator(".sw-donut-plot")).not().hasAttribute("tabindex", "0");
            var zero = chart(page, "Before the first share"); assertThat(zero.locator(".sw-donut-segment")).hasCount(0);
            assertThat(zero.locator("[role=status]")).hasText("Keine positiven Werte vorhanden.");
            zero.locator("summary").click(); assertThat(zero.locator("tbody tr").first().locator("td").last().textContent().replace('\u00a0',' ')).isEqualTo("0 %");
            assertThat(zero.locator("tbody tr").last().locator("td").first()).isEmpty();
            assertThat(chart(page, "Your next breakdown").locator("[role=status]")).hasText("Keine Daten vorhanden.");
            assertThat(page.evaluate("document.documentElement.scrollWidth <= innerWidth + 1")).isEqualTo(true);
        }
    }
}
