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
class RecordDisplayBrowserTest {
    static Playwright playwright;
    static Browser chromium, firefox;
    @LocalServerPort int port;
    @Autowired WebApplication app;
    @BeforeAll static void start() throws Exception {
        Files.createDirectories(Path.of("target/record-display-screenshots"));
        playwright = Playwright.create(); chromium = playwright.chromium().launch(); firefox = playwright.firefox().launch();
    }
    @AfterAll static void stop() { playwright.close(); }
    @BeforeEach void markup() { app.getMarkupSettings().setStripWicketTags(true); }
    @AfterEach void restore() { app.getMarkupSettings().setStripWicketTags(false); }
    Browser browser(String engine) { return engine.equals("chromium") ? chromium : firefox; }
    String url(String skin, String mode, String lang) { return "http://localhost:" + port + "/record-display?skin=" + skin + "&mode=" + mode + "&lang=" + lang; }
    void idle(Page p) { p.waitForCondition(() -> (Boolean)p.evaluate("Object.keys(Wicket.channelManager.channels).length===0")); }
    List<String> errors(Page p) {
        var result = new ArrayList<String>(); p.onPageError(result::add);
        p.onConsoleMessage(m -> { if (m.type().equals("error")) result.add(m.text()); }); return result;
    }
    void audit(Page p) {
        assertThat(p.evaluate("document.documentElement.scrollWidth<=innerWidth+1"))
                .as("Overflow: %s", p.evaluate("[...document.querySelectorAll('body *')].filter(n=>{let r=n.getBoundingClientRect();return r.width&&(r.right>innerWidth+1||r.left < -1)}).slice(0,10).map(n=>[n.tagName,n.className,n.getBoundingClientRect().width])")).isEqualTo(true);
        assertThat(new AxeBuilder(p).withTags(List.of("wcag2a", "wcag2aa", "wcag21a", "wcag21aa", "wcag22aa")).analyze().getViolations()).isEmpty();
    }
    static Stream<Arguments> skins() {
        var selected = Arrays.asList(System.getProperty("shadwick.browser.skins", "vega,lyra,nova,maia,mira,luma,rhea,sera,sketch").split(","));
        return Stream.of("chromium", "firefox").flatMap(engine -> Arrays.stream(Skin.values()).filter(skin -> selected.contains(skin.id())).flatMap(skin -> Stream.of(
                Arguments.of(engine, skin.id(), "light", 1440), Arguments.of(engine, skin.id(), "dark", 320))));
    }
    @ParameterizedTest @MethodSource("skins")
    void recordDetailsAndHistoryStayNativeResponsiveAndCurrentAfterAjax(String engine, String skin, String mode, int width) {
        try (var context = browser(engine).newContext(new Browser.NewContextOptions().setViewportSize(width,1000).setReducedMotion(ReducedMotion.REDUCE)); var p = context.newPage()) {
            var errors = errors(p); var response = p.navigate(url(skin, mode, "en-US"));
            assertThat(response.headers().get("content-security-policy")).contains("script-src");
            var details = p.locator("dl.sw-description-list").first(); var compact = p.locator(".ex-record-compact dl");
            assertThat(details.locator(":scope > .sw-description-list-item")).hasCount(6);
            assertThat(details.locator("dt")).hasCount(6); assertThat(details.locator("dd")).hasCount(6);
            assertThat(details.locator("dd").nth(1)).hasText("—");
            assertThat(details.locator("dd").nth(3)).hasText("48,250.1254");
            assertThat(details.locator(".sw-description-list-item").first().evaluate("n=>getComputedStyle(n).gridTemplateColumns.split(' ').length")).isEqualTo(width>500 ? 2 : 1);
            assertThat(compact.locator(".sw-description-list-item").first().evaluate("n=>getComputedStyle(n).gridTemplateColumns.split(' ').length")).isEqualTo(1);
            var history = p.locator("ol.sw-timeline"); var events = history.locator(":scope > li");
            assertThat(events).hasCount(5);
            assertThat(history.locator(".sw-timeline-title")).hasText(new String[]{"Customer created", "Proposal reviewed", "Delivery failed", "Payment received", "Account note"});
            assertThat(history.locator("time").first()).hasAttribute("datetime", "2026-11-01T09:30");
            assertThat(events.last().locator("time")).hasCount(0);
            assertThat(events.first().evaluate("n=>getComputedStyle(n,'::after').content")).isEqualTo("\"\"");
            assertThat(events.last().evaluate("n=>getComputedStyle(n,'::after').content")).isEqualTo("none");
            var contact = details.getByRole(AriaRole.LINK, new Locator.GetByRoleOptions().setName("jordan@example.com"));
            assertThat(contact).hasAttribute("href", "mailto:jordan@example.com"); contact.focus(); contact.press("Tab");
            assertThat(p.getByRole(AriaRole.BUTTON, new Page.GetByRoleOptions().setName("Record payment").setExact(true))).isFocused();
            var name = p.getByLabel("Customer name", new Page.GetByLabelOptions().setExact(true)); String inputId = name.getAttribute("id");
            String updated = "Customer <North&South> " + "unbroken-reference-".repeat(10);
            name.fill(updated); p.getByRole(AriaRole.BUTTON, new Page.GetByRoleOptions().setName("Save with Ajax").setExact(true)).click();
            assertThat(details.locator("dd").first()).hasText(updated); idle(p); assertThat(name).hasAttribute("id", inputId);
            assertThat(compact.locator("dd").first()).hasText(updated);
            assertThat(history.locator(".sw-timeline-title").last()).hasText("Customer updated");
            assertThat(details.locator("script")).hasCount(0);
            p.getByRole(AriaRole.BUTTON, new Page.GetByRoleOptions().setName("Record payment").setExact(true)).click();
            assertThat(details.locator("dd").nth(3)).hasText("48,350.1254"); idle(p);
            assertThat(history.locator(".sw-timeline-title").last()).hasText("Payment received");
            p.getByRole(AriaRole.BUTTON, new Page.GetByRoleOptions().setName("Clear history").setExact(true)).click();
            assertThat(events).hasCount(0); assertThat(p.getByText("No activity yet", new Page.GetByTextOptions().setExact(true))).isVisible(); idle(p);
            p.getByRole(AriaRole.BUTTON, new Page.GetByRoleOptions().setName("Restore sample history").setExact(true)).click();
            assertThat(events).hasCount(5); idle(p); assertThat(p.getByText("No activity yet", new Page.GetByTextOptions().setExact(true))).hasCount(0);
            audit(p); assertThat(errors).isEmpty(); assertThat(p.locator(".sw-description-list [style], .sw-timeline [style]")).hasCount(0);
            p.locator("section[aria-labelledby=details-heading]").screenshot(new Locator.ScreenshotOptions().setPath(Path.of("target/record-display-screenshots",engine+"-"+skin+"-"+mode+"-details.png")));
            p.locator("section[aria-labelledby=history-heading]").screenshot(new Locator.ScreenshotOptions().setPath(Path.of("target/record-display-screenshots",engine+"-"+skin+"-"+mode+"-history.png")));
        }
    }
    static Stream<Arguments> locales() {
        return Stream.of("chromium", "firefox").flatMap(engine -> Stream.of("en-US", "de-DE", "fr-FR", "es-ES", "it-IT", "ja-JP", "nl-NL", "pt-PT", "hr-HR").map(locale -> Arguments.of(engine, locale)));
    }
    @ParameterizedTest @MethodSource("locales")
    void nativeSubmissionAndDetailsRemainUsableWithoutScriptInEveryLocale(String engine, String locale) {
        try (var context = browser(engine).newContext(new Browser.NewContextOptions().setJavaScriptEnabled(false)); var p = context.newPage()) {
            p.navigate(url("nova", "light", locale));
            p.getByLabel("Customer name", new Page.GetByLabelOptions().setExact(true)).fill("Native customer");
            p.getByRole(AriaRole.BUTTON, new Page.GetByRoleOptions().setName("Save normally").setExact(true)).click();
            assertThat(p.locator("dl.sw-description-list").first().locator("dd").first()).hasText("Native customer");
            assertThat(p.locator(".sw-timeline-title").last()).hasText("Customer updated");
            assertThat(p.locator(".sw-timeline time").first()).hasAttribute("datetime", "2026-11-01T09:30");
            assertThat(p.locator("html")).hasAttribute("lang", locale);
            assertThat(p.getByRole(AriaRole.LINK, new Page.GetByRoleOptions().setName("jordan@example.com"))).hasAttribute("href", "mailto:jordan@example.com");
        }
    }
    static Stream<Arguments> embedded() {
        return Stream.of("chromium","firefox").flatMap(engine -> Stream.of("vega","sera","sketch").map(skin -> Arguments.of(engine,skin)));
    }
    @ParameterizedTest @MethodSource("embedded")
    void embeddedRtlForcedColorsAndPrintingRetainReadableDetailsAndMarkers(String engine, String skin) {
        ShadwickSettings.get(app).setBundle(Bundle.EMBEDDED);
        try (var context = browser(engine).newContext(new Browser.NewContextOptions().setViewportSize(320,1000)); var p = context.newPage()) {
            var errors = errors(p); p.navigate(url(skin,"dark","en-US")); p.locator("html").evaluate("n=>n.dir='rtl'"); audit(p);
            var first = p.locator(".sw-timeline-event").first();
            assertThat(first.evaluate("n=>{let r=n.getBoundingClientRect();return getComputedStyle(n).direction==='rtl'&&parseFloat(getComputedStyle(n).paddingRight)>0;}")).isEqualTo(true);
            p.emulateMedia(new Page.EmulateMediaOptions().setForcedColors(ForcedColors.ACTIVE));
            assertThat(first.evaluate("n=>getComputedStyle(n,'::before').forcedColorAdjust")).isEqualTo("none");
            assertThat(first.evaluate("n=>getComputedStyle(n,'::before').borderStyle")).isEqualTo("solid");
            assertThat(first.evaluate("n=>getComputedStyle(n,'::after').borderInlineStartStyle")).isEqualTo(skin.equals("sketch") ? "dashed" : "solid");
            p.locator("section[aria-labelledby=history-heading]").screenshot(new Locator.ScreenshotOptions().setPath(Path.of("target/record-display-screenshots",engine+"-"+skin+"-forced-rtl.png")));
            p.emulateMedia(new Page.EmulateMediaOptions().setForcedColors(ForcedColors.NONE).setMedia(Media.PRINT));
            assertThat(first).hasCSS("break-inside","avoid");
            assertThat(first.evaluate("n=>getComputedStyle(n,'::before').backgroundColor")).isEqualTo("rgba(0, 0, 0, 0)");
            assertThat(p.locator("dl.sw-description-list").first().locator("dt")).hasCount(6);
            assertThat(errors).isEmpty();
        } finally { ShadwickSettings.get(app).setBundle(Bundle.STANDALONE); }
    }
}
