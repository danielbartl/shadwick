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

/** Sonner-style toast stacking and the six toaster positions, in real browsers. */
@Tag("browser")
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class ToastStackBrowserTest {
    static Playwright playwright;
    static Browser chromium, firefox;
    @LocalServerPort int port;
    @Autowired WebApplication app;

    @BeforeAll static void start() throws Exception {
        Files.createDirectories(Path.of("target/toast-stack-screenshots"));
        playwright = Playwright.create();
        chromium = playwright.chromium().launch();
        firefox = playwright.firefox().launch();
    }
    @AfterAll static void stop() { playwright.close(); }
    @BeforeEach void markup() { app.getMarkupSettings().setStripWicketTags(true); }
    @AfterEach void restore() { app.getMarkupSettings().setStripWicketTags(false); }

    Browser browser(String engine) { return engine.equals("chromium") ? chromium : firefox; }
    String url(String skin, String mode) { return "http://localhost:" + port + "/toasts?skin=" + skin + "&mode=" + mode + "&lang=en-US"; }
    void idle(Page p) { p.waitForCondition(() -> (Boolean) p.evaluate("Object.keys(Wicket.channelManager.channels).length===0")); }
    List<String> errors(Page p) {
        var result = new ArrayList<String>();
        p.onPageError(result::add);
        p.onConsoleMessage(m -> { if (m.type().equals("error")) result.add(m.text()); });
        return result;
    }
    void settle(Page p) {
        p.waitForCondition(() -> (Boolean) p.evaluate("document.getAnimations().every(a => a.playState !== 'running' || a.effect?.getTiming().iterations === Infinity)"));
    }
    void audit(Page p) {
        settle(p);
        assertThat(p.evaluate("document.documentElement.scrollWidth <= innerWidth + 1")).as("No horizontal overflow").isEqualTo(true);
        assertThat(new AxeBuilder(p).withTags(List.of("wcag2a", "wcag2aa", "wcag21a", "wcag21aa", "wcag22aa")).analyze().getViolations()).isEmpty();
    }
    double number(Locator l, String script) { return ((Number) l.evaluate(script)).doubleValue(); }
    Locator showFive(Page p) {
        p.getByRole(AriaRole.BUTTON, new Page.GetByRoleOptions().setName("Show five toasts")).click();
        Locator toasts = p.locator("#sw-toaster > .sw-toast");
        assertThat(toasts).hasCount(5);
        assertThat(toasts.last()).containsText("The newest is in front");
        settle(p);
        return toasts;
    }

    static Stream<Arguments> skins() {
        List<String> selected = Arrays.asList(System.getProperty("shadwick.browser.skins", "vega,lyra,nova,maia,mira,luma,rhea,sera,sketch").split(","));
        return Stream.of("chromium", "firefox").flatMap(engine -> Arrays.stream(Skin.values())
                .map(skin -> skin.name().toLowerCase(Locale.ROOT)).filter(selected::contains)
                .flatMap(skin -> Stream.of(Arguments.of(engine, skin, "light", 1440), Arguments.of(engine, skin, "dark", 320))));
    }

    @ParameterizedTest @MethodSource("skins")
    void theNewestToastIsInFrontAndTheStackSpreadsOutOnHoverAndFocus(String engine, String skin, String mode, int width) {
        try (var context = browser(engine).newContext(new Browser.NewContextOptions().setViewportSize(width, 900)); var p = context.newPage()) {
            var errors = errors(p);
            p.navigate(url(skin, mode));
            Locator toasts = showFive(p);
            Locator toaster = p.locator("#sw-toaster");
            assertThat(toaster).hasAttribute("data-stacked", "");
            assertThat(toaster).hasAttribute("data-position", "bottom-end");
            assertThat(p.locator("#sw-toaster > .sw-toast:not([data-hidden])")).hasCount(3);
            assertThat(p.locator("#sw-toaster > .sw-toast[data-hidden]")).hasCount(2);
            assertThat(p.locator("#sw-toaster > .sw-toast[data-behind]")).hasCount(2);
            Locator front = toasts.last();
            // The front toast is on top, and the collapsed toaster is as tall as it
            assertThat(front.evaluate("t => { const r = t.getBoundingClientRect(); return t.contains(document.elementFromPoint(r.left + r.width / 2, r.top + r.height / 2)); }")).isEqualTo(true);
            assertThat(Math.abs(number(toaster, "n => n.getBoundingClientRect().height") - number(front, "n => n.offsetHeight"))).isLessThan(1.5);
            assertThat(toasts.nth(3).locator(".sw-toast-content").evaluate("n => getComputedStyle(n).opacity")).isEqualTo("0");
            // Behind the front toast, the hidden close buttons take no clicks
            assertThat(toasts.nth(3).locator(".sw-toast-close").evaluate("n => getComputedStyle(n).pointerEvents")).isEqualTo("none");
            assertThat(number(toaster, "n => n.getBoundingClientRect().bottom")).isLessThanOrEqualTo(900);
            // Stack geometry lives in a constructable stylesheet, never in style attributes (checked before axe,
            // which can leave an empty style attribute on an element it measures)
            assertThat(p.locator("[style]")).hasCount(0);
            p.screenshot(new Page.ScreenshotOptions().setPath(Path.of("target/toast-stack-screenshots", engine + "-" + skin + "-" + mode + "-collapsed.png")));
            audit(p);

            // Hover spreads the three shown toasts out, one above the other, all inside the viewport
            var box = front.boundingBox();
            p.mouse().move(box.x + box.width / 2, box.y + box.height / 2);
            assertThat(toaster).hasAttribute("data-expanded", "");
            settle(p);
            assertThat(p.evaluate("() => { const shown = [...document.querySelectorAll('#sw-toaster > .sw-toast:not([data-hidden])')].map(t => t.getBoundingClientRect());"
                    + " return shown.every((r, i) => i === 0 || r.top >= shown[i - 1].bottom - 1) && shown.every(r => r.top >= 0 && r.bottom <= innerHeight + 1); }"))
                    .as("spread out without overlapping, inside the viewport").isEqualTo(true);
            p.screenshot(new Page.ScreenshotOptions().setPath(Path.of("target/toast-stack-screenshots", engine + "-" + skin + "-" + mode + "-expanded.png")));
            p.mouse().move(1, 1);
            assertThat(toaster).not().hasAttribute("data-expanded", "");

            // Alt+T reaches the newest toast and expands the stack; Shift+Tab reaches the next shown one
            p.keyboard().press("Alt+KeyT");
            assertThat(front.getByRole(AriaRole.BUTTON)).isFocused();
            assertThat(toaster).hasAttribute("data-expanded", "");
            p.keyboard().press("Shift+Tab");
            assertThat(toasts.nth(3).getByRole(AriaRole.BUTTON)).isFocused();
            audit(p);
            // Closing one lets a waiting toast take its place
            p.keyboard().press("Escape");
            assertThat(toasts).hasCount(4);
            assertThat(p.locator("#sw-toaster > .sw-toast[data-hidden]")).hasCount(1);
            assertThat(p.locator("#sw-toaster > .sw-toast[data-hidden]")).containsText("Message 1 of five.");
            assertThat(errors).isEmpty();
        }
    }

    @Test
    void waitingToastsKeepTheirTimeUntilTheyShow() {
        try (var context = chromium.newContext(); var p = context.newPage()) {
            p.navigate(url("vega", "light"));
            p.clock().install(new Clock.InstallOptions().setTime(1000000));
            p.clock().pauseAt(1000000);
            p.evaluate("for (let i = 1; i <= 4; i++) Shadwick.toast('Timed ' + i, {duration: 2000})");
            p.clock().runFor(100);
            Locator oldest = p.locator(".sw-toast").filter(new Locator.FilterOptions().setHasText("Timed 1"));
            assertThat(oldest).hasAttribute("data-hidden", "");
            p.clock().runFor(2100);
            // The three shown toasts closed; the waiting one shows now with its time intact
            assertThat(p.locator(".sw-toast")).hasCount(1);
            assertThat(oldest).not().hasAttribute("data-hidden", "");
            p.clock().runFor(1500);
            assertThat(oldest).hasCount(1);
            p.clock().runFor(600);
            assertThat(oldest).hasCount(0);
            p.clock().resume();
        }
    }

    static Stream<Arguments> positions() {
        return Stream.of("chromium", "firefox").flatMap(engine -> Stream.of("ltr", "rtl").map(dir -> Arguments.of(engine, dir)));
    }

    @ParameterizedTest @MethodSource("positions")
    void toastsAppearAtEachPositionAndStartAndEndFollowTheTextDirection(String engine, String dir) {
        try (var context = browser(engine).newContext(new Browser.NewContextOptions().setViewportSize(1280, 800)); var p = context.newPage()) {
            var errors = errors(p);
            p.navigate(url("vega", "light"));
            p.evaluate("d => document.documentElement.dir = d", dir);
            showFive(p);
            Locator toaster = p.locator("#sw-toaster");
            for (String position : List.of("top-start", "top-center", "top-end", "bottom-start", "bottom-center", "bottom-end")) {
                p.evaluate("pos => Shadwick.configureToasts({position: pos})", position);
                assertThat(toaster).hasAttribute("data-position", position);
                settle(p);
                @SuppressWarnings("unchecked")
                Map<String, Number> r = (Map<String, Number>) toaster.evaluate("n => { const r = n.getBoundingClientRect(); return {left: r.left, right: innerWidth - r.right, top: r.top, bottom: innerHeight - r.bottom}; }");
                boolean rtl = dir.equals("rtl");
                if (position.startsWith("top")) assertThat(r.get("top").doubleValue()).as(position + " top gap").isBetween(20.0, 28.0);
                else assertThat(r.get("bottom").doubleValue()).as(position + " bottom gap").isBetween(20.0, 28.0);
                String side = position.substring(position.indexOf('-') + 1);
                if (side.equals("center")) {
                    assertThat(Math.abs(r.get("left").doubleValue() - r.get("right").doubleValue())).as(position + " centred").isLessThan(2);
                } else {
                    boolean left = side.equals("start") != rtl;
                    assertThat(r.get(left ? "left" : "right").doubleValue()).as(position + " " + dir + " edge").isBetween(20.0, 28.0);
                }
            }
            assertThat(errors).isEmpty();
        }
    }

    @ParameterizedTest @ValueSource(strings = {"chromium", "firefox"})
    void thePagesPositionChoiceMovesItsToastsOverAjaxAndNarrowScreensUseTheFullWidth(String engine) {
        try (var context = browser(engine).newContext(new Browser.NewContextOptions().setViewportSize(1280, 800)); var p = context.newPage()) {
            var errors = errors(p);
            p.navigate(url("vega", "light"));
            p.locator("form[aria-labelledby=stack-heading] select").selectOption("top-center");
            idle(p);
            Locator toaster = p.locator("#sw-toaster");
            assertThat(toaster).hasAttribute("data-position", "top-center");
            assertThat(p.locator(".sw-toast")).containsText("Toasts now appear here on this page.");
            settle(p);
            assertThat(number(toaster, "n => n.getBoundingClientRect().top")).isBetween(20.0, 28.0);

            p.setViewportSize(320, 700);
            for (String position : List.of("top-start", "top-center", "bottom-end")) {
                p.evaluate("pos => Shadwick.configureToasts({position: pos})", position);
                settle(p);
                assertThat(toaster.evaluate("n => { const r = n.getBoundingClientRect(); return Math.round(r.left) + ',' + Math.round(innerWidth - r.right); }"))
                        .as(position + " on a narrow screen").isEqualTo("16,16");
            }
            assertThat(errors).isEmpty();
        }
    }

    @ParameterizedTest @ValueSource(strings = {"chromium", "firefox"})
    void reducedMotionOnlyFadesAndADialogKeepsItsOwnStack(String engine) {
        try (var context = browser(engine).newContext(new Browser.NewContextOptions().setReducedMotion(ReducedMotion.REDUCE)); var p = context.newPage()) {
            var errors = errors(p);
            p.navigate(url("vega", "light"));
            Locator toasts = showFive(p);
            assertThat(toasts.last().evaluate("t => getComputedStyle(t).transitionProperty")).isEqualTo("opacity");
            p.keyboard().press("Alt+KeyT");
            for (int i = 0; i < 5; i++) p.keyboard().press("Escape");
            assertThat(p.locator("#sw-toaster")).hasCount(0);

            p.getByRole(AriaRole.BUTTON, new Page.GetByRoleOptions().setName("Open toast dialog")).click();
            Locator dialog = p.getByRole(AriaRole.DIALOG, new Page.GetByRoleOptions().setName("Notifications in a dialog"));
            Locator notify = dialog.getByRole(AriaRole.BUTTON, new Locator.GetByRoleOptions().setName("Notify inside dialog"));
            for (int i = 0; i < 4; i++) { notify.click(); idle(p); }
            Locator inDialog = dialog.locator("#sw-toaster");
            assertThat(inDialog).hasAttribute("data-stacked", "");
            assertThat(inDialog.locator(".sw-toast[data-hidden]")).hasCount(1);
            assertThat(notify).isFocused();
            audit(p);
            assertThat(errors).isEmpty();
        }
    }

    @ParameterizedTest @ValueSource(strings = {"chromium", "firefox"})
    void withoutConstructableStylesheetsToastsStayAPlainColumn(String engine) {
        try (var context = browser(engine).newContext(new Browser.NewContextOptions().setViewportSize(1280, 800)); var p = context.newPage()) {
            var errors = errors(p);
            p.addInitScript("window.CSSStyleSheet = undefined;");
            p.navigate(url("vega", "light"));
            Locator toasts = showFive(p);
            Locator toaster = p.locator("#sw-toaster");
            assertThat(toaster).not().hasAttribute("data-stacked", "");
            assertThat(p.locator(".sw-toast[data-hidden]")).hasCount(0);
            assertThat(p.evaluate("() => { const r = [...document.querySelectorAll('#sw-toaster > .sw-toast')].map(t => t.getBoundingClientRect());"
                    + " return r.every((b, i) => i === 0 || b.top >= r[i - 1].bottom - 1); }")).as("a column").isEqualTo(true);
            p.keyboard().press("Alt+KeyT");
            assertThat(toasts.last().getByRole(AriaRole.BUTTON)).isFocused();
            audit(p);
            assertThat(errors).isEmpty();
        }
    }
}
