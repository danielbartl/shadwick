package dev.jbaby.shadwick.examples;

import com.deque.html.axecore.playwright.AxeBuilder;
import com.deque.html.axecore.results.CheckedNode;
import com.deque.html.axecore.results.Rule;
import com.microsoft.playwright.Browser;
import com.microsoft.playwright.BrowserContext;
import com.microsoft.playwright.BrowserType;
import com.microsoft.playwright.Locator;
import com.microsoft.playwright.Page;
import com.microsoft.playwright.Playwright;
import com.microsoft.playwright.options.AriaRole;
import com.microsoft.playwright.options.ForcedColors;
import com.microsoft.playwright.options.FilePayload;
import org.apache.wicket.protocol.http.WebApplication;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static com.microsoft.playwright.assertions.PlaywrightAssertions.assertThat;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * The showcase in a real browser, in every skin, light and dark: no accessibility violations axe-core can find, and
 * no errors in the console, such as a style or script Wicket's strict Content Security Policy blocked. The dialogs are
 * checked open, and opening, validating and closing them is driven as a user would. Run with
 * {@code ./mvnw verify -pl shadwick-examples -am -Pbrowser}. Playwright downloads its Chromium the first time;
 * {@code BROWSER_EXECUTABLE} points it at another one instead.
 */
