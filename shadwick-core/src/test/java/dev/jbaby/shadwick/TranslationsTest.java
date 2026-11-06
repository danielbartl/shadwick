package dev.jbaby.shadwick;

import dev.jbaby.shadwick.behaviors.*;
import dev.jbaby.shadwick.components.SwDialog;
import dev.jbaby.shadwick.components.form.SwCombobox;
import dev.jbaby.shadwick.components.form.SwDateField;
import org.apache.wicket.MarkupContainer;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.markup.html.AjaxLink;
import org.apache.wicket.markup.IMarkupResourceStreamProvider;
import org.apache.wicket.markup.Markup;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.WebPage;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.model.Model;
import org.apache.wicket.protocol.http.WebApplication;
import org.apache.wicket.util.resource.IResourceStream;
import org.apache.wicket.util.resource.StringResourceStream;
import org.apache.wicket.util.tester.TagTester;
import org.apache.wicket.util.tester.WicketTester;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;

import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Locale;
import java.util.Properties;
import java.util.Set;
import java.util.TreeSet;
import java.util.regex.Pattern;

import static org.assertj.core.api.Assertions.assertThat;

/** The actual Wicket lookup path, including UTF-8, fallback, overrides and Ajax rendering. */
class TranslationsTest {
    private WicketTester tester;
    @BeforeEach void setup() { tester = new WicketTester(); }
    @AfterEach void cleanup() { tester.destroy(); }

    @ParameterizedTest
    @CsvSource({"de,Abbrechen,Bestätigen", "es,Cancelar,Confirmar", "fr,Annuler,Confirmer",
            "it,Annulla,Conferma", "ja,キャンセル,確認", "nl,Annuleren,Bevestigen", "pt,Cancelar,Confirmar", "hr,Odustani,Potvrdi"})
    void dialogDefaultsFollowTheLocale(String language, String cancel, String confirm) {
        tester.getSession().setLocale(Locale.forLanguageTag(language));
        tester.startComponentInPage(new SwDialog("dialog", "Edit"));
        assertThat(tester.getTagByWicketId("cancelLabel").getValue()).isEqualTo(cancel);
        assertThat(tester.getTagByWicketId("confirmLabel").getValue()).isEqualTo(confirm);
    }

    @ParameterizedTest @CsvSource({"de-AT,Abbrechen", "fr-CA,Annuler", "pt-BR,Cancelar", "hr-HR,Odustani", "fi,Cancel"})
    void regionalAndUnsupportedLocalesHavePredictableFallbacks(String tag, String cancel) {
        tester.getSession().setLocale(Locale.forLanguageTag(tag));
        tester.startComponentInPage(new SwDialog("dialog", "Edit"));
        assertThat(tester.getTagByWicketId("cancelLabel").getValue()).isEqualTo(cancel);
    }

    @Test void anExplicitComponentLocaleWinsOverTheSession() {
        tester.getSession().setLocale(Locale.GERMAN);
        tester.startComponentInPage(new SwDialog("dialog", "Edit") {
            @Override public Locale getLocale() { return Locale.FRENCH; }
        });
        assertThat(tester.getTagByWicketId("cancelLabel").getValue()).isEqualTo("Annuler");
    }

    @Test void pageAndApplicationResourcesOverrideLibraryTranslations() {
        tester.destroy(); tester = new WicketTester(new OverrideApp());
        tester.getSession().setLocale(Locale.GERMAN); tester.startPage(OverridePage.class);
        assertThat(tester.getTagByWicketId("cancelLabel").getValue()).isEqualTo("Verwerfen (Seite)");
        assertThat(tester.getTagByWicketId("confirmLabel").getValue()).isEqualTo("Speichern (App)");
    }

