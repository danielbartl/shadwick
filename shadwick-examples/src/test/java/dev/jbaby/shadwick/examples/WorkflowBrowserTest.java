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

/** The wizard, the filter and the master-detail on /workflow: keyboard, focus, one pane on a phone, without script. */
@Tag("browser")
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class WorkflowBrowserTest {
    static Playwright playwright;
    static Browser chromium, firefox;
    @LocalServerPort int port;
    @Autowired WebApplication app;

    @BeforeAll static void start() throws Exception {
        Files.createDirectories(Path.of("target/workflow-screenshots"));
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
    String url(String skin, String mode) { return "http://localhost:" + port + "/workflow?skin=" + skin + "&mode=" + mode; }
    void idle(Page p) { p.waitForCondition(() -> (Boolean) p.evaluate("Object.keys(Wicket.channelManager.channels).length===0")); }
    Object focused(Page p, String script) { return p.evaluate("() => { const e = document.activeElement; return " + script + "; }"); }
    void audit(Page p) {
        assertThat(p.locator("[style]")).hasCount(0);
        assertThat(p.evaluate("document.documentElement.scrollWidth <= innerWidth + 1")).as("No horizontal overflow").isEqualTo(true);
        assertThat(new AxeBuilder(p).withTags(List.of("wcag2a", "wcag2aa", "wcag21a", "wcag21aa", "wcag22aa")).analyze().getViolations()).isEmpty();
    }

    @ParameterizedTest @MethodSource("skins")
    void theWizardChecksEachStepAndMovesTheFocus(String engine, String skin, String mode, int width) {
        try (var context = browser(engine).newContext(new Browser.NewContextOptions().setViewportSize(width, 1000)); var p = context.newPage()) {
            var errors = new ArrayList<String>();
            p.onPageError(errors::add);
            p.onConsoleMessage(m -> { if (m.type().equals("error")) errors.add(m.text()); });
            p.navigate(url(skin, mode));
            Locator wizard = p.locator("section[aria-labelledby=signup-heading]");
            Button next = () -> wizard.getByRole(AriaRole.BUTTON, new Locator.GetByRoleOptions().setName("Next").setExact(true));

            // Next with the field empty: the error at the field, which takes the focus
            next.get().click();
            idle(p);
            assertThat(wizard.locator(".sw-field-error").first()).containsText("is required");
            assertThat(wizard.getByLabel("Work email")).isFocused();
            // A check only the server makes: in the summary, which takes the focus
            wizard.getByLabel("Work email").fill("taken@example.com");
            next.get().click();
            idle(p);
            assertThat(wizard.locator(".sw-form-errors")).isFocused();
            assertThat(wizard.locator(".sw-form-errors")).containsText("already exists");
            // Moving on: the new step takes the focus; Enter submits Next
            wizard.getByLabel("Work email").fill("ada@example.com");
            p.keyboard().press("Enter");
            idle(p);
            assertThat(wizard.locator("[role=group][aria-label='Step 2 of 3: Profile']")).isFocused();
            assertThat(wizard.locator(".sw-step[aria-current=step]")).containsText("Profile");
            wizard.getByLabel("Your name").fill("Ada Lovelace");
            wizard.getByLabel("Team name").fill("Analytical Engines");
            audit(p);
            next.get().click();
            idle(p);
            assertThat(wizard.locator("[role=group]")).hasAttribute("aria-label", "Step 3 of 3: Review");
            assertThat(wizard.locator("dd").nth(1)).hasText("Ada Lovelace");
            // Back keeps what was entered
            wizard.getByRole(AriaRole.BUTTON, new Locator.GetByRoleOptions().setName("Back")).click();
            idle(p);
            assertThat(wizard.getByLabel("Team name")).hasValue("Analytical Engines");
            // A finished step is a link back
            wizard.locator(".sw-step-action").first().click();
            idle(p);
            assertThat(wizard.getByLabel("Work email")).hasValue("ada@example.com");
            next.get().click();
            idle(p);
            next.get().click();
            idle(p);
            wizard.getByRole(AriaRole.BUTTON, new Locator.GetByRoleOptions().setName("Finish")).click();
            idle(p);
            assertThat(p.locator("[role=status].ex-muted").first()).hasText("Welcome, Ada Lovelace! Your workspace for Analytical Engines is ready.");
            p.screenshot(new Page.ScreenshotOptions().setPath(Path.of("target/workflow-screenshots", "wizard-" + engine + "-" + skin + "-" + mode + ".png")));
            assertThat(errors).isEmpty();
        }
    }

    interface Button { Locator get(); }

    @ParameterizedTest @MethodSource("skins")
    void theFilterNarrowsTheInboxAndTheDetailsTakeTheirPlaceOnAPhone(String engine, String skin, String mode, int width) {
        try (var context = browser(engine).newContext(new Browser.NewContextOptions().setViewportSize(width, 1000)); var p = context.newPage()) {
            var errors = new ArrayList<String>();
            p.onPageError(errors::add);
            p.onConsoleMessage(m -> { if (m.type().equals("error")) errors.add(m.text()); });
            p.navigate(url(skin, mode));
            Locator inbox = p.locator("section[aria-labelledby=inbox-heading]"), tickets = inbox.locator(".ex-workflow-ticket");
            Locator root = inbox.locator(".sw-master-detail");
            assertThat(tickets).hasCount(4);

            // The arrow keys choose a chip; the list follows and the chip keeps the focus
            Locator all = inbox.getByRole(AriaRole.RADIO, new Locator.GetByRoleOptions().setName("All"));
            assertThat(all).isChecked();
            all.focus();
            p.keyboard().press("ArrowRight");
            idle(p);
            assertThat(tickets).hasCount(2);
            assertThat(tickets).hasText(new String[]{"Invoice shows the wrong addressFern & Field", "Export to CSVLantern"});
            assertThat(inbox.getByRole(AriaRole.RADIO, new Locator.GetByRoleOptions().setName("Open"))).isFocused();
            inbox.getByRole(AriaRole.RADIO, new Locator.GetByRoleOptions().setName("All")).check();
            idle(p);
            assertThat(tickets).hasCount(4);

            Locator ticket = inbox.getByRole(AriaRole.LINK, new Locator.GetByRoleOptions().setName("Export to CSV Lantern"));
            ticket.click();
            idle(p);
            Locator detail = root.locator(".sw-master-detail-detail");
            assertThat(detail.locator(".sw-card-title")).hasText("Export to CSV");
            assertThat(inbox.locator(".ex-workflow-ticket[aria-current=true]")).hasText("Export to CSVLantern");
            if (width >= 1000) {
                // Both panes: the list stays, the focus stays on the ticket used
                assertThat(inbox.locator(".sw-master-detail-master")).isVisible();
                assertThat(inbox.locator(".sw-master-detail-back")).isHidden();
                assertThat(focused(p, "e.textContent.trim()")).isEqualTo("Export to CSVLantern");
            } else {
                // One pane: the details replace the list and take the focus; Back returns it to the ticket
                assertThat(root).hasAttribute("data-showing", "detail");
                assertThat(inbox.locator(".sw-master-detail-master")).isHidden();
                assertThat(detail).isFocused();
                inbox.getByRole(AriaRole.LINK, new Locator.GetByRoleOptions().setName("Back")).click();
                assertThat(root).hasAttribute("data-showing", "master");
                assertThat(focused(p, "e.textContent.trim()")).isEqualTo("Export to CSVLantern");
            }
            audit(p);
            p.screenshot(new Page.ScreenshotOptions().setPath(Path.of("target/workflow-screenshots", "inbox-" + engine + "-" + skin + "-" + mode + ".png")));
            assertThat(errors).isEmpty();
        }
    }

    @ParameterizedTest @ValueSource(strings = {"chromium", "firefox"})
    void withoutJavaScriptEveryActionStillWorks(String engine) {
        try (var context = browser(engine).newContext(new Browser.NewContextOptions().setJavaScriptEnabled(false).setViewportSize(360, 1000));
             var p = context.newPage()) {
            p.navigate(url("vega", "light"));
            Locator wizard = p.locator("section[aria-labelledby=signup-heading]");
            wizard.getByLabel("Work email").fill("grace@example.com");
            wizard.getByRole(AriaRole.BUTTON, new Locator.GetByRoleOptions().setName("Next").setExact(true)).click();
            assertThat(wizard.locator("[role=group]")).hasAttribute("aria-label", "Step 2 of 3: Profile");
            wizard.getByRole(AriaRole.BUTTON, new Locator.GetByRoleOptions().setName("Next").setExact(true)).click();
            assertThat(wizard.locator(".sw-field-error").first()).containsText("is required");
            wizard.getByLabel("Your name").fill("Grace Hopper");
            wizard.getByLabel("Team name").fill("Compilers");
            wizard.getByRole(AriaRole.BUTTON, new Locator.GetByRoleOptions().setName("Next").setExact(true)).click();
            wizard.getByRole(AriaRole.BUTTON, new Locator.GetByRoleOptions().setName("Finish")).click();
            assertThat(p.locator("[role=status].ex-muted").first()).hasText("Welcome, Grace Hopper! Your workspace for Compilers is ready.");

            // The filter's Apply button, then a ticket and Back
            Locator inbox = p.locator("section[aria-labelledby=inbox-heading]");
            inbox.getByRole(AriaRole.RADIO, new Locator.GetByRoleOptions().setName("Closed")).check();
            inbox.getByRole(AriaRole.BUTTON, new Locator.GetByRoleOptions().setName("Apply")).click();
            assertThat(inbox.locator(".ex-workflow-ticket")).hasText(new String[]{"Login page in GermanAcme Studio"});
            inbox.getByRole(AriaRole.LINK, new Locator.GetByRoleOptions().setName("Login page in German Acme Studio")).click();
            assertThat(inbox.locator(".sw-master-detail")).hasAttribute("data-showing", "detail");
            assertThat(inbox.locator(".sw-master-detail-detail .sw-card-title")).hasText("Login page in German");
            inbox.getByRole(AriaRole.LINK, new Locator.GetByRoleOptions().setName("Back")).click();
            assertThat(inbox.locator(".sw-master-detail")).hasAttribute("data-showing", "master");
        }
    }
}
