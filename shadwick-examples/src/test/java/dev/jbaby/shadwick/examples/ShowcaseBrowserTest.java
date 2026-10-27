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

    private static final List<String> PAGES = List.of("", "buttons", "form", "dialog");
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

    private static List<String> describe(List<Rule> violations) {
        return violations.stream().map(rule -> rule.getId() + " (" + rule.getImpact() + "): " + rule.getHelp()
                        + rule.getNodes().stream().map(ShowcaseBrowserTest::describe).collect(Collectors.joining()))
                .toList();
    }

    private static String describe(CheckedNode node) {
        return "\n    " + node.getTarget() + ": " + String.valueOf(node.getFailureSummary()).replace("\n", " ");
    }
}
