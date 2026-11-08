package dev.jbaby.shadwick.examples;

import org.apache.wicket.ajax.AbstractAjaxTimerBehavior;
import org.apache.wicket.extensions.ajax.markup.html.AjaxLazyLoadPanel;
import org.apache.wicket.util.tester.WicketTester;
import org.junit.jupiter.api.*;
import static org.assertj.core.api.Assertions.assertThat;

class LazyLoadPageTest {
    WicketTester tester;
    @BeforeEach void start(){tester=new WicketTester(new ExamplesApplication());}
    @AfterEach void stop(){tester.destroy();}
    @Test void reportsAreAbsentInitiallyAndOneTimerLoadsReadyAndFailureContent() {
        tester.startPage(LazyLoadPage.class);
        assertThat(tester.getComponentFromLastRenderedPage("revenue:content")).isNotInstanceOf(LazyLoadPage.ReportContent.class);
        assertThat(tester.getLastRenderedPage().getBehaviors(AbstractAjaxTimerBehavior.class)).hasSize(1);
        tester.executeAllTimerBehaviors(tester.getLastRenderedPage());
        assertThat(tester.getLastResponseAsString()).contains("Seats 12; projected total 1506.00.", "Report unavailable");
        assertThat(tester.getLastRenderedPage().getBehaviors(AbstractAjaxTimerBehavior.class)).hasSize(1);
    }
    @Test void loadedFormsValidateUpdateAndSubmitNormally() {
        tester.startPage(LazyLoadPage.class);tester.executeAllTimerBehaviors(tester.getLastRenderedPage());
        var form=tester.newFormTester("revenue:content:editor");form.setValue("seats:input","0");
        tester.executeAjaxEvent("revenue:content:editor:save","click");
        assertThat(tester.getLastResponseAsString()).contains("Shadwick.fieldFeedback");
        form=tester.newFormTester("revenue:content:editor");form.setValue("seats:input","20");
        tester.executeAjaxEvent("revenue:content:editor:save","click");tester.assertNoErrorMessage();
        assertThat(tester.getLastResponseAsString()).contains("Seats 20; projected total 2510.00.");
        form=tester.newFormTester("revenue:content:editor");form.setValue("seats:input","10");form.submit("normal");tester.assertNoErrorMessage();
        assertThat(tester.getLastResponseAsString()).contains("Seats 10; projected total 1255.00.");
    }
    @Test void retryAndReadinessUseFreshPanelsAndStopTheTimerAfterCompletion() {
        tester.startPage(LazyLoadPage.class);var page=tester.getLastRenderedPage();tester.executeAllTimerBehaviors(page);
        tester.clickLink("recoverable:content:error:retry");tester.executeAllTimerBehaviors(page);
        assertThat(tester.getLastResponseAsString()).contains("Recovered report").doesNotContain("Report unavailable");
        tester.clickLink("replace");tester.executeAllTimerBehaviors(page);
        tester.clickLink("ready");tester.executeAllTimerBehaviors(page);
        assertThat(tester.getLastResponseAsString()).contains("Queued report 2");
        assertThat(page.getBehaviors(AjaxLazyLoadPanel.AjaxLazyLoadTimer.class)).isEmpty();
        tester.clickLink("replace");assertThat(page.getBehaviors(AjaxLazyLoadPanel.AjaxLazyLoadTimer.class)).hasSize(1);
        tester.clickLink("ready");tester.executeAllTimerBehaviors(page);assertThat(tester.getLastResponseAsString()).contains("Queued report 3");
    }
    @Test void pendingAndLoadedDemoPagesKeepCallbacksAfterSerialization() {
        tester.startPage(LazyLoadPage.class);var serializer=tester.getApplication().getFrameworkSettings().getSerializer();
        var restored=(LazyLoadPage)serializer.deserialize(serializer.serialize(tester.getLastRenderedPage()));tester.startPage(restored);
        tester.executeAllTimerBehaviors(restored);tester.clickLink("recoverable:content:error:retry");tester.executeAllTimerBehaviors(restored);
        var loaded=(LazyLoadPage)serializer.deserialize(serializer.serialize(restored));tester.startPage(loaded);
        tester.clickLink("ready");tester.executeAllTimerBehaviors(loaded);
        assertThat(tester.getLastResponseAsString()).contains("Queued report 1");
    }
    @Test void nativeFallbackLoadsAReportAndPackagedSourcesIncludeContentAndRetryMarkup() {
        tester.startPage(LazyLoadPage.class);tester.clickLink("revenue:content:load");
        assertThat(tester.getLastResponseAsString()).contains("Seats 12; projected total 1506.00.");
        assertThat(ExampleSources.files(LazyLoadPage.class)).extracting(ExampleSources.File::name)
                .contains("LazyLoadPage.java","LazyLoadPage.html","LazyLoadPage$ReportContent.html","LazyLoadPage$RetryContent.html");
    }
}
