package dev.jbaby.shadwick.examples;

import org.apache.wicket.util.tester.WicketTester;
import org.junit.jupiter.api.*;
import static org.assertj.core.api.Assertions.assertThat;

class ButtonGroupPageTest {
    WicketTester tester;
    @BeforeEach void start() { tester = new WicketTester(new ExamplesApplication()); }
    @AfterEach void stop() { tester.destroy(); }
    @Test void splitActionsLeaveRequiredFormUnsubmitted() {
        tester.startPage(ButtonGroupPage.class);
        tester.executeAjaxEvent("form:commands:primary", "click"); tester.assertNoErrorMessage();
        assertThat(tester.getLastResponseAsString()).contains("Preview opened (action 1)");
        tester.executeAjaxEvent("form:commands:menu:items:0", "click"); tester.assertNoErrorMessage();
        assertThat(tester.getLastResponseAsString()).contains("Draft preview opened (action 2)");
    }
    @Test void groupedSubmitButtonsValidateAndSaveNormallyAndWithAjax() {
        tester.startPage(ButtonGroupPage.class);
        tester.executeAjaxEvent("form:formActions:ajaxSave", "click");
        assertThat(tester.getLastResponseAsString()).contains("Shadwick.fieldFeedback", "is required.");
        var input = tester.newFormTester("form"); input.setValue("title:input", "Website");
        tester.executeAjaxEvent("form:formActions:ajaxSave", "click"); tester.assertNoErrorMessage();
        assertThat(tester.getLastResponseAsString()).contains("Saved Website (submission 1)");
        input = tester.newFormTester("form"); input.setValue("title:input", "Store"); input.submit("formActions:nativeSave");
        tester.assertNoErrorMessage(); assertThat(tester.getLastResponseAsString()).contains("Saved Store (submission 2)");
    }
    @Test void serializedDemoKeepsCallbacksAndSupportsDynamicReplacement() {
        tester.startPage(ButtonGroupPage.class); var serializer = tester.getApplication().getFrameworkSettings().getSerializer();
        var page = (ButtonGroupPage) serializer.deserialize(serializer.serialize(tester.getLastRenderedPage()));
        tester.startPage(page); tester.clickLink("refresh");
        assertThat(tester.getLastResponseAsString()).contains("Save revision 2");
        tester.executeAjaxEvent("split:menu:items:0", "click");
        assertThat(tester.getLastResponseAsString()).contains("Draft saved (action 1)");
        tester.clickLink("toggle"); assertThat(tester.getLastResponseAsString()).doesNotContain(">Save as draft<");
        tester.clickLink("toggle"); tester.executeAjaxEvent("split:primary", "click");
        assertThat(tester.getLastResponseAsString()).contains("Saved revision 2 (action 2)");
    }
}