    @Test void behaviorsOnCallerOwnedMarkupFindTheirLibraryBundles() {
        tester.getSession().setLocale(Locale.GERMAN);
        WebMarkupContainer root = new WebMarkupContainer("root");
        root.add(new WebMarkupContainer("spinner").add(new SpinnerBehavior()));
        root.add(new WebMarkupContainer("pagination").add(new PaginationBehavior()));
        root.add(new WebMarkupContainer("calendar").add(new CalendarBehavior()));
        root.add(new WebMarkupContainer("combobox").add(new ComboboxBehavior()));
        tester.startComponentInPage(root, Markup.of("<div wicket:id='root'><span wicket:id='spinner'></span><nav wicket:id='pagination'></nav><div wicket:id='calendar'></div><div wicket:id='combobox'></div></div>"));
        assertThat(tester.getTagByWicketId("spinner").getAttribute("aria-label")).isEqualTo("Wird geladen");
        assertThat(tester.getTagByWicketId("pagination").getAttribute("aria-label")).isEqualTo("Seitennavigation");
        assertThat(tester.getTagByWicketId("calendar").getAttribute("data-previous")).isEqualTo("Zum vorherigen Monat");
        assertThat(tester.getTagByWicketId("combobox").getAttribute("data-placeholder")).isEqualTo("Suchen…");
        assertThat(tester.getTagByWicketId("combobox").getAttribute("data-empty")).isEqualTo("Keine Einträge gefunden.");
    }

    @Test void ajaxReplacementReevaluatesTheLocaleAndKeepsCustomAndEmptyPlaceholders() {
        tester.getSession().setLocale(Locale.GERMAN); ComboPage page = new ComboPage(); tester.startPage(page);
        assertThat(placeholder()).isEqualTo("Suchen…");
        tester.getSession().setLocale(Locale.FRENCH); tester.clickLink("refresh");
        tester.assertComponentOnAjaxResponse(page.choice);
        assertThat(tester.getLastResponseAsString()).contains("Rechercher…", "Aucun élément trouvé.");
        page.choice.setPlaceholder("Choose your own wording"); tester.clickLink("refresh");
        assertThat(tester.getLastResponseAsString()).contains("Choose your own wording");
        page.choice.setPlaceholder((String) null); tester.clickLink("refresh");
        assertThat(tester.getLastResponseAsString()).contains("data-placeholder=\"\"");
    }

    private String placeholder() {
        return TagTester.createTagsByAttribute(tester.getLastResponseAsString(), "data-placeholder", "true", false).get(0).getValue();
    }

    @ParameterizedTest @CsvSource({"de,ist kein gültiger Wert", "es,no es un valor válido", "fr,n’est pas une valeur valide",
            "it,non è un valore valido", "ja,有効な値ではありません", "nl,is geen geldige waarde", "pt,não é um valor válido", "hr,nije valjana vrijednost"})
    void nativeDateConversionErrorsUseUtf8MessagesAndPreserveTheIsoFormat(String language, String expected) {
        tester.getSession().setLocale(Locale.forLanguageTag(language));
        Form<Void> form = new Form<>("form"); form.add(new SwDateField("day", "Day", Model.of()));
        tester.startComponentInPage(form, Markup.of("<form wicket:id='form'><div wicket:id='day'></div></form>"));
        var input = tester.newFormTester("form"); input.setValue("day:input", "not-a-date"); input.submit();
        assertThat(tester.getLastResponseAsString()).contains(expected, "YYYY-MM-DD").doesNotContain("${label}", "${format}");
    }

    @Test void fullRenderAndAjaxFeedbackToastsUseLocalizedRegionAndDismissalLabels() {
        tester.getSession().setLocale(Locale.JAPANESE); ToastPage page = new ToastPage(); tester.startPage(page);
        assertThat(tester.getLastResponseAsString()).contains("通知 (Alt+T)", "通知を閉じる");
        tester.getSession().setLocale(Locale.GERMAN); tester.clickLink("notify");
        assertThat(tester.getLastResponseAsString()).contains("Benachrichtigungen (Alt+T)", "Benachrichtigung schließen");
    }

