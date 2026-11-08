package dev.jbaby.shadwick.examples;

import dev.jbaby.shadwick.components.form.SwTagInput;
import org.apache.wicket.util.tester.WicketTester;
import org.junit.jupiter.api.*;
import static org.assertj.core.api.Assertions.assertThat;

class TagInputPageTest {
    WicketTester tester;
    @BeforeEach void start(){tester=new WicketTester(new ExamplesApplication());}
    @AfterEach void stop(){tester.destroy();}
    @Test void nativeFormsConvertDeduplicateAndValidateLists(){
        tester.startPage(TagInputPage.class);
        var form=tester.newFormTester("nativeForm");form.setValue("fields:tags:input",", ,");form.submit("save");
        var field=(SwTagInput)tester.getComponentFromLastRenderedPage("nativeForm:fields:tags");
        assertThat(field.getModelObject()).containsExactly("wicket","java");
        assertThat(field.getInput().isValid()).isFalse();
        form=tester.newFormTester("nativeForm");form.setValue("fields:tags:input","a, b, a, c");form.submit("save");tester.assertNoErrorMessage();
        assertThat(field.getModelObject()).containsExactly("a","b","c");assertThat(tester.getLastResponseAsString()).contains("Saved tags: a | b | c");
    }
    @Test void ajaxValidationUsesInPlaceFeedbackAndOnlyUpdatesValidModels(){
        tester.startPage(TagInputPage.class);var form=tester.newFormTester("ajaxForm");form.setValue("fields:tags:input","a,b,c,d");
        tester.executeAjaxEvent("ajaxForm:save","click");assertThat(tester.getLastResponseAsString()).contains("Shadwick.fieldFeedback", "Choose at most three tags.");
        form=tester.newFormTester("ajaxForm");form.setValue("fields:tags:input","a,b,c");tester.executeAjaxEvent("ajaxForm:save","click");tester.assertNoErrorMessage();
        assertThat(tester.getLastResponseAsString()).contains("Saved tags: a | b | c");
    }
    @Test void commandsReplaceFieldsAndDisableTheirWicketHierarchy(){
        tester.startPage(TagInputPage.class);var old=tester.getComponentFromLastRenderedPage("ajaxForm:fields:tags");
        tester.executeAjaxEvent("ajaxForm:replace","click");assertThat(tester.getComponentFromLastRenderedPage("ajaxForm:fields:tags")).isNotSameAs(old);
        tester.executeAjaxEvent("ajaxForm:preset","click");var field=(SwTagInput)tester.getComponentFromLastRenderedPage("ajaxForm:fields:tags");
        assertThat(field.getModelObject()).containsExactly("release","ready");
        tester.executeAjaxEvent("ajaxForm:toggle","click");assertThat(field.getInput().isEnabledInHierarchy()).isFalse();
        tester.executeAjaxEvent("ajaxForm:toggle","click");assertThat(field.getInput().isEnabledInHierarchy()).isTrue();
    }
    @Test void renderedPagesRetainCallbacksAndConvertersAfterSerialization(){
        tester.startPage(TagInputPage.class);var serializer=tester.getApplication().getFrameworkSettings().getSerializer();
        var page=(TagInputPage)serializer.deserialize(serializer.serialize(tester.getLastRenderedPage()));tester.startPage(page);
        tester.executeAjaxEvent("ajaxForm:preset","click");var form=tester.newFormTester("ajaxForm");form.setValue("fields:tags:input","serialized, tags");
        tester.executeAjaxEvent("ajaxForm:save","click");tester.assertNoErrorMessage();assertThat(tester.getLastResponseAsString()).contains("Saved tags: serialized | tags");
    }
    @Test void catalogSourcesAndResourcesExposeTheImplementation(){
        tester.startPage(TagInputPage.class);assertThat(tester.getLastResponseAsString()).contains("/js/tag-input-");
        assertThat(ExampleSources.files(TagInputPage.class)).extracting(ExampleSources.File::name).contains("TagInputPage.java","TagInputPage.html");
        assertThat(CatalogPages.find(TagInputPage.class).route()).isEqualTo("tag-input");
    }
}
