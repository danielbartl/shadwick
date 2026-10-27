package dev.jbaby.shadwick.components.form;

import org.apache.wicket.AttributeModifier;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.form.AjaxFormComponentUpdatingBehavior;
import org.apache.wicket.model.Model;
import org.apache.wicket.util.tester.FormTester;
import org.apache.wicket.util.tester.TagTester;
import org.apache.wicket.util.tester.WicketTester;
import org.apache.wicket.validation.validator.StringValidator;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Shadwick form fields used the way Wicket developers use plain form components: inherited models, labels from
 * properties, Ajax re-rendering, add(validator).
 */
class WicketIdiomsFieldTest {

    private WicketTester tester;

    @BeforeEach
    void setUp() {
        tester = new WicketTester();
    }

    @AfterEach
    void tearDown() {
        tester.destroy();
    }

    @Test
    void labelsAreLookedUpByIdAndFallBackToTheId() {
        tester.startPage(FieldTestPage.class);

        tester.assertLabel("form:name:label", "Full name");
        tester.assertLabel("form:age:label", "Age");
        tester.assertLabel("form:nickname:label", "nickname");
    }

    @Test
    void validationMessagesUseTheLookedUpLabel() {
        tester.startPage(FieldTestPage.class);

        FormTester formTester = tester.newFormTester("form");
        formTester.setValue("name:input", "");
        formTester.submit();

        tester.assertErrorMessages("'Full name' is required.");
        // ... and is shown inline (HTML-escaped) in the field's error
        assertThat(errorOf("name").getValue()).isEqualTo("&#039;Full name&#039; is required.");
    }

    @Test
    void modelsAreInheritedFromTheCompoundPropertyModelById() {
        FieldTestPage page = new FieldTestPage();
        page.person.name = "Ada";
        page.person.age = 36;
        page.person.bio = "Countess";
        tester.startPage(page);

        assertThat(inputOf("name").getAttribute("value")).isEqualTo("Ada");
        assertThat(inputOf("age").getAttribute("value")).isEqualTo("36");
        assertThat(inputOf("bio").getValue()).isEqualTo("Countess");

        FormTester formTester = tester.newFormTester("form");
        formTester.setValue("name:input", "Grace");
        formTester.setValue("age:input", "42");
        formTester.setValue("bio:input", "Admiral");
        formTester.submit();

        tester.assertNoErrorMessage();
        assertThat(page.person.name).isEqualTo("Grace");
        // Converted to the bean property's type, which the inherited model exposes
        assertThat(page.person.age).isEqualTo(42);
        assertThat(page.person.bio).isEqualTo("Admiral");
    }

    @Test
    void fieldsCanBeReRenderedAfterAnAjaxSubmit() {
        tester.startPage(FieldTestPage.class);

        FormTester formTester = tester.newFormTester("form");
        formTester.setValue("name:input", "");
        tester.executeAjaxEvent("form:submit", "click");

        tester.assertComponentOnAjaxResponse("form:name");
        String response = tester.getLastResponseAsString();
        assertThat(response).contains("data-invalid=\"true\"").contains("&#039;Full name&#039; is required.");
    }

    @Test
    void thePanelRendersItsOwnTagWithAnIdAndKeepsTheMarkupAttributes() {
        tester.startPage(FieldTestPage.class);

        TagTester tag = tester.getTagByWicketId("name");
        assertThat(tag.getAttribute("id")).isNotBlank();
        assertThat(tag.getAttribute("class")).isEqualTo("span-2");
    }

    @Test
    void validatorsAndAjaxUpdatingBehaviorsAreAddedToTheInnerField() {
        Model<String> model = Model.of("");
        SwTextField<String> field = new SwTextField<>("id", "Label", model);
        AjaxFormComponentUpdatingBehavior onChange = new AjaxFormComponentUpdatingBehavior("change") {
            @Override
            protected void onUpdate(AjaxRequestTarget target) {
            }
        };
        field.add(StringValidator.maximumLength(3), onChange, AttributeModifier.append("class", "wide"));

        assertThat(field.getInput().getValidators()).hasSize(1);
        assertThat(field.getInput().getBehaviors(AjaxFormComponentUpdatingBehavior.class)).containsExactly(onChange);
        assertThat(field.getBehaviors()).hasSize(1); // only the AttributeModifier

        tester.startComponentInPage(field);
        assertThat(tester.getTagByWicketId("id").getAttribute("class")).isEqualTo("wide");
    }

    @Test
    void addToInputPutsBehaviorsOnTheInputItself() {
        SwTextField<String> field = new SwTextField<String>("id", "Email", Model.of(""))
                .addToInput(AttributeModifier.replace("autocomplete", "email"))
                .setPlaceholder("you@example.com");
        tester.startComponentInPage(field);

        TagTester input = tester.getTagByWicketId("input");
        assertThat(input.getAttribute("autocomplete")).isEqualTo("email");
        assertThat(input.getAttribute("placeholder")).isEqualTo("you@example.com");
        assertThat(tester.getTagByWicketId("id").getAttribute("autocomplete")).isNull();
    }

    @Test
    void theInputIsAChildOfTheFieldWithAShortPath() {
        SwTextField<String> field = new SwTextField<>("email", "Email", Model.of(""));
        tester.startComponentInPage(field);

        assertThat(field.get(BaseSwField.INPUT_ID)).isSameAs(field.getInput());
        assertThat(field.getInput().getInputName()).isEqualTo("email:input");
    }

    @Test
    void fluentSettersReturnTheConcreteFieldType() {
        // These declarations only compile if the setters keep the concrete type
        SwTextField<String> text = new SwTextField<>("t", Model.of("")).setPlaceholder(Model.of("Type here")).setLabel(Model.of("Text"));
        SwTextArea<String> area = new SwTextArea<>("a", Model.of("")).setRequired(true).setDescription("Help");

        assertThat(text.getInput().getLabel().getObject()).isEqualTo("Text");
        assertThat(area.getInput().isRequired()).isTrue();
    }

    @Test
    void setLabelUpdatesTheVisibleLabelAndTheValidationLabel() {
        SwTextField<String> field = new SwTextField<String>("id", Model.of("")).setLabel(Model.of("Email"));
        tester.startComponentInPage(field);

        tester.assertLabel("id:label", "Email");
        assertThat(field.getInput().getLabel().getObject()).isEqualTo("Email");
    }

    private TagTester fieldOf(String id) {
        return TagTester.createTagByAttribute(tester.getLastResponseAsString(), "id", ((BaseSwField<?, ?, ?>) tester.getComponentFromLastRenderedPage("form:" + id)).getMarkupId());
    }

    private TagTester inputOf(String id) {
        return TagTester.createTagByAttribute(fieldOf(id).getValue(), "name", id + ":input");
    }

    private TagTester errorOf(String id) {
        return TagTester.createTagByAttribute(fieldOf(id).getValue(), "class", "sw-field-error");
    }
}
