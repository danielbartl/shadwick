package dev.jbaby.shadwick.components.form;

import dev.jbaby.shadwick.Sw;
import dev.jbaby.shadwick.behaviors.SwitchBehavior;
import dev.jbaby.shadwick.components.SwFeedbackPanel;
import dev.jbaby.shadwick.components.SwSubmitButton;
import org.apache.wicket.MarkupContainer;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.form.AjaxFormChoiceComponentUpdatingBehavior;
import org.apache.wicket.ajax.form.AjaxFormComponentUpdatingBehavior;
import org.apache.wicket.feedback.DefaultCleanupFeedbackMessageFilter;
import org.apache.wicket.markup.IMarkupResourceStreamProvider;
import org.apache.wicket.markup.Markup;
import org.apache.wicket.markup.html.WebPage;
import org.apache.wicket.markup.html.form.CheckBox;
import org.apache.wicket.markup.html.form.ChoiceRenderer;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.model.CompoundPropertyModel;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.util.resource.IResourceStream;
import org.apache.wicket.util.resource.StringResourceStream;
import org.apache.wicket.util.tester.FormTester;
import org.apache.wicket.util.tester.TagTester;
import org.apache.wicket.util.tester.WicketTester;
import org.apache.wicket.validation.ValidationError;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/** Submission and rendered semantics, following Wicket Oat's choice-field and core-form tests. */
class ChoiceFieldsTest {

    enum Kind { CHECKBOX, SWITCH }

    private WicketTester tester;

    @BeforeEach
    void setUp() {
        tester = new WicketTester();
        tester.getApplication().getApplicationSettings().setFeedbackMessageCleanupFilter(new DefaultCleanupFeedbackMessageFilter());
    }

    @AfterEach
    void tearDown() {
        if (tester.getLastResponse() != null) {
            assertThat(html()).doesNotContain("style=").doesNotContainPattern("\\son[a-z]+=");
        }
        tester.destroy();
    }

    private String html() { return tester.getLastResponseAsString(); }

    private BaseSwField<Boolean, CheckBox, ?> bool(Kind kind, Model<Boolean> value) {
        return kind == Kind.CHECKBOX ? new SwCheckBox("choice", "Enabled", value) : new SwSwitch("choice", "Enabled", value);
    }

    private FormTester formWith(BaseSwField<?, ?, ?> field) {
        Form<Void> form = new Form<>("form");
        form.add(field);
        tester.startComponentInPage(form, Markup.of("<form wicket:id='form'><div wicket:id='choice'></div></form>"));
        return tester.newFormTester("form");
    }

    @ParameterizedTest
    @EnumSource(Kind.class)
    void booleansUseNativeInputsWithSeparateLabelsAndDescriptions(Kind kind) {
        var field = bool(kind, Model.of(true)).setDescription("You can change this later.");
        tester.startComponentInPage(field);

        TagTester input = tester.getTagByWicketId("input");
        assertThat(input.getName()).isEqualTo("input");
        assertThat(input.getAttribute("type")).isEqualTo("checkbox");
        assertThat(input.getAttribute("checked")).isEqualTo("checked");
        assertThat(input.getAttribute("role")).isEqualTo(kind == Kind.SWITCH ? "switch" : null);
        assertThat(input.getAttribute("aria-checked")).isNull();
        assertThat(tester.getTagByWicketId("label").getAttribute("for")).isEqualTo(input.getAttribute("id"));
        assertThat(input.getAttribute("aria-describedby")).isEqualTo(tester.getTagByWicketId("description").getAttribute("id")
                + " " + tester.getTagByWicketId("error").getAttribute("id"));
        assertThat(tester.getTagByWicketId("label").getValue()).doesNotContain("role=\"status\"");
    }

    @ParameterizedTest
    @EnumSource(Kind.class)
    void checkedAndUncheckedSubmissionsUpdateTheBooleanModel(Kind kind) {
        Model<Boolean> value = Model.of(false);
        var field = bool(kind, value);
        FormTester form = formWith(field);
        form.setValue("choice:input", true);
        form.submit();
        assertThat(value.getObject()).isTrue();

        FormTester unchecked = tester.newFormTester("form");
        unchecked.setValue("choice:input", false);
        unchecked.submit();
        assertThat(value.getObject()).isFalse();
    }

