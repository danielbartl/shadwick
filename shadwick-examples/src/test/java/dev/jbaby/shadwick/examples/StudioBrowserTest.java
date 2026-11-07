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
import java.util.zip.ZipInputStream;
import static com.microsoft.playwright.assertions.PlaywrightAssertions.assertThat;
import static org.assertj.core.api.Assertions.assertThat;

/** The composed workflow, plus every screen in every skin at desktop/mobile sizes. */
@Tag("browser")
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class StudioBrowserTest {
    private static Playwright playwright;
    private static Browser chromium, firefox;
    private static final List<String> WCAG = List.of("wcag2a", "wcag2aa", "wcag21a", "wcag21aa", "wcag22aa");
    private static final Path SHOTS = Path.of("target", "studio-screenshots");
    @LocalServerPort int port;
    @Autowired WebApplication app;
    @BeforeAll static void start() throws Exception {
        Files.createDirectories(SHOTS); playwright = Playwright.create();
        var launch = new BrowserType.LaunchOptions();
        String executable = System.getenv("BROWSER_EXECUTABLE");
        if (executable != null && !executable.isBlank()) launch.setExecutablePath(Path.of(executable));
        chromium = playwright.chromium().launch(launch); firefox = playwright.firefox().launch();
    }
    @AfterAll static void stop() { if (playwright != null) playwright.close(); }
    @BeforeEach void markup() { app.getMarkupSettings().setStripWicketTags(true); }
    String url(String view) { return "http://localhost:" + port + "/studio?view=" + view + "&project=1"; }
    static Stream<Arguments> views() {
        String selected = System.getProperty("shadwick.browser.skins");
        return Arrays.stream(Skin.values()).filter(skin -> selected == null || Arrays.stream(selected.split(",")).anyMatch(value -> skin.id().equalsIgnoreCase(value.strip()))).flatMap(skin -> Stream.of("en", "de").flatMap(language -> Stream.of(Arguments.of(skin.id(), "light", 1440, language), Arguments.of(skin.id(), "dark", 390, language))));
    }
    private Browser.NewContextOptions options(int width) { return new Browser.NewContextOptions().setViewportSize(width, 1000).setReducedMotion(ReducedMotion.REDUCE); }
    private void audit(Page page) {
        page.evaluate("document.fonts.ready");
        page.waitForFunction("() => document.getAnimations().every(a => !a.effect || a.effect.getTiming().iterations === Infinity || a.playState === 'finished' || a.playState === 'idle')");
        assertThat(page.locator("h1")).hasCount(1);
        assertThat(page.evaluate("document.documentElement.scrollWidth <= innerWidth + 1")).as("page overflow at " + page.url()).isEqualTo(true);
        assertThat(new AxeBuilder(page).withTags(WCAG).analyze().getViolations()).as("accessibility at " + page.url()).isEmpty();
    }
    @ParameterizedTest(name="{0} {1} {2}px {3}") @MethodSource("views")
    void fourScreensFitAndRemainAccessible(String skin, String mode, int width, String language) {
        try (var context = chromium.newContext(options(width)); var page = context.newPage()) {
            List<String> errors = new ArrayList<>(); page.onPageError(errors::add); page.onConsoleMessage(m -> { if (m.type().equals("error")) errors.add(m.text()); });
            for (String view : List.of("overview", "projects", "project", "invoices")) {
                assertThat(page.navigate(url(view) + "&skin=" + skin + "&mode=" + mode + "&lang=" + language).status()).isEqualTo(200);
                audit(page);
                page.screenshot(new Page.ScreenshotOptions().setPath(SHOTS.resolve(skin + "-" + mode + "-" + width + "-" + language + "-" + view + ".png")).setFullPage(true));
            }
            page.getByText(language.equals("de") ? "Demo-Einstellungen" : "Demo settings", new Page.GetByTextOptions().setExact(true)).click(); audit(page);
            assertThat(page.locator(".studio-settings-panel")).isVisible();
            assertThat(errors).isEmpty();
        }
    }
    @ParameterizedTest @ValueSource(strings={"chromium", "firefox"})
    void connectedJourneyCreatesEditsCompletesDownloadsPaysAndResets(String engine) throws Exception {
        Browser browser = engine.equals("firefox") ? firefox : chromium;
        try (var context = browser.newContext(options(1440)); var page = context.newPage(); var otherVisitor = browser.newContext(options(390)); var other = otherVisitor.newPage()) {
            List<String> errors = new ArrayList<>(); page.onPageError(errors::add);
            page.navigate(url("project") + "&skin=vega&mode=light");
            other.navigate(url("project"));
            page.getByRole(AriaRole.BUTTON, new Page.GetByRoleOptions().setName("Edit project").setExact(true)).click();
            Locator dialog = page.locator("dialog[open]");
            assertThat(dialog).isVisible();
            dialog.getByLabel("Project name", new Locator.GetByLabelOptions().setExact(true)).fill("A little closer to nature. Together.");
            dialog.getByRole(AriaRole.BUTTON, new Locator.GetByRoleOptions().setName("Save project").setExact(true)).click();
            assertThat(page.locator(".studio-project-intro h2")).hasText("A little closer to nature. Together.");
            for (String progress : List.of("75%", "100%")) {
                page.getByRole(AriaRole.BUTTON, new Page.GetByRoleOptions().setName("Complete").setExact(true)).first().click();
                assertThat(page.locator(".studio-project-intro .studio-progress-label strong")).hasText(progress);
            }
            assertThat(page.locator(".studio-project-intro .studio-progress-label strong")).hasText("100%");
            page.getByRole(AriaRole.BUTTON, new Page.GetByRoleOptions().setName("Prepare client handoff").setExact(true)).click();
            assertThat(page.locator("dialog[open]")).isVisible(); audit(page);
            page.getByRole(AriaRole.BUTTON, new Page.GetByRoleOptions().setName("Prepare handoff").setExact(true)).click();
            assertThat(page.getByRole(AriaRole.LINK, new Page.GetByRoleOptions().setName("Download handoff ZIP"))).isVisible();
            Download handoff = page.waitForDownload(() -> page.getByRole(AriaRole.LINK, new Page.GetByRoleOptions().setName("Download handoff ZIP")).click());
            try (var zip = new ZipInputStream(Files.newInputStream(handoff.path()))) {
                assertThat(zip.getNextEntry().getName()).isEqualTo("README.md");
                assertThat(new String(zip.readAllBytes(), java.nio.charset.StandardCharsets.UTF_8)).contains("Fern & Field", "Together.");
            }
            page.getByRole(AriaRole.BUTTON, new Page.GetByRoleOptions().setName("View invoice").setExact(true)).nth(1).click();
            assertThat(page.locator("dialog[open]")).containsText("CLV-2402"); audit(page);
            page.getByRole(AriaRole.BUTTON, new Page.GetByRoleOptions().setName("Record payment").setExact(true)).click();
            assertThat(page.locator(".studio-invoice-mini").filter(new Locator.FilterOptions().setHasText("CLV-2402"))).containsText("Paid");
            page.navigate(url("invoices"));
            Download csv = page.waitForDownload(() -> page.getByRole(AriaRole.LINK, new Page.GetByRoleOptions().setName("Export CSV")).click());
            assertThat(Files.readString(csv.path())).contains("CLV-2402", "Paid", "Fern & Field");
            page.navigate(url("overview"));
            assertThat(page.locator(".studio-metrics article").first()).containsText("2");
            assertThat(page.locator(".studio-metrics article").nth(2)).containsText("12,400");
            other.reload(); assertThat(other.locator(".studio-project-intro .studio-progress-label strong")).hasText("50%");
            // Create a project, then add a scoped task with a real estimate.
            page.getByRole(AriaRole.BUTTON, new Page.GetByRoleOptions().setName("New project").setExact(true)).click(); dialog = page.locator("dialog[open]");
            dialog.getByLabel("Project name", new Locator.GetByLabelOptions().setExact(true)).fill("A new chapter"); dialog.getByLabel("Client", new Locator.GetByLabelOptions().setExact(true)).fill("Bright Studio");
            dialog.getByRole(AriaRole.BUTTON, new Locator.GetByRoleOptions().setName("Save project").setExact(true)).click();
            assertThat(page.locator("h1")).hasText("Bright Studio");
            page.getByRole(AriaRole.BUTTON, new Page.GetByRoleOptions().setName("Add task").setExact(true)).click(); dialog = page.locator("dialog[open]");
            dialog.getByLabel("Task", new Locator.GetByLabelOptions().setExact(true)).fill("Make something useful");
            dialog.getByRole(AriaRole.BUTTON, new Locator.GetByRoleOptions().setName("Add task").setExact(true)).click();
            assertThat(page.locator(".studio-tasks")).containsText("Make something useful");
            // An older tab cannot apply its callback after reset.
            try (var stale = context.newPage()) {
                stale.navigate(page.url().replaceFirst("\\?\\d+&", "?"));
                page.getByText("Demo settings", new Page.GetByTextOptions().setExact(true)).click();
                page.getByRole(AriaRole.BUTTON, new Page.GetByRoleOptions().setName("Reset sample data").setExact(true)).click();
                page.getByRole(AriaRole.BUTTON, new Page.GetByRoleOptions().setName("Reset workspace").setExact(true)).click();
                assertThat(page.locator(".studio-metrics article").first()).containsText("3");
                stale.getByRole(AriaRole.BUTTON, new Page.GetByRoleOptions().setName("Complete").setExact(true)).click();
                assertThat(stale.locator("body")).containsText("reset in another tab");
                assertThat(stale.locator("body")).containsText("isn’t in your workspace");
            }
            assertThat(errors).isEmpty();
        }
    }
    @ParameterizedTest @ValueSource(strings={"chromium", "firefox"})
    void languagePickerTranslatesTheApplicationAndPreservesRecords(String engine) {
        Browser browser = engine.equals("firefox") ? firefox : chromium;
        try (var context = browser.newContext(options(390)); var page = context.newPage()) {
            var errors = new ArrayList<String>(); page.onPageError(errors::add);
            page.navigate(url("overview"));
            page.getByText("Demo settings", new Page.GetByTextOptions().setExact(true)).click();
            assertThat(page.locator(".studio-settings select").nth(1).locator("option")).hasText(new String[]{"English", "Deutsch"});
            page.locator(".studio-settings select").nth(1).selectOption("de");
            page.getByRole(AriaRole.BUTTON, new Page.GetByRoleOptions().setName("Apply preferences").setExact(true)).click();
            assertThat(page.locator("h1")).hasText("Ein guter Tag für neue Ideen.");
            assertThat(page.locator(".studio-metrics")).containsText("Aktive Projekte");
            page.getByRole(AriaRole.BUTTON, new Page.GetByRoleOptions().setName("Neues Projekt").setExact(true)).click();
            Locator dialog = page.locator("dialog[open]");
            assertThat(dialog.getByLabel("Projektname", new Locator.GetByLabelOptions().setExact(true))).isVisible();
            dialog.getByRole(AriaRole.BUTTON, new Locator.GetByRoleOptions().setName("Projekt speichern").setExact(true)).click();
            assertThat(dialog.locator("[aria-invalid=true]")).hasCount(2); audit(page);
            page.keyboard().press("Escape");
            page.navigate(url("project"));
            assertThat(page.locator(".studio-project-intro")).containsText("Projektbudget");
            page.getByRole(AriaRole.BUTTON, new Page.GetByRoleOptions().setName("Abschließen").setExact(true)).first().click();
            assertThat(page.locator(".studio-project-intro")).containsText("3 von 4 abgeschlossen");
            page.getByRole(AriaRole.BUTTON, new Page.GetByRoleOptions().setName("Projekt bearbeiten").setExact(true)).click();
            dialog = page.locator("dialog[open]");
            assertThat(dialog.getByLabel("Projektname", new Locator.GetByLabelOptions().setExact(true))).hasValue("A little closer to nature.");
            page.keyboard().press("Escape");
            page.getByRole(AriaRole.BUTTON, new Page.GetByRoleOptions().setName("Rechnung ansehen").setExact(true)).nth(1).click();
            dialog = page.locator("dialog[open]"); assertThat(dialog).containsText("Zahlung ausstehend"); audit(page);
            dialog.getByRole(AriaRole.BUTTON, new Locator.GetByRoleOptions().setName("Zahlung erfassen").setExact(true)).click();
            assertThat(page.locator(".studio-invoice-mini").filter(new Locator.FilterOptions().setHasText("CLV-2402"))).containsText("Bezahlt");
            page.navigate(url("invoices"));
            page.getByText("Demo-Einstellungen", new Page.GetByTextOptions().setExact(true)).click();
            page.locator(".studio-settings select").nth(1).selectOption("en");
            page.getByRole(AriaRole.BUTTON, new Page.GetByRoleOptions().setName("Einstellungen anwenden").setExact(true)).click();
            assertThat(page.locator("h1")).hasText("The business of good work.");
            assertThat(page.locator("tbody tr").filter(new Locator.FilterOptions().setHasText("CLV-2402"))).containsText("Paid");
            page.navigate(url("project")); assertThat(page.locator(".studio-project-intro")).containsText("3 of 4 complete");
            assertThat(errors).isEmpty();
        }
    }
    @Test void directRenderingIsScopedToReadOnlyProjectNavigations() {
        try (var context = chromium.newContext()) {
            var options = RequestOptions.create().setMaxRedirects(0);
            assertThat(context.request().get(url("projects"), options).status()).isEqualTo(200);
            for (String view : List.of("overview", "project", "invoices")) {
                assertThat(context.request().get(url(view), options).status()).as(view).isEqualTo(200);
            }
            assertThat(context.request().post(url("projects"), options).status()).isEqualTo(302);
            assertThat(app.getRequestCycleSettings().getRenderStrategy()).isEqualTo(org.apache.wicket.settings.RequestCycleSettings.RenderStrategy.REDIRECT_TO_BUFFER);
        }
    }
    static Stream<Arguments> searchRequests() {
        return Stream.of("chromium", "firefox").flatMap(engine -> Stream.of(false, true).flatMap(script -> Stream.of("en", "de").map(language -> Arguments.of(engine, script, language))));
    }
    @ParameterizedTest @MethodSource("searchRequests")
    void projectSearchReturnsOne200AndKeepsHistoryAndActions(String engine, boolean script, String language) {
        Browser browser = engine.equals("firefox") ? firefox : chromium;
        boolean german = language.equals("de");
        try (var context = browser.newContext(options(1440).setJavaScriptEnabled(script)); var page = context.newPage()) {
            var errors = new ArrayList<String>(); page.onPageError(errors::add);
            var navigations = new ArrayList<Request>();
            page.onRequest(request -> { if (request.isNavigationRequest()) navigations.add(request); });
            Response initial = page.navigate("http://localhost:" + port + "/studio?view=projects&lang=" + language);
            assertThat(initial.status()).isEqualTo(200); assertThat(initial.request().redirectedFrom()).isNull();
            assertThat(navigations).hasSize(1); assertThat(page.url()).contains("?view=projects&lang=" + language);
            String directory = page.url();
            navigations.clear();
            page.getByLabel(german ? "Projekte durchsuchen" : "Search projects", new Page.GetByLabelOptions().setExact(true)).fill("Sunday");
            if (script) {
                page.getByRole(AriaRole.BUTTON, new Page.GetByRoleOptions().setName(german ? "Filter anwenden" : "Apply filters").setExact(true)).click();
                assertThat(page.locator(".studio-project-card")).hasCount(1);
                assertThat(navigations).isEmpty();
            } else {
                Response search = page.waitForNavigation(() -> page.getByRole(AriaRole.BUTTON, new Page.GetByRoleOptions().setName(german ? "Filter anwenden" : "Apply filters").setExact(true)).click());
                assertThat(search.status()).isEqualTo(200); assertThat(search.request().redirectedFrom()).isNull();
                assertThat(navigations).hasSize(1).allSatisfy(request -> assertThat(request.method()).isEqualTo("GET"));
            }
            assertThat(page.url()).contains("view=projects", "q=Sunday");
            assertThat(page.locator(".studio-project-card")).hasCount(1); assertThat(page.locator(".studio-project-card")).containsText("Sunday Coffee");
            String filtered = page.url();
            assertThat(page.reload().status()).isEqualTo(200); assertThat(page.url()).isEqualTo(filtered);
            assertThat(page.locator(".studio-project-card")).hasCount(1);
            page.goBack(); assertThat(page.url()).isEqualTo(directory); assertThat(page.locator(".studio-project-card")).hasCount(4);
            page.goForward(); assertThat(page.url()).isEqualTo(filtered); assertThat(page.locator(".studio-project-card")).hasCount(1);
            if (script) {
                navigations.clear();
                page.getByRole(AriaRole.LINK, new Page.GetByRoleOptions().setName(german ? "Filter zurücksetzen" : "Clear filters").setExact(true)).click();
                assertThat(page.locator(".studio-project-card")).hasCount(4); assertThat(navigations).isEmpty();
            } else {
                Response clear = page.waitForNavigation(() -> page.getByRole(AriaRole.LINK, new Page.GetByRoleOptions().setName(german ? "Filter zurücksetzen" : "Clear filters").setExact(true)).click());
                assertThat(clear.status()).isEqualTo(200); assertThat(clear.request().redirectedFrom()).isNull();
            }
            assertThat(page.locator(".studio-project-card")).hasCount(4);
            if (script) {
                // The direct-rendered results still carry valid page IDs in their Ajax callback URLs.
                page.getByRole(AriaRole.BUTTON, new Page.GetByRoleOptions().setName(german ? "Neues Projekt" : "New project").setExact(true)).click();
                Locator dialog = page.locator("dialog[open]");
                dialog.getByLabel(german ? "Projektname" : "Project name", new Locator.GetByLabelOptions().setExact(true)).fill("Direct-rendered creation");
                dialog.getByLabel(german ? "Kunde" : "Client", new Locator.GetByLabelOptions().setExact(true)).fill("Search workflow client");
                dialog.getByRole(AriaRole.BUTTON, new Locator.GetByRoleOptions().setName(german ? "Projekt speichern" : "Save project").setExact(true)).click();
                assertThat(page.locator("h1")).hasText("Search workflow client");
                page.navigate(url("projects")); assertThat(page.locator(".studio-project-card")).hasCount(5);
            }
            assertThat(errors).isEmpty();
        }
    }
    @ParameterizedTest @ValueSource(strings={"chromium", "firefox"})
    void taskAndPaymentCommandsPatchOnlyChangedComponentsWithoutNavigating(String engine) {
        Browser browser = engine.equals("firefox") ? firefox : chromium;
        try (var context = browser.newContext(options(1440)); var page = context.newPage()) {
            page.navigate(url("project") + "&skin=vega");
            page.getByText("Demo settings", new Page.GetByTextOptions().setExact(true)).click();
            page.locator(".studio-settings select").first().selectOption(new SelectOption().setLabel("nova"));
            page.getByRole(AriaRole.BUTTON, new Page.GetByRoleOptions().setName("Apply preferences").setExact(true)).click();
            page.waitForFunction("() => [...document.querySelectorAll('link[href*=\"/css/shadwick-\"]')].some(link => !link.disabled && link.href.includes('/css/shadwick-nova-'))");
            List<Request> documents = new ArrayList<>(), commands = new ArrayList<>(); List<Response> responses = new ArrayList<>();
            page.onRequest(request -> {
                if (request.isNavigationRequest()) documents.add(request);
                if ("true".equals(request.headers().get("wicket-ajax"))) commands.add(request);
            });
            page.onResponse(response -> { if (response.request().url().contains("row-toggle")) responses.add(response); });
            page.evaluate("""
                window.savedStudioNodes = {
                  main: document.querySelector('.studio-main'), sidebar: document.querySelector('.sw-sidebar'),
                  header: document.querySelector('.sw-site-header'), summary: document.querySelector('.studio-project-intro'),
                  budget: document.querySelector('.studio-project-facts dd'), files: document.querySelector('.studio-deliverables'),
                  milestones: document.querySelector('.studio-milestones'), handoff: document.querySelector('.studio-handoff'),
                  status: document.querySelector('.studio-project-intro .sw-badge'),
                  first: document.querySelectorAll('.studio-tasks li')[0], fourth: document.querySelectorAll('.studio-tasks li')[3],
                  changed: document.querySelectorAll('.studio-tasks li')[2]
                };
                """);
            String originalUrl = page.url();
            page.locator(".studio-tasks li").nth(2).getByRole(AriaRole.BUTTON, new Locator.GetByRoleOptions().setName("Complete").setExact(true)).click();
            assertThat(page.locator(".studio-project-intro .studio-progress-label strong")).hasText("75%");
            assertThat(page.url()).isEqualTo(originalUrl); assertThat(documents).isEmpty(); assertThat(commands).hasSize(1);
            assertThat(page.locator("link[href*=\"/css/shadwick-\"]").evaluateAll("links => { const active=links.filter(link => !link.disabled); return active.length === 1 && active[0].href.includes('/css/shadwick-nova-'); }")).isEqualTo(true);
            assertThat(page.evaluate("""
                () => {
                  const n = window.savedStudioNodes;
                  return n.main === document.querySelector('.studio-main') && n.sidebar === document.querySelector('.sw-sidebar')
                    && n.header === document.querySelector('.sw-site-header') && n.summary === document.querySelector('.studio-project-intro')
                    && n.budget === document.querySelector('.studio-project-facts dd') && n.files === document.querySelector('.studio-deliverables')
                    && n.milestones === document.querySelector('.studio-milestones') && n.handoff === document.querySelector('.studio-handoff')
                    && n.status === document.querySelector('.studio-project-intro .sw-badge')
                    && n.first === document.querySelectorAll('.studio-tasks li')[0] && n.fourth === document.querySelectorAll('.studio-tasks li')[3]
                    && n.changed !== document.querySelectorAll('.studio-tasks li')[2];
                }
                """)).isEqualTo(true);
            assertThat(responses).hasSize(1); assertThat(responses.get(0).status()).isEqualTo(200);
            assertThat(responses.get(0).request().method()).isEqualTo("POST");
            assertThat(responses.get(0).request().redirectedFrom()).isNull(); assertThat(responses.get(0).text()).doesNotContain("<redirect>");
            // Exercise the same row callback again; its expected-state guard must track the last render.
            page.locator(".studio-tasks li").nth(2).getByRole(AriaRole.BUTTON, new Locator.GetByRoleOptions().setName("Reopen").setExact(true)).click();
            assertThat(page.locator(".studio-project-intro .studio-progress-label strong")).hasText("50%");
            page.getByRole(AriaRole.BUTTON, new Page.GetByRoleOptions().setName("View invoice").setExact(true)).nth(1).click();
            page.getByRole(AriaRole.BUTTON, new Page.GetByRoleOptions().setName("Record payment").setExact(true)).click();
            assertThat(page.locator(".studio-invoice-mini").nth(1)).containsText("Paid");
            assertThat(page.url()).isEqualTo(originalUrl); assertThat(documents).isEmpty();
            assertThat(page.evaluate("window.savedStudioNodes.main === document.querySelector('.studio-main') && window.savedStudioNodes.summary === document.querySelector('.studio-project-intro')")).isEqualTo(true);
            page.navigate(url("invoices")); documents.clear();
            page.evaluate("window.savedInvoiceTable = document.querySelector('.sw-data-table'); window.savedInvoiceHeading = document.querySelector('h1')");
            var row = page.locator("tbody tr").filter(new Locator.FilterOptions().setHasText("CLV-2405"));
            row.getByRole(AriaRole.BUTTON).click(); page.getByRole(AriaRole.MENUITEM, new Page.GetByRoleOptions().setName("View invoice").setExact(true)).click();
            page.getByRole(AriaRole.BUTTON, new Page.GetByRoleOptions().setName("Record payment").setExact(true)).click();
            assertThat(page.locator("tbody tr").filter(new Locator.FilterOptions().setHasText("CLV-2405"))).containsText("Paid");
            assertThat(page.evaluate("window.savedInvoiceTable === document.querySelector('.sw-data-table') && window.savedInvoiceHeading === document.querySelector('h1')")).isEqualTo(true);
            assertThat(documents).isEmpty();
        }
    }
    @ParameterizedTest @ValueSource(strings={"chromium", "firefox"})
    void tableControlsAndRepeatedSkinChangesDoNotNavigate(String engine) {
        Browser browser = engine.equals("firefox") ? firefox : chromium;
        try (var context = browser.newContext(options(1440)); var page = context.newPage()) {
            page.navigate(url("invoices") + "&skin=vega");
            List<Request> documents = new ArrayList<>(); page.onRequest(request -> { if (request.isNavigationRequest()) documents.add(request); });
            page.evaluate("window.tableShell = document.querySelector('.studio-main'); window.metrics = document.querySelector('.studio-metrics')");
            page.getByRole(AriaRole.LINK, new Page.GetByRoleOptions().setName("Amount").setExact(true)).click();
            assertThat(page.getByRole(AriaRole.COLUMNHEADER, new Page.GetByRoleOptions().setName("Amount"))).hasAttribute("aria-sort", "ascending");
            assertThat(page.locator("tbody tr").first()).containsText("CLV-2404");
            page.getByRole(AriaRole.BUTTON, new Page.GetByRoleOptions().setName("Rows per page 5").setExact(true)).click();
            page.getByRole(AriaRole.MENUITEMRADIO, new Page.GetByRoleOptions().setName("10").setExact(true)).click();
            assertThat(page.getByRole(AriaRole.BUTTON, new Page.GetByRoleOptions().setName("Rows per page 10").setExact(true))).isVisible();
            page.getByRole(AriaRole.BUTTON, new Page.GetByRoleOptions().setName("Columns").setExact(true)).click();
            page.getByRole(AriaRole.MENUITEMCHECKBOX, new Page.GetByRoleOptions().setName("Due date").setExact(true)).click();
            assertThat(page.getByRole(AriaRole.COLUMNHEADER, new Page.GetByRoleOptions().setName("Due date"))).hasCount(0);
            assertThat(documents).isEmpty();
            page.evaluate("window.sameInvoiceTable = document.querySelector('.sw-data-table')");
            page.locator(".sw-sidebar").getByRole(AriaRole.LINK, new Locator.GetByRoleOptions().setName("Invoices").setExact(true)).click();
            assertThat(page.evaluate("window.sameInvoiceTable === document.querySelector('.sw-data-table')")).isEqualTo(true);
            assertThat(page.evaluate("window.tableShell === document.querySelector('.studio-main') && window.metrics === document.querySelector('.studio-metrics')")).isEqualTo(true);
            for (String skin : List.of("nova", "vega", "sera", "nova")) {
                page.getByText("Demo settings", new Page.GetByTextOptions().setExact(true)).click();
                page.locator(".studio-settings select").first().selectOption(new SelectOption().setLabel(skin));
                page.getByRole(AriaRole.BUTTON, new Page.GetByRoleOptions().setName("Apply preferences").setExact(true)).click();
                try {
                    page.waitForFunction("skin => [...document.querySelectorAll('link[href*=\"/css/shadwick-\"]')].filter(link => !link.disabled).length === 1 && [...document.querySelectorAll('link[href*=\"/css/shadwick-\"]')].some(link => !link.disabled && link.href.includes('/css/shadwick-' + skin + '-'))", skin, new Page.WaitForFunctionOptions().setTimeout(5000));
                } catch (RuntimeException error) {
                    throw new AssertionError("Skin " + skin + ": " + page.locator("link[rel=stylesheet]").evaluateAll("links => links.map(link => ({href:link.href,disabled:link.disabled}))"), error);
                }
                assertThat(page.getByRole(AriaRole.COLUMNHEADER, new Page.GetByRoleOptions().setName("Due date"))).hasCount(0);
                assertThat(page.getByRole(AriaRole.COLUMNHEADER, new Page.GetByRoleOptions().setName("Amount"))).hasAttribute("aria-sort", "ascending");
                assertThat(page.getByRole(AriaRole.BUTTON, new Page.GetByRoleOptions().setName("Rows per page 10").setExact(true))).isVisible();
                assertThat(documents).isEmpty();
            }
            audit(page);
        }
    }
    @ParameterizedTest @ValueSource(strings={"chromium", "firefox"})
    void studioNavigationAndHistoryKeepTheApplicationShell(String engine) {
        Browser browser = engineBrowser(engine);
        try (var context = browser.newContext(options(1440)); var page = context.newPage()) {
            page.navigate(url("overview"));
            List<Request> documents = new ArrayList<>(); page.onRequest(request -> { if (request.isNavigationRequest()) documents.add(request); });
            page.evaluate("window.navigationShell = { main: document.querySelector('.studio-main'), sidebar: document.querySelector('.sw-sidebar'), header: document.querySelector('.sw-site-header') }");
            page.locator(".sw-sidebar").getByRole(AriaRole.LINK, new Locator.GetByRoleOptions().setName("Projects").setExact(true)).click();
            assertThat(page.locator("h1")).hasText("Good work, in progress.");
            String directory = page.url();
            page.locator(".studio-project-cover").filter(new Locator.FilterOptions().setHasText("Fern & Field")).click();
            assertThat(page.locator("h1")).hasText("Fern & Field"); String project = page.url();
            page.locator(".sw-sidebar").getByRole(AriaRole.LINK, new Locator.GetByRoleOptions().setName("Invoices").setExact(true)).click();
            assertThat(page.locator("h1")).hasText("The business of good work.");
            page.goBack(); assertThat(page.locator("h1")).hasText("Fern & Field"); assertThat(page.url()).isEqualTo(project);
            page.goBack(); assertThat(page.locator("h1")).hasText("Good work, in progress."); assertThat(page.url()).isEqualTo(directory);
            page.goForward(); assertThat(page.locator("h1")).hasText("Fern & Field");
            assertThat(documents).isEmpty();
            assertThat(page.evaluate("window.navigationShell.main === document.querySelector('.studio-main') && window.navigationShell.sidebar === document.querySelector('.sw-sidebar') && window.navigationShell.header === document.querySelector('.sw-site-header')")).isEqualTo(true);
        }
    }
    private Browser engineBrowser(String engine) { return engine.equals("firefox") ? firefox : chromium; }
    @ParameterizedTest @ValueSource(strings={"chromium", "firefox"})
    void everySearchReturnsRenderedResultsWithoutDocumentNavigation(String engine) {
        try (var context = engineBrowser(engine).newContext(options(1440)); var page = context.newPage()) {
            page.navigate("http://localhost:" + port + "/studio?view=projects&skin=nova&lang=en");
            assertThat(page.locator(".studio-project-card")).hasCount(4);
            var requests = new ArrayList<Request>();
            page.onRequest(request -> {
                if (request.isNavigationRequest() || List.of("xhr", "fetch").contains(request.resourceType())) requests.add(request);
            });
            page.evaluate("window.originalStudioDocument = document");
            String resultsId = page.locator(".studio-project-grid").getAttribute("id");
            page.locator(".studio-filters select[name=status]").selectOption("In progress", new Locator.SelectOptionOptions().setForce(true));
            Locator apply = page.getByRole(AriaRole.BUTTON, new Page.GetByRoleOptions().setName("Apply filters").setExact(true));
            Response changed = page.waitForResponse(response -> "true".equals(response.request().headers().get("wicket-ajax")), apply::click);
            assertThat(page.locator(".studio-project-card")).hasCount(3);
            assertThat(changed.status()).isEqualTo(200);
            assertThat(changed.text()).contains("<component id=\"" + resultsId + "\"", "studio-project-card", "Sunday Coffee")
                    .doesNotContain("<redirect>", "window.location");
            page.waitForFunction("() => new URL(location.href).searchParams.get('status') === 'In progress'");
            assertThat(requests).hasSize(1);
            page.evaluate("window.filteredStudioGrid = document.querySelector('.studio-project-grid')");
            Response repeated = page.waitForResponse(response -> "true".equals(response.request().headers().get("wicket-ajax")), apply::click);
            assertThat(repeated.text()).contains("<component id=\"" + resultsId + "\"", "studio-project-card", "Sunday Coffee").doesNotContain("<redirect>");
            page.waitForFunction("() => window.filteredStudioGrid !== document.querySelector('.studio-project-grid')");
            assertThat(page.evaluate("window.originalStudioDocument === document")).isEqualTo(true);
            assertThat(requests).hasSize(2).allSatisfy(request -> {
                assertThat(request.isNavigationRequest()).isFalse(); assertThat(request.redirectedFrom()).isNull();
            });
        }
    }
    @ParameterizedTest @ValueSource(strings={"chromium", "firefox"})
    void successiveAjaxSearchesKeepTheInputsAndUnrelatedRegions(String engine) {
        Browser browser = engineBrowser(engine);
        try (var context = browser.newContext(options(1440)); var page = context.newPage()) {
            page.navigate(url("projects") + "&q=Fern");
            List<Request> documents = new ArrayList<>(); page.onRequest(request -> { if (request.isNavigationRequest()) documents.add(request); });
            page.evaluate("window.searchNodes = { input: document.querySelector('.studio-filters input[name=q]'), filters: document.querySelector('.studio-filters'), header: document.querySelector('.sw-site-header'), footer: document.querySelector('.studio-footer') }");
            for (String client : List.of("Sunday", "Forma", "Fern")) {
                page.getByLabel("Search projects", new Page.GetByLabelOptions().setExact(true)).fill(client);
                page.getByRole(AriaRole.BUTTON, new Page.GetByRoleOptions().setName("Apply filters").setExact(true)).click();
                assertThat(page.locator(".studio-project-card")).hasCount(1); assertThat(page.locator(".studio-project-card")).containsText(client);
                assertThat(page.evaluate("window.searchNodes.input === document.querySelector('.studio-filters input[name=q]') && window.searchNodes.filters === document.querySelector('.studio-filters') && window.searchNodes.header === document.querySelector('.sw-site-header') && window.searchNodes.footer === document.querySelector('.studio-footer')")).isEqualTo(true);
            }
            page.goBack(); assertThat(page.locator(".studio-project-card")).containsText("Forma");
            assertThat(page.getByLabel("Search projects", new Page.GetByLabelOptions().setExact(true))).hasValue("Forma");
            assertThat(documents).isEmpty();
        }
    }
    @Test void mobileKeyboardNavigationValidationAndPreferencesWork() {
        try (var context = chromium.newContext(options(320)); var page = context.newPage()) {
            page.navigate(url("overview") + "&mode=light");
            page.locator(".sw-sidebar-trigger").focus(); page.keyboard().press("Enter");
            assertThat(page.locator(".sw-sidebar")).isVisible();
            page.keyboard().press("Escape"); assertThat(page.locator(".sw-sidebar-trigger")).isFocused();
            page.getByRole(AriaRole.BUTTON, new Page.GetByRoleOptions().setName("New project").setExact(true)).focus(); page.keyboard().press("Enter");
            Locator dialog = page.locator("dialog[open]");
            assertThat(dialog.getByLabel("Project name", new Locator.GetByLabelOptions().setExact(true))).isFocused();
            dialog.getByRole(AriaRole.BUTTON, new Locator.GetByRoleOptions().setName("Save project").setExact(true)).click();
            assertThat(dialog.locator("[aria-invalid=true]")).hasCount(2); audit(page);
            page.keyboard().press("Escape"); assertThat(page.getByRole(AriaRole.BUTTON, new Page.GetByRoleOptions().setName("New project").setExact(true))).isFocused();
            page.getByText("Demo settings", new Page.GetByTextOptions().setExact(true)).click();
            page.locator(".studio-settings select").first().selectOption(new SelectOption().setLabel("nova"));
            page.locator(".studio-settings select").nth(1).selectOption("de");
            page.getByRole(AriaRole.BUTTON, new Page.GetByRoleOptions().setName("Apply preferences").setExact(true)).click();
            page.navigate(url("invoices")); assertThat(page.locator("html")).hasAttribute("lang", "de");
            assertThat(page.getByRole(AriaRole.LINK, new Page.GetByRoleOptions().setName("CSV exportieren").setExact(true))).isVisible();
            assertThat(page.locator("head link[href*=shadwick-nova]")).hasCount(1); audit(page);
        }
    }
    @Test void nativeBrowsingFiltersAndDownloadsWorkWithoutScript() {
        try (var context = chromium.newContext(options(320).setJavaScriptEnabled(false)); var page = context.newPage()) {
            page.navigate(url("projects"));
            page.getByText("Demo settings", new Page.GetByTextOptions().setExact(true)).click();
            page.locator(".studio-settings select").nth(1).selectOption("de");
            page.getByRole(AriaRole.BUTTON, new Page.GetByRoleOptions().setName("Apply preferences").setExact(true)).click();
            assertThat(page.locator("h1")).hasText("Gute Arbeit nimmt Gestalt an.");
            page.getByLabel("Projekte durchsuchen", new Page.GetByLabelOptions().setExact(true)).fill("Sunday");
            var searches = new ArrayList<Request>();
            page.onRequest(request -> { if (request.isNavigationRequest()) searches.add(request); });
            page.getByRole(AriaRole.BUTTON, new Page.GetByRoleOptions().setName("Filter anwenden").setExact(true)).click();
            assertThat(page.locator(".studio-project-card")).hasCount(1); assertThat(page.locator(".studio-project-card")).containsText("Sunday Coffee");
            assertThat(searches).hasSize(1).allSatisfy(request -> {
                assertThat(request.method()).isEqualTo("GET");
                assertThat(request.redirectedFrom()).isNull();
            });
            assertThat(page.url()).contains("q=Sunday");
            page.getByRole(AriaRole.LINK, new Page.GetByRoleOptions().setName("Filter zurücksetzen").setExact(true)).click();
            assertThat(page.locator(".studio-project-card")).hasCount(4);
            page.navigate(url("project").replace("&project=1", "&project=4"));
            assertThat(page.getByRole(AriaRole.LINK, new Page.GetByRoleOptions().setName("Übergabe-ZIP herunterladen"))).isVisible();
            assertThat(page.evaluate("document.documentElement.scrollWidth <= innerWidth + 1")).isEqualTo(true);
        }
    }
}
