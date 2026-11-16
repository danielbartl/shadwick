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

/**
 * The Shadleaf parity increment in real browsers: the extra button sizes and touch hit area, the checkbox's mixed
 * state, the small confirmation dialog with an icon, the empty state without outline and explicitly sized icons.
 */
@Tag("browser")
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class QuickParityBrowserTest {
    static Playwright playwright;
    static Browser chromium, firefox;
    @LocalServerPort int port;
    @Autowired WebApplication app;

    @BeforeAll static void start() throws Exception {
        Files.createDirectories(Path.of("target/quick-parity-screenshots"));
        playwright = Playwright.create();
        chromium = playwright.chromium().launch();
        firefox = playwright.firefox().launch();
    }
    @AfterAll static void stop() { playwright.close(); }
    @BeforeEach void markup() { app.getMarkupSettings().setStripWicketTags(true); }
    @AfterEach void restore() { app.getMarkupSettings().setStripWicketTags(false); }

    Browser browser(String engine) { return engine.equals("chromium") ? chromium : firefox; }
    String url(String route, String skin, String mode, String lang) {
        return "http://localhost:" + port + "/" + route + "?skin=" + skin + "&mode=" + mode + "&lang=" + lang;
    }
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
    /** Waits for finite transitions (a dialog's opening fade) to finish, so screenshots and audits see the end state. */
    void settle(Page p) {
        p.waitForCondition(() -> (Boolean) p.evaluate("document.getAnimations().every(a => a.playState !== 'running' || a.effect?.getTiming().iterations === Infinity)"));
    }
    double number(Locator l, String script) { return ((Number) l.evaluate(script)).doubleValue(); }

    static Stream<Arguments> skins() {
        List<String> selected = Arrays.asList(System.getProperty("shadwick.browser.skins", "vega,lyra,nova,maia,mira,luma,rhea,sera,sketch").split(","));
        return Stream.of("chromium", "firefox").flatMap(engine -> Arrays.stream(Skin.values())
                .map(skin -> skin.name().toLowerCase(Locale.ROOT)).filter(selected::contains)
                .flatMap(skin -> Stream.of(Arguments.of(engine, skin, "light", 1440), Arguments.of(engine, skin, "dark", 320))));
    }

    @ParameterizedTest @MethodSource("skins")
    void theExtraButtonSizesKeepTheirOrderAndIconButtonsStaySquare(String engine, String skin, String mode, int width) {
        try (var context = browser(engine).newContext(new Browser.NewContextOptions().setViewportSize(width, 1000).setReducedMotion(ReducedMotion.REDUCE));
             var p = context.newPage()) {
            var errors = errors(p);
            p.navigate(url("buttons", skin, mode, "en-US"));
            String height = "n => n.getBoundingClientRect().height";
            Locator sizes = p.locator("section[aria-labelledby=sizes-heading]");
            double xs = number(sizes.getByRole(AriaRole.BUTTON, new Locator.GetByRoleOptions().setName("Extra small")), height);
            double sm = number(sizes.getByRole(AriaRole.BUTTON, new Locator.GetByRoleOptions().setName("Small").setExact(true)), height);
            double md = number(sizes.getByRole(AriaRole.BUTTON, new Locator.GetByRoleOptions().setName("Default").setExact(true)), height);
            assertThat(xs).as("xs below sm").isLessThan(sm);
            assertThat(sm).as("sm below default").isLessThanOrEqualTo(md);

            double previous = 0;
            for (String size : List.of("icon-xs", "icon-sm", "icon", "icon-lg")) {
                Locator icon = p.locator(".sw-button[data-size='" + size + "'][aria-label='Add']");
                assertThat(icon).hasCount(1);
                double w = number(icon, "n => n.getBoundingClientRect().width");
                double h = number(icon, height);
                assertThat(Math.abs(w - h)).as(size + " is square").isLessThan(1);
                assertThat(w).as(size + " grows").isGreaterThanOrEqualTo(previous);
                previous = w;
            }
            // icon-sm is as tall as sm, icon-lg as tall as lg
            assertThat(Math.abs(number(p.locator(".sw-button[data-size='icon-sm']"), height) - sm)).isLessThan(1);
            assertThat(Math.abs(number(p.locator(".sw-button[data-size='icon-lg']"), height)
                    - number(sizes.getByRole(AriaRole.BUTTON, new Locator.GetByRoleOptions().setName("Large").setExact(true)), height))).isLessThan(1);

            p.locator(".sw-button[data-size='icon-sm']").click();
            assertThat(p.getByText("Clicked Add (icon-sm).")).isVisible();
            settle(p);
            p.screenshot(new Page.ScreenshotOptions().setPath(Path.of("target/quick-parity-screenshots", "buttons-" + engine + "-" + skin + "-" + mode + ".png")));
            audit(p);
            assertThat(errors).isEmpty();
        }
    }

    @ParameterizedTest @MethodSource("skins")
    void extraSmallButtonsStillJoinInAGroup(String engine, String skin, String mode, int width) {
        try (var context = browser(engine).newContext(new Browser.NewContextOptions().setViewportSize(width, 1000)); var p = context.newPage()) {
            p.navigate(url("button-groups", skin, mode, "en-US"));
            var records = p.getByRole(AriaRole.GROUP, new Page.GetByRoleOptions().setName("Browse records").setExact(true));
            for (String size : List.of("xs", "icon-xs")) {
                records.evaluate("(n, size) => n.querySelectorAll('.sw-button').forEach(b => b.dataset.size = size)", size);
                assertThat(records.evaluate("n=>{let b=n.querySelectorAll('.sw-button');let r0=b[0].getBoundingClientRect(),r1=b[1].getBoundingClientRect();"
                        + "return Math.abs(r1.left-r0.right+parseFloat(getComputedStyle(b[1]).borderLeftWidth))<1"
                        + "&&getComputedStyle(b[0]).borderStartEndRadius==='0px'&&getComputedStyle(b[1]).borderStartStartRadius==='0px';}"))
                        .as(size + " buttons join with square inner corners").isEqualTo(true);
            }
        }
    }

    @Test
    void onATouchScreenSmallIconButtonsAreEasierToTapThanTheyLook() {
        // Chromium reports pointer: coarse for a mobile touch context; Firefox has no such emulation
        for (boolean touch : List.of(true, false)) {
            var options = new Browser.NewContextOptions().setViewportSize(390, 900);
            if (touch) options.setHasTouch(true).setIsMobile(true);
            try (var context = chromium.newContext(options); var p = context.newPage()) {
                p.navigate(url("buttons", "vega", "light", "en-US"));
                assertThat(p.evaluate("matchMedia('(pointer: coarse)').matches")).isEqualTo(touch);
                for (String size : List.of("icon-xs", "icon-sm", "icon")) {
                    Locator button = p.locator(".sw-button[data-size='" + size + "'][aria-label='Add']");
                    button.scrollIntoViewIfNeeded();
                    // 3px below the visible edge: inside the 44px area of all three sizes (icon is 36px in vega, so 4px each side)
                    Object hit = button.evaluate("b => { const r = b.getBoundingClientRect(); const target = document.elementFromPoint(r.left + r.width / 2, r.bottom + 3); return target === b || b.contains(target); }");
                    assertThat(hit).as(size + " tapped 3px below " + (touch ? "on a touch screen" : "with a mouse")).isEqualTo(touch);
                }
                Locator large = p.locator(".sw-button[data-size='icon-lg'][aria-label='Add']");
                assertThat(large.evaluate("b => getComputedStyle(b, '::after').content")).isIn("none", "normal");

                p.navigate(url("groups", "vega", "light", "en-US"));
                Object inGroup = p.evaluate("() => { const g = document.querySelector('.sw-input-group'); const b = document.createElement('button');"
                        + " b.className = 'sw-button'; b.dataset.size = 'icon-sm'; b.setAttribute('aria-label', 'Probe'); g.append(b);"
                        + " const c = getComputedStyle(b, '::after').content; b.remove(); return c; }");
                assertThat(inGroup).as("no enlargement inside an input group").isIn("none", "normal");
            }
        }
    }

    @ParameterizedTest @ValueSource(strings = {"chromium", "firefox"})
    void aSelectAllCheckboxShowsTheMixedStateFromTheServer(String engine) {
        try (var context = browser(engine).newContext(new Browser.NewContextOptions().setViewportSize(1280, 1000)); var p = context.newPage()) {
            var errors = errors(p);
            p.navigate(url("choices", "vega", "light", "en-US"));
            Locator all = p.getByRole(AriaRole.CHECKBOX, new Page.GetByRoleOptions().setName("All projects"));
            Locator forma = p.getByRole(AriaRole.CHECKBOX, new Page.GetByRoleOptions().setName("Forma Architects"));
            String indeterminate = "n => n.indeterminate";
            String minusShown = "n => getComputedStyle(n.parentElement.querySelector('.sw-checkbox-indeterminate-indicator')).display !== 'none'";
            // Two of three projects start selected
            assertThat(all.evaluate(indeterminate)).isEqualTo(true);
            assertThat(all.evaluate(minusShown)).isEqualTo(true);
            assertThat(p.getByText("2 of 3 projects selected.")).isVisible();
            audit(p);

            forma.check(); idle(p);
            all = p.getByRole(AriaRole.CHECKBOX, new Page.GetByRoleOptions().setName("All projects"));
            assertThat(all).isChecked();
            assertThat(all.evaluate(indeterminate)).isEqualTo(false);
            assertThat(p.getByText("3 of 3 projects selected.")).isVisible();

            all.uncheck(); idle(p);
            for (String name : List.of("Fern & Field", "Forma Architects", "Sunday Coffee"))
                assertThat(p.getByRole(AriaRole.CHECKBOX, new Page.GetByRoleOptions().setName(name))).not().isChecked();
            all = p.getByRole(AriaRole.CHECKBOX, new Page.GetByRoleOptions().setName("All projects"));
            assertThat(all.evaluate(indeterminate)).isEqualTo(false);

            p.getByRole(AriaRole.CHECKBOX, new Page.GetByRoleOptions().setName("Sunday Coffee")).check(); idle(p);
            all = p.getByRole(AriaRole.CHECKBOX, new Page.GetByRoleOptions().setName("All projects"));
            assertThat(all.evaluate(indeterminate)).as("mixed again after an Ajax re-render").isEqualTo(true);
            assertThat(all).not().isChecked();

            // A click on a mixed box clears the minus, checks it, and the server checks every project
            all.click(); idle(p);
            assertThat(p.getByRole(AriaRole.CHECKBOX, new Page.GetByRoleOptions().setName("All projects")).evaluate(indeterminate)).isEqualTo(false);
            assertThat(p.getByText("3 of 3 projects selected.")).isVisible();
            assertThat(errors).isEmpty();
        }
    }

    @ParameterizedTest @MethodSource("skins")
    void aSmallConfirmationIsNarrowerWithEqualButtonsAndAnIconAboveItsTitle(String engine, String skin, String mode, int width) {
        try (var context = browser(engine).newContext(new Browser.NewContextOptions().setViewportSize(width, 900).setReducedMotion(ReducedMotion.REDUCE));
             var p = context.newPage()) {
            var errors = errors(p);
            p.navigate(url("overlays", skin, mode, "en-US"));
            p.getByRole(AriaRole.BUTTON, new Page.GetByRoleOptions().setName("Delete draft")).click();
            Locator regular = p.getByRole(AriaRole.ALERTDIALOG, new Page.GetByRoleOptions().setName("Delete this draft?"));
            assertThat(regular).isVisible();
            double regularWidth = number(regular, "n => n.getBoundingClientRect().width");
            p.keyboard().press("Escape");
            assertThat(regular).isHidden();

            p.getByRole(AriaRole.BUTTON, new Page.GetByRoleOptions().setName("Remove member")).click();
            Locator small = p.getByRole(AriaRole.ALERTDIALOG, new Page.GetByRoleOptions().setName("Remove Jordan?"));
            assertThat(small).isVisible();
            assertThat(small).hasAttribute("data-size", "sm");
            double smallWidth = number(small, "n => n.getBoundingClientRect().width");
            if (width > 640) assertThat(smallWidth).as("narrower than a default dialog").isLessThan(regularWidth - 40);
            assertThat(smallWidth).as("keeps a margin on a phone").isLessThanOrEqualTo(width - 32 + 0.5);

            Locator media = small.locator(".sw-alert-dialog-media");
            Locator title = small.locator(".sw-alert-dialog-title");
            assertThat(media).isVisible();
            assertThat(number(media, "n => n.getBoundingClientRect().bottom")).as("icon above the title")
                    .isLessThanOrEqualTo(number(title, "n => n.getBoundingClientRect().top") + 0.5);
            assertThat(media).hasAttribute("data-variant", "destructive");
            assertThat(media.evaluate("n => getComputedStyle(n).backgroundColor !== getComputedStyle(n.closest('dialog')).backgroundColor")).isEqualTo(true);

            Locator cancel = small.getByRole(AriaRole.BUTTON, new Locator.GetByRoleOptions().setName("Cancel"));
            Locator remove = small.locator("button[type=submit]");
            assertThat(remove).hasAccessibleName("Remove");
            assertThat(cancel).isFocused();
            assertThat(Math.abs(number(cancel, "n => n.getBoundingClientRect().width") - number(remove, "n => n.getBoundingClientRect().width")))
                    .as("equal halves").isLessThan(1);
            assertThat(Math.abs(number(cancel, "n => n.getBoundingClientRect().top") - number(remove, "n => n.getBoundingClientRect().top")))
                    .as("side by side").isLessThan(1);
            settle(p);
            p.screenshot(new Page.ScreenshotOptions().setPath(Path.of("target/quick-parity-screenshots", "confirm-" + engine + "-" + skin + "-" + mode + ".png")));
            audit(p);

            // A label too long for its half (but not for the dialog) stacks the buttons, the action on top, without
            // overflowing. Button labels never wrap, as in Shadleaf, so one wider than the whole dialog would.
            // Skins differ in type size, so take the first German label wider than half the footer but narrower than it
            assertThat(remove.evaluate("n => { const footer = n.parentElement.clientWidth;"
                    + " for (const label of ['Endgültig entfernen', 'Mitglied endgültig entfernen', 'Mitglied jetzt endgültig entfernen']) {"
                    + "   n.textContent = label; if (n.scrollWidth > footer / 2 && n.scrollWidth < footer) return true; }"
                    + " return false; }")).as("a label wider than half and narrower than the footer").isEqualTo(true);
            assertThat(number(remove, "n => n.getBoundingClientRect().bottom")).as("action on top")
                    .isLessThanOrEqualTo(number(cancel, "n => n.getBoundingClientRect().top") + 0.5);
            assertThat(small.evaluate("n => n.scrollWidth <= n.clientWidth + 1")).isEqualTo(true);
            // Tab still goes from Cancel to the action
            cancel.focus(); p.keyboard().press("Tab");
            assertThat(remove).isFocused();

            p.evaluate("document.documentElement.dir = 'rtl'");
            assertThat(small.evaluate("n => n.scrollWidth <= n.clientWidth + 1")).isEqualTo(true);
            p.keyboard().press("Escape");
            assertThat(small).isHidden();
            assertThat(errors).isEmpty();
        }
    }

    @ParameterizedTest @ValueSource(strings = {"chromium", "firefox"})
    void anEmptyStateCanLeaveOutItsOutlineAndIconsTakeAnExplicitSize(String engine) {
        try (var context = browser(engine).newContext(new Browser.NewContextOptions().setViewportSize(1280, 1000)); var p = context.newPage()) {
            var errors = errors(p);
            p.navigate(url("display", "vega", "light", "en-US"));
            Locator outlined = p.locator(".sw-empty").filter(new Locator.FilterOptions().setHasText("No projects yet"));
            Locator plain = p.locator(".sw-empty").filter(new Locator.FilterOptions().setHasText("Nothing scheduled"));
            // The skins keep a dashed style and draw it only with data-border, so the width says whether it shows
            assertThat(outlined.evaluate("n => getComputedStyle(n).borderTopStyle")).isEqualTo("dashed");
            assertThat(outlined.evaluate("n => getComputedStyle(n).borderTopWidth")).isNotEqualTo("0px");
            assertThat(plain.evaluate("n => getComputedStyle(n).borderTopWidth")).isEqualTo("0px");

            Locator star = p.getByRole(AriaRole.IMG, new Page.GetByRoleOptions().setName("Favourite"));
            assertThat(number(star, "n => n.getBoundingClientRect().width")).isEqualTo(32.0);
            assertThat(star).hasAttribute("stroke-width", "1.25");
            Locator check = p.getByRole(AriaRole.IMG, new Page.GetByRoleOptions().setName("Done"));
            assertThat(number(check, "n => n.getBoundingClientRect().height")).isEqualTo(24.0);
            // An unsized icon still follows the text
            Locator search = p.getByRole(AriaRole.IMG, new Page.GetByRoleOptions().setName("Search"));
            assertThat(number(search, "n => n.getBoundingClientRect().width"))
                    .isEqualTo(number(search, "n => parseFloat(getComputedStyle(n).fontSize)"));
            audit(p);

            // Inside a button, the skin's 16px icon size leaves an explicit size alone
            p.navigate(url("buttons", "vega", "light", "en-US"));
            Locator svg = p.locator(".sw-button[data-size='icon'][aria-label='Add'] svg");
            assertThat(number(svg, "n => n.getBoundingClientRect().width")).isEqualTo(16.0);
            svg.evaluate("n => { n.setAttribute('width', '20'); n.setAttribute('height', '20'); n.setAttribute('data-icon-size', '20'); }");
            assertThat(number(svg, "n => n.getBoundingClientRect().width")).isEqualTo(20.0);
            assertThat(errors).isEmpty();
        }
    }
}
