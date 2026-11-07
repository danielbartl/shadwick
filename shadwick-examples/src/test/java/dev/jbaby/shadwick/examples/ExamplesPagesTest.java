package dev.jbaby.shadwick.examples;

import org.apache.wicket.markup.html.WebPage;
import org.apache.wicket.util.tester.FormTester;
import org.apache.wicket.util.tester.TagTester;
import org.apache.wicket.util.tester.WicketTester;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.junit.jupiter.params.provider.CsvSource;
import java.util.Locale;
import org.apache.wicket.request.mapper.parameter.PageParameters;
import org.junit.jupiter.params.provider.EnumSource;
import dev.jbaby.shadwick.Skin;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Every showcase page, rendered by the showcase's own application in development mode, where an accessibility mistake
 * Shadwick detects fails the render.
 */
class ExamplesPagesTest {

    private WicketTester tester;

    @BeforeEach
    void setUp() {
        tester = new WicketTester(new ExamplesApplication());
        // Keeps wicket:id in the markup, which getTagByWicketId looks for
        tester.getApplication().getMarkupSettings().setStripWicketTags(false);
    }

    @AfterEach
    void tearDown() {
        tester.destroy();
    }

    private String html() {
        return tester.getLastResponseAsString();
    }

    @Test
    void healthcheckReturnsAnUncachedResponseWithoutBindingAVisitorSession() {
        tester.executeUrl("healthz");
        assertThat(tester.getLastResponse().getStatus()).isEqualTo(200);
        assertThat(html()).isEqualTo("ok");
        assertThat(tester.getLastResponse().getHeader("Cache-Control")).contains("no-cache");
        assertThat(tester.getSession().isTemporary()).isTrue();
    }

    @SuppressWarnings("unchecked")
    private static Class<? extends WebPage> page(String name) throws ClassNotFoundException {
        return (Class<? extends WebPage>) Class.forName("dev.jbaby.shadwick.examples." + name);
    }

    @ParameterizedTest
    @ValueSource(strings = {"HomePage", "InternationalizationPage", "StatusControlsPage", "AdvancedTablePage", "ButtonPage", "FormPage", "InputPage", "ChoicePage", "SelectPage", "CustomSelectPage", "ComboboxPage", "CalendarPage", "MonthViewPage", "ChartPage", "DateTimePage", "GroupPage", "UploadPage", "DialogPage", "OverlayPage", "PopupPage", "MenuPage", "ToastPage", "ShellPage", "SidebarPage", "ThemePage", "AppLayoutPage", "HeaderLayoutPage", "DisclosurePage", "AttachmentPage", "DataTablePage", "InvoicePage", "TablePage", "PaginationPage", "UserMenuPage", "UserSignInPage", "DisplayPage", "FeedbackPage"})
    void everyPageRendersInTheDefaultSkinWithoutInlineStylesOrHandlers(String name) throws ClassNotFoundException {
        tester.startPage(page(name));

        tester.assertRenderedPage(page(name));
        assertThat(html()).containsPattern("css/shadwick-vega[^\"]*\\.css").containsPattern("examples[^\"]*\\.css")
                .doesNotContain("style=").doesNotContainPattern("\\son[a-z]+=");
        assertThat(TagTester.createTagByName(html(), "html").getAttribute("class")).isNull();
    }

    @ParameterizedTest
    @EnumSource(value = Skin.class, names = "VEGA", mode = EnumSource.Mode.EXCLUDE)
    void theSkinSwitcherKeepsTheSkinForTheSession(Skin selected) {
        String skin = selected.id();
        tester.startPage(ButtonPage.class, new org.apache.wicket.request.mapper.parameter.PageParameters().add("skin", skin));
        assertThat(html()).contains("shadwick-" + skin).doesNotContain("shadwick-vega");
        assertThat(tester.getTagByWicketId(skin).getAttribute("aria-current")).isEqualTo("true");
        assertThat(tester.getTagByWicketId("vega").getAttribute("aria-current")).isNull();

        tester.startPage(FormPage.class);
        assertThat(html()).contains("shadwick-" + skin).doesNotContain("shadwick-vega");
    }

    @Test
    void darkModeIsTheDarkClassOnTheHtmlElement() {
        tester.startPage(HomePage.class, new org.apache.wicket.request.mapper.parameter.PageParameters().add("mode", "dark"));
        assertThat(TagTester.createTagByName(html(), "html").getAttribute("class")).isEqualTo("dark");

        tester.startPage(DialogPage.class);
        assertThat(TagTester.createTagByName(html(), "html").getAttribute("class")).isEqualTo("dark");

        tester.startPage(DialogPage.class, new org.apache.wicket.request.mapper.parameter.PageParameters().add("mode", "light"));
        assertThat(TagTester.createTagByName(html(), "html").getAttribute("class")).isNull();
    }

