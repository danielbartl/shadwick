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

    private static final List<String> PAGES = List.of("", "buttons", "form", "inputs", "choices", "selections", "dates", "groups", "uploads", "dialog", "display", "feedback");
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
        try (BrowserContext context = browser.newContext(); Page tab = context.newPage()) {
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

            String where = "/" + page + " in " + skin + ", " + mode;
            assertThat(describe(new AxeBuilder(tab).withTags(WCAG).analyze().getViolations()))
                    .as("axe-core's WCAG violations on " + where).isEmpty();
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

    private static List<String> describe(List<Rule> violations) {
        return violations.stream().map(rule -> rule.getId() + " (" + rule.getImpact() + "): " + rule.getHelp()
                        + rule.getNodes().stream().map(ShowcaseBrowserTest::describe).collect(Collectors.joining()))
                .toList();
    }

    private static String describe(CheckedNode node) {
        return "\n    " + node.getTarget() + ": " + String.valueOf(node.getFailureSummary()).replace("\n", " ");
    }
}