    @ParameterizedTest @ValueSource(strings = {"de", "es", "fr", "it", "ja", "nl", "pt", "hr"})
    void everyBundleHasExactlyTheEnglishKeysAndPlaceholders(String language) throws Exception {
        Path root = Path.of("src/main/resources/dev/jbaby/shadwick");
        List<Path> bases;
        try (var files = Files.walk(root)) {
            bases = files.filter(p -> p.getFileName().toString().matches("[A-Za-z][A-Za-z0-9]*\\.properties"))
                    .filter(p -> !p.getParent().endsWith("icons")).toList();
        }
        assertThat(bases).hasSizeGreaterThanOrEqualTo(31);
        for (Path base : bases) {
            Path translation = base.resolveSibling(base.getFileName().toString().replace(".properties", "_" + language + ".utf8.properties"));
            assertThat(translation).exists();
            Properties english = load(base, false), translated = load(translation, true);
            assertThat(translated.stringPropertyNames()).as(translation.toString()).isEqualTo(english.stringPropertyNames());
            for (String key : english.stringPropertyNames()) {
                String text = translated.getProperty(key);
                assertThat(text).as(key + " in " + translation).isNotBlank().doesNotContain("\uFFFD");
                assertThat(arguments(text)).as(key + " placeholders in " + translation).isEqualTo(arguments(english.getProperty(key)));
                String resource = "/" + Path.of("src/main/resources").relativize(translation).toString().replace('\\', '/');
                assertThat(Shadwick.class.getResource(resource)).isNotNull();
            }
        }
    }

    private static Set<String> arguments(String text) {
        Set<String> values = new TreeSet<>();
        var matcher = Pattern.compile("\\$\\{[^}]+}|\\{[0-9]+}").matcher(text);
        while (matcher.find()) values.add(matcher.group());
        return values;
    }
    private static Properties load(Path file, boolean utf8) throws Exception {
        Properties properties = new Properties();
        try (Reader reader = new InputStreamReader(Files.newInputStream(file), utf8 ? StandardCharsets.UTF_8 : StandardCharsets.ISO_8859_1)) {
            properties.load(reader);
        }
        return properties;
    }

    public static class OverrideApp extends WebApplication {
        @Override public Class<OverridePage> getHomePage() { return OverridePage.class; }
        @Override protected void init() {
            super.init();
            // Wicket normally checks component bundles first. An app can explicitly prioritize its bundle.
            getResourceSettings().getStringResourceLoaders().add(0,
                    new org.apache.wicket.resource.loader.ClassStringResourceLoader(OverrideApp.class));
        }
    }
    public static class OverridePage extends WebPage implements IMarkupResourceStreamProvider {
        public OverridePage() { add(new SwDialog("dialog", "Edit")); }
        @Override public IResourceStream getMarkupResourceStream(MarkupContainer container, Class<?> type) {
            return new StringResourceStream("<html><body><div wicket:id='dialog'></div></body></html>");
        }
    }
    public static class ComboPage extends WebPage implements IMarkupResourceStreamProvider {
        final SwCombobox<String> choice = new SwCombobox<>("choice", "Choice", Model.of(), Model.ofList(List.of("One", "Two")));
        public ComboPage() {
            add(choice); choice.setOutputMarkupId(true);
            add(new AjaxLink<Void>("refresh") { @Override public void onClick(AjaxRequestTarget target) { target.add(choice); } });
        }
        @Override public IResourceStream getMarkupResourceStream(MarkupContainer container, Class<?> type) {
            return new StringResourceStream("<html><body><div wicket:id='choice'></div><a wicket:id='refresh'>Refresh</a></body></html>");
        }
    }
    public static class ToastPage extends WebPage implements IMarkupResourceStreamProvider {
        public ToastPage() {
            add(new FeedbackToastsBehavior()); info("Welcome");
            add(new AjaxLink<Void>("notify") { @Override public void onClick(AjaxRequestTarget target) { info("Again"); } });
        }
        @Override public IResourceStream getMarkupResourceStream(MarkupContainer container, Class<?> type) {
            return new StringResourceStream("<html><body><a wicket:id='notify'>Notify</a></body></html>");
        }
    }
}
