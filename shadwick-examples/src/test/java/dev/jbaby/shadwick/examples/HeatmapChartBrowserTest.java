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
class HeatmapChartBrowserTest {
    static Playwright playwright;
    static Browser chromium, firefox;
    static final Path SHOTS = Path.of("target/heatmap-chart-screenshots");
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
        return page.locator(".sw-heatmap-chart").filter(new Locator.FilterOptions()
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

    Locator cell(Locator heatmap, int row, int column) {
        return heatmap.locator(".sw-heatmap-cell[data-row='" + row + "'][data-column='" + column + "']");
    }
    void expectTooltip(Locator heatmap, String label, String value) {
        assertThat(heatmap.locator(".sw-chart-tooltip-label")).hasText(label);
        assertThat(heatmap.locator(".sw-chart-tooltip-value")).hasText(value);
    }
    void cellIsVisibleBesideStickyHeader(Locator heatmap) {
        assertThat(heatmap.locator(".sw-heatmap-cell[data-active]").evaluate("""
            cell=>{const plot=cell.closest('.sw-heatmap-plot'),bounds=plot.getBoundingClientRect(),box=cell.getBoundingClientRect();
              const header=cell.parentElement.cells[0].getBoundingClientRect(),rtl=getComputedStyle(plot).direction==='rtl';
              return box.left>=bounds.left-1&&box.right<=bounds.right+1&&(rtl?box.right<=header.left+1:box.left>=header.right-1);}
            """)).as("active cell is visible beside its sticky row header").isEqualTo(true);
    }

    @ParameterizedTest @MethodSource("appearances")
    void matrixLabelsValuesAndFiveBandsFitEverySkin(String engine, String skin, String mode, int width) {
        try (var context = browser(engine).newContext(new Browser.NewContextOptions().setViewportSize(width, 1000)
                .setReducedMotion(ReducedMotion.REDUCE)); var page = context.newPage()) {
            var errors = errors(page);
            page.navigate(url(skin, mode));
            var heatmap = chart(page, "Weekly team workload");
            assertThat(heatmap.locator(".sw-heatmap-cell")).hasCount(15);
            assertThat(heatmap.locator("thead th")).hasText(new String[]{"Team", "Monday", "Tuesday", "Wednesday", "Thursday", "Friday"});
            assertThat(heatmap.locator("tbody th")).hasText(new String[]{"Design", "Engineering", "Support"});
            assertThat(heatmap.locator(".sw-heatmap-bands li")).hasCount(5);
            assertThat(heatmap.locator(".sw-heatmap-bands li").first()).containsText("0 – 10");
            assertThat(heatmap.locator(".sw-heatmap-bands li").last()).containsText("40 – 50");
            assertThat(cell(heatmap, 1, 2).locator(".sw-heatmap-level-4")).hasCount(1);
            if (width >= 1000) assertThat(heatmap.locator(".sw-heatmap-plot").evaluate("n=>n.scrollWidth<=n.clientWidth+1")).isEqualTo(true);
            cell(heatmap, 0, 0).hover();
            expectTooltip(heatmap, "Design / Monday", "12");
            heatmap.locator(".sw-heatmap-plot").focus();
            heatmap.locator(".sw-heatmap-plot").press("End");
            expectTooltip(heatmap, "Design / Friday", "0");
            cellIsVisibleBesideStickyHeader(heatmap);
            heatmap.locator(".sw-heatmap-plot").press("Control+End");
            heatmap.locator(".sw-heatmap-plot").press("ArrowLeft");
            heatmap.locator(".sw-heatmap-plot").press("ArrowLeft");
            expectTooltip(heatmap, "Support / Wednesday", "Not measured");
            assertThat(cell(heatmap, 2, 2).locator(".sw-heatmap-value")).isEmpty();
            assertThat(cell(heatmap, 2, 2).locator(".sw-heatmap-missing")).hasCount(1);
            var hidden = chart(page, "A quieter workload overview");
            assertThat(cell(hidden, 0, 0).locator(".sw-heatmap-value")).hasText("12");
            hidden.locator(".sw-heatmap-plot").focus();
            expectTooltip(hidden, "Design / Monday", "12");
            var signed = chart(page, "Changes against the plan");
            assertThat(cell(signed, 2, 0).locator(".sw-heatmap-level-0")).hasCount(1);
            assertThat(cell(signed, 0, 1).locator(".sw-heatmap-level-2")).hasCount(1);
            var nativeLabels = chart(page, "Long team and reporting labels");
            assertThat(nativeLabels.locator(".sw-heatmap-cell[title]")).hasCount(10);
            assertThat(nativeLabels.locator(".sw-heatmap-plot")).not().hasAttribute("data-heatmap-enhanced", "true");
            assertThat(chart(page, "A measured zero week").locator(".sw-heatmap-missing")).hasCount(0);
            assertThat(chart(page, "Waiting for weekly reports").locator(".sw-heatmap-cell")).hasCount(5);
            assertThat(chart(page, "Waiting for weekly reports").locator("p[role=status]")).containsText("No measurements");
            assertThat(chart(page, "The next workload matrix").locator("[role=status]")).containsText("No data");
            assertThat(chart(page, "One team, one report").locator(".sw-heatmap-cell")).hasCount(1);
            var tiny = chart(page, "Small measurements, exact bands");
            assertThat(cell(tiny, 0, 0).locator(".sw-heatmap-level-4")).hasCount(1);
            assertThat(cell(tiny, 0, 1).locator(".sw-heatmap-level-2")).hasCount(1);
            assertThat(cell(tiny, 0, 0).locator(".sw-heatmap-value")).hasText("1E-1000");
            audit(page);
            page.mouse().move(0, 0);
            page.evaluate("()=>document.activeElement?.blur()");
            heatmap.locator(".sw-heatmap-plot").evaluate("n=>n.scrollLeft=0");
            page.locator("section.ex-section").filter(new Locator.FilterOptions().setHasText("A matrix of measurements"))
                    .screenshot(new Locator.ScreenshotOptions().setPath(SHOTS.resolve(engine + "-" + skin + "-" + mode + ".png")));
            assertThat(errors).isEmpty();
        }
    }

    @ParameterizedTest @ValueSource(strings={"chromium", "firefox"})
    void twoDimensionalNavigationRtlTouchAndAjaxKeepCellsReachable(String engine) {
        var settings = ShadwickSettings.get(app);
        settings.setBundle(Bundle.EMBEDDED);
        try (var context = browser(engine).newContext(new Browser.NewContextOptions().setViewportSize(320, 950)
                .setHasTouch(true).setReducedMotion(ReducedMotion.REDUCE)); var page = context.newPage()) {
            var errors = errors(page);
            page.navigate(url("nova", "dark"));
            var heatmap = chart(page, "Weekly team workload");
            var plot = heatmap.locator(".sw-heatmap-plot");
            plot.focus();
            expectTooltip(heatmap, "Design / Monday", "12");
            plot.press("ArrowRight");
            expectTooltip(heatmap, "Design / Tuesday", "18");
            plot.press("ArrowDown");
            expectTooltip(heatmap, "Engineering / Tuesday", "36");
            plot.press("End");
            expectTooltip(heatmap, "Engineering / Friday", "16");
            cellIsVisibleBesideStickyHeader(heatmap);
            plot.press("Home");
            expectTooltip(heatmap, "Engineering / Monday", "30");
            cellIsVisibleBesideStickyHeader(heatmap);
            plot.press("Control+End");
            expectTooltip(heatmap, "Support / Friday", "24");
            plot.press("ArrowDown");
            expectTooltip(heatmap, "Support / Friday", "24");
            plot.press("Control+Home");
            page.locator("main").evaluate("n=>n.dir='rtl'");
            plot.press("ArrowLeft");
            expectTooltip(heatmap, "Design / Tuesday", "18");
            plot.press("End");
            expectTooltip(heatmap, "Design / Friday", "0");
            cellIsVisibleBesideStickyHeader(heatmap);
            plot.press("Home");
            cellIsVisibleBesideStickyHeader(heatmap);
            plot.press("Escape");
            assertThat(heatmap.locator(".sw-heatmap-tooltip")).isHidden();
            assertThat(plot).isFocused();
            cell(heatmap, 0, 0).tap();
            expectTooltip(heatmap, "Design / Monday", "12");
            assertThat(heatmap.locator("[tabindex='0']")).hasCount(1);
            page.setViewportSize(700, 950);
            plot.press("Control+End");
            cellIsVisibleBesideStickyHeader(heatmap);
            heatmap.evaluate("n=>n.dataset.untouched='true'");
            List<String> navigations = new ArrayList<>();
            page.onRequest(request -> { if (request.isNavigationRequest()) navigations.add(request.url()); });
            page.getByText("Refresh workload matrix", new Page.GetByTextOptions().setExact(true)).click();
            var live = chart(page, "A changing workload matrix");
            assertThat(live.locator(".sw-heatmap-cell")).hasCount(10);
            assertThat(live.locator("tbody th")).hasText(new String[]{"Delivery", "Research"});
            assertThat(live.locator("thead th").nth(1)).hasText("Latest Monday");
            assertThat(live.locator(".sw-heatmap-bands li").last()).containsText("48 – 60");
            assertThat(live.locator(".sw-heatmap-content")).hasClass("sw-heatmap-content sw-chart-series-5");
            assertThat(live.locator(".sw-heatmap-content")).hasAttribute("data-values", "false");
            live.locator(".sw-heatmap-plot").focus();
            live.locator(".sw-heatmap-plot").press("Control+Home");
            expectTooltip(live, "Delivery / Latest Monday", "34");
            assertThat(live.locator(".sw-chart-tooltip-name")).hasText("Updated hours");
            assertThat(heatmap).hasAttribute("data-untouched", "true");
            assertThat(navigations).isEmpty();
            live.evaluate("n=>n.replaceWith(n.cloneNode(true))");
            page.evaluate("()=>{Shadwick.enhanceHeatmaps();Shadwick.enhanceHeatmaps();}");
            live.locator(".sw-heatmap-plot").focus();
            live.locator(".sw-heatmap-plot").press("Control+End");
            expectTooltip(live, "Research / Friday", "16");
            cellIsVisibleBesideStickyHeader(live);
            assertThat(live.locator("[data-heatmap-status]")).hasCount(1);
            page.emulateMedia(new Page.EmulateMediaOptions().setForcedColors(ForcedColors.ACTIVE));
            live.locator(".sw-heatmap-plot").press("Tab"); page.keyboard().press("Shift+Tab");
            assertThat(live.locator(".sw-heatmap-plot")).hasCSS("outline-style", "solid");
            page.emulateMedia(new Page.EmulateMediaOptions().setForcedColors(ForcedColors.NONE));
            audit(page);
            assertThat(errors).isEmpty();
        } finally { settings.setBundle(Bundle.STANDALONE); }
    }

    @ParameterizedTest @ValueSource(strings={"chromium", "firefox"})
    void localizedNativeTableValuesTitlesAndMissingMarksWorkWithoutScript(String engine) {
        try (var context = browser(engine).newContext(new Browser.NewContextOptions().setViewportSize(320, 950)
                .setJavaScriptEnabled(false)); var page = context.newPage()) {
            page.navigate(url("vega", "light") + "&lang=de");
            page.locator("main").evaluate("n=>n.dir='rtl'");
            var heatmap = chart(page, "Weekly team workload");
            assertThat(heatmap.locator(".sw-heatmap-cell[title]")).hasCount(15);
            assertThat(cell(heatmap, 0, 0)).hasAttribute("title", "Design; Monday; Hours: 12");
            assertThat(heatmap.locator(".sw-heatmap-plot")).hasAttribute("tabindex", "0");
            assertThat(heatmap.locator(".sw-heatmap-plot")).not().hasAttribute("data-heatmap-enhanced", "true");
            assertThat(heatmap.locator(".sw-heatmap-scale-title")).hasText("Skala: Hours");
            assertThat(cell(heatmap, 2, 2)).hasAttribute("aria-label", "Nicht gemessen");
            assertThat(cell(heatmap, 2, 2).locator(".sw-heatmap-value")).isEmpty();
            assertThat(cell(heatmap, 2, 2)).hasAttribute("title", "Support; Wednesday; Hours: Nicht gemessen");
            assertThat(heatmap.locator(".sw-heatmap-value")).hasCount(15);
            assertThat(chart(page, "Waiting for weekly reports").locator("p[role=status]")).hasText("Keine Messwerte vorhanden.");
            assertThat(chart(page, "The next workload matrix").locator("[role=status]")).hasText("Keine Daten vorhanden.");
            assertThat(chart(page, "Small measurements, exact bands").locator(".sw-heatmap-value").first()).hasText("1E-1000");
            assertThat(page.evaluate("document.documentElement.scrollWidth <= innerWidth + 1")).isEqualTo(true);
        }
    }
}
