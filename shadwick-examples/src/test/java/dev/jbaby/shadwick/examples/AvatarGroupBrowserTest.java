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
class AvatarGroupBrowserTest {
    static Playwright playwright;
    static Browser chromium,firefox;
    @LocalServerPort int port;
    @Autowired WebApplication app;
    @BeforeAll static void start() throws Exception {
        Files.createDirectories(Path.of("target/avatar-group-screenshots"));
        playwright=Playwright.create();chromium=playwright.chromium().launch();firefox=playwright.firefox().launch();
    }
    @AfterAll static void stop(){playwright.close();}
    @BeforeEach void markup(){app.getMarkupSettings().setStripWicketTags(true);}
    @AfterEach void restore(){app.getMarkupSettings().setStripWicketTags(false);}
    Browser browser(String engine){return engine.equals("chromium")?chromium:firefox;}
    String url(String skin,String mode,String lang){return "http://localhost:"+port+"/avatar-groups?skin="+skin+"&mode="+mode+"&lang="+lang;}
    void idle(Page p){p.waitForCondition(()->(Boolean)p.evaluate("Object.keys(Wicket.channelManager.channels).length===0"));}
    Locator action(Page p,String name){return p.getByRole(AriaRole.BUTTON,new Page.GetByRoleOptions().setName(name).setExact(true));}
    Locator link(Page p,String name){return p.getByRole(AriaRole.LINK,new Page.GetByRoleOptions().setName(name).setExact(true));}
    List<String> errors(Page p){var errors=new ArrayList<String>();p.onPageError(errors::add);p.onConsoleMessage(m->{if(m.type().equals("error"))errors.add(m.text());});return errors;}
    void audit(Page p){
        assertThat(p.evaluate("document.documentElement.scrollWidth<=innerWidth+1")).as("page overflow").isEqualTo(true);
        assertThat(new AxeBuilder(p).withTags(List.of("wcag2a","wcag2aa","wcag21a","wcag21aa","wcag22aa")).analyze().getViolations()).isEmpty();
    }
    void geometry(Page p){
        for(int i=0;i<3;i++){
            var group=p.locator("#sizes-example .sw-avatar-group").nth(i);double size=new double[]{24,32,40}[i];
            assertThat(group.locator(".sw-avatar").first().boundingBox().width).isEqualTo(size);
            assertThat(group.locator(".sw-avatar").first().boundingBox().height).isEqualTo(size);
            assertThat(group.locator(".sw-avatar-fallback").first().evaluate("n=>getComputedStyle(n).fontSize")).isEqualTo(i==0?"12px":"14px");
            var first=group.locator(".sw-avatar").nth(0).boundingBox();var next=group.locator(".sw-avatar").nth(1).boundingBox();
            assertThat(Math.abs(next.x-first.x)).isEqualTo(size*.75);
            assertThat(first.y).isEqualTo(next.y);
            assertThat(group).hasAttribute("role","list");
        }
        assertThat(p.locator("#wrapping-example > ul.sw-avatar-group").evaluate("n=>{const r=n.getBoundingClientRect();return [...n.querySelectorAll('.sw-avatar')].every(a=>{const b=a.getBoundingClientRect();return b.left>=r.left&&b.right<=r.right&&b.top>=r.top&&b.bottom<=r.bottom})}")).isEqualTo(true);
    }
    static Stream<Arguments> skins(){
        var selected=Arrays.asList(System.getProperty("shadwick.browser.skins","vega,lyra,nova,maia,mira,luma,rhea,sera,sketch").split(","));
        return Stream.of("chromium","firefox").flatMap(e->Arrays.stream(Skin.values()).filter(s->selected.contains(s.id())).flatMap(s->Stream.of(Arguments.of(e,s.id(),"light",1440),Arguments.of(e,s.id(),"dark",320))));
    }
    @ParameterizedTest @MethodSource("skins")
    void sizesOverlapLinksFallbacksAndAjaxUpdatesWorkAcrossSkins(String engine,String skin,String mode,int width){
        try(var context=browser(engine).newContext(new Browser.NewContextOptions().setViewportSize(width,1000).setReducedMotion(ReducedMotion.REDUCE));var p=context.newPage()){
            var errors=errors(p);var response=p.navigate(url(skin,mode,"en-US"));assertThat(response.headers().get("content-security-policy")).contains("script-src");
            var first=p.locator("#sizes-example .sw-avatar").first();assertThat(first.locator("img")).hasAttribute("data-status","loaded");assertThat(first.locator(".sw-avatar-fallback")).isHidden();
            geometry(p);audit(p);
            p.route("**/avatar-group-broken",route->route.fulfill(new Route.FulfillOptions().setStatus(200).setContentType("image/svg+xml").setBody("not an image")));
            first.locator("img").evaluate("n=>n.src='/avatar-group-broken'");assertThat(first.locator("img")).hasAttribute("data-status","error");assertThat(first.locator(".sw-avatar-fallback")).isVisible();
            var member=link(p,"View AL: Ada Lovelace");member.focus();assertThat(member).isFocused();
            assertThat(member.evaluate("n=>getComputedStyle(n.parentElement).zIndex")).isEqualTo("10");
            assertThat(member.evaluate("n=>getComputedStyle(n).outlineStyle")).isEqualTo("solid");member.press("Enter");assertThat(p.locator("#team-example p[role=status]")).hasText("Selected Ada Lovelace.");
            link(p,"Show 2 more members (+2)").click();assertThat(p.locator("#team-example .sw-avatar-group > li")).hasCount(5);assertThat(link(p,"View SM: Sofía Martínez")).isVisible();
            assertThat(link(p,"Show 2 more members (+2)")).hasCount(0);
            action(p,"Clear reviewers").click();assertThat(p.locator("#dynamic-example .sw-avatar")).hasCount(0);idle(p);assertThat(p.locator("#dynamic-example")).containsText("No reviewers assigned.");
            action(p,"Restore reviewers").click();assertThat(p.locator("#dynamic-example .sw-avatar")).hasCount(3);idle(p);assertThat(p.locator("#dynamic-example img")).hasAttribute("data-status","loaded");
            action(p,"Replace reviewer group").click();idle(p);assertThat(p.locator("#dynamic-example .sw-avatar-group")).hasAttribute("data-size","lg");assertThat(p.locator("#dynamic-example .sw-avatar").first().boundingBox().width).isEqualTo(40);
            assertThat(p.locator(".sw-avatar-group[style],.sw-avatar-group [style]")).hasCount(0);audit(p);assertThat(errors).isEmpty();
            p.locator("#sizes-example").screenshot(new Locator.ScreenshotOptions().setPath(Path.of("target/avatar-group-screenshots",engine+"-"+skin+"-"+mode+".png")));
        }
    }
    @ParameterizedTest @ValueSource(strings={"chromium","firefox"})
    void delayedImagesKeepNamedFallbacksUntilTheyLoad(String engine){
        try(var context=browser(engine).newContext();var p=context.newPage()){
            var held=new ArrayList<Route>();p.route("**/*avatar*svg*",held::add);
            p.navigate(url("vega","light","en-US"),new Page.NavigateOptions().setWaitUntil(WaitUntilState.DOMCONTENTLOADED));
            p.waitForCondition(()->!held.isEmpty());var avatar=p.locator("#sizes-example .sw-avatar").first();
            assertThat(avatar.locator(".sw-avatar-fallback")).isVisible();assertThat(avatar.locator(".sw-avatar-fallback")).hasAttribute("aria-label","Ada Lovelace");
            assertThat(avatar.locator("img")).isHidden();held.forEach(Route::resume);
            assertThat(avatar.locator("img")).hasAttribute("data-status","loaded");assertThat(avatar.locator(".sw-avatar-fallback")).isHidden();
        }
    }
    static Stream<Arguments> locales(){return Stream.of("chromium","firefox").flatMap(e->Stream.of("en-US","de-DE","fr-FR","es-ES","it-IT","ja-JP","nl-NL","pt-PT","hr-HR").map(l->Arguments.of(e,l)));}
    @ParameterizedTest @MethodSource("locales")
    void nativeMemberLinksAndOverflowWorkWithoutScriptInEveryLocale(String engine,String locale){
        try(var context=browser(engine).newContext(new Browser.NewContextOptions().setJavaScriptEnabled(false));var p=context.newPage()){
            p.navigate(url("nova","light",locale));geometry(p);link(p,"View GH: Grace Hopper").click();assertThat(p.locator("#team-example p[role=status]")).hasText("Selected Grace Hopper.");
            link(p,"Show 2 more members (+2)").click();assertThat(p.locator("#team-example .sw-avatar-group > li")).hasCount(5);assertThat(p.locator("html")).hasAttribute("lang",locale);
        }
    }
    static Stream<Arguments> embedded(){return Stream.of("chromium","firefox").flatMap(e->Stream.of("vega","sera","sketch").map(s->Arguments.of(e,s)));}
    @ParameterizedTest @MethodSource("embedded")
    void embeddedRtlWrappingAndForcedColorsKeepLinksAndMemberOutlinesVisible(String engine,String skin){
        ShadwickSettings.get(app).setBundle(Bundle.EMBEDDED);
        try(var context=browser(engine).newContext(new Browser.NewContextOptions().setViewportSize(320,1000).setReducedMotion(ReducedMotion.REDUCE));var p=context.newPage()){
            var errors=errors(p);p.navigate(url(skin,"dark","en-US"));p.locator("html").evaluate("n=>n.dir='rtl'");geometry(p);audit(p);
            var group=p.locator("#wrapping-example > ul.sw-avatar-group");
            assertThat(group.evaluate("n=>{const rows=new Map();for(const a of n.querySelectorAll('.sw-avatar')){const r=a.getBoundingClientRect();if(!rows.has(r.top))rows.set(r.top,r.right)}const edges=[...rows.values()];return edges.length>1&&edges.every(x=>Math.abs(x-edges[0])<1)}")).isEqualTo(true);
            var first=p.locator("#sizes-example .sw-avatar").nth(0).boundingBox();var second=p.locator("#sizes-example .sw-avatar").nth(1).boundingBox();assertThat(second.x).isLessThan(first.x);
            p.emulateMedia(new Page.EmulateMediaOptions().setForcedColors(ForcedColors.ACTIVE));var member=link(p,"View GH: Grace Hopper");member.focus();assertThat(member).isFocused();assertThat(member.evaluate("n=>getComputedStyle(n).outlineStyle")).isEqualTo("solid");
            assertThat(p.locator("#team-example .sw-avatar").first().evaluate("n=>getComputedStyle(n).borderStyle")).isEqualTo("solid");
            assertThat(p.evaluate("document.documentElement.scrollWidth<=innerWidth+1")).isEqualTo(true);assertThat(errors).isEmpty();
            p.locator("#wrapping-example").screenshot(new Locator.ScreenshotOptions().setPath(Path.of("target/avatar-group-screenshots",engine+"-"+skin+"-forced-rtl.png")));
        }finally{ShadwickSettings.get(app).setBundle(Bundle.STANDALONE);}
    }
}
