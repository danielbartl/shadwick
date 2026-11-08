package dev.jbaby.shadwick.examples;

import org.apache.wicket.util.tester.WicketTester;
import org.junit.jupiter.api.*;
import java.util.Locale;
import static org.assertj.core.api.Assertions.assertThat;

class RecordDisplayPageTest {
    WicketTester tester;
    @BeforeEach void start() { tester = new WicketTester(new ExamplesApplication()); tester.getSession().setLocale(Locale.US); }
    @AfterEach void stop() { tester.destroy(); }
    @Test void normalFormUpdatesBothDetailsAndHistoryAndRejectsEmptyNames() {
        tester.startPage(RecordDisplayPage.class);
        var form = tester.newFormTester("form"); form.setValue("name:input", "New customer"); form.submit("normalSave");
        tester.assertNoErrorMessage();
        assertThat(tester.getLastResponseAsString()).contains("New customer", "Customer updated", "Name changed to New customer");
        form = tester.newFormTester("form"); form.setValue("name:input", ""); form.submit("normalSave");
        assertThat(tester.getMessages(org.apache.wicket.feedback.FeedbackMessage.ERROR)).isNotEmpty();
    }
    @Test void ajaxPaymentUpdatesOnlyTheRecordViewsAndTheActivity() {
        tester.startPage(RecordDisplayPage.class); tester.clickLink("payment");
        tester.assertComponentOnAjaxResponse("details"); tester.assertComponentOnAjaxResponse("compact");
        tester.assertComponentOnAjaxResponse("activity"); tester.assertComponentOnAjaxResponse("receipt");
        assertThat(tester.getLastResponseAsString()).contains("48,350.1254", "An additional 100", "Recorded payment 100.");
        tester.clickLink("clear"); assertThat(tester.getLastResponseAsString()).contains("No activity yet").doesNotContain("Customer created");
        tester.clickLink("restore"); assertThat(tester.getLastResponseAsString()).contains("Customer created", "Delivery failed");
    }
    @Test void pageSerializationKeepsCustomValuesPropertiesAndCallbacksUsable() {
        tester.startPage(RecordDisplayPage.class); var serializer = tester.getApplication().getFrameworkSettings().getSerializer();
        var restored = (RecordDisplayPage)serializer.deserialize(serializer.serialize(tester.getLastRenderedPage()));
        tester.startPage(restored); tester.clickLink("payment");
        var form = tester.newFormTester("form"); form.setValue("name:input", "Restored customer");
        tester.executeAjaxEvent("form:save", "click"); tester.assertNoErrorMessage();
        assertThat(tester.getLastResponseAsString()).contains("Restored customer", "Customer updated");
        tester.startPage(restored); assertThat(tester.getLastResponseAsString()).contains("mailto:jordan@example.com");
    }
    @Test void packagedExampleSourcesIncludeTheCustomValueMarkup() {
        assertThat(ExampleSources.files(RecordDisplayPage.class)).extracting(ExampleSources.File::name)
                .contains("RecordDisplayPage.java", "RecordDisplayPage.html", "RecordDisplayPage$ContactLink.html");
    }
}