    @ParameterizedTest
    @EnumSource(Kind.class)
    void disabledControlsCannotChangeTheirModels(Kind kind) {
        Model<Boolean> value = Model.of(false);
        var field = bool(kind, value);
        field.setEnabled(false);
        FormTester form = formWith(field);
        assertThat(tester.getTagByWicketId("input").getAttribute("disabled")).isNotNull();
        form.setValue("choice:input", true);
        form.submit();
        assertThat(value.getObject()).isFalse();
    }

    @ParameterizedTest
    @EnumSource(Kind.class)
    void requiredUsesWicketsBooleanSemanticsRatherThanRequiringTrue(Kind kind) {
        Model<Boolean> value = Model.of(true);
        FormTester form = formWith(bool(kind, value).setRequired(true));
        form.setValue("choice:input", false);
        form.submit();
        tester.assertNoErrorMessage();
        assertThat(value.getObject()).isFalse();
    }

    @ParameterizedTest
    @EnumSource(Kind.class)
    void aConsentValidatorCanRequireACheckedValue(Kind kind) {
        var field = bool(kind, Model.of(false)).addValidator(value -> {
            if (!Boolean.TRUE.equals(value.getValue())) value.error(new ValidationError().setMessage("Please enable this."));
        });
        formWith(field).submit();
        assertThat(tester.getTagByWicketId("input").getAttribute("aria-invalid")).isEqualTo("true");
        assertThat(tester.getTagByWicketId("error").getValue()).isEqualTo("Please enable this.");

        FormTester form = tester.newFormTester("form");
        form.setValue("choice:input", true);
        form.submit();
        assertThat(tester.getTagByWicketId("error").getValue()).isEmpty();
    }

    @Test
    void switchSizeIsFluentAndTheBehaviorAlsoStylesPlainWicketCheckboxes() {
        SwSwitch toggle = new SwSwitch("choice", Model.of(true)).setDescription("Small switch").setSize(SwitchBehavior.Size.SMALL);
        tester.startComponentInPage(toggle);
        assertThat(tester.getTagByWicketId("input").getAttribute("data-size")).isEqualTo("sm");
        CheckBox plain = new CheckBox("plain", Model.of(true));
        plain.add(Sw.Behaviors.swSwitch());
        tester.startComponentInPage(plain, Markup.of("<input wicket:id='plain' type='checkbox' aria-label='Notifications'/>"));
        assertThat(tester.getTagByWicketId("plain").getAttribute("class")).isEqualTo("sw-switch");
        assertThat(tester.getTagByWicketId("plain").getAttribute("role")).isEqualTo("switch");
    }

    @Test
    void radioChoicesHaveANamedGroupAndNamedNativeOptions() {
        var field = new SwRadioChoice<>("choice", "Size", Model.of("Medium"), Model.ofList(List.of("Small", "Medium", "Large")))
                .setRequired(true).setDescription("Choose one size.");
        formWith(field);
        assertThat(tester.getTagByWicketId("container").getName()).isEqualTo("fieldset");
        TagTester legend = tester.getTagByWicketId("label");
        assertThat(legend.getName()).isEqualTo("legend");
        assertThat(legend.getAttribute("for")).isNull();
        TagTester group = tester.getTagByWicketId("input");
        assertThat(group.getAttribute("role")).isEqualTo("radiogroup");
        assertThat(group.getAttribute("aria-labelledby")).isEqualTo(legend.getAttribute("id"));
        assertThat(group.getAttribute("aria-required")).isEqualTo("true");
        assertThat(group.getValue()).doesNotContain("<br");
        List<TagTester> options = TagTester.createTagsByAttribute(html(), "type", "radio", false);
        assertThat(options).hasSize(3);
        assertThat(options).allSatisfy(option -> {
            assertThat(option.getAttribute("class")).isEqualTo("sw-radio-group-item");
            assertThat(option.getAttribute("aria-describedby")).isEqualTo(group.getAttribute("aria-describedby"));
            assertThat(option.getAttribute("name")).isEqualTo("choice:input");
        });
        assertThat(options.get(1).getAttribute("checked")).isNotNull();
    }