@Tag("browser")
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class ShowcaseBrowserTest {

    // WCAG 2.2 A and AA, as axe-core tags its rules
    private static final List<String> WCAG = List.of("wcag2a", "wcag2aa", "wcag21a", "wcag21aa", "wcag22aa");

    private static final List<String> PAGES = List.of("", "buttons", "form", "inputs", "choices", "selections", "dates", "groups", "uploads", "dialog", "overlays", "popups", "menus", "toasts", "shell", "sidebar", "display", "feedback");
    private static final List<String> SKINS = List.of("vega", "lyra");
    private static final List<String> MODES = List.of("light", "dark");

    private static Playwright playwright;
    private static Browser browser;

    @LocalServerPort
    private int port;

    @Autowired
    private WebApplication wicketApp;

    // The markup a deployed application sends: development mode keeps Wicket's own tags
    @BeforeEach
    void stripWicketTags() {
        wicketApp.getMarkupSettings().setStripWicketTags(true);
    }

    @AfterEach
    void keepWicketTags() {
        wicketApp.getMarkupSettings().setStripWicketTags(false);
    }

    @BeforeAll
    static void launch() {
        playwright = Playwright.create();
        BrowserType.LaunchOptions options = new BrowserType.LaunchOptions();
        String executable = System.getenv("BROWSER_EXECUTABLE");
        if (executable != null) {
            options.setExecutablePath(Path.of(executable));
        }
        browser = playwright.chromium().launch(options);
    }

    @AfterAll
    static void close() {
        playwright.close();
    }

    static Stream<Arguments> pagesInEverySkinAndMode() {
        return PAGES.stream().flatMap(page -> SKINS.stream().flatMap(skin -> MODES.stream().map(mode -> Arguments.of(page, skin, mode))));
    }

    static Stream<Arguments> everySkinAndMode() {
        return SKINS.stream().flatMap(skin -> MODES.stream().map(mode -> Arguments.of(skin, mode)));
    }

    private String url(String page, String skin, String mode) {
        return "http://localhost:" + port + "/" + page + "?skin=" + skin + "&mode=" + mode;
    }

    /** Opens the page in a fresh browser session, runs the steps, and checks it with axe-core and for console errors. */
    private void check(String page, String skin, String mode, Consumer<Page> steps) {
        check(page, skin, mode, new Browser.NewContextOptions(), steps);
    }

    private void check(String page, String skin, String mode, Browser.NewContextOptions options, Consumer<Page> steps) {
        try (BrowserContext context = browser.newContext(options); Page tab = context.newPage()) {
            List<String> errors = new ArrayList<>();
            tab.onConsoleMessage(message -> {
                if ("error".equals(message.type())) {
                    errors.add(message.text());
                }
            });
            tab.onPageError(errors::add);
            tab.navigate(url(page, skin, mode));
            assertThat(tab.locator("html")).hasClass("dark".equals(mode) ? "dark" : "");
            assertThat(tab.locator("link[href*='shadwick-" + skin + "']")).hasCount(1);

            steps.accept(tab);

            tab.evaluate("() => Promise.all(document.getAnimations().filter(a => a.effect && a.effect.getTiming().iterations !== Infinity)"
                    + ".map(a => a.finished.catch(() => {})))");

            String where = "/" + page + " in " + skin + ", " + mode;
            // axe injects scripts; no-JavaScript navigation is checked separately from the normal page audits.
            if (!Boolean.FALSE.equals(options.javaScriptEnabled)) {
                assertThat(describe(new AxeBuilder(tab).withTags(WCAG).analyze().getViolations()))
                        .as("axe-core's WCAG violations on " + where).isEmpty();
            }
            assertThat(errors).as("console errors on " + where).isEmpty();
        }
    }

    @ParameterizedTest(name = "/{0} in {1}, {2}")
    @MethodSource("pagesInEverySkinAndMode")
    void noAccessibilityViolationsAndNoConsoleErrors(String page, String skin, String mode) {
        check(page, skin, mode, tab -> {
        });
    }

    @ParameterizedTest(name = "{0}, {1}")
    @MethodSource("everySkinAndMode")
    void aDialogOpensInTheBrowserAndClosesWithEscape(String skin, String mode) {
        check("dialog", skin, mode, tab -> {
            tab.getByRole(AriaRole.BUTTON, new Page.GetByRoleOptions().setName("About Shadwick")).click();
            Locator dialog = tab.getByRole(AriaRole.DIALOG, new Page.GetByRoleOptions().setName("About Shadwick"));
            assertThat(dialog).isVisible();
            // While it is open: the page behind it is inert, the dialog is what axe checks
            dialog.evaluate("dialog => Promise.all(dialog.getAnimations({subtree: true}).map(animation => animation.finished))");
            assertThat(describe(new AxeBuilder(tab).withTags(WCAG).analyze().getViolations())).as("with the dialog open").isEmpty();

            tab.keyboard().press("Escape");
            assertThat(dialog).isHidden();
        });
    }

    @ParameterizedTest(name = "{0}, {1}")
    @MethodSource("everySkinAndMode")
    void aDialogOpensOverAjaxAndConfirms(String skin, String mode) {
        check("dialog", skin, mode, tab -> {
            tab.getByRole(AriaRole.BUTTON, new Page.GetByRoleOptions().setName("Delete project")).click();
            Locator dialog = tab.getByRole(AriaRole.DIALOG, new Page.GetByRoleOptions().setName("Delete the project?"));
            assertThat(dialog).isVisible();
            assertThat(dialog).hasAccessibleDescription("This cannot be undone. Every file in it goes with it.");

            dialog.getByRole(AriaRole.BUTTON, new Locator.GetByRoleOptions().setName("Delete")).click();
            assertThat(dialog).isHidden();
            assertThat(tab.getByRole(AriaRole.STATUS).filter(new Locator.FilterOptions().setHasText("deleted"))).hasText("The project was deleted.");
        });
    }

    @ParameterizedTest(name = "{0}, {1}")
    @MethodSource("everySkinAndMode")
    void aDialogWithAFormStaysOpenOnAnErrorAndKeepsTheFocus(String skin, String mode) {
        check("dialog", skin, mode, tab -> {
            tab.getByRole(AriaRole.BUTTON, new Page.GetByRoleOptions().setName("Edit profile")).click();
            Locator dialog = tab.getByRole(AriaRole.DIALOG, new Page.GetByRoleOptions().setName("Edit profile"));
            assertThat(dialog).isVisible();
            Locator name = dialog.getByLabel("Name");
            // The browser focuses the first field, not the × in the corner
            assertThat(name).isFocused();

            name.fill("");
            name.press("Enter");
            assertThat(name).hasAttribute("aria-invalid", "true");
            assertThat(name).hasAccessibleDescription("'Name' is required.");
            assertThat(dialog).isVisible();
            // Shown in place: the field keeps the focus
            assertThat(name).isFocused();
            assertThat(describe(new AxeBuilder(tab).withTags(WCAG).analyze().getViolations())).as("with an error shown").isEmpty();

            name.fill("Grace Hopper");
            dialog.getByRole(AriaRole.BUTTON, new Locator.GetByRoleOptions().setName("Save changes")).click();
            assertThat(dialog).isHidden();
            assertThat(tab.getByText("Profile: Grace Hopper <ada@example.com>")).isVisible();
        });
    }

    @ParameterizedTest(name = "{0}, {1}")
    @MethodSource("everySkinAndMode")
    void anAjaxSubmitShowsErrorsInPlaceAndKeepsWhatWasTyped(String skin, String mode) {
        check("form", skin, mode, tab -> {
            Locator form = tab.locator("form").nth(1);
            Locator email = form.getByLabel("Email");
            email.fill("not an email");
            form.getByRole(AriaRole.BUTTON, new Locator.GetByRoleOptions().setName("Save")).click();

            Locator name = form.getByLabel("Name");
            assertThat(name).hasAttribute("aria-invalid", "true");
            assertThat(email).hasAttribute("aria-invalid", "true");
            assertThat(email).hasValue("not an email");
            assertThat(email).hasAccessibleDescription("We never share it. The value of 'Email' is not a valid email address.");
            assertThat(describe(new AxeBuilder(tab).withTags(WCAG).analyze().getViolations())).as("with errors shown").isEmpty();

            name.fill("Grace Hopper");
            email.fill("grace@example.com");
            form.getByRole(AriaRole.BUTTON, new Locator.GetByRoleOptions().setName("Save")).click();
            assertThat(name).hasAttribute("aria-invalid", "false");
            assertThat(email).hasAccessibleDescription("We never share it.");
            assertThat(form.getByRole(AriaRole.STATUS).last()).hasText("Saved Grace Hopper <grace@example.com>.");
        });
    }

    @ParameterizedTest(name = "{0}, {1}")
    @MethodSource("everySkinAndMode")
    void avatarFallbacksHandleBrokenPicturesAndCachedAjaxReplacements(String skin, String mode) {
        check("display", skin, mode, tab -> {
            Locator avatar = tab.locator(".sw-avatar").first();
            Locator image = avatar.locator("img");
            assertThat(image).hasAttribute("data-status", "loaded");
            assertThat(avatar.locator(".sw-avatar-fallback")).isHidden();
            // A successful response that isn't an image triggers error without a network/console error.
            tab.route("**/broken-avatar", route -> route.fulfill(new com.microsoft.playwright.Route.FulfillOptions()
                    .setStatus(200).setContentType("text/plain").setBody("not an image")));
            image.evaluate("image => image.src = '/broken-avatar'");
            assertThat(image).hasAttribute("data-status", "error");
            assertThat(image).isHidden();
            assertThat(avatar.locator(".sw-avatar-fallback")).isVisible();
            tab.getByRole(AriaRole.BUTTON, new Page.GetByRoleOptions().setName("Refresh avatar")).click();
            assertThat(image).hasAttribute("data-status", "loaded");
            assertThat(avatar.locator(".sw-avatar-fallback")).isHidden();
        });
    }

    @ParameterizedTest(name = "{0}, {1}")
    @MethodSource("everySkinAndMode")
    void feedbackPanelsAndToastsShowMessagesOnceAndDismissWithTheKeyboard(String skin, String mode) {
        check("feedback", skin, mode, tab -> {
            tab.getByRole(AriaRole.BUTTON, new Page.GetByRoleOptions().setName("Save project")).click();
            assertThat(tab.locator(".sw-feedback")).containsText("Please fix the errors below.");
            assertThat(tab.locator(".sw-field-error")).containsText("is required.");
            assertThat(tab.locator(".sw-toast")).hasCount(0);
            tab.getByLabel("Project name").fill("Shadwick");
            tab.getByRole(AriaRole.BUTTON, new Page.GetByRoleOptions().setName("Save project")).click();
            assertThat(tab.locator(".sw-feedback")).containsText("Project saved.");

            tab.getByRole(AriaRole.BUTTON, new Page.GetByRoleOptions().setName("Show success")).click();
            assertThat(tab.locator(".sw-toast[data-level='success']")).containsText("Your changes have been saved.");
            assertThat(tab.locator(".sw-feedback")).isEmpty();
            tab.getByRole(AriaRole.BUTTON, new Page.GetByRoleOptions().setName("Show error")).click();
            assertThat(tab.locator(".sw-toast[data-variant='destructive']")).containsText("could not be saved");
            tab.getByRole(AriaRole.BUTTON, new Page.GetByRoleOptions().setName("Show literal text")).click();
            assertThat(tab.locator(".sw-toast[data-level='info']")).containsText("<strong>This is plain text.</strong>");
            assertThat(tab.locator(".sw-toast strong")).hasCount(0);
            assertThat(describe(new AxeBuilder(tab).withTags(WCAG).analyze().getViolations())).isEmpty();

            Locator dismiss = tab.getByRole(AriaRole.BUTTON, new Page.GetByRoleOptions().setName("Dismiss notification")).first();
            dismiss.focus();
            tab.keyboard().press("Enter");
            assertThat(tab.locator(".sw-toast")).hasCount(2);
            assertThat(tab.getByRole(AriaRole.BUTTON, new Page.GetByRoleOptions().setName("Dismiss notification")).first()).isFocused();
            tab.keyboard().press("Enter");
            assertThat(tab.locator(".sw-toast")).hasCount(1);
            tab.keyboard().press("Enter");
            assertThat(tab.locator("#sw-toaster")).hasCount(0);
            assertThat(tab.getByRole(AriaRole.BUTTON, new Page.GetByRoleOptions().setName("Show literal text"))).isFocused();

            tab.getByRole(AriaRole.LINK, new Page.GetByRoleOptions().setName("Full page feedback")).click();
            assertThat(tab.locator(".sw-toast")).hasCount(1);
            assertThat(tab.locator(".sw-toast")).containsText("Welcome back.");
        });
    }

    @ParameterizedTest(name = "{0}, {1}")
    @MethodSource("everySkinAndMode")
    void embeddedBundlesLeaveTheHostPagesMarginsAlone(String skin, String mode) {
        dev.jbaby.shadwick.ShadwickSettings settings = dev.jbaby.shadwick.ShadwickSettings.get(wicketApp);
        settings.setBundle(dev.jbaby.shadwick.Bundle.EMBEDDED);
        try {
            check("display", skin, mode, tab -> {
                assertThat(tab.locator("link[href*='shadwick-" + skin + "-embedded']")).hasCount(1);
                // The showcase supplies no reset. Embedded CSS must preserve the browser's body margin.
                org.assertj.core.api.Assertions.assertThat(tab.locator("body").evaluate("element => getComputedStyle(element).margin"))
                        .isEqualTo("8px");
                org.assertj.core.api.Assertions.assertThat(tab.locator(".sw-badge").first()
                        .evaluate("element => getComputedStyle(element).display")).isEqualTo("flex");
                assertThat(tab.locator(".sw-avatar-image")).hasAttribute("data-status", "loaded");
            });
        } finally {
            settings.setBundle(dev.jbaby.shadwick.Bundle.STANDALONE);
        }
    }

    @ParameterizedTest(name = "{0}, {1}")
    @MethodSource("everySkinAndMode")
    void choicesValidateAndSaveWithNormalAndAjaxSubmission(String skin, String mode) {
        check("choices", skin, mode, tab -> {
            Locator normal = tab.getByRole(AriaRole.FORM, new Page.GetByRoleOptions().setName("Normal preferences"));
            normal.getByRole(AriaRole.BUTTON, new Locator.GetByRoleOptions().setName("Save preferences")).click();
            assertThat(normal).containsText("Accept the terms to continue.");
            assertThat(normal.getByRole(AriaRole.RADIOGROUP)).hasAccessibleName("Contact preference");
            assertThat(normal.getByRole(AriaRole.RADIOGROUP)).hasAccessibleDescription("Choose one way to hear from us. 'Contact preference' is required.");
            normal.getByRole(AriaRole.CHECKBOX, new Locator.GetByRoleOptions().setName("Accept the terms")).check();
            normal.getByRole(AriaRole.SWITCH).check();
            normal.getByRole(AriaRole.RADIO, new Locator.GetByRoleOptions().setName("Email").setExact(true)).check();
            normal.getByRole(AriaRole.BUTTON).click();
            assertThat(normal.getByRole(AriaRole.STATUS).last()).hasText("Saved: terms accepted; notifications on; contact Email.");

            Locator ajax = tab.getByRole(AriaRole.FORM, new Page.GetByRoleOptions().setName("Ajax preferences"));
            Locator radio = ajax.getByRole(AriaRole.RADIO, new Locator.GetByRoleOptions().setName("Email").setExact(true));
            radio.focus();
            // Programmatic activation preserves the radio's focus while exercising the real submit handler.
            ajax.getByRole(AriaRole.BUTTON).evaluate("button => button.click()");
            assertThat(ajax).containsText("Accept the terms to continue.");
            assertThat(radio).hasAttribute("aria-invalid", "true");
            assertThat(radio).hasAccessibleDescription("Choose one way to hear from us. 'Contact preference' is required.");
            assertThat(radio).isFocused();
            assertThat(describe(new AxeBuilder(tab).withTags(WCAG).analyze().getViolations())).isEmpty();

            ajax.getByRole(AriaRole.CHECKBOX).check();
            radio.check();
            ajax.getByRole(AriaRole.BUTTON).evaluate("button => button.click()");
            assertThat(ajax.getByRole(AriaRole.STATUS).last()).hasText("Saved: terms accepted; notifications off; contact Email.");
            assertThat(radio).hasAttribute("aria-invalid", "false");
            assertThat(radio).isFocused();
            assertThat(ajax.locator(".sw-field-error")).hasText(new String[] {"", "", ""});
        });
    }

    @ParameterizedTest(name = "{0}, {1}")
    @MethodSource("everySkinAndMode")
    void switchesAndRadioChoicesUpdateOverAjaxUsingTheKeyboard(String skin, String mode) {
        check("choices", skin, mode, tab -> {
            Locator form = tab.getByRole(AriaRole.FORM, new Page.GetByRoleOptions().setName("Live preferences"));
            Locator toggle = form.getByRole(AriaRole.SWITCH);
            toggle.focus();
            tab.keyboard().press("Space");
            assertThat(toggle).isChecked();
            assertThat(form.getByRole(AriaRole.STATUS).last()).hasText("Notifications on; frequency Weekly.");
            assertThat(toggle).isFocused();
            form.getByRole(AriaRole.RADIO, new Locator.GetByRoleOptions().setName("Weekly")).focus();
            tab.keyboard().press("ArrowRight");
            assertThat(form.getByRole(AriaRole.RADIO, new Locator.GetByRoleOptions().setName("Monthly"))).isChecked();
            assertThat(form.getByRole(AriaRole.STATUS).last()).hasText("Notifications on; frequency Monthly.");
        });
    }

    @ParameterizedTest(name = "{0}, {1}")
    @MethodSource("everySkinAndMode")
    void choicesKeepNativeDisabledSemanticsAndSupportRtlAndForcedColors(String skin, String mode) {
        check("choices", skin, mode, tab -> {
            Locator disabled = tab.getByRole(AriaRole.CHECKBOX, new Page.GetByRoleOptions().setName("Managed by your workspace"));
            assertThat(disabled).isDisabled();
            assertThat(disabled).isChecked();
            assertThat(tab.getByRole(AriaRole.SWITCH, new Page.GetByRoleOptions().setName("Administrator setting"))).isDisabled();
            Locator small = tab.getByRole(AriaRole.SWITCH, new Page.GetByRoleOptions().setName("Compact switch"));
            assertThat(small).isChecked();
            assertThat(small).hasAttribute("data-size", "sm");
            tab.locator("html").evaluate("html => html.dir = 'rtl'");
            // Wait for the switch's native CSS transition after changing direction.
            assertThat(small).hasCSS("background-position-x", "0%");
            tab.emulateMedia(new Page.EmulateMediaOptions().setForcedColors(ForcedColors.ACTIVE));
            org.assertj.core.api.Assertions.assertThat(disabled.evaluate("input => getComputedStyle(input).appearance")).isEqualTo("auto");
            assertThat(tab.locator(".sw-checkbox-indicator").first()).isHidden();
            // The shared axe pass audits authored colours. Check the native forced-colour fallback above,
            // then restore the authored palette rather than mixing it with the browser's system canvas.
            tab.emulateMedia(new Page.EmulateMediaOptions().setForcedColors(ForcedColors.NONE));
        });
    }

    @ParameterizedTest(name = "{0}, {1}")
    @MethodSource("everySkinAndMode")
    void everydayInputsValidateWithoutLosingAjaxPasswordsOrFocus(String skin, String mode) {
        check("inputs", skin, mode, tab -> {
            for (String name : List.of("Normal settings", "Ajax settings")) {
                Locator form = tab.getByRole(AriaRole.FORM, new Page.GetByRoleOptions().setName(name));
                form.getByLabel("Email", new Locator.GetByLabelOptions().setExact(true)).fill("not-an-email");
                form.getByLabel("Password", new Locator.GetByLabelOptions().setExact(true)).fill("browser-only-secret");
                form.getByLabel("Website", new Locator.GetByLabelOptions().setExact(true)).fill("not-a-url");
                Locator seats = form.getByLabel("Seats", new Locator.GetByLabelOptions().setExact(true));
                seats.fill("99"); seats.focus();
                form.getByRole(AriaRole.BUTTON).evaluate("button => button.click()");
                assertThat(form.getByLabel("Email", new Locator.GetByLabelOptions().setExact(true))).hasAttribute("aria-invalid", "true");
                assertThat(seats).hasAttribute("aria-invalid", "true");
                if (name.startsWith("Ajax")) {
                    assertThat(seats).isFocused();
                    assertThat(form.getByLabel("Password", new Locator.GetByLabelOptions().setExact(true))).hasValue("browser-only-secret");
                } else {
                    assertThat(form.getByLabel("Password", new Locator.GetByLabelOptions().setExact(true))).hasValue("");
                }
                form.getByLabel("Email", new Locator.GetByLabelOptions().setExact(true)).fill("ada@example.com");
                form.getByLabel("Password", new Locator.GetByLabelOptions().setExact(true)).fill("browser-only-secret");
                form.getByLabel("Website", new Locator.GetByLabelOptions().setExact(true)).fill("https://example.com");
                form.getByLabel("Telephone", new Locator.GetByLabelOptions().setExact(true)).fill("+49 (30) 123-456");
                seats.fill("4");
                form.getByRole(AriaRole.BUTTON).click();
                assertThat(form.getByRole(AriaRole.STATUS).last()).hasText("Saved ada@example.com; seats 4.");
            }
        });
    }

    @ParameterizedTest(name = "{0}, {1}")
    @MethodSource("everySkinAndMode")
    void nativeSelectionsValidateAndKeepAjaxSelectionAndFocus(String skin, String mode) {
        check("selections", skin, mode, tab -> {
            for (String name : List.of("Normal selections", "Ajax selections")) {
                Locator form = tab.getByRole(AriaRole.FORM, new Page.GetByRoleOptions().setName(name));
                Locator team = form.getByLabel("Team", new Locator.GetByLabelOptions().setExact(true));
                Locator topics = form.getByLabel("Topics", new Locator.GetByLabelOptions().setExact(true));
                Locator email = form.getByLabel("Email", new Locator.GetByLabelOptions().setExact(true));
                form.getByRole(AriaRole.BUTTON).click();
                assertThat(team).hasAttribute("aria-invalid", "true");
                assertThat(topics).hasAttribute("aria-invalid", "true");
                assertThat(email).hasAttribute("aria-invalid", "true");
                assertThat(describe(new AxeBuilder(tab).withTags(WCAG).analyze().getViolations())).isEmpty();

                topics.selectOption(new String[]{"0", "1"});
                email.check();
                email.focus();
                assertThat(topics.locator("option:checked").first().evaluate("option => getComputedStyle(option).backgroundColor"))
                        .isNotEqualTo(topics.locator("option").last().evaluate("option => getComputedStyle(option).backgroundColor"));
                form.getByRole(AriaRole.BUTTON).evaluate("button => button.click()");
                assertThat(team).hasAttribute("aria-invalid", "true");
                assertThat(topics.locator("option:checked")).hasText(new String[]{"Releases", "Guides"});
                assertThat(email).isChecked();
                if (name.startsWith("Ajax")) assertThat(email).isFocused();

                team.selectOption("engineering");
                form.getByRole(AriaRole.BUTTON).click();
                assertThat(form.getByRole(AriaRole.STATUS).last())
                        .hasText("Saved: team Engineering; topics Releases, Guides; channels Email.");
                assertThat(team).hasAttribute("aria-invalid", "false");
                assertThat(email).hasAttribute("aria-invalid", "false");
            }
        });
    }

    @ParameterizedTest(name = "{0}, {1}")
    @MethodSource("everySkinAndMode")
    void allNativeSelectionTypesUpdateImmediatelyOverAjax(String skin, String mode) {
        check("selections", skin, mode, tab -> {
            Locator form = tab.getByRole(AriaRole.FORM, new Page.GetByRoleOptions().setName("Immediate selections"));
            Locator summary = form.getByRole(AriaRole.STATUS).last();
            assertThat(form.getByLabel("Team", new Locator.GetByLabelOptions().setExact(true)).locator("option").first())
                    .hasText("No team");
            form.getByLabel("Team", new Locator.GetByLabelOptions().setExact(true)).selectOption("design");
            assertThat(summary).hasText("Current: team Design; topics ; channels .");
            form.getByLabel("Topics", new Locator.GetByLabelOptions().setExact(true)).selectOption(new String[]{"0", "2"});
            assertThat(summary).hasText("Current: team Design; topics Releases, Events; channels .");
            Locator sms = form.getByLabel("SMS", new Locator.GetByLabelOptions().setExact(true));
            sms.focus();
            sms.press("Space");
            assertThat(summary).hasText("Current: team Design; topics Releases, Events; channels SMS.");
            assertThat(sms).isFocused();
            sms.press("Space");
            assertThat(summary).hasText("Current: team Design; topics Releases, Events; channels .");
            form.getByLabel("Topics", new Locator.GetByLabelOptions().setExact(true)).selectOption(new String[0]);
            assertThat(summary).hasText("Current: team Design; topics ; channels .");
            form.getByLabel("Team", new Locator.GetByLabelOptions().setExact(true)).selectOption("");
            assertThat(summary).hasText("Current: team none; topics ; channels .");
        });
    }

    @ParameterizedTest(name = "{0}, {1}")
    @MethodSource("everySkinAndMode")
    void nativeSelectionsSupportKeyboardAndForcedColors(String skin, String mode) {
        check("selections", skin, mode, tab -> {
            Locator form = tab.getByRole(AriaRole.FORM, new Page.GetByRoleOptions().setName("Normal selections"));
            Locator team = form.getByLabel("Team", new Locator.GetByLabelOptions().setExact(true));
            assertThat(team.locator("option").first()).hasText("Choose a team");
            team.focus();
            team.press("Home");
            team.press("ArrowDown");
            assertThat(team).hasValue("design");
            Locator topics = form.getByLabel("Topics", new Locator.GetByLabelOptions().setExact(true));
            topics.focus();
            topics.press("Home");
            topics.press("Shift+ArrowDown");
            assertThat(topics.locator("option:checked")).hasText(new String[]{"Releases", "Guides"});
            Locator email = form.getByLabel("Email", new Locator.GetByLabelOptions().setExact(true));
            email.focus();
            email.press("Space");
            assertThat(email).isChecked();
            Locator indicator = email.locator("xpath=../following-sibling::*[local-name()='svg']");
            assertThat(indicator).isVisible();
            tab.emulateMedia(new Page.EmulateMediaOptions().setForcedColors(ForcedColors.ACTIVE));
            assertThat(email).hasCSS("appearance", "auto");
            assertThat(indicator).isHidden();
            assertThat(tab.getByLabel("Managed team", new Page.GetByLabelOptions().setExact(true))).isDisabled();
            assertThat(tab.getByLabel("Managed topics", new Page.GetByLabelOptions().setExact(true))).isDisabled();
            assertThat(tab.locator(".sw-checkbox-group input:disabled")).hasCount(2);
            // Audit authored colours after checking the system-colour fallback.
            tab.emulateMedia(new Page.EmulateMediaOptions().setForcedColors(ForcedColors.NONE));
        });
    }

    @ParameterizedTest(name = "{0}, {1}")
    @MethodSource("everySkinAndMode")
    void nativeDateTimesValidateAndSubmitWithoutLosingAjaxFocus(String skin, String mode) {
        check("dates", skin, mode, tab -> {
            for (String name : List.of("Normal schedule", "Ajax schedule")) {
                Locator form = tab.getByRole(AriaRole.FORM, new Page.GetByRoleOptions().setName(name));
                Locator date = form.getByLabel("Date", new Locator.GetByLabelOptions().setExact(true));
                form.getByRole(AriaRole.BUTTON).click();
                assertThat(date).hasAttribute("aria-invalid", "true");
                assertThat(form.getByText("'Date' is required.", new Locator.GetByTextOptions().setExact(true))).isVisible();
                date.fill("2027-01-01");
                date.focus();
                form.getByRole(AriaRole.BUTTON).evaluate("button => button.click()");
                assertThat(date).hasAttribute("aria-invalid", "true");
                assertThat(form.getByRole(AriaRole.STATUS).filter(new Locator.FilterOptions().setHasText("between"))).isVisible();
                assertThat(date).hasValue("2027-01-01");
                if (name.startsWith("Ajax")) assertThat(date).isFocused();
                assertThat(describe(new AxeBuilder(tab).withTags(WCAG).analyze().getViolations())).isEmpty();

                date.fill("2026-12-24");
                form.getByLabel("Time", new Locator.GetByLabelOptions().setExact(true)).fill("14:05");
                form.getByLabel("Local date/time", new Locator.GetByLabelOptions().setExact(true)).fill("2026-12-24T14:05:30.125");
                form.getByLabel("Month", new Locator.GetByLabelOptions().setExact(true)).fill("2026-12");
                form.getByLabel("Week", new Locator.GetByLabelOptions().setExact(true)).fill("2026-W52");
                form.getByRole(AriaRole.BUTTON).click();
                assertThat(form.getByRole(AriaRole.STATUS).last()).hasText(
                        "Saved: date 2026-12-24; time 14:05; local 2026-12-24T14:05:30.125; month 2026-12; week 2026-W52.");
                assertThat(date).hasAttribute("aria-invalid", "false");
            }
            assertThat(tab.getByLabel("Time with seconds", new Page.GetByLabelOptions().setExact(true)))
                    .hasValue("10:30:15.123");
            assertThat(tab.getByLabel("Managed date", new Page.GetByLabelOptions().setExact(true))).isDisabled();
        });
    }

    @ParameterizedTest(name = "{0}, {1}")
    @MethodSource("everySkinAndMode")
    void immediateDateValidationClearsErrorsAndKeepsTheNativeInput(String skin, String mode) {
        check("dates", skin, mode, tab -> {
            Locator form = tab.getByRole(AriaRole.FORM, new Page.GetByRoleOptions().setName("Immediate date validation"));
            Locator date = form.getByLabel("Preview date", new Locator.GetByLabelOptions().setExact(true));
            Locator summary = form.getByRole(AriaRole.STATUS).last();
            date.evaluate("input => window.originalDateInput = input");
            date.fill("2027-01-01");
            assertThat(date).hasAttribute("aria-invalid", "true");
            assertThat(summary).hasText("No date selected.");
            assertThat(date).isFocused();
            assertThat(describe(new AxeBuilder(tab).withTags(WCAG).analyze().getViolations())).isEmpty();
            date.fill("2026-12-24");
            assertThat(summary).hasText("Selected: 2026-12-24.");
            assertThat(date).hasAttribute("aria-invalid", "false");
            assertThat(date).isFocused();
            date.fill("");
            assertThat(summary).hasText("No date selected.");
            assertThat(date).hasAttribute("aria-invalid", "false");
            assertThat(date.evaluate("input => input === window.originalDateInput")).isEqualTo(true);
            assertThat(date).hasCSS("color-scheme", mode);
        });
    }

    @ParameterizedTest(name = "{0}, {1}")
    @MethodSource("everySkinAndMode")
    void togglesAndInputGroupsValidateAndSaveWithoutLosingAjaxState(String skin, String mode) {
        check("groups", skin, mode, tab -> {
            for (String name : List.of("Normal composition", "Ajax composition")) {
                Locator form = tab.getByRole(AriaRole.FORM, new Page.GetByRoleOptions().setName(name));
                Locator workspace = form.getByLabel("Workspace", new Locator.GetByLabelOptions().setExact(true));
                form.getByRole(AriaRole.BUTTON).click();
                assertThat(workspace).hasAttribute("aria-invalid", "true");
                assertThat(form.getByLabel("Left", new Locator.GetByLabelOptions().setExact(true))).hasAttribute("aria-invalid", "true");
                assertThat(form.getByLabel("Bold", new Locator.GetByLabelOptions().setExact(true))).hasAttribute("aria-invalid", "true");
                assertThat(describe(new AxeBuilder(tab).withTags(WCAG).analyze().getViolations())).isEmpty();
                form.getByLabel("Highlight", new Locator.GetByLabelOptions().setExact(true)).check();
                form.getByLabel("Center", new Locator.GetByLabelOptions().setExact(true)).check();
                form.getByLabel("Bold", new Locator.GetByLabelOptions().setExact(true)).check();
                form.getByLabel("Underline", new Locator.GetByLabelOptions().setExact(true)).check();
                workspace.fill("Bad value");
                Locator weight = form.getByLabel("Weight", new Locator.GetByLabelOptions().setExact(true));
                weight.fill("0");
                workspace.focus();
                form.getByRole(AriaRole.BUTTON).evaluate("button => button.click()");
                assertThat(workspace).hasAttribute("aria-invalid", "true");
                assertThat(weight).hasAttribute("aria-invalid", "true");
                assertThat(form.getByLabel("Center", new Locator.GetByLabelOptions().setExact(true))).isChecked();
                assertThat(form.getByLabel("Bold", new Locator.GetByLabelOptions().setExact(true))).isChecked();
                if (name.startsWith("Ajax")) assertThat(workspace).isFocused();
                workspace.fill("studio");
                weight.fill("7");
                form.getByLabel("Project", new Locator.GetByLabelOptions().setExact(true)).fill("Custom project");
                form.getByLabel("Notes", new Locator.GetByLabelOptions().setExact(true)).fill("A draft with grouped addons.");
                form.getByRole(AriaRole.BUTTON).click();
                assertThat(form.getByRole(AriaRole.STATUS).last()).hasText(
                        "Saved: highlight on; alignment Center; formats Bold, Underline; workspace studio; weight 7 kg; project Custom project.");
                assertThat(workspace).hasAttribute("aria-invalid", "false");
            }
        });
    }

    @ParameterizedTest(name = "{0}, {1}")
    @MethodSource("everySkinAndMode")
    void nativeToggleKeysUpdateModelsAndKeepFocus(String skin, String mode) {
        check("groups", skin, mode, tab -> {
            Locator form = tab.getByRole(AriaRole.FORM, new Page.GetByRoleOptions().setName("Immediate formatting"));
            Locator summary = form.getByRole(AriaRole.STATUS).last();
            Locator highlight = form.getByLabel("Highlight", new Locator.GetByLabelOptions().setExact(true));
            highlight.focus(); highlight.press("Space");
            assertThat(summary).hasText("Live: highlight on; alignment Left; formats none.");
            assertThat(highlight).isFocused();
            Locator left = form.getByLabel("Left", new Locator.GetByLabelOptions().setExact(true));
            left.focus(); left.press("ArrowDown");
            Locator center = form.getByLabel("Center", new Locator.GetByLabelOptions().setExact(true));
            assertThat(center).isChecked();
            assertThat(center).isFocused();
            assertThat(summary).hasText("Live: highlight on; alignment Center; formats none.");
            Locator bold = form.getByLabel("Bold", new Locator.GetByLabelOptions().setExact(true));
            bold.focus(); bold.press("Space");
            assertThat(summary).hasText("Live: highlight on; alignment Center; formats Bold.");
            bold.press("Space");
            assertThat(summary).hasText("Live: highlight on; alignment Center; formats none.");
            assertThat(bold).isFocused();
        });
    }

    @ParameterizedTest(name = "{0}, {1}")
    @MethodSource("everySkinAndMode")
    void inputGroupAddonsFocusControlsAndKeepButtonActionsAfterAjaxReplacement(String skin, String mode) {
        check("groups", skin, mode, tab -> {
            Locator form = tab.getByRole(AriaRole.FORM, new Page.GetByRoleOptions().setName("Normal composition"));
            Locator workspace = form.getByLabel("Workspace", new Locator.GetByLabelOptions().setExact(true));
            form.getByText("https://", new Locator.GetByTextOptions().setExact(true)).click();
            assertThat(workspace).isFocused();
            form.getByText(".example", new Locator.GetByTextOptions().setExact(true)).click();
            assertThat(workspace).isFocused();
            assertThat(workspace).hasAccessibleName("Workspace");
            assertThat(workspace).hasAccessibleDescription(java.util.regex.Pattern.compile("https://.*\\.example"));
            Locator query = tab.getByLabel("Search library", new Page.GetByLabelOptions().setExact(true));
            Locator prefix = query.locator("xpath=..").locator(".sw-input-group-addon").first();
            prefix.click();
            assertThat(query).isFocused();
            query.fill("Find components");
            Locator clear = tab.getByRole(AriaRole.BUTTON, new Page.GetByRoleOptions().setName("Clear search"));
            clear.click();
            assertThat(query).hasValue("");
            assertThat(clear).isFocused();
            query.evaluate("input => window.originalGroupInput = input");
            tab.getByRole(AriaRole.BUTTON, new Page.GetByRoleOptions().setName("Refresh search group")).click();
            assertThat(query).hasValue("Refreshed components");
            assertThat(query.evaluate("input => input === window.originalGroupInput")).isEqualTo(false);
            query.locator("xpath=..").locator(".sw-input-group-addon").first().click();
            assertThat(query).isFocused();
            assertThat(tab.getByLabel("Managed weight", new Page.GetByLabelOptions().setExact(true))).isDisabled();
        });
    }

    @ParameterizedTest(name = "{0}, {1}")
    @MethodSource("everySkinAndMode")
    void togglesKeepJoinedRtlBordersAndForcedColorStates(String skin, String mode) {
        check("groups", skin, mode, tab -> {
            Locator form = tab.getByRole(AriaRole.FORM, new Page.GetByRoleOptions().setName("Normal composition"));
            Locator group = form.getByRole(AriaRole.RADIOGROUP, new Locator.GetByRoleOptions().setName("Alignment"));
            tab.locator("html").evaluate("html => html.dir = 'rtl'");
            assertThat(group.locator("label").first()).hasCSS("border-right-width", "1px");
            assertThat(group.locator("label").nth(1)).hasCSS("border-right-width", "0px");
            Locator highlight = form.getByLabel("Highlight", new Locator.GetByLabelOptions().setExact(true));
            highlight.check();
            highlight.focus();
            tab.emulateMedia(new Page.EmulateMediaOptions().setForcedColors(ForcedColors.ACTIVE));
            assertThat(highlight.locator("xpath=..")).hasCSS("forced-color-adjust", "none");
            Locator disabled = tab.getByLabel("Managed emphasis", new Page.GetByLabelOptions().setExact(true));
            assertThat(disabled).isDisabled();
            assertThat(disabled.locator("xpath=..")).hasCSS("outline-style", "solid");
            tab.emulateMedia(new Page.EmulateMediaOptions().setForcedColors(ForcedColors.NONE));
        });
    }

    @ParameterizedTest(name = "{0}, {1}")
    @MethodSource("everySkinAndMode")
    void nativeAndSingleAjaxUploadsProcessActualMultipartFiles(String skin, String mode) {
        check("uploads", skin, mode, tab -> {
            Locator form = tab.getByRole(AriaRole.FORM, new Page.GetByRoleOptions().setName("Native upload"));
            Locator input = form.getByLabel("Documents", new Locator.GetByLabelOptions().setExact(true));
            form.getByRole(AriaRole.BUTTON, new Locator.GetByRoleOptions().setName("Upload documents").setExact(true)).click();
            assertThat(input).hasAttribute("aria-invalid", "true");
            form.getByLabel("Title", new Locator.GetByLabelOptions().setExact(true)).fill("Manual");
            input.setInputFiles(new FilePayload[]{upload("one.txt", "alpha"), upload("two.txt", "beta")});
            form.getByRole(AriaRole.BUTTON, new Locator.GetByRoleOptions().setName("Upload documents").setExact(true)).click();
            assertThat(form.getByRole(AriaRole.STATUS).last()).hasText("Received Manual: one.txt (5 bytes), two.txt (4 bytes).");
            Locator single = tab.getByRole(AriaRole.FORM, new Page.GetByRoleOptions().setName("Single-file Ajax upload"));
            Locator document = single.getByLabel("Single document", new Locator.GetByLabelOptions().setExact(true));
            assertThat(document.getAttribute("multiple")).isNull();
            document.setInputFiles(upload("single.txt", "contents"));
            single.getByRole(AriaRole.BUTTON, new Locator.GetByRoleOptions().setName("Upload one file").setExact(true)).click();
            assertThat(single.getByRole(AriaRole.STATUS).last()).hasText("Received single: single.txt (8 bytes).");
            assertThat(document.evaluate("input => input.files.length")).isEqualTo(0);
            single.getByRole(AriaRole.BUTTON, new Locator.GetByRoleOptions().setName("Upload one file").setExact(true)).click();
            assertThat(document).hasAttribute("aria-invalid", "true");
        });
    }

    @ParameterizedTest(name = "{0}, {1}")
    @MethodSource("everySkinAndMode")
    void ajaxDropzonesKeepFilesAfterErrorsAndClearCompletedSelections(String skin, String mode) {
        check("uploads", skin, mode, tab -> {
            Locator form = tab.getByRole(AriaRole.FORM, new Page.GetByRoleOptions().setName("Ajax dropzone"));
            Locator input = form.getByLabel("Documents", new Locator.GetByLabelOptions().setExact(true));
            form.getByRole(AriaRole.BUTTON, new Locator.GetByRoleOptions().setName("Upload documents").setExact(true)).click();
            assertThat(input).hasAttribute("aria-invalid", "true");
            input.setInputFiles(upload("strange <name>.txt", "data"));
            assertThat(form.locator(".sw-file-upload-name")).hasText("strange <name>.txt");
            assertThat(form.locator("name")).hasCount(0);
            input.focus();
            form.getByRole(AriaRole.BUTTON, new Locator.GetByRoleOptions().setName("Upload documents").setExact(true)).evaluate("button => button.click()");
            Locator title = form.getByLabel("Title", new Locator.GetByLabelOptions().setExact(true));
            assertThat(title).hasAttribute("aria-invalid", "true");
            assertThat(input.evaluate("input => input.files.length")).isEqualTo(1);
            assertThat(input).isFocused();
            tab.getByRole(AriaRole.BUTTON, new Page.GetByRoleOptions().setName("Check server status")).click();
            assertThat(tab.getByText("Server checks: 1", new Page.GetByTextOptions().setExact(true))).isVisible();
            assertThat(title).hasAttribute("aria-invalid", "true");
            assertThat(form.locator(".sw-file-upload-name")).hasText("strange <name>.txt");
            title.fill("Ajax");
            form.getByRole(AriaRole.BUTTON, new Locator.GetByRoleOptions().setName("Upload documents").setExact(true)).click();
            assertThat(form.getByRole(AriaRole.STATUS).last()).hasText("Received Ajax: strange <name>.txt (4 bytes).");
            assertThat(form.locator("name")).hasCount(0);
            assertThat(input.evaluate("input => input.files.length")).isEqualTo(0);
            assertThat(form.locator(".sw-file-upload-list")).isHidden();
        });
    }

    @ParameterizedTest(name = "{0}, {1}")
    @MethodSource("everySkinAndMode")
    void dropzonesMergeDropsRejectHintsAndRemoveFilesWithKeyboardFocus(String skin, String mode) {
        check("uploads", skin, mode, tab -> {
            Locator form = tab.getByRole(AriaRole.FORM, new Page.GetByRoleOptions().setName("Ajax dropzone"));
            Locator input = form.getByLabel("Documents", new Locator.GetByLabelOptions().setExact(true));
            Locator root = form.locator(".sw-file-upload");
            input.focus();
            tab.waitForFileChooser(() -> input.press("Space")).setFiles(upload("one.txt", "alpha"));
            input.setInputFiles(upload("two.txt", "beta"));
            assertThat(form.locator(".sw-file-upload-name")).hasText(new String[]{"one.txt", "two.txt"});
            input.evaluate("input => { const transfer = new DataTransfer(); transfer.items.add(input.files[0]);"
                    + "input.closest('.sw-file-upload').dispatchEvent(new DragEvent('drop', {bubbles:true,cancelable:true,dataTransfer:transfer})); }");
            assertThat(input.evaluate("input => input.files.length")).isEqualTo(2);
            root.evaluate("root => { const transfer = new DataTransfer(); transfer.items.add(new File(['gamma'], 'three.txt', {type:'text/plain'}));"
                    + "root.dispatchEvent(new DragEvent('dragover', {bubbles:true,cancelable:true,dataTransfer:transfer})); }");
            assertThat(root).hasAttribute("data-dragging", "");
            drop(root, "three.txt", "text/plain", 5);
            assertThat(input.evaluate("input => input.files.length")).isEqualTo(3);
            drop(root, "large.txt", "text/plain", 65537);
            assertThat(root.locator("[data-rejected] .sw-file-upload-size")).containsText("65536");
            drop(root, "four.txt", "text/plain", 4);
            assertThat(root.locator("[data-rejected] .sw-file-upload-size")).containsText("at most 3");
            drop(root, "image.png", "image/png", 4);
            assertThat(root.locator("[data-rejected] .sw-file-upload-size")).containsText("not accepted");
            root.getByRole(AriaRole.BUTTON, new Locator.GetByRoleOptions().setName("Dismiss image.png")).click();
            root.getByRole(AriaRole.BUTTON, new Locator.GetByRoleOptions().setName("Remove two.txt")).click();
            assertThat(input.evaluate("input => Array.from(input.files).map(file => file.name)")).isEqualTo(List.of("one.txt", "three.txt"));
            Locator removeThird = root.getByRole(AriaRole.BUTTON, new Locator.GetByRoleOptions().setName("Remove three.txt"));
            assertThat(removeThird).isFocused();
            removeThird.press("Enter");
            root.getByRole(AriaRole.BUTTON, new Locator.GetByRoleOptions().setName("Remove one.txt")).press("Enter");
            assertThat(input).isFocused();
            assertThat(input.evaluate("input => input.files.length")).isEqualTo(0);
            drop(root, "reset.txt", "text/plain", 4);
            form.evaluate("form => form.reset()");
            assertThat(root.locator(".sw-file-upload-list")).isHidden();
            assertThat(input.evaluate("input => input.files.length")).isEqualTo(0);
            Locator disabled = tab.getByLabel("Managed attachments", new Page.GetByLabelOptions().setExact(true));
            assertThat(disabled).isDisabled();
            drop(disabled.locator("xpath=.."), "ignored.txt", "text/plain", 4);
            assertThat(disabled.evaluate("input => input.files.length")).isEqualTo(0);
            tab.emulateMedia(new Page.EmulateMediaOptions().setForcedColors(ForcedColors.ACTIVE));
            input.focus();
            assertThat(form.locator(".sw-file-upload-browse")).hasCSS("outline-style", "solid");
            tab.emulateMedia(new Page.EmulateMediaOptions().setForcedColors(ForcedColors.NONE));
        });
    }

    @ParameterizedTest(name = "{0}, {1}")
    @MethodSource("everySkinAndMode")
    void nativeUploadsValidateTypeCountAndSizeAndReportMultipartLimits(String skin, String mode) {
        check("uploads", skin, mode, tab -> {
            Locator form = tab.getByRole(AriaRole.FORM, new Page.GetByRoleOptions().setName("Native upload"));
            Locator input = form.getByLabel("Documents", new Locator.GetByLabelOptions().setExact(true));
            form.getByLabel("Title", new Locator.GetByLabelOptions().setExact(true)).fill("Limits");
            input.setInputFiles(new FilePayload("image.png", "image/png", new byte[4]));
            form.getByRole(AriaRole.BUTTON, new Locator.GetByRoleOptions().setName("Upload documents").setExact(true)).click();
            assertThat(form.getByRole(AriaRole.STATUS).filter(new Locator.FilterOptions().setHasText("Choose .txt files"))).isVisible();
            form.getByLabel("Title", new Locator.GetByLabelOptions().setExact(true)).fill("Limits");
            input.setInputFiles(new FilePayload("large.txt", "text/plain", new byte[65537]));
            form.getByRole(AriaRole.BUTTON, new Locator.GetByRoleOptions().setName("Upload documents").setExact(true)).click();
            assertThat(form.getByRole(AriaRole.STATUS).filter(new Locator.FilterOptions().setHasText("65536 bytes"))).isVisible();
            form.getByLabel("Title", new Locator.GetByLabelOptions().setExact(true)).fill("Limits");
            input.setInputFiles(new FilePayload[]{upload("a.txt", "a"), upload("b.txt", "b"), upload("c.txt", "c"), upload("d.txt", "d")});
            form.getByRole(AriaRole.BUTTON, new Locator.GetByRoleOptions().setName("Upload documents").setExact(true)).click();
            assertThat(form.getByRole(AriaRole.STATUS).filter(new Locator.FilterOptions().setHasText("at most 3"))).isVisible();
            form.getByLabel("Title", new Locator.GetByLabelOptions().setExact(true)).fill("Limits");
            input.setInputFiles(new FilePayload("parser-limit.txt", "text/plain", new byte[131072]));
            form.getByRole(AriaRole.BUTTON, new Locator.GetByRoleOptions().setName("Upload documents").setExact(true)).click();
            assertThat(form.getByText("Upload failed. Check the 96 KB per-file", new Locator.GetByTextOptions().setExact(false))).isVisible();
            form.getByLabel("Title", new Locator.GetByLabelOptions().setExact(true)).fill("Request limit");
            input.setInputFiles(new FilePayload("request-limit.txt", "text/plain", new byte[262145]));
            form.getByRole(AriaRole.BUTTON, new Locator.GetByRoleOptions().setName("Upload documents").setExact(true)).click();
            assertThat(form.getByText("Upload limit exceeded: the request", new Locator.GetByTextOptions().setExact(false))).isVisible();
        });
    }

    private static FilePayload upload(String name, String content) {
        return new FilePayload(name, "text/plain", content.getBytes(java.nio.charset.StandardCharsets.UTF_8));
    }

    private static void drop(Locator root, String name, String type, int size) {
        root.evaluate("(root, file) => { const transfer = new DataTransfer(); transfer.items.add(new File([new Uint8Array(file.size)], file.name, {type:file.type}));"
                + "root.dispatchEvent(new DragEvent('drop', {bubbles:true,cancelable:true,dataTransfer:transfer})); }",
                java.util.Map.of("name", name, "type", type, "size", size));
    }

    @ParameterizedTest(name = "{0}, {1}")
    @MethodSource("everySkinAndMode")
    void confirmOverlaysFocusCancelIgnoreTheBackdropAndForgetCancelledActions(String skin, String mode) {
        check("overlays", skin, mode, tab -> {
            Locator opener = tab.locator("section[aria-labelledby='confirm-heading'] > .ex-row")
                    .getByRole(AriaRole.BUTTON, new Locator.GetByRoleOptions().setName("Delete draft").setExact(true));
            opener.click();
            Locator dialog = tab.getByRole(AriaRole.ALERTDIALOG);
            assertThat(dialog).hasAccessibleName("Delete this draft?");
            assertThat(dialog.getByRole(AriaRole.BUTTON, new Locator.GetByRoleOptions().setName("Cancel"))).isFocused();
            tab.keyboard().press("Tab");
            assertThat(dialog.getByRole(AriaRole.BUTTON, new Locator.GetByRoleOptions().setName("Delete draft"))).isFocused();
            tab.keyboard().press("Shift+Tab");
            assertThat(dialog.getByRole(AriaRole.BUTTON, new Locator.GetByRoleOptions().setName("Cancel"))).isFocused();
            opener.evaluate("button => button.focus()");
            assertThat(dialog.getByRole(AriaRole.BUTTON, new Locator.GetByRoleOptions().setName("Cancel"))).isFocused();
            tab.mouse().click(2, 2);
            assertThat(dialog).isVisible();
            dialog.evaluate("d => Promise.all(d.getAnimations({subtree:true}).map(a => a.finished))");
            assertThat(describe(new AxeBuilder(tab).withTags(WCAG).analyze().getViolations())).isEmpty();
            tab.keyboard().press("Escape");
            assertThat(dialog).isHidden();
            assertThat(opener).isFocused();
            assertThat(tab.getByRole(AriaRole.STATUS).filter(new Locator.FilterOptions().setHasText("Confirmed actions:"))).hasText("Confirmed actions: 0");
            tab.getByRole(AriaRole.BUTTON, new Page.GetByRoleOptions().setName("Archive draft")).click();
            assertThat(dialog).hasAccessibleName("Archive this draft?");
            dialog.getByRole(AriaRole.BUTTON, new Locator.GetByRoleOptions().setName("Archive").setExact(true)).click();
            assertThat(dialog).isHidden();
            assertThat(tab.getByRole(AriaRole.STATUS).filter(new Locator.FilterOptions().setHasText("Confirmed actions:"))).hasText("Confirmed actions: 1");
        });
    }

    @ParameterizedTest(name = "{0}, {1}")
    @MethodSource("everySkinAndMode")
    void confirmOverlaysDoNotAnswerAChainedQuestionOnADoubleClick(String skin, String mode) {
        check("overlays", skin, mode, tab -> {
            tab.getByRole(AriaRole.BUTTON, new Page.GetByRoleOptions().setName("Two-step confirmation")).click();
            Locator dialog = tab.getByRole(AriaRole.ALERTDIALOG);
            dialog.getByRole(AriaRole.BUTTON, new Locator.GetByRoleOptions().setName("Continue").setExact(true))
                    .evaluate("button => { button.click(); button.click(); }");
            assertThat(dialog).hasAccessibleName("Confirm the final action?");
            assertThat(dialog.getByRole(AriaRole.BUTTON, new Locator.GetByRoleOptions().setName("Cancel"))).isFocused();
            assertThat(tab.getByRole(AriaRole.STATUS).filter(new Locator.FilterOptions().setHasText("Confirmed actions:"))).hasText("Confirmed actions: 0");
            dialog.getByRole(AriaRole.BUTTON, new Locator.GetByRoleOptions().setName("Confirm final action"))
                    .evaluate("button => { button.click(); button.click(); }");
            assertThat(dialog).isHidden();
            assertThat(tab.getByRole(AriaRole.STATUS).filter(new Locator.FilterOptions().setHasText("Confirmed actions:"))).hasText("Confirmed actions: 1");
        });
    }

    @ParameterizedTest(name = "{0}, {1}")
    @MethodSource("everySkinAndMode")
    void sheetsValidateInPlaceKeepActionsVisibleAndRestoreNestedFocus(String skin, String mode) {
        check("overlays", skin, mode, tab -> {
            tab.setViewportSize(390, 700);
            Locator opener = tab.getByRole(AriaRole.BUTTON, new Page.GetByRoleOptions().setName("Edit project").setExact(true));
            opener.click();
            Locator sheet = tab.getByRole(AriaRole.DIALOG, new Page.GetByRoleOptions().setName("Project settings"));
            Locator name = sheet.getByLabel("Project name", new Locator.GetByLabelOptions().setExact(true));
            assertThat(name).isFocused();
            assertThat(tab.locator("html")).hasCSS("overflow", "hidden");
            name.fill("");
            name.press("Enter");
            assertThat(name).hasAttribute("aria-invalid", "true");
            assertThat(name).isFocused();
            name.fill("admin");
            name.press("Enter");
            assertThat(name).hasAccessibleDescription("That project name is reserved.");
            assertThat(name).isFocused();
            assertThat(sheet.getByRole(AriaRole.BUTTON, new Locator.GetByRoleOptions().setName("Save project"))).isInViewport();
            sheet.locator(".sw-sheet-body-content").evaluate("n => n.scrollTop = n.scrollHeight");
            assertThat(sheet.locator(".sw-sheet-title")).isInViewport();
            assertThat(sheet.getByRole(AriaRole.BUTTON, new Locator.GetByRoleOptions().setName("Save project"))).isInViewport();
            assertThat((Boolean) sheet.locator(".sw-sheet-body-content").evaluate("n => n.scrollHeight > n.clientHeight && n.scrollTop > 0")).isTrue();
            Locator nestedTrigger = sheet.getByRole(AriaRole.BUTTON, new Locator.GetByRoleOptions().setName("Review reset"));
            nestedTrigger.click();
            Locator confirm = tab.getByRole(AriaRole.ALERTDIALOG);
            assertThat(confirm.getByRole(AriaRole.BUTTON, new Locator.GetByRoleOptions().setName("Cancel"))).isFocused();
            tab.keyboard().press("Escape");
            assertThat(confirm).isHidden();
            assertThat(nestedTrigger).isFocused();
            assertThat(sheet).isVisible();
            name.fill("New project");
            sheet.getByRole(AriaRole.BUTTON, new Locator.GetByRoleOptions().setName("Save project")).click();
            assertThat(sheet).isHidden();
            assertThat(opener).isFocused();
            assertThat(tab.getByRole(AriaRole.STATUS).filter(new Locator.FilterOptions().setHasText("Saved project:"))).hasText("Saved project: New project.");
        });
    }

    @ParameterizedTest(name = "{0}, {1}")
    @MethodSource("everySkinAndMode")
    void sheetsFollowEveryEdgeAndTextDirectionAndSurviveAjaxRefresh(String skin, String mode) {
        check("overlays", skin, mode, tab -> {
            for (String direction : List.of("ltr", "rtl")) {
                tab.locator("html").evaluate("(n, dir) => n.dir = dir", direction);
                for (String edge : List.of("Start", "Top", "Bottom")) {
                    Locator opener = tab.getByRole(AriaRole.BUTTON, new Page.GetByRoleOptions().setName(edge + " sheet"));
                    opener.click();
                    Locator sheet = tab.locator(".sw-sheet[open]");
                    sheet.evaluate("d => Promise.all(d.getAnimations({subtree:true}).map(a => a.finished))");
                    var rect = sheet.boundingBox();
                    if ("Top".equals(edge)) assertThat(rect.y).isCloseTo(0, org.assertj.core.data.Offset.offset(1.0));
                    else if ("Bottom".equals(edge)) assertThat(rect.y + rect.height).isCloseTo((double) tab.viewportSize().height, org.assertj.core.data.Offset.offset(1.0));
                    else if ("ltr".equals(direction)) assertThat(rect.x).isCloseTo(0, org.assertj.core.data.Offset.offset(1.0));
                    else assertThat(rect.x + rect.width).isCloseTo((double) tab.viewportSize().width, org.assertj.core.data.Offset.offset(1.0));
                    assertThat(describe(new AxeBuilder(tab).withTags(WCAG).analyze().getViolations())).isEmpty();
                    tab.keyboard().press("Escape");
                    assertThat(sheet).hasCount(0);
                    assertThat(opener).isFocused();
                }
            }
            tab.getByRole(AriaRole.BUTTON, new Page.GetByRoleOptions().setName("Edit project").setExact(true)).click();
            Locator sheet = tab.getByRole(AriaRole.DIALOG, new Page.GetByRoleOptions().setName("Project settings"));
            var beforeRefresh = sheet.elementHandle();
            sheet.getByRole(AriaRole.BUTTON, new Locator.GetByRoleOptions().setName("Refresh open sheet")).click();
            tab.waitForFunction("old => !old.isConnected", beforeRefresh);
            assertThat(sheet).isVisible();
            assertThat(sheet).hasJSProperty("open", true);
            assertThat((Boolean) sheet.evaluate("d => d.matches(':modal')")).isTrue();
            assertThat(sheet).hasAttribute("data-side", "end");
            sheet.getByRole(AriaRole.BUTTON, new Locator.GetByRoleOptions().setName("Close").setExact(true)).click();
            assertThat(sheet).isHidden();
        });
    }

    @ParameterizedTest(name = "{0}, {1}")
    @MethodSource("everySkinAndMode")
    void popoversValidateInPlaceRefreshContentAndCloseAfterAnAjaxSave(String skin, String mode) {
        check("popups", skin, mode, tab -> {
            Locator trigger = tab.getByRole(AriaRole.BUTTON, new Page.GetByRoleOptions().setName("Quick edit").setExact(true));
            trigger.click();
            Locator popup = tab.getByRole(AriaRole.DIALOG, new Page.GetByRoleOptions().setName("Quick project edit"));
            Locator name = popup.getByLabel("Display name", new Locator.GetByLabelOptions().setExact(true));
            assertThat(name).isFocused();
            assertThat(trigger).hasAttribute("aria-expanded", "true");
            name.fill(""); name.press("Enter");
            assertThat(name).hasAttribute("aria-invalid", "true");
            assertThat(name).isFocused();
            assertThat(name).hasAccessibleDescription(java.util.regex.Pattern.compile(".*Visible to your team.*is required.*public project name.*"));
            popup.getByRole(AriaRole.BUTTON, new Locator.GetByRoleOptions().setName("Refresh content")).click();
            assertThat(popup.getByText("Content refreshes: 1")).isVisible();
            assertThat(popup).isVisible();
            assertThat(trigger).hasAttribute("aria-expanded", "true");
            name.fill("New display name");
            popup.getByRole(AriaRole.BUTTON, new Locator.GetByRoleOptions().setName("Apply changes")).click();
            assertThat(popup).isHidden();
            assertThat(trigger).isFocused();
            assertThat(trigger).hasAttribute("aria-expanded", "false");
            assertThat(tab.getByRole(AriaRole.STATUS).filter(new Locator.FilterOptions().setHasText("Saved display name:")))
                    .hasText("Saved display name: New display name.");
        });
    }

    @ParameterizedTest(name = "{0}, {1}")
    @MethodSource("everySkinAndMode")
    void popoversDismissWithoutMakingThePageInertAndNestedEscapeKeepsTheParentOpen(String skin, String mode) {
        check("popups", skin, mode, tab -> {
            Locator trigger = tab.getByRole(AriaRole.BUTTON, new Page.GetByRoleOptions().setName("Quick edit").setExact(true));
            Locator popup = tab.getByRole(AriaRole.DIALOG, new Page.GetByRoleOptions().setName("Quick project edit"));
            trigger.click();
            Locator nestedTrigger = popup.getByRole(AriaRole.BUTTON, new Locator.GetByRoleOptions().setName("Visibility details"));
            nestedTrigger.click();
            Locator nested = tab.getByRole(AriaRole.DIALOG, new Page.GetByRoleOptions().setName("Workspace visibility"));
            assertThat(nested).isFocused();
            assertThat(tab.getByRole(AriaRole.TOOLTIP)).hasCount(0);
            tab.keyboard().press("Escape");
            assertThat(nested).isHidden();
            assertThat(nestedTrigger).isFocused();
            assertThat(popup).isVisible();
            popup.getByRole(AriaRole.BUTTON, new Locator.GetByRoleOptions().setName("Refresh content")).focus();
            tab.keyboard().press("Tab");
            assertThat(popup).isHidden();
            assertThat(tab.getByRole(AriaRole.BUTTON, new Page.GetByRoleOptions().setName("Replace popover"))).isFocused();
            trigger.click();
            tab.getByRole(AriaRole.BUTTON, new Page.GetByRoleOptions().setName("Continue on page")).click();
            assertThat(popup).isHidden();
            assertThat(tab.getByRole(AriaRole.STATUS)).hasText("The page stayed interactive.");
        });
    }

    @ParameterizedTest(name = "{0}, {1}")
    @MethodSource("everySkinAndMode")
    void tooltipsShowOnKeyboardFocusAndEscapeLeavesTheirContainingPopoverOpen(String skin, String mode) {
        check("popups", skin, mode, tab -> {
            tab.keyboard().press("Tab");
            Locator export = tab.getByRole(AriaRole.BUTTON, new Page.GetByRoleOptions().setName("Export").setExact(true));
            export.focus();
            Locator tooltip = tab.getByRole(AriaRole.TOOLTIP);
            assertThat(tooltip).hasText("Download a CSV copy.");
            assertThat(export).isFocused();
            assertThat(export).hasAccessibleDescription("Download a CSV copy.");
            tab.keyboard().press("Escape");
            assertThat(tooltip).hasCount(0);
            assertThat(export).isFocused();
            tab.getByRole(AriaRole.BUTTON, new Page.GetByRoleOptions().setName("Quick edit").setExact(true)).click();
            Locator name = tab.getByLabel("Display name", new Page.GetByLabelOptions().setExact(true));
            name.focus();
            assertThat(tooltip).hasText("Use the public project name.");
            tab.keyboard().press("Escape");
            assertThat(tooltip).hasCount(0);
            assertThat(tab.getByRole(AriaRole.DIALOG, new Page.GetByRoleOptions().setName("Quick project edit"))).isVisible();
            tab.keyboard().press("Escape");
            assertThat(tab.getByRole(AriaRole.DIALOG)).hasCount(0);
        });
    }

    @ParameterizedTest(name = "{0}, {1}")
    @MethodSource("everySkinAndMode")
    void tooltipsRemainHoverableAndLabelModeDoesNotRepeatAnAccessibleName(String skin, String mode) {
        check("popups", skin, mode, tab -> {
            Locator export = tab.getByRole(AriaRole.BUTTON, new Page.GetByRoleOptions().setName("Export").setExact(true));
            export.hover();
            Locator tooltip = tab.getByRole(AriaRole.TOOLTIP);
            assertThat(tooltip).hasText("Download a CSV copy.");
            tooltip.evaluate("n => Promise.all(n.getAnimations().map(a => a.finished))");
            var tipBox = tooltip.boundingBox();
            // Move the pointer directly: Locator.hover() can scroll the anchor after measuring this top-layer node.
            tab.mouse().move(tipBox.x + tipBox.width / 2, tipBox.y + tipBox.height / 2);
            tab.waitForTimeout(180);
            assertThat(tooltip).isVisible();
            tab.keyboard().press("Escape");
            assertThat(tooltip).hasCount(0);
            tab.mouse().move(0, 0);
            Locator copy = tab.getByRole(AriaRole.BUTTON, new Page.GetByRoleOptions().setName("Copy link").setExact(true));
            copy.hover();
            Locator labelTip = tab.locator(".sw-tooltip-content[aria-hidden='true']:popover-open");
            assertThat(labelTip).hasText("Copy link");
            assertThat(copy).hasAccessibleName("Copy link");
            assertThat(copy).hasAccessibleDescription("");
            assertThat(tab.locator("[style]")).hasCount(0);
            tab.emulateMedia(new Page.EmulateMediaOptions().setForcedColors(ForcedColors.ACTIVE));
            assertThat(labelTip).hasCSS("border-top-width", "1px");
            tab.mouse().move(0, 0);
            assertThat(labelTip).hasCount(0);
            tab.emulateMedia(new Page.EmulateMediaOptions().setForcedColors(ForcedColors.NONE));
            tab.evaluate("() => {const label=document.createElement('label'); label.id='wrapped-label'; label.textContent='Wrapped control';"
                    + "const input=document.createElement('input'); input.type='checkbox'; input.id='wrapped-control';"
                    + "input.setAttribute('data-sw-tooltip','Additional explanation'); label.append(input); document.querySelector('main').append(label);}");
            tab.keyboard().press("Tab");
            Locator wrapped = tab.getByRole(AriaRole.CHECKBOX, new Page.GetByRoleOptions().setName("Wrapped control").setExact(true));
            wrapped.focus();
            assertThat(tooltip).hasText("Additional explanation");
            assertThat(wrapped).hasAccessibleName("Wrapped control");
            assertThat(wrapped).hasAccessibleDescription("Additional explanation");
            tab.locator("#wrapped-label").evaluate("n => n.remove()");
            assertThat(tooltip).hasCount(0);
        });
    }

    @ParameterizedTest(name = "{0}, {1}")
    @MethodSource("everySkinAndMode")
    void tooltipModelsSurviveAjaxReplacementAndRemovedHintsLeaveNoOrphan(String skin, String mode) {
        check("popups", skin, mode, tab -> {
            Locator export = tab.getByRole(AriaRole.BUTTON, new Page.GetByRoleOptions().setName("Export").setExact(true));
            export.hover();
            Locator tooltip = tab.getByRole(AriaRole.TOOLTIP);
            assertThat(tooltip).isVisible();
            String old = tooltip.getAttribute("id");
            long total = tab.locator(".sw-tooltip-content").count();
            tab.getByRole(AriaRole.BUTTON, new Page.GetByRoleOptions().setName("Update export hint")).click();
            tab.waitForFunction("id => !document.getElementById(id)", old);
            export.hover();
            assertThat(tooltip).hasText("Updated <CSV> export hint.");
            assertThat(tooltip.locator("csv")).hasCount(0);
            assertThat(tab.locator(".sw-tooltip-content")).hasCount((int) total);
            tab.setViewportSize(320, 700);
            String longHint = "LongFilename".repeat(25);
            export.evaluate("(n, text) => n.setAttribute('data-sw-tooltip', text)", longHint);
            tab.mouse().move(0, 0); export.hover();
            assertThat(tooltip).hasText(longHint);
            assertThat((Boolean) tooltip.evaluate("n => n.scrollWidth <= n.clientWidth + 1")).isTrue();
            tab.getByRole(AriaRole.BUTTON, new Page.GetByRoleOptions().setName("Remove export hint")).click();
            assertThat(export).not().hasAttribute("data-sw-tooltip", java.util.regex.Pattern.compile(".+"));
            assertThat(export).hasAccessibleDescription("");
            assertThat(tab.locator(".sw-tooltip-content")).hasCount((int) total - 1);
        });
    }

    @ParameterizedTest(name = "{0}, {1}")
    @MethodSource("everySkinAndMode")
    void popupAnchorsFlipAndStayInsideSmallViewportsInBothTextDirections(String skin, String mode) {
        check("popups", skin, mode, tab -> {
            tab.setViewportSize(390, 700);
            for (String direction : List.of("ltr", "rtl")) {
                tab.locator("html").evaluate("(n, dir) => n.dir = dir", direction);
                for (String side : List.of("Top", "Bottom", "Start", "End")) {
                    Locator trigger = tab.getByRole(AriaRole.BUTTON, new Page.GetByRoleOptions().setName(side + " placement"));
                    trigger.click();
                    Locator popup = tab.getByRole(AriaRole.DIALOG, new Page.GetByRoleOptions().setName(side + " example"));
                    popup.evaluate("n => Promise.all(n.getAnimations().map(a => a.finished))");
                    var rect = popup.boundingBox();
                    assertThat(rect.x).isGreaterThanOrEqualTo(0);
                    assertThat(rect.x + rect.width).isLessThanOrEqualTo(391);
                    assertThat(rect.y).isGreaterThanOrEqualTo(0);
                    assertThat(rect.y + rect.height).isLessThanOrEqualTo(701);
                    tab.keyboard().press("Escape");
                    assertThat(popup).isHidden();
                }
            }
            tab.setViewportSize(390, 360);
            Locator top = tab.getByRole(AriaRole.BUTTON, new Page.GetByRoleOptions().setName("Top placement"));
            top.evaluate("n => window.scrollTo(0, scrollY + n.getBoundingClientRect().top - 4)");
            top.click();
            Locator popup = tab.getByRole(AriaRole.DIALOG, new Page.GetByRoleOptions().setName("Top example"));
            popup.evaluate("n => Promise.all(n.getAnimations().map(a => a.finished))");
            assertThat(popup.boundingBox().y).isGreaterThanOrEqualTo(top.boundingBox().y + top.boundingBox().height);
            tab.emulateMedia(new Page.EmulateMediaOptions().setForcedColors(ForcedColors.ACTIVE));
            assertThat(popup).hasCSS("border-top-width", "1px");
            tab.keyboard().press("Escape");
            tab.emulateMedia(new Page.EmulateMediaOptions().setForcedColors(ForcedColors.NONE));
        });
    }

    @ParameterizedTest(name = "{0}, {1}")
    @MethodSource("everySkinAndMode")
    void touchPopoversWorkWithoutShowingHoverTooltips(String skin, String mode) {
        check("popups", skin, mode, new Browser.NewContextOptions().setHasTouch(true).setViewportSize(390, 700), tab -> {
            tab.getByRole(AriaRole.BUTTON, new Page.GetByRoleOptions().setName("Export").setExact(true)).tap();
            assertThat(tab.getByRole(AriaRole.STATUS)).hasText("Export preview requested.");
            assertThat(tab.locator(".sw-tooltip-content:popover-open")).hasCount(0);
            tab.getByRole(AriaRole.BUTTON, new Page.GetByRoleOptions().setName("Quick edit").setExact(true)).tap();
            assertThat(tab.getByLabel("Display name", new Page.GetByLabelOptions().setExact(true))).isFocused();
            assertThat(tab.locator(".sw-tooltip-content:popover-open")).hasCount(0);
            tab.getByRole(AriaRole.BUTTON, new Page.GetByRoleOptions().setName("Apply changes")).tap();
            assertThat(tab.getByRole(AriaRole.DIALOG)).hasCount(0);
            tab.keyboard().press("Tab");
            tab.getByRole(AriaRole.BUTTON, new Page.GetByRoleOptions().setName("Export").setExact(true)).focus();
            assertThat(tab.getByRole(AriaRole.TOOLTIP)).hasText("Download a CSV copy.");
        });
    }

    @ParameterizedTest(name = "{0}, {1}")
    @MethodSource("everySkinAndMode")
    void dropdownKeyboardNavigationTypeaheadAndActivation(String skin, String mode) {
        check("menus", skin, mode, tab -> {
            Locator trigger = tab.getByRole(AriaRole.BUTTON, new Page.GetByRoleOptions().setName("Project actions").setExact(true));
            Locator menu = tab.getByRole(AriaRole.MENU, new Page.GetByRoleOptions().setName("Project actions").setExact(true));
            trigger.focus(); trigger.press("ArrowDown");
            assertThat(menu.getByRole(AriaRole.MENUITEM, new Locator.GetByRoleOptions().setName("Edit project"))).isFocused();
            assertThat(trigger).hasAttribute("aria-expanded", "true");
            tab.keyboard().press("End");
            assertThat(menu.getByRole(AriaRole.MENUITEM, new Locator.GetByRoleOptions().setName("Delete project…"))).isFocused();
            menu.evaluate("n => Promise.all(n.getAnimations().map(a=>a.finished))");
            assertThat(describe(new AxeBuilder(tab).withTags(WCAG).analyze().getViolations())).isEmpty();
            tab.keyboard().press("ArrowUp");
            assertThat(menu.getByRole(AriaRole.MENUITEM, new Locator.GetByRoleOptions().setName("Export PDF"))).isFocused();
            tab.keyboard().press("Home");
            tab.keyboard().press("e");
            assertThat(menu.getByRole(AriaRole.MENUITEM, new Locator.GetByRoleOptions().setName("Export CSV"))).isFocused();
            tab.keyboard().press("e");
            assertThat(menu.getByRole(AriaRole.MENUITEM, new Locator.GetByRoleOptions().setName("Export PDF"))).isFocused();
            tab.keyboard().press("e");
            assertThat(menu.getByRole(AriaRole.MENUITEM, new Locator.GetByRoleOptions().setName("Edit project"))).isFocused();
            tab.keyboard().press("Escape");
            assertThat(menu).isHidden(); assertThat(trigger).isFocused();
            assertThat(trigger).hasAttribute("aria-expanded", "false");
            trigger.press("ArrowUp");
            assertThat(menu.getByRole(AriaRole.MENUITEM, new Locator.GetByRoleOptions().setName("Delete project…"))).isFocused();
            tab.keyboard().press("Escape");
            trigger.press("Space");
            assertThat(menu.getByRole(AriaRole.MENUITEM, new Locator.GetByRoleOptions().setName("Edit project"))).isFocused();
            tab.keyboard().press("e"); tab.keyboard().press("x");
            assertThat(menu.getByRole(AriaRole.MENUITEM, new Locator.GetByRoleOptions().setName("Export CSV"))).isFocused();
            tab.keyboard().press("Enter");
            assertThat(menu).isHidden(); assertThat(trigger).isFocused();
            assertThat(tab.getByRole(AriaRole.STATUS).filter(new Locator.FilterOptions().setHasText("Chosen:"))).hasText("Chosen: Export CSV.");
        });
    }

    @ParameterizedTest(name = "{0}, {1}")
    @MethodSource("everySkinAndMode")
    void dropdownPointerDisabledActionsAndRepeatedClicks(String skin, String mode) {
        check("menus", skin, mode, tab -> {
            Locator trigger = tab.getByRole(AriaRole.BUTTON, new Page.GetByRoleOptions().setName("Project actions").setExact(true));
            trigger.click();
            Locator menu = tab.getByRole(AriaRole.MENU, new Page.GetByRoleOptions().setName("Project actions").setExact(true));
            assertThat(menu).isFocused();
            Locator disabled = menu.getByRole(AriaRole.MENUITEM, new Locator.GetByRoleOptions().setName("Archive (unavailable)"));
            assertThat(disabled).isDisabled();
            disabled.evaluate("n => n.dispatchEvent(new MouseEvent('click',{bubbles:true,cancelable:true}))");
            assertThat(tab.getByRole(AriaRole.STATUS).filter(new Locator.FilterOptions().setHasText("Actions:"))).hasText("Actions: 0");
            menu.evaluate("n => Promise.all(n.getAnimations().map(a=>a.finished))");
            Locator export = menu.getByRole(AriaRole.MENUITEM, new Locator.GetByRoleOptions().setName("Export CSV"));
            var box = export.boundingBox(); tab.mouse().move(box.x + box.width / 2, box.y + box.height / 2);
            assertThat(export).isFocused();
            box = disabled.boundingBox(); tab.mouse().move(box.x + box.width / 2, box.y + box.height / 2);
            assertThat(menu).isFocused();
            export.evaluate("n => {n.click(); n.click();}");
            assertThat(menu).isHidden(); assertThat(trigger).isFocused();
            assertThat(tab.getByRole(AriaRole.STATUS).filter(new Locator.FilterOptions().setHasText("Chosen:"))).hasText("Chosen: Export CSV.");
            assertThat(tab.getByRole(AriaRole.STATUS).filter(new Locator.FilterOptions().setHasText("Actions:"))).hasText("Actions: 1");
        });
    }

    @ParameterizedTest(name = "{0}, {1}")
    @MethodSource("everySkinAndMode")
    void dropdownCheckedItemsUseServerStateAndSpaceActivatesLinks(String skin, String mode) {
        check("menus", skin, mode, tab -> {
            Locator trigger = tab.getByRole(AriaRole.BUTTON, new Page.GetByRoleOptions().setName("View options"));
            Locator menu = tab.getByRole(AriaRole.MENU, new Page.GetByRoleOptions().setName("View options"));
            trigger.click();
            Locator compact = menu.getByRole(AriaRole.MENUITEMCHECKBOX, new Locator.GetByRoleOptions().setName("Compact rows"));
            assertThat(compact).hasAttribute("aria-checked", "false");
            compact.click();
            assertThat(tab.getByRole(AriaRole.STATUS).filter(new Locator.FilterOptions().setHasText("Compact rows:"))).hasText("Compact rows: true.");
            assertThat(menu).isHidden();
            trigger.press("ArrowDown");
            assertThat(compact).hasAttribute("aria-checked", "true");
            Locator name = menu.getByRole(AriaRole.MENUITEMRADIO, new Locator.GetByRoleOptions().setName("Sort by name"));
            Locator updated = menu.getByRole(AriaRole.MENUITEMRADIO, new Locator.GetByRoleOptions().setName("Sort by updated"));
            assertThat(name).hasAttribute("aria-checked", "true");
            updated.focus(); updated.press("Space");
            assertThat(tab.getByRole(AriaRole.STATUS).filter(new Locator.FilterOptions().setHasText("Sort order:"))).hasText("Sort order: Updated.");
            assertThat(menu).isHidden();
            trigger.press("Enter");
            assertThat(updated).hasAttribute("aria-checked", "true");
            assertThat(name).hasAttribute("aria-checked", "false");
            menu.evaluate("n => Promise.all(n.getAnimations().map(a=>a.finished))");
            assertThat(describe(new AxeBuilder(tab).withTags(WCAG).analyze().getViolations())).isEmpty();
            tab.keyboard().press("Escape");
        });
    }

    @ParameterizedTest(name = "{0}, {1}")
    @MethodSource("everySkinAndMode")
    void dropdownNavigationAndTabDismissal(String skin, String mode) {
        check("menus", skin, mode, tab -> {
            Locator project = tab.getByRole(AriaRole.BUTTON, new Page.GetByRoleOptions().setName("Project actions").setExact(true));
            project.press("ArrowDown");
            assertThat(tab.getByRole(AriaRole.MENUITEM, new Page.GetByRoleOptions().setName("Edit project"))).isFocused();
            tab.keyboard().press("Tab");
            assertThat(tab.getByRole(AriaRole.MENU)).hasCount(0);
            assertThat(tab.getByRole(AriaRole.BUTTON, new Page.GetByRoleOptions().setName("Replace project actions"))).isFocused();
            project.press("ArrowDown");
            assertThat(tab.getByRole(AriaRole.MENUITEM, new Page.GetByRoleOptions().setName("Edit project"))).isFocused();
            tab.keyboard().press("Shift+Tab");
            assertThat(tab.getByRole(AriaRole.MENU)).hasCount(0);
            Locator navigation = tab.getByRole(AriaRole.BUTTON, new Page.GetByRoleOptions().setName("Navigate examples"));
            navigation.press("ArrowDown");
            Locator menu = tab.getByRole(AriaRole.MENU, new Page.GetByRoleOptions().setName("Navigate examples"));
            assertThat(menu.getByRole(AriaRole.MENUITEM, new Locator.GetByRoleOptions().setName("Home").setExact(true))).isFocused();
            assertThat(menu.getByRole(AriaRole.MENUITEM, new Locator.GetByRoleOptions().setName("Unavailable destination")))
                    .not().hasAttribute("href", java.util.regex.Pattern.compile(".+"));
            tab.keyboard().press("End");
            Locator link = menu.getByRole(AriaRole.MENUITEM, new Locator.GetByRoleOptions().setName(java.util.regex.Pattern.compile("Popovers.*")));
            assertThat(link).isFocused();
            link.press("Space");
            tab.waitForURL("**/popups*");
            assertThat(tab.locator("h1")).hasText("Popovers & tooltips");
        });
    }

    @ParameterizedTest(name = "{0}, {1}")
    @MethodSource("everySkinAndMode")
    void dropdownAjaxReplacementAndEmptyModels(String skin, String mode) {
        check("menus", skin, mode, tab -> {
            assertThat(tab.getByRole(AriaRole.BUTTON, new Page.GetByRoleOptions().setName("No available actions"))).isDisabled();
            Locator trigger = tab.getByRole(AriaRole.BUTTON, new Page.GetByRoleOptions().setName("Project actions").setExact(true));
            Locator menu = tab.getByRole(AriaRole.MENU, new Page.GetByRoleOptions().setName("Project actions").setExact(true));
            trigger.press("ArrowDown");
            tab.getByRole(AriaRole.BUTTON, new Page.GetByRoleOptions().setName("Replace project actions")).evaluate("n => n.click()");
            assertThat(trigger).hasAttribute("aria-expanded", "false");
            assertThat(menu).isHidden();
            trigger.press("ArrowDown");
            Locator rename = menu.getByRole(AriaRole.MENUITEM, new Locator.GetByRoleOptions().setName("Rename project"));
            assertThat(rename).isFocused();
            assertThat(menu.getByRole(AriaRole.MENUITEM)).hasCount(3);
            rename.press("Enter");
            assertThat(tab.getByRole(AriaRole.STATUS).filter(new Locator.FilterOptions().setHasText("Chosen:"))).hasText("Chosen: Rename project.");
            assertThat(menu).isHidden();
        });
    }

    @ParameterizedTest(name = "{0}, {1}")
    @MethodSource("everySkinAndMode")
    void dropdownActionsOpenConfirmationsAndMenusStayInsideTheirSheet(String skin, String mode) {
        check("menus", skin, mode, tab -> {
            Locator project = tab.getByRole(AriaRole.BUTTON, new Page.GetByRoleOptions().setName("Project actions").setExact(true));
            project.press("ArrowUp");
            assertThat(tab.getByRole(AriaRole.MENUITEM, new Page.GetByRoleOptions().setName("Delete project…"))).isFocused();
            tab.keyboard().press("Enter");
            Locator confirm = tab.getByRole(AriaRole.ALERTDIALOG);
            assertThat(confirm).hasAccessibleName("Delete this preview project?");
            assertThat(confirm.getByRole(AriaRole.BUTTON, new Locator.GetByRoleOptions().setName("Cancel"))).isFocused();
            confirm.getByRole(AriaRole.BUTTON, new Locator.GetByRoleOptions().setName("Cancel")).click();
            assertThat(confirm).isHidden(); assertThat(project).isFocused();
            tab.getByRole(AriaRole.BUTTON, new Page.GetByRoleOptions().setName("Open menu sheet")).click();
            Locator sheet = tab.getByRole(AriaRole.DIALOG, new Page.GetByRoleOptions().setName("Menu in a sheet"));
            Locator trigger = sheet.getByRole(AriaRole.BUTTON, new Locator.GetByRoleOptions().setName("Sheet actions"));
            trigger.press("ArrowDown");
            Locator menu = sheet.getByRole(AriaRole.MENU, new Locator.GetByRoleOptions().setName("Sheet actions"));
            assertThat(menu.getByRole(AriaRole.MENUITEM, new Locator.GetByRoleOptions().setName("Copy reference"))).isFocused();
            tab.keyboard().press("Escape");
            assertThat(menu).isHidden(); assertThat(sheet).isVisible(); assertThat(trigger).isFocused();
            trigger.press("ArrowUp");
            assertThat(menu.getByRole(AriaRole.MENUITEM, new Locator.GetByRoleOptions().setName("Check status"))).isFocused();
            tab.keyboard().press("Enter");
            assertThat(menu).isHidden(); assertThat(sheet).isVisible(); assertThat(trigger).isFocused();
            sheet.getByRole(AriaRole.BUTTON, new Locator.GetByRoleOptions().setName("Close").setExact(true)).click();
            assertThat(sheet).isHidden();
            assertThat(tab.getByRole(AriaRole.STATUS).filter(new Locator.FilterOptions().setHasText("Sheet:"))).hasText("Sheet: Check status.");
        });
    }

    @ParameterizedTest(name = "{0}, {1}")
    @MethodSource("everySkinAndMode")
    void dropdownTouchPositioningAndForcedColors(String skin, String mode) {
        check("menus", skin, mode, new Browser.NewContextOptions().setHasTouch(true).setViewportSize(390, 700), tab -> {
            for (String direction : List.of("ltr", "rtl")) {
                tab.locator("html").evaluate("(n, dir) => n.dir = dir", direction);
                Locator trigger = tab.getByRole(AriaRole.BUTTON, new Page.GetByRoleOptions().setName("Export as").setExact(true));
                trigger.tap();
                Locator menu = tab.getByRole(AriaRole.MENU, new Page.GetByRoleOptions().setName("Export as").setExact(true));
                menu.evaluate("n => Promise.all(n.getAnimations().map(a=>a.finished))");
                var rect = menu.boundingBox();
                assertThat(rect.x).isGreaterThanOrEqualTo(0); assertThat(rect.x + rect.width).isLessThanOrEqualTo(391);
                assertThat(rect.y).isGreaterThanOrEqualTo(0); assertThat(rect.y + rect.height).isLessThanOrEqualTo(701);
                menu.getByRole(AriaRole.MENUITEM, new Locator.GetByRoleOptions().setName("JSON").setExact(true)).tap();
                assertThat(menu).isHidden(); assertThat(trigger).isFocused();
                assertThat(tab.getByRole(AriaRole.STATUS).filter(new Locator.FilterOptions().setHasText("Export format:"))).hasText("Export format: JSON.");
            }
            Locator trigger = tab.getByRole(AriaRole.BUTTON, new Page.GetByRoleOptions().setName("Project actions").setExact(true));
            trigger.press("ArrowDown");
            Locator menu = tab.getByRole(AriaRole.MENU, new Page.GetByRoleOptions().setName("Project actions").setExact(true));
            tab.emulateMedia(new Page.EmulateMediaOptions().setForcedColors(ForcedColors.ACTIVE));
            assertThat(menu).hasCSS("border-top-width", "1px");
            assertThat(menu.getByRole(AriaRole.MENUITEM, new Locator.GetByRoleOptions().setName("Edit project"))).hasCSS("outline-style", "solid");
            tab.keyboard().press("Escape");
            tab.emulateMedia(new Page.EmulateMediaOptions().setForcedColors(ForcedColors.NONE));
        });
    }

    @ParameterizedTest(name = "{0}, {1}")
    @MethodSource("everySkinAndMode")
    void toastVariantsAreTextOnlyAndDoNotTakeFocus(String skin, String mode) {
        check("toasts", skin, mode, tab -> {
            for (String variant : List.of("default", "success", "info", "warning", "error")) {
                Locator trigger = tab.getByRole(AriaRole.BUTTON, new Page.GetByRoleOptions().setName("Show " + variant + " toast").setExact(true));
                trigger.click();
                Locator toast = tab.locator(".sw-toast[data-level='" + variant + "']");
                assertThat(toast).containsText("Your workspace is ready for the next step.");
                assertThat(trigger).isFocused();
                assertThat(toast.locator(".sw-toast-content")).hasAttribute("role", variant.equals("error") ? "alert" : "status");
                assertThat(toast.locator("svg")).hasCount(variant.equals("default") ? 0 : 1);
            }
            tab.getByRole(AriaRole.BUTTON, new Page.GetByRoleOptions().setName("Show literal toast")).click();
            assertThat(tab.locator(".sw-toast").last().locator(".sw-toast-title")).hasText("<b>Plain title</b>");
            assertThat(tab.locator(".sw-toast").last().locator(".sw-toast-description")).hasText("<strong>Zoë & 李</strong> stays plain text.");
            assertThat(tab.locator(".sw-toast b,.sw-toast strong")).hasCount(0);
            assertThat(tab.getByRole(AriaRole.REGION, new Page.GetByRoleOptions().setName("Notifications (Alt+T)"))).isVisible();
            tab.keyboard().press("Alt+KeyT");
            assertThat(tab.locator(".sw-toast").last().getByRole(AriaRole.BUTTON)).isFocused();
        });
    }

    @ParameterizedTest(name = "{0}, {1}")
    @MethodSource("everySkinAndMode")
    void toastKeyboardDismissalReturnsFocusAndOutsideEscapeLeavesThemAlone(String skin, String mode) {
        check("toasts", skin, mode, tab -> {
            Locator trigger = tab.getByRole(AriaRole.BUTTON, new Page.GetByRoleOptions().setName("Show three toasts"));
            trigger.click();
            assertThat(tab.locator(".sw-toast")).hasCount(3);
            tab.keyboard().press("Escape");
            assertThat(tab.locator(".sw-toast")).hasCount(3);
            tab.keyboard().press("Alt+KeyT");
            assertThat(tab.locator(".sw-toast").last().getByRole(AriaRole.BUTTON)).isFocused();
            assertThat((Boolean) tab.locator(".sw-toast").last().getByRole(AriaRole.BUTTON).evaluate(
                    "n=>n.matches(':focus-visible') && (getComputedStyle(n).boxShadow!=='none' || getComputedStyle(n).outlineStyle!=='none')")).isTrue();
            for (int remaining = 2; remaining >= 0; remaining--) {
                tab.keyboard().press("Escape");
                assertThat(tab.locator(".sw-toast")).hasCount(remaining);
                if (remaining > 0) assertThat(tab.locator(".sw-toast").last().getByRole(AriaRole.BUTTON)).isFocused();
            }
            assertThat(tab.locator("#sw-toaster")).hasCount(0);
            assertThat(trigger).isFocused();
            trigger.click();
            Locator first = tab.getByRole(AriaRole.BUTTON, new Page.GetByRoleOptions().setName("Dismiss notification")).first();
            first.focus(); tab.keyboard().press("Enter");
            assertThat(tab.locator(".sw-toast")).hasCount(2);
            assertThat(tab.getByRole(AriaRole.BUTTON, new Page.GetByRoleOptions().setName("Dismiss notification")).first()).isFocused();
        });
    }

    @ParameterizedTest(name = "{0}, {1}")
    @MethodSource("everySkinAndMode")
    void toastTimersPauseOnHoverFocusAndHiddenPages(String skin, String mode) {
        check("toasts", skin, mode, tab -> {
            tab.clock().install(new com.microsoft.playwright.Clock.InstallOptions().setTime(1000000));
            tab.clock().pauseAt(1000000);
            tab.evaluate("Shadwick.toast('Temporary',{duration:2000,title:'Timed'}); Shadwick.toast('Persistent',{duration:0})");
            tab.clock().runFor(40);
            Locator timed = tab.locator(".sw-toast").filter(new Locator.FilterOptions().setHasText("Temporary"));
            assertThat(timed).isVisible();
            tab.clock().runFor(1000);
            var box = timed.boundingBox(); tab.mouse().move(box.x + 10, box.y + 10);
            tab.clock().runFor(5000);
            assertThat(timed).isVisible();
            tab.mouse().move(0, 0);
            tab.clock().runFor(500);
            timed.getByRole(AriaRole.BUTTON).focus();
            tab.clock().runFor(5000);
            assertThat(timed).isVisible();
            tab.getByRole(AriaRole.BUTTON, new Page.GetByRoleOptions().setName("Keep until dismissed")).focus();
            tab.evaluate("Object.defineProperty(document,'hidden',{configurable:true,get:()=>true}); document.dispatchEvent(new Event('visibilitychange'))");
            tab.clock().runFor(5000);
            assertThat(timed).isVisible();
            tab.evaluate("delete document.hidden; document.dispatchEvent(new Event('visibilitychange'))");
            tab.clock().runFor(900);
            assertThat(timed).isVisible();
            tab.clock().runFor(200);
            assertThat(timed).hasCount(0);
            assertThat(tab.locator(".sw-toast")).hasCount(1);
            tab.clock().runFor(60000);
            assertThat(tab.locator(".sw-toast")).containsText("Persistent");
            tab.clock().resume();
        });
    }

    @ParameterizedTest(name = "{0}, {1}")
    @MethodSource("everySkinAndMode")
    void toastModalContextStaysInteractiveAndEscapeDismissesOnlyTheToast(String skin, String mode) {
        check("toasts", skin, mode, tab -> {
            tab.getByRole(AriaRole.BUTTON, new Page.GetByRoleOptions().setName("Keep until dismissed")).click();
            tab.getByRole(AriaRole.BUTTON, new Page.GetByRoleOptions().setName("Open toast dialog")).click();
            Locator dialog = tab.getByRole(AriaRole.DIALOG, new Page.GetByRoleOptions().setName("Notifications in a dialog"));
            assertThat(dialog.locator("#sw-toaster")).isVisible();
            dialog.getByRole(AriaRole.BUTTON, new Locator.GetByRoleOptions().setName("Notify inside dialog")).click();
            assertThat(dialog.locator(".sw-toast")).hasCount(2);
            assertThat(dialog.getByRole(AriaRole.BUTTON, new Locator.GetByRoleOptions().setName("Notify inside dialog"))).isFocused();
            tab.keyboard().press("Alt+KeyT"); tab.keyboard().press("Escape");
            assertThat(dialog).isVisible(); assertThat(dialog.locator(".sw-toast")).hasCount(1);
            tab.keyboard().press("Escape");
            assertThat(dialog).isVisible();
            assertThat(dialog.getByRole(AriaRole.BUTTON, new Locator.GetByRoleOptions().setName("Notify inside dialog"))).isFocused();
            dialog.getByRole(AriaRole.BUTTON, new Locator.GetByRoleOptions().setName("Notify and close")).click();
            assertThat(dialog).isHidden();
            assertThat(tab.locator("body > #sw-toaster")).containsText("The dialog has closed and this toast stays usable.");
            tab.keyboard().press("Alt+KeyT"); tab.keyboard().press("Escape");
            assertThat(tab.locator("#sw-toaster")).hasCount(0);
            tab.getByRole(AriaRole.BUTTON, new Page.GetByRoleOptions().setName("Open toast sheet")).click();
            Locator sheet = tab.getByRole(AriaRole.DIALOG, new Page.GetByRoleOptions().setName("Notifications in a sheet"));
            sheet.getByRole(AriaRole.BUTTON, new Locator.GetByRoleOptions().setName("Notify inside sheet")).click();
            assertThat(sheet.locator(".sw-toast")).hasCount(1);
            tab.keyboard().press("Alt+KeyT");
            sheet.evaluate("n => Promise.all(n.getAnimations().map(a=>a.finished))");
            assertThat(describe(new AxeBuilder(tab).withTags(WCAG).analyze().getViolations())).isEmpty();
            tab.keyboard().press("Escape"); assertThat(sheet).isVisible();
            sheet.getByRole(AriaRole.BUTTON, new Locator.GetByRoleOptions().setName("Close").setExact(true)).click();
            assertThat(sheet).isHidden();
        });
    }

    @ParameterizedTest(name = "{0}, {1}")
    @MethodSource("everySkinAndMode")
    void toastFullRenderAjaxReplacementAndRemovedViewportLeaveNoStaleMessages(String skin, String mode) {
        check("toasts", skin, mode, tab -> {
            tab.getByRole(AriaRole.LINK, new Page.GetByRoleOptions().setName("Full page toast")).click();
            assertThat(tab.locator(".sw-toast")).containsText("A notification from a full page render.");
            tab.keyboard().press("Alt+KeyT"); tab.keyboard().press("Escape");
            Locator replace = tab.getByRole(AriaRole.BUTTON, new Page.GetByRoleOptions().setName("Replace this button"));
            String oldId = replace.getAttribute("id");
            var oldElement = replace.elementHandle();
            replace.click();
            tab.waitForFunction("n => !n.isConnected", oldElement);
            assertThat(tab.locator(".sw-toast")).containsText("The Ajax update keeps this notification visible.");
            assertThat(replace).hasAttribute("id", oldId);
            tab.locator("#sw-toaster").evaluate("n=>n.remove()");
            tab.getByRole(AriaRole.BUTTON, new Page.GetByRoleOptions().setName("Keep until dismissed")).click();
            assertThat(tab.locator("#sw-toaster")).hasCount(1);
            assertThat(tab.locator(".sw-toast")).hasCount(1);
            assertThat(tab.locator(".sw-toast")).containsText("This message waits for you to dismiss it.");
            tab.keyboard().press("Alt+KeyT"); tab.keyboard().press("Escape");
            assertThat(tab.locator("#sw-toaster")).hasCount(0);
        });
    }

    @ParameterizedTest(name = "{0}, {1}")
    @MethodSource("everySkinAndMode")
    void toastMobileOverflowRtlAndForcedColors(String skin, String mode) {
        check("toasts", skin, mode, new Browser.NewContextOptions().setHasTouch(true).setViewportSize(320, 500), tab -> {
            tab.locator("html").evaluate("n=>n.dir='rtl'");
            tab.evaluate("for(let i=0;i<10;i++) Shadwick.toast('LongFilename'.repeat(30),{title:'Message '+i,variant:'warning',duration:0})");
            assertThat(tab.locator(".sw-toast")).hasCount(10);
            assertThat((Boolean) tab.locator("#sw-toaster").evaluate("n=>n.scrollHeight>n.clientHeight && n.scrollWidth<=n.clientWidth+1")).isTrue();
            var box = tab.locator("#sw-toaster").boundingBox();
            assertThat(box.x).isGreaterThanOrEqualTo(0); assertThat(box.x + box.width).isLessThanOrEqualTo(321);
            assertThat(box.y).isGreaterThanOrEqualTo(0); assertThat(box.y + box.height).isLessThanOrEqualTo(501);
            tab.locator(".sw-toast").last().getByRole(AriaRole.BUTTON).tap();
            assertThat(tab.locator(".sw-toast")).hasCount(9);
            tab.emulateMedia(new Page.EmulateMediaOptions().setForcedColors(ForcedColors.ACTIVE));
            assertThat(tab.locator(".sw-toast").last()).hasCSS("border-top-width", "1px");
            tab.keyboard().press("Alt+KeyT");
            assertThat(tab.locator(".sw-toast").last().getByRole(AriaRole.BUTTON)).isFocused();
            assertThat(tab.locator(".sw-toast").last().getByRole(AriaRole.BUTTON)).hasCSS("outline-style", "solid");
            tab.emulateMedia(new Page.EmulateMediaOptions().setForcedColors(ForcedColors.NONE));
        });
    }

    @ParameterizedTest(name = "{0}, {1}")
    @MethodSource("everySkinAndMode")
    void shellSkipLinkIsFirstAndMovesFocusPastGlobalNavigation(String skin, String mode) {
        check("shell", skin, mode, tab -> {
            tab.keyboard().press("Tab");
            Locator skip = tab.getByRole(AriaRole.LINK, new Page.GetByRoleOptions().setName("Skip to main content").setExact(true));
            assertThat(skip).isFocused();
            assertThat(skip).hasCSS("position", "fixed");
            assertThat(skip.boundingBox().x).isGreaterThanOrEqualTo(0);
            assertThat(describe(new AxeBuilder(tab).withTags(WCAG).analyze().getViolations())).isEmpty();
            tab.keyboard().press("Enter");
            assertThat(tab.getByRole(AriaRole.MAIN)).isFocused();
            assertThat(tab.getByRole(AriaRole.MAIN)).hasAttribute("tabindex", "-1");
            tab.keyboard().press("Tab");
            assertThat(tab.getByRole(AriaRole.MAIN).getByRole(AriaRole.LINK, new Locator.GetByRoleOptions().setName("Shadwick").setExact(true))).isFocused();
            assertThat(tab.getByRole(AriaRole.BANNER)).hasCount(1);
            tab.getByRole(AriaRole.BUTTON, new Page.GetByRoleOptions().setName("Save view").setExact(true)).click();
            assertThat(tab.getByRole(AriaRole.STATUS)).hasText("View saved.");
        });
    }

    @ParameterizedTest(name = "{0}, {1}")
    @MethodSource("everySkinAndMode")
    void shellBreadcrumbOverflowNavigatesWithKeyboardAndKeepsCurrentSemantics(String skin, String mode) {
        check("shell", skin, mode, tab -> {
            Locator trail = tab.getByRole(AriaRole.NAVIGATION, new Page.GetByRoleOptions().setName("Workspace path").setExact(true));
            assertThat(trail.locator("[aria-current='page']")).hasCount(1);
            assertThat(trail.locator("[aria-current='page']")).hasText("Overview");
            assertThat(trail.locator("span[aria-current='page']")).hasCount(1);
            assertThat(trail.getByRole(AriaRole.LISTITEM)).hasCount(4);
            Locator more = trail.getByRole(AriaRole.BUTTON, new Locator.GetByRoleOptions().setName("Show more levels"));
            more.press("ArrowDown");
            Locator menu = trail.getByRole(AriaRole.MENU, new Locator.GetByRoleOptions().setName("Show more levels"));
            assertThat(menu.getByRole(AriaRole.MENUITEM, new Locator.GetByRoleOptions().setName("Workspace").setExact(true))).isFocused();
            tab.keyboard().press("End");
            assertThat(menu.getByRole(AriaRole.MENUITEM, new Locator.GetByRoleOptions().setName("Design system").setExact(true))).isFocused();
            tab.keyboard().press("p");
            assertThat(menu.getByRole(AriaRole.MENUITEM, new Locator.GetByRoleOptions().setName("Projects").setExact(true))).isFocused();
            menu.evaluate("n=>Promise.all(n.getAnimations().map(a=>a.finished))");
            assertThat(describe(new AxeBuilder(tab).withTags(WCAG).analyze().getViolations())).isEmpty();
            tab.keyboard().press("Escape");
            assertThat(menu).isHidden(); assertThat(more).isFocused();
            more.press("ArrowUp");
            Locator design = menu.getByRole(AriaRole.MENUITEM, new Locator.GetByRoleOptions().setName("Design system").setExact(true));
            assertThat(design).isFocused(); design.press("Enter");
            tab.waitForURL("**/shell*page=Design*");
            assertThat(tab.getByRole(AriaRole.STATUS)).hasText("Viewing: Design system.");
            assertThat(trail.locator("[aria-current='page']")).hasText("Design system");
        });
    }

    @ParameterizedTest(name = "{0}, {1}")
    @MethodSource("everySkinAndMode")
    void shellBreadcrumbAjaxClearsRestoresAndDismissesReplacedMenus(String skin, String mode) {
        check("shell", skin, mode, tab -> {
            Locator trail = tab.getByRole(AriaRole.NAVIGATION, new Page.GetByRoleOptions().setName("Workspace path").setExact(true));
            trail.getByRole(AriaRole.BUTTON, new Locator.GetByRoleOptions().setName("Show more levels")).click();
            assertThat(trail.getByRole(AriaRole.MENU)).isVisible();
            tab.getByRole(AriaRole.BUTTON, new Page.GetByRoleOptions().setName("Refresh breadcrumb")).evaluate("n=>n.click()");
            assertThat(tab.getByRole(AriaRole.STATUS)).hasText("Breadcrumb refreshed.");
            assertThat(trail.locator("[aria-current='page']")).hasText("Updated overview");
            assertThat(trail.getByRole(AriaRole.MENU)).hasCount(0);
            assertThat(trail.getByRole(AriaRole.BUTTON)).hasAttribute("aria-expanded", "false");
            tab.getByRole(AriaRole.BUTTON, new Page.GetByRoleOptions().setName("Clear breadcrumb")).click();
            assertThat(tab.getByRole(AriaRole.STATUS)).hasText("Breadcrumb cleared.");
            assertThat(trail).hasCount(0);
            tab.getByRole(AriaRole.BUTTON, new Page.GetByRoleOptions().setName("Restore breadcrumb")).click();
            assertThat(trail).isVisible();
            assertThat(trail.locator("[aria-current='page']")).hasText("Overview");
            trail.getByRole(AriaRole.BUTTON).press("ArrowDown");
            assertThat(trail.getByRole(AriaRole.MENUITEM, new Locator.GetByRoleOptions().setName("Workspace").setExact(true))).isFocused();
            tab.keyboard().press("Escape");
            assertThat(tab.locator("#main")).hasAttribute("tabindex", "-1");
        });
    }

    @ParameterizedTest(name = "{0}, {1}")
    @MethodSource("everySkinAndMode")
    void shellHeadersAndLongBreadcrumbsWrapAndReverseInRtl(String skin, String mode) {
        check("shell", skin, mode, new Browser.NewContextOptions().setViewportSize(320, 700), tab -> {
            tab.getByRole(AriaRole.BUTTON, new Page.GetByRoleOptions().setName("Use long labels")).click();
            assertThat(tab.getByRole(AriaRole.STATUS)).hasText("Long labels applied.");
            for (String direction : List.of("ltr", "rtl")) {
                tab.locator("html").evaluate("(n,dir)=>n.dir=dir", direction);
                assertThat((Boolean) tab.evaluate("document.documentElement.scrollWidth<=innerWidth+1")).isTrue();
                Locator trail = tab.getByRole(AriaRole.NAVIGATION, new Page.GetByRoleOptions().setName("Workspace path").setExact(true));
                assertThat((Boolean) trail.evaluate("n=>n.scrollWidth<=n.clientWidth+1")).isTrue();
                assertThat(trail.locator(".sw-breadcrumb-separator-icon").first()).hasCSS("scale", direction.equals("rtl") ? "-1 1" : "none");
                assertThat(tab.locator(".sw-site-header[data-inset='true']")).hasCSS("border-bottom-width", "0px");
            }
        });
    }

    @ParameterizedTest(name = "{0}, {1}")
    @MethodSource("everySkinAndMode")
    void shellSkipAndBreadcrumbLinksWorkWithoutJavaScriptAndHaveForcedColorFocus(String skin, String mode) {
        check("shell", skin, mode, new Browser.NewContextOptions().setJavaScriptEnabled(false), tab -> {
            tab.keyboard().press("Tab");
            Locator skip = tab.getByRole(AriaRole.LINK, new Page.GetByRoleOptions().setName("Skip to main content"));
            assertThat(skip).isFocused(); tab.keyboard().press("Enter");
            assertThat(tab.getByRole(AriaRole.MAIN)).isFocused();
            tab.emulateMedia(new Page.EmulateMediaOptions().setForcedColors(ForcedColors.ACTIVE));
            skip.focus();
            assertThat(skip).hasCSS("outline-style", "solid");
            Locator home = tab.getByRole(AriaRole.NAVIGATION, new Page.GetByRoleOptions().setName("Simple path").setExact(true))
                    .getByRole(AriaRole.LINK, new Locator.GetByRoleOptions().setName("Home").setExact(true));
            home.focus(); tab.keyboard().press("Tab"); tab.keyboard().press("Shift+Tab");
            assertThat(home).hasCSS("outline-style", "solid");
            home.press("Enter");
            tab.waitForURL("**/shell*page=Home*");
            assertThat(tab.getByRole(AriaRole.STATUS)).hasText("Viewing: Home.");
            tab.emulateMedia(new Page.EmulateMediaOptions().setForcedColors(ForcedColors.NONE));
        });
    }

    private static List<String> describe(List<Rule> violations) {
        return violations.stream().map(rule -> rule.getId() + " (" + rule.getImpact() + "): " + rule.getHelp()
                        + rule.getNodes().stream().map(ShowcaseBrowserTest::describe).collect(Collectors.joining()))
                .toList();
    }

    private static String describe(CheckedNode node) {
        return "\n    " + node.getTarget() + ": " + String.valueOf(node.getFailureSummary()).replace("\n", " ");
    }
    @ParameterizedTest(name = "{0}, {1}")
    @MethodSource("everySkinAndMode")
    void sidebarDesktopCollapsePreservesNamesAndRemembersThePreference(String skin, String mode) {
        check("sidebar", skin, mode, tab -> {
            Locator nav = tab.getByRole(AriaRole.NAVIGATION, new Page.GetByRoleOptions().setName("Workspace sidebar"));
            Locator trigger = tab.getByRole(AriaRole.BUTTON, new Page.GetByRoleOptions().setName("Toggle sidebar"));
            assertThat(trigger).hasAttribute("aria-expanded", "true");
            trigger.click();
            assertThat(trigger).hasAttribute("aria-expanded", "false");
            assertThat(nav.getByRole(AriaRole.LINK, new Locator.GetByRoleOptions().setName("Inbox").setExact(true))).isVisible();
            assertThat(nav).hasCSS("width", "64px"); // inset includes its padding around the icon rail
            Locator inbox = nav.getByRole(AriaRole.LINK, new Locator.GetByRoleOptions().setName("Inbox").setExact(true));
            inbox.focus(); tab.keyboard().press("Tab"); tab.keyboard().press("Shift+Tab");
            assertThat(tab.locator(".sw-sidebar-menu-tooltip:popover-open")).hasCount(1);
            tab.keyboard().press("Escape");
            tab.reload(); assertThat(trigger).hasAttribute("aria-expanded", "false");
            tab.keyboard().press("Control+b"); assertThat(trigger).hasAttribute("aria-expanded", "true");
            trigger.focus(); tab.keyboard().press("Control+Shift+b");
            assertThat(trigger).hasAttribute("aria-expanded", "true");
            tab.getByRole(AriaRole.TEXTBOX).focus(); tab.keyboard().press("Control+b");
            assertThat(trigger).hasAttribute("aria-expanded", "true");
            assertThat(tab.locator(".sw-sidebar-menu-tooltip:popover-open")).hasCount(0);
        });
    }

    @ParameterizedTest(name = "{0}, {1}")
    @MethodSource("everySkinAndMode")
    void sidebarMobileTrapsFocusAndRestoresOnlyOwnedInertState(String skin, String mode) {
        check("sidebar", skin, mode, new Browser.NewContextOptions().setViewportSize(390, 800), tab -> {
            Locator nav = tab.locator(".sw-sidebar"), trigger = tab.locator(".sw-sidebar-trigger");
            tab.evaluate("() => { const e=document.createElement('div'); e.id='preexisting-inert'; e.inert=true; document.body.appendChild(e); }");
            trigger.click();
            assertThat(nav).isVisible();
            assertThat(nav.locator("a[aria-current=page]")).isFocused();
            assertThat(tab.locator(".sw-sidebar-inset")).hasAttribute("inert", "");
            assertThat(describe(new AxeBuilder(tab).withTags(WCAG).analyze().getViolations())).isEmpty();
            nav.locator(".sw-sidebar-close").focus(); tab.keyboard().press("Tab");
            assertThat(nav.getByRole(AriaRole.LINK).first()).isFocused();
            tab.keyboard().press("Shift+Tab"); assertThat(nav.locator(".sw-sidebar-close")).isFocused();
            tab.keyboard().press("Escape"); assertThat(trigger).isFocused();
            assertThat(nav).isHidden(); assertThat(tab.locator(".sw-sidebar-inset")).not().hasAttribute("inert", "");
            assertThat(tab.locator("#preexisting-inert")).hasAttribute("inert", "");
            trigger.click(); tab.mouse().click(380, 400); assertThat(nav).isHidden();
            trigger.click(); tab.setViewportSize(1200, 800);
            assertThat(nav).isVisible(); assertThat(nav).not().hasAttribute("inert", "");
            assertThat(tab.locator(".sw-sidebar:popover-open")).hasCount(0);
            assertThat(tab.locator(".sw-sidebar-inset")).not().hasAttribute("inert", "");
        });
    }

    @ParameterizedTest(name = "{0}, {1}")
    @MethodSource("everySkinAndMode")
    void sidebarBadgesUpdateWithoutClosingAndReplacingThePanelReleasesFocus(String skin, String mode) {
        check("sidebar", skin, mode, new Browser.NewContextOptions().setViewportSize(390, 800), tab -> {
            Locator nav = tab.locator(".sw-sidebar"), trigger = tab.locator(".sw-sidebar-trigger"); trigger.click();
            assertThat(nav.locator(".sw-sidebar-menu-badge")).hasText("3");
            nav.getByRole(AriaRole.BUTTON, new Locator.GetByRoleOptions().setName("Mark all read")).click();
            assertThat(nav.locator(".sw-sidebar-menu-badge")).hasCount(0);
            assertThat(nav).isVisible(); assertThat(nav.locator("button").last()).isVisible();
            // Exercise a full Ajax replacement from inside the otherwise inert panel.
            tab.evaluate("() => document.querySelector('.sw-sidebar').appendChild(document.querySelector('button[id^=refresh]'))");
            nav.getByRole(AriaRole.BUTTON, new Locator.GetByRoleOptions().setName("Refresh sidebar")).click();
            assertThat(nav).isHidden(); assertThat(tab.locator(".sw-sidebar-inset")).not().hasAttribute("inert", "");
            trigger.click(); assertThat(nav.locator(".sw-sidebar-menu-badge")).hasText("3");
            nav.getByRole(AriaRole.LINK, new Locator.GetByRoleOptions().setName("Inbox").setExact(true)).click();
            assertThat(tab.getByRole(AriaRole.HEADING, new Page.GetByRoleOptions().setName("Inbox").setExact(true))).isVisible();
            trigger.click();
            assertThat(nav.locator("a[aria-current=page]")).isFocused();
            tab.evaluate("() => document.querySelector('.sw-sidebar-provider').remove()");
            assertThat(tab.locator("body > [inert]")).hasCount(0);
        });
    }

    @ParameterizedTest(name = "{0}, {1}")
    @MethodSource("everySkinAndMode")
    void sidebarOffcanvasNoneEndAndRtlLayoutsFit(String skin, String mode) {
        check("sidebar", skin, mode, tab -> {
            tab.getByRole(AriaRole.LINK, new Page.GetByRoleOptions().setName("Off-canvas").setExact(true)).click();
            Locator trigger = tab.locator(".sw-sidebar-trigger"), nav = tab.locator(".sw-sidebar");
            trigger.click(); assertThat(nav).isHidden();
            assertThat(nav.locator("a").first()).not().isInViewport();
            trigger.click(); assertThat(nav).isVisible();
            nav.locator("a[aria-current=page]").focus(); tab.keyboard().press("Control+b");
            assertThat(trigger).isFocused(); tab.keyboard().press("Control+b");
            tab.getByRole(AriaRole.LINK, new Page.GetByRoleOptions().setName("End").setExact(true)).click();
            assertThat(nav.boundingBox().x).isGreaterThan(tab.locator(".sw-sidebar-inset").boundingBox().x);
            tab.locator("html").evaluate("n => n.dir='rtl'");
            assertThat(nav.boundingBox().x).isLessThan(tab.locator(".sw-sidebar-inset").boundingBox().x);
            tab.getByRole(AriaRole.LINK, new Page.GetByRoleOptions().setName("Always open").setExact(true)).click();
            assertThat(trigger).isHidden(); assertThat(nav).isVisible();
            tab.setViewportSize(320, 700); assertThat(trigger).isVisible(); trigger.click();
            assertThat(nav.boundingBox().width).isLessThanOrEqualTo(272.1);
            tab.keyboard().press("Escape");
            assertThat((Boolean) tab.evaluate("() => document.documentElement.scrollWidth <= innerWidth")).isTrue();
        });
    }

    @ParameterizedTest(name = "{0}, {1}")
    @MethodSource("everySkinAndMode")
    void sidebarNativeNavigationWorksWithoutJavaScript(String skin, String mode) {
        check("sidebar", skin, mode, new Browser.NewContextOptions().setJavaScriptEnabled(false).setViewportSize(390, 800), tab -> {
            tab.context().addCookies(List.of(new com.microsoft.playwright.options.Cookie("sw-sidebar-state", "collapsed")
                    .setUrl(url("sidebar", skin, mode))));
            tab.reload();
            tab.locator(".sw-sidebar-trigger").click();
            Locator nav = tab.locator(".sw-sidebar"); assertThat(nav).isVisible();
            nav.getByRole(AriaRole.LINK, new Locator.GetByRoleOptions().setName("Inbox").setExact(true)).click();
            assertThat(tab.getByRole(AriaRole.HEADING, new Page.GetByRoleOptions().setName("Inbox").setExact(true))).isVisible();
            tab.setViewportSize(1200, 800); assertThat(nav).isVisible(); assertThat(tab.locator(".sw-sidebar-trigger")).isHidden();
            assertThat(nav.boundingBox().width).isGreaterThan(200);
        });
    }

    @ParameterizedTest(name = "{0}, {1}")
    @MethodSource("everySkinAndMode")
    void sidebarNestedMenusAndToastsHandleEscapeBeforeThePanel(String skin, String mode) {
        check("sidebar", skin, mode, new Browser.NewContextOptions().setViewportSize(390, 800), tab -> {
            Locator nav = tab.locator(".sw-sidebar"); tab.locator(".sw-sidebar-trigger").click();
            nav.evaluate("n => n.insertAdjacentHTML('beforeend', `<button id='nested-trigger' type='button' popovertarget='nested-menu' aria-haspopup='menu' aria-expanded='false'>More actions</button><div id='nested-menu' class='sw-dropdown-menu-content' popover='auto' role='menu' aria-label='More actions' tabindex='-1'><button type='button' role='menuitem'>A nested action</button></div>`)");
            tab.locator("#nested-trigger").click();
            assertThat(tab.locator("#nested-menu")).isVisible();
            tab.keyboard().press("Escape"); assertThat(tab.locator("#nested-menu")).isHidden();
            assertThat(nav).isVisible(); assertThat(tab.locator("#nested-trigger")).isFocused();
            // Escape may arrive before the nested popup's deferred initial focus.
            for (int attempt = 0; attempt < 6; attempt++) {
                tab.locator("#nested-trigger").click(); tab.keyboard().press("Escape");
                assertThat(tab.locator("#nested-menu")).isHidden(); assertThat(nav).isVisible();
                assertThat(tab.locator("#nested-trigger")).isFocused();
            }
            for (int attempt = 0; attempt < 6; attempt++) {
                tab.locator("#nested-trigger").focus(); tab.keyboard().press("ArrowDown");
                assertThat(tab.locator("#nested-menu")).isVisible();
                tab.keyboard().press("Tab");
                assertThat(tab.locator("#nested-menu")).isHidden();
                assertThat(nav.getByRole(AriaRole.LINK).first()).isFocused();
            }
            tab.evaluate("() => Shadwick.toast('Within the sidebar', {duration:0})");
            assertThat(nav.locator(".sw-toaster")).hasCount(1);
            tab.keyboard().press("Alt+t");
            assertThat(nav.locator(".sw-toast-close")).isFocused();
            tab.keyboard().press("Escape"); assertThat(nav.locator(".sw-toast")).hasCount(0); assertThat(nav).isVisible();
            tab.keyboard().press("Escape"); assertThat(nav).isHidden();
        });
    }

    @ParameterizedTest(name = "{0}, {1}")
    @MethodSource("everySkinAndMode")
    void sidebarForcedColorsAndReducedMotionKeepCurrentAndFocusedLinksVisible(String skin, String mode) {
        check("sidebar", skin, mode, tab -> {
            tab.emulateMedia(new Page.EmulateMediaOptions().setForcedColors(ForcedColors.ACTIVE)
                    .setReducedMotion(com.microsoft.playwright.options.ReducedMotion.REDUCE));
            tab.locator(".sw-sidebar-trigger").click();
            Locator current = tab.locator(".sw-sidebar a[aria-current=page]");
            current.focus(); tab.keyboard().press("Tab"); tab.keyboard().press("Shift+Tab");
            assertThat(current).isFocused(); assertThat(current).hasCSS("outline-style", "solid");
            assertThat(current).hasCSS("outline-width", "2px");
            tab.keyboard().press("Escape");
            tab.setViewportSize(390, 800); tab.locator(".sw-sidebar-trigger").click();
            assertThat(current).isFocused();
            assertThat(tab.locator(".sw-sidebar:popover-open")).hasCount(1);
            tab.keyboard().press("Escape"); assertThat(tab.locator(".sw-sidebar-trigger")).isFocused();
            tab.emulateMedia(new Page.EmulateMediaOptions().setForcedColors(ForcedColors.NONE));
        });
    }

}
