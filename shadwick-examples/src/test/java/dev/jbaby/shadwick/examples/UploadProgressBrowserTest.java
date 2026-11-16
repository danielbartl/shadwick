package dev.jbaby.shadwick.examples;

import com.deque.html.axecore.playwright.AxeBuilder;
import com.google.gson.JsonObject;
import com.microsoft.playwright.*;
import com.microsoft.playwright.options.*;
import com.microsoft.playwright.assertions.LocatorAssertions;
import dev.jbaby.shadwick.Skin;
import org.junit.jupiter.api.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.*;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;

import javax.imageio.ImageIO;
import java.awt.Color;
import java.awt.GradientPaint;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.nio.file.*;
import java.util.*;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import static com.microsoft.playwright.assertions.PlaywrightAssertions.assertThat;
import static org.assertj.core.api.Assertions.assertThat;

/** The dropzone's attachment rows, grid and canvas previews, and its progress, Cancel and outcomes over Ajax. */
@Tag("browser")
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class UploadProgressBrowserTest {
    static Playwright playwright;
    static Browser chromium, firefox;
    static FilePayload png, jpg, pdf, svg, broken;
    @LocalServerPort int port;
    @org.springframework.beans.factory.annotation.Autowired org.apache.wicket.protocol.http.WebApplication app;

    @BeforeAll static void start() throws Exception {
        Files.createDirectories(Path.of("target/upload-screenshots"));
        playwright = Playwright.create();
        chromium = playwright.chromium().launch();
        firefox = playwright.firefox().launch();
        png = new FilePayload("meadow.png", "image/png", image("png", 900, 600));
        jpg = new FilePayload("roof.jpg", "image/jpeg", image("jpg", 600, 900));
        byte[] noise = new byte[300_000];
        new Random(7).nextBytes(noise);
        pdf = new FilePayload("report.pdf", "application/pdf", noise);
        svg = new FilePayload("logo.svg", "image/svg+xml", "<svg xmlns=\"http://www.w3.org/2000/svg\"/>".getBytes());
        broken = new FilePayload("broken.png", "image/png", "not really a png".getBytes());
    }
    @AfterAll static void stop() { playwright.close(); }
    @BeforeEach void markup() { app.getMarkupSettings().setStripWicketTags(true); }
    @AfterEach void restore() { app.getMarkupSettings().setStripWicketTags(false); }

    static byte[] image(String format, int width, int height) throws Exception {
        BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
        var g = image.createGraphics();
        g.setPaint(new GradientPaint(0, 0, new Color(0x2f7d4f), width, height, new Color(0xe8d27a)));
        g.fillRect(0, 0, width, height);
        g.dispose();
        var out = new ByteArrayOutputStream();
        ImageIO.write(image, format, out);
        return out.toByteArray();
    }

    Browser browser(String engine) { return engine.equals("chromium") ? chromium : firefox; }
    String url(String skin, String mode) { return "http://localhost:" + port + "/uploads?skin=" + skin + "&mode=" + mode; }
    /** Console errors, without those a test causes on purpose: an aborted or failed request. */
    List<String> errors(Page p) {
        var result = new ArrayList<String>();
        p.onPageError(result::add);
        p.onConsoleMessage(m -> {
            if (!m.type().equals("error")) return;
            String text = m.text();
            if (text.contains("Wicket.Ajax.Call.failure") || text.contains("net::ERR_FAILED")) return;
            result.add(text);
        });
        return result;
    }
    void audit(Page p) {
        assertThat(p.locator("[style]").evaluateAll("els => els.map(e => e.outerHTML.slice(0, 160))")).as("inline styles").isEqualTo(List.of());
        assertThat(p.evaluate("document.documentElement.scrollWidth <= innerWidth + 1")).as("No horizontal overflow").isEqualTo(true);
        assertThat(new AxeBuilder(p).withTags(List.of("wcag2a", "wcag2aa", "wcag21a", "wcag21aa", "wcag22aa")).analyze().getViolations()).isEmpty();
    }
    Locator photos(Page p) { return p.locator("form[aria-label='Photos with previews and progress']"); }
    Locator descriptions(Locator form) { return form.locator(".sw-file-upload-list .sw-attachment-description"); }
    CDPSession throttle(BrowserContext context, Page p, int bytesPerSecond) {
        return throttle(context.newCDPSession(p), bytesPerSecond);
    }
    CDPSession throttle(CDPSession cdp, int bytesPerSecond) {
        JsonObject conditions = new JsonObject();
        conditions.addProperty("offline", false);
        conditions.addProperty("latency", 0);
        conditions.addProperty("downloadThroughput", -1);
        conditions.addProperty("uploadThroughput", bytesPerSecond);
        cdp.send("Network.enable");
        cdp.send("Network.emulateNetworkConditions", conditions);
        return cdp;
    }

    static Stream<Arguments> skins() {
        List<String> selected = Arrays.asList(System.getProperty("shadwick.browser.skins", "vega,lyra,nova,maia,mira,luma,rhea,sera,sketch").split(","));
        return Stream.of("chromium", "firefox").flatMap(engine -> Arrays.stream(Skin.values())
                .map(skin -> skin.name().toLowerCase(Locale.ROOT)).filter(selected::contains)
                .flatMap(skin -> Stream.of(Arguments.of(engine, skin, "light", 1440), Arguments.of(engine, skin, "dark", 320))));
    }

    static Stream<String> chromiumSkins() {
        List<String> selected = Arrays.asList(System.getProperty("shadwick.browser.skins", "vega,lyra,nova,maia,mira,luma,rhea,sera,sketch").split(","));
        return Arrays.stream(Skin.values()).map(skin -> skin.name().toLowerCase(Locale.ROOT)).filter(selected::contains);
    }

    @ParameterizedTest @MethodSource("skins")
    void filesShowAsAttachmentsInAListOrAGridWithCanvasPreviews(String engine, String skin, String mode, int width) {
        try (var context = browser(engine).newContext(new Browser.NewContextOptions().setViewportSize(width, 1000)); var p = context.newPage()) {
            var errors = errors(p);
            p.navigate(url(skin, mode));
            Locator form = photos(p);
            form.locator("input[type=file]").setInputFiles(new FilePayload[]{png, jpg, pdf, svg, broken});
            Locator items = form.locator(".sw-file-upload-item > .sw-attachment");
            assertThat(items).hasCount(5);
            assertThat(items.first()).hasAttribute("data-orientation", "vertical");
            // The two decodable images get a drawn thumbnail; the PDF, the SVG and a broken image keep their icon
            assertThat(form.locator(".sw-attachment-media[data-variant='image'] > canvas")).hasCount(2);
            assertThat(items.nth(2).locator(".sw-attachment-media > svg")).hasCount(1);
            assertThat(items.nth(3).locator(".sw-attachment-media > svg")).hasCount(1);
            assertThat(items.nth(4).locator(".sw-attachment-media > svg")).hasCount(1);
            assertThat(form.locator("canvas").first().evaluate("c => { const d = c.getContext('2d').getImageData(c.width / 2, c.height / 2, 1, 1).data; return d[3]; }"))
                    .as("the thumbnail is drawn").isEqualTo(255);
            if (width >= 1000) {
                assertThat(items.nth(0).evaluate("a => a.getBoundingClientRect().top")).as("a grid row")
                        .isEqualTo(items.nth(1).evaluate("a => a.getBoundingClientRect().top"));
            }
            assertThat(descriptions(form).first()).hasText(Pattern.compile("B$"));

            // The list: small attachments, and a rejected file as an error card with the reason
            Locator documents = p.locator("form[aria-label='Ajax dropzone']");
            documents.locator("input[type=file]").setInputFiles(png);
            Locator rejected = documents.locator(".sw-file-upload-item > .sw-attachment");
            assertThat(rejected).hasAttribute("data-size", "sm");
            assertThat(rejected).hasAttribute("data-state", "error");
            assertThat(rejected.locator(".sw-attachment-description")).hasText("This file type is not accepted.");
            form.scrollIntoViewIfNeeded();
            audit(p);
            // After the audit: hiding the caret for a screenshot leaves empty style attributes on inputs
            p.screenshot(new Page.ScreenshotOptions().setPath(Path.of("target/upload-screenshots", "grid-" + engine + "-" + skin + "-" + mode + ".png")));

            // Removing from the keyboard: the next file's remove button takes the focus
            Locator remove = form.getByRole(AriaRole.BUTTON, new Locator.GetByRoleOptions().setName("Remove meadow.png"));
            remove.focus();
            p.keyboard().press("Enter");
            assertThat(items).hasCount(4);
            assertThat(form.getByRole(AriaRole.BUTTON, new Locator.GetByRoleOptions().setName("Remove roof.jpg"))).isFocused();
            assertThat(form.locator("canvas")).hasCount(1);
            assertThat(errors).isEmpty();
        }
    }

    @ParameterizedTest @MethodSource("chromiumSkins")
    void eachFileShowsItsProgressThenProcessingThenUploaded(String skin) {
        try (var context = chromium.newContext(new Browser.NewContextOptions().setViewportSize(1280, 1000)); var p = context.newPage()) {
            var errors = errors(p);
            p.navigate(url(skin, "light"));
            Locator form = photos(p);
            Locator root = form.locator(".sw-file-upload");
            form.locator("input[type=file]").setInputFiles(new FilePayload[]{png, pdf});
            throttle(context, p, 80 * 1024);
            form.getByRole(AriaRole.BUTTON, new Locator.GetByRoleOptions().setName("Send photos")).click();

            assertThat(root).hasAttribute("data-uploading", "sending");
            assertThat(form.getByRole(AriaRole.BUTTON, new Locator.GetByRoleOptions().setName("Cancel upload"))).isVisible();
            assertThat(form.locator(".sw-file-upload-browse")).isHidden();
            // The remove buttons are hidden while the files are sent
            assertThat(form.locator(".sw-attachment-actions").first()).isHidden();
            Locator last = descriptions(form).nth(1);
            assertThat(last).hasText(Pattern.compile("Uploading · [1-9]\\d?%"), new LocatorAssertions.HasTextOptions().setTimeout(15_000));
            assertThat(form.locator(".sw-attachment").first()).hasAttribute("data-state", Pattern.compile("uploading|processing"));
            assertThat(form.locator(".sw-attachment").first()).hasAttribute("aria-busy", "true");
            assertThat(p.locator("[style]")).hasCount(0);

            // All sent: the server works on them, and Cancel is gone
            assertThat(root).hasAttribute("data-uploading", "processing", new LocatorAssertions.HasAttributeOptions().setTimeout(15_000));
            assertThat(descriptions(form)).hasText(new String[]{"Processing", "Processing"});
            assertThat(form.locator(".sw-file-upload-cancel")).isHidden();

            // The callback cleared the selection: the files show as uploaded and have left the input
            assertThat(descriptions(form)).hasText(new String[]{"Uploaded", "Uploaded"}, new LocatorAssertions.HasTextOptions().setTimeout(10_000));
            assertThat(root).not().hasAttribute("data-uploading", Pattern.compile(".*"));
            assertThat(form.locator(".sw-file-upload-input")).not().hasAttribute("aria-disabled", "true");
            assertThat(form.locator(".sw-file-upload-input").evaluate("i => i.files.length")).isEqualTo(0);
            assertThat(form.locator(".sw-file-upload-status")).hasText("2 file(s) uploaded.");
            assertThat(form.getByText("Received meadow.png")).isVisible();
            audit(p);
            // A new choice ends the uploaded list
            form.locator("input[type=file]").setInputFiles(jpg);
            assertThat(form.locator(".sw-file-upload-item")).hasCount(1);
            assertThat(errors).isEmpty();
        }
    }

    @ParameterizedTest @ValueSource(strings = {"vega", "sketch"})
    void cancelStopsTheUploadAndLeavesTheFilesReadyToSendAgain(String skin) {
        try (var context = chromium.newContext(new Browser.NewContextOptions().setViewportSize(1280, 1000)); var p = context.newPage()) {
            var errors = errors(p);
            p.navigate(url(skin, "light"));
            Locator form = photos(p);
            form.locator("input[type=file]").setInputFiles(new FilePayload[]{png, pdf});
            CDPSession network = throttle(context, p, 40 * 1024);
            form.getByRole(AriaRole.BUTTON, new Locator.GetByRoleOptions().setName("Send photos")).click();
            Locator cancel = form.getByRole(AriaRole.BUTTON, new Locator.GetByRoleOptions().setName("Cancel upload"));
            assertThat(cancel).isVisible();
            // The input refuses new files meanwhile
            assertThat(form.locator(".sw-file-upload-input")).hasAttribute("aria-disabled", "true");
            cancel.focus();
            p.keyboard().press("Enter");
            Locator root = form.locator(".sw-file-upload");
            assertThat(root).not().hasAttribute("data-uploading", Pattern.compile(".*"));
            assertThat(descriptions(form).first()).hasText(Pattern.compile("B$"));
            assertThat(form.locator(".sw-file-upload-input")).isFocused();
            assertThat(form.locator(".sw-file-upload-input").evaluate("i => i.files.length")).isEqualTo(2);
            assertThat(form.locator(".sw-file-upload-status")).hasText("Upload cancelled.");
            assertThat(form.getByRole(AriaRole.BUTTON, new Locator.GetByRoleOptions().setName("Remove meadow.png"))).isVisible();

            // Sent again at full speed, they arrive
            throttle(network, -1);
            form.getByRole(AriaRole.SWITCH, new Locator.GetByRoleOptions().setName(Pattern.compile("Slow server"))).uncheck();
            form.getByRole(AriaRole.BUTTON, new Locator.GetByRoleOptions().setName("Send photos")).click();
            assertThat(descriptions(form)).hasText(new String[]{"Uploaded", "Uploaded"}, new LocatorAssertions.HasTextOptions().setTimeout(10_000));
            assertThat(errors).isEmpty();
        }
    }

    @ParameterizedTest @ValueSource(strings = {"chromium", "firefox"})
    void aFailedRequestKeepsTheFilesAndProcessingShowsWhileTheServerWorks(String engine) {
        try (var context = browser(engine).newContext(new Browser.NewContextOptions().setViewportSize(1280, 1000)); var p = context.newPage()) {
            var errors = errors(p);
            p.navigate(url("vega", "light") + "&lang=de-DE");
            Locator form = photos(p);
            Locator root = form.locator(".sw-file-upload");
            form.locator("input[type=file]").setInputFiles(new FilePayload[]{png, jpg});
            // The connection drops (a parsed error page would only add the browser's own XML parser complaints)
            p.route(Pattern.compile(".*photoForm-send.*"), route -> route.abort("failed"));
            form.locator("button[type=submit]").first().click();
            assertThat(descriptions(form)).hasText(new String[]{"Hochladen fehlgeschlagen", "Hochladen fehlgeschlagen"});
            assertThat(form.locator(".sw-attachment").first()).hasAttribute("data-state", "error");
            assertThat(form.locator(".sw-file-upload-input").evaluate("i => i.files.length")).isEqualTo(2);
            assertThat(form.locator(".sw-file-upload-status")).containsText("konnten nicht hochgeladen werden");
            assertThat(form.locator(".sw-file-upload-item [data-sw-remove-file]")).hasCount(2);
            p.unroute(Pattern.compile(".*photoForm-send.*"));

            // Sent again: the slow server's two seconds show as processing, then the files are uploaded
            form.locator("button[type=submit]").first().click();
            assertThat(root).hasAttribute("data-uploading", "processing");
            assertThat(descriptions(form)).hasText(new String[]{"Wird verarbeitet", "Wird verarbeitet"});
            assertThat(descriptions(form)).hasText(new String[]{"Hochgeladen", "Hochgeladen"}, new LocatorAssertions.HasTextOptions().setTimeout(10_000));
            assertThat(errors).isEmpty();
        }
    }
}
