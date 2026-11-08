package dev.jbaby.shadwick.examples;

import com.deque.html.axecore.playwright.AxeBuilder;
import com.microsoft.playwright.*;
import com.microsoft.playwright.options.*;
import dev.jbaby.shadwick.*;
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
class ButtonGroupBrowserTest {
    static Playwright playwright;
    static Browser chromium, firefox;
    @LocalServerPort int port;
    @Autowired WebApplication app;
    @BeforeAll static void start() throws Exception {
        Files.createDirectories(Path.of("target/button-group-screenshots"));
        playwright = Playwright.create(); chromium = playwright.chromium().launch(); firefox = playwright.firefox().launch();
    }
    @AfterAll static void stop() { playwright.close(); }
    @BeforeEach void markup() { app.getMarkupSettings().setStripWicketTags(true); }
    @AfterEach void restore() { app.getMarkupSettings().setStripWicketTags(false); }
    Browser browser(String engine) { return engine.equals("chromium") ? chromium : firefox; }
    String url(String skin, String mode, String lang) { return "http://localhost:" + port + "/button-groups?skin=" + skin + "&mode=" + mode + "&lang=" + lang; }
    void idle(Page p) { p.waitForCondition(() -> (Boolean)p.evaluate("Object.keys(Wicket.channelManager.channels).length===0")); }
    List<String> errors(Page p) {
        var result = new ArrayList<String>(); p.onPageError(result::add);
        p.onConsoleMessage(m -> { if (m.type().equals("error")) result.add(m.text()); }); return result;
    }
    void audit(Page p) {
        assertThat(p.evaluate("document.documentElement.scrollWidth <= innerWidth + 1")).as("No horizontal overflow: %s", p.evaluate("[...document.querySelectorAll('body *')].filter(n=>{let r=n.getBoundingClientRect();return r.width&&(r.right>innerWidth+1||r.left < -1)}).slice(0,15).map(n=>[n.tagName,n.className,n.textContent.slice(0,80),n.getBoundingClientRect().width])")).isEqualTo(true);
        assertThat(new AxeBuilder(p).withTags(List.of("wcag2a", "wcag2aa", "wcag21a", "wcag21aa", "wcag22aa")).analyze().getViolations()).isEmpty();
    }
    static Stream<Arguments> skins() {
        return Stream.of("chromium", "firefox").flatMap(engine -> Arrays.stream(Skin.values()).filter(skin -> Arrays.asList(System.getProperty("shadwick.browser.skins", "vega,lyra,nova,maia,mira,luma,rhea,sera,sketch").split(",")).contains(skin.id())).flatMap(skin -> Stream.of(
                Arguments.of(engine, skin.name().toLowerCase(Locale.ROOT), "light", 1440),
                Arguments.of(engine, skin.name().toLowerCase(Locale.ROOT), "dark", 320))));
    }
    @ParameterizedTest @MethodSource("skins")
    void groupsJoinAndSplitActionsWorkWithKeyboardAcrossSkins(String engine, String skin, String mode, int width) {
        try (var context = browser(engine).newContext(new Browser.NewContextOptions().setViewportSize(width, 1050).setReducedMotion(ReducedMotion.REDUCE)); var p = context.newPage()) {
            var errors = errors(p); p.navigate(url(skin, mode, "en-US"));
            var records = p.getByRole(AriaRole.GROUP, new Page.GetByRoleOptions().setName("Browse records").setExact(true));
            var previous = records.getByRole(AriaRole.BUTTON, new Locator.GetByRoleOptions().setName("Previous"));
            previous.focus(); previous.press("Tab");
            assertThat(records.getByRole(AriaRole.BUTTON, new Locator.GetByRoleOptions().setName("Next"))).isFocused();
            assertThat(records.getByRole(AriaRole.BUTTON, new Locator.GetByRoleOptions().setName("Locked"))).isDisabled();
            assertThat(records.evaluate("n=>{let b=n.querySelectorAll('.sw-button');let r0=b[0].getBoundingClientRect(),r1=b[1].getBoundingClientRect();return Math.abs(r1.left-r0.right+parseFloat(getComputedStyle(b[1]).borderLeftWidth))<1;}")).isEqualTo(true);
            records.evaluate("n=>n.dataset.orientation='vertical'");
            assertThat(records.evaluate("n=>{let b=[...n.querySelectorAll('.sw-button')],r=b.map(x=>x.getBoundingClientRect());return r.every(x=>Math.abs(x.width-r[0].width)<1)&&Math.abs(r[1].top-r[0].bottom+parseFloat(getComputedStyle(b[1]).borderTopWidth))<1;}")).isEqualTo(true);
            records.evaluate("n=>n.dataset.orientation='horizontal'");
            var split = p.getByRole(AriaRole.GROUP, new Page.GetByRoleOptions().setName("Save revision 1").setExact(true));
            var primary = split.getByRole(AriaRole.BUTTON, new Locator.GetByRoleOptions().setName("Save revision 1").setExact(true));
            var trigger = split.getByRole(AriaRole.BUTTON, new Locator.GetByRoleOptions().setName("More actions"));
            assertThat(split.evaluate("n=>{let b=n.querySelectorAll(':scope > .sw-button');return getComputedStyle(b[0]).borderStartEndRadius==='0px'&&getComputedStyle(b[1]).borderStartStartRadius==='0px';}")).isEqualTo(true);
            assertThat(trigger.evaluate("n=>{let r=n.getBoundingClientRect();return Math.abs(r.width-r.height)<1;}")).isEqualTo(true);
            primary.click(); assertThat(p.locator("p.ex-muted[role=status]").filter(new Locator.FilterOptions().setHasText("Saved revision 1 (action 1)"))).hasCount(1);
            trigger.focus(); trigger.press("ArrowDown");
            var draft = split.getByRole(AriaRole.MENUITEM, new Locator.GetByRoleOptions().setName("Save as draft"));
            var discard = split.getByRole(AriaRole.MENUITEM, new Locator.GetByRoleOptions().setName("Discard"));
            assertThat(draft).isFocused(); draft.press("ArrowDown"); assertThat(discard).isFocused();
            discard.press("Home"); assertThat(draft).isFocused(); draft.press("End"); assertThat(discard).isFocused();
            discard.press("Escape"); assertThat(trigger).isFocused(); assertThat(split.locator(":popover-open")).hasCount(0);
            trigger.press("Enter"); assertThat(draft).isFocused(); draft.press("Enter");
            assertThat(p.locator("p.ex-muted[role=status]").filter(new Locator.FilterOptions().setHasText("Draft saved (action 2)"))).hasCount(1); idle(p);
            assertThat(trigger).isFocused(); assertThat(split.locator(":popover-open")).hasCount(0);
            p.getByRole(AriaRole.BUTTON, new Page.GetByRoleOptions().setName("Refresh actions")).click(); idle(p);
            split = p.getByRole(AriaRole.GROUP, new Page.GetByRoleOptions().setName("Save revision 2").setExact(true));
            assertThat(split.locator(":popover-open")).hasCount(0);
            trigger = split.getByRole(AriaRole.BUTTON, new Locator.GetByRoleOptions().setName("More actions"));
            trigger.focus(); trigger.press("ArrowUp");
            discard = split.getByRole(AriaRole.MENUITEM, new Locator.GetByRoleOptions().setName("Discard"));
            assertThat(discard).isFocused(); discard.press("Enter");
            assertThat(p.locator("p.ex-muted[role=status]").filter(new Locator.FilterOptions().setHasText("Discarded draft (action 3)"))).hasCount(1); idle(p);
            p.getByRole(AriaRole.BUTTON, new Page.GetByRoleOptions().setName("Toggle draft action")).click(); idle(p);
            trigger.focus(); trigger.press("ArrowDown"); assertThat(discard).isFocused(); discard.press("Escape");
            for (var button : p.getByRole(AriaRole.GROUP, new Page.GetByRoleOptions().setName("Managed action").setExact(true)).getByRole(AriaRole.BUTTON).all()) assertThat(button).isDisabled();
            assertThat(p.getByRole(AriaRole.GROUP, new Page.GetByRoleOptions().setName("Primary only").setExact(true))
                    .getByRole(AriaRole.BUTTON, new Locator.GetByRoleOptions().setName("More actions"))).isDisabled();
            p.locator("#split-heading").scrollIntoViewIfNeeded();
            p.screenshot(new Page.ScreenshotOptions().setFullPage(true).setPath(Path.of("target/button-group-screenshots", engine + "-" + skin + "-" + mode + ".png")));
            audit(p); assertThat(errors).isEmpty(); assertThat(p.locator(".sw-button-group[style], .sw-button-group [style]")).hasCount(0);
        }
    }
    @ParameterizedTest @ValueSource(strings={"chromium", "firefox"})
    void splitCommandsDoNotSubmitAndGroupedSubmitButtonsValidateAndReset(String engine) {
        try (var context = browser(engine).newContext(); var p = context.newPage()) {
            var errors = errors(p); p.navigate(url("vega", "light", "en-US"));
            var form = p.getByRole(AriaRole.FORM, new Page.GetByRoleOptions().setName("Quote actions"));
            form.getByRole(AriaRole.BUTTON, new Locator.GetByRoleOptions().setName("Preview").setExact(true)).click(); idle(p);
            assertThat(form.locator("p.ex-muted[role=status]")).hasText("No form submission yet.");
            assertThat(form.locator("[aria-invalid=true]")).hasCount(0);
            form.getByRole(AriaRole.BUTTON, new Locator.GetByRoleOptions().setName("Save (Ajax)")).click();
            var title = form.getByLabel("Quote title"); assertThat(title).hasAttribute("aria-invalid", "true");
            title.fill("Website"); form.getByRole(AriaRole.BUTTON, new Locator.GetByRoleOptions().setName("Save (Ajax)")).click();
            assertThat(form.locator("p.ex-muted[role=status]")).hasText("Saved Website (submission 1)."); idle(p);
            title.fill("Unsaved"); form.getByRole(AriaRole.BUTTON, new Locator.GetByRoleOptions().setName("Reset").setExact(true)).click();
            assertThat(title).hasValue("");
            title.fill("Store"); form.getByRole(AriaRole.BUTTON, new Locator.GetByRoleOptions().setName("Save").setExact(true)).click();
            assertThat(form.locator("p.ex-muted[role=status]")).hasText("Saved Store (submission 2).");
            audit(p); assertThat(errors).isEmpty();
        }
    }
    static Stream<Arguments> locales() {
        return Stream.of("chromium", "firefox").flatMap(engine -> Stream.of("en-US", "de-DE", "fr-FR", "es-ES", "it-IT", "ja-JP", "nl-NL", "pt-PT", "hr-HR").map(locale -> Arguments.of(engine, locale)));
    }
    @ParameterizedTest @MethodSource("locales")
    void nativeGroupedFormWorksWithoutScriptAndSplitTriggerKeepsItsLocale(String engine, String locale) {
        try (var context = browser(engine).newContext(new Browser.NewContextOptions().setJavaScriptEnabled(false)); var p = context.newPage()) {
            p.navigate(url("nova", "light", locale));
            var form = p.getByRole(AriaRole.FORM, new Page.GetByRoleOptions().setName("Quote actions"));
            form.getByLabel("Quote title").fill("Native quote");
            form.getByRole(AriaRole.BUTTON, new Locator.GetByRoleOptions().setName("Save").setExact(true)).click();
            assertThat(form.locator("p.ex-muted[role=status]")).hasText("Saved Native quote (submission 1).");
            var trigger = p.locator(".sw-split-button").first().locator(":scope > .sw-split-button-trigger");
            String name = Map.of("en", "More actions", "de", "Weitere Aktionen", "fr", "Plus d’actions", "es", "Más acciones", "it", "Altre azioni", "ja", "その他の操作", "nl", "Meer acties", "pt", "Mais ações", "hr", "Više radnji").get(locale.substring(0,2));
            assertThat(trigger).hasAttribute("aria-label", name); assertThat(p.locator("html")).hasAttribute("lang", locale);
            trigger.click(); assertThat(p.locator(":popover-open")).hasCount(1);
        }
    }
    @ParameterizedTest @ValueSource(strings={"chromium", "firefox"})
    void embeddedRtlAndForcedColorsKeepJoinsFocusAndMenuAccess(String engine) {
        ShadwickSettings.get(app).setBundle(Bundle.EMBEDDED);
        try (var context = browser(engine).newContext(new Browser.NewContextOptions().setViewportSize(320, 1050).setReducedMotion(ReducedMotion.REDUCE)); var p = context.newPage()) {
            var errors = errors(p); p.navigate(url("sketch", "dark", "en-US")); p.locator("html").evaluate("n=>n.dir='rtl'"); audit(p);
            var split = p.getByRole(AriaRole.GROUP, new Page.GetByRoleOptions().setName("Save revision 1").setExact(true));
            assertThat(split.evaluate("n=>{let b=n.querySelectorAll(':scope > .sw-button');return b[0].getBoundingClientRect().left>b[1].getBoundingClientRect().left&&getComputedStyle(b[0]).borderTopLeftRadius==='0px';}")).isEqualTo(true);
            p.emulateMedia(new Page.EmulateMediaOptions().setForcedColors(ForcedColors.ACTIVE));
            var trigger = split.getByRole(AriaRole.BUTTON, new Locator.GetByRoleOptions().setName("More actions")); trigger.focus();
            assertThat(trigger).hasCSS("outline-style", "solid");
            assertThat(trigger.evaluate("n=>getComputedStyle(n).getPropertyValue('--sw-button-group-divider').trim()")).isEqualTo("ButtonText");
            trigger.press("ArrowDown"); var draft = split.getByRole(AriaRole.MENUITEM, new Locator.GetByRoleOptions().setName("Save as draft"));
            assertThat(draft).isFocused(); draft.press("Escape"); assertThat(trigger).isFocused();
            auditOverflow(p); assertThat(errors).isEmpty();
            p.screenshot(new Page.ScreenshotOptions().setFullPage(true).setPath(Path.of("target/button-group-screenshots", engine + "-forced-rtl.png")));
        } finally { ShadwickSettings.get(app).setBundle(Bundle.STANDALONE); }
    }
    void auditOverflow(Page p) { assertThat(p.evaluate("document.documentElement.scrollWidth <= innerWidth + 1")).isEqualTo(true); }
}
