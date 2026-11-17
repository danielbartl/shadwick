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

/** The load-more feed and the customer CRUD on /collections: batches, focus, the editor's errors, without script. */
@Tag("browser")
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class CollectionsBrowserTest {
    static Playwright playwright;
    static Browser chromium, firefox;
    @LocalServerPort int port;
    @Autowired WebApplication app;

    @BeforeAll static void start() throws Exception {
        Files.createDirectories(Path.of("target/collections-screenshots"));
        playwright = Playwright.create();
        chromium = playwright.chromium().launch();
        firefox = playwright.firefox().launch();
    }
    @AfterAll static void stop() { playwright.close(); }
    @BeforeEach void markup() { app.getMarkupSettings().setStripWicketTags(true); }
    @AfterEach void restore() { app.getMarkupSettings().setStripWicketTags(false); }

    static Stream<Arguments> skins() {
        List<String> selected = Arrays.asList(System.getProperty("shadwick.browser.skins", "vega,lyra,nova,maia,mira,luma,rhea,sera,sketch").split(","));
        return Stream.of("chromium", "firefox").flatMap(engine -> Arrays.stream(Skin.values())
                .map(skin -> skin.name().toLowerCase(Locale.ROOT)).filter(selected::contains)
                .flatMap(skin -> Stream.of(Arguments.of(engine, skin, "light", 1440), Arguments.of(engine, skin, "dark", 320))));
    }

    Browser browser(String engine) { return engine.equals("chromium") ? chromium : firefox; }
    String url(String skin, String mode) { return "http://localhost:" + port + "/collections?skin=" + skin + "&mode=" + mode; }
    void idle(Page p) { p.waitForCondition(() -> (Boolean) p.evaluate("Object.keys(Wicket.channelManager.channels).length===0")); }
    void settle(Page p) {
        p.waitForCondition(() -> (Boolean) p.evaluate("document.getAnimations().every(a => a.playState !== 'running' || !Number.isFinite(a.effect?.getComputedTiming().endTime))"));
    }
    Object focused(Page p, String script) { return p.evaluate("() => { const e = document.activeElement; return " + script + "; }"); }
    void audit(Page p) {
        assertThat(p.locator("[style]")).hasCount(0);
        assertThat(p.evaluate("document.documentElement.scrollWidth <= innerWidth + 1")).as("No horizontal overflow").isEqualTo(true);
        assertThat(new AxeBuilder(p).withTags(List.of("wcag2a", "wcag2aa", "wcag21a", "wcag21aa", "wcag22aa")).analyze().getViolations()).isEmpty();
    }
    List<String> errors(Page p) {
        var errors = new ArrayList<String>();
        p.onPageError(errors::add);
        p.onConsoleMessage(m -> { if (m.type().equals("error")) errors.add(m.text()); });
        return errors;
    }
    Locator button(Locator scope, String name) {
        return scope.getByRole(AriaRole.BUTTON, new Locator.GetByRoleOptions().setName(name).setExact(true));
    }

    @ParameterizedTest @MethodSource("skins")
    void theFeedLoadsABatchAtATimeAndMovesTheFocusToIt(String engine, String skin, String mode, int width) {
        try (var context = browser(engine).newContext(new Browser.NewContextOptions().setViewportSize(width, 1000)); var p = context.newPage()) {
            var errors = errors(p);
            p.navigate(url(skin, mode));
            Locator feed = p.locator("section[aria-labelledby=feed-heading]"), items = feed.locator("[role=listitem]");
            Locator more = feed.getByRole(AriaRole.LINK, new Locator.GetByRoleOptions().setName("Load more"));
            assertThat(items).hasCount(20);
            assertThat(feed.locator(".sw-load-more-count")).hasText("20 of 134");
            assertThat(feed.getByRole(AriaRole.LIST, new Locator.GetByRoleOptions().setName("Recent activity"))).isVisible();
            audit(p);

            // With the keyboard: the focus lands on the first new event, and screen readers hear the count
            more.focus();
            p.keyboard().press("Enter");
            idle(p);
            assertThat(items).hasCount(40);
            assertThat(items.nth(20)).isFocused();
            assertThat(items.nth(20)).containsText(CollectionsPage.EVENTS.get(20).text());
            assertThat(feed.locator(".sw-load-more-status")).hasText("Showing 40 of 134");
            assertThat(feed.locator(".sw-load-more-count")).hasText("40 of 134");

            // The last batch has 14; then the button goes
            for (int shown = 40; shown < 134; shown += 20) {
                more.click();
                idle(p);
            }
            assertThat(items).hasCount(134);
            assertThat(more).hasCount(0);
            audit(p);
            // Last: a screenshot leaves an empty style attribute on inputs (it hides their carets)
            p.screenshot(new Page.ScreenshotOptions().setPath(Path.of("target/collections-screenshots", "feed-" + engine + "-" + skin + "-" + mode + ".png")));
            assertThat(errors).isEmpty();
        }
    }

    @ParameterizedTest @ValueSource(strings = {"chromium", "firefox"})
    void loadingAsYouScrollLeavesTheFocusWhereItIs(String engine) {
        try (var context = browser(engine).newContext(new Browser.NewContextOptions().setViewportSize(1280, 800)); var p = context.newPage()) {
            var errors = errors(p);
            p.navigate(url("vega", "light"));
            Locator feed = p.locator("section[aria-labelledby=feed-heading]"), items = feed.locator("[role=listitem]");
            Locator toggle = feed.getByRole(AriaRole.SWITCH, new Locator.GetByRoleOptions().setName("Load as you scroll"));
            toggle.check();
            idle(p);
            assertThat(feed.locator("[data-sw-load-on-scroll]")).hasCount(1);
            toggle.focus();
            feed.locator(".sw-load-more").scrollIntoViewIfNeeded();
            p.waitForCondition(() -> items.count() >= 40);
            idle(p);
            assertThat(toggle).isFocused();
            assertThat(errors).isEmpty();
        }
    }

    @ParameterizedTest @MethodSource("skins")
    void theCustomersAreCreatedEditedAndDeletedInDialogs(String engine, String skin, String mode, int width) {
        try (var context = browser(engine).newContext(new Browser.NewContextOptions().setViewportSize(width, 1000)); var p = context.newPage()) {
            var errors = errors(p);
            p.navigate(url(skin, mode));
            Locator section = p.locator("section[aria-labelledby=customers-heading]"), rows = section.locator("tbody tr");
            Locator status = section.locator(".sw-crud-status");
            assertThat(rows).hasCount(5);

            // New: a required field, then a name the server refuses, both shown at the field with the dialog open
            button(section, "New").click();
            idle(p);
            Locator editor = p.getByRole(AriaRole.DIALOG, new Page.GetByRoleOptions().setName("New entry"));
            assertThat(editor).isVisible();
            settle(p);
            button(editor, "Save").click();
            idle(p);
            assertThat(editor.locator(".sw-field-error").first()).containsText("is required");
            assertThat(editor.getByLabel("Name")).isFocused();
            editor.getByLabel("Name").fill("Globex");
            button(editor, "Save").click();
            idle(p);
            assertThat(editor.locator(".sw-field-error").first()).hasText("There is already a customer called Globex.");
            assertThat(editor).isVisible();
            audit(p);
            editor.getByLabel("Name").fill("Initech");
            editor.getByLabel("Email").fill("hi@initech.example");
            editor.getByLabel("City").fill("Munich");
            button(editor, "Save").click();
            idle(p);
            assertThat(editor).isHidden();
            assertThat(rows).hasCount(6);
            assertThat(section.getByRole(AriaRole.CELL, new Locator.GetByRoleOptions().setName("Initech").setExact(true))).isVisible();
            assertThat(status).hasText("Saved Initech.");
            assertThat(button(section, "New")).isFocused();

            // Edit from a row's menu: the focus returns to that row's actions button
            button(section, "Actions for Lantern").click();
            p.getByRole(AriaRole.MENUITEM, new Page.GetByRoleOptions().setName("Edit")).click();
            idle(p);
            editor = p.getByRole(AriaRole.DIALOG, new Page.GetByRoleOptions().setName("Edit Lantern"));
            assertThat(editor.getByLabel("City")).hasValue("Lisbon");
            editor.getByLabel("City").fill("Porto");
            button(editor, "Save").click();
            idle(p);
            assertThat(editor).isHidden();
            assertThat(section.getByRole(AriaRole.CELL, new Locator.GetByRoleOptions().setName("Porto").setExact(true))).isVisible();
            assertThat(button(section, "Actions for Lantern")).isFocused();

            // Escape changes nothing
            button(section, "Actions for Northwind").click();
            p.getByRole(AriaRole.MENUITEM, new Page.GetByRoleOptions().setName("Edit")).click();
            idle(p);
            editor = p.getByRole(AriaRole.DIALOG, new Page.GetByRoleOptions().setName("Edit Northwind"));
            editor.getByLabel("Name").fill("Southwind");
            p.keyboard().press("Escape");
            idle(p);
            assertThat(editor).isHidden();
            assertThat(section.getByRole(AriaRole.CELL, new Locator.GetByRoleOptions().setName("Northwind").setExact(true))).isVisible();

            // Delete asks first; the row has gone, so the focus moves to New
            button(section, "Actions for Globex").click();
            p.getByRole(AriaRole.MENUITEM, new Page.GetByRoleOptions().setName("Delete")).click();
            idle(p);
            Locator question = p.getByRole(AriaRole.ALERTDIALOG, new Page.GetByRoleOptions().setName("Delete Globex?"));
            assertThat(question).containsText("This can't be undone.");
            settle(p);
            button(question, "Delete").click();
            idle(p);
            assertThat(question).isHidden();
            assertThat(section.getByRole(AriaRole.CELL, new Locator.GetByRoleOptions().setName("Globex").setExact(true))).hasCount(0);
            assertThat(status).hasText("Deleted Globex.");
            assertThat(button(section, "New")).isFocused();

            // Searching re-renders the rows, not the field
            Locator search = section.getByLabel("Search customers");
            search.fill("north");
            p.waitForCondition(() -> rows.count() == 1);
            idle(p);
            assertThat(rows.first()).containsText("Northwind");
            assertThat(search).isFocused();
            settle(p);
            audit(p);
            p.screenshot(new Page.ScreenshotOptions().setPath(Path.of("target/collections-screenshots", "crud-" + engine + "-" + skin + "-" + mode + ".png")));
            assertThat(errors).isEmpty();
        }
    }

    @ParameterizedTest @ValueSource(strings = {"chromium", "firefox"})
    void withoutJavaScriptLoadMoreRendersThePageAtTheNextBatch(String engine) {
        try (var context = browser(engine).newContext(new Browser.NewContextOptions().setJavaScriptEnabled(false).setViewportSize(360, 900));
             var p = context.newPage()) {
            p.navigate(url("vega", "light"));
            Locator feed = p.locator("section[aria-labelledby=feed-heading]"), items = feed.locator("[role=listitem]");
            feed.getByRole(AriaRole.LINK, new Locator.GetByRoleOptions().setName("Load more")).click();
            assertThat(items).hasCount(40);
            assertThat(feed.locator(".sw-load-more-count")).hasText("40 of 134");
            assertThat(p).hasURL(java.util.regex.Pattern.compile("#.+-batch-1$"));
            // The customers still read, without the editing that needs script
            assertThat(p.locator("section[aria-labelledby=customers-heading] tbody tr")).hasCount(5);
        }
    }
}
