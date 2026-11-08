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
@SpringBootTest(webEnvironment=SpringBootTest.WebEnvironment.RANDOM_PORT)
class TagInputBrowserTest {
    static Playwright playwright;
    static Browser chromium, firefox;
    @LocalServerPort int port;
    @Autowired WebApplication app;
    @BeforeAll static void start() throws Exception {
        Files.createDirectories(Path.of("target/tag-input-screenshots"));
        playwright=Playwright.create(); chromium=playwright.chromium().launch(); firefox=playwright.firefox().launch();
    }
    @AfterAll static void stop(){playwright.close();}
    @BeforeEach void markup(){app.getMarkupSettings().setStripWicketTags(true);}
    @AfterEach void restore(){app.getMarkupSettings().setStripWicketTags(false);}
    Browser browser(String engine){return engine.equals("chromium")?chromium:firefox;}
    String url(String skin,String mode,String lang){return "http://localhost:"+port+"/tag-input?skin="+skin+"&mode="+mode+"&lang="+lang;}
    void idle(Page p){p.waitForCondition(()->(Boolean)p.evaluate("Object.keys(Wicket.channelManager.channels).length===0"));}
    Locator editor(Locator parent){return parent.locator(".sw-combobox-input");}
    Locator source(Locator parent){return parent.locator(".sw-tag-input-native");}
    Locator action(Locator parent,String name){return parent.getByRole(AriaRole.BUTTON,new Locator.GetByRoleOptions().setName(name).setExact(true));}
    void audit(Page p){
        assertThat(p.evaluate("document.documentElement.scrollWidth<=innerWidth+1")).as("page overflow").isEqualTo(true);
        assertThat(new AxeBuilder(p).withTags(List.of("wcag2a","wcag2aa","wcag21a","wcag21aa","wcag22aa")).analyze().getViolations()).isEmpty();
    }
    List<String> errors(Page p){var errors=new ArrayList<String>();p.onPageError(errors::add);p.onConsoleMessage(m->{if(m.type().equals("error"))errors.add(m.text());});return errors;}
    static Stream<Arguments> skins(){
        var selected=Arrays.asList(System.getProperty("shadwick.browser.skins","vega,lyra,nova,maia,mira,luma,rhea,sera,sketch").split(","));
        return Stream.of("chromium","firefox").flatMap(engine->Arrays.stream(Skin.values()).filter(s->selected.contains(s.id())).flatMap(s->Stream.of(Arguments.of(engine,s.id(),"light",1440),Arguments.of(engine,s.id(),"dark",320))));
    }
    @ParameterizedTest @MethodSource("skins")
    void chipsDraftsValidationResetReplacementAndNativeSubmissionAcrossSkins(String engine,String skin,String mode,int width){
        try(var context=browser(engine).newContext(new Browser.NewContextOptions().setViewportSize(width,1000).setReducedMotion(ReducedMotion.REDUCE));var p=context.newPage()){
            var errors=errors(p);var response=p.navigate(url(skin,mode,"en-US"));assertThat(response.headers().get("content-security-policy")).contains("script-src");
            var ajax=p.locator("#ajax-example");var input=editor(ajax);String id=input.getAttribute("id");
            assertThat(ajax.getByLabel("Project tags",new Locator.GetByLabelOptions().setExact(true))).hasAttribute("id",id);
            assertThat(ajax.locator(".sw-combobox-chip-label")).hasText(new String[]{"wicket","java"});
            assertThat(ajax.getByRole(AriaRole.COMBOBOX)).hasCount(0);
            input.fill("third"); action(ajax,"Save with Ajax").evaluate("n=>n.click()");
            assertThat(ajax.locator("p[role=status]")).hasText("Saved tags: wicket | java | third");idle(p);
            assertThat(input).hasValue("third");assertThat(input).hasAttribute("id",id);assertThat(input).isFocused();
            input.fill("third, fourth, fifth"); action(ajax,"Save with Ajax").evaluate("n=>n.click()");
            assertThat(input).hasAttribute("aria-invalid","true");idle(p);
            assertThat(ajax.locator(".sw-field-error")).hasText("Choose at most three tags.");assertThat(input).hasValue(" fifth");
            action(ajax,"Remove fourth").click();action(ajax,"Remove third").click();
            action(ajax,"Save with Ajax").evaluate("n=>n.click()");
            assertThat(ajax.locator("p[role=status]")).hasText("Saved tags: wicket | java | fifth");idle(p);assertThat(input).hasAttribute("aria-invalid","false");
            action(ajax,"Refresh status").evaluate("n=>n.click()");assertThat(ajax.locator("p[role=status]")).hasText("Status refreshed 1");idle(p);assertThat(input).hasValue(" fifth");
            input.focus();input.press("Enter");assertThat(input).hasValue("");assertThat(ajax.locator(".sw-combobox-chip-label")).hasText(new String[]{"wicket","java","fifth"});
            assertThat(ajax.locator("p[role=status]")).hasText("Status refreshed 1");
            input.press("Backspace");assertThat(action(ajax,"Remove fifth")).isFocused();action(ajax,"Remove fifth").press("Backspace");
            input.fill("java,");assertThat(ajax.locator(".sw-combobox-chip-label")).hasText(new String[]{"wicket","java"});
            assertThat(ajax.locator(".sw-tag-input-status")).hasText("Already added java");
            action(ajax,"Use preset tags").click();idle(p);assertThat(ajax.locator(".sw-combobox-chip-label")).hasText(new String[]{"release","ready"});
            input.fill("extra,");action(ajax,"Reset tags").click();assertThat(ajax.locator(".sw-combobox-chip-label")).hasText(new String[]{"release","ready"});
            input.fill("keep,");ajax.locator("form").evaluate("n=>n.addEventListener('reset',e=>e.preventDefault(),{once:true})");
            action(ajax,"Reset tags").click();assertThat(ajax.locator(".sw-combobox-chip-label")).hasText(new String[]{"release","ready","keep"});
            action(ajax,"Replace field").click();idle(p);assertThat(ajax.locator(".sw-combobox-control")).hasCount(1);assertThat(ajax.locator(".sw-combobox-chip-label")).hasText(new String[]{"release","ready"});
            action(ajax,"Toggle fieldset").click();idle(p);assertThat(input).isDisabled();assertThat(ajax.locator(".sw-combobox-chip-remove:enabled")).hasCount(0);
            action(ajax,"Toggle fieldset").click();idle(p);assertThat(input).isEnabled();
            var nativeForm=p.locator("#native-example");action(nativeForm,"Remove java").click();action(nativeForm,"Remove wicket").click();
            editor(nativeForm).fill(" alpha, beta ,alpha, gamma ");action(nativeForm,"Save normally").click();
            assertThat(nativeForm.locator("p[role=status]")).hasText("Saved tags: alpha | beta | gamma");assertThat(nativeForm.locator(".sw-combobox-chip-label")).hasText(new String[]{"alpha","beta","gamma"});
            assertThat(p.getByLabel("Read-only tags",new Page.GetByLabelOptions().setExact(true))).hasAttribute("readonly","");
            assertThat(p.locator("#states-example .sw-tag-input").nth(0).locator("button:enabled")).hasCount(0);
            assertThat(p.getByLabel("Disabled tags",new Page.GetByLabelOptions().setExact(true))).isDisabled();
            audit(p);assertThat(errors).isEmpty();assertThat(p.locator(".sw-tag-input[style],.sw-tag-input [style]")).hasCount(0);
            p.locator("#states-example").screenshot(new Locator.ScreenshotOptions().setPath(Path.of("target/tag-input-screenshots",engine+"-"+skin+"-"+mode+".png")));
        }
    }
    static Stream<Arguments> locales(){return Stream.of("chromium","firefox").flatMap(e->Stream.of("en-US","de-DE","fr-FR","es-ES","it-IT","ja-JP","nl-NL","pt-PT","hr-HR").map(l->Arguments.of(e,l)));}
    @ParameterizedTest @MethodSource("locales")
    void nativeInputSubmitsAndValidatesWithoutJavaScriptInEveryLocale(String engine,String lang){
        try(var context=browser(engine).newContext(new Browser.NewContextOptions().setJavaScriptEnabled(false));var p=context.newPage()){
            p.navigate(url("nova","light",lang));var form=p.locator("#native-example");var field=form.getByLabel("Project tags",new Locator.GetByLabelOptions().setExact(true));
            assertThat(form.locator(".sw-combobox-control")).hasCount(0);field.fill(", ,");action(form,"Save normally").click();assertThat(field).hasAttribute("aria-invalid","true");
            field.fill(" java, wicket,java,日本語 ");action(form,"Save normally").click();assertThat(form.locator("p[role=status]")).hasText("Saved tags: java | wicket | 日本語");
            assertThat(field).hasValue("java,wicket,日本語");
            if(!lang.startsWith("en"))assertThat(field.getAttribute("placeholder")).isNotEqualTo("Add tags…");
            assertThat(p.locator("html")).hasAttribute("lang",lang);
        }
    }
    @ParameterizedTest @ValueSource(strings={"chromium","firefox"})
    void compositionPasteExternalChangesHistoryClonesAndFieldsetChangesKeepValues(String engine){
        try(var context=browser(engine).newContext();var p=context.newPage()){
            var errors=errors(p);p.navigate(url("vega","light","en-US"));var form=p.locator("#ajax-example");var input=editor(form);
            input.focus();input.evaluate("n=>{n.dispatchEvent(new CompositionEvent('compositionstart',{bubbles:true}));n.value='日本,語';n.dispatchEvent(new InputEvent('input',{bubbles:true,isComposing:true}));n.dispatchEvent(new KeyboardEvent('keydown',{key:'Enter',isComposing:true,bubbles:true}))}");
            assertThat(form.locator(".sw-combobox-chip-label")).hasText(new String[]{"wicket","java"});assertThat(input).hasValue("日本,語");
            input.dispatchEvent("compositionend");assertThat(form.locator(".sw-combobox-chip-label")).hasText(new String[]{"wicket","java","日本"});assertThat(input).hasValue("語");
            source(form).evaluate("n=>{n.value='first, second';n.dispatchEvent(new Event('change',{bubbles:true}))}");
            assertThat(form.locator(".sw-combobox-chip-label")).hasText(new String[]{"first","second"});
            input.fill("\u00a0first\u00a0, <img src=x onerror=alert(1)>, pending");
            assertThat(form.locator(".sw-combobox-chip-label")).hasText(new String[]{"first","second","\u00a0first\u00a0","<img src=x onerror=alert(1)>"});
            assertThat(form.locator(".sw-tag-input img")).hasCount(0);
            // Clone as a history cache would: restore submitted value explicitly (cloneNode uses the current value).
            form.locator(".sw-tag-input").evaluate("n=>{const copy=n.cloneNode(true);n.replaceWith(copy);Shadwick.enhanceTagInputs()}");
            assertThat(form.locator(".sw-combobox-control")).hasCount(1);assertThat(input).hasValue(" pending");
            assertThat(form.getByLabel("Project tags",new Locator.GetByLabelOptions().setExact(true))).hasAttribute("id",input.getAttribute("id"));
            form.locator("fieldset").evaluate("n=>n.disabled=true");assertThat(input).isDisabled();
            form.locator("fieldset").evaluate("n=>n.disabled=false");assertThat(input).isEnabled();
            source(form).evaluate("n=>n.readOnly=true");assertThat(input).hasAttribute("readonly","");assertThat(form.locator("button.sw-combobox-chip-remove:enabled")).hasCount(0);
            source(form).evaluate("n=>n.readOnly=false");assertThat(input).not().hasAttribute("readonly","");
            assertThat(errors).isEmpty();
        }
    }
    static Stream<Arguments> embedded(){return Stream.of("chromium","firefox").flatMap(e->Stream.of("vega","sera","sketch").map(s->Arguments.of(e,s)));}
    @ParameterizedTest @MethodSource("embedded")
    void embeddedRtlForcedColorsKeepChipsFocusAndWrappingUsable(String engine,String skin){
        ShadwickSettings.get(app).setBundle(Bundle.EMBEDDED);
        try(var context=browser(engine).newContext(new Browser.NewContextOptions().setViewportSize(320,1000).setReducedMotion(ReducedMotion.REDUCE));var p=context.newPage()){
            var errors=errors(p);p.navigate(url(skin,"dark","en-US"));p.locator("html").evaluate("n=>n.dir='rtl'");
            var form=p.locator("#ajax-example");var input=editor(form);input.focus();input.press("Backspace");
            var last=action(form,"Remove java");assertThat(last).isFocused();last.press("ArrowRight");assertThat(action(form,"Remove wicket")).isFocused();
            action(form,"Remove wicket").press("ArrowLeft");assertThat(last).isFocused();last.press("End");assertThat(input).isFocused();
            audit(p);
            p.emulateMedia(new Page.EmulateMediaOptions().setForcedColors(ForcedColors.ACTIVE));
            last.focus();assertThat(last.evaluate("n=>getComputedStyle(n).outlineStyle")).isNotEqualTo("none");
            assertThat(form.locator(".sw-combobox-chip").first().evaluate("n=>getComputedStyle(n).borderStyle")).isEqualTo("solid");
            assertThat(p.evaluate("document.documentElement.scrollWidth<=innerWidth+1")).isEqualTo(true);assertThat(errors).isEmpty();
            p.locator("#states-example").screenshot(new Locator.ScreenshotOptions().setPath(Path.of("target/tag-input-screenshots",engine+"-"+skin+"-forced-rtl.png")));
        }finally{ShadwickSettings.get(app).setBundle(Bundle.STANDALONE);}
    }
}