    @Test
    void inlineRadioChoicesUpdateTheSelectedModel() {
        Model<String> selected = Model.of();
        var field = new SwRadioChoice<>("choice", "Size", selected, Model.ofList(List.of("S", "M", "L"))).setInline(true);
        FormTester form = formWith(field);
        assertThat(tester.getTagByWicketId("input").getAttribute("data-orientation")).isEqualTo("horizontal");
        form.select("choice:input", 2);
        form.submit();
        assertThat(selected.getObject()).isEqualTo("L");
    }

    @Test
    void anUnselectedRequiredRadioGroupShowsItsErrorOnEveryOption() {
        var field = new SwRadioChoice<>("choice", "Size", new Model<String>(), Model.ofList(List.of("S", "M"))).setRequired(true);
        formWith(field).submit();
        assertThat(tester.getTagByWicketId("error").getValue()).contains("Size").contains("is required.");
        assertThat(TagTester.createTagsByAttribute(html(), "type", "radio", false))
                .allSatisfy(option -> assertThat(option.getAttribute("aria-invalid")).isEqualTo("true"));
    }

    public record Option(String id, String label) implements Serializable {}

    @Test
    void customRenderersUseStableIdsAndEscapeLabels() {
        Option first = new Option("a\"<&", "<First>");
        Option second = new Option("second", "<Second>");
        Model<Option> selected = Model.of();
        var field = new SwRadioChoice<>("choice", "Plan", selected, Model.ofList(List.of(first, second)),
                new ChoiceRenderer<>("label", "id"));
        FormTester form = formWith(field);
        assertThat(html()).contains("&lt;First&gt;").contains("&lt;Second&gt;").doesNotContain("<First>");
        assertThat(html()).contains("a&quot;&lt;&amp;");
        form.select("choice:input", 1);
        form.submit();
        assertThat(selected.getObject()).isEqualTo(second);
    }

    @Test
    void disabledRadioGroupsDisableEveryOptionAndKeepTheSelection() {
        Model<String> selected = Model.of("S");
        var field = new SwRadioChoice<>("choice", "Size", selected, Model.ofList(List.of("S", "M")));
        field.setEnabled(false);
        FormTester form = formWith(field);
        assertThat(TagTester.createTagsByAttribute(html(), "type", "radio", false))
                .allSatisfy(option -> assertThat(option.getAttribute("disabled")).isNotNull());
        form.select("choice:input", 1);
        form.submit();
        assertThat(selected.getObject()).isEqualTo("S");
    }

    @Test
    void choicesAreReactiveAndTheirModelDetaches() {
        boolean[] detached = {false};
        Model<ArrayList<String>> options = new Model<>(new ArrayList<>(List.of("S", "M"))) {
            @Override
            public void detach() { detached[0] = true; }
        };
        var field = new SwRadioChoice<>("choice", "Size", Model.of("S"), options);
        formWith(field);
        assertThat(detached[0]).isTrue();
        options.setObject(new ArrayList<>(List.of("S", "M", "L")));
        tester.startPage(tester.getLastRenderedPage());
        assertThat(TagTester.createTagsByAttribute(html(), "type", "radio", false)).hasSize(3);
    }

    public static class Preferences implements Serializable {
        public boolean accepted;
        public boolean notifications;
        public String contact;
    }

    public static class ChoicesPage extends WebPage implements IMarkupResourceStreamProvider {
        final Preferences preferences = new Preferences();
        final SwCheckBox accepted;
        final SwSwitch notifications;
        final SwRadioChoice<String> contact;
        int changes;

        public ChoicesPage() {
            Form<Preferences> form = new Form<>("form", new CompoundPropertyModel<>(preferences));
            add(form);
            form.add(accepted = new SwCheckBox("accepted"));
            accepted.setDescription("Accept before saving.").addValidator(value -> {
                if (!Boolean.TRUE.equals(value.getValue())) value.error(new ValidationError().setMessage("Accept the terms."));
            });
            form.add(notifications = new SwSwitch("notifications"));
            notifications.add(new AjaxFormComponentUpdatingBehavior("change") {
                @Override
                protected void onUpdate(AjaxRequestTarget target) { changes++; }
            });
            form.add(contact = new SwRadioChoice<String>("contact", Model.ofList(List.of("Email", "SMS"))).setRequired(true)
                    .setDescription("Pick one contact method."));
            contact.add(new AjaxFormChoiceComponentUpdatingBehavior() {
                @Override
                protected void onUpdate(AjaxRequestTarget target) { changes++; }
            });
            form.add(SwSubmitButton.onSubmit("save", "Save", target -> {}));
            form.add(new SwFeedbackPanel("feedback", form));
        }