    @ParameterizedTest
    @CsvSource({"en,Language,Apply,Records,Actions", "de,Sprache,Anwenden,Datensätze,Aktionen", "es,Idioma,Aplicar,Registros,Acciones",
            "fr,Langue,Appliquer,Enregistrements,Actions", "it,Lingua,Applica,Record,Azioni", "ja,言語,適用,データ,操作",
            "nl,Taal,Toepassen,Gegevens,Acties", "pt,Idioma,Aplicar,Registros,Ações", "hr,Jezik,Primijeni,Zapisi,Radnje"})
    void theShowcaseAndTableDefaultsUseTheSelectedLanguage(String language, String label, String apply, String records, String actions) {
        tester.startPage(InternationalizationPage.class, new PageParameters().set("lang", language));
        assertThat(TagTester.createTagByName(html(), "html").getAttribute("lang")).isEqualTo(language);
        tester.assertLabel("languageForm:languageLabel", label);
        assertThat(tester.getTagByWicketId("applyLanguage").getValue()).contains(apply);
        assertThat(tester.getTagByWicketId("table").getAttribute("aria-label")).isEqualTo(records);
        assertThat(TagTester.createTagsByAttribute(html(), "class", "sw-table-column-label", false))
                .anySatisfy(tag -> assertThat(tag.getValue()).isEqualTo(actions));
        tester.clickLink("form:refresh");
        tester.assertComponentOnAjaxResponse("form");
        assertThat(html()).doesNotContain("style=").doesNotContainPattern("\\son[a-z]+=");
    }

    @Test void theNativeLanguageFormPreservesThePageAndPreferences() {
        tester.startPage(InternationalizationPage.class, new PageParameters().set("skin", "sketch").set("mode", "dark").set("lang", "de-AT"));
        FormTester form = tester.newFormTester("languageForm");
        form.select("language", 5); // 日本語, stable language tag values in real HTML
        form.submit("applyLanguage");
        tester.assertRenderedPage(InternationalizationPage.class);
        assertThat(tester.getSession().getLocale()).isEqualTo(Locale.JAPANESE);
        assertThat(html()).contains("shadwick-sketch", "キャンセル");
        assertThat(TagTester.createTagByName(html(), "html").getAttribute("class")).isEqualTo("dark");
        tester.startPage(ButtonPage.class);
        assertThat(TagTester.createTagByName(html(), "html").getAttribute("lang")).isEqualTo("ja");
        tester.assertLabel("languageForm:languageLabel", "言語");
    }

    @Test
    void theCurrentPageIsMarkedInTheNavigation() {
        tester.startPage(FormPage.class);

        TagTester current = tester.getTagByWicketId("navForm");
        assertThat(current.getAttribute("aria-current")).isEqualTo("page");
        assertThat(current.getAttribute("data-variant")).isEqualTo("secondary");
        assertThat(tester.getTagByWicketId("navButtons").getAttribute("data-variant")).isEqualTo("ghost");
    }

    @Test
    void aButtonUpdatesTheStatusOverAjax() {
        tester.startPage(ButtonPage.class);

        tester.clickLink("buttons:2:button");

        tester.assertComponentOnAjaxResponse("status");
        assertThat(html()).contains("Clicked Outline.");
    }

    @Test
    void theNormalFormShowsItsErrorsInlineAndSaves() {
        tester.startPage(FormPage.class);
        FormTester form = tester.newFormTester("form");
        form.setValue("email:input", "not an email");
        form.submit("save");

        assertThat(html()).contains("&#039;Name&#039; is required.").contains("is not a valid email address.");

        form = tester.newFormTester("form");
        form.setValue("name:input", "Grace Hopper");
        form.setValue("email:input", "grace@example.com");
        form.submit("save");

        tester.assertNoErrorMessage();
        assertThat(html()).contains("Saved Grace Hopper &lt;grace@example.com&gt;.");
    }

    @Test
    void theAjaxFormShowsItsErrorsInPlace() {
        tester.startPage(FormPage.class);

        tester.newFormTester("ajaxForm").setValue("name:input", "");
        tester.executeAjaxEvent("ajaxForm:save", "click");

        assertThat(html()).contains("Shadwick.fieldFeedback(").contains("is required.");
    }

    @Test
    void theDeleteDialogOpensOverAjaxAndConfirms() {
        tester.startPage(DialogPage.class);

        tester.clickLink("openDelete");
        assertThat(html()).contains("Shadwick.showDialog(");

        tester.executeAjaxEvent("delete:dialog:form:confirm", "click");
        assertThat(html()).contains("The project was deleted.").contains(".close()");
    }

    @Test
    void theProfileDialogStaysOpenOnAnError() {
        tester.startPage(DialogPage.class);

        tester.newFormTester("edit:dialog:form").setValue("content:name:input", "admin");
        tester.executeAjaxEvent("edit:dialog:form:confirm", "click");
        assertThat(html()).contains("That name is reserved.").doesNotContain(".close()");

        tester.newFormTester("edit:dialog:form").setValue("content:name:input", "Ada King");
        tester.executeAjaxEvent("edit:dialog:form:confirm", "click");
        assertThat(html()).contains("Saved Ada King").contains(".close()");
    }
}
