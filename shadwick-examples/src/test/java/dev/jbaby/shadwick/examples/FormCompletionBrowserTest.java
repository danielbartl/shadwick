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
class FormCompletionBrowserTest {
    static Playwright playwright;
    static Browser chromium, firefox;
    @LocalServerPort int port;
    @Autowired WebApplication app;
    @BeforeAll static void start() throws Exception {
        Files.createDirectories(Path.of("target/form-completion-screenshots"));
        playwright = Playwright.create(); chromium = playwright.chromium().launch(); firefox = playwright.firefox().launch();
    }
    @AfterAll static void stop() { playwright.close(); }
    @BeforeEach void markup() { app.getMarkupSettings().setStripWicketTags(true); }
    @AfterEach void restoreMarkup() { app.getMarkupSettings().setStripWicketTags(false); }
    Browser browser(String engine) { return engine.equals("chromium") ? chromium : firefox; }
    String url(String skin, String mode, String lang) { return "http://localhost:" + port + "/form-completion?skin=" + skin + "&mode=" + mode + "&lang=" + lang; }
    List<String> errors(Page p) {
        var result = new ArrayList<String>(); p.onPageError(result::add);
        p.onConsoleMessage(m -> { if (m.type().equals("error")) result.add(m.text()); }); return result;
    }
    void audit(Page p) {
        assertThat(p.evaluate("document.documentElement.scrollWidth <= innerWidth + 1")).as("Overflow: %s", p.evaluate("[...document.querySelectorAll('body *')].filter(n=>{let r=n.getBoundingClientRect();return r.width&& (r.right>innerWidth+1 || r.left < -1)}).slice(0,15).map(n=>[n.tagName,n.className,n.getBoundingClientRect().width])")).isEqualTo(true);
        assertThat(new AxeBuilder(p).withTags(List.of("wcag2a", "wcag2aa", "wcag21a", "wcag21aa", "wcag22aa")).analyze().getViolations()).isEmpty();
    }
    static Stream<Arguments> skins() {
        return Stream.of("chromium", "firefox").flatMap(engine -> Arrays.stream(Skin.values()).flatMap(skin -> Stream.of(
                Arguments.of(engine, skin.name().toLowerCase(Locale.ROOT), "light", 1440),
                Arguments.of(engine, skin.name().toLowerCase(Locale.ROOT), "dark", 390))));
    }
    @ParameterizedTest @MethodSource("skins")
    void composedFieldsValidateSubmitResetAndDisableAcrossSkins(String engine, String skin, String mode, int width) {
        try (var context = browser(engine).newContext(new Browser.NewContextOptions().setViewportSize(width, 1000).setReducedMotion(ReducedMotion.REDUCE)); var p = context.newPage()) {
            var errors = errors(p); p.navigate(url(skin, mode, "en-US"));
            var form = p.getByRole(AriaRole.FORM, new Page.GetByRoleOptions().setName("Ajax quote"));
            var budget = form.getByLabel("Budget", new Locator.GetByLabelOptions().setExact(true));
            var range = form.getByRole(AriaRole.SLIDER); assertThat(range).hasAttribute("min", "0");
            range.focus(); range.press("End"); assertThat(range).hasValue("10"); range.press("ArrowLeft"); assertThat(range).hasValue("9");
            range.press("Home"); assertThat(range).hasValue("0");
            form.getByRole(AriaRole.BUTTON, new Locator.GetByRoleOptions().setName("Reset inputs")).click(); assertThat(range).hasValue("5");
            budget.fill("12,34.50"); var budgetId = budget.getAttribute("id");
            form.getByRole(AriaRole.BUTTON, new Locator.GetByRoleOptions().setName("Save quote")).click();
            assertThat(budget).hasAttribute("aria-invalid", "true"); assertThat(budget).hasAttribute("id", budgetId);
            assertThat(form.locator(".sw-field-error").filter(new Locator.FilterOptions().setHasText("valid amount"))).hasCount(1);
            budget.fill("1234.565"); form.getByLabel("Tax rate", new Locator.GetByLabelOptions().setExact(true)).fill("7.5%");
            form.getByLabel("Project code", new Locator.GetByLabelOptions().setExact(true)).fill("QUOTE");
            form.getByRole(AriaRole.BUTTON, new Locator.GetByRoleOptions().setName("Save quote")).click();
            assertThat(form.locator("p.ex-muted[role=status]")).containsText("Saved budget 1234.56; tax 7.5%; priority 5; reference QUOTE-1.");
            form.getByText("Toggle section", new Locator.GetByTextOptions().setExact(true)).click(); assertThat(range).isDisabled(); assertThat(budget).isDisabled();
            form.getByText("Toggle section", new Locator.GetByTextOptions().setExact(true)).click(); assertThat(range).isEnabled();
            assertThat(form.locator("fieldset.sw-field-set").first().evaluate("n=>parseFloat(getComputedStyle(n).gap)>0")).isEqualTo(true);
            audit(p); assertThat(errors).isEmpty(); assertThat(form.locator("[style]")).hasCount(0);
            form.screenshot(new Locator.ScreenshotOptions().setPath(Path.of("target/form-completion-screenshots", engine + "-" + skin + "-" + mode + ".png")));
        }
    }
    @ParameterizedTest @ValueSource(strings={"chromium", "firefox"})
    void compoundValidationReplacementAndSerializationKeepAriaCurrent(String engine) {
        try (var context = browser(engine).newContext(); var p = context.newPage()) {
            var errors = errors(p);
            p.navigate(url("vega", "light", "en-US"));
            var form = p.getByRole(AriaRole.FORM, new Page.GetByRoleOptions().setName("Ajax quote"));
            var code = form.getByLabel("Project code", new Locator.GetByLabelOptions().setExact(true)); code.fill("x");
            form.getByRole(AriaRole.BUTTON, new Locator.GetByRoleOptions().setName("Save quote")).click();
            assertThat(code).hasAttribute("aria-invalid", "true");
            assertThat(form.locator(".sw-custom-field .sw-field-error")).containsText("minimum");
            for (int i = 0; i < 3; i++) {
                String before = form.getByLabel("Revision", new Locator.GetByLabelOptions().setExact(true)).getAttribute("id");
                form.getByText("Replace revision", new Locator.GetByTextOptions().setExact(true)).click();
                assertThat(form.getByLabel("Revision", new Locator.GetByLabelOptions().setExact(true))).not().hasAttribute("id", before);
                assertThat(form.getByLabel("Revision", new Locator.GetByLabelOptions().setExact(true))).hasValue("2");
                // Markup becomes visible before Wicket finishes the Ajax response's replacement/focus steps.
                p.waitForCondition(() -> (Boolean) p.evaluate("Object.keys(Wicket.channelManager.channels).length === 0"));
                assertThat(form.getByLabel("Revision", new Locator.GetByLabelOptions().setExact(true)).evaluate("n=>{const ids=n.getAttribute('aria-describedby').split(/\\s+/);return new Set(ids).size===ids.length&&ids.every(id=>document.getElementById(id));}")).isEqualTo(true);
            }
            code.fill("FIXED");
            form.getByRole(AriaRole.BUTTON, new Locator.GetByRoleOptions().setName("Save quote")).click();
            assertThat(form.locator("p.ex-muted[role=status]")).containsText("reference FIXED-2");
            assertThat(code).hasAttribute("aria-invalid", "false");
            audit(p); assertThat(errors).isEmpty();
        }
    }
    static Stream<Arguments> locales() {
        return Stream.of("chromium", "firefox").flatMap(engine -> Stream.of("en-US", "de-DE", "fr-FR", "es-ES", "it-IT", "ja-JP", "nl-NL", "pt-PT", "hr-HR").map(locale -> Arguments.of(engine, locale)));
    }
    @ParameterizedTest @MethodSource("locales")
    void nativeNoScriptSubmissionUsesTheSelectedLocale(String engine, String locale) {
        try (var context = browser(engine).newContext(new Browser.NewContextOptions().setJavaScriptEnabled(false)); var p = context.newPage()) {
            p.navigate(url("nova", "light", locale)); var form = p.getByRole(AriaRole.FORM, new Page.GetByRoleOptions().setName("Normal quote"));
            String amount = locale.startsWith("en") || locale.startsWith("ja") ? "1234.50" : "1234,50";
            form.getByLabel("Budget", new Locator.GetByLabelOptions().setExact(true)).fill(amount);
            form.getByRole(AriaRole.SLIDER).focus(); form.getByRole(AriaRole.SLIDER).press("End");
            form.getByRole(AriaRole.BUTTON, new Locator.GetByRoleOptions().setName("Save quote")).click();
            assertThat(p.getByRole(AriaRole.FORM, new Page.GetByRoleOptions().setName("Normal quote"))
                    .locator("p.ex-muted[role=status]")).containsText("Saved budget 1234.50; tax 19%; priority 10;");
            assertThat(p.locator("html")).hasAttribute("lang", locale);
        }
    }
    @ParameterizedTest @ValueSource(strings={"chromium", "firefox"})
    void forcedColorsEmbeddedAndRtlRetainNativeControls(String engine) {
        var settings = ShadwickSettings.get(app); settings.setBundle(Bundle.EMBEDDED);
        try (var context = browser(engine).newContext(new Browser.NewContextOptions().setViewportSize(320, 950)); var p = context.newPage()) {
            var errors = errors(p); p.navigate(url("sketch", "dark", "de-DE")); p.locator("html").evaluate("n=>n.dir='rtl'");
            var form = p.getByRole(AriaRole.FORM, new Page.GetByRoleOptions().setName("Normal quote")); var range = form.getByRole(AriaRole.SLIDER);
            audit(p);
            // Axe uses authored colors rather than the browser's forced-color paint mapping.
            // Audit ordinary embedded RTL, then check the actual system controls separately.
            p.emulateMedia(new Page.EmulateMediaOptions().setForcedColors(ForcedColors.ACTIVE));
            range.focus(); range.press("ArrowLeft"); assertThat(range).hasValue("6"); range.press("Home"); assertThat(range).hasValue("0");
            assertThat(errors).isEmpty(); assertThat(range).hasCSS("forced-color-adjust", "auto");
            assertThat(range.evaluate("n=>matchMedia('(forced-colors: active)').matches")).isEqualTo(true);
            form.screenshot(new Locator.ScreenshotOptions().setPath(Path.of("target/form-completion-screenshots", engine + "-forced-colors-rtl.png")));
        } finally { settings.setBundle(Bundle.STANDALONE); }
    }
}
