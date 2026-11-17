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
import java.util.regex.Pattern;
import java.util.stream.Stream;

import static com.microsoft.playwright.assertions.PlaywrightAssertions.assertThat;
import static org.assertj.core.api.Assertions.assertThat;

/** The six layout recipes in every skin: desktop navigation and collapse, the phone panel, their menus and links. */
@Tag("browser")
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class LayoutRecipeBrowserTest {
    static Playwright playwright;
    static Browser chromium, firefox;
    static final List<String> RECIPES = List.of("collapses-to-icons", "grouped-by-section", "collapsible-sub-menus",
            "secondary-navigation", "table-of-contents", "header-navigation");
    @LocalServerPort int port;
    @Autowired WebApplication app;

    @BeforeAll static void start() throws Exception {
        Files.createDirectories(Path.of("target/recipe-screenshots"));
        playwright = Playwright.create();
        chromium = playwright.chromium().launch();
        firefox = playwright.firefox().launch();
    }
    @AfterAll static void stop() { playwright.close(); }
    @BeforeEach void markup() { app.getMarkupSettings().setStripWicketTags(true); }
    @AfterEach void restore() { app.getMarkupSettings().setStripWicketTags(false); }

    static Stream<Arguments> cases() {
        List<String> selected = Arrays.asList(System.getProperty("shadwick.browser.skins", "vega,lyra,nova,maia,mira,luma,rhea,sera,sketch").split(","));
        return Stream.of("chromium", "firefox").flatMap(engine -> Arrays.stream(Skin.values())
                .map(skin -> skin.name().toLowerCase(Locale.ROOT)).filter(selected::contains)
                .flatMap(skin -> RECIPES.stream().flatMap(recipe -> Stream.of(
                        Arguments.of(engine, skin, recipe, "light", 1440), Arguments.of(engine, skin, recipe, "dark", 320)))));
    }

    String url(String path) { return "http://localhost:" + port + "/" + path; }

    void audit(Page p) {
        assertThat(p.locator("[style]")).hasCount(0);
        assertThat(p.evaluate("document.documentElement.scrollWidth <= innerWidth + 1")).as("No horizontal overflow").isEqualTo(true);
        assertThat(new AxeBuilder(p).withTags(List.of("wcag2a", "wcag2aa", "wcag21a", "wcag21aa", "wcag22aa")).analyze().getViolations()).isEmpty();
    }

    void settle(Page p) {
        p.waitForFunction("() => document.getAnimations().every(a => a.playState !== 'running' || a.effect?.getTiming().iterations === Infinity)");
    }

    @ParameterizedTest(name = "{0} {1} {2} {3}") @MethodSource("cases")
    void eachRecipeNavigatesCollapsesAndOpensItsPanelInEverySkin(String engine, String skin, String recipe, String mode, int width) {
        var options = new Browser.NewContextOptions().setViewportSize(width, 900)
                .setColorScheme(mode.equals("dark") ? ColorScheme.DARK : ColorScheme.LIGHT);
        try (var context = (engine.equals("chromium") ? chromium : firefox).newContext(options); var p = context.newPage()) {
            // The visitor's saved theme choice, which the theme script applies before the page is painted
            context.addInitScript("try { localStorage.setItem('shadwick-theme', '" + mode + "'); } catch (e) {}");
            var errors = new ArrayList<String>();
            p.onPageError(errors::add);
            p.onConsoleMessage(m -> { if (m.type().equals("error")) errors.add(m.text()); });
            // The catalog keeps the chosen skin in the session; the recipe's own code knows nothing of it
            p.navigate(url("components?skin=" + skin));
            assertThat(p.navigate(url("recipes/" + recipe)).status()).isEqualTo(200);
            assertThat(p.locator("html")).hasClass(Pattern.compile(mode.equals("dark") ? "\\bdark\\b" : "^(?!.*\\bdark\\b).*$"));
            Locator nav = p.locator("nav.sw-sidebar"), trigger = p.locator(".sw-sidebar-trigger");
            assertThat(p.locator("main")).hasCount(1);
            assertThat(p.locator("a.sw-skip-link")).hasAttribute("href", "#" + p.locator("main").getAttribute("id"));
            settle(p);
            audit(p);

            if (width >= 1000) {
                assertThat(nav).isVisible();
                if (!recipe.equals("table-of-contents")) assertThat(nav.locator("a[aria-current=page]")).hasCount(1);
                if (recipe.equals("header-navigation")) {
                    // The pages are a row in the header; the trigger is only for phones
                    assertThat(nav.getByRole(AriaRole.LINK, new Locator.GetByRoleOptions().setName("Projects"))).isVisible();
                    assertThat(trigger).isHidden();
                } else {
                    trigger.click();
                    assertThat(p.locator(".sw-sidebar-provider")).hasAttribute("data-state", "collapsed");
                    if (recipe.equals("collapses-to-icons"))
                        p.waitForFunction("() => document.querySelector('nav.sw-sidebar').getBoundingClientRect().width < 80");
                    trigger.click();
                    assertThat(p.locator(".sw-sidebar-provider")).hasAttribute("data-state", "expanded");
                }
                switch (recipe) {
                    case "collapses-to-icons" -> openMenu(p, nav.locator(".sw-sidebar-header button"), "Acme Studio");
                    case "grouped-by-section" -> openMenu(p, nav.locator(".sw-sidebar-header button"), "v1.4");
                    case "table-of-contents" -> {
                        nav.getByRole(AriaRole.LINK, new Locator.GetByRoleOptions().setName("Picking trees")).click();
                        assertThat(p.locator("#trees")).isFocused();
                    }
                    default -> { }
                }
            } else {
                // A phone: the same navigation opens as a panel, keeps the focus, and Escape returns it
                assertThat(nav).isHidden();
                trigger.click();
                assertThat(nav).isVisible();
                settle(p);
                assertThat(p.evaluate("() => document.querySelector('nav.sw-sidebar').contains(document.activeElement)")).isEqualTo(true);
                p.keyboard().press("Escape");
                assertThat(nav).isHidden();
                assertThat(trigger).isFocused();
            }
            p.screenshot(new Page.ScreenshotOptions().setPath(Path.of("target/recipe-screenshots", recipe + "-" + engine + "-" + skin + "-" + mode + ".png")));
            assertThat(errors).isEmpty();
        }
    }

    private void openMenu(Page p, Locator trigger, String item) {
        trigger.click();
        Locator entry = p.getByRole(AriaRole.MENUITEMRADIO, new Page.GetByRoleOptions().setName(item));
        assertThat(entry).isVisible();
        p.keyboard().press("Escape");
        assertThat(entry).isHidden();
        assertThat(trigger).isFocused();
    }

    @ParameterizedTest @ValueSource(strings = {"collapses-to-icons", "grouped-by-section", "collapsible-sub-menus",
            "secondary-navigation", "table-of-contents", "header-navigation"})
    void withoutJavaScriptEveryRecipeStillNavigates(String recipe) {
        try (var context = chromium.newContext(new Browser.NewContextOptions().setJavaScriptEnabled(false).setViewportSize(1280, 900));
             var p = context.newPage()) {
            p.navigate(url("recipes/" + recipe));
            Locator links = p.locator("nav.sw-sidebar .sw-sidebar-content a[href]");
            assertThat(links.first()).isVisible();
            if (recipe.equals("table-of-contents")) {
                links.first().click();
                assertThat(p).hasURL(Pattern.compile("#site$"));
            } else {
                Locator other = p.locator("nav.sw-sidebar .sw-sidebar-content a[href]:visible:not([aria-current])").first();
                String label = other.innerText().trim();
                other.click();
                assertThat(p.locator("nav.sw-sidebar .sw-sidebar-content a[aria-current=page]")).containsText(label);
            }
        }
    }
}
