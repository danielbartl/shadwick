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
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Stream;

import static com.microsoft.playwright.assertions.PlaywrightAssertions.assertThat;
import static org.assertj.core.api.Assertions.assertThat;

@Tag("browser")
@SpringBootTest(webEnvironment=SpringBootTest.WebEnvironment.RANDOM_PORT)
class LazyLoadBrowserTest {
    static Playwright playwright;
    static Browser chromium,firefox;
    @LocalServerPort int port;
    @Autowired WebApplication app;
    @BeforeAll static void start() throws Exception {
        Files.createDirectories(Path.of("target/lazy-load-screenshots"));
        playwright=Playwright.create();chromium=playwright.chromium().launch();firefox=playwright.firefox().launch();
    }
    @AfterAll static void stop(){playwright.close();}
    @BeforeEach void markup(){app.getMarkupSettings().setStripWicketTags(true);}
    @AfterEach void restore(){app.getMarkupSettings().setStripWicketTags(false);}
    Browser browser(String engine){return engine.equals("chromium")?chromium:firefox;}
    String url(String skin,String mode,String lang){return "http://localhost:"+port+"/lazy-load?skin="+skin+"&mode="+mode+"&lang="+lang;}
    void idle(Page p){p.waitForCondition(()->(Boolean)p.evaluate("Object.keys(Wicket.channelManager.channels).length===0"));}
    List<String> errors(Page p){
        var result=new ArrayList<String>();p.onPageError(result::add);
        p.onConsoleMessage(m->{if(m.type().equals("error"))result.add(m.text());});return result;
    }
    List<Route> holdFirstAjax(Page p){
        var held=new ArrayList<Route>();var first=new AtomicBoolean(true);
        p.route("**/*",route->{
            if("true".equals(route.request().headers().get("wicket-ajax"))&&first.compareAndSet(true,false))held.add(route);
            else route.resume();
        });return held;
    }
    void audit(Page p){
        assertThat(p.evaluate("document.documentElement.scrollWidth<=innerWidth+1")).as("Overflow: %s",p.evaluate("[...document.querySelectorAll('body *')].filter(n=>{let r=n.getBoundingClientRect();return r.width&&(r.right>innerWidth+1||r.left < -1)}).slice(0,10).map(n=>[n.tagName,n.className,n.getBoundingClientRect().width])")).isEqualTo(true);
        assertThat(new AxeBuilder(p).withTags(List.of("wcag2a","wcag2aa","wcag21a","wcag21aa","wcag22aa")).analyze().getViolations()).isEmpty();
    }
    static Stream<Arguments> skins(){
        var selected=Arrays.asList(System.getProperty("shadwick.browser.skins","vega,lyra,nova,maia,mira,luma,rhea,sera,sketch").split(","));
        return Stream.of("chromium","firefox").flatMap(engine->Arrays.stream(Skin.values()).filter(s->selected.contains(s.id())).flatMap(s->Stream.of(
                Arguments.of(engine,s.id(),"light",1440),Arguments.of(engine,s.id(),"dark",320))));
    }
    @ParameterizedTest @MethodSource("skins")
    void skeletonsLoadFormsReadinessReplacementAndRetryAcrossSkins(String engine,String skin,String mode,int width){
        try(var context=browser(engine).newContext(new Browser.NewContextOptions().setViewportSize(width,1000).setReducedMotion(ReducedMotion.REDUCE));var p=context.newPage()){
            var errors=errors(p);var held=holdFirstAjax(p);var response=p.navigate(url(skin,mode,"en-US"));
            assertThat(response.headers().get("content-security-policy")).contains("script-src");
            p.waitForCondition(()->!held.isEmpty());
            var revenue=p.locator("#revenue-example");var queued=p.locator("#queued-example");var recovery=p.locator("#recovery-example");
            assertThat(revenue.locator(".sw-lazy-load")).hasAttribute("aria-busy","true");
            assertThat(revenue.locator(".sw-skeleton")).hasCount(3);
            assertThat(revenue.locator(".sw-skeleton").first()).hasAttribute("aria-hidden","true");
            assertThat(revenue.getByRole(AriaRole.STATUS)).hasText("Loading content…");
            assertThat(revenue.getByRole(AriaRole.FORM)).hasCount(0);
            assertThat(revenue.getByRole(AriaRole.LINK,new Locator.GetByRoleOptions().setName("Load content").setExact(true))).hasCount(0);
            audit(p);
            revenue.screenshot(new Locator.ScreenshotOptions().setPath(Path.of("target/lazy-load-screenshots",engine+"-"+skin+"-"+mode+"-pending.png")));
            held.remove(0).resume();
            assertThat(revenue.locator(".sw-lazy-load")).hasAttribute("aria-busy","false");
            assertThat(revenue.getByRole(AriaRole.HEADING,new Locator.GetByRoleOptions().setName("Revenue report").setExact(true))).isVisible();
            assertThat(revenue.locator(".sw-skeleton")).hasCount(0);
            assertThat(recovery.getByRole(AriaRole.ALERT)).containsText("Report unavailable");idle(p);audit(p);
            var seats=revenue.getByLabel("Seats",new Locator.GetByLabelOptions().setExact(true));String inputId=seats.getAttribute("id");
            seats.fill("0");revenue.getByRole(AriaRole.BUTTON,new Locator.GetByRoleOptions().setName("Update report").setExact(true)).click();
            assertThat(seats).hasAttribute("aria-invalid","true");idle(p);
            seats.fill("20");revenue.getByRole(AriaRole.BUTTON,new Locator.GetByRoleOptions().setName("Update report").setExact(true)).click();
            assertThat(revenue.locator("p[role=status]")).hasText("Seats 20; projected total 2510.00.");idle(p);
            queued.getByRole(AriaRole.BUTTON,new Locator.GetByRoleOptions().setName("Replace queued panel").setExact(true)).click();idle(p);
            assertThat(queued.locator(".sw-lazy-load")).hasAttribute("aria-busy","true");
            var ready=queued.getByRole(AriaRole.BUTTON,new Locator.GetByRoleOptions().setName("Complete queued report").setExact(true));ready.focus();ready.press("Enter");
            assertThat(queued.getByRole(AriaRole.HEADING,new Locator.GetByRoleOptions().setName("Queued report 2").setExact(true))).isVisible();idle(p);assertThat(ready).isFocused();
            recovery.getByRole(AriaRole.BUTTON,new Locator.GetByRoleOptions().setName("Retry report").setExact(true)).click();
            assertThat(recovery.getByRole(AriaRole.HEADING,new Locator.GetByRoleOptions().setName("Recovered report").setExact(true))).isVisible();idle(p);
            assertThat(recovery.getByRole(AriaRole.ALERT)).hasCount(0);
            // No panels remain pending here; adding another must start a fresh page timer.
            queued.getByRole(AriaRole.BUTTON,new Locator.GetByRoleOptions().setName("Replace queued panel").setExact(true)).click();idle(p);
            assertThat(queued.locator(".sw-lazy-load")).hasAttribute("aria-busy","true");ready.click();
            assertThat(queued.getByRole(AriaRole.HEADING,new Locator.GetByRoleOptions().setName("Queued report 3").setExact(true))).isVisible();idle(p);
            assertThat(seats).hasAttribute("id",inputId);assertThat(seats).hasValue("20");
            assertThat(p.locator("#main .sw-lazy-load[aria-busy=true]")).hasCount(0);
            audit(p);assertThat(errors).isEmpty();assertThat(p.locator(".sw-lazy-load[style],.sw-lazy-load [style]")).hasCount(0);
            revenue.screenshot(new Locator.ScreenshotOptions().setPath(Path.of("target/lazy-load-screenshots",engine+"-"+skin+"-"+mode+"-loaded.png")));
        }
    }
    static Stream<Arguments> locales(){
        return Stream.of("chromium","firefox").flatMap(engine->Stream.of("en-US","de-DE","fr-FR","es-ES","it-IT","ja-JP","nl-NL","pt-PT","hr-HR").map(locale->Arguments.of(engine,locale)));
    }
    @ParameterizedTest @MethodSource("locales")
    void nativeFallbackAndLoadedFormsWorkWithoutScriptInEveryLocale(String engine,String locale){
        try(var context=browser(engine).newContext(new Browser.NewContextOptions().setJavaScriptEnabled(false));var p=context.newPage()){
            p.navigate(url("nova","light",locale));var revenue=p.locator("#revenue-example");var queued=p.locator("#queued-example");
            String load=Map.of("en","Load content","de","Inhalt laden","fr","Charger le contenu","es","Cargar contenido","it","Carica contenuto","ja","コンテンツを読み込む","nl","Inhoud laden","pt","Carregar conteúdo","hr","Učitaj sadržaj").get(locale.substring(0,2));
            assertThat(revenue.locator(".sw-lazy-load")).hasAttribute("aria-busy","true");
            queued.getByRole(AriaRole.LINK,new Locator.GetByRoleOptions().setName(load).setExact(true)).click();
            assertThat(queued.locator(".sw-lazy-load")).hasAttribute("aria-busy","true");
            revenue.getByRole(AriaRole.LINK,new Locator.GetByRoleOptions().setName(load).setExact(true)).click();
            assertThat(revenue.locator(".sw-lazy-load")).hasAttribute("aria-busy","false");
            revenue.getByLabel("Seats",new Locator.GetByLabelOptions().setExact(true)).fill("5");
            revenue.getByRole(AriaRole.BUTTON,new Locator.GetByRoleOptions().setName("Update normally").setExact(true)).click();
            assertThat(revenue.locator("p[role=status]")).hasText("Seats 5; projected total 627.50.");
            assertThat(p.locator("html")).hasAttribute("lang",locale);
        }
    }
    static Stream<Arguments> embedded(){return Stream.of("chromium","firefox").flatMap(engine->Stream.of("vega","sera","sketch").map(skin->Arguments.of(engine,skin)));}
    @ParameterizedTest @MethodSource("embedded")
    void embeddedRtlForcedColorsAndReducedMotionKeepTheLoadingStateLegible(String engine,String skin){
        ShadwickSettings.get(app).setBundle(Bundle.EMBEDDED);
        try(var context=browser(engine).newContext(new Browser.NewContextOptions().setViewportSize(320,1000).setReducedMotion(ReducedMotion.REDUCE));var p=context.newPage()){
            var errors=errors(p);var held=holdFirstAjax(p);p.navigate(url(skin,"dark","en-US"));p.waitForCondition(()->!held.isEmpty());
            p.locator("html").evaluate("n=>n.dir='rtl'");audit(p);
            var revenue=p.locator("#revenue-example");var skeleton=revenue.locator(".sw-skeleton").first();
            assertThat(skeleton).hasCSS("animation-name","none");
            p.emulateMedia(new Page.EmulateMediaOptions().setForcedColors(ForcedColors.ACTIVE));
            assertThat(revenue.getByRole(AriaRole.STATUS)).hasText("Loading content…");assertThat(skeleton).hasAttribute("aria-hidden","true");
            revenue.screenshot(new Locator.ScreenshotOptions().setPath(Path.of("target/lazy-load-screenshots",engine+"-"+skin+"-forced-rtl.png")));
            held.remove(0).resume();assertThat(revenue.locator(".sw-lazy-load")).hasAttribute("aria-busy","false");
            var seats=revenue.getByLabel("Seats",new Locator.GetByLabelOptions().setExact(true));seats.focus();assertThat(seats).isFocused();
            assertThat(errors).isEmpty();
        }finally{ShadwickSettings.get(app).setBundle(Bundle.STANDALONE);}
    }
}
