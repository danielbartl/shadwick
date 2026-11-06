package dev.jbaby.shadwick.examples;

import com.deque.html.axecore.playwright.AxeBuilder;
import com.github.openjson.JSONArray;
import com.microsoft.playwright.*;
import com.microsoft.playwright.options.AriaRole;
import com.microsoft.playwright.options.ColorScheme;
import com.microsoft.playwright.options.ReducedMotion;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.ValueSource;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.stream.IntStream;
import java.util.stream.Stream;
import static com.microsoft.playwright.assertions.PlaywrightAssertions.assertThat;
import static org.assertj.core.api.Assertions.assertThat;

/** Static docs need no Wicket server. Reuses the existing build, preview and browser-test dependencies. */
@Tag("browser")
class DocumentationBrowserTest {
    private static final List<String> WCAG=List.of("wcag2a","wcag2aa","wcag21a","wcag21aa","wcag22aa");
    private static final Path ROOT=(Files.isDirectory(Path.of("site"))?Path.of("."):Path.of("..")).toAbsolutePath().normalize();
    @TempDir static Path temporary;
    private static Playwright playwright;
    private static Browser browser;
    private static Process preview;
    private static String base;

    @BeforeAll static void start() throws Exception {
        Path pinned=ROOT.resolve("shadwick-core/target/node/"+(System.getProperty("os.name").startsWith("Windows")?"node.exe":"node"));
        String node=Files.isExecutable(pinned)?pinned.toString():"node";
        for(String command:List.of("site/build.mjs","site/check.mjs")) {
            Path log=temporary.resolve(Path.of(command).getFileName()+".log");
            Process build=new ProcessBuilder(node,command).directory(ROOT.toFile()).redirectErrorStream(true).redirectOutput(log.toFile()).start();
            if(!build.waitFor(60,TimeUnit.SECONDS)){build.destroyForcibly();throw new IllegalStateException("Docs build timed out");}
            assertThat(build.exitValue()).as(Files.readString(log)).isZero();
        }
        preview=new ProcessBuilder(node,"site/serve.mjs","0").directory(ROOT.toFile()).redirectErrorStream(true).start();
        var line=new BufferedReader(new InputStreamReader(preview.getInputStream())).readLine();
        assertThat(line).startsWith("Shadwick preview: http://127.0.0.1:");base=line.substring("Shadwick preview: ".length());
        playwright=Playwright.create();var options=new BrowserType.LaunchOptions();
        String executable=System.getenv("BROWSER_EXECUTABLE");if(executable!=null&&!executable.isBlank())options.setExecutablePath(Path.of(executable));
        browser=playwright.chromium().launch(options);
    }
    @AfterAll static void stop() throws Exception {
        try {if(browser!=null)browser.close();} finally {
            if(playwright!=null)playwright.close();
            if(preview!=null){preview.destroy();if(!preview.waitFor(5,TimeUnit.SECONDS))preview.destroyForcibly();}
        }
    }
    static Stream<Arguments> guideViews() throws Exception {
        var catalog=new JSONArray(Files.readString(ROOT.resolve("docs/catalog.json")));
        return Stream.concat(Stream.of(""),IntStream.range(0,catalog.length()).mapToObj(i->catalog.getJSONObject(i).getString("slug")+"/"))
                .flatMap(route->Stream.of(Arguments.of(route,1440,ColorScheme.LIGHT),Arguments.of(route,390,ColorScheme.DARK)));
    }
    private Browser.NewContextOptions view(int width,ColorScheme scheme){return new Browser.NewContextOptions().setViewportSize(width,1000).setColorScheme(scheme).setReducedMotion(ReducedMotion.REDUCE);}
    @ParameterizedTest(name="docs {0}, {1}px, {2}") @MethodSource("guideViews")
    void everyGuideHasAccessibleContentAndFitsTheViewport(String route,int width,ColorScheme scheme) {
        try(var context=browser.newContext(view(width,scheme));var tab=context.newPage()) {
            var errors=new ArrayList<String>();tab.onPageError(errors::add);tab.onConsoleMessage(m->{if("error".equals(m.type()))errors.add(m.text());});
            assertThat(tab.navigate(base+"docs/"+route).status()).isEqualTo(200);
            tab.waitForFunction("() => !document.querySelector('.docs-search').hidden");tab.evaluate("document.fonts.ready");
            assertThat(tab.getByRole(AriaRole.HEADING,new Page.GetByRoleOptions().setLevel(1))).hasCount(1);
            assertThat(tab.evaluate("document.documentElement.scrollWidth <= innerWidth + 1")).isEqualTo(true);
            assertThat(new AxeBuilder(tab).withTags(WCAG).analyze().getViolations()).as("WCAG violations at "+route).isEmpty();
            assertThat(errors).as("Console errors at "+route).isEmpty();
        }
    }
    @Test void keyboardSearchCopyAndApiDisclosuresWork() {
        try(var context=browser.newContext(view(390,ColorScheme.DARK));var tab=context.newPage()) {
            context.grantPermissions(List.of("clipboard-read","clipboard-write"));tab.navigate(base+"docs/");
            tab.keyboard().press("Control+k");var input=tab.getByRole(AriaRole.SEARCHBOX,new Page.GetByRoleOptions().setName("Search documentation"));
            assertThat(input).isFocused();input.fill("SwSelect");assertThat(tab.locator(".docs-search-results strong").first()).hasText("Custom select");
            input.press("ArrowDown");assertThat(tab.locator(".docs-search-results a").first()).isFocused();tab.keyboard().press("Enter");
            tab.waitForURL("**/docs/components/select/");
            String example=tab.locator(".code-example code").first().textContent();
            tab.getByRole(AriaRole.BUTTON,new Page.GetByRoleOptions().setName("Copy Java example").setExact(true)).first().click();
            assertThat(tab.locator("#docs-copy-status")).hasText("Example copied to clipboard.");
            assertThat(tab.evaluate("navigator.clipboard.readText()")).isEqualTo(example);
            tab.locator(".api-members summary").first().click();assertThat(tab.locator(".api-members[open] dl").first()).isVisible();
            tab.locator(".docs-toc summary").click();tab.getByRole(AriaRole.NAVIGATION,new Page.GetByRoleOptions().setName("On this page")).getByRole(AriaRole.LINK,new Locator.GetByRoleOptions().setName("API reference").setExact(true)).click();
            assertThat(tab.url()).endsWith("#api-reference");
            assertThat(new AxeBuilder(tab).withTags(WCAG).analyze().getViolations()).isEmpty();
        }
    }
    @Test void failedSearchRecoversAndEscapeRestoresNavigation() {
        try(var context=browser.newContext(view(1440,ColorScheme.LIGHT));var tab=context.newPage()) {
            tab.route("**/docs/search-index.json?*",route->route.fulfill(new Route.FulfillOptions().setStatus(503).setBody("unavailable")));
            tab.navigate(base+"docs?q=SwSelect");assertThat(tab.url()).endsWith("/docs/?q=SwSelect");var input=tab.getByRole(AriaRole.SEARCHBOX,new Page.GetByRoleOptions().setName("Search documentation"));
            assertThat(tab.locator("#docs-search-status")).hasText("Search is unavailable. Browse the guides below.");assertThat(tab.locator(".docs-sections")).isVisible();
            tab.unroute("**/docs/search-index.json?*");input.fill("SwEditableColumn");assertThat(tab.locator(".docs-search-results strong").first()).hasText("Advanced tables");
            input.fill("zzzz-no-such-guide");assertThat(tab.locator("#docs-search-status")).containsText("No matching guides");
            input.press("Escape");assertThat(input).hasValue("");assertThat(tab.locator(".docs-sections")).isVisible();
        }
    }
    @Test void deniedClipboardSelectsTheExampleAndExplainsHowToCopy() {
        try(var context=browser.newContext(view(1440,ColorScheme.LIGHT));var tab=context.newPage()) {
            tab.addInitScript("Object.defineProperty(navigator,'clipboard',{value:{writeText:async()=>{throw new Error('denied')}}})");
            tab.navigate(base+"docs/components/buttons/");String example=tab.locator(".code-example code").first().textContent();
            tab.getByRole(AriaRole.BUTTON,new Page.GetByRoleOptions().setName("Copy Java example").setExact(true)).first().click();
            assertThat(tab.locator("#docs-copy-status")).containsText("The example is selected");assertThat(tab.evaluate("getSelection().toString()")).isEqualTo(example);
            assertThat(new AxeBuilder(tab).withTags(WCAG).analyze().getViolations()).isEmpty();
        }
    }
    @ParameterizedTest @ValueSource(ints={1440,320})
    void documentationAndApiReferencesWorkWithoutJavaScript(int width) {
        try(var context=browser.newContext(view(width,ColorScheme.LIGHT).setJavaScriptEnabled(false));var tab=context.newPage()) {
            tab.navigate(base+"docs/");assertThat(tab.locator(".docs-search")).isHidden();tab.locator(".docs-nav summary").click();
            tab.getByRole(AriaRole.NAVIGATION,new Page.GetByRoleOptions().setName("Documentation").setExact(true)).getByRole(AriaRole.LINK,new Locator.GetByRoleOptions().setName("Custom select").setExact(true)).click();
            assertThat(tab.getByRole(AriaRole.HEADING,new Page.GetByRoleOptions().setLevel(1))).hasText("Custom select");
            tab.locator(".api-members summary").first().click();assertThat(tab.locator(".api-members[open] dl").first()).isVisible();
            assertThat(tab.locator(".copy-code").first()).isHidden();
        }
    }
}
