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

@Tag("browser")
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class CatalogBrowserTest {
    static Playwright playwright;
    static Browser chromium, firefox;
    static final Path SHOTS = Path.of("target/catalog-screenshots");
    @LocalServerPort int port;
    @Autowired WebApplication app;
    @BeforeAll static void start() throws Exception {
        Files.createDirectories(SHOTS); playwright = Playwright.create();
        chromium = playwright.chromium().launch(); firefox = playwright.firefox().launch();
    }
    @AfterAll static void stop() { playwright.close(); }
    @BeforeEach void markup() { app.getMarkupSettings().setStripWicketTags(true); }
    String url(String path) { return "http://localhost:" + port + "/" + path; }
    Browser browser(String engine) { return engine.equals("chromium") ? chromium : firefox; }
    void audit(Page page) {
        assertThat(page.locator("h1")).hasCount(1);
        assertThat(page.evaluate("document.documentElement.scrollWidth <= innerWidth + 1")).as("page overflow at " + page.url()).isEqualTo(true);
        assertThat(new AxeBuilder(page).withTags(List.of("wcag2a", "wcag2aa", "wcag21a", "wcag21aa", "wcag22aa")).analyze().getViolations())
                .as("accessibility at " + page.url()).isEmpty();
    }
    static Stream<Arguments> skins() {
        return Arrays.stream(Skin.values()).flatMap(skin -> Stream.of(Arguments.of(skin.id(), "light", 1440), Arguments.of(skin.id(), "dark", 390)));
    }
    @ParameterizedTest @MethodSource("skins")
    void catalogAndCodeFitInEverySkin(String skin, String mode, int width) {
        try (var context = chromium.newContext(new Browser.NewContextOptions().setViewportSize(width, 1000).setReducedMotion(ReducedMotion.REDUCE)); var page = context.newPage()) {
            List<String> errors = new ArrayList<>(); page.onPageError(errors::add);
            page.onConsoleMessage(message -> { if (message.type().equals("error")) errors.add(message.text()); });
            for (String path : List.of("components", "buttons")) {
                page.navigate(url(path) + "?skin=" + skin + "&mode=" + mode);
                audit(page);
                page.screenshot(new Page.ScreenshotOptions().setPath(SHOTS.resolve(skin + "-" + mode + "-" + width + "-" + path + ".png")).setFullPage(true));
                if (path.equals("buttons")) {
                    page.locator(".ex-source-file summary").nth(1).click(); audit(page);
                    page.locator(".ex-source").screenshot(new Locator.ScreenshotOptions().setPath(SHOTS.resolve(skin + "-" + mode + "-" + width + "-source.png")));
                    page.locator(".ex-preferences > summary").click(); audit(page);
                }
            }
            assertThat(errors).isEmpty();
        }
    }
    @ParameterizedTest @ValueSource(strings={"chromium", "firefox"})
    void studioRootCatalogSearchAndCodeToolsWorkTogether(String engine) {
        try (var context = browser(engine).newContext(new Browser.NewContextOptions().setViewportSize(1440, 1000)); var page = context.newPage()) {
            Response root = page.navigate(url(""));
            assertThat(root.status()).isEqualTo(200); assertThat(root.request().redirectedFrom()).isNull();
            assertThat(page.locator("h1")).hasText("A good day to create.");
            page.locator(".studio-footer a").click();
            assertThat(page.url()).contains("/components");
            assertThat(page.locator("#catalog-query")).isVisible();
            page.keyboard().press("Control+k");
            assertThat(page.locator("#catalog-query")).isFocused();
            page.locator("#catalog-query").fill("calendar");
            var navigation = page.getByRole(AriaRole.NAVIGATION, new Page.GetByRoleOptions().setName("Components").setExact(true));
            assertThat(navigation.locator("li:visible")).hasCount(1);
            navigation.getByRole(AriaRole.LINK, new Locator.GetByRoleOptions().setName("Calendar & date picker").setExact(true)).click();
            assertThat(navigation.locator("[aria-current=page]")).hasText("Calendar & date picker");
            page.locator("#catalog-query").fill("no-such-control");
            assertThat(page.locator(".ex-catalog-no-results")).isVisible();
            page.locator("#catalog-query").press("Escape");
            assertThat(navigation.locator("li:visible")).hasCount(CatalogPages.ALL.size());
            page.evaluate("Object.defineProperty(navigator, 'clipboard', {configurable:true,value:{writeText:async text => {window.copiedExample=text;}}})");
            page.getByRole(AriaRole.BUTTON, new Page.GetByRoleOptions().setName("Copy code: CalendarPage.java").setExact(true)).click();
            page.waitForFunction("() => window.copiedExample && window.copiedExample.includes('class CalendarPage')");
            assertThat(page.evaluate("window.copiedExample")).isEqualTo(ExampleSources.read("CalendarPage.java"));
            page.locator(".ex-source-file summary").filter(new Locator.FilterOptions().setHasText("CalendarPage.html")).click();
            assertThat(page.locator(".ex-source-file").nth(1).locator("code")).containsText("<wicket:extend");
            Response alias = page.navigate(url("studio?view=projects&q=Sunday"));
            assertThat(alias.status()).isEqualTo(200); assertThat(alias.request().redirectedFrom()).isNull();
            assertThat(page.locator(".studio-project-card")).hasCount(1);
        }
    }
    @ParameterizedTest @ValueSource(strings={"chromium", "firefox"})
    void mobileNavigationClipboardFallbackAndPreferencesRemainUsable(String engine) {
        try (var context = browser(engine).newContext(new Browser.NewContextOptions().setViewportSize(320, 850)); var page = context.newPage()) {
            page.navigate(url("buttons?skin=nova&mode=dark&lang=de"));
            assertThat(page.locator("#catalog-query")).not().isVisible();
            page.locator(".ex-catalog-menu > summary").click();
            page.locator("#catalog-query").fill("combobox");
            page.getByRole(AriaRole.NAVIGATION, new Page.GetByRoleOptions().setName("Components").setExact(true))
                    .getByRole(AriaRole.LINK, new Locator.GetByRoleOptions().setName("Combobox").setExact(true)).click();
            assertThat(page.url()).contains("skin=nova", "mode=dark", "lang=de");
            assertThat(page.locator("html")).hasClass("dark");
            page.evaluate("Object.defineProperty(navigator, 'clipboard', {configurable:true,value:{writeText:async () => {throw Error('Clipboard unavailable');}}})");
            page.getByRole(AriaRole.BUTTON, new Page.GetByRoleOptions().setName("Copy code: ComboboxPage.java").setExact(true)).click();
            assertThat(page.locator(".ex-copy-status")).containsText("Code selected");
            assertThat(page.evaluate("getSelection().toString()")).isEqualTo(ExampleSources.read("ComboboxPage.java"));
            page.locator(".ex-preferences > summary").click();
            page.locator("#showcase-language").selectOption("en");
            page.getByRole(AriaRole.BUTTON, new Page.GetByRoleOptions().setName("Anwenden").setExact(true)).click();
            assertThat(page.locator("html")).hasAttribute("lang", "en"); audit(page);
        }
    }
    @ParameterizedTest @ValueSource(strings={"chromium", "firefox"})
    void sourcesAndNavigationWorkWithoutJavascript(String engine) {
        try (var context = browser(engine).newContext(new Browser.NewContextOptions().setViewportSize(390, 900).setJavaScriptEnabled(false)); var page = context.newPage()) {
            page.navigate(url("buttons"));
            assertThat(page.locator(".ex-source-file code").first()).containsText("class ButtonPage");
            assertThat(page.locator("[data-source-copy]").first()).not().isVisible();
            page.locator(".ex-source-file summary").nth(1).click();
            assertThat(page.locator(".ex-source-file code").nth(1)).isVisible();
            page.getByRole(AriaRole.NAVIGATION, new Page.GetByRoleOptions().setName("Components").setExact(true))
                    .getByRole(AriaRole.LINK, new Locator.GetByRoleOptions().setName("Dialogs").setExact(true)).click();
            assertThat(page.locator(".ex-source-file code").first()).containsText("class DialogPage");
            assertThat(page.evaluate("document.documentElement.scrollWidth <= innerWidth + 1")).isEqualTo(true);
        }
    }
    @Test void everyCatalogRouteLoadsItsOwnSourceInTheBrowser() {
        try (var context = chromium.newContext(); var page = context.newPage()) {
            List<String> errors = new ArrayList<>(); page.onPageError(errors::add);
            for (var example : CatalogPages.ALL) {
                assertThat(page.navigate(url(example.route())).status()).isEqualTo(200);
                assertThat(page.locator(".ex-source-file code").first()).containsText("class " + example.page().getSimpleName());
                assertThat(page.locator(".ex-source-heading > a")).hasAttribute("href", CatalogPages.guide(example.page()));
            }
            assertThat(errors).isEmpty();
        }
    }
}
