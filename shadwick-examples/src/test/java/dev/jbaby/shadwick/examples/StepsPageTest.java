package dev.jbaby.shadwick.examples;

import org.apache.wicket.util.tester.WicketTester;
import org.junit.jupiter.api.*;
import static org.assertj.core.api.Assertions.assertThat;

class StepsPageTest {
    WicketTester tester;
    @BeforeEach void start(){tester=new WicketTester(new ExamplesApplication());}
    @AfterEach void stop(){tester.destroy();}
    @Test void nativeNextValidatesAndThenAdvancesTheIndicator(){
        tester.startPage(StepsPage.class);var form=tester.newFormTester("form");form.setValue("name:input","");form.submit("normal");
        tester.assertLabel("form:steps:list:items:1:link:status","Current");
        form=tester.newFormTester("form");form.setValue("name:input","New project");form.submit("normal");tester.assertNoErrorMessage();
        tester.assertLabel("form:steps:list:items:2:link:status","Current");
    }
    @Test void ajaxNextAndBackKeepNavigationAndValidationApplicationOwned(){
        tester.startPage(StepsPage.class);var form=tester.newFormTester("form");form.setValue("name:input","");
        tester.executeAjaxEvent("form:next","click");assertThat(tester.getLastResponseAsString()).contains("Shadwick.fieldFeedback");
        tester.executeAjaxEvent("form:steps:list:items:0:link","click");
        assertThat(tester.getLastResponseAsString()).contains("Returned to Details.");tester.assertLabel("form:steps:list:items:0:link:status","Current");
        form=tester.newFormTester("form");form.setValue("name:input","New project");tester.executeAjaxEvent("form:next","click");tester.assertNoErrorMessage();
        tester.assertLabel("form:steps:list:items:1:link:status","Current");
    }
    @Test void commandsCompleteResetAndReplaceOnlyTheIndicator(){
        tester.startPage(StepsPage.class);var old=tester.getComponentFromLastRenderedPage("form:steps");
        tester.executeAjaxEvent("form:finish","click");tester.assertLabel("form:steps:list:items:2:link:status","Completed");
        tester.executeAjaxEvent("form:replace","click");assertThat(tester.getComponentFromLastRenderedPage("form:steps")).isNotSameAs(old);
        tester.executeAjaxEvent("form:reset","click");tester.assertLabel("form:steps:list:items:0:link:status","Current");
    }
    @Test void renderedPageKeepsCallbacksAndModelsAfterSerialization(){
        tester.startPage(StepsPage.class);var serializer=tester.getApplication().getFrameworkSettings().getSerializer();
        var restored=(StepsPage)serializer.deserialize(serializer.serialize(tester.getLastRenderedPage()));tester.startPage(restored);
        tester.executeAjaxEvent("form:finish","click");tester.executeAjaxEvent("form:steps:list:items:1:link","click");
        tester.assertLabel("form:steps:list:items:1:link:status","Current");tester.executeAjaxEvent("form:replace","click");
    }
    @Test void catalogProvidesTheGuideAndPackagedExampleSources(){
        assertThat(CatalogPages.find(StepsPage.class).route()).isEqualTo("steps");
        assertThat(ExampleSources.files(StepsPage.class)).extracting(ExampleSources.File::name).contains("StepsPage.java","StepsPage.html");
    }
}
