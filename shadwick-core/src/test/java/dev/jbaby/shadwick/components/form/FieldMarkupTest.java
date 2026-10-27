package dev.jbaby.shadwick.components.form;

import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.markup.Markup;
import org.apache.wicket.model.Model;
import org.apache.wicket.util.tester.FormTester;
import org.apache.wicket.util.tester.TagTester;
import org.apache.wicket.util.tester.WicketTester;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/** shadcn's field markup, and the ARIA attributes that tie its parts together. */
class FieldMarkupTest {

    private WicketTester tester;

    @BeforeEach
    void setUp() {
        tester = new WicketTester();
    }

    @AfterEach
    void tearDown() {
        tester.destroy();
    }

    private String html() {
        return tester.getLastResponseAsString();
    }

    @Test
    void aTextFieldIsShadcnsField() {
        SwTextField<String> field = new SwTextField<>("email", "Email", Model.of(""));
        tester.startComponentInPage(field);

        TagTester container = TagTester.createTagByAttribute(html(), "class", "sw-field");
        assertThat(container.getAttribute("data-invalid")).isNull();
        TagTester label = TagTester.createTagByAttribute(html(), "class", "sw-label sw-field-label");
        assertThat(label.getName()).isEqualTo("label");
        assertThat(label.getAttribute("for")).isEqualTo(field.getInput().getMarkupId());
        TagTester input = tester.getTagByWicketId("input");
        assertThat(input.getAttribute("class")).isEqualTo("sw-input");
        assertThat(input.getAttribute("type")).isEqualTo("text");
        assertThat(input.getAttribute("aria-invalid")).isEqualTo("false");
        assertThat(input.getAttribute("required")).isNull();
    }

    @Test
    void aTextAreaIsShadcnsTextarea() {
        tester.startComponentInPage(new SwTextArea<>("bio", "Bio", Model.of("")));

        TagTester input = tester.getTagByWicketId("input");
        assertThat(input.getName()).isEqualTo("textarea");
        assertThat(input.getAttribute("class")).isEqualTo("sw-textarea");
    }

    @Test
    void theInputIsDescribedByTheErrorAndTheDescriptionIfThereIsOne() {
        SwTextField<String> field = new SwTextField<>("email", "Email", Model.of(""));
        tester.startComponentInPage(field);
        TagTester error = tester.getTagByWicketId("error");
        assertThat(error.getAttribute("role")).isEqualTo("status");
        assertThat(error.getValue()).isEmpty();
        assertThat(tester.getTagByWicketId("input").getAttribute("aria-describedby")).isEqualTo(error.getAttribute("id"));
        assertThat(tester.getTagByWicketId("description")).isNull();

        field.setDescription("We never share it.");
        tester.startComponentInPage(field);
        TagTester description = tester.getTagByWicketId("description");
        assertThat(description.getName()).isEqualTo("p");
        assertThat(description.getAttribute("class")).isEqualTo("sw-field-description");
        assertThat(description.getValue()).isEqualTo("We never share it.");
        assertThat(tester.getTagByWicketId("input").getAttribute("aria-describedby"))
                .isEqualTo(description.getAttribute("id") + " " + tester.getTagByWicketId("error").getAttribute("id"));
    }

    @Test
    void anInvalidFieldShowsItsErrorBesideTheDescription() {
        Form<Void> form = new Form<>("form");
        form.add(new SwTextField<>("email", "Email", Model.of("")).setRequired(true).setDescription("We never share it."));
        tester.startComponentInPage(form, Markup.of("<form wicket:id='form'><div wicket:id='email'></div></form>"));

        FormTester formTester = tester.newFormTester("form");
        formTester.setValue("email:input", "");
        formTester.submit();

        assertThat(TagTester.createTagByAttribute(html(), "class", "sw-field").getAttribute("data-invalid")).isEqualTo("true");
        TagTester input = tester.getTagByWicketId("input");
        assertThat(input.getAttribute("aria-invalid")).isEqualTo("true");
        assertThat(input.getAttribute("aria-required")).isEqualTo("true");
        assertThat(tester.getTagByWicketId("error").getValue()).isEqualTo("&#039;Email&#039; is required.");
        assertThat(tester.getTagByWicketId("description").getValue()).isEqualTo("We never share it.");
    }

    @Test
    void aFieldAddsTheStylesheetAndScriptAndRendersNoInlineStylesOrHandlers() {
        tester.startPage(FieldTestPage.class);

        assertThat(html()).containsPattern("css/shadwick-vega[^\"]*\\.css").containsPattern("js/shadwick[^\"]*\\.js")
                .doesNotContain("style=").doesNotContainPattern("\\son[a-z]+=");
    }
}
