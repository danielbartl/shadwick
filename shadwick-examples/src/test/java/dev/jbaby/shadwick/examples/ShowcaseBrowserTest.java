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
import org.junit.jupiter.params.provider.CsvSource;
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

    private static final List<String> PAGES = List.of("", "internationalization", "status-controls", "advanced-tables", "buttons", "form", "inputs", "choices", "selections", "custom-select", "combobox", "calendar", "month-view", "charts", "dates", "groups", "uploads", "dialog", "overlays", "popups", "menus", "toasts", "shell", "sidebar", "user-menu", "demo-sign-in", "theme", "layouts", "header-layout", "disclosure", "attachments", "data", "invoice", "tables", "pagination", "display", "feedback");
    private static final List<String> SKINS = browserSkins();

    private static List<String> browserSkins() {
        String selected = System.getProperty("shadwick.browser.skins");
        if (selected == null) return java.util.Arrays.stream(dev.jbaby.shadwick.Skin.values()).map(dev.jbaby.shadwick.Skin::id).toList();
        return java.util.Arrays.stream(selected.split(","))
                .map(value -> dev.jbaby.shadwick.Skin.valueOf(value.strip().toUpperCase(java.util.Locale.ROOT)).id()).toList();
    }
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

    static Stream<Arguments> nativeLanguagesInEverySkinAndMode() {
        return SKINS.stream().flatMap(skin -> MODES.stream().flatMap(mode ->
                Stream.of("ja", "hr").map(language -> Arguments.of(skin, mode, language))));
    }

    private void settleAnimations(Page tab) {
        tab.waitForFunction("() => document.getAnimations().every(a => !(a.timeline instanceof DocumentTimeline) || !a.effect || a.effect.getTiming().iterations === Infinity"
                + " || a.playState === 'finished' || a.playState === 'idle')");
    }

    private String url(String page, String skin, String mode) {
        return "http://localhost:" + port + "/" + page + "?skin=" + skin + "&mode=" + mode;
    }

    /** Opens the page in a fresh browser session, runs the steps, and checks it with axe-core and for console errors. */
    private void check(String page, String skin, String mode, Consumer<Page> steps) {
        check(page, skin, mode, new Browser.NewContextOptions(), steps);
    }

    private void check(String page, String skin, String mode, Browser.NewContextOptions options, Consumer<Page> steps) {
        check(page, skin, mode, options, steps, List.of());
    }

    private void check(String page, String skin, String mode, Browser.NewContextOptions options, Consumer<Page> steps, List<String> expectedErrors) {
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

            // Poll the current animations rather than retaining finished promises from transitions that
            // disappear during native details/visibility changes. A stalled transition gets a bounded failure.
            if (!Boolean.FALSE.equals(options.javaScriptEnabled)) {
                settleAnimations(tab);
            }

            String where = "/" + page + " in " + skin + ", " + mode;
            // axe injects scripts; no-JavaScript navigation is checked separately from the normal page audits.
            if (!Boolean.FALSE.equals(options.javaScriptEnabled)) {
                assertThat(describe(new AxeBuilder(tab).withTags(WCAG).analyze().getViolations()))
                        .as("axe-core's WCAG violations on " + where).isEmpty();
            }
            assertThat(errors).as("console errors on " + where).containsExactlyInAnyOrderElementsOf(expectedErrors);
        }
    }

    @ParameterizedTest(name = "busy controls, {0}, {1}")
    @MethodSource("everySkinAndMode")
    void busyActionsDropDuplicatesRecoverAndSurviveReplacement(String skin, String mode) {
        check("status-controls", skin, mode, tab -> {
            var pending = new java.util.concurrent.atomic.AtomicReference<com.microsoft.playwright.Route>();
            var requests = new java.util.concurrent.atomic.AtomicInteger();
            tab.route("**/*", route -> {
                if ("true".equalsIgnoreCase(route.request().headers().get("wicket-ajax"))) {
                    if (requests.incrementAndGet() == 1) pending.set(route); else route.resume();
                } else route.resume();
            });
            Locator button = tab.locator("#busy-action"); button.click(); tab.waitForCondition(() -> pending.get() != null);
            assertThat(button).hasAttribute("aria-busy", "true"); assertThat(button).isFocused();
            assertThat(button.locator(".sw-busy-spinner")).isVisible();
            button.evaluate("n=>{n.click();n.click();n.click()}"); pending.get().resume();
            assertThat(tab.getByRole(AriaRole.STATUS).first()).hasText("Completed requests: 1");
            assertThat(button).not().hasAttribute("aria-busy", "true"); assertThat(button.locator(".sw-busy-indicator")).hasCount(0);
            assertThat(button).isFocused();
            tab.locator("#busy-replace").click(); assertThat(tab.locator("#busy-replace")).not().hasAttribute("aria-busy", "true");
            tab.locator("#busy-link").click(); assertThat(tab.getByRole(AriaRole.STATUS).first()).hasText("Completed requests: 2");
            assertThat(tab.locator("#busy-link")).not().hasAttribute("aria-disabled", "true");
            var iconPending = new java.util.concurrent.atomic.AtomicReference<com.microsoft.playwright.Route>();
            tab.route("**/*", route -> {
                if ("true".equalsIgnoreCase(route.request().headers().get("wicket-ajax"))) iconPending.set(route); else route.resume();
            });
            Locator icon = tab.locator("#busy-icon"); icon.click(); tab.waitForCondition(() -> iconPending.get() != null);
            assertThat(icon.locator(".sw-busy-spinner")).isVisible(); assertThat(icon.locator("svg")).isHidden();
            iconPending.get().resume(); assertThat(tab.getByRole(AriaRole.STATUS).first()).hasText("Completed requests: 3");
            assertThat(icon.locator("svg")).isVisible();
        });
    }

    @ParameterizedTest(name = "busy cancellation/error, {0}, {1}")
    @MethodSource("everySkinAndMode")
    void busyControlsRestoreAfterCancelledPreconditionsAndTransportFailure(String skin, String mode) {
        check("status-controls", skin, mode, new Browser.NewContextOptions(), tab -> {
            tab.locator("#busy-cancel").click(); assertThat(tab.locator("#busy-cancel")).not().hasAttribute("aria-busy", "true");
            var abort = new java.util.concurrent.atomic.AtomicBoolean(true);
            tab.route("**/*", route -> {
                if ("true".equalsIgnoreCase(route.request().headers().get("wicket-ajax")) && abort.getAndSet(false)) route.abort(); else route.resume();
            });
            tab.locator("#busy-action").click();
            assertThat(tab.locator("#busy-action")).not().hasAttribute("aria-busy", "true");
            tab.locator("#busy-action").click(); assertThat(tab.getByRole(AriaRole.STATUS).first()).hasText("Completed requests: 1");
        }, List.of("Failed to load resource: net::ERR_FAILED"));
    }

    @ParameterizedTest(name = "rating validation/submission, {0}, {1}")
    @MethodSource("everySkinAndMode")
    void ratingValidationKeyboardAndNativeSubmittersWorkWithBusyFeedback(String skin, String mode) {
        check("status-controls", skin, mode, tab -> {
            Locator form = tab.getByRole(AriaRole.FORM, new Page.GetByRoleOptions().setName("Rating form"));
            Locator save = form.getByRole(AriaRole.BUTTON, new Locator.GetByRoleOptions().setName("Save rating with Ajax"));
            save.click(); assertThat(save).not().hasAttribute("aria-busy", "true");
            assertThat(form.locator(".sw-field-error").first()).containsText("required");
            form.getByLabel("Your name", new Locator.GetByLabelOptions().setExact(true)).fill("Ada");
            Locator group = form.getByRole(AriaRole.RADIOGROUP, new Locator.GetByRoleOptions().setName("Your rating"));
            Locator first = group.getByRole(AriaRole.RADIO).first(); first.focus(); first.press("ArrowRight");
            assertThat(group.getByRole(AriaRole.RADIO).nth(1)).isChecked();
            save.focus(); save.press("Enter"); assertThat(form.getByRole(AriaRole.STATUS).last()).hasText("Saved Ada: 2 stars.");
            assertThat(form.locator(".sw-field-error").first()).isEmpty();
            group.locator("label").nth(3).click();
            form.getByRole(AriaRole.BUTTON, new Locator.GetByRoleOptions().setName("Save with input")).click();
            assertThat(form.getByRole(AriaRole.STATUS).last()).hasText("Saved Ada: 4 stars.");
            assertThat(form.locator(".sw-busy-indicator")).hasCount(0);
            tab.getByRole(AriaRole.BUTTON, new Page.GetByRoleOptions().setName("Open busy dialog")).click();
            Locator dialog = tab.getByRole(AriaRole.DIALOG, new Page.GetByRoleOptions().setName("Confirm server action"));
            dialog.getByRole(AriaRole.BUTTON, new Locator.GetByRoleOptions().setName("Confirm").setExact(true)).click();
            assertThat(dialog).isHidden(); assertThat(tab.getByRole(AriaRole.STATUS).first()).hasText("Completed requests: 1");
        });
    }

    @ParameterizedTest(name = "value controls responsive/RTL, {0}, {1}")
    @MethodSource("everySkinAndMode")
    void valueControlsRefreshAndKeepAccessibleValuesInRtlForcedColorsAndReducedMotion(String skin, String mode) {
        check("status-controls", skin, mode, new Browser.NewContextOptions().setViewportSize(390, 850)
                .setReducedMotion(com.microsoft.playwright.options.ReducedMotion.REDUCE), tab -> {
            tab.locator("html").evaluate("n=>n.dir='rtl'");
            assertThat(tab.getByRole(AriaRole.PROGRESSBAR, new Page.GetByRoleOptions().setName("Profile completion"))).hasAttribute("aria-valuenow", "25");
            tab.getByRole(AriaRole.BUTTON, new Page.GetByRoleOptions().setName("Advance work")).click();
            assertThat(tab.getByRole(AriaRole.PROGRESSBAR, new Page.GetByRoleOptions().setName("Profile completion"))).hasAttribute("aria-valuenow", "50");
            assertThat((Double) tab.locator("progress").first().evaluate("n=>n.position")).isEqualTo(.5);
            assertThat(tab.locator(".sw-rating").first()).hasAttribute("aria-label", "4.5 out of 5 stars");
            assertThat(tab.locator(".sw-rating [data-fill=half]").first().locator(".sw-rating-fill")).hasCSS("clip-path", "inset(0px 0px 0px 50%)");
            tab.emulateMedia(new Page.EmulateMediaOptions().setForcedColors(ForcedColors.ACTIVE));
            assertThat(tab.locator("progress").first()).hasCSS("appearance", "auto");
            Locator stars = tab.getByRole(AriaRole.RADIOGROUP, new Page.GetByRoleOptions().setName("Your rating"));
            stars.getByRole(AriaRole.RADIO).first().focus(); stars.getByRole(AriaRole.RADIO).first().press("Space");
            assertThat(stars.locator("label").first()).hasCSS("outline-style", "solid");
            assertThat(stars.locator("label").first().locator(".sw-rating-fill")).isVisible();
            assertThat(stars.locator("label").nth(1).locator(".sw-rating-fill")).isHidden();
            tab.emulateMedia(new Page.EmulateMediaOptions().setForcedColors(ForcedColors.NONE));
            assertThat((Integer) tab.evaluate("document.documentElement.scrollWidth-innerWidth")).isLessThanOrEqualTo(1);
        });
    }

    @ParameterizedTest(name = "native rating without script, {0}, {1}")
    @MethodSource("everySkinAndMode")
    void ratingsAndMeasurementsRemainUsableWithoutScript(String skin, String mode) {
        check("status-controls", skin, mode, new Browser.NewContextOptions().setJavaScriptEnabled(false).setViewportSize(390, 850), tab -> {
            Locator form = tab.getByRole(AriaRole.FORM, new Page.GetByRoleOptions().setName("Rating form"));
            form.getByLabel("Your name", new Locator.GetByLabelOptions().setExact(true)).fill("Grace");
            form.getByRole(AriaRole.RADIOGROUP, new Locator.GetByRoleOptions().setName("Your rating")).locator("label").nth(4).click();
            form.getByRole(AriaRole.BUTTON, new Locator.GetByRoleOptions().setName("Save rating").setExact(true)).click();
            assertThat(form.getByRole(AriaRole.STATUS).last()).hasText("Saved Grace: 5 stars.");
            assertThat(tab.locator("progress").first()).isVisible(); assertThat(tab.locator("meter").first()).isVisible();
        });
    }

    @ParameterizedTest(name = "status control messages in {0}")
    @CsvSource({"en,Loading,4.5 out of 5 stars,Stars: 1", "de,Wird geladen,'4,5 von 5 Sternen',Sterne: 1",
            "es,Cargando,'4,5 de 5 estrellas',Estrellas: 1", "fr,Chargement,'4,5 sur 5 étoiles',Étoiles : 1",
            "it,Caricamento,'4,5 su 5 stelle',Stelle: 1", "ja,読み込み中,5 つ星のうち 4.5,星の数：1",
            "nl,Laden,'4,5 van 5 sterren',Sterren: 1", "pt,Carregando,'4,5 de 5 estrelas',Estrelas: 1",
            "hr-HR,Učitavanje,'4,5 od 5 zvjezdica',Broj zvjezdica: 1"})
    void statusControlMessagesFollowTheLocale(String language, String loading, String rating, String star) {
        check("status-controls", "sketch", "dark", new Browser.NewContextOptions().setViewportSize(390, 850), tab -> {
            tab.navigate(url("status-controls", "sketch", "dark") + "&lang=" + language);
            assertThat(tab.locator(".sw-rating").first()).hasAttribute("aria-label", rating);
            assertThat(tab.getByRole(AriaRole.RADIOGROUP, new Page.GetByRoleOptions().setName("Your rating"))
                    .getByRole(AriaRole.RADIO).first()).hasAccessibleName(star);
            var pending = new java.util.concurrent.atomic.AtomicReference<com.microsoft.playwright.Route>();
            tab.route("**/*", route -> {
                if ("true".equalsIgnoreCase(route.request().headers().get("wicket-ajax"))) pending.set(route); else route.resume();
            });
            tab.locator("#busy-action").click(); tab.waitForCondition(() -> pending.get() != null);
            assertThat(tab.locator("#busy-action .sw-busy-label")).hasText(loading);
            pending.get().resume(); assertThat(tab.locator("#busy-action")).not().hasAttribute("aria-busy", "true");
        });
    }

    @ParameterizedTest(name = "library controls in {0}")
    @CsvSource({"de,Suchen…,Abbrechen", "es,Buscar…,Cancelar", "fr,Rechercher…,Annuler", "it,Cerca…,Annulla",
            "ja,検索…,キャンセル", "nl,Zoeken…,Annuleren", "pt,Pesquisar…,Cancelar", "hr,Pretraži…,Odustani"})
    void translatedControlsSurviveAjaxAndRemainAccessible(String language, String placeholder, String cancel) {
        check("internationalization", "vega", "light", tab -> {
            tab.navigate(url("internationalization", "vega", "light") + "&lang=" + language);
            assertThat(tab.locator("html")).hasAttribute("lang", language);
            Locator city = tab.locator(".sw-combobox").first();
            Locator input = city.locator(".sw-combobox-input");
            assertThat(input).hasAttribute("placeholder", placeholder);
            input.fill("zzzzzz");
            assertThat(city.locator(".sw-combobox-empty")).hasText(city.getAttribute("data-empty"));
            input.press("Escape");
            tab.getByRole(AriaRole.BUTTON, new Page.GetByRoleOptions().setName("Refresh fields with Ajax")).click();
            assertThat(tab.locator(".sw-combobox-input")).hasCount(2);
            assertThat(input).hasAttribute("placeholder", placeholder);
            tab.getByRole(AriaRole.BUTTON, new Page.GetByRoleOptions().setName("Open dialog")).click();
            Locator dialog = tab.getByRole(AriaRole.DIALOG, new Page.GetByRoleOptions().setName("A localized dialog"));
            assertThat(dialog).isVisible();
            assertThat(describe(new AxeBuilder(tab).withTags(WCAG).analyze().getViolations())).as("localized dialog").isEmpty();
            dialog.getByRole(AriaRole.BUTTON, new Locator.GetByRoleOptions().setName(cancel).setExact(true)).click();
            assertThat(dialog).isHidden();
        });
    }

    @ParameterizedTest(name = "Japanese mobile, {0}, {1}")
    @MethodSource("everySkinAndMode")
    void japaneseUploadsToastsAndPaginationWorkOnNarrowScreens(String skin, String mode) {
        check("internationalization", skin, mode, new Browser.NewContextOptions().setViewportSize(390, 850), tab -> {
            tab.navigate(url("internationalization", skin, mode) + "&lang=ja");
            Locator upload = tab.locator(".sw-file-upload");
            upload.locator("input[type=file]").setInputFiles(new FilePayload("旅行.txt", "text/plain", "Trip notes".getBytes(java.nio.charset.StandardCharsets.UTF_8)));
            assertThat(upload.getByRole(AriaRole.BUTTON, new Locator.GetByRoleOptions().setName("旅行.txt を削除"))).isVisible();
            upload.getByRole(AriaRole.BUTTON, new Locator.GetByRoleOptions().setName("旅行.txt を削除")).click();
            tab.getByRole(AriaRole.BUTTON, new Page.GetByRoleOptions().setName("Show notification").setExact(true)).click();
            assertThat(tab.locator("#sw-toaster")).hasAttribute("aria-label", "通知 (Alt+T)");
            tab.getByRole(AriaRole.BUTTON, new Page.GetByRoleOptions().setName("通知を閉じる")).click();
            tab.getByRole(AriaRole.BUTTON, new Page.GetByRoleOptions().setName("Show feedback toast")).click();
            assertThat(tab.locator("#sw-toaster")).hasAttribute("aria-label", "通知 (Alt+T)");
            tab.getByRole(AriaRole.BUTTON, new Page.GetByRoleOptions().setName("通知を閉じる")).click();
            assertThat(tab.locator(".sw-pagination-summary")).hasText("12 件中 1～3 件");
            tab.getByRole(AriaRole.LINK, new Page.GetByRoleOptions().setName("次のページへ")).click();
            assertThat(tab.locator(".sw-pagination-summary")).hasText("12 件中 4～6 件");
            assertThat((Integer) tab.evaluate("document.documentElement.scrollWidth - innerWidth")).isLessThanOrEqualTo(1);
        });
    }

    @ParameterizedTest(name = "Croatian mobile, {0}, {1}")
    @MethodSource("everySkinAndMode")
    void croatianControlsUseRegionalFallbackAndKeepDiacritics(String skin, String mode) {
        check("internationalization", skin, mode, new Browser.NewContextOptions().setViewportSize(390, 850), tab -> {
            tab.navigate(url("internationalization", skin, mode) + "&lang=hr-HR");
            assertThat(tab.locator("html")).hasAttribute("lang", "hr-HR");
            assertThat(tab.locator("#showcase-language option:checked")).hasText("hrvatski");
            assertThat(tab.locator(".sw-combobox-input").first()).hasAttribute("placeholder", "Pretraži…");
            Locator upload = tab.locator(".sw-file-upload");
            upload.locator("input[type=file]").setInputFiles(new FilePayload("priča.txt", "text/plain", new byte[]{65}));
            assertThat(upload.getByRole(AriaRole.BUTTON, new Locator.GetByRoleOptions().setName("Ukloni priča.txt"))).isVisible();
            assertThat(upload.locator(".sw-file-upload-status")).hasText("Broj odabranih datoteka: 1.");
            upload.getByRole(AriaRole.BUTTON, new Locator.GetByRoleOptions().setName("Ukloni priča.txt")).click();
            for (String trigger : List.of("Show notification", "Show feedback toast")) {
                tab.getByRole(AriaRole.BUTTON, new Page.GetByRoleOptions().setName(trigger).setExact(true)).click();
                assertThat(tab.locator("#sw-toaster")).hasAttribute("aria-label", "Obavijesti (Alt+T)");
                tab.getByRole(AriaRole.BUTTON, new Page.GetByRoleOptions().setName("Zatvori obavijest")).click();
            }
            assertThat(tab.locator(".sw-pagination-summary")).hasText("1–3 od 12");
            tab.getByRole(AriaRole.LINK, new Page.GetByRoleOptions().setName("Sljedeća stranica")).click();
            assertThat(tab.locator(".sw-pagination-summary")).hasText("4–6 od 12");
            assertThat((Integer) tab.evaluate("document.documentElement.scrollWidth - innerWidth")).isLessThanOrEqualTo(1);
        });
    }

    @ParameterizedTest(name = "native language picker, {0}, {1}, {2}")
    @MethodSource("nativeLanguagesInEverySkinAndMode")
    void languageSelectionAndComboboxFallbackWorkWithoutScript(String skin, String mode, String language) {
        check("internationalization", skin, mode, new Browser.NewContextOptions().setJavaScriptEnabled(false).setViewportSize(390, 850), tab -> {
            tab.navigate(url("internationalization", skin, mode) + "&lang=de-AT");
            assertThat(tab.locator("html")).hasAttribute("lang", "de-AT");
            Locator select = tab.locator(".sw-combobox select").first();
            assertThat(select.locator("option").first()).hasText("Suchen…");
            select.selectOption(new com.microsoft.playwright.options.SelectOption().setLabel("Berlin"));
            assertThat(select.locator("option:checked")).hasText("Berlin");
            tab.locator("#showcase-language").selectOption(language);
            tab.getByRole(AriaRole.BUTTON, new Page.GetByRoleOptions().setName("Anwenden")).click();
            assertThat(tab.locator("html")).hasAttribute("lang", language);
            assertThat(tab.locator(".sw-combobox select").first().locator("option").first())
                    .hasText("hr".equals(language) ? "Pretraži…" : "検索…");
            assertThat(tab.locator("link[href*='shadwick-" + skin + "']")).hasCount(1);
            assertThat(tab.locator("html")).hasClass("dark".equals(mode) ? "dark" : "");
        });
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
            tab.waitForFunction("n => getComputedStyle(n).backgroundPositionX.split(',').every(position => position.trim() === '0%')", small.elementHandle());
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
            assertThat(group.locator("label").first()).hasCSS("border-right-width", "sketch".equals(skin) ? "2px" : "1px");
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
            // Native popup placement can move a readable tooltip under a stationary pointer.
            // This assertion tests focus dismissal; leave hover before checking it.
            tab.mouse().move(0, 0);
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
            assertThat(tab.locator(".sw-toast").last()).hasCSS("border-top-width", "sketch".equals(skin) ? "2px" : "1px");
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

    @ParameterizedTest(name = "{0}, {1}")
    @MethodSource("everySkinAndMode")
    void userMenuHeaderKeyboardAndNativeSignOutSignInWork(String skin, String mode) {
        check("user-menu", skin, mode, tab -> {
            List<String> posts = new ArrayList<>();
            tab.onRequest(request -> { if ("POST".equals(request.method())) posts.add(request.url()); });
            Locator header = tab.locator("#header-user"), trigger = header.locator(".sw-user-menu-trigger"), menu = header.locator("[role=menu]");
            assertThat(trigger).hasAccessibleName("Account: Alex Morgan");
            trigger.focus(); tab.keyboard().press("ArrowDown");
            assertThat(menu.getByRole(AriaRole.MENUITEM).first()).isFocused();
            assertThat(describe(new AxeBuilder(tab).withTags(WCAG).analyze().getViolations())).isEmpty();
            tab.keyboard().press("s"); assertThat(menu.getByRole(AriaRole.MENUITEM, new Locator.GetByRoleOptions().setName("Settings").setExact(true))).isFocused();
            tab.keyboard().press("Escape"); assertThat(trigger).isFocused();
            tab.keyboard().press("ArrowUp");
            assertThat(menu.getByRole(AriaRole.MENUITEM, new Locator.GetByRoleOptions().setName("Sign out").setExact(true))).isFocused();
            tab.keyboard().press("Enter");
            assertThat(tab.locator("[role=status]")).hasText("Signed out of the demo workspace.");
            assertThat(posts).hasSize(1);
            assertThat(tab.getByRole(AriaRole.LINK, new Page.GetByRoleOptions().setName("Sign in").setExact(true))).hasCount(2);
            assertThat(describe(new AxeBuilder(tab).withTags(WCAG).analyze().getViolations())).isEmpty();
            tab.getByRole(AriaRole.LINK, new Page.GetByRoleOptions().setName("Sign in").setExact(true)).first().click();
            assertThat(tab.getByRole(AriaRole.HEADING, new Page.GetByRoleOptions().setName("Demo sign in").setExact(true))).isVisible();
            assertThat(tab.getByRole(AriaRole.LINK, new Page.GetByRoleOptions().setName("Sign in").setExact(true))).hasCount(0);
            tab.getByRole(AriaRole.LINK, new Page.GetByRoleOptions().setName("Enter demo workspace").setExact(true)).click();
            assertThat(tab.locator("#header-user .sw-user-menu-trigger")).isVisible();
        });
    }

    @ParameterizedTest(name = "{0}, {1}")
    @MethodSource("everySkinAndMode")
    void userMenuSidebarHintAndMenuNeverShowTogether(String skin, String mode) {
        check("user-menu", skin, mode, tab -> {
            tab.locator(".sw-sidebar-trigger").click();
            Locator root = tab.locator("#sidebar-user"), trigger = root.locator(".sw-user-menu-trigger"), menu = root.locator("[role=menu]");
            trigger.focus(); tab.keyboard().press("Tab"); tab.keyboard().press("Shift+Tab");
            assertThat(tab.locator(".sw-sidebar-menu-tooltip:popover-open")).hasCount(1);
            assertThat(trigger).hasAccessibleName("Alex Morgan alex@example.com");
            assertThat(trigger).hasCSS("width", "32px");
            trigger.click(); assertThat(menu).isVisible();
            assertThat(tab.locator(".sw-sidebar-menu-tooltip:popover-open")).hasCount(0);
            tab.mouse().move(trigger.boundingBox().x + 4, trigger.boundingBox().y + 4);
            tab.waitForTimeout(400);
            assertThat(tab.locator(".sw-sidebar-menu-tooltip:popover-open")).hasCount(0);
            assertThat(menu.boundingBox().width).isGreaterThanOrEqualTo(224);
            tab.screenshot(new Page.ScreenshotOptions().setPath(Path.of("/tmp/shadwick-user-sidebar-" + skin + "-" + mode + ".png")));
            tab.keyboard().press("Escape"); assertThat(trigger).isFocused();
            tab.keyboard().press("Escape");
            tab.locator(".sw-sidebar-trigger").click();
            tab.locator("#header-user .sw-user-menu-trigger").click();
            Locator headerMenu = tab.locator("#header-user [role=menu]");
            headerMenu.getByRole(AriaRole.MENUITEM, new Locator.GetByRoleOptions().setName("My account").setExact(true)).click();
            assertThat(tab.locator("[role=status]")).hasText("Viewing account.");
        });
    }

    @ParameterizedTest(name = "{0}, {1}")
    @MethodSource("everySkinAndMode")
    void userMenuMobileStaysInsideThePanelAndEscapeClosesOneLayer(String skin, String mode) {
        check("user-menu", skin, mode, new Browser.NewContextOptions().setViewportSize(390, 800), tab -> {
            tab.locator(".sw-sidebar-trigger").click();
            Locator nav = tab.locator(".sw-sidebar"), root = tab.locator("#sidebar-user"), trigger = root.locator(".sw-user-menu-trigger"), menu = root.locator("[role=menu]");
            trigger.click(); assertThat(menu).isVisible();
            assertThat(menu.boundingBox().y + menu.boundingBox().height).isLessThanOrEqualTo(trigger.boundingBox().y + 1);
            assertThat((Boolean) menu.evaluate("n => !!n.closest('[inert]')")).isFalse();
            assertThat(describe(new AxeBuilder(tab).withTags(WCAG).analyze().getViolations())).isEmpty();
            tab.screenshot(new Page.ScreenshotOptions().setPath(Path.of("/tmp/shadwick-user-mobile-" + skin + "-" + mode + ".png")));
            tab.keyboard().press("Escape"); assertThat(menu).isHidden(); assertThat(trigger).isFocused(); assertThat(nav).isVisible();
            for (int attempt = 0; attempt < 3; attempt++) {
                trigger.click(); tab.keyboard().press("Escape"); assertThat(menu).isHidden(); assertThat(nav).isVisible();
            }
            trigger.focus(); tab.keyboard().press("ArrowDown");
            assertThat(menu.getByRole(AriaRole.MENUITEM).first()).isFocused();
            tab.keyboard().press("Tab"); assertThat(menu).isHidden();
            tab.keyboard().press("Escape"); assertThat(nav).isHidden();
            assertThat(tab.locator(".sw-sidebar-trigger")).isFocused();
        });
    }

    @ParameterizedTest(name = "{0}, {1}")
    @MethodSource("everySkinAndMode")
    void userMenuModelsRefreshPicturesInitialsAndAnonymousStateOverAjax(String skin, String mode) {
        check("user-menu", skin, mode, tab -> {
            Locator header = tab.locator("#header-user"), trigger = header.locator(".sw-user-menu-trigger");
            trigger.click();
            tab.getByRole(AriaRole.BUTTON, new Page.GetByRoleOptions().setName("Change account name").setExact(true)).click();
            assertThat(trigger).hasAccessibleName("Account: Alexandra Morgan from the Design Systems and Platform Engineering team");
            assertThat(header.locator("[role=menu]")).isHidden();
            tab.getByRole(AriaRole.BUTTON, new Page.GetByRoleOptions().setName("Use account picture").setExact(true)).click();
            assertThat(trigger.locator(".sw-avatar-image")).hasAttribute("data-status", "loaded");
            assertThat(trigger.locator(".sw-avatar-fallback")).isHidden();
            tab.getByRole(AriaRole.BUTTON, new Page.GetByRoleOptions().setName("Use initials").setExact(true)).click();
            assertThat(trigger.locator(".sw-avatar-image")).hasCount(0); assertThat(trigger.locator(".sw-avatar-fallback")).isVisible();
            tab.getByRole(AriaRole.BUTTON, new Page.GetByRoleOptions().setName("Show anonymous visitor").setExact(true)).click();
            assertThat(tab.getByRole(AriaRole.LINK, new Page.GetByRoleOptions().setName("Sign in").setExact(true))).hasCount(2);
            assertThat(describe(new AxeBuilder(tab).withTags(WCAG).analyze().getViolations())).isEmpty();
            tab.getByRole(AriaRole.BUTTON, new Page.GetByRoleOptions().setName("Restore demo account").setExact(true)).click();
            assertThat(trigger).hasAccessibleName("Account: Alex Morgan");
        });
    }

    @ParameterizedTest(name = "{0}, {1}")
    @MethodSource("everySkinAndMode")
    void userMenuNativeMobileSignOutWorksWithoutJavaScript(String skin, String mode) {
        check("user-menu", skin, mode, new Browser.NewContextOptions().setJavaScriptEnabled(false).setViewportSize(390, 800), tab -> {
            tab.locator(".sw-sidebar-trigger").click();
            Locator root = tab.locator("#sidebar-user"); root.locator(".sw-user-menu-trigger").click();
            root.getByRole(AriaRole.MENUITEM, new Locator.GetByRoleOptions().setName("Sign out").setExact(true)).click();
            assertThat(tab.locator("[role=status]")).hasText("Signed out of the demo workspace.");
            assertThat(tab.locator("#header-user .sw-user-menu-sign-in")).isVisible();
        });
    }

    @ParameterizedTest(name = "{0}, {1}")
    @MethodSource("everySkinAndMode")
    void userMenuBrokenPicturesLongNamesRtlAndForcedColorsRemainUsable(String skin, String mode) {
        check("user-menu", skin, mode, tab -> {
            tab.route("**/clover*.svg*", route -> route.fulfill(new com.microsoft.playwright.Route.FulfillOptions()
                    .setStatus(200).setContentType("image/png").setBody("An undecodable image")));
            tab.getByRole(AriaRole.BUTTON, new Page.GetByRoleOptions().setName("Use account picture").setExact(true)).click();
            Locator headerTrigger = tab.locator("#header-user .sw-user-menu-trigger");
            assertThat(headerTrigger.locator(".sw-avatar-image")).hasAttribute("data-status", "error");
            assertThat(headerTrigger.locator(".sw-avatar-fallback")).isVisible();
            tab.getByRole(AriaRole.BUTTON, new Page.GetByRoleOptions().setName("Change account name").setExact(true)).click();
            tab.setViewportSize(320, 700); tab.locator("html").evaluate("n => n.dir='rtl'");
            tab.emulateMedia(new Page.EmulateMediaOptions().setForcedColors(ForcedColors.ACTIVE)
                    .setReducedMotion(com.microsoft.playwright.options.ReducedMotion.REDUCE));
            tab.locator(".sw-sidebar-trigger").click();
            Locator trigger = tab.locator("#sidebar-user .sw-user-menu-trigger"), menu = tab.locator("#sidebar-user [role=menu]");
            trigger.focus(); tab.keyboard().press("ArrowDown");
            assertThat(menu.getByRole(AriaRole.MENUITEM).first()).isFocused();
            assertThat(menu.getByRole(AriaRole.MENUITEM).first()).hasCSS("outline-style", "solid");
            assertThat(menu.boundingBox().x).isGreaterThanOrEqualTo(-1);
            assertThat(menu.boundingBox().x + menu.boundingBox().width).isLessThanOrEqualTo(321);
            tab.keyboard().press("Escape"); tab.keyboard().press("Escape");
            assertThat((Boolean) tab.evaluate("() => document.documentElement.scrollWidth <= innerWidth")).isTrue();
            tab.emulateMedia(new Page.EmulateMediaOptions().setForcedColors(ForcedColors.NONE));
        });
    }

    private void chooseTheme(Page tab, String root, String mode) {
        tab.locator("#" + root + " [popovertarget]").click();
        tab.locator("#" + root + " [data-sw-theme-choice=" + mode + "]").click();
        assertThat(tab.locator("#" + root + " [popover]")).isHidden();
    }

    @ParameterizedTest(name = "{0}, {1}")
    @MethodSource("everySkinAndMode")
    void themeControlsChooseWithTheKeyboardAndStayInSync(String skin, String mode) {
        check("theme", skin, mode, tab -> {
            Locator trigger = tab.locator("#theme-primary [popovertarget]"), button = tab.locator("#theme-button button");
            trigger.focus(); tab.keyboard().press("ArrowDown");
            assertThat(tab.locator("#theme-primary [data-sw-theme-choice=light]")).isFocused();
            assertThat(describe(new AxeBuilder(tab).withTags(WCAG).analyze().getViolations())).isEmpty();
            tab.keyboard().press("ArrowDown"); tab.keyboard().press("Enter");
            assertThat(tab.locator("html")).hasAttribute("data-sw-theme-mode", "dark");
            assertThat(button).hasAttribute("aria-pressed", "true");
            assertThat(trigger).isFocused();
            assertThat(tab.locator("[data-sw-theme-choice=dark][aria-checked=true]")).hasCount(2);
            assertThat(tab.evaluate("() => localStorage.getItem('shadwick-example-theme')")).isEqualTo("dark");
            button.focus(); tab.keyboard().press("Space");
            assertThat(button).hasAttribute("aria-pressed", "false");
            assertThat(tab.locator("[data-sw-theme-choice=light][aria-checked=true]")).hasCount(2);
            chooseTheme(tab, "theme-secondary", "system");
            assertThat(tab.evaluate("() => localStorage.getItem('shadwick-example-theme')")).isEqualTo("system");
            assertThat(tab.locator("[data-sw-theme-choice=system][aria-checked=true]")).hasCount(2);
        });
    }

    @ParameterizedTest(name = "{0}, {1}")
    @MethodSource("everySkinAndMode")
    void themeSystemChangesCrossTabsAndPrintingKeepThePreference(String skin, String mode) {
        check("theme", skin, mode, tab -> {
            chooseTheme(tab, "theme-primary", "system");
            tab.emulateMedia(new Page.EmulateMediaOptions().setColorScheme(com.microsoft.playwright.options.ColorScheme.LIGHT));
            assertThat(tab.locator("html")).hasAttribute("data-sw-theme-resolved", "light");
            tab.emulateMedia(new Page.EmulateMediaOptions().setColorScheme(com.microsoft.playwright.options.ColorScheme.DARK));
            assertThat(tab.locator("html")).hasAttribute("data-sw-theme-resolved", "dark");
            chooseTheme(tab, "theme-primary", "light");
            tab.emulateMedia(new Page.EmulateMediaOptions().setColorScheme(com.microsoft.playwright.options.ColorScheme.LIGHT));
            tab.emulateMedia(new Page.EmulateMediaOptions().setColorScheme(com.microsoft.playwright.options.ColorScheme.DARK));
            assertThat(tab.locator("html")).hasAttribute("data-sw-theme-resolved", "light");
            try (Page other = tab.context().newPage()) {
                other.navigate(url("theme", skin, mode));
                assertThat(other.locator("html")).hasAttribute("data-sw-theme-mode", "light");
                chooseTheme(other, "theme-primary", "dark");
                assertThat(tab.locator("html")).hasAttribute("data-sw-theme-mode", "dark");
                assertThat(tab.locator("#theme-button button")).hasAttribute("aria-pressed", "true");
                tab.evaluate("() => window.dispatchEvent(new Event('beforeprint'))");
                assertThat(tab.locator("html")).hasAttribute("data-sw-theme-resolved", "light");
                assertThat(tab.locator("html")).hasAttribute("data-sw-theme-mode", "dark");
                tab.evaluate("() => window.dispatchEvent(new Event('afterprint'))");
                assertThat(tab.locator("html")).hasAttribute("data-sw-theme-resolved", "dark");
                other.evaluate("() => localStorage.clear()");
                assertThat(tab.locator("html")).hasAttribute("data-sw-theme-mode", mode);
            }
        });
    }

    @ParameterizedTest(name = "{0}, {1}")
    @MethodSource("everySkinAndMode")
    void themeAppliesBeforeBodyParsingAndSurvivesAjaxNavigationAndHistory(String skin, String mode) {
        check("theme", skin, mode, tab -> {
            chooseTheme(tab, "theme-primary", "dark");
            tab.addInitScript("new MutationObserver((records, observer) => { if(document.body) { window.themeAtBody=document.documentElement.classList.contains('dark'); observer.disconnect(); } }).observe(document,{childList:true,subtree:true}); requestAnimationFrame(function capture(){if(!document.body){requestAnimationFrame(capture);return;}window.themeAtFrame=document.documentElement.classList.contains('dark');});");
            tab.reload();
            tab.waitForFunction("() => window.themeAtFrame !== undefined");
            assertThat(tab.evaluate("() => window.themeAtBody")).isEqualTo(true);
            assertThat(tab.evaluate("() => window.themeAtFrame")).isEqualTo(true);
            tab.locator("#theme-primary [popovertarget]").click();
            tab.getByRole(AriaRole.BUTTON, new Page.GetByRoleOptions().setName("Refresh theme controls").setExact(true)).click();
            assertThat(tab.locator(".sw-theme-toggle[data-sw-theme-ready]")).hasCount(3);
            assertThat(tab.locator("[data-sw-theme-choice=dark][aria-checked=true]")).hasCount(2);
            assertThat(tab.locator("#theme-primary [popover]")).isHidden();
            tab.getByRole(AriaRole.LINK, new Page.GetByRoleOptions().setName("Open the other view").setExact(true)).click();
            assertThat(tab.locator("html")).hasAttribute("data-sw-theme-mode", "dark");
            chooseTheme(tab, "theme-primary", "light"); tab.goBack();
            assertThat(tab.locator("html")).hasAttribute("data-sw-theme-mode", "light");
            tab.evaluate("() => { localStorage.setItem('shadwick-example-theme','system'); window.dispatchEvent(new PageTransitionEvent('pageshow',{persisted:true})); }");
            assertThat(tab.locator("html")).hasAttribute("data-sw-theme-mode", "system");
        });
    }

    @ParameterizedTest(name = "{0}, {1}")
    @MethodSource("everySkinAndMode")
    void themeStorageFailuresKeepAnInPageChoiceAndInvalidValuesUseTheDefault(String skin, String mode) {
        check("theme", skin, mode, tab -> {
            tab.evaluate("() => localStorage.setItem('shadwick-example-theme','not-a-mode')");
            tab.reload(); assertThat(tab.locator("html")).hasAttribute("data-sw-theme-mode", mode);
            tab.addInitScript("Object.defineProperty(window,'localStorage',{get(){throw new DOMException('Storage unavailable','SecurityError');}});");
            tab.reload();
            chooseTheme(tab, "theme-primary", "dark");
            assertThat(tab.locator("#theme-button button")).hasAttribute("aria-pressed", "true");
            tab.emulateMedia(new Page.EmulateMediaOptions().setColorScheme(com.microsoft.playwright.options.ColorScheme.LIGHT));
            assertThat(tab.locator("html")).hasAttribute("data-sw-theme-resolved", "dark");
            chooseTheme(tab, "theme-primary", "system");
            assertThat(tab.locator("html")).hasAttribute("data-sw-theme-resolved", "light");
            tab.emulateMedia(new Page.EmulateMediaOptions().setColorScheme(com.microsoft.playwright.options.ColorScheme.DARK));
            assertThat(tab.locator("html")).hasAttribute("data-sw-theme-resolved", "dark");
        });
    }

    @ParameterizedTest(name = "{0}, {1}")
    @MethodSource("everySkinAndMode")
    void themeWithoutJavaScriptHidesControlsAndRetainsNativeNavigation(String skin, String mode) {
        check("theme", skin, mode, new Browser.NewContextOptions().setJavaScriptEnabled(false).setViewportSize(390, 800), tab -> {
            assertThat(tab.locator("#theme-primary [popovertarget]")).isHidden();
            assertThat(tab.locator("#theme-button button")).isHidden();
            tab.getByRole(AriaRole.LINK, new Page.GetByRoleOptions().setName("Open the other view").setExact(true)).click();
            assertThat(tab.locator("#theme-primary [popovertarget]")).isHidden();
            assertThat(tab.locator("html")).hasClass(mode.equals("dark") ? "dark" : "");
        });
    }

    @ParameterizedTest(name = "{0}, {1}")
    @MethodSource("everySkinAndMode")
    void themeForcedColorsRtlAndSmallScreensKeepTheMenuUsable(String skin, String mode) {
        check("theme", skin, mode, new Browser.NewContextOptions().setViewportSize(320, 700), tab -> {
            tab.locator("html").evaluate("n => n.dir='rtl'");
            tab.emulateMedia(new Page.EmulateMediaOptions().setForcedColors(ForcedColors.ACTIVE)
                    .setReducedMotion(com.microsoft.playwright.options.ReducedMotion.REDUCE));
            Locator trigger = tab.locator("#theme-primary [popovertarget]");
            trigger.focus(); tab.keyboard().press("ArrowDown");
            Locator light = tab.locator("#theme-primary [data-sw-theme-choice=light]");
            assertThat(light).isFocused(); assertThat(light).hasCSS("outline-style", "solid");
            tab.keyboard().press("ArrowDown"); tab.keyboard().press("Enter");
            assertThat(tab.locator("html")).hasAttribute("data-sw-theme-mode", "dark");
            assertThat(trigger.locator(".sw-theme-toggle-dark")).isVisible();
            assertThat(trigger.locator(".sw-theme-toggle-light")).isHidden();
            trigger.click();
            Locator chosen = tab.locator("#theme-primary [data-sw-theme-choice=dark]");
            assertThat(chosen).hasAttribute("aria-checked", "true");
            assertThat((String) chosen.evaluate("n => getComputedStyle(n,'::after').display")).isNotEqualTo("none");
            tab.screenshot(new Page.ScreenshotOptions().setPath(Path.of("/tmp/shadwick-theme-" + skin + "-" + mode + ".png")));
            tab.keyboard().press("Escape");
            assertThat((Boolean) tab.evaluate("() => document.documentElement.scrollWidth <= innerWidth")).isTrue();
            tab.emulateMedia(new Page.EmulateMediaOptions().setForcedColors(ForcedColors.NONE));
        });
    }

    @ParameterizedTest(name = "{0}, {1}")
    @MethodSource("everySkinAndMode")
    void layoutSubmenusNavigateAndOpenTheirCurrentSection(String skin, String mode) {
        check("layouts", skin, mode, tab -> {
            Locator nav = tab.locator("#layout-navigation");
            Locator projects = nav.locator("summary.sw-sidebar-menu-button");
            projects.focus(); tab.keyboard().press("Enter");
            assertThat(nav.getByRole(AriaRole.LINK, new Locator.GetByRoleOptions().setName("Recent").setExact(true))).isVisible();
            nav.getByRole(AriaRole.LINK, new Locator.GetByRoleOptions().setName("Recent").setExact(true)).click();
            assertThat(tab.getByRole(AriaRole.HEADING, new Page.GetByRoleOptions().setLevel(1))).hasText("Pick up where you left off.");
            assertThat(nav.locator("[aria-current=page]")).hasCount(1);
            assertThat(nav.locator("[aria-current=page]")).containsText("Recent");
            nav.getByRole(AriaRole.LINK, new Locator.GetByRoleOptions().setName("Settings").setExact(true)).click();
            Locator toggle = nav.locator("summary[aria-label='Settings pages']");
            assertThat(toggle.locator("..")).hasAttribute("open", "open");
            nav.getByRole(AriaRole.LINK, new Locator.GetByRoleOptions().setName("Settings").setExact(true)).focus();
            tab.keyboard().press("Tab"); assertThat(toggle).isFocused();
            tab.keyboard().press("Space");
            assertThat(nav.getByRole(AriaRole.LINK, new Locator.GetByRoleOptions().setName("Billing").setExact(true))).isHidden();
            tab.keyboard().press("Enter");
            nav.getByRole(AriaRole.LINK, new Locator.GetByRoleOptions().setName("Billing").setExact(true)).click();
            assertThat(nav.locator("[aria-current=page]")).containsText("Billing");
            assertThat(tab.getByRole(AriaRole.HEADING, new Page.GetByRoleOptions().setLevel(1))).hasText("The practical details.");
        });
    }

    @ParameterizedTest(name = "{0}, {1}")
    @MethodSource("everySkinAndMode")
    void layoutIconRailExpandsOnSubmenuActivation(String skin, String mode) {
        check("layouts", skin, mode, tab -> {
            tab.locator("#layout-trigger").click();
            assertThat(tab.locator("#layout-provider")).hasAttribute("data-state", "collapsed");
            Locator summary = tab.locator("#layout-navigation summary.sw-sidebar-menu-button");
            assertThat(tab.locator("#layout-navigation .sw-sidebar-menu-sub").first()).isHidden();
            summary.focus(); tab.keyboard().press("Enter");
            assertThat(tab.locator("#layout-provider")).hasAttribute("data-state", "expanded");
            assertThat(summary).isFocused();
            assertThat(tab.locator("#layout-navigation").getByRole(AriaRole.LINK,
                    new Locator.GetByRoleOptions().setName("Recent").setExact(true))).isVisible();
            assertThat(tab.evaluate("() => document.cookie")).asString().contains("shadwick-layout-sidebar=expanded");
            tab.getByRole(AriaRole.SEARCHBOX).focus(); tab.keyboard().press("Control+b");
            assertThat(tab.locator("#layout-provider")).hasAttribute("data-state", "expanded");
        });
    }

    @ParameterizedTest(name = "{0}, {1}")
    @MethodSource("everySkinAndMode")
    void layoutPanelActionsRefreshBadgesAndKeepThePanelOpen(String skin, String mode) {
        check("layouts", skin, mode, new Browser.NewContextOptions().setViewportSize(390, 850), tab -> {
            Locator nav = tab.locator("#layout-navigation"); tab.locator("#layout-trigger").click();
            assertThat(nav.locator("[aria-current=page]")).isFocused();
            nav.getByRole(AriaRole.BUTTON, new Locator.GetByRoleOptions().setName("Mark Inbox as read").setExact(true)).click();
            assertThat(nav.locator(".sw-sidebar-menu-badge")).hasCount(0);
            assertThat(nav).isVisible(); assertThat((Boolean) tab.locator("main").evaluate("n => !!n.closest('[inert]')")).isTrue();
            nav.getByRole(AriaRole.BUTTON, new Locator.GetByRoleOptions().setName("Add workspace update").setExact(true)).click();
            assertThat(nav.locator(".sw-sidebar-menu-badge")).hasText("1");
            assertThat(nav.locator(".sw-toaster")).hasCount(1);
            assertThat(describe(new AxeBuilder(tab).withTags(WCAG).analyze().getViolations())).isEmpty();
            tab.keyboard().press("Escape"); assertThat(nav).isHidden();
            assertThat(tab.locator("#layout-trigger")).isFocused();
            assertThat(tab.locator("[inert]")).hasCount(0);
            tab.getByRole(AriaRole.BUTTON, new Page.GetByRoleOptions().setName("Refresh navigation").setExact(true)).click();
            assertThat(tab.getByRole(AriaRole.STATUS).filter(new Locator.FilterOptions().setHasText("Navigation refreshed"))).hasCount(1);
        });
    }

    @ParameterizedTest(name = "{0}, {1}")
    @MethodSource("everySkinAndMode")
    void layoutHeaderRowIgnoresCollapseAndScrollsWithoutWideningThePage(String skin, String mode) {
        check("header-layout", skin, mode, new Browser.NewContextOptions().setViewportSize(900, 850), tab -> {
            tab.evaluate("() => document.cookie='shadwick-layout-sidebar=collapsed;path=/'"); tab.reload();
            Locator nav = tab.locator("#layout-navigation"), trigger = tab.locator("#layout-trigger");
            assertThat(nav).isVisible(); assertThat(trigger).isHidden();
            assertThat(trigger).not().hasAttribute("popovertarget", java.util.regex.Pattern.compile(".*")); assertThat(trigger).not().hasAttribute("aria-expanded", java.util.regex.Pattern.compile(".*"));
            assertThat(nav.locator(".sw-sidebar-header")).isHidden();
            assertThat(nav.getByRole(AriaRole.LINK, new Locator.GetByRoleOptions().setName("Component showcase").setExact(true))).isHidden();
            assertThat(nav.locator("[aria-current=page]")).hasCount(1);
            assertThat(nav.locator(".sw-sidebar-menu-sub").first()).isHidden();
            tab.locator("main").focus(); tab.keyboard().press("Control+b");
            assertThat(tab.locator("#layout-provider")).hasAttribute("data-state", "collapsed");
            nav.evaluate("n => { const list=n.querySelector('.sw-sidebar-group-collapsible .sw-sidebar-menu'); for(let i=0;i<8;i++){const li=document.createElement('li');li.className='sw-sidebar-menu-item';const a=document.createElement('a');a.className='sw-sidebar-menu-button';a.href='#sw-main';a.textContent='Quarterly report '+i;li.append(a);list.append(li);} }");
            assertThat((Boolean) nav.evaluate("n => n.scrollWidth > n.clientWidth")).isTrue();
            nav.getByRole(AriaRole.LINK, new Locator.GetByRoleOptions().setName("Quarterly report 7").setExact(true)).focus();
            tab.waitForFunction("() => document.querySelector('#layout-navigation').scrollLeft > 0");
            assertThat((Boolean) tab.evaluate("() => document.documentElement.scrollWidth <= innerWidth")).isTrue();
            assertThat(tab.evaluate("() => document.cookie")).asString().contains("shadwick-layout-sidebar=collapsed");
        });
    }

    @ParameterizedTest(name = "{0}, {1}")
    @MethodSource("everySkinAndMode")
    void layoutHeaderPanelReleasesInertnessAcrossResizeAjaxAndNavigation(String skin, String mode) {
        check("header-layout", skin, mode, new Browser.NewContextOptions().setViewportSize(390, 850), tab -> {
            Locator nav = tab.locator("#layout-navigation"), trigger = tab.locator("#layout-trigger");
            trigger.click(); assertThat(nav.locator("[aria-current=page]")).isFocused();
            nav.locator("summary.sw-sidebar-group-label").filter(new Locator.FilterOptions().setHasText("Explore")).click();
            assertThat(nav.getByRole(AriaRole.LINK, new Locator.GetByRoleOptions().setName("Component showcase").setExact(true))).isVisible();
            tab.keyboard().press("Shift+Tab");
            assertThat((Boolean) tab.evaluate("() => document.querySelector('#layout-navigation').contains(document.activeElement)")).isTrue();
            tab.setViewportSize(1200, 850);
            assertThat(tab.locator("[inert]")).hasCount(0); assertThat(nav).isVisible(); assertThat(trigger).isHidden();
            assertThat(trigger).not().hasAttribute("aria-expanded", java.util.regex.Pattern.compile(".*"));
            tab.getByRole(AriaRole.BUTTON, new Page.GetByRoleOptions().setName("Replace navigation").setExact(true)).click();
            assertThat(nav).hasAttribute("data-placement", "header");
            tab.setViewportSize(390, 850); assertThat(nav).isHidden(); trigger.click();
            nav.getByRole(AriaRole.LINK, new Locator.GetByRoleOptions().setName("Inbox").setExact(true)).click();
            assertThat(nav).isHidden(); assertThat(tab.locator("[inert]")).hasCount(0);
            trigger.click(); assertThat(nav.locator("[aria-current=page]")).containsText("Inbox");
            assertThat(nav.locator("[aria-current=page]")).isFocused(); tab.keyboard().press("Escape");
        });
    }

    @ParameterizedTest(name = "{0}, {1}")
    @MethodSource("everySkinAndMode")
    void layoutNativeNavigationAndDisclosuresWorkWithoutJavaScript(String skin, String mode) {
        check("layouts", skin, mode, new Browser.NewContextOptions().setJavaScriptEnabled(false).setViewportSize(390, 850), tab -> {
            tab.keyboard().press("Tab"); tab.keyboard().press("Enter");
            assertThat(tab.locator("main")).isFocused();
            tab.locator("#layout-trigger").click();
            Locator nav = tab.locator("#layout-navigation"); nav.locator("summary.sw-sidebar-menu-button").click();
            nav.getByRole(AriaRole.LINK, new Locator.GetByRoleOptions().setName("Recent").setExact(true)).click();
            assertThat(tab.getByRole(AriaRole.HEADING, new Page.GetByRoleOptions().setLevel(1))).hasText("Pick up where you left off.");
            tab.getByRole(AriaRole.LINK, new Page.GetByRoleOptions().setName("Try the other layout")).click();
            tab.setViewportSize(1200, 850); assertThat(nav).isVisible(); assertThat(tab.locator("#layout-trigger")).isHidden();
            tab.setViewportSize(390, 850); tab.locator("#layout-trigger").click(); assertThat(nav).isVisible();
        });
    }

    @ParameterizedTest(name = "{0}, {1}")
    @MethodSource("everySkinAndMode")
    void layoutRtlForcedColorsAndPrintingRetainReadableContent(String skin, String mode) {
        check("header-layout", skin, mode, new Browser.NewContextOptions().setViewportSize(320, 850), tab -> {
            tab.locator("html").evaluate("n => n.dir='rtl'");
            tab.emulateMedia(new Page.EmulateMediaOptions().setForcedColors(ForcedColors.ACTIVE)
                    .setReducedMotion(com.microsoft.playwright.options.ReducedMotion.REDUCE));
            tab.locator("#layout-trigger").click();
            Locator current = tab.locator("#layout-navigation [aria-current=page]");
            assertThat(current).isFocused(); assertThat(current).hasCSS("outline-style", "solid");
            assertThat(describe(new AxeBuilder(tab).withTags(WCAG).analyze().getViolations())).isEmpty();
            tab.keyboard().press("Escape");
            tab.emulateMedia(new Page.EmulateMediaOptions().setForcedColors(ForcedColors.NONE)
                    .setMedia(com.microsoft.playwright.options.Media.PRINT));
            assertThat(tab.locator("#layout-navigation")).isHidden(); assertThat(tab.locator(".sw-site-header")).isHidden();
            assertThat(tab.getByRole(AriaRole.HEADING, new Page.GetByRoleOptions().setLevel(1))).isVisible();
            tab.emulateMedia(new Page.EmulateMediaOptions().setMedia(com.microsoft.playwright.options.Media.SCREEN));
        });
    }

    @ParameterizedTest(name = "{0}, {1}")
    @MethodSource("everySkinAndMode")
    void disclosureTabsUseRovingKeyboardFocusAndPreservePanelValues(String skin, String mode) {
        check("disclosure", skin, mode, tab -> {
            Locator account = tab.locator("#settings-tabs-tab-0"), preferences = tab.locator("#settings-tabs-tab-1");
            tab.locator("#settings-note-0").fill("Keep this little idea");
            account.focus(); tab.keyboard().press("ArrowRight");
            assertThat(preferences).isFocused(); assertThat(preferences).hasAttribute("aria-selected", "true");
            assertThat(account).hasAttribute("tabindex", "-1");
            assertThat(tab.locator("#settings-tabs-panel-0")).hasAttribute("hidden", "until-found");
            assertThat(tab.locator("#settings-tabs-tab-2")).hasAttribute("aria-disabled", "true");
            assertThat(tab.locator("#settings-tabs-tab-3")).hasCount(0);
            tab.keyboard().press("Tab"); assertThat(tab.locator("#settings-tabs-panel-1")).isFocused();
            account.focus(); tab.keyboard().press("End"); assertThat(preferences).isFocused();
            tab.keyboard().press("ArrowRight"); assertThat(account).isFocused();
            assertThat(tab.locator("#settings-note-0")).hasValue("Keep this little idea");
            tab.locator("#settings-tabs-tab-2").click(new Locator.ClickOptions().setForce(true));
            assertThat(account).hasAttribute("aria-selected", "true");
        });
    }

    @ParameterizedTest(name = "{0}, {1}")
    @MethodSource("everySkinAndMode")
    void disclosureTabLinksReplaceTheAddressAndRetainNativeModifierNavigation(String skin, String mode) {
        check("disclosure", skin, mode, tab -> {
            java.util.concurrent.atomic.AtomicInteger documents = new java.util.concurrent.atomic.AtomicInteger();
            tab.onRequest(request -> { if (request.isNavigationRequest() && request.resourceType().equals("document")) documents.incrementAndGet(); });
            int entries = ((Number) tab.evaluate("() => history.length")).intValue();
            tab.locator("#settings-tabs-tab-1").click();
            assertThat(tab.url()).contains("tab=preferences"); assertThat(documents.get()).isZero();
            assertThat(((Number) tab.evaluate("() => history.length")).intValue()).isEqualTo(entries);
            try (Page other = tab.context().waitForPage(() -> tab.locator("#settings-tabs-tab-0").click(new Locator.ClickOptions()
                    .setModifiers(List.of(com.microsoft.playwright.options.KeyboardModifier.CONTROL))))) {
                other.waitForLoadState(); assertThat(other.locator("#settings-tabs-tab-0")).hasAttribute("aria-selected", "true");
            }
            assertThat(tab.locator("#settings-tabs-tab-1")).hasAttribute("aria-selected", "true");
            tab.reload(); assertThat(tab.locator("#settings-tabs-tab-1")).hasAttribute("aria-selected", "true");
        });
    }

    @ParameterizedTest(name = "{0}, {1}")
    @MethodSource("everySkinAndMode")
    void disclosureManualVerticalTabsAndNestedTabsKeepSeparateState(String skin, String mode) {
        check("disclosure", skin, mode, tab -> {
            Locator overview = tab.locator("#manual-tabs-tab-0"), activity = tab.locator("#manual-tabs-tab-1");
            overview.focus(); tab.keyboard().press("ArrowDown"); assertThat(activity).isFocused();
            assertThat(overview).hasAttribute("aria-selected", "true"); assertThat(activity).hasAttribute("aria-selected", "false");
            tab.evaluate("() => document.querySelector('#manual-tabs-panel-0 p').append(document.createTextNode(' Ready.'))");
            assertThat(activity).hasAttribute("tabindex", "0");
            tab.keyboard().press("ArrowRight"); assertThat(activity).isFocused();
            tab.keyboard().press("Space"); assertThat(activity).hasAttribute("aria-selected", "true");
            tab.keyboard().press("Home"); assertThat(overview).isFocused();
            tab.keyboard().press("Enter"); assertThat(overview).hasAttribute("aria-selected", "true");
            tab.locator("#nested-tabs-0-tab-1").click();
            assertThat(overview).hasAttribute("aria-selected", "true");
            assertThat(tab.locator("#nested-tabs-0-tab-1")).hasAttribute("aria-selected", "true");
            tab.keyboard().press("ArrowRight");
            assertThat(tab.locator("#nested-tabs-0-tab-0")).isFocused();
            assertThat(overview).hasAttribute("aria-selected", "true");
            overview.focus(); tab.keyboard().press("End");
            assertThat(tab.locator("#manual-tabs-tab-2")).isFocused();
            tab.keyboard().press("Tab"); assertThat(overview).hasAttribute("tabindex", "0");
        });
    }

    @ParameterizedTest(name = "{0}, {1}")
    @MethodSource("everySkinAndMode")
    void disclosureFindMatchesRevealNestedPanelsAndAjaxResetsTheServerSelection(String skin, String mode) {
        check("disclosure", skin, mode, tab -> {
            tab.evaluate("() => {window.tabShows=0;document.addEventListener('sw-tabs-show',()=>window.tabShows++,true);}");
            tab.evaluate("() => location.hash='settings-note-1'");
            assertThat(tab.locator("#settings-tabs-tab-1")).hasAttribute("aria-selected", "true");
            assertThat(tab.url()).contains("#settings-note-1");
            tab.locator("#manual-tabs-tab-2").click();
            tab.evaluate("() => location.hash='nested-tabs-0-panel-1'");
            assertThat(tab.locator("#manual-tabs-tab-0")).hasAttribute("aria-selected", "true");
            assertThat(tab.locator("#nested-tabs-0-tab-1")).hasAttribute("aria-selected", "true");
            assertThat(((Number) tab.evaluate("() => window.tabShows")).intValue()).isGreaterThanOrEqualTo(3);
            var old = tab.locator("#settings-tabs").elementHandle();
            tab.getByRole(AriaRole.BUTTON, new Page.GetByRoleOptions().setName("Restore initial tab").setExact(true)).click();
            tab.waitForFunction("old => !old.isConnected", old);
            assertThat(tab.locator("#settings-tabs-tab-0")).hasAttribute("aria-selected", "true");
            tab.locator("#settings-tabs-tab-1").click();
            assertThat(tab.locator("#settings-tabs-panel-1")).isVisible();
        });
    }

    @ParameterizedTest(name = "{0}, {1}")
    @MethodSource("everySkinAndMode")
    void disclosureAccordionsUseNativeExclusiveMultipleAndIndependentGroups(String skin, String mode) {
        check("disclosure", skin, mode, tab -> {
            Locator items = tab.locator("#faq-single > details");
            assertThat(items.nth(0)).hasJSProperty("open", true);
            items.nth(1).locator("summary").focus(); tab.keyboard().press("Space");
            assertThat(items.nth(1)).hasJSProperty("open", true); assertThat(items.nth(0)).hasJSProperty("open", false);
            tab.keyboard().press("Enter"); assertThat(items.nth(1)).hasJSProperty("open", false);
            tab.getByRole(AriaRole.LINK, new Page.GetByRoleOptions().setName("Read about making it yours")).click();
            assertThat(items.nth(1)).hasJSProperty("open", true);
            Locator multiple = tab.locator("details.sw-accordion-item:not(#faq-single > details)");
            assertThat(multiple.nth(0)).hasJSProperty("open", true); assertThat(multiple.nth(1)).hasJSProperty("open", true);
            multiple.nth(0).locator("summary").focus(); tab.keyboard().press("Enter");
            assertThat(multiple.nth(0)).hasJSProperty("open", false); assertThat(multiple.nth(1)).hasJSProperty("open", true);
            tab.locator("#workspace-more > summary").click(); assertThat(tab.locator("#workspace-more")).hasJSProperty("open", true);
            Locator nested = tab.locator("#workspace-more details");
            assertThat(nested.nth(0)).hasJSProperty("open", true);
            nested.nth(1).locator("summary").click(); assertThat(nested.nth(0)).hasJSProperty("open", true);
            assertThat(nested.nth(1)).hasJSProperty("open", true); assertThat(items.nth(1)).hasJSProperty("open", true);
            String name = items.nth(0).getAttribute("name");
            var oldFaq = tab.locator("#faq-single").elementHandle();
            tab.getByRole(AriaRole.BUTTON, new Page.GetByRoleOptions().setName("Open the second answer")).click();
            tab.waitForFunction("old => !old.isConnected", oldFaq);
            assertThat(items.nth(1)).hasJSProperty("open", true);
            assertThat(items.nth(0).getAttribute("name")).isNotEqualTo(name);
        });
    }

    @ParameterizedTest(name = "{0}, {1}")
    @MethodSource("everySkinAndMode")
    void disclosureDisabledSummariesKeepTheirStateAndClosedContentOutOfThePage(String skin, String mode) {
        check("disclosure", skin, mode, tab -> {
            Locator disabled = tab.locator("details[data-disabled]");
            assertThat(disabled).hasCount(3);
            for (int i = 0; i < 3; i++) {
                Locator root = disabled.nth(i), summary = root.locator(":scope > summary");
                boolean wasOpen = (Boolean) root.evaluate("n => n.open");
                assertThat(summary).hasAttribute("tabindex", "-1"); assertThat(summary).hasAttribute("aria-disabled", "true");
                summary.click(new Locator.ClickOptions().setForce(true)); assertThat(root).hasJSProperty("open", wasOpen);
                summary.focus(); tab.keyboard().press("Enter"); assertThat(root).hasJSProperty("open", wasOpen);
            }
            assertThat(tab.getByText("Private preview content", new Page.GetByTextOptions().setExact(true))).hasCount(0);
            assertThat(tab.getByText("This preview is not available yet.", new Page.GetByTextOptions().setExact(true))).hasCount(0);
        });
    }

    @ParameterizedTest(name = "{0}, {1}")
    @MethodSource("everySkinAndMode")
    void disclosureLinksAndNativeSectionsWorkWithoutJavaScript(String skin, String mode) {
        check("disclosure", skin, mode, new Browser.NewContextOptions().setJavaScriptEnabled(false).setViewportSize(390, 850), tab -> {
            tab.locator("#settings-tabs-tab-1").click();
            assertThat(tab.locator("#settings-tabs-tab-1")).hasAttribute("aria-selected", "true");
            assertThat(tab.locator("#settings-tabs-panel-1")).isVisible(); assertThat(tab.url()).contains("tab=preferences");
            tab.locator("#faq-single > details").nth(1).locator("summary").click();
            assertThat(tab.locator("#faq-single > details").nth(1)).hasJSProperty("open", true);
            assertThat(tab.locator("#faq-single > details").nth(0)).hasJSProperty("open", false);
            tab.locator("#workspace-more > summary").focus(); tab.keyboard().press("Enter");
            assertThat(tab.locator("#workspace-more")).hasJSProperty("open", true);
            assertThat(tab.locator("#workspace-more details").first()).hasJSProperty("open", true);
        });
    }

    @ParameterizedTest(name = "{0}, {1}")
    @MethodSource("everySkinAndMode")
    void disclosureSmallRtlForcedColorAndReducedMotionKeepVisibleFocus(String skin, String mode) {
        check("disclosure", skin, mode, new Browser.NewContextOptions().setViewportSize(320, 850), tab -> {
            tab.locator("html").evaluate("n => n.dir='rtl'");
            tab.emulateMedia(new Page.EmulateMediaOptions().setForcedColors(ForcedColors.ACTIVE)
                    .setReducedMotion(com.microsoft.playwright.options.ReducedMotion.REDUCE));
            tab.locator("#settings-tabs-tab-0").focus(); tab.keyboard().press("ArrowLeft");
            assertThat(tab.locator("#settings-tabs-tab-1")).isFocused();
            assertThat(tab.locator("#settings-tabs-tab-1")).hasAttribute("aria-selected", "true");
            assertThat(tab.locator("#settings-tabs-tab-1")).hasCSS("outline-style", "solid");
            Locator summary = tab.locator("#faq-single > details").nth(1).locator("summary");
            summary.focus(); tab.keyboard().press("Enter"); assertThat(summary).hasCSS("outline-style", "solid");
            assertThat((String) summary.locator("..").evaluate("n => getComputedStyle(n,'::details-content').transitionDuration")).isEqualTo("0s");
            tab.locator("#workspace-more > summary").focus(); tab.keyboard().press("Enter");
            assertThat(tab.locator("#workspace-more > summary")).hasCSS("outline-style", "solid");
            assertThat((Boolean) tab.evaluate("() => document.documentElement.scrollWidth <= innerWidth")).isTrue();
            tab.emulateMedia(new Page.EmulateMediaOptions().setForcedColors(ForcedColors.NONE));
            tab.locator("#workspace-more > summary > span").evaluate("n => n.textContent='A very long workspace label with averylongunbrokenword'.repeat(4)");
            tab.locator("#manual-tabs-tab-0 > span").evaluate("n => n.textContent='Averylongunbrokenlabel'.repeat(6)");
            assertThat((Boolean) tab.evaluate("() => document.documentElement.scrollWidth <= innerWidth")).isTrue();
            assertThat((Boolean) tab.locator("#workspace-more > summary").evaluate("n => n.clientHeight >= n.querySelector('span').clientHeight")).isTrue();
        });
    }

    @ParameterizedTest @MethodSource("everySkinAndMode")
    void attachmentActionsDownloadsPreviewsAndAjaxRemovalStaySeparate(String skin, String mode) {
        check("attachments", skin, mode, tab -> {
            Locator photo = tab.locator("#photo-attachment");
            Locator trigger = photo.getByRole(AriaRole.BUTTON, new Locator.GetByRoleOptions().setName("Preview Wicklow hills.svg"));
            Locator download = photo.getByRole(AriaRole.LINK, new Locator.GetByRoleOptions().setName("Download Wicklow hills.svg"));
            var saved = tab.waitForDownload(download::click);
            assertThat(saved.suggestedFilename()).isEqualTo("wicklow-hills.svg");
            assertThat(tab.getByRole(AriaRole.DIALOG)).isHidden();
            download.focus(); tab.keyboard().press("Tab"); assertThat(trigger).isFocused();
            trigger.hover();
            tab.waitForFunction("() => getComputedStyle(document.querySelector('#photo-attachment .sw-attachment-trigger')).backgroundColor === 'rgba(0, 0, 0, 0)'");
            var cardBox = photo.boundingBox(); var triggerBox = trigger.boundingBox();
            double borderAllowance = "sketch".equals(skin) ? 4 : 2; // two 2px pencil edges, otherwise two 1px edges
            assertThat(Math.abs(cardBox.width - triggerBox.width)).isLessThanOrEqualTo(borderAllowance);
            assertThat(Math.abs(cardBox.height - triggerBox.height)).isLessThanOrEqualTo(borderAllowance);
            photo.click(new Locator.ClickOptions().setPosition(70, 25));
            Locator dialog = tab.getByRole(AriaRole.DIALOG); assertThat(dialog).isVisible();
            assertThat(dialog.locator("img")).isVisible(); tab.keyboard().press("Escape"); assertThat(dialog).isHidden();
            assertThat(trigger).isFocused();
            tab.getByRole(AriaRole.BUTTON, new Page.GetByRoleOptions().setName("Remove Workspace notes.txt")).click();
            assertThat(tab.locator("#notes-attachment")).isHidden();
            assertThat(tab.getByRole(AriaRole.BUTTON, new Page.GetByRoleOptions().setName("Restore notes"))).isFocused();
            assertThat(tab.getByRole(AriaRole.STATUS)).containsText("Workspace notes removed");
            tab.getByRole(AriaRole.BUTTON, new Page.GetByRoleOptions().setName("Restore notes")).click();
            assertThat(tab.locator("#notes-attachment")).isVisible();
            var notes = tab.waitForDownload(() -> tab.getByRole(AriaRole.LINK, new Page.GetByRoleOptions().setName("Download Workspace notes.txt")).click());
            assertThat(notes.suggestedFilename()).isEqualTo("workspace-notes.txt");
        });
    }

    @ParameterizedTest @MethodSource("everySkinAndMode")
    void attachmentServerStatesResetBusyAndAnnounceOneReadableStatus(String skin, String mode) {
        check("attachments", skin, mode, tab -> {
            Locator file = tab.locator("#status-attachment");
            assertThat(file).hasAttribute("data-state", "idle");
            for (String state : List.of("uploading", "processing", "error", "done")) {
                tab.getByRole(AriaRole.BUTTON, new Page.GetByRoleOptions().setName("Next file status")).click();
                if (state.equals("done")) assertThat(file).not().hasAttribute("data-state", java.util.regex.Pattern.compile(".*"));
                else assertThat(file).hasAttribute("data-state", state);
                if (state.equals("uploading") || state.equals("processing")) {
                    assertThat(file).hasAttribute("aria-busy", "true");
                    assertThat(file.locator(".sw-attachment-title").evaluate("n => getComputedStyle(n).animationName")).isEqualTo("sw-attachment-shimmer");
                } else {
                    assertThat(file).not().hasAttribute("aria-busy", java.util.regex.Pattern.compile(".*"));
                    assertThat(file.locator(".sw-attachment-title").evaluate("n => getComputedStyle(n).animationName")).isEqualTo("none");
                }
                assertThat(file.locator("[aria-live], [role='alert']")).hasCount(0);
                if (state.equals("error")) {
                    assertThat(file).containsText("Upload failed · Please try again");
                    assertThat(tab.getByRole(AriaRole.STATUS)).containsText("Upload failed");
                }
            }
        });
    }

    @ParameterizedTest @MethodSource("everySkinAndMode")
    void attachmentGroupsWrapScrollAndFollowNativeKeyboardFocus(String skin, String mode) {
        check("attachments", skin, mode, tab -> {
            Locator row = tab.locator("#attachment-row"); Locator grid = tab.locator("#attachment-gallery");
            assertThat(row).hasAttribute("role", "group"); assertThat(row).hasAttribute("tabindex", "0");
            assertThat(grid).not().hasAttribute("tabindex", java.util.regex.Pattern.compile(".*"));
            tab.setViewportSize(375, 900); row.focus(); tab.keyboard().press("ArrowRight");
            tab.waitForFunction("() => document.querySelector('#attachment-row').scrollLeft > 0");
            assertThat(row.evaluate("n => getComputedStyle(n).maskImage")).isEqualTo("none");
            for (int width : List.of(375, 1100)) {
                tab.setViewportSize(width, 900);
                assertThat(grid.evaluate("n => n.scrollWidth <= n.clientWidth + 1")).isEqualTo(true);
                assertThat(tab.locator("html").evaluate("n => n.scrollWidth <= innerWidth + 1")).isEqualTo(true);
                List<Double> widths = grid.locator(":scope > .sw-attachment").all().stream().map(card -> card.boundingBox().width).toList();
                assertThat(widths).hasSize(3); assertThat(widths.get(0)).isCloseTo(widths.get(1), org.assertj.core.data.Offset.offset(1.0));
            }
            row.evaluate("n => { n.dir = 'rtl'; n.scrollLeft = 0; }"); row.focus(); tab.keyboard().press("ArrowLeft");
            tab.waitForFunction("() => document.querySelector('#attachment-row').scrollLeft < 0");
            tab.locator("#attachment-gallery .sw-attachment-trigger").last().focus();
            assertThat(grid.locator(".sw-attachment-trigger").last()).isFocused();
        });
    }

    @ParameterizedTest @MethodSource("everySkinAndMode")
    void attachmentThumbnailsRecoverFromErrorsAndCachedAjaxReplacements(String skin, String mode) {
        check("attachments", skin, mode, tab -> {
            Locator image = tab.locator("#photo-attachment img");
            assertThat(image).hasAttribute("data-status", "loaded");
            String good = image.getAttribute("src");
            tab.route("**/broken-thumbnail", route -> route.fulfill(new com.microsoft.playwright.Route.FulfillOptions()
                    .setStatus(200).setContentType("image/png").setBody("not a picture")));
            image.evaluate("n => n.src = '/broken-thumbnail'"); assertThat(image).hasAttribute("data-status", "error");
            assertThat(image).isHidden(); assertThat(tab.locator("#photo-attachment .sw-attachment-fallback")).isVisible();
            image.evaluate("(n, src) => n.src = src", good); assertThat(image).hasAttribute("data-status", "loaded");
            assertThat(image).isVisible();
            image.evaluate("n => n.replaceWith(n.cloneNode(true))");
            assertThat(tab.locator("#photo-attachment img")).hasAttribute("data-status", "loaded");
            assertThat(tab.getByRole(AriaRole.IMG, new Page.GetByRoleOptions().setName("Wicklow landscape").setExact(true))).isVisible();
        });
    }

    @ParameterizedTest @MethodSource("everySkinAndMode")
    void attachmentNativePreviewsAndDownloadsWorkWithoutJavaScript(String skin, String mode) {
        check("attachments", skin, mode, new Browser.NewContextOptions().setJavaScriptEnabled(false), tab -> {
            var saved = tab.waitForDownload(() -> tab.getByRole(AriaRole.LINK, new Page.GetByRoleOptions().setName("Download Wicklow hills.svg")).click());
            assertThat(saved.suggestedFilename()).isEqualTo("wicklow-hills.svg");
            tab.getByRole(AriaRole.BUTTON, new Page.GetByRoleOptions().setName("Preview Wicklow hills.svg")).click();
            assertThat(tab.getByRole(AriaRole.DIALOG)).isVisible(); tab.keyboard().press("Escape"); assertThat(tab.getByRole(AriaRole.DIALOG)).isHidden();
            assertThat(tab.locator("#photo-attachment img")).isVisible();
        });
    }

    @ParameterizedTest @MethodSource("everySkinAndMode")
    void attachmentReducedMotionAndForcedColorsKeepStateAndFocusReadable(String skin, String mode) {
        check("attachments", skin, mode, new Browser.NewContextOptions().setReducedMotion(com.microsoft.playwright.options.ReducedMotion.REDUCE).setForcedColors(ForcedColors.ACTIVE), tab -> {
            tab.getByRole(AriaRole.BUTTON, new Page.GetByRoleOptions().setName("Next file status")).click();
            assertThat(tab.locator("#status-attachment")).hasAttribute("aria-busy", "true");
            Locator title = tab.locator("#status-attachment .sw-attachment-title");
            assertThat(title.evaluate("n => getComputedStyle(n).animationName")).isEqualTo("none");
            assertThat(title.evaluate("n => getComputedStyle(n).webkitTextFillColor")).isNotEqualTo("rgba(0, 0, 0, 0)");
            Locator trigger = tab.locator("#photo-attachment .sw-attachment-trigger"); trigger.focus();
            assertThat(((Number) trigger.evaluate("n => parseFloat(getComputedStyle(n).outlineWidth)")).doubleValue()).isGreaterThanOrEqualTo(2);
            assertThat(tab.locator("#status-attachment")).containsText("Uploading · 64%");
            // Check the native system-color fallback above, then audit the authored palette like the other showcases.
            tab.emulateMedia(new Page.EmulateMediaOptions().setForcedColors(ForcedColors.NONE));
        });
    }


    @ParameterizedTest @MethodSource("everySkinAndMode")
    void attachmentEmbeddedAndPrintLayoutsKeepFilesReadable(String skin, String mode) {
        var settings = dev.jbaby.shadwick.ShadwickSettings.get(wicketApp);
        settings.setBundle(dev.jbaby.shadwick.Bundle.EMBEDDED);
        try {
            check("attachments", skin, mode, tab -> {
                assertThat(tab.locator("body").evaluate("n => getComputedStyle(n).margin")).isEqualTo("8px");
                Locator photo = tab.locator("#photo-attachment"); Locator trigger = photo.locator(".sw-attachment-trigger");
                assertThat(Math.abs(photo.boundingBox().width - trigger.boundingBox().width)).isLessThanOrEqualTo("sketch".equals(skin) ? 4 : 2);
                trigger.hover();
                tab.waitForFunction("() => getComputedStyle(document.querySelector('#photo-attachment .sw-attachment-trigger')).backgroundColor === 'rgba(0, 0, 0, 0)'");
                tab.getByRole(AriaRole.BUTTON, new Page.GetByRoleOptions().setName("Next file status")).click();
                assertThat(tab.locator("#status-attachment")).hasAttribute("aria-busy", "true");
                tab.emulateMedia(new Page.EmulateMediaOptions().setMedia(com.microsoft.playwright.options.Media.PRINT));
                Locator title = tab.locator("#status-attachment .sw-attachment-title");
                assertThat(title.evaluate("n => getComputedStyle(n).animationName")).isEqualTo("none");
                assertThat(title.evaluate("n => getComputedStyle(n).webkitTextFillColor")).isNotEqualTo("rgba(0, 0, 0, 0)");
                assertThat(trigger).isHidden();
                assertThat(tab.locator("#attachment-row").evaluate("n => getComputedStyle(n).overflowX")).isEqualTo("visible");
                // Audit the interactive screen layout; print intentionally removes preview/action controls.
                tab.emulateMedia(new Page.EmulateMediaOptions().setMedia(com.microsoft.playwright.options.Media.SCREEN));
                tab.getByRole(AriaRole.BUTTON, new Page.GetByRoleOptions().setName("Next file status")).click();
                assertThat(tab.locator("#status-attachment")).hasAttribute("data-state", "processing");
                tab.getByRole(AriaRole.BUTTON, new Page.GetByRoleOptions().setName("Next file status")).click();
                assertThat(tab.locator("#status-attachment")).containsText("Upload failed");
            });
        } finally { settings.setBundle(dev.jbaby.shadwick.Bundle.STANDALONE); }
    }

    @ParameterizedTest @MethodSource("everySkinAndMode")
    void advancedTableDraftsValidateCancelAndSaveWithKeyboard(String skin, String mode) {
        check("advanced-tables", skin, mode, tab -> {
            Locator row = tab.locator(".sw-table > tbody > tr:not(.sw-table-row-details)").filter(new Locator.FilterOptions().setHasText("Customer portal"));
            row.locator(".sw-table-details-button").click();
            assertThat(row.locator(".sw-table-details-button")).hasAttribute("aria-expanded", "true");
            assertThat(tab.locator(".ex-work-details")).containsText("TASK-1001");
            assertThat(tab.locator(".sw-table-row-details:visible > td")).hasAttribute("colspan", "7");
            row.locator(".sw-table-editable").click();
            Locator input = row.locator("input.sw-input"); assertThat(input).isFocused(); input.fill("0");
            row.getByRole(AriaRole.BUTTON, new Locator.GetByRoleOptions().setName("Save").setExact(true)).click();
            assertThat(input).hasAttribute("aria-invalid", "true"); assertThat(row.locator(".sw-field-error")).containsText("between 1 and 80");
            input.press("Escape"); assertThat(row.locator(".sw-table-editable")).hasText("8"); assertThat(row.locator(".sw-table-editable")).isFocused();
            row.locator(".sw-table-editable").press("Enter"); input.fill("13"); input.press("Enter");
            assertThat(tab.getByRole(AriaRole.STATUS)).hasText("Saved 13 hours for Customer portal.");
            assertThat(row.locator(".sw-table-editable")).hasText("13"); assertThat(row.locator(".sw-table-editable")).isFocused();
            assertThat(tab.locator(".sw-table > tbody > tr:not(.sw-table-row-details)").filter(new Locator.FilterOptions().setHasText("Autumn release")).locator(".sw-table-editable")).hasAttribute("aria-disabled", "true");
        });
    }

    @ParameterizedTest @MethodSource("everySkinAndMode")
    void advancedTableChooserSelectionAndFilteredCsvWorkTogether(String skin, String mode) {
        check("advanced-tables", skin, mode, tab -> {
            tab.getByRole(AriaRole.CHECKBOX, new Page.GetByRoleOptions().setName("Select Customer portal").setExact(true)).check();
            tab.getByRole(AriaRole.BUTTON, new Page.GetByRoleOptions().setName("Columns").setExact(true)).click();
            tab.getByRole(AriaRole.MENUITEMCHECKBOX, new Page.GetByRoleOptions().setName("Owner").setExact(true)).click();
            assertThat(tab.locator(".sw-table th:visible")).hasCount(6);
            assertThat(tab.getByRole(AriaRole.CHECKBOX, new Page.GetByRoleOptions().setName("Select Customer portal").setExact(true))).isChecked();
            assertThat(tab.getByRole(AriaRole.BUTTON, new Page.GetByRoleOptions().setName("Columns").setExact(true))).isFocused();
            var export = tab.getByRole(AriaRole.LINK, new Page.GetByRoleOptions().setName("Export CSV").setExact(true));
            var download = tab.waitForDownload(export::click); assertThat(download.suggestedFilename()).isEqualTo("shadwick-work.csv");
            try {
                String csv = java.nio.file.Files.readString(download.path());
                assertThat(csv).startsWith("\ufeffProject,Hours,Status,Owner,Due\r\n").contains("Customer portal,8,In progress,Maya Chen,2026-11-09\r\n");
                assertThat(csv.lines().count()).isEqualTo(10);
            } catch (java.io.IOException failure) { throw new java.io.UncheckedIOException(failure); }
            tab.getByLabel("Find a project", new Page.GetByLabelOptions().setExact(true)).fill("Maya");
            tab.getByRole(AriaRole.BUTTON, new Page.GetByRoleOptions().setName("Filter").setExact(true)).click();
            assertThat(tab.locator(".sw-table-toolbar")).hasText("2 projects in view");
            download = tab.waitForDownload(export::click);
            try { assertThat(java.nio.file.Files.readString(download.path()).lines().count()).isEqualTo(3); }
            catch (java.io.IOException failure) { throw new java.io.UncheckedIOException(failure); }
        });
    }

    @ParameterizedTest @MethodSource("everySkinAndMode")
    void advancedTableNativeDetailsEditingAndColumnChoicesWorkWithoutScript(String skin, String mode) {
        check("advanced-tables", skin, mode, new Browser.NewContextOptions().setJavaScriptEnabled(false), tab -> {
            Locator row = tab.locator(".sw-table > tbody > tr:not(.sw-table-row-details)").filter(new Locator.FilterOptions().setHasText("Customer portal"));
            row.locator(".sw-table-details-button").click(); assertThat(tab.locator(".ex-work-details")).containsText("TASK-1001");
            row.locator(".sw-table-editable").click(); row.locator("input.sw-input").fill("0");
            row.getByRole(AriaRole.BUTTON, new Locator.GetByRoleOptions().setName("Save").setExact(true)).click();
            assertThat(row.locator("input.sw-input")).hasAttribute("aria-invalid", "true");
            row.locator("input.sw-input").fill("9"); row.getByRole(AriaRole.BUTTON, new Locator.GetByRoleOptions().setName("Save").setExact(true)).click();
            assertThat(row.locator(".sw-table-editable")).hasText("9");
            tab.getByRole(AriaRole.BUTTON, new Page.GetByRoleOptions().setName("Columns").setExact(true)).click();
            tab.getByRole(AriaRole.MENUITEMCHECKBOX, new Page.GetByRoleOptions().setName("Due").setExact(true)).click();
            assertThat(tab.locator(".sw-table th:visible")).hasCount(6);
        });
    }

    @ParameterizedTest @MethodSource("everySkinAndMode")
    void advancedTableMobileLayoutAndEditingHaveNoPageOverflow(String skin, String mode) {
        check("advanced-tables", skin, mode, new Browser.NewContextOptions().setViewportSize(390, 900), tab -> {
            tab.getByRole(AriaRole.LINK, new Page.GetByRoleOptions().setName("Show details for Customer portal").setExact(true)).click();
            Locator row=tab.locator(".sw-table > tbody > tr:not(.sw-table-row-details)").filter(new Locator.FilterOptions().setHasText("Customer portal"));
            row.locator(".sw-table-editable").click(); row.locator("input.sw-input").fill("bad");
            row.getByRole(AriaRole.BUTTON, new Locator.GetByRoleOptions().setName("Save").setExact(true)).click();
            assertThat(row.locator("input.sw-input")).hasAttribute("aria-invalid", "true");
            assertThat(tab.evaluate("document.documentElement.scrollWidth <= innerWidth + 1")).isEqualTo(true);
            assertThat(tab.locator(".sw-table-container").evaluate("n => n.scrollWidth > n.clientWidth")).isEqualTo(true);
            var viewport=tab.locator(".sw-table-container").boundingBox();
            var input=row.locator("input.sw-input").boundingBox();
            assertThat(input.x).isGreaterThanOrEqualTo(viewport.x - 1);
            assertThat(input.x + input.width).isLessThanOrEqualTo(viewport.x + viewport.width + 1);
            var details=tab.locator(".ex-work-details").boundingBox();
            assertThat(details.x).isGreaterThanOrEqualTo(viewport.x - 1);
            assertThat(details.x + details.width).isLessThanOrEqualTo(viewport.x + viewport.width + 1);
        });
    }

    @ParameterizedTest @MethodSource("everySkinAndMode")
    void dataBookmarkableSortingPagingFiltersAndHistoryKeepTheView(String skin, String mode) {
        check("data", skin, mode, tab -> {
            Locator table = tab.locator("#invoice-table");
            Locator amount = table.getByRole(AriaRole.LINK, new Locator.GetByRoleOptions().setName("Amount").setExact(true));
            amount.click(); assertThat(table.getByRole(AriaRole.COLUMNHEADER, new Locator.GetByRoleOptions().setName("Amount"))).hasAttribute("aria-sort", "ascending");
            amount.click(); assertThat(table.getByRole(AriaRole.COLUMNHEADER, new Locator.GetByRoleOptions().setName("Amount"))).hasAttribute("aria-sort", "descending");
            assertThat(table.locator("tbody > tr").first()).containsText("INV-2024");
            table.getByRole(AriaRole.LINK, new Locator.GetByRoleOptions().setName("Go to next page")).click();
            assertThat(tab.evaluate("() => new URL(location.href).searchParams.get('page')")).isEqualTo("1");
            assertThat(tab.evaluate("() => new URL(location.href).searchParams.get('sort')")).isEqualTo("amount,desc");
            assertThat(table.locator(".sw-pagination-summary")).hasText("6–10 of 24");
            String row = table.locator("tbody > tr").first().getByRole(AriaRole.LINK).first().innerText(); tab.reload(); assertThat(table.locator("tbody > tr").first()).containsText(row);
            tab.goBack(); assertThat(table.locator("tbody > tr").first()).containsText("INV-2024");
            tab.goForward(); assertThat(table.locator(".sw-pagination-summary")).hasText("6–10 of 24");
            tab.getByLabel("Search invoices", new Page.GetByLabelOptions().setExact(true)).fill("Grace");
            tab.getByRole(AriaRole.BUTTON, new Page.GetByRoleOptions().setName("Apply filters")).click();
            assertThat(tab.evaluate("() => new URL(location.href).searchParams.get('page')")).isNull();
            assertThat(tab.evaluate("() => new URL(location.href).searchParams.get('sort')")).isEqualTo("amount,desc");
            assertThat(table.locator(".sw-pagination-summary")).hasText("1–1 of 1");
            table.getByRole(AriaRole.LINK, new Locator.GetByRoleOptions().setName("INV-2002").setExact(true)).click();
            assertThat(tab.getByRole(AriaRole.HEADING, new Page.GetByRoleOptions().setName("INV-2002"))).isVisible();
            assertThat(describe(new AxeBuilder(tab).withTags(WCAG).analyze().getViolations())).isEmpty();
            tab.getByRole(AriaRole.LINK, new Page.GetByRoleOptions().setName("Back to invoices")).click();
            assertThat(tab.getByLabel("Search invoices", new Page.GetByLabelOptions().setExact(true))).hasValue("Grace");
        });
    }

    @ParameterizedTest @MethodSource("everySkinAndMode")
    void dataAjaxSortingPagingSelectionAndBulkActionsKeepFocusAndKeys(String skin, String mode) {
        check("data", skin, mode, tab -> {
            tab.navigate(url("data", skin, mode) + "&navigation=ajax"); String original = tab.url();
            Locator table = tab.locator("#invoice-table");
            table.getByRole(AriaRole.LINK, new Locator.GetByRoleOptions().setName("Go to next page")).click();
            assertThat(table.locator(".sw-pagination-summary")).hasText("6–10 of 24"); assertThat(tab.url()).isEqualTo(original);
            Locator customer = table.getByRole(AriaRole.LINK, new Locator.GetByRoleOptions().setName("Customer").setExact(true)); customer.click();
            assertThat(table.locator(".sw-pagination-summary")).hasText("1–5 of 24"); assertThat(customer).isFocused();
            Locator all = table.getByRole(AriaRole.CHECKBOX, new Locator.GetByRoleOptions().setName("Select all rows on this page"));
            table.locator("tbody input[type='checkbox']").first().check();
            tab.waitForFunction("() => document.querySelector('#invoice-table thead input[type=checkbox]').indeterminate");
            assertThat(table.locator(".sw-table-selection-count")).hasText("1 selected");
            all.focus(); tab.keyboard().press("Space"); assertThat(table.locator(".sw-table-selection-count")).hasText("5 selected"); assertThat(all).isChecked(); assertThat(all).isFocused();
            table.getByRole(AriaRole.LINK, new Locator.GetByRoleOptions().setName("Go to next page")).click();
            assertThat(table.locator(".sw-pagination-summary")).hasText("6–10 of 24"); assertThat(table.locator(".sw-table-selection-count")).hasText("5 selected");
            table.getByRole(AriaRole.BUTTON, new Locator.GetByRoleOptions().setName("Mark selected as paid")).click();
            assertThat(tab.getByRole(AriaRole.STATUS)).containsText("5 invoices marked as paid");
            assertThat(table.locator(".sw-table-bulk-actions")).isHidden();
            table.getByRole(AriaRole.LINK, new Locator.GetByRoleOptions().setName("Page 1").setExact(true)).click();
            assertThat(table.locator("tbody .sw-badge")).hasText(new String[]{"Paid", "Paid", "Paid", "Paid", "Paid"});
            assertThat(tab.url()).isEqualTo(original);
        });
    }

    @ParameterizedTest @MethodSource("everySkinAndMode")
    void dataCrudValidatesDialogsPreservesActionFocusAndConfirmsDeletion(String skin, String mode) {
        check("data", skin, mode, tab -> {
            Locator table = tab.locator("#invoice-table");
            Locator row = table.locator("tbody > tr").filter(new Locator.FilterOptions().setHasText("INV-2002"));
            Locator actions = row.getByRole(AriaRole.BUTTON, new Locator.GetByRoleOptions().setName("Actions for INV-2002")); actions.click();
            tab.getByRole(AriaRole.MENUITEM, new Page.GetByRoleOptions().setName("Edit invoice").setExact(true)).click();
            Locator dialog = tab.getByRole(AriaRole.DIALOG, new Page.GetByRoleOptions().setName("Edit INV-2002")); assertThat(dialog).isVisible();
            settleAnimations(tab);
            assertThat(describe(new AxeBuilder(tab).withTags(WCAG).analyze().getViolations())).isEmpty();
            dialog.getByLabel("Customer", new Locator.GetByLabelOptions().setExact(true)).fill("Grace Hopper Studio");
            dialog.getByLabel("Email", new Locator.GetByLabelOptions().setExact(true)).fill("wrong");
            dialog.getByRole(AriaRole.BUTTON, new Locator.GetByRoleOptions().setName("Save invoice")).click();
            assertThat(dialog).isVisible(); assertThat(dialog.locator(".sw-field-error").filter(new Locator.FilterOptions().setHasText("email"))).isVisible();
            dialog.getByLabel("Email", new Locator.GetByLabelOptions().setExact(true)).fill("grace@example.com");
            dialog.getByLabel("Amount (EUR)", new Locator.GetByLabelOptions().setExact(true)).fill("250.00");
            dialog.getByLabel("Due date", new Locator.GetByLabelOptions().setExact(true)).fill("2026-12-10");
            dialog.getByRole(AriaRole.BUTTON, new Locator.GetByRoleOptions().setName("Save invoice")).click();
            assertThat(dialog).isHidden(); assertThat(row).containsText("Grace Hopper Studio"); assertThat(row).containsText("250.00"); assertThat(actions).isFocused();
            actions.click(); tab.getByRole(AriaRole.MENUITEM, new Page.GetByRoleOptions().setName("Mark as paid").setExact(true)).click();
            assertThat(row.locator(".sw-badge")).hasText("Paid"); assertThat(actions).isFocused();
            actions.click(); tab.getByRole(AriaRole.MENUITEM, new Page.GetByRoleOptions().setName("Delete invoice").setExact(true)).click();
            Locator confirm = tab.getByRole(AriaRole.ALERTDIALOG); assertThat(confirm).isVisible();
            confirm.getByRole(AriaRole.BUTTON, new Locator.GetByRoleOptions().setName("Cancel").setExact(true)).click(); assertThat(confirm).isHidden(); assertThat(row).hasCount(1);
            actions.click(); tab.getByRole(AriaRole.MENUITEM, new Page.GetByRoleOptions().setName("Delete invoice").setExact(true)).click();
            confirm.getByRole(AriaRole.BUTTON, new Locator.GetByRoleOptions().setName("Delete invoice").setExact(true)).click();
            assertThat(confirm).isHidden(); assertThat(row).hasCount(0); assertThat(tab.getByRole(AriaRole.BUTTON, new Page.GetByRoleOptions().setName("New invoice").setExact(true))).isFocused();
            tab.getByRole(AriaRole.BUTTON, new Page.GetByRoleOptions().setName("New invoice").setExact(true)).click();
            Locator fresh = tab.getByRole(AriaRole.DIALOG, new Page.GetByRoleOptions().setName("New invoice").setExact(true));
            fresh.getByLabel("Customer", new Locator.GetByLabelOptions().setExact(true)).fill("New Studio");
            fresh.getByLabel("Email", new Locator.GetByLabelOptions().setExact(true)).fill("hello@example.com");
            fresh.getByLabel("Amount (EUR)", new Locator.GetByLabelOptions().setExact(true)).fill("75.50");
            fresh.getByRole(AriaRole.BUTTON, new Locator.GetByRoleOptions().setName("Save invoice")).click(); assertThat(fresh).isHidden();
            tab.getByLabel("Search invoices", new Page.GetByLabelOptions().setExact(true)).fill("New Studio"); tab.getByRole(AriaRole.BUTTON, new Page.GetByRoleOptions().setName("Apply filters")).click();
            assertThat(table.locator("tbody > tr")).hasCount(1); assertThat(table).containsText("INV-2025"); assertThat(table).containsText("75.50");
        });
    }

    @ParameterizedTest @MethodSource("everySkinAndMode")
    void dataWideTablesPinSelectionAndKeepRowMenusWithinTheViewport(String skin, String mode) {
        check("data", skin, mode, new Browser.NewContextOptions().setViewportSize(375, 850), tab -> {
            Locator table = tab.locator("#invoice-table"); Locator scroll = table.locator(".sw-table-container");
            scroll.evaluate("n => n.scrollLeft = 250");
            var bounds = scroll.boundingBox(); var cell = table.locator("tbody > tr > td").first().boundingBox();
            assertThat(Math.abs(bounds.x - cell.x)).isLessThan(3);
            Locator actions = table.getByRole(AriaRole.BUTTON, new Locator.GetByRoleOptions().setName("Actions for INV-2001")); actions.click();
            Locator menu = tab.getByRole(AriaRole.MENU); assertThat(menu).isVisible();
            var box = menu.boundingBox(); assertThat(box.x).isGreaterThanOrEqualTo(-1); assertThat(box.x + box.width).isLessThanOrEqualTo(376);
            tab.keyboard().press("Escape"); assertThat(menu).isHidden(); assertThat(actions).isFocused();
            tab.locator("html").evaluate("n => n.dir = 'rtl'"); scroll.evaluate("n => n.scrollLeft = -250");
            bounds = scroll.boundingBox(); cell = table.locator("tbody > tr > td").first().boundingBox();
            assertThat(Math.abs(bounds.x + bounds.width - cell.x - cell.width)).isLessThan(3);
            assertThat(tab.locator("html").evaluate("n => n.scrollWidth <= innerWidth + 1")).isEqualTo(true);
        });
    }

    @ParameterizedTest @MethodSource("everySkinAndMode")
    void dataNativeLinksAndAjaxFallbacksWorkWithoutJavaScript(String skin, String mode) {
        check("data", skin, mode, new Browser.NewContextOptions().setJavaScriptEnabled(false), tab -> {
            Locator table = tab.locator("#invoice-table");
            table.getByRole(AriaRole.LINK, new Locator.GetByRoleOptions().setName("Customer").setExact(true)).click();
            assertThat(table.getByRole(AriaRole.COLUMNHEADER, new Locator.GetByRoleOptions().setName("Customer"))).hasAttribute("aria-sort", "ascending");
            table.getByRole(AriaRole.LINK, new Locator.GetByRoleOptions().setName("Go to next page")).click(); assertThat(table.locator(".sw-pagination-summary")).hasText("6–10 of 24");
            table.getByRole(AriaRole.BUTTON, new Locator.GetByRoleOptions().setName("Rows per page 5")).click();
            tab.getByRole(AriaRole.MENUITEMRADIO, new Page.GetByRoleOptions().setName("10").setExact(true)).click(); assertThat(table.locator(".sw-pagination-summary")).hasText("1–10 of 24");
            tab.getByLabel("Search invoices", new Page.GetByLabelOptions().setExact(true)).fill("no such customer"); tab.getByRole(AriaRole.BUTTON, new Page.GetByRoleOptions().setName("Apply filters")).click();
            assertThat(table).containsText("A little breathing room."); assertThat(table.locator(".sw-pagination-summary")).hasText("0 of 0");
            tab.navigate(url("data", skin, mode) + "&navigation=ajax");
            table.getByRole(AriaRole.LINK, new Locator.GetByRoleOptions().setName("Go to next page")).click(); assertThat(table.locator(".sw-pagination-summary")).hasText("6–10 of 24");
            table.getByRole(AriaRole.LINK, new Locator.GetByRoleOptions().setName("Customer").setExact(true)).click(); assertThat(table.locator(".sw-pagination-summary")).hasText("1–5 of 24");
        });
    }

    @ParameterizedTest @MethodSource("everySkinAndMode")
    void dataPaginationQualifiersKeepListsIndependentAndSizesResetOnlyTheirPage(String skin, String mode) {
        check("pagination", skin, mode, tab -> {
            tab.navigate(url("pagination", skin, mode) + "&tag=green&tag=blue");
            Locator projects = tab.getByRole(AriaRole.NAVIGATION, new Page.GetByRoleOptions().setName("Project pages").setExact(true));
            Locator archive = tab.getByRole(AriaRole.NAVIGATION, new Page.GetByRoleOptions().setName("Archive pages").setExact(true));
            projects.getByRole(AriaRole.LINK, new Locator.GetByRoleOptions().setName("Page 28").setExact(true)).click();
            assertThat(tab.locator(".sw-pagination-summary").first()).hasText("271–275 of 275");
            archive.getByRole(AriaRole.LINK, new Locator.GetByRoleOptions().setName("Page 24").setExact(true)).click();
            assertThat(tab.evaluate("() => new URL(location.href).searchParams.get('page')")).isEqualTo("27");
            assertThat(tab.evaluate("() => new URL(location.href).searchParams.get('archive_page')")).isEqualTo("23");
            tab.getByRole(AriaRole.BUTTON, new Page.GetByRoleOptions().setName("Rows per page 10")).click();
            tab.getByRole(AriaRole.MENUITEMRADIO, new Page.GetByRoleOptions().setName("20").setExact(true)).click();
            assertThat(tab.locator(".sw-pagination-summary").first()).hasText("1–20 of 275"); assertThat(tab.locator(".sw-pagination-summary").last()).hasText("Page 24 of 24");
            assertThat(tab.evaluate("() => new URL(location.href).searchParams.getAll('tag')")).isEqualTo(List.of("green", "blue"));
            assertThat(archive.getByRole(AriaRole.LINK, new Locator.GetByRoleOptions().setName("Go to next page"))).hasAttribute("aria-disabled", "true");
        });
    }

    @ParameterizedTest @MethodSource("everySkinAndMode")
    void dataPlainTablesKeepCompactNestedGroupedRtlAndPrintLayouts(String skin, String mode) {
        check("tables", skin, mode, new Browser.NewContextOptions().setViewportSize(375, 850), tab -> {
            double normal = ((Number) tab.locator("#normal-table tbody > tr").first().evaluate("n => n.getBoundingClientRect().height")).doubleValue();
            double compact = ((Number) tab.locator("#compact-table tbody > tr").first().evaluate("n => n.getBoundingClientRect().height")).doubleValue(); assertThat(compact).isLessThan(normal);
            assertThat(tab.locator("#inner-table th").first().evaluate("n => getComputedStyle(n).height")).isEqualTo(tab.locator("#normal-table th").first().evaluate("n => getComputedStyle(n).height"));
            Locator scroll = tab.locator("#quarter-scroll"); scroll.focus(); tab.keyboard().press("ArrowRight");
            tab.waitForFunction("() => document.querySelector('#quarter-scroll').scrollLeft > 0");
            scroll.evaluate("n => n.scrollLeft = 250");
            var box = scroll.boundingBox(); var pinned = tab.locator("#quarter-table tbody th").first().boundingBox(); assertThat(Math.abs(box.x - pinned.x)).isLessThan(3);
            tab.locator("html").evaluate("n => n.dir = 'rtl'"); scroll.evaluate("n => n.scrollLeft = -250");
            box = scroll.boundingBox(); pinned = tab.locator("#quarter-table tbody th").first().boundingBox(); assertThat(Math.abs(box.x + box.width - pinned.x - pinned.width)).isLessThan(3);
            tab.emulateMedia(new Page.EmulateMediaOptions().setMedia(com.microsoft.playwright.options.Media.PRINT));
            assertThat(tab.locator("#quarter-table tbody th").first()).hasCSS("position", "static"); assertThat(scroll).hasCSS("overflow-x", "visible");
            tab.emulateMedia(new Page.EmulateMediaOptions().setMedia(com.microsoft.playwright.options.Media.SCREEN).setForcedColors(ForcedColors.ACTIVE));
            assertThat(tab.locator("#quarter-table colgroup + colgroup").first()).hasCSS("border-inline-start-width", "1px");
            tab.emulateMedia(new Page.EmulateMediaOptions().setForcedColors(ForcedColors.NONE));
        });
    }

    @ParameterizedTest @MethodSource("everySkinAndMode")
    void dataEmbeddedBundleKeepsHostMarginsAndAccessibleControls(String skin, String mode) {
        var settings = dev.jbaby.shadwick.ShadwickSettings.get(wicketApp); settings.setBundle(dev.jbaby.shadwick.Bundle.EMBEDDED);
        try {
            check("data", skin, mode, new Browser.NewContextOptions().setViewportSize(390, 850).setReducedMotion(com.microsoft.playwright.options.ReducedMotion.REDUCE), tab -> {
                assertThat(tab.locator("body")).hasCSS("margin", "8px");
                assertThat(tab.locator(".sw-pagination-content")).hasCSS("margin", "0px"); assertThat(tab.locator(".sw-pagination-content")).hasCSS("padding", "0px");
                assertThat(tab.locator("html").evaluate("n => n.scrollWidth <= innerWidth + 1")).isEqualTo(true);
                assertThat(tab.getByRole(AriaRole.CHECKBOX, new Page.GetByRoleOptions().setName("Select INV-2005").setExact(true))).isDisabled();
                tab.emulateMedia(new Page.EmulateMediaOptions().setForcedColors(ForcedColors.ACTIVE));
                assertThat(tab.getByRole(AriaRole.CHECKBOX, new Page.GetByRoleOptions().setName("Select INV-2001").setExact(true))).hasCSS("appearance", "auto");
                tab.emulateMedia(new Page.EmulateMediaOptions().setForcedColors(ForcedColors.NONE));
            });
        } finally { settings.setBundle(dev.jbaby.shadwick.Bundle.STANDALONE); }
    }

    private void customSelectFallback(Page tab, String skin, String mode) {
        tab.addInitScript("const originalSupports = CSS.supports.bind(CSS); CSS.supports = (...args) => args.join(' ').includes('base-select') ? false : originalSupports(...args);");
        tab.navigate(url("custom-select", skin, mode) + "&fresh=fallback");
        assertThat(tab.locator(".sw-select-wrapper[data-enhanced]")).hasCount(7);
    }

    private Locator customSelectControl(Locator wrapper) {
        Locator trigger = wrapper.locator(":scope > button");
        return trigger.count() > 0 ? trigger : wrapper.locator("select");
    }

    @ParameterizedTest @MethodSource("everySkinAndMode")
    void customSelectNativeKeyboardResetAndSubmission(String skin, String mode) {
        check("custom-select", skin, mode, tab -> {
            Locator form = tab.locator("form[aria-label='Normal workspace']"), wrapper = form.locator(".sw-select-wrapper");
            Locator control = customSelectControl(wrapper);
            control.focus(); tab.keyboard().press("End"); tab.keyboard().press("Enter");
            assertThat(wrapper.locator("select")).hasValue("success");
            form.getByRole(AriaRole.BUTTON, new Locator.GetByRoleOptions().setName("Reset")).click();
            assertThat(wrapper.locator("select")).hasValue("");
            control.focus(); tab.keyboard().press("Home"); tab.keyboard().press("Enter");
            assertThat(wrapper.locator("select")).hasValue("design");
            form.getByRole(AriaRole.BUTTON, new Locator.GetByRoleOptions().setName("Save workspace")).click();
            assertThat(form.getByRole(AriaRole.STATUS).last()).hasText("Saved: Design");
        });
    }

    @ParameterizedTest @MethodSource("everySkinAndMode")
    void customSelectFallbackKeyboardTypeaheadAndCommit(String skin, String mode) {
        check("custom-select", skin, mode, tab -> {
            customSelectFallback(tab, skin, mode);
            Locator wrapper = tab.locator("form[aria-label='Normal workspace'] .sw-select-wrapper"), control = customSelectControl(wrapper);
            control.focus(); tab.keyboard().press("ArrowDown");
            assertThat(control).hasAttribute("aria-expanded", "true");
            assertThat(control).isFocused();
            tab.keyboard().press("ArrowDown"); tab.keyboard().press("ArrowDown");
            assertThat(wrapper.locator("[data-active]")).hasText("Support");
            tab.keyboard().press("Escape"); assertThat(wrapper.locator("select")).hasValue("");
            tab.keyboard().press("s"); tab.keyboard().press("u"); tab.keyboard().press("c"); tab.keyboard().press("Enter");
            assertThat(wrapper.locator("select")).hasValue("success");
            // A committed search starts a fresh typeahead sequence on the next opening.
            tab.keyboard().press("s"); assertThat(wrapper.locator("[data-active]")).hasText("Support"); tab.keyboard().press("Escape");
            tab.keyboard().press("Home"); tab.keyboard().press("Tab");
            assertThat(wrapper.locator("select")).hasValue("design");
            assertThat(control).not().isFocused();
            assertThat(tab.locator("[style]")).hasCount(0);
        });
    }

    @ParameterizedTest @MethodSource("everySkinAndMode")
    void customSelectFallbackPointerEventsCancellationAndReset(String skin, String mode) {
        check("custom-select", skin, mode, tab -> {
            customSelectFallback(tab, skin, mode);
            Locator form = tab.locator("form[aria-label='Normal workspace']"), wrapper = form.locator(".sw-select-wrapper"), control = customSelectControl(wrapper);
            wrapper.locator("select").evaluate("s => {window.selectEvents={input:0,change:0}; ['input','change'].forEach(k=>s.addEventListener(k,()=>window.selectEvents[k]++));}");
            control.click(); wrapper.getByRole(AriaRole.OPTION, new Locator.GetByRoleOptions().setName("Support").setExact(true)).click();
            assertThat(control).isFocused(); assertThat(wrapper.locator("select")).hasValue("support");
            assertThat(tab.evaluate("() => window.selectEvents")).isEqualTo(java.util.Map.of("input",1,"change",1));
            control.click(); wrapper.getByRole(AriaRole.OPTION, new Locator.GetByRoleOptions().setName("Enterprise · unavailable")).click(new Locator.ClickOptions().setForce(true));
            assertThat(wrapper.locator("select")).hasValue("support"); tab.keyboard().press("Escape");
            control.click(); tab.locator("h1").click(); assertThat(control).hasAttribute("aria-expanded", "false");
            assertThat(wrapper.locator("select")).hasValue("support");
            form.getByRole(AriaRole.BUTTON, new Locator.GetByRoleOptions().setName("Reset")).click();
            assertThat(wrapper.locator("select")).hasValue(""); assertThat(control).containsText("Choose a workspace");
        });
    }

    @ParameterizedTest @MethodSource("everySkinAndMode")
    void customSelectAjaxFeedbackAndReplacementInBothPickerModes(String skin, String mode) {
        check("custom-select", skin, mode, tab -> {
            for (boolean fallback : new boolean[]{false,true}) {
                if (fallback) customSelectFallback(tab, skin, mode);
                Locator form = tab.locator("form[aria-label='Ajax workspace']"), wrapper = form.locator(".sw-select-wrapper");
                form.getByRole(AriaRole.BUTTON, new Locator.GetByRoleOptions().setName("Save workspace")).click();
                assertThat(wrapper.locator("select")).hasAttribute("aria-invalid", "true");
                Locator control = customSelectControl(wrapper);
                if (fallback) assertThat(control).hasAttribute("aria-invalid", "true");
                control.focus(); tab.keyboard().press("Home"); tab.keyboard().press("Enter");
                form.getByRole(AriaRole.BUTTON, new Locator.GetByRoleOptions().setName("Save workspace")).click();
                assertThat(wrapper.locator("select")).hasAttribute("aria-invalid", "false");
                assertThat(form.getByRole(AriaRole.STATUS).last()).hasText("Saved: Design");
                Locator live = tab.locator("form[aria-label='Live workspace']"), liveWrapper = live.locator(".sw-select-wrapper");
                Locator liveControl = customSelectControl(liveWrapper);
                liveControl.focus(); tab.keyboard().press("End"); tab.keyboard().press("Enter");
                assertThat(live.getByRole(AriaRole.STATUS).last()).hasText("Current: Success");
                live.getByRole(AriaRole.LINK, new Locator.GetByRoleOptions().setName("Refresh choices")).click();
                assertThat(live.getByRole(AriaRole.STATUS).last()).hasText("Current: none");
                assertThat(liveWrapper.locator("select option")).hasCount(4);
                if (fallback) assertThat(liveWrapper.locator(":scope > button")).hasCount(1);
                liveControl = customSelectControl(liveWrapper); liveControl.focus(); tab.keyboard().press("ArrowDown"); tab.keyboard().press("End"); tab.keyboard().press("Enter");
                assertThat(live.getByRole(AriaRole.STATUS).last()).hasText("Current: Engineering");
            }
        });
    }

    @ParameterizedTest @MethodSource("everySkinAndMode")
    void customSelectFallbackAriaDisabledAndDynamicOptions(String skin, String mode) {
        check("custom-select", skin, mode, tab -> {
            customSelectFallback(tab, skin, mode);
            Locator wrapper = tab.locator("form[aria-label='Normal workspace'] .sw-select-wrapper"), control = customSelectControl(wrapper);
            assertThat(control).hasAttribute("aria-required", "true");
            assertThat(tab.getByRole(AriaRole.COMBOBOX, new Page.GetByRoleOptions().setName("Managed workspace"))).isDisabled();
            wrapper.locator("select").evaluate("s => {s.setAttribute('aria-label','Explicit workspace'); s.disabled=true;}");
            assertThat(control).isDisabled(); assertThat(control).hasAttribute("aria-label", "Explicit workspace");
            wrapper.locator("select").evaluate("s => {s.disabled=false; let o=new Option('Travel','travel'); s.append(o); s.value='travel'; s.dispatchEvent(new Event('change',{bubbles:true}));}");
            assertThat(control).containsText("Travel");
            control.click(); assertThat(wrapper.getByRole(AriaRole.OPTION, new Locator.GetByRoleOptions().setName("Travel"))).hasAttribute("aria-selected", "true");
            tab.keyboard().press("Escape");
            wrapper.evaluate("w => {let f=document.createElement('fieldset'); w.parentNode.insertBefore(f,w); f.append(w); f.disabled=true;}");
            assertThat(control).isDisabled();
            wrapper.evaluate("w => w.parentElement.disabled=false"); assertThat(control).isEnabled();
            wrapper.locator("select").focus(); assertThat(control).isFocused();
            tab.evaluate("() => {document.querySelector('form[aria-label=\"Normal workspace\"] select').value='design'; window.dispatchEvent(new PageTransitionEvent('pageshow',{persisted:true})); Shadwick.enhanceSelects();}");
            assertThat(control).containsText("Design"); assertThat(wrapper.locator(":scope > button")).hasCount(1);
        });
    }

    @ParameterizedTest @MethodSource("everySkinAndMode")
    void customSelectWithoutScriptSubmitsNativeValues(String skin, String mode) {
        check("custom-select", skin, mode, new Browser.NewContextOptions().setJavaScriptEnabled(false).setViewportSize(390,850), tab -> {
            Locator form = tab.locator("form[aria-label='Normal workspace']");
            form.locator("select").selectOption("support");
            form.getByRole(AriaRole.BUTTON, new Locator.GetByRoleOptions().setName("Save workspace")).click();
            assertThat(form.getByRole(AriaRole.STATUS).last()).hasText("Saved: Support");
            assertThat(tab.locator(".sw-select-wrapper[data-enhanced]")).hasCount(0);
        });
    }

    @ParameterizedTest @MethodSource("everySkinAndMode")
    void customSelectFallbackMobileEmbeddedAndForcedColors(String skin, String mode) {
        var settings = dev.jbaby.shadwick.ShadwickSettings.get(wicketApp); settings.setBundle(dev.jbaby.shadwick.Bundle.EMBEDDED);
        try {
        check("custom-select", skin, mode, new Browser.NewContextOptions().setViewportSize(320,850).setReducedMotion(com.microsoft.playwright.options.ReducedMotion.REDUCE), tab -> {
            customSelectFallback(tab, skin, mode);
            assertThat(tab.locator("link[href*='-embedded']")).hasCount(1);
            tab.locator("html").evaluate("html => html.dir='rtl'");
            Locator wrapper = tab.locator(".sw-select-wrapper").filter(new Locator.FilterOptions().setHas(tab.locator("select option[value=\"39\"]"))), control = customSelectControl(wrapper);
            control.focus(); tab.keyboard().press("ArrowDown"); tab.keyboard().press("PageDown"); tab.keyboard().press("Enter");
            assertThat(wrapper.locator("select")).hasValue("29");
            assertThat(tab.evaluate("() => document.documentElement.scrollWidth <= innerWidth")).isEqualTo(true);
            tab.emulateMedia(new Page.EmulateMediaOptions().setForcedColors(ForcedColors.ACTIVE));
            control.focus(); tab.keyboard().press("ArrowDown");
            assertThat(wrapper.locator("[data-active]")).hasCSS("outline-style", "solid");
            tab.keyboard().press("Escape"); tab.emulateMedia(new Page.EmulateMediaOptions().setForcedColors(ForcedColors.NONE));
        });
        } finally { settings.setBundle(dev.jbaby.shadwick.Bundle.STANDALONE); }
    }

    @ParameterizedTest @MethodSource("everySkinAndMode")
    void customSelectNestedOverlaysDismissOnlyOneLayer(String skin, String mode) {
        check("custom-select", skin, mode, tab -> {
            for (boolean fallback : new boolean[]{false,true}) {
                if (fallback) customSelectFallback(tab, skin, mode);
                tab.getByRole(AriaRole.BUTTON, new Page.GetByRoleOptions().setName("Open select dialog")).click();
                Locator dialog = tab.locator("#select-dialog"), control = customSelectControl(dialog.locator(".sw-select-wrapper"));
                control.focus(); tab.keyboard().press("ArrowDown"); tab.keyboard().press("Escape");
                assertThat(dialog).isVisible(); assertThat(control).isFocused();
                dialog.getByRole(AriaRole.BUTTON, new Locator.GetByRoleOptions().setName("Close dialog")).click();
                tab.getByRole(AriaRole.BUTTON, new Page.GetByRoleOptions().setName("Open select popover")).click();
                Locator popover = tab.locator("#select-popover"); control = customSelectControl(popover.locator(".sw-select-wrapper"));
                control.focus(); tab.keyboard().press("ArrowDown"); tab.keyboard().press("Escape");
                assertThat(popover).isVisible(); assertThat(control).isFocused();
                tab.keyboard().press("Escape"); assertThat(popover).not().isVisible();
            }
        });
    }

    @ParameterizedTest @MethodSource("everySkinAndMode")
    void customSelectWithoutEnhancementApisKeepsTheNativeField(String skin, String mode) {
        check("custom-select", skin, mode, tab -> {
            tab.addInitScript("const originalSupports=CSS.supports.bind(CSS); CSS.supports=(...args)=>args.join(' ').includes('base-select')?false:originalSupports(...args); HTMLElement.prototype.showPopover=undefined; HTMLElement.prototype.hidePopover=undefined;");
            tab.navigate(url("custom-select",skin,mode) + "&fresh=native-fallback");
            assertThat(tab.locator(".sw-select-wrapper[data-enhanced]")).hasCount(0);
            Locator form = tab.locator("form[aria-label='Normal workspace']");
            form.locator("select").selectOption("support");
            tab.keyboard().press("Escape");
            tab.evaluate("() => window.dispatchEvent(new PageTransitionEvent('pagehide',{persisted:true}))");
            form.getByRole(AriaRole.BUTTON, new Locator.GetByRoleOptions().setName("Save workspace")).click();
            assertThat(form.getByRole(AriaRole.STATUS).last()).hasText("Saved: Support");
        });
    }

    private Locator combo(Locator form,int index) { return form.locator(".sw-combobox").nth(index); }
    private Locator comboInput(Locator root) { return root.locator(".sw-combobox-input"); }
    private void chooseCombo(Locator root,String query) {
        Locator input=comboInput(root);input.fill(query);input.press("ArrowDown");input.press("Enter");
    }

    @ParameterizedTest @MethodSource("everySkinAndMode")
    void comboboxLocalFilteringExplicitChoiceAndEvents(String skin,String mode) {
        check("combobox",skin,mode,tab->{
            Locator root=combo(tab.locator("form[aria-label='Normal combobox']"),0),input=comboInput(root);
            root.locator("select").evaluate("s=>{window.comboEvents={input:0,change:0}; for(const k of ['input','change']) s.addEventListener(k,()=>window.comboEvents[k]++);}");
            input.fill("SAO");assertThat(root.locator("[role=option]:visible")).hasText("São Paulo");
            assertThat(root.locator("[data-active]")).hasCount(0);input.press("Enter");assertThat(root.locator("select")).hasValue("");
            input.press("ArrowDown");input.press("Enter");assertThat(root.locator("select")).hasValue("sao");assertThat(input).hasValue("São Paulo");
            assertThat(tab.evaluate("()=>window.comboEvents")).isEqualTo(java.util.Map.of("input",1,"change",1));
            input.fill("Tokyo");input.press("Escape");assertThat(input).hasValue("São Paulo");
            input.fill("bad");input.press("Tab");assertThat(input).hasValue("São Paulo");
            input.click();assertThat(root.getByRole(AriaRole.OPTION,new Locator.GetByRoleOptions().setName("São Paulo").setExact(true)).locator("svg")).hasCount(2);
            input.press("Escape");
        });
    }

    @ParameterizedTest @MethodSource("everySkinAndMode")
    void comboboxMultipleChipsAndKeyboardRemoval(String skin,String mode) {
        check("combobox",skin,mode,tab->{
            Locator root=combo(tab.locator("form[aria-label='Normal combobox']"),1),input=comboInput(root);
            input.fill("Design");input.press("Enter");input.fill("Support");input.press("Enter");
            assertThat(root.locator(".sw-combobox-chip-label")).hasText(new String[]{"Design","Support"});
            assertThat(input).hasAttribute("aria-expanded","true");assertThat(root.locator("[role=listbox]")).hasAttribute("aria-multiselectable","true");
            input.press("Backspace");assertThat(root.locator(".sw-combobox-chip-label")).hasText(new String[]{"Design"});
            input.fill("Engineering");input.press("Enter");input.press("Escape");input.press("ArrowLeft");
            Locator removes=root.locator(".sw-combobox-chip-remove");assertThat(removes.last()).isFocused();
            tab.keyboard().press("ArrowLeft");tab.keyboard().press("Delete");assertThat(root.locator(".sw-combobox-chip-label")).hasText(new String[]{"Engineering"});
            tab.keyboard().press("ArrowRight");assertThat(input).isFocused();
            root.getByRole(AriaRole.BUTTON,new Locator.GetByRoleOptions().setName("Clear selection")).click();assertThat(root.locator(".sw-combobox-chip")).hasCount(0);
        });
    }

    @ParameterizedTest @MethodSource("everySkinAndMode")
    void comboboxNormalSubmissionAndReset(String skin,String mode) {
        check("combobox",skin,mode,tab->{
            Locator form=tab.locator("form[aria-label='Normal combobox']");chooseCombo(combo(form,0),"turk");
            Locator input=comboInput(combo(form,1));input.fill("Engineering");input.press("Enter");input.press("Escape");
            form.getByRole(AriaRole.BUTTON,new Locator.GetByRoleOptions().setName("Reset")).click();
            assertThat(comboInput(combo(form,0))).hasValue("");assertThat(combo(form,1).locator(".sw-combobox-chip")).hasCount(0);
            chooseCombo(combo(form,0),"turk");input.fill("Design");input.press("Enter");input.press("Escape");
            form.getByRole(AriaRole.BUTTON,new Locator.GetByRoleOptions().setName("Save choices")).click();
            assertThat(form.getByRole(AriaRole.STATUS).last()).hasText("Saved: Türkiye; Design");
        });
    }

    @ParameterizedTest @MethodSource("everySkinAndMode")
    void comboboxAjaxFeedbackLiveValidationAndReplacement(String skin,String mode) {
        check("combobox",skin,mode,tab->{
            Locator form=tab.locator("form[aria-label='Ajax combobox']");
            form.getByRole(AriaRole.BUTTON,new Locator.GetByRoleOptions().setName("Save choices")).click();
            assertThat(comboInput(combo(form,0))).hasAttribute("aria-invalid","true");assertThat(comboInput(combo(form,1))).hasAttribute("aria-required","true");
            chooseCombo(combo(form,0),"Dublin");Locator teams=comboInput(combo(form,1));teams.fill("Support");teams.press("Enter");teams.press("Escape");
            form.getByRole(AriaRole.BUTTON,new Locator.GetByRoleOptions().setName("Save choices")).click();assertThat(form.getByRole(AriaRole.STATUS).last()).hasText("Saved: Dublin; Support");
            assertThat(comboInput(combo(form,0))).hasAttribute("aria-invalid","false");
            Locator live=tab.locator("form[aria-label='Live combobox']"),root=combo(live,0),input=comboInput(root);
            root.getByRole(AriaRole.BUTTON,new Locator.GetByRoleOptions().setName("Clear selection")).click();
            assertThat(input).hasAttribute("aria-invalid","true");assertThat(input).isFocused();
            chooseCombo(root,"Seattle");assertThat(live.getByRole(AriaRole.STATUS).last()).hasText("Current: Seattle");assertThat(input).hasAttribute("aria-invalid","false");
            live.getByRole(AriaRole.LINK,new Locator.GetByRoleOptions().setName("Refresh cities")).click();assertThat(live.getByRole(AriaRole.STATUS).last()).hasText("Current: none");
            assertThat(root.locator(".sw-combobox-input")).hasCount(1);input.click();assertThat(root.locator("[role=option]:visible")).hasCount(2);input.press("Escape");
        });
    }

    @ParameterizedTest @MethodSource("everySkinAndMode")
    void comboboxServerSearchPreservesChoicesAndResetDefaults(String skin,String mode) {
        check("combobox",skin,mode,tab->{
            Locator form=tab.locator("form[aria-label='Server combobox']"),one=combo(form,0),many=combo(form,1),input=comboInput(one);
            input.fill("sao");assertThat(one.locator("[data-active]")).hasText("São Paulo");assertThat(one.locator("select")).hasValue("berlin");
            assertThat(one.locator("select option[value=berlin]")).hasAttribute("hidden","");input.press("Enter");assertThat(input).hasValue("São Paulo");
            input.fill("zzzzz");assertThat(one.locator(".sw-combobox-empty")).hasText("No items found.");input.press("Escape");assertThat(input).hasValue("São Paulo");
            Locator more=comboInput(many);more.fill("tok");assertThat(many.locator("[data-active]")).hasText("Tokyo");more.press("Enter");
            assertThat(many.locator(".sw-combobox-chip-label")).hasText(new String[]{"Dublin","Tokyo"});more.press("Escape");
            form.getByRole(AriaRole.BUTTON,new Locator.GetByRoleOptions().setName("Save destinations")).click();assertThat(form.getByRole(AriaRole.STATUS).last()).hasText("Saved: São Paulo; [Dublin, Tokyo]");
            form.getByRole(AriaRole.BUTTON,new Locator.GetByRoleOptions().setName("Reset")).click();assertThat(input).hasValue("Berlin");assertThat(many.locator(".sw-combobox-chip-label")).hasText(new String[]{"Dublin"});
        });
    }

    @ParameterizedTest @MethodSource("everySkinAndMode")
    void comboboxRemoteNewerQueryWinsAndErrorsAreRecoverable(String skin,String mode) {
        check("combobox",skin,mode,new Browser.NewContextOptions(),tab->{
            Locator root=combo(tab.locator("form[aria-label='Server combobox']"),0),input=comboInput(root);
            tab.route("**/*",route->{
                var uri=java.net.URI.create(route.request().url());String query=uri.getRawQuery();
                if (query!=null&&query.contains("q=broken")) route.fulfill(new com.microsoft.playwright.Route.FulfillOptions().setStatus(500).setContentType("application/json").setBody("[]"));else route.resume();
            });
            input.fill("broken");assertThat(root.locator(".sw-combobox-empty")).hasText("The options could not be loaded.");assertThat(root.locator("select")).hasValue("berlin");
            input.fill("dub");assertThat(root.locator("[data-active]")).hasText("Dublin");input.press("Enter");assertThat(root.locator("select")).hasValue("dublin");
            // Cancel an in-flight fetch and a pending debounce before a newer search is displayed.
            input.fill("sao");input.fill("tok");assertThat(root.locator("[data-active]")).hasText("Tokyo");input.press("Enter");assertThat(root.locator("select")).hasValue("tokyo");
        }, List.of("Failed to load resource: the server responded with a status of 500 (Internal Server Error)"));
    }

    @ParameterizedTest @MethodSource("everySkinAndMode")
    void comboboxDynamicAriaFieldsetDisablingAndIme(String skin,String mode) {
        check("combobox",skin,mode,tab->{
            Locator root=combo(tab.locator("form[aria-label='Normal combobox']"),0),input=comboInput(root);
            root.locator("select").evaluate("s=>{s.setAttribute('aria-label','Explicit city');s.setAttribute('aria-describedby',s.getAttribute('aria-describedby'));}");assertThat(input).hasAttribute("aria-label","Explicit city");
            root.evaluate("r=>{const f=document.createElement('fieldset');r.parentNode.insertBefore(f,r);f.append(r);f.disabled=true;}");assertThat(input).isDisabled();
            root.evaluate("r=>r.parentElement.disabled=false");assertThat(input).isEnabled();root.locator("select").focus();assertThat(input).isFocused();
            input.evaluate("i=>{i.value='Tokyo';i.dispatchEvent(new InputEvent('input',{bubbles:true,isComposing:true}));}");assertThat(input).hasAttribute("aria-expanded","false");
            input.evaluate("i=>i.dispatchEvent(new CompositionEvent('compositionend',{bubbles:true}))");assertThat(root.locator("[role=option]:visible")).hasText("Tokyo");input.press("Escape");
            tab.evaluate("()=>{Shadwick.enhanceComboboxes();window.dispatchEvent(new PageTransitionEvent('pageshow',{persisted:true}));}");assertThat(root.locator(".sw-combobox-input")).hasCount(1);
            assertThat(tab.locator("[style]")).hasCount(0);
        });
    }

    @ParameterizedTest @MethodSource("everySkinAndMode")
    void comboboxNestedDialogAndPopoverDismissOneLayer(String skin,String mode) {
        check("combobox",skin,mode,tab->{
            tab.getByRole(AriaRole.BUTTON,new Page.GetByRoleOptions().setName("Open combobox dialog")).click();
            Locator dialog=tab.locator("#combobox-dialog"),input=dialog.locator(".sw-combobox-input");input.fill("Dublin");input.press("Escape");assertThat(dialog).isVisible();assertThat(input).isFocused();
            dialog.getByRole(AriaRole.BUTTON,new Locator.GetByRoleOptions().setName("Close dialog")).click();
            tab.getByRole(AriaRole.BUTTON,new Page.GetByRoleOptions().setName("Open combobox popover")).click();
            Locator popover=tab.locator("#combobox-popover");input=popover.locator(".sw-combobox-input");input.fill("Tokyo");input.press("Escape");assertThat(popover).isVisible();input.press("Escape");assertThat(popover).not().isVisible();
        });
    }

    @ParameterizedTest @MethodSource("everySkinAndMode")
    void comboboxWithoutScriptRetainsNativeSingleAndMultipleSubmission(String skin,String mode) {
        check("combobox",skin,mode,new Browser.NewContextOptions().setJavaScriptEnabled(false).setViewportSize(390,850),tab->{
            Locator form=tab.locator("form[aria-label='Normal combobox']");combo(form,0).locator("select").selectOption("dublin");combo(form,1).locator("select").selectOption(new String[]{"0","2"});
            form.getByRole(AriaRole.BUTTON,new Locator.GetByRoleOptions().setName("Save choices")).click();assertThat(form.getByRole(AriaRole.STATUS).last()).hasText("Saved: Dublin; Design, Support");assertThat(tab.locator(".sw-combobox-input")).hasCount(0);
        });
    }

    @ParameterizedTest @MethodSource("everySkinAndMode")
    void comboboxMobileEmbeddedForcedColorsAndReducedMotion(String skin,String mode) {
        var settings=dev.jbaby.shadwick.ShadwickSettings.get(wicketApp);settings.setBundle(dev.jbaby.shadwick.Bundle.EMBEDDED);
        try { check("combobox",skin,mode,new Browser.NewContextOptions().setViewportSize(320,850).setReducedMotion(com.microsoft.playwright.options.ReducedMotion.REDUCE),tab->{
            tab.locator("html").evaluate("n=>n.dir='rtl'");Locator root=combo(tab.locator("form[aria-label='Normal combobox']"),1),input=comboInput(root);
            input.fill("Engineering");input.press("Enter");input.press("Escape");assertThat(tab.evaluate("()=>document.documentElement.scrollWidth<=innerWidth+1")).isEqualTo(true);
            tab.emulateMedia(new Page.EmulateMediaOptions().setForcedColors(ForcedColors.ACTIVE));input.press("ArrowDown");assertThat(root.locator("[data-active]")).hasCSS("outline-style","solid");input.press("Escape");
            tab.emulateMedia(new Page.EmulateMediaOptions().setForcedColors(ForcedColors.NONE));
        }); } finally { settings.setBundle(dev.jbaby.shadwick.Bundle.STANDALONE); }
    }

    @ParameterizedTest @MethodSource("everySkinAndMode")
    void comboboxSlowSearchCannotReplaceNewResultsAndSearchEventsAddParameters(String skin,String mode) {
        check("combobox",skin,mode,tab->{
            var slow=new java.util.concurrent.atomic.AtomicReference<com.microsoft.playwright.Route>();
            var sawParameter=new java.util.concurrent.atomic.AtomicBoolean();
            tab.route("**/*",route->{String query=java.net.URI.create(route.request().url()).getRawQuery();
                if(query!=null&&query.contains("q=slow"))slow.set(route);
                else {if(query!=null&&query.contains("q=tok")&&query.contains("region=eu"))sawParameter.set(true);route.resume();}
            });
            tab.evaluate("()=>document.addEventListener('sw-combobox-search',e=>e.detail.url.searchParams.set('region','eu'))");
            Locator root=combo(tab.locator("form[aria-label='Server combobox']"),0),input=comboInput(root);
            input.fill("slow");tab.waitForCondition(()->slow.get()!=null);input.fill("tok");assertThat(root.locator("[data-active]")).hasText("Tokyo");
            slow.get().fulfill(new com.microsoft.playwright.Route.FulfillOptions().setContentType("application/json").setBody("[{\"id\":\"old\",\"text\":\"Old result\"}]"));
            input.press("Enter");assertThat(root.locator("select")).hasValue("tokyo");assertThat(root.locator("select option[value=old]")).hasCount(0);assertThat(sawParameter.get()).isTrue();
        });
    }

    @ParameterizedTest @MethodSource("everySkinAndMode")
    void comboboxWithoutPopoverApiLeavesNativeFieldsAccessible(String skin,String mode) {
        check("combobox",skin,mode,tab->{
            tab.addInitScript("HTMLElement.prototype.showPopover=undefined; HTMLElement.prototype.hidePopover=undefined;");
            tab.navigate(url("combobox",skin,mode)+"&fresh=native");
            assertThat(tab.locator(".sw-combobox-input")).hasCount(0);
            Locator form=tab.locator("form[aria-label='Normal combobox']");combo(form,0).locator("select").selectOption("dublin");combo(form,1).locator("select").selectOption("0");
            form.getByRole(AriaRole.BUTTON,new Locator.GetByRoleOptions().setName("Save choices")).click();assertThat(form.getByRole(AriaRole.STATUS).last()).hasText("Saved: Dublin; Design");
        });
    }

    private Locator calendarField(Locator scope,String label){
        return scope.locator(".sw-field:has(> .sw-field-label:text-is(\""+label+"\"))");
    }
    private Locator calendarDay(Locator root,String date){return root.locator(".sw-calendar-day-button[data-date='"+date+"']:not([data-outside])").first();}

    @ParameterizedTest @MethodSource("everySkinAndMode")
    void calendarKeyboardRovingFocusBoundsAndDisabledDates(String skin,String mode){
        check("calendar",skin,mode,tab->{
            Locator field=calendarField(tab.locator("main"),"Delivery day"),root=field.locator(".sw-calendar");
            Locator chosen=calendarDay(root,"2026-11-10");chosen.focus();tab.keyboard().press("ArrowRight");assertThat(calendarDay(root,"2026-11-11")).isFocused();
            tab.keyboard().press("ArrowDown");assertThat(calendarDay(root,"2026-11-18")).isFocused();tab.keyboard().press("Home");assertThat(calendarDay(root,"2026-11-16")).isFocused();
            tab.keyboard().press("End");assertThat(calendarDay(root,"2026-11-22")).isFocused();tab.keyboard().press("Enter");assertThat(root.locator("input[type=date]")).hasValue("2026-11-10");
            tab.keyboard().press("PageDown");assertThat(calendarDay(root,"2026-12-22")).isFocused();tab.keyboard().press("Enter");assertThat(root.locator("input[type=date]")).hasValue("2026-12-22");
            assertThat(root.locator(".sw-calendar-next")).isDisabled();assertThat(root.locator(".sw-calendar-day-button[tabindex='0']")).hasCount(1);
            assertThat(root.locator(".sw-calendar-week-number").first()).hasText("49");
        });
    }

    @ParameterizedTest @MethodSource("everySkinAndMode")
    void calendarRangeSwapsEndpointsAndWritesBothBeforeEvents(String skin,String mode){
        check("calendar",skin,mode,tab->{
            Locator field=calendarField(tab.locator("main"),"Planning window"),root=field.locator(".sw-calendar");
            root.evaluate("r=>{window.rangeEvents=[];for(const i of r.querySelectorAll('input[type=date]'))i.addEventListener('change',()=>window.rangeEvents.push([...r.querySelectorAll('input[type=date]')].map(n=>n.value)));}");
            calendarDay(root,"2026-11-20").click();assertThat(root.locator("input[type=date]").nth(0)).hasValue("2026-11-20");assertThat(root.locator("input[type=date]").nth(1)).hasValue("");
            calendarDay(root,"2026-11-16").click();assertThat(root.locator("input[type=date]").nth(0)).hasValue("2026-11-16");assertThat(root.locator("input[type=date]").nth(1)).hasValue("2026-11-20");
            assertThat(tab.evaluate("()=>window.rangeEvents.slice(-2).every(v=>v[0]==='2026-11-16'&&v[1]==='2026-11-20')")).isEqualTo(true);
            assertThat(root.locator(".sw-calendar-day[data-range-middle]:not([data-outside])")).hasCount(3);
        });
    }

    @ParameterizedTest @MethodSource("everySkinAndMode")
    void calendarPickerKeyboardPickCancelReopenAndRequiredDate(String skin,String mode){
        check("calendar",skin,mode,tab->{
            Locator form=tab.locator("form[aria-label='Normal booking']"),field=calendarField(form,"Departure"),trigger=field.locator(".sw-date-picker-trigger"),root=field.locator(".sw-calendar");
            trigger.focus();trigger.press("Enter");assertThat(calendarDay(root,"2026-11-10")).isFocused();tab.keyboard().press("ArrowRight");tab.keyboard().press("Enter");
            assertThat(field.locator(".sw-date-picker-content")).isHidden();assertThat(trigger).isFocused();assertThat(field.locator("input[type=date]")).hasValue("2026-11-11");assertThat(trigger).containsText("Nov 11, 2026");
            trigger.click();root.locator(".sw-calendar-next").click();tab.keyboard().press("Escape");assertThat(trigger).isFocused();trigger.click();assertThat(calendarDay(root,"2026-11-11")).isFocused();
            tab.keyboard().press("Enter");assertThat(field.locator("input[type=date]")).hasValue("2026-11-11");
        });
    }

    @ParameterizedTest @MethodSource("everySkinAndMode")
    void calendarRangePickerClosesOnlyOnCompletedRangeAndNormalSubmit(String skin,String mode){
        check("calendar",skin,mode,tab->{
            Locator form=tab.locator("form[aria-label='Normal booking']"),field=calendarField(form,"Stay"),root=field.locator(".sw-calendar"),trigger=field.locator(".sw-date-picker-trigger");trigger.click();
            calendarDay(root,"2026-11-16").click();assertThat(field.locator(".sw-date-picker-content")).isVisible();calendarDay(root,"2026-11-20").click();
            assertThat(field.locator(".sw-date-picker-content")).isHidden();assertThat(trigger).isFocused();assertThat(trigger).containsText("Nov 16, 2026");
            form.getByRole(AriaRole.BUTTON,new Locator.GetByRoleOptions().setName("Save booking")).click();assertThat(form.getByRole(AriaRole.STATUS).last()).hasText("Saved: 2026-11-10; 2026-11-16 – 2026-11-20");
        });
    }

    @ParameterizedTest @MethodSource("everySkinAndMode")
    void calendarAjaxValidationPreservesPickerAndNativeRangeFeedback(String skin,String mode){
        check("calendar",skin,mode,tab->{
            Locator form=tab.locator("form[aria-label='Ajax booking']"),day=calendarField(form,"Departure"),stay=calendarField(form,"Stay");
            day.locator("input[type=date]").evaluate("n=>{n.value='2026-11-14';n.dispatchEvent(new Event('change',{bubbles:true}));}");
            form.getByRole(AriaRole.BUTTON,new Locator.GetByRoleOptions().setName("Save booking")).click();assertThat(day.locator(".sw-date-picker-trigger")).hasAttribute("aria-invalid","true");
            day.locator(".sw-date-picker-trigger").click();calendarDay(day,"2026-11-16").click();
            form.getByRole(AriaRole.BUTTON,new Locator.GetByRoleOptions().setName("Save booking")).click();assertThat(day.locator(".sw-date-picker-trigger")).not().hasAttribute("aria-invalid","true");
            stay.locator("input[type=date]").nth(1).evaluate("n=>{n.value='';n.dispatchEvent(new Event('change',{bubbles:true}));}");
            form.getByRole(AriaRole.BUTTON,new Locator.GetByRoleOptions().setName("Save booking")).click();assertThat(stay.locator(".sw-date-picker-trigger")).hasAttribute("aria-invalid","true");
            assertThat(stay.locator("input[type=date]").nth(1)).hasAttribute("aria-invalid","true");stay.locator(".sw-date-picker-trigger").click();calendarDay(stay,"2026-11-12").click();
            form.getByRole(AriaRole.BUTTON,new Locator.GetByRoleOptions().setName("Save booking")).click();assertThat(form.getByRole(AriaRole.STATUS).last()).hasText("Saved: 2026-11-16; 2026-11-10 – 2026-11-12");
        });
    }

    @ParameterizedTest @MethodSource("everySkinAndMode")
    void calendarPickerResetProgrammaticValueAndLiveAjaxReplacement(String skin,String mode){
        check("calendar",skin,mode,tab->{
            Locator form=tab.locator("form[aria-label='Normal booking']"),day=calendarField(form,"Departure");day.locator(".sw-date-picker-trigger").click();calendarDay(day,"2026-11-16").click();
            form.getByRole(AriaRole.BUTTON,new Locator.GetByRoleOptions().setName("Reset")).click();assertThat(day.locator(".sw-date-picker-trigger")).containsText("Nov 10, 2026");
            Locator live=tab.locator("form[aria-label='Live calendar']"),field=calendarField(live,"Live appointment");field.locator(".sw-date-picker-trigger").click();calendarDay(field,"2026-11-16").click();
            assertThat(live.getByRole(AriaRole.STATUS).last()).hasText("Current: 2026-11-16");live.getByRole(AriaRole.LINK,new Locator.GetByRoleOptions().setName("Refresh picker")).click();
            field.evaluate("async f=>{const root=f.querySelector('.sw-calendar'),input=root.querySelector('input[type=date]');input.remove();await new Promise(resolve=>setTimeout(resolve,0));root.append(input);}");
            assertThat(field.locator(".sw-calendar-months")).hasCount(1);field.locator(".sw-date-picker-trigger").click();assertThat(calendarDay(field,"2026-11-16")).isFocused();tab.keyboard().press("Escape");
        });
    }

    @ParameterizedTest @MethodSource("everySkinAndMode")
    void calendarCaptionSelectsLeapDayAndLocaleWeekRules(String skin,String mode){
        check("calendar",skin,mode,tab->{
            Locator field=calendarField(tab.locator("main"),"Date of birth");field.locator(".sw-date-picker-trigger").click();
            assertThat(calendarDay(field,"1996-02-29")).isFocused();field.locator(".sw-calendar-dropdown[data-unit=year] select").selectOption("2000");
            assertThat(calendarDay(field,"2000-02-29")).isVisible();calendarDay(field,"2000-02-29").click();assertThat(field.locator("input[type=date]")).hasValue("2000-02-29");
            Locator inline=calendarField(tab.locator("main"),"Delivery day");assertThat(inline.locator(".sw-calendar-weekday").first()).hasText("Mo");assertThat(inline.locator(".sw-calendar-week-number").first()).hasText("44");
        });
    }

    @ParameterizedTest @MethodSource("everySkinAndMode")
    void calendarLabelFocusReadonlyDisabledAndDynamicFieldset(String skin,String mode){
        check("calendar",skin,mode,tab->{
            Locator inline=calendarField(tab.locator("main"),"Delivery day");inline.locator("label.sw-field-label").click();assertThat(calendarDay(inline,"2026-11-10")).isFocused();
            Locator read=calendarField(tab.locator("main"),"Reference date");calendarDay(read,"2026-11-10").click();assertThat(read.locator("input[type=date]")).hasValue("2026-11-05");
            assertThat(calendarField(tab.locator("main"),"Managed date").locator(".sw-calendar-day-button[tabindex='0']")).hasCount(0);
            Locator live=calendarField(tab.locator("form[aria-label='Live calendar']"),"Live appointment");live.evaluate("f=>{const fs=document.createElement('fieldset');f.parentNode.insertBefore(fs,f);fs.append(f);fs.disabled=true;}");assertThat(live.locator(".sw-date-picker-trigger")).isDisabled();
            live.evaluate("f=>f.parentElement.disabled=false");assertThat(live.locator(".sw-date-picker-trigger")).isEnabled();live.locator("label.sw-field-label").click();assertThat(live.locator(".sw-date-picker-trigger")).isFocused();
        });
    }

    @ParameterizedTest @MethodSource("everySkinAndMode")
    void calendarNestedOverlaysDismissPickerBeforeParent(String skin,String mode){
        check("calendar",skin,mode,tab->{
            tab.getByRole(AriaRole.BUTTON,new Page.GetByRoleOptions().setName("Open calendar dialog")).click();Locator dialog=tab.locator("#calendar-dialog");dialog.locator(".sw-date-picker-trigger").click();
            tab.keyboard().press("Escape");assertThat(dialog).isVisible();assertThat(dialog.locator(".sw-date-picker-trigger")).isFocused();dialog.getByRole(AriaRole.BUTTON,new Locator.GetByRoleOptions().setName("Close dialog")).click();
            tab.getByRole(AriaRole.BUTTON,new Page.GetByRoleOptions().setName("Open calendar popover")).click();Locator popup=tab.locator("#calendar-popover");popup.locator(".sw-date-picker-trigger").click();tab.keyboard().press("Escape");assertThat(popup).isVisible();tab.keyboard().press("Escape");assertThat(popup).isHidden();
        });
    }

    @ParameterizedTest @MethodSource("everySkinAndMode")
    void calendarWithoutScriptNativeInputsSubmitAndValidate(String skin,String mode){
        check("calendar",skin,mode,new Browser.NewContextOptions().setJavaScriptEnabled(false).setViewportSize(390,850),tab->{
            Locator form=tab.locator("form[aria-label='Normal booking']"),day=calendarField(form,"Departure");day.locator(".sw-date-picker-trigger").click();day.locator("input[type=date]").fill("2026-11-16");
            tab.locator("h1").click();form.getByRole(AriaRole.BUTTON,new Locator.GetByRoleOptions().setName("Save booking")).click();assertThat(form.getByRole(AriaRole.STATUS).last()).hasText("Saved: 2026-11-16; 2026-11-10 – 2026-11-12");assertThat(tab.locator(".sw-calendar-months")).hasCount(0);
        });
    }

    @ParameterizedTest @MethodSource("everySkinAndMode")
    void calendarUtcArithmeticSurvivesTimeZonesDstAndEarlyYears(String skin,String mode){
        check("calendar",skin,mode,new Browser.NewContextOptions().setTimezoneId("Pacific/Apia"),tab->{
            Locator field=calendarField(tab.locator("main"),"Date of birth");field.locator("input[type=date]").evaluate("n=>{n.value='2011-12-30';n.dispatchEvent(new Event('change',{bubbles:true}));}");
            field.locator(".sw-date-picker-trigger").click();assertThat(calendarDay(field,"2011-12-30")).hasAttribute("aria-label",java.util.regex.Pattern.compile(".*December 30, 2011"));calendarDay(field,"2011-12-30").click();
            assertThat(field.locator("input[type=date]")).hasValue(""); // optional selected date toggles off
            tab.keyboard().press("Escape");
            tab.evaluate("""
                () => {
                  const root=document.createElement('div');root.id='boundary-calendar';
                  root.className='sw-calendar';root.setAttribute('aria-label','Boundary calendar');
                  root.dataset.startMonth='0001-01';root.dataset.endMonth='9999-12';
                  root.innerHTML='<input type="date" aria-label="Boundary date" value="0001-01-01">';
                  document.querySelector('main').append(root);Shadwick.enhanceCalendars();
                }
                """);
            Locator boundary=tab.locator("#boundary-calendar");
            assertThat(boundary.locator("[data-date^='0000-']")).hasCount(0);
            calendarDay(boundary,"0001-01-01").focus();tab.keyboard().press("Home");assertThat(calendarDay(boundary,"0001-01-01")).isFocused();
            boundary.locator("input").evaluate("n=>{n.value='9999-12-31';n.dispatchEvent(new Event('change',{bubbles:true}));}");
            assertThat(calendarDay(boundary,"9999-12-31")).isVisible();assertThat(boundary.locator("[data-date^='10000-']")).hasCount(0);
            calendarDay(boundary,"9999-12-31").focus();tab.keyboard().press("End");assertThat(calendarDay(boundary,"9999-12-31")).isFocused();
            boundary.evaluate("n=>n.remove()");
        });
    }

    @ParameterizedTest @MethodSource("everySkinAndMode")
    void calendarMobileEmbeddedRtlForcedColorsAndReducedMotion(String skin,String mode){
        var settings=dev.jbaby.shadwick.ShadwickSettings.get(wicketApp);settings.setBundle(dev.jbaby.shadwick.Bundle.EMBEDDED);
        try{check("calendar",skin,mode,new Browser.NewContextOptions().setViewportSize(320,850).setReducedMotion(com.microsoft.playwright.options.ReducedMotion.REDUCE),tab->{
            tab.locator("html").evaluate("n=>n.dir='rtl'");Locator field=calendarField(tab.locator("main"),"Delivery day");calendarDay(field,"2026-11-10").focus();tab.keyboard().press("ArrowRight");assertThat(calendarDay(field,"2026-11-09")).isFocused();
            assertThat(tab.evaluate("()=>document.documentElement.scrollWidth<=innerWidth+1")).isEqualTo(true);tab.emulateMedia(new Page.EmulateMediaOptions().setForcedColors(ForcedColors.ACTIVE));assertThat(calendarDay(field,"2026-11-09")).hasCSS("outline-style","solid");
            tab.emulateMedia(new Page.EmulateMediaOptions().setForcedColors(ForcedColors.NONE));
        });}finally{settings.setBundle(dev.jbaby.shadwick.Bundle.STANDALONE);}
    }

    @ParameterizedTest @MethodSource("everySkinAndMode")
    void calendarLiveRangeValidationReadsCurrentPairAndRecovers(String skin,String mode){
        check("calendar",skin,mode,tab->{
            Locator form=tab.locator("form[aria-label='Live range']"),field=calendarField(form,"Live stay");field.locator(".sw-date-picker-trigger").click();
            calendarDay(field,"2026-11-16").click();assertThat(field.locator(".sw-date-picker-content")).isVisible();assertThat(field.locator(".sw-date-picker-trigger")).hasAttribute("aria-invalid","true");
            calendarDay(field,"2026-11-20").click();assertThat(form.getByRole(AriaRole.STATUS).last()).hasText("Current range: 2026-11-16 to 2026-11-20");
            assertThat(field.locator(".sw-date-picker-trigger")).not().hasAttribute("aria-invalid","true");
        });
    }

    @ParameterizedTest @MethodSource("everySkinAndMode")
    void calendarWithoutPopoverApiKeepsNativePickerFieldsVisible(String skin,String mode){
        check("calendar",skin,mode,tab->{
            tab.addInitScript("HTMLElement.prototype.showPopover=undefined;HTMLElement.prototype.hidePopover=undefined;");tab.navigate(url("calendar",skin,mode)+"&fresh=native");
            Locator form=tab.locator("form[aria-label='Normal booking']"),day=calendarField(form,"Departure");assertThat(day.locator("input[type=date]")).isVisible();
            day.locator("input[type=date]").fill("2026-11-16");form.getByRole(AriaRole.BUTTON,new Locator.GetByRoleOptions().setName("Save booking")).click();assertThat(form.getByRole(AriaRole.STATUS).last()).hasText("Saved: 2026-11-16; 2026-11-10 – 2026-11-12");
        });
    }

    @ParameterizedTest @MethodSource("everySkinAndMode")
    void monthViewBookmarkableLinksPreserveFiltersAndIndependentMonths(String skin,String mode){
        check("month-view",skin,mode,tab->{
            tab.navigate(url("month-view",skin,mode)+"&team=ops&month=2026-11&rota=2026-12");
            Locator agenda=tab.locator(".sw-month-view").first();
            assertThat(agenda.locator("caption")).hasText("November 2026");
            agenda.locator("[data-date='2026-11-10'] .sw-month-view-day-link").click();
            assertThat(tab.getByRole(AriaRole.STATUS).first()).hasText("Selected day: 2026-11-10");
            agenda.locator(".sw-month-view-nav-button").last().click();
            assertThat(agenda.locator("caption")).hasText("December 2026");
            org.assertj.core.api.Assertions.assertThat(tab.url()).contains("team=ops","rota=2026-12");
            assertThat(tab.locator(".sw-month-view").nth(1).locator("caption")).hasText("December 2026");
        });
    }
    @ParameterizedTest @MethodSource("everySkinAndMode")
    void monthViewAjaxRefreshRebuildsCustomDayContent(String skin,String mode){
        check("month-view",skin,mode,tab->{
            Locator live=tab.locator(".sw-month-view").last();assertThat(live.locator("caption")).hasText("November 2026");
            tab.getByText("Next month over Ajax",new Page.GetByTextOptions().setExact(true)).click();
            assertThat(live.locator("caption")).hasText("December 2026");
            assertThat(live.locator("[data-date='2026-12-10']")).containsText("Planning");
        });
    }
    @ParameterizedTest @MethodSource("everySkinAndMode")
    void monthViewWorksWithoutScriptAndInvalidQueryFallsBack(String skin,String mode){
        check("month-view",skin,mode,new Browser.NewContextOptions().setJavaScriptEnabled(false),tab->{
            tab.navigate(url("month-view",skin,mode)+"&month=invalid&team=ops");
            Locator view=tab.locator(".sw-month-view").first();assertThat(view.locator("caption")).hasText("November 2026");
            view.locator(".sw-month-view-nav-button").first().click();assertThat(view.locator("caption")).hasText("October 2026");
            org.assertj.core.api.Assertions.assertThat(tab.url()).contains("team=ops");
        });
    }
    @ParameterizedTest @MethodSource("everySkinAndMode")
    void monthViewYearLimitsAndNarrowRtlTouchLayout(String skin,String mode){
        check("month-view",skin,mode,new Browser.NewContextOptions().setViewportSize(320,850).setHasTouch(true),tab->{
            tab.locator("main").evaluate("n=>n.dir='rtl'");
            org.assertj.core.api.Assertions.assertThat(tab.evaluate("()=>document.documentElement.scrollWidth<=innerWidth+1")).isEqualTo(true);
            org.assertj.core.api.Assertions.assertThat(tab.locator(".sw-month-view-nav-button").first().evaluate("n=>parseFloat(getComputedStyle(n,'::after').width)>=44")).isEqualTo(true);
            tab.navigate(url("month-view",skin,mode)+"&month=0001-01");
            assertThat(tab.locator(".sw-month-view").first().locator(".sw-month-view-nav-button").first()).hasAttribute("aria-disabled","true");
            tab.navigate(url("month-view",skin,mode)+"&month=9999-12");
            assertThat(tab.locator(".sw-month-view").first().locator(".sw-month-view-nav-button").last()).hasAttribute("aria-disabled","true");
        });
    }
    @ParameterizedTest @MethodSource("everySkinAndMode")
    void chartKeyboardExploresExactValuesAndEscapeDismisses(String skin,String mode){
        check("charts",skin,mode,tab->{
            Locator chart=tab.locator(".sw-chart").first(),plot=chart.locator(".sw-chart-plot"),tip=chart.locator(".sw-chart-tooltip-content");
            plot.focus();assertThat(tip).containsText("January");assertThat(tip).containsText("186");
            tab.keyboard().press("ArrowRight");assertThat(tip).containsText("February");assertThat(tip).containsText("305");
            tab.keyboard().press("End");assertThat(tip).containsText("June");tab.keyboard().press("Home");assertThat(tip).containsText("January");
            tab.keyboard().press("Escape");assertThat(tip).isHidden();assertThat(plot).isFocused();
            tab.keyboard().press("ArrowRight");assertThat(tip).containsText("February");
        });
    }
    @ParameterizedTest @MethodSource("everySkinAndMode")
    void chartPointerLegendAndNativeDataDisclosureAgree(String skin,String mode){
        check("charts",skin,mode,tab->{
            Locator chart=tab.locator(".sw-chart").first();chart.locator(".sw-chart-hit[data-index='2']").hover();
            assertThat(chart.locator(".sw-chart-tooltip-content")).containsText("March");assertThat(chart.locator(".sw-chart-tooltip-content")).containsText("237");
            assertThat(chart.locator(".sw-chart-legend-item")).hasText(new String[]{"Desktop","Mobile"});
            chart.locator("summary").click();assertThat(chart.locator("table")).isVisible();assertThat(chart.locator("tbody tr").nth(2)).containsText("237");
        });
    }
    @ParameterizedTest @MethodSource("everySkinAndMode")
    void chartAjaxReplacementRemovesStaleState(String skin,String mode){
        check("charts",skin,mode,tab->{
            Locator chart=tab.locator(".sw-chart").filter(new Locator.FilterOptions().setHasText("Live report"));
            chart.locator(".sw-chart-plot").focus();assertThat(chart.locator(".sw-chart-tooltip-content")).containsText("January");
            tab.getByText("Refresh report",new Page.GetByTextOptions().setExact(true)).click();
            assertThat(chart.locator("tbody tr").first()).containsText("July");chart.locator(".sw-chart-plot").focus();assertThat(chart.locator(".sw-chart-tooltip-content")).containsText("July");
            chart.evaluate("n=>n.replaceWith(n.cloneNode(true))");
            tab.evaluate("()=>{Shadwick.enhanceCharts();Shadwick.enhanceCharts();}");chart.locator(".sw-chart-plot").focus();assertThat(chart.locator("[role=status]")).hasCount(1);
        });
    }
    @ParameterizedTest @MethodSource("everySkinAndMode")
    void chartNativeSvgAndDataRemainUsefulWithoutJavascript(String skin,String mode){
        check("charts",skin,mode,new Browser.NewContextOptions().setJavaScriptEnabled(false),tab->{
            Locator chart=tab.locator(".sw-chart").first();assertThat(chart.locator("svg")).isVisible();chart.locator("summary").click();assertThat(chart.locator("table")).isVisible();assertThat(chart.locator("tbody tr").first()).containsText("186");
        });
    }
    @ParameterizedTest @MethodSource("everySkinAndMode")
    void chartMobileEmbeddedRtlAndForcedColors(String skin,String mode){
        var settings=dev.jbaby.shadwick.ShadwickSettings.get(wicketApp);settings.setBundle(dev.jbaby.shadwick.Bundle.EMBEDDED);
        try{check("charts",skin,mode,new Browser.NewContextOptions().setViewportSize(320,850).setHasTouch(true).setReducedMotion(com.microsoft.playwright.options.ReducedMotion.REDUCE),tab->{
            tab.locator("main").evaluate("n=>n.dir='rtl'");Locator chart=tab.locator(".sw-chart").first();assertThat(chart.locator("svg")).hasAttribute("data-rtl","true");
            chart.locator(".sw-chart-hit[data-index='2']").tap();assertThat(chart.locator(".sw-chart-tooltip-content")).containsText("March");
            tab.keyboard().press("Home");tab.keyboard().press("ArrowLeft");assertThat(chart.locator(".sw-chart-tooltip-content")).containsText("February");
            org.assertj.core.api.Assertions.assertThat(tab.evaluate("()=>document.documentElement.scrollWidth<=innerWidth+1")).isEqualTo(true);
            tab.emulateMedia(new Page.EmulateMediaOptions().setForcedColors(ForcedColors.ACTIVE));assertThat(chart.locator(".sw-chart-plot")).hasCSS("outline-style","solid");tab.emulateMedia(new Page.EmulateMediaOptions().setForcedColors(ForcedColors.NONE));
        });}finally{settings.setBundle(dev.jbaby.shadwick.Bundle.STANDALONE);}
    }
    @ParameterizedTest @MethodSource("everySkinAndMode")
    void chartEscapeDismissesTooltipBeforeItsNativeDialog(String skin,String mode){
        check("charts",skin,mode,tab->{
            tab.evaluate("()=>{const d=document.createElement('dialog');d.id='chart-dialog';d.setAttribute('aria-label','Chart dialog');d.append(document.querySelector('.sw-chart'));document.body.append(d);d.showModal();}");
            tab.locator("#chart-dialog .sw-chart-plot").focus();tab.keyboard().press("Escape");assertThat(tab.locator("#chart-dialog")).isVisible();assertThat(tab.locator("#chart-dialog .sw-chart-tooltip-content")).isHidden();
            tab.keyboard().press("Escape");assertThat(tab.locator("#chart-dialog")).isHidden();
            tab.evaluate("()=>{const d=document.querySelector('#chart-dialog');document.querySelector('main').append(d.firstElementChild);d.remove();}");
        });
    }

    @ParameterizedTest @MethodSource("everySkinAndMode")
    void skinControlsUseTheirOwnGeometry(String skin,String mode){
        check("buttons",skin,mode,tab->{
            Locator sizes=tab.locator("section[aria-labelledby='sizes-heading']");
            int[] heights = switch (skin) {
                case "vega", "maia", "luma", "sketch" -> new int[]{36,32,40};
                case "mira" -> new int[]{28,24,32};
                case "sera" -> new int[]{40,36,44};
                default -> new int[]{32,28,36};
            };
            String radius = switch (skin) {
                case "vega", "mira" -> "8px";
                case "nova" -> "10px";
                case "maia", "luma" -> "32px";
                case "rhea" -> "16px";
                case "sketch" -> "255px 15px";
                default -> "0px";
            };
            assertThat(sizes.getByRole(AriaRole.BUTTON,new Locator.GetByRoleOptions().setName("Default").setExact(true))).hasCSS("height",heights[0]+"px");
            assertThat(sizes.getByRole(AriaRole.BUTTON,new Locator.GetByRoleOptions().setName("Default").setExact(true))).hasCSS("border-top-left-radius",radius);
            assertThat(sizes.getByRole(AriaRole.BUTTON,new Locator.GetByRoleOptions().setName("Small").setExact(true))).hasCSS("height",heights[1]+"px");
            assertThat(sizes.getByRole(AriaRole.BUTTON,new Locator.GetByRoleOptions().setName("Large").setExact(true))).hasCSS("height",heights[2]+"px");
        });
    }
    @ParameterizedTest @MethodSource("everySkinAndMode")
    void skinCardFooterMatchesItsIdentity(String skin,String mode){
        check("display",skin,mode,tab->{
            Locator card=tab.locator(".sw-card").first(),footer=card.locator(".sw-card-footer");
            assertThat(card).hasCSS("padding-top",switch(skin){case "vega","maia","luma","sketch"->"24px";case "rhea"->"20px";case "sera"->"32px";default->"16px";});
            if("nova".equals(skin)){
                assertThat(card).hasCSS("padding-bottom","0px");assertThat(footer).hasCSS("padding-bottom","16px");assertThat(footer).hasCSS("border-top-width","1px");
                org.assertj.core.api.Assertions.assertThat(footer.evaluate("n=>getComputedStyle(n).backgroundColor!=='rgba(0, 0, 0, 0)'" )).isEqualTo(true);
            }
        });
    }
    @ParameterizedTest @MethodSource("everySkinAndMode")
    void skinFieldsPreserveReadonlyDisabledAndMobileType(String skin,String mode){
        check("inputs",skin,mode,new Browser.NewContextOptions().setViewportSize(390,850),tab->{
            Locator input=tab.locator("input[type=email]").first();
            assertThat(input).hasCSS("font-size",switch(skin){case "lyra"->"12px";case "mira"->"14px";default->"16px";});assertThat(input).hasCSS("height",switch(skin){case "vega","maia","luma","sketch"->"36px";case "mira"->"28px";case "sera"->"40px";default->"32px";});
            if("nova".equals(skin)){
                assertThat(input).hasCSS("box-shadow","none");input.evaluate("n=>n.disabled=true");
                org.assertj.core.api.Assertions.assertThat(input.evaluate("n=>getComputedStyle(n).backgroundColor!=='rgba(0, 0, 0, 0)'" )).isEqualTo(true);
                input.evaluate("n=>{n.disabled=false;n.readOnly=true;}");input.focus();assertThat(input).isFocused();
            }
        });
    }

}
