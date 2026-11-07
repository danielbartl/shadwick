package dev.jbaby.shadwick.examples;

import dev.jbaby.shadwick.Skin;
import org.apache.wicket.util.tester.WicketTester;
import org.junit.jupiter.api.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import static org.assertj.core.api.Assertions.*;

class StudioPageTest {
    private WicketTester tester;
    @BeforeEach void start() { tester = new WicketTester(new ExamplesApplication()); }
    @AfterEach void stop() { tester.destroy(); }

    @ParameterizedTest @ValueSource(strings = {"overview", "projects", "project", "invoices"})
    void everyScreenRendersAcrossAllSkins(String view) {
        for (Skin skin : Skin.values()) {
            tester.startPage(StudioPage.class, StudioPage.route(view, 1).set("skin", skin.id()));
            tester.assertRenderedPage(StudioPage.class);
            assertThat(tester.getLastResponseAsString()).contains("Clover", "studio", "shadwick-" + skin.id()).doesNotContain("style=", "onclick=");
        }
    }
    @Test void projectCreationUsesValidatedFieldsAndUpdatesNavigation() {
        tester.startPage(StudioPage.class);
        tester.clickLink("newProject");
        var form = tester.newFormTester("projectEditor:dialog:form");
        form.setValue("content:name:input", "A brighter future"); form.setValue("content:client:input", "Bright Studio");
        form.setValue("content:category:input", "Identity"); form.setValue("content:due:input", "2027-01-20"); form.setValue("content:budget:input", "4200");
        tester.executeAjaxEvent("projectEditor:dialog:form:confirm", "click");
        tester.assertNoErrorMessage();
        assertThat(StudioWorkspace.get().projects()).hasSize(5);
        assertThat(tester.getLastResponseAsString()).contains("Bright Studio", "A brighter future");
    }
    @Test void taskActionUpdatesProgressAndOldPageCannotMutateAResetWorkspace() {
        tester.startPage(StudioPage.class, StudioPage.route("project", 1));
        tester.clickLink("content:taskItems:tasks:2:row:toggle");
        assertThat(StudioWorkspace.get().project(1).progress()).isEqualTo(75);
        StudioWorkspace.reset();
        tester.clickLink("content:taskItems:tasks:3:row:toggle");
        assertThat(StudioWorkspace.get().project(1).progress()).isEqualTo(50);
        assertThat(tester.getLastResponseAsString()).contains("reset in another tab");
    }
    @Test void invalidProjectAndConflictingEditsRemainInTheDialogWithoutChangingTheStore() {
        tester.startPage(StudioPage.class);
        tester.clickLink("newProject");
        tester.newFormTester("projectEditor:dialog:form");
        tester.executeAjaxEvent("projectEditor:dialog:form:confirm", "click");
        assertThat(StudioWorkspace.get().projects()).hasSize(4);
        assertThat(((dev.jbaby.shadwick.components.SwDialog) tester.getLastRenderedPage().get("projectEditor")).isOpen()).isTrue();
        tester.startPage(StudioPage.class, StudioPage.route("project", 1)); tester.clickLink("content:edit");
        StudioWorkspace.get().completeTask(1, 13, false);
        var form = tester.newFormTester("projectEditor:dialog:form");
        form.setValue("content:name:input", "A conflicting edit");
        tester.executeAjaxEvent("projectEditor:dialog:form:confirm", "click");
        assertThat(StudioWorkspace.get().project(1).name()).isEqualTo("A little closer to nature.");
        assertThat(tester.getLastResponseAsString()).contains("Refresh before editing");
    }
    @Test void languageAndSkinPreferencesPreserveWorkspaceData() {
        tester.startPage(StudioPage.class); var initial = StudioWorkspace.get().generation; StudioWorkspace.get().pay(2);
        for (String language : java.util.List.of("en", "de")) {
            tester.startPage(StudioPage.class, StudioPage.route("invoices", 0).set("lang", language).set("skin", "nova"));
            assertThat(StudioWorkspace.get().generation).isEqualTo(initial); assertThat(StudioWorkspace.get().invoice(2).paid()).isTrue();
            assertThat(tester.getLastResponseAsString()).contains("lang=\"" + language + "\"", "shadwick-nova");
        }
    }
    @Test void languagePickerTranslatesAllScreensAndPreservesEnteredContentAndProgress() {
        tester.startPage(StudioPage.class); var generation = StudioWorkspace.get().generation;
        var preferences = tester.newFormTester("provider:workspace:topNav:topNavExtra:settings");
        preferences.select("language", 1); preferences.submit("apply");
        tester.assertLabel("heading", "Ein guter Tag für neue Ideen.");
        assertThat(tester.getLastResponseAsString()).contains("Aktive Projekte", "Demo-Einstellungen", "Sprache", "Einstellungen anwenden").doesNotContain("Application copy stays in English");
        tester.startPage(StudioPage.class, StudioPage.route("projects", 0));
        tester.assertLabel("heading", "Gute Arbeit nimmt Gestalt an.");
        assertThat(tester.getLastResponseAsString()).contains("Projekte durchsuchen", "In Bearbeitung", "Alle Projekte");
        tester.startPage(StudioPage.class, StudioPage.route("project", 1)); tester.clickLink("content:taskItems:tasks:2:row:toggle");
        assertThat(tester.getLastResponseAsString()).contains("Aufgabe abgeschlossen", "3 von 4 abgeschlossen");
        assertThat(StudioWorkspace.get().project(1).tasks().get(2).title()).isEqualTo("Review the mobile experience");
        tester.startPage(StudioPage.class, StudioPage.route("invoices", 0));
        assertThat(tester.getLastResponseAsString()).contains("Rechnungen", "Bezahlt", "Zahlung ausstehend", "CSV exportieren");
        preferences = tester.newFormTester("provider:workspace:topNav:topNavExtra:settings"); preferences.select("language", 0); preferences.submit("apply");
        tester.assertLabel("heading", "The business of good work.");
        assertThat(StudioWorkspace.get().generation).isEqualTo(generation); assertThat(StudioWorkspace.get().project(1).progress()).isEqualTo(75);
    }
    @Test void unsupportedStudioLocalesFallBackToEnglishAndOnlyTwoLanguagesAreOffered() {
        tester.getSession().setLocale(java.util.Locale.forLanguageTag("hr"));
        tester.startPage(StudioPage.class);
        tester.assertLabel("heading", "A good day to create.");
        var language = (org.apache.wicket.markup.html.form.DropDownChoice<?>) tester.getLastRenderedPage().get("provider:workspace:topNav:topNavExtra:settings:language");
        assertThat(language.getChoices().stream().map(Object::toString).toList()).containsExactly("en", "de");
        tester.startPage(StudioPage.class, StudioPage.route("overview", 0).set("lang", "fr"));
        assertThat(tester.getSession().getLocale()).isEqualTo(java.util.Locale.ENGLISH);
    }
    @Test void projectFiltersUseNativeGetWithStableParameterNamesAndValues() {
        tester.getApplication().getMarkupSettings().setStripWicketTags(false);
        tester.startPage(StudioPage.class, StudioPage.route("projects", 0).set("lang", "de").set("q", "Sunday").set("status", "In progress"));
        assertThat(tester.getTagByWicketId("filters").getAttribute("method")).isEqualTo("get");
        assertThat(tester.getLastResponseAsString()).contains("name=\"q\"", "name=\"status\"", "name=\"sort\"", "name=\"view\" value=\"projects\"", "value=\"In progress\"", "In Bearbeitung");
        assertThat(tester.getLastResponseAsString()).contains("Sunday Coffee").doesNotContain("Common Ground</strong>");
    }
    @Test void germanBundleCoversEveryEnglishMessage() throws Exception {
        var english = new java.util.Properties(); var german = new java.util.Properties();
        try (var stream = StudioPage.class.getResourceAsStream("StudioPage.properties")) { english.load(new java.io.InputStreamReader(stream, java.nio.charset.StandardCharsets.UTF_8)); }
        try (var stream = StudioPage.class.getResourceAsStream("StudioPage_de.utf8.properties")) { german.load(new java.io.InputStreamReader(stream, java.nio.charset.StandardCharsets.UTF_8)); }
        assertThat(german.keySet()).containsExactlyInAnyOrderElementsOf(english.keySet());
        assertThat(german.values()).allSatisfy(value -> assertThat(value.toString()).isNotBlank());
    }
    @Test void composedPageSurvivesWicketPassivationAndStillHandlesActions() {
        tester.startPage(StudioPage.class, StudioPage.route("project", 1));
        var serializer = tester.getApplication().getFrameworkSettings().getSerializer();
        var restored = (StudioPage) serializer.deserialize(serializer.serialize(tester.getLastRenderedPage()));
        assertThat(restored).isNotNull(); tester.startPage(restored);
        tester.clickLink("content:taskItems:tasks:2:row:toggle");
        assertThat(StudioWorkspace.get().project(1).progress()).isEqualTo(75);
    }
    @Test void completingATaskReturnsOnlyTheChangedRowProgressAndFeedback() {
        tester.startPage(StudioPage.class, StudioPage.route("project", 1));
        var page = tester.getLastRenderedPage();
        var expected = java.util.Set.of(page.get("content:taskItems:tasks:2:row").getMarkupId(), page.get("content:progress").getMarkupId(),
                page.get("content:percent").getMarkupId(), page.get("content:taskCount").getMarkupId(), page.get("feedback").getMarkupId());
        tester.clickLink("content:taskItems:tasks:2:row:toggle");
        java.util.Set<String> actual = new java.util.HashSet<>();
        var ids = java.util.regex.Pattern.compile("<component id=\"([^\"]+)\"").matcher(tester.getLastResponseAsString());
        while (ids.find()) actual.add(ids.group(1));
        assertThat(actual).containsExactlyInAnyOrderElementsOf(expected);
        assertThat(tester.getLastResponseAsString()).doesNotContain("<redirect>", "window.location");
        assertThat(tester.getLastRenderedPage()).isSameAs(page);
        tester.clickLink("content:taskItems:tasks:2:row:toggle");
        assertThat(StudioWorkspace.get().project(1).progress()).isEqualTo(50);
    }
    @Test void paymentPatchesCurrentProjectInsteadOfNavigatingToTheInvoiceRegister() {
        tester.startPage(StudioPage.class, StudioPage.route("project", 1)); var page = tester.getLastRenderedPage();
        tester.clickLink("content:invoices:1:inspect");
        tester.newFormTester("invoiceDialog:dialog:form"); tester.executeAjaxEvent("invoiceDialog:dialog:form:confirm", "click");
        assertThat(StudioWorkspace.get().invoice(2).paid()).isTrue();
        tester.assertComponentOnAjaxResponse("content:invoices:1:status");
        assertThat(tester.getLastRenderedPage()).isSameAs(page);
        assertThat(page.getPageParameters().get("view").toString()).isEqualTo("project");
        assertThat(tester.getLastResponseAsString()).doesNotContain("<redirect>", "<component id=\"" + page.get("content").getMarkupId() + "\"");
    }
    @Test void aTaskCallbackFromAnotherProjectContextCannotChangeTheCurrentRow() {
        tester.startPage(StudioPage.class, StudioPage.route("project", 1));
        tester.getLastRenderedPage().getPageParameters().set("project", 2);
        tester.clickLink("content:taskItems:tasks:2:row:toggle");
        assertThat(StudioWorkspace.get().project(1).progress()).isEqualTo(50);
        assertThat(tester.getLastResponseAsString()).contains("workspace changed before this action").doesNotContain("<redirect>");
    }
    @Test void anOldInvoiceDialogSubmissionCannotPayAnotherInvoice() {
        tester.startPage(StudioPage.class, StudioPage.route("project", 1)); tester.clickLink("content:invoices:1:inspect");
        var oldToken = ((org.apache.wicket.markup.html.form.HiddenField<?>) tester.getLastRenderedPage().get("invoiceDialog:dialog:form:content:context")).getModelObject().toString();
        tester.clickLink("content:back"); tester.clickLink("content:results:projects:2:open"); tester.clickLink("content:invoices:0:inspect");
        var form = tester.newFormTester("invoiceDialog:dialog:form"); form.setValue("content:context", oldToken);
        tester.executeAjaxEvent("invoiceDialog:dialog:form:confirm", "click");
        assertThat(StudioWorkspace.get().invoice(2).paid()).isFalse(); assertThat(StudioWorkspace.get().invoice(3).paid()).isFalse();
        assertThat(tester.getLastResponseAsString()).contains("workspace changed before this action");
    }
    @Test void missingProjectAndEmptyFiltersHaveUsefulRecovery() {
        tester.startPage(StudioPage.class, StudioPage.route("project", 999));
        assertThat(tester.getLastResponseAsString()).contains("isn’t in your workspace", "Back to projects");
        tester.startPage(StudioPage.class, StudioPage.route("projects", 0).set("q", "not-a-client"));
        assertThat(tester.getLastResponseAsString()).contains("No projects match", "0 projects");
    }
    @Test void separateVisitorsHaveSeparateDataAndResetGenerations() {
        tester.startPage(StudioPage.class); var first = StudioWorkspace.get(); first.pay(2);
        var firstGeneration = first.generation;
        tester.destroy(); tester = new WicketTester(new ExamplesApplication()); tester.startPage(StudioPage.class);
        assertThat(StudioWorkspace.get().generation).isNotEqualTo(firstGeneration);
        assertThat(StudioWorkspace.get().invoice(2).paid()).isFalse();
        assertThat(first.invoice(2).paid()).isTrue();
    }
}
