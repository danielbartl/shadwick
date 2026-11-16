package dev.jbaby.shadwick.examples;

import com.deque.html.axecore.playwright.AxeBuilder;
import com.microsoft.playwright.*;
import com.microsoft.playwright.options.*;
import dev.jbaby.shadwick.Skin;
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

/** Side-by-side and responsive field layout, field groups and the form error summary, in real browsers. */
@Tag("browser")
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class FormLayoutBrowserTest {
    static Playwright playwright;
    static Browser chromium, firefox;
    @LocalServerPort int port;
    @Autowired WebApplication app;

    @BeforeAll static void start() throws Exception {
        Files.createDirectories(Path.of("target/form-layout-screenshots"));
        playwright = Playwright.create();
        chromium = playwright.chromium().launch();
        firefox = playwright.firefox().launch();
    }
    @AfterAll static void stop() { playwright.close(); }
    @BeforeEach void markup() { app.getMarkupSettings().setStripWicketTags(true); }
    @AfterEach void restore() { app.getMarkupSettings().setStripWicketTags(false); }

    Browser browser(String engine) { return engine.equals("chromium") ? chromium : firefox; }
    String url(String skin, String mode, String lang) { return "http://localhost:" + port + "/form?skin=" + skin + "&mode=" + mode + "&lang=" + lang; }
    void idle(Page p) { p.waitForCondition(() -> (Boolean) p.evaluate("Object.keys(Wicket.channelManager.channels).length===0")); }
    List<String> errors(Page p) {
        var result = new ArrayList<String>();
        p.onPageError(result::add);
        p.onConsoleMessage(m -> { if (m.type().equals("error")) result.add(m.text()); });
        return result;
    }
    void audit(Page p) {
        assertThat(p.evaluate("document.documentElement.scrollWidth <= innerWidth + 1")).as("No horizontal overflow").isEqualTo(true);
        assertThat(new AxeBuilder(p).withTags(List.of("wcag2a", "wcag2aa", "wcag21a", "wcag21aa", "wcag22aa")).analyze().getViolations()).isEmpty();
    }
    /** Whether a field's label sits beside its control (same row) rather than above it. */
    boolean besideControl(Locator field) {
        return (Boolean) field.evaluate("f => { const l = f.querySelector(':scope > label').getBoundingClientRect(),"
                + " c = f.querySelector(':scope > input, :scope > textarea').getBoundingClientRect(); return c.top < l.bottom && (c.left >= l.right - 1 || c.right <= l.left + 1); }");
    }

    static Stream<Arguments> skins() {
        List<String> selected = Arrays.asList(System.getProperty("shadwick.browser.skins", "vega,lyra,nova,maia,mira,luma,rhea,sera,sketch").split(","));
        return Stream.of("chromium", "firefox").flatMap(engine -> Arrays.stream(Skin.values())
                .map(skin -> skin.name().toLowerCase(Locale.ROOT)).filter(selected::contains)
                .flatMap(skin -> Stream.of(Arguments.of(engine, skin, "light", 1440), Arguments.of(engine, skin, "dark", 320))));
    }

    @ParameterizedTest @MethodSource("skins")
    void responsiveFieldsSitBesideTheirControlInAWideGroupAndStackInANarrowOne(String engine, String skin, String mode, int width) {
        try (var context = browser(engine).newContext(new Browser.NewContextOptions().setViewportSize(width, 1000)); var p = context.newPage()) {
            var errors = errors(p);
            p.navigate(url(skin, mode, "en-US"));
            Locator form = p.locator("form.ex-form-wide");
            assertThat(form).hasClass(java.util.regex.Pattern.compile("sw-field-group"));
            Locator displayName = form.locator(".sw-field").first();
            assertThat(displayName).hasAttribute("data-orientation", "responsive");
            form.scrollIntoViewIfNeeded();
            assertThat(p.locator("[style]")).hasCount(0);
            if (width >= 1000) {
                assertThat(besideControl(displayName)).as("side by side in a wide group").isTrue();
                // The description stays under the label, in the first column
                assertThat(displayName.evaluate("f => { const l = f.querySelector(':scope > label').getBoundingClientRect(), d = f.querySelector(':scope > .sw-field-description').getBoundingClientRect(); return d.top >= l.bottom - 1 && Math.abs(d.left - l.left) < 2; }")).isEqualTo(true);
                p.evaluate("document.documentElement.dir = 'rtl'");
                assertThat(displayName.evaluate("f => f.querySelector(':scope > label').getBoundingClientRect().left > f.querySelector(':scope > input').getBoundingClientRect().left")).as("mirrored in RTL").isEqualTo(true);
                p.evaluate("document.documentElement.dir = 'ltr'");
                // Narrowing the group itself, not the window, stacks them
                // Through a constructable stylesheet: the page's strict CSP rightly blocks an injected <style>
                form.evaluate("f => { f.classList.add('probe-narrow'); const sheet = new CSSStyleSheet();"
                        + " sheet.replaceSync('.probe-narrow { max-width: 20rem !important; }');"
                        + " document.adoptedStyleSheets = [...document.adoptedStyleSheets, sheet]; }");
                assertThat(besideControl(displayName)).as("stacked in a narrow group").isFalse();
                form.evaluate("f => f.classList.remove('probe-narrow')");
            } else {
                assertThat(besideControl(displayName)).as("stacked on a phone").isFalse();
            }
            p.screenshot(new Page.ScreenshotOptions().setPath(Path.of("target/form-layout-screenshots", "layout-" + engine + "-" + skin + "-" + mode + ".png")).setFullPage(false));
            audit(p);
            assertThat(errors).isEmpty();
        }
    }

    @ParameterizedTest @MethodSource("skins")
    void theErrorSummaryListsTheFormsErrorsTakesFocusAndLinksToTheFields(String engine, String skin, String mode, int width) {
        try (var context = browser(engine).newContext(new Browser.NewContextOptions().setViewportSize(width, 1000)); var p = context.newPage()) {
            var errors = errors(p);
            p.navigate(url(skin, mode, "en-US"));
            Locator form = p.locator("form[aria-labelledby=summary-heading]");
            form.getByLabel("Password", new Locator.GetByLabelOptions().setExact(true)).fill("one");
            form.getByLabel("Repeat password").fill("two");
            form.getByRole(AriaRole.BUTTON, new Locator.GetByRoleOptions().setName("Create account").setExact(true)).click();
            p.waitForLoadState();
            form = p.locator("form[aria-labelledby=summary-heading]");
            Locator summary = form.locator(".sw-form-errors");
            assertThat(summary).isVisible();
            assertThat(summary).isFocused();
            assertThat(summary.locator(".sw-alert-title")).hasText("There is a problem");
            assertThat(summary.getByRole(AriaRole.HEADING, new Locator.GetByRoleOptions().setLevel(3))).hasText("There is a problem");
            Locator items = summary.locator("li");
            assertThat(items).hasCount(2);
            assertThat(items.first()).hasText("The two passwords don't match.");
            // The field's error links to it; the field still shows it too
            Locator link = items.nth(1).getByRole(AriaRole.LINK);
            assertThat(link).containsText("Email");
            assertThat(form.locator(".sw-field-error").first()).containsText("Email");
            assertThat(p.locator("[style]")).hasCount(0);
            p.screenshot(new Page.ScreenshotOptions().setPath(Path.of("target/form-layout-screenshots", "summary-" + engine + "-" + skin + "-" + mode + ".png")));
            audit(p);
            link.click();
            assertThat(form.getByLabel("Email", new Locator.GetByLabelOptions().setExact(true))).isFocused();

            // Over Ajax: one remaining error, as text without a bullet, and the focus moves to the summary again
            form.getByLabel("Email", new Locator.GetByLabelOptions().setExact(true)).fill("ada@example.com");
            form.getByLabel("Password", new Locator.GetByLabelOptions().setExact(true)).fill("one");
            form.getByLabel("Repeat password").fill("two");
            form.getByRole(AriaRole.BUTTON, new Locator.GetByRoleOptions().setName("Create account (Ajax)")).click();
            idle(p);
            assertThat(summary.locator("li")).hasCount(1);
            assertThat(summary.locator("li").evaluate("li => getComputedStyle(li).listStyleType")).isEqualTo("none");
            assertThat(summary).isFocused();

            form.getByLabel("Password", new Locator.GetByLabelOptions().setExact(true)).fill("same");
            form.getByLabel("Repeat password").fill("same");
            form.getByRole(AriaRole.BUTTON, new Locator.GetByRoleOptions().setName("Create account (Ajax)")).click();
            idle(p);
            assertThat(form.getByText("Account created.")).isVisible();
            assertThat(form.locator(".sw-form-errors")).hasCount(0);
            assertThat(errors).isEmpty();
        }
    }

    @ParameterizedTest @CsvSource({"chromium,de-DE,Es ist ein Problem aufgetreten", "firefox,ja-JP,問題があります", "chromium,hr-HR,Postoji problem"})
    void theSummarysTitleFollowsTheLocale(String engine, String locale, String title) {
        try (var context = browser(engine).newContext(); var p = context.newPage()) {
            p.navigate(url("vega", "light", locale));
            Locator form = p.locator("form[aria-labelledby=summary-heading]");
            form.locator("input[type=password]").first().fill("one");
            form.locator("input[type=password]").nth(1).fill("two");
            form.locator("button[type=submit]").first().click();
            p.waitForLoadState();
            assertThat(p.locator(".sw-form-errors .sw-alert-title")).hasText(title);
        }
    }
}
