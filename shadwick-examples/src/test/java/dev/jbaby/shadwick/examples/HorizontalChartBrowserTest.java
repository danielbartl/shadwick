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
class HorizontalChartBrowserTest {
    static Playwright playwright;
    static Browser chromium, firefox;
    static final Path SHOTS = Path.of("target/horizontal-chart-screenshots");
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
    Locator chart(Page page, String title) { return page.locator(".sw-chart").filter(new Locator.FilterOptions().setHasText(title)); }
    List<String> errors(Page page) {
        List<String> errors = new ArrayList<>(); page.onPageError(errors::add);
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
    void horizontalRowsAndTheirLabelsFitEverySkin(String engine, String skin, String mode, int width) {
        try (var context = browser(engine).newContext(new Browser.NewContextOptions().setViewportSize(width, 1000)
                .setReducedMotion(ReducedMotion.REDUCE)); var page = context.newPage()) {
            var errors = errors(page); page.navigate(url(skin, mode));
            var grouped = chart(page, "Planned and delivered");
            assertThat(grouped.locator(".sw-chart-row")).hasCount(4);
            assertThat(grouped.locator(".sw-chart-category").first()).hasText("Northstar Foundation accessibility improvements");
            assertThat(grouped.evaluate("""
                chart => [...chart.querySelectorAll('.sw-chart-category')].every(label => {
                    const svg=label.nextElementSibling.querySelector('svg'), a=label.getBoundingClientRect(), b=svg.getBoundingClientRect();
                    return label.scrollWidth<=label.clientWidth+1 && Math.abs((a.top+a.bottom-b.top-b.bottom)/2)<1;
                })
                """)).as("wrapped labels stay centered beside their bars").isEqualTo(true);
            assertThat(grouped.locator(".sw-chart-value-axis").evaluate("""
                axis => {
                    const box=axis.getBoundingClientRect();
                    const labels=[...axis.querySelectorAll(':scope > span > span')].filter(n=>getComputedStyle(n).visibility!=='hidden')
                        .map(n=>n.getBoundingClientRect()).sort((a,b)=>a.left-b.left);
                    return labels.every((label,i)=>label.left>=box.left-1 && label.right<=box.right+1 && (!i || labels[i-1].right<=label.left+1));
                }
                """)).as("value labels fit without overlapping").isEqualTo(true);
            grouped.locator(".sw-chart-hit[data-index='1']").hover();
            assertThat(grouped.locator(".sw-chart-tooltip-content")).containsText("Harbor & Pine");
            assertThat(grouped.locator(".sw-chart-tooltip-content").textContent()).contains("68", "72");
            assertThat(grouped.locator(".sw-chart-row").nth(1).locator(".sw-chart-tooltip-content")).isVisible();
            audit(page);
            var section = page.locator("section.ex-section").filter(new Locator.FilterOptions().setHasText("Rankings with room for the names"));
            section.screenshot(new Locator.ScreenshotOptions().setPath(SHOTS.resolve(engine + "-" + skin + "-" + mode + ".png")));
            assertThat(errors).isEmpty();
        }
    }
    @ParameterizedTest @ValueSource(strings={"chromium", "firefox"})
    void rowKeyboardTouchRtlAndAjaxKeepTheirOwnState(String engine) {
        var settings = ShadwickSettings.get(app); settings.setBundle(Bundle.EMBEDDED);
        try (var context = browser(engine).newContext(new Browser.NewContextOptions().setViewportSize(320, 950)
                .setHasTouch(true).setReducedMotion(ReducedMotion.REDUCE)); var page = context.newPage()) {
            var errors = errors(page); page.navigate(url("nova", "dark"));
            page.locator("main").evaluate("n=>n.dir='rtl'");
            var grouped = chart(page, "Planned and delivered"); var plot = grouped.locator(".sw-chart-plot");
            assertThat(grouped.locator(".sw-chart-row-svg")).hasCount(4);
            assertThat(grouped.locator(".sw-chart-row-svg").last()).hasAttribute("data-rtl", "true");
            plot.focus(); assertThat(grouped.locator(".sw-chart-tooltip-content")).containsText("Northstar");
            plot.press("ArrowDown"); assertThat(grouped.locator(".sw-chart-tooltip-content")).containsText("Harbor & Pine");
            plot.press("ArrowUp"); assertThat(grouped.locator(".sw-chart-tooltip-content")).containsText("Northstar");
            plot.press("End"); assertThat(grouped.locator(".sw-chart-tooltip-content")).containsText("Greenfield");
            assertThat(grouped.locator(".sw-chart-hit").last().evaluate("n=>{const r=n.getBoundingClientRect();return r.top>=0 && r.bottom<=innerHeight;}")).isEqualTo(true);
            plot.press("Escape"); assertThat(grouped.locator(".sw-chart-tooltip-content")).isHidden(); assertThat(plot).isFocused();
            grouped.locator(".sw-chart-hit[data-index='2']").tap();
            assertThat(grouped.locator(".sw-chart-tooltip-content")).containsText("Clover Studio");
            assertThat(grouped.locator(".sw-chart-tooltip-content").textContent()).contains("52", "48");
            var stacked = chart(page, "Change in planned hours");
            stacked.locator(".sw-chart-hit[data-index='1']").tap();
            assertThat(stacked.locator(".sw-chart-tooltip-content")).containsText("-12");
            assertThat(stacked.locator(".sw-chart-tooltip-content")).containsText("-8");
            stacked.locator("summary").click(); assertThat(stacked.locator("tbody tr").last().locator("td").last()).isEmpty();
            var live = chart(page, "Live horizontal report");
            live.locator(".sw-chart-plot").focus(); assertThat(live.locator(".sw-chart-tooltip-content")).containsText("January");
            grouped.evaluate("n=>n.dataset.untouched='true'");
            List<String> navigations = new ArrayList<>(); page.onRequest(request -> { if (request.isNavigationRequest()) navigations.add(request.url()); });
            page.getByText("Refresh report", new Page.GetByTextOptions().setExact(true)).click();
            assertThat(live.locator(".sw-chart-row")).hasCount(3);
            live.locator(".sw-chart-plot").focus(); live.locator(".sw-chart-plot").press("End");
            assertThat(live.locator(".sw-chart-tooltip-content")).containsText("September");
            assertThat(live.locator(".sw-chart-tooltip-content")).containsText("280");
            assertThat(live.locator("[data-chart-status]")).hasCount(1);
            assertThat(grouped).hasAttribute("data-untouched", "true"); assertThat(navigations).isEmpty();
            page.emulateMedia(new Page.EmulateMediaOptions().setForcedColors(ForcedColors.ACTIVE));
            live.locator(".sw-chart-plot").press("Tab"); page.keyboard().press("Shift+Tab");
            assertThat(live.locator(".sw-chart-plot")).isFocused();
            assertThat(live.locator(".sw-chart-plot")).hasCSS("outline-style", "solid");
            page.emulateMedia(new Page.EmulateMediaOptions().setForcedColors(ForcedColors.NONE));
            audit(page); assertThat(errors).isEmpty();
        } finally { settings.setBundle(Bundle.STANDALONE); }
    }
    @ParameterizedTest @ValueSource(strings={"chromium", "firefox"})
    void nativeHorizontalChartsAndWrappedRtlLabelsWorkWithoutScript(String engine) {
        try (var context = browser(engine).newContext(new Browser.NewContextOptions().setViewportSize(320, 950)
                .setJavaScriptEnabled(false)); var page = context.newPage()) {
            page.navigate(url("vega", "light") + "&lang=de");
            page.locator("main").evaluate("n=>n.dir='rtl'");
            var grouped = chart(page, "Planned and delivered");
            assertThat(grouped.locator(".sw-chart-row-svg").first()).isVisible();
            assertThat(grouped.locator(".sw-chart-row-svg").first()).not().hasCSS("transform", "none");
            assertThat(grouped.locator(".sw-chart-hit title")).hasCount(4);
            assertThat(grouped.locator(".sw-chart-sr-only").first()).containsText("oben und unten");
            grouped.locator("summary").click(); assertThat(grouped.locator("table")).isVisible();
            assertThat(grouped.locator("tbody tr").first()).containsText("Northstar");
            assertThat(grouped.locator("tbody tr").first().textContent()).contains("84", "96");
            assertThat(grouped.locator(".sw-chart-plot")).not().hasAttribute("tabindex", "0");
            assertThat(page.evaluate("document.documentElement.scrollWidth <= innerWidth + 1")).isEqualTo(true);
        }
    }
}
