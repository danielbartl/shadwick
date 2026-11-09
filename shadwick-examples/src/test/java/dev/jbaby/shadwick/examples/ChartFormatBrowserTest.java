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
import java.util.regex.Pattern;
import java.util.stream.Stream;
import static com.microsoft.playwright.assertions.PlaywrightAssertions.assertThat;
import static org.assertj.core.api.Assertions.assertThat;

@Tag("browser")
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class ChartFormatBrowserTest {
    static Playwright playwright;
    static Browser chromium, firefox;
    static final Path SHOTS = Path.of("target/chart-format-screenshots");
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
    String url(String skin, String mode, String language) { return "http://localhost:" + port + "/charts?skin=" + skin + "&mode=" + mode + "&lang=" + language; }
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
    void tooltipFits(Locator c) {
        tooltipFits(c, true);
    }
    void tooltipFits(Locator c, boolean requireSingleLine) {
        @SuppressWarnings("unchecked")
        var layout = (Map<String, Object>) c.locator(".sw-chart-tooltip-content").evaluate("""
            (tip,requireSingleLine)=>{
                const box=tip.getBoundingClientRect(),root=tip.closest('.sw-chart').getBoundingClientRect();
                const node=tip.querySelector('.sw-chart-tooltip-value'),name=tip.querySelector('.sw-chart-tooltip-name').getBoundingClientRect(),value=node.getBoundingClientRect();
                const range=document.createRange();range.selectNodeContents(node);
                const tops=[...range.getClientRects()].map(r=>r.top),singleLine=Math.max(...tops)-Math.min(...tops)<1;
                const inside=box.width>0 && box.left>=root.left-1 && box.right<=root.right+1;
                const disjoint=name.right<=value.left || value.right<=name.left,style=getComputedStyle(node);
                return {fits:(!requireSingleLine || singleLine) && inside && disjoint,singleLine,inside,disjoint,
                    box:box.toJSON(),root:root.toJSON(),name:name.toJSON(),value:value.toJSON(),tops,
                    text:node.textContent,font:style.font,fontFamily:style.fontFamily,fontSize:style.fontSize,
                    maxInlineSize:style.maxInlineSize,padding:style.padding,fonts:document.fonts.status};
            }
            """, requireSingleLine);
        if (!Boolean.TRUE.equals(layout.get("fits")))
            c.screenshot(new Locator.ScreenshotOptions().setPath(SHOTS.resolve("tooltip-fit-failure-" + System.nanoTime() + ".png")));
        assertThat(layout.get("fits")).as("Tooltip sizing in %s: %s", c.locator(".sw-chart-title").textContent(), layout).isEqualTo(true);
    }
    static Stream<Arguments> appearances() {
        return Stream.of("chromium", "firefox").flatMap(engine -> Stream.of("vega", "nova", "sketch").flatMap(skin ->
                Stream.of(Arguments.of(engine, skin, "light", 1440), Arguments.of(engine, skin, "dark", 390))));
    }
    @ParameterizedTest @MethodSource("appearances")
    void compactAxesAndDetailedTooltipsLeaveTablesAndLabelsUnchanged(String engine, String skin, String mode, int width) {
        try (var context = browser(engine).newContext(new Browser.NewContextOptions().setViewportSize(width, 1000)
                .setTimezoneId("America/Los_Angeles").setReducedMotion(ReducedMotion.REDUCE)); var p = context.newPage()) {
            var errors = errors(p); p.navigate(url(skin, mode, "en"));
            for (String title : List.of("Revenue, from overview to detail", "Monthly revenue, side by side")) {
                var c = chart(p, title);
                var axis = c.locator(".sw-chart-y-axis,.sw-chart-value-axis");
                assertThat(axis).containsText("30K"); assertThat(axis).not().containsText("€");
                assertThat(c.locator(".sw-chart-x-axis > span,.sw-chart-category").first()).hasText("Apr");
                c.locator(".sw-chart-hit[data-index='0']").hover();
                assertThat(c.locator(".sw-chart-tooltip-label")).hasText("Apr 1, 2026");
                assertThat(c.locator(".sw-chart-tooltip-value")).hasText("€12,345.67");
                tooltipFits(c);
                if (title.equals("Revenue, from overview to detail"))
                    c.screenshot(new Locator.ScreenshotOptions().setPath(SHOTS.resolve(engine + "-" + skin + "-" + mode + "-detail.png")));
                c.locator(".sw-chart-hit[data-index='1']").hover();
                assertThat(c.locator(".sw-chart-tooltip-label")).hasText("May 1, 2026");
                assertThat(c.locator(".sw-chart-tooltip-item")).hasCount(0);
                c.locator(".sw-chart-hit[data-index='3']").hover();
                assertThat(c.locator(".sw-chart-tooltip-value")).hasText("€0.00");
                c.locator(".sw-chart-hit[data-index='4']").hover();
                assertThat(c.locator(".sw-chart-tooltip-value")).hasText("-€1,250.25");
                assertThat(c.locator("[data-label-dir=ltr] text")).hasText("-1,250.25");
                c.locator("summary").click();
                assertThat(c.locator("tbody tr").first().locator("th")).hasText("Apr 1, 2026");
                assertThat(c.locator("tbody tr").first().locator("td")).hasText("12,345.67");
                assertThat(c.locator("tbody tr").nth(1).locator("td")).isEmpty();
                audit(p); c.locator("summary").click();
            }
            var invoices = chart(p, "The right context for an invoice");
            assertThat(invoices.locator(".sw-chart-category").first()).hasText("Northstar");
            invoices.locator(".sw-chart-hit").first().hover();
            assertThat(invoices.locator(".sw-chart-tooltip-label")).hasText("Northstar Foundation · INV-2041");
            assertThat(invoices.locator(".sw-chart-tooltip-value")).hasText("4,800.5 USD");
            invoices.locator("summary").click();
            assertThat(invoices.locator("tbody tr").first().locator("th")).hasText("Northstar Foundation");
            audit(p); invoices.locator("summary").click();
            p.locator("section.ex-section").filter(new Locator.FilterOptions().setHasText("A clear overview. Detail on demand."))
                    .screenshot(new Locator.ScreenshotOptions().setPath(SHOTS.resolve(engine + "-" + skin + "-" + mode + ".png")));
            assertThat(errors).isEmpty();
        }
    }
    @ParameterizedTest @ValueSource(strings={"chromium", "firefox"})
    void germanKeyboardAndTouchKeepTheirFormatsAfterPartialAjaxAndRepeatedInitialization(String engine) {
        var settings = ShadwickSettings.get(app); settings.setBundle(Bundle.EMBEDDED);
        try (var context = browser(engine).newContext(new Browser.NewContextOptions().setViewportSize(320, 950).setHasTouch(true)
                .setTimezoneId("Pacific/Honolulu").setLocale("en-US").setReducedMotion(ReducedMotion.REDUCE)); var p = context.newPage()) {
            var errors = errors(p); p.navigate(url("nova", "dark", "de")); p.locator("main").evaluate("n=>n.dir='rtl'");
            var c = chart(p, "Fresh figures, familiar formats"); var plot = c.locator(".sw-chart-plot");
            plot.focus(); plot.press("Home");
            assertThat(c.locator(".sw-chart-tooltip-label")).hasText("01.04.2026");
            assertThat(c.locator(".sw-chart-tooltip-value")).hasText(Pattern.compile("12\\.345,67\\s*€"));
            tooltipFits(c);
            assertThat(c.locator("[data-chart-status]")).containsText("12.345,67");
            plot.press("ArrowLeft"); assertThat(c.locator(".sw-chart-tooltip-item")).hasCount(0);
            assertThat(c.locator("[data-chart-status]")).hasText("01.05.2026");
            var horizontal = chart(p, "Monthly revenue, side by side");
            horizontal.locator(".sw-chart-hit").first().tap();
            assertThat(horizontal.locator(".sw-chart-tooltip-value")).hasText(Pattern.compile("12\\.345,67\\s*€"));
            var original = chart(p, "Revenue, from overview to detail"); original.evaluate("n=>n.dataset.untouched='true'");
            List<String> navigation = new ArrayList<>(); p.onRequest(request -> { if (request.isNavigationRequest()) navigation.add(request.url()); });
            p.getByText("Refresh report", new Page.GetByTextOptions().setExact(true)).click();
            assertThat(c.locator(".sw-chart-hit")).hasCount(2); plot.focus(); plot.press("End");
            assertThat(c.locator(".sw-chart-tooltip-label")).hasText("01.10.2026");
            assertThat(c.locator(".sw-chart-tooltip-value")).hasText(Pattern.compile("22\\.500,75\\s*€"));
            tooltipFits(c);
            assertThat(c.locator("[data-chart-status]")).containsText("22.500,75");
            assertThat(c.locator(".sw-chart-x-axis > span").last()).hasText("Okt.");
            assertThat(c.locator("[data-label-dir=rtl] text")).hasText("22.500,75");
            assertThat(original).hasAttribute("data-untouched", "true"); assertThat(navigation).isEmpty();
            c.evaluate("n=>n.replaceWith(n.cloneNode(true))"); p.evaluate("()=>{Shadwick.enhanceCharts();Shadwick.enhanceCharts();}");
            assertThat(c.locator("[data-chart-status]")).hasCount(1); plot.focus(); plot.press("End");
            assertThat(c.locator(".sw-chart-tooltip-value")).hasText(Pattern.compile("22\\.500,75\\s*€"));
            c.locator("tbody tr").last().evaluate("row=>{row.cells[0].dataset.tooltipLabel='<img src=x>';row.cells[1].dataset.tooltipValue='<script>literal</script>';}");
            plot.press("End"); assertThat(c.locator(".sw-chart-tooltip-label")).hasText("<img src=x>");
            assertThat(c.locator(".sw-chart-tooltip-value")).hasText("<script>literal</script>");
            assertThat(c.locator(".sw-chart-tooltip-content img,.sw-chart-tooltip-content script")).hasCount(0);
            String longValue = "9".repeat(90) + " EUR";
            c.locator("tbody tr").last().locator("td").evaluate("(cell,value)=>cell.dataset.tooltipValue=value", longValue);
            plot.press("End"); assertThat(c.locator(".sw-chart-tooltip-value")).hasText(longValue); tooltipFits(c, false);
            c.locator("tbody tr").last().evaluate("row=>row.cells[1].dataset.tooltipValue=''"); plot.press("End");
            assertThat(c.locator(".sw-chart-tooltip-item")).hasCount(0);
            c.locator("tbody tr").last().evaluate("row=>delete row.cells[1].dataset.tooltipValue"); plot.press("End");
            assertThat(c.locator(".sw-chart-tooltip-value")).hasText("22.500,75");
            plot.press("Escape"); audit(p); assertThat(errors).isEmpty();
        } finally { settings.setBundle(Bundle.STANDALONE); }
    }
    @ParameterizedTest @ValueSource(strings={"chromium", "firefox"})
    void nativeTitlesAndExactDataUseServerFormatsWithoutScriptsOrBrowserDateParsing(String engine) {
        try (var context = browser(engine).newContext(new Browser.NewContextOptions().setViewportSize(320, 950)
                .setTimezoneId("America/Los_Angeles").setJavaScriptEnabled(false)); var p = context.newPage()) {
            p.navigate(url("vega", "light", "en")); p.locator("main").evaluate("n=>n.dir='rtl'");
            for (String title : List.of("Revenue, from overview to detail", "Monthly revenue, side by side")) {
                var c = chart(p, title);
                assertThat(c.locator(".sw-chart-hit").first().locator("title")).hasText("Apr 1, 2026; Net revenue (EUR): €12,345.67");
                c.locator("summary").click(); assertThat(c.locator("table")).isVisible();
                assertThat(c.locator("tbody tr").first().locator("td")).hasText("12,345.67");
                assertThat(c.locator(".sw-chart-plot")).not().hasAttribute("tabindex", "0");
            }
            assertThat(p.evaluate("document.documentElement.scrollWidth <= innerWidth + 1")).isEqualTo(true);
        }
    }
}