        @Override
        public IResourceStream getMarkupResourceStream(MarkupContainer container, Class<?> containerClass) {
            return new StringResourceStream("<html><head></head><body><form wicket:id='form'>"
                    + "<div wicket:id='accepted'></div><div wicket:id='notifications'></div><div wicket:id='contact'></div>"
                    + "<button wicket:id='save'></button><div wicket:id='feedback'></div></form></body></html>");
        }
    }

    @Test
    void compoundPropertyModelsAndPropertyLabelsWorkForAllThreeFields() {
        ChoicesPage page = tester.startPage(new ChoicesPage());
        tester.assertLabel("form:accepted:label", "Accept terms");
        tester.assertLabel("form:notifications:label", "Send updates");
        tester.assertLabel("form:contact:label", "Contact preference");
        FormTester form = tester.newFormTester("form");
        form.setValue("accepted:input", true);
        form.setValue("notifications:input", true);
        form.select("contact:input", 1);
        form.submit("save");
        assertThat(page.preferences.accepted).isTrue();
        assertThat(page.preferences.notifications).isTrue();
        assertThat(page.preferences.contact).isEqualTo("SMS");
    }

    @Test
    void ajaxValidationChangesErrorsWithoutReplacingControlsOrRepeatingInlineFeedback() {
        ChoicesPage page = tester.startPage(new ChoicesPage());
        tester.executeAjaxEvent("form:save", "click");
        assertThat(html()).contains("Shadwick.fieldFeedback(").contains("Accept the terms.").contains("Contact preference");
        for (var field : List.of(page.accepted, page.contact)) {
            assertThat(html()).doesNotContain("<component id=\"" + field.getMarkupId() + "\"");
            assertThat(field.getInput().getFeedbackMessages().isEmpty()).isTrue();
        }
        assertThat(html().split("Accept the terms\\.", -1)).hasSize(2);

        FormTester form = tester.newFormTester("form");
        form.setValue("accepted:input", true);
        form.select("contact:input", 0);
        tester.executeAjaxEvent("form:save", "click");
        assertThat(html()).contains(",\"\",false)").doesNotContain("Accept the terms.");
        assertThat(page.preferences.contact).isEqualTo("Email");
    }

    @Test
    void switchAndRadioAjaxBehaviorsAreRoutedToTheInputsAndUpdateTheirModels() {
        ChoicesPage page = tester.startPage(new ChoicesPage());
        assertThat(page.notifications.getBehaviors(AjaxFormComponentUpdatingBehavior.class)).isEmpty();
        assertThat(page.contact.getInput().getBehaviors(AjaxFormChoiceComponentUpdatingBehavior.class)).hasSize(1);
        tester.newFormTester("form").setValue("notifications:input", true);
        tester.executeAjaxEvent("form:notifications:input", "change");
        assertThat(page.preferences.notifications).isTrue();
        tester.newFormTester("form").select("contact:input", 1);
        tester.executeAjaxEvent("form:contact:input", "change");
        assertThat(page.preferences.contact).isEqualTo("SMS");
        assertThat(page.changes).isEqualTo(2);
    }

    @Test
    void checkboxChangeValidationShowsAndClearsItsInlineErrorWithoutAFullSubmit() {
        ChoicesPage page = new ChoicesPage();
        page.accepted.add(new AjaxFormComponentUpdatingBehavior("change") {
            @Override
            protected void onUpdate(AjaxRequestTarget target) {}
        });
        tester.startPage(page);
        tester.newFormTester("form").setValue("accepted:input", false);
        tester.executeAjaxEvent("form:accepted:input", "change");
        assertThat(html()).contains("Shadwick.fieldFeedback(").contains("Accept the terms.")
                .doesNotContain("<component id=\"" + page.accepted.getMarkupId() + "\"");
        tester.newFormTester("form").setValue("accepted:input", true);
        tester.executeAjaxEvent("form:accepted:input", "change");
        assertThat(page.preferences.accepted).isTrue();
        assertThat(html()).contains(",\"\",false)").doesNotContain("Accept the terms.");
    }
}
