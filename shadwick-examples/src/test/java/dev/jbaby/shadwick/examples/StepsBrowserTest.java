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
class StepsBrowserTest {
    static Playwright playwright;
    static Browser chromium,firefox;
    @LocalServerPort int port;
    @Autowired WebApplication app;
    @BeforeAll static void start() throws Exception {
        Files.createDirectories(Path.of("target/steps-screenshots"));playwright=Playwright.create();chromium=playwright.chromium().launch();firefox=playwright.firefox().launch();
    }
    @AfterAll static void stop(){playwright.close();}
    @BeforeEach void markup(){app.getMarkupSettings().setStripWicketTags(true);}
    @AfterEach void restore(){app.getMarkupSettings().setStripWicketTags(false);}
    Browser browser(String engine){return engine.equals("chromium")?chromium:firefox;}
    String url(String skin,String mode,String lang){return "http://localhost:"+port+"/steps?skin="+skin+"&mode="+mode+"&lang="+lang;}
    void idle(Page p){p.waitForCondition(()->(Boolean)p.evaluate("Object.keys(Wicket.channelManager.channels).length===0"));}
    Locator action(Locator root,String name){return root.getByRole(AriaRole.BUTTON,new Locator.GetByRoleOptions().setName(name).setExact(true));}
    List<String> errors(Page p){var errors=new ArrayList<String>();p.onPageError(errors::add);p.onConsoleMessage(m->{if(m.type().equals("error"))errors.add(m.text());});return errors;}
    void audit(Page p){
        assertThat(p.evaluate("document.documentElement.scrollWidth<=innerWidth+1")).as("Overflow: %s",p.evaluate("[...document.querySelectorAll('body *')].filter(n=>{const r=n.getBoundingClientRect();return r.width&&(r.right>innerWidth+1||r.left < -1)}).slice(0,24).map(n=>[n.tagName,n.className,n.getBoundingClientRect().left,n.getBoundingClientRect().right])")).isEqualTo(true);
        assertThat(new AxeBuilder(p).withTags(List.of("wcag2a","wcag2aa","wcag21a","wcag21aa","wcag22aa")).analyze().getViolations()).isEmpty();
    }
    void layout(Page p){
        var shipping=p.locator("#shipping-example");var first=shipping.locator(".sw-step-number").nth(0).boundingBox();var second=shipping.locator(".sw-step-number").nth(1).boundingBox();
        if(shipping.locator(".sw-steps-shell").boundingBox().width>=512){assertThat(first.y).isEqualTo(second.y);assertThat(Math.abs(first.x-second.x)).isGreaterThan(first.width);}
        else{assertThat(first.x).isEqualTo(second.x);assertThat(second.y).isGreaterThan(first.y+first.height);}
        var compact=p.locator("#compact-example");var a=compact.locator(".sw-step-number").nth(0).boundingBox();var b=compact.locator(".sw-step-number").nth(1).boundingBox();assertThat(a.x).isEqualTo(b.x);assertThat(b.y).isGreaterThan(a.y+a.height);
        assertThat(p.locator("#long-example .sw-step-title").nth(2)).hasText("<script>literal title</script>");assertThat(p.locator("#long-example script")).hasCount(0);
        assertThat(p.locator("#states-example .sw-steps").evaluateAll("lists=>lists.every(n=>n.querySelectorAll('[aria-current=step]').length<=1)")).isEqualTo(true);
    }
    static Stream<Arguments> skins(){var selected=Arrays.asList(System.getProperty("shadwick.browser.skins","vega,lyra,nova,maia,mira,luma,rhea,sera,sketch").split(","));return Stream.of("chromium","firefox").flatMap(e->Arrays.stream(Skin.values()).filter(s->selected.contains(s.id())).flatMap(s->Stream.of(Arguments.of(e,s.id(),"light",1440),Arguments.of(e,s.id(),"dark",320))));}
    @ParameterizedTest @MethodSource("skins")
    void statesLayoutValidationAndCompletedStepNavigationWorkAcrossSkins(String engine,String skin,String mode,int width){
        try(var context=browser(engine).newContext(new Browser.NewContextOptions().setViewportSize(width,1000).setReducedMotion(ReducedMotion.REDUCE));var p=context.newPage()){
            var errors=errors(p);var response=p.navigate(url(skin,mode,"en-US"));assertThat(response.headers().get("content-security-policy")).contains("script-src");layout(p);audit(p);
            assertThat(p.locator("#shipping-example .sw-step").evaluateAll("nodes=>nodes.map(n=>n.dataset.state)")).isEqualTo(List.of("done","done","current","upcoming"));
            assertThat(p.locator("#shipping-example .sw-step[aria-current=step] .sw-step-title")).hasText("Shipped");
            assertThat(p.locator("#shipping-example").getByRole(AriaRole.LINK)).hasCount(0);
            var flow=p.locator("#workflow-example");var input=flow.getByLabel("Project name",new Locator.GetByLabelOptions().setExact(true));String id=input.getAttribute("id");
            assertThat(flow.getByRole(AriaRole.LINK)).hasCount(1);assertThat(flow.locator(".sw-step[aria-current=step] .sw-step-title")).hasText("Review");
            input.fill("");action(flow,"Next with Ajax").click();assertThat(input).hasAttribute("aria-invalid","true");idle(p);assertThat(flow.locator(".sw-step[aria-current=step] .sw-step-title")).hasText("Review");
            var back=flow.getByRole(AriaRole.LINK);input.focus();input.press("Shift+Tab");assertThat(back).isFocused();assertThat(back.evaluate("n=>getComputedStyle(n).outlineStyle")).isEqualTo("solid");back.press("Enter");
            assertThat(flow.locator("p[role=status]")).hasText("Returned to Details.");idle(p);assertThat(input).isFocused();assertThat(input).hasValue("");assertThat(input).hasAttribute("id",id);
            input.fill("Draft project");action(flow,"Next with Ajax").click();assertThat(flow.locator(".sw-step[aria-current=step] .sw-step-title")).hasText("Review");idle(p);assertThat(input).hasAttribute("aria-invalid","false");
            action(flow,"Next with Ajax").click();assertThat(flow.locator(".sw-step[aria-current=step] .sw-step-title")).hasText("Publish");idle(p);
            action(flow,"Next with Ajax").click();assertThat(flow.locator("p[role=status]")).hasText("All stages complete.");idle(p);assertThat(flow.locator(".sw-step[aria-current]")).hasCount(0);assertThat(flow.getByRole(AriaRole.LINK)).hasCount(3);
            action(flow,"Replace indicator").click();idle(p);assertThat(flow.locator(".sw-steps")).hasCount(1);assertThat(flow.getByRole(AriaRole.LINK)).hasCount(3);assertThat(input).hasValue("Draft project");
            action(flow,"Start again").click();idle(p);assertThat(flow.locator(".sw-step[aria-current=step] .sw-step-title")).hasText("Details");assertThat(flow.getByRole(AriaRole.LINK)).hasCount(0);
            action(flow,"Next normally").click();assertThat(flow.locator(".sw-step[aria-current=step] .sw-step-title")).hasText("Review");assertThat(input).hasValue("Draft project");
            assertThat(p.locator(".sw-steps[style],.sw-steps [style]")).hasCount(0);layout(p);audit(p);assertThat(errors).isEmpty();
            p.locator("#shipping-example").screenshot(new Locator.ScreenshotOptions().setPath(Path.of("target/steps-screenshots",engine+"-"+skin+"-"+mode+".png")));
        }
    }
    static Stream<Arguments> locales(){return Stream.of("chromium","firefox").flatMap(e->Stream.of("en-US","de-DE","fr-FR","es-ES","it-IT","ja-JP","nl-NL","pt-PT","hr-HR").map(l->Arguments.of(e,l)));}
    @ParameterizedTest @MethodSource("locales")
    void noScriptShowsLocalizedStatesWithoutFakeLinksAndNativeFormsStillAdvance(String engine,String locale){
        try(var context=browser(engine).newContext(new Browser.NewContextOptions().setJavaScriptEnabled(false));var p=context.newPage()){
            p.navigate(url("nova","light",locale));var flow=p.locator("#workflow-example");
            assertThat(flow.getByRole(AriaRole.LINK)).hasCount(0);assertThat(flow.locator("noscript .sw-step-link")).hasCount(1);assertThat(flow.locator("noscript .sw-step-title")).isVisible();
            var input=flow.getByLabel("Project name",new Locator.GetByLabelOptions().setExact(true));input.fill("");action(flow,"Next normally").click();assertThat(input).hasAttribute("aria-invalid","true");
            input.fill("Native project");action(flow,"Next normally").click();assertThat(flow.locator(".sw-step[aria-current=step] .sw-step-title")).hasText("Publish");assertThat(flow.getByRole(AriaRole.LINK)).hasCount(0);
            String label=Map.of("en","Steps","de","Schritte","fr","Étapes","es","Pasos","it","Passaggi","ja","ステップ","nl","Stappen","pt","Etapas","hr","Koraci").get(locale.substring(0,2));
            assertThat(p.locator("#states-example .sw-steps").first()).hasAttribute("aria-label",label);assertThat(p.locator("html")).hasAttribute("lang",locale);
            if(!locale.startsWith("en"))assertThat(p.locator("#shipping-example .sw-step-status").first().textContent()).isNotEqualTo("Completed");
        }
    }
    static Stream<Arguments> embedded(){return Stream.of("chromium","firefox").flatMap(e->Stream.of("vega","sera","sketch").map(s->Arguments.of(e,s)));}
    @ParameterizedTest @MethodSource("embedded")
    void embeddedRtlForcedColorsAndPrintingKeepCurrentStateAndConnectorsVisible(String engine,String skin){
        ShadwickSettings.get(app).setBundle(Bundle.EMBEDDED);
        try(var context=browser(engine).newContext(new Browser.NewContextOptions().setViewportSize(320,1000).setReducedMotion(ReducedMotion.REDUCE));var p=context.newPage()){
            var errors=errors(p);p.navigate(url(skin,"dark","en-US"));p.locator("html").evaluate("n=>n.dir='rtl'");layout(p);audit(p);
            var first=p.locator("#shipping-example .sw-step").first();
            assertThat(first.evaluate("n=>parseFloat(getComputedStyle(n,'::after').right)<parseFloat(getComputedStyle(n,'::after').left)")).isEqualTo(true);
            p.emulateMedia(new Page.EmulateMediaOptions().setForcedColors(ForcedColors.ACTIVE));
            var current=p.locator("#shipping-example .sw-step[aria-current=step] .sw-step-number");assertThat(current.evaluate("n=>getComputedStyle(n).borderTopWidth")).isEqualTo("3px");
            var back=p.locator("#workflow-example").getByRole(AriaRole.LINK);back.focus();assertThat(back).isFocused();assertThat(back.evaluate("n=>getComputedStyle(n).outlineStyle")).isEqualTo("solid");
            assertThat(first.evaluate("n=>getComputedStyle(n,'::after').borderInlineStartStyle")).isEqualTo(skin.equals("sketch")?"dashed":"solid");
            p.locator("#shipping-example").screenshot(new Locator.ScreenshotOptions().setPath(Path.of("target/steps-screenshots",engine+"-"+skin+"-forced-rtl.png")));
            p.emulateMedia(new Page.EmulateMediaOptions().setForcedColors(ForcedColors.NONE).setMedia(Media.PRINT));
            assertThat(current.evaluate("n=>getComputedStyle(n).boxShadow")).isEqualTo("none");assertThat(first.evaluate("n=>getComputedStyle(n).breakInside")).isEqualTo("avoid");
            assertThat(first.locator(".sw-step-status").evaluate("n=>getComputedStyle(n).color")).isEqualTo("rgb(0, 0, 0)");assertThat(errors).isEmpty();
        }finally{ShadwickSettings.get(app).setBundle(Bundle.STANDALONE);}
    }
}
