package dev.jbaby.shadwick.components.form;

import dev.jbaby.shadwick.behaviors.ToggleBehavior;
import dev.jbaby.shadwick.behaviors.ToggleGroupBehavior;
import org.apache.wicket.markup.Markup;
import org.apache.wicket.markup.html.form.ChoiceRenderer;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.model.Model;
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
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;

class ToggleFieldsTest {
    enum Kind { SINGLE, MULTIPLE }
    private WicketTester tester;

    @BeforeEach void setUp() { tester = new WicketTester(); }
    @AfterEach void tearDown() {
        if (tester.getLastResponse() != null) {
            assertThat(html()).doesNotContain("style=").doesNotContainPattern("\\son[a-z]+=");
        }
        tester.destroy();
    }
    private String html() { return tester.getLastResponseAsString(); }
    private FormTester form(BaseSwField<?, ?, ?> field) {
        Form<Void> form = new Form<>("form");
        form.add(field);
        tester.startComponentInPage(form, Markup.of("<form wicket:id='form'><div wicket:id='choice'></div></form>"));
        return tester.newFormTester("form");
    }
    private BaseSwToggleGroup<?, ?, ?> group(Kind kind) {
        var choices = Model.ofList(List.of("One", "Two", "Three"));
        return kind == Kind.SINGLE
                ? new SwToggleChoice<>("choice", "Options", new Model<String>(), choices)
                : new SwToggleMultipleChoice<>("choice", "Options", Model.ofList(new ArrayList<String>()), choices);
    }

    @Test
    void aToggleIsALabelledNativeCheckboxWithIconAndHelp() {
        var field = new SwToggle("choice", "Bold", Model.of(true)).setIcon("pencil").setIconOnly(true)
                .setVariant(ToggleBehavior.Variant.OUTLINE).setSize(ToggleBehavior.Size.SMALL).setDescription("Emphasize text.");
        form(field);
        var input = tester.getTagByWicketId("input");
        assertThat(input.getAttribute("type")).isEqualTo("checkbox");
        assertThat(input.getAttribute("class")).isEqualTo("sw-toggle-input");
        assertThat(input.getAttribute("checked")).isNotNull();
        assertThat(input.getAttribute("aria-pressed")).isNull();
        var toggle = tester.getTagByWicketId("toggle");
        assertThat(toggle.getName()).isEqualTo("label");
        assertThat(toggle.getAttribute("class")).isEqualTo("sw-toggle");
        assertThat(toggle.getAttribute("data-variant")).isEqualTo("outline");
        assertThat(toggle.getAttribute("data-size")).isEqualTo("sm");
        assertThat(tester.getTagByWicketId("label").getValue()).isEqualTo("Bold");
        assertThat(tester.getTagByWicketId("label").getAttribute("class")).isEqualTo("sw-toggle-caption-hidden");
        assertThat(input.getAttribute("aria-describedby")).contains(tester.getTagByWicketId("description").getAttribute("id"));
    }

    @Test
    void toggleSubmissionsAndBooleanValidationFollowWicket() {
        Model<Boolean> value = Model.of(true);
        var toggle = new SwToggle("choice", "Bold", value).setRequired(true);
        FormTester form = form(toggle);
        form.setValue("choice:input", false);
        form.submit();
        tester.assertNoErrorMessage();
        assertThat(value.getObject()).isFalse();
        toggle.addValidator(validatable -> {
            if (!Boolean.TRUE.equals(validatable.getValue())) validatable.error(new ValidationError().setMessage("Turn bold on."));
        });
        form = tester.newFormTester("form");
        form.submit();
        assertThat(tester.getTagByWicketId("input").getAttribute("aria-invalid")).isEqualTo("true");
        form = tester.newFormTester("form");
        form.setValue("choice:input", true);
        form.submit();
        tester.assertNoErrorMessage();
        assertThat(value.getObject()).isTrue();
    }

    @ParameterizedTest
    @EnumSource(Kind.class)
    void toggleGroupsKeepNativeLabelsAndGroupSemantics(Kind kind) {
        var group = group(kind).setVariant(ToggleBehavior.Variant.OUTLINE).setSize(ToggleBehavior.Size.LARGE)
                .setSpacing(0).setOrientation(ToggleGroupBehavior.Orientation.VERTICAL).setDescription("Pick an option.");
        form(group);
        var input = tester.getTagByWicketId("input");
        assertThat(tester.getTagByWicketId("container").getName()).isEqualTo("fieldset");
        assertThat(input.getAttribute("role")).isEqualTo(kind == Kind.SINGLE ? "radiogroup" : "group");
        assertThat(input.getAttribute("aria-labelledby")).isEqualTo(tester.getTagByWicketId("label").getAttribute("id"));
        assertThat(input.getAttribute("data-spacing")).isEqualTo("0");
        assertThat(input.getAttribute("data-orientation")).isEqualTo("vertical");
        assertThat(TagTester.createTagsByAttribute(html(), "class", "sw-toggle-input", false)).hasSize(3).allSatisfy(option -> {
            assertThat(option.getAttribute("type")).isEqualTo(kind == Kind.SINGLE ? "radio" : "checkbox");
            assertThat(option.getAttribute("aria-describedby")).isEqualTo(input.getAttribute("aria-describedby"));
        });
        assertThat(TagTester.createTagsByAttribute(html(), "class", "sw-toggle sw-toggle-group-item", false))
                .hasSize(3).allSatisfy(label -> {
                    assertThat(label.getName()).isEqualTo("label");
                    assertThat(label.getAttribute("data-variant")).isEqualTo("outline");
                    assertThat(label.getAttribute("data-size")).isEqualTo("lg");
                });
    }

    @ParameterizedTest
    @EnumSource(Kind.class)
    void requiredGroupsRejectEmptyAndForgedIdsThenUpdateModels(Kind kind) {
        var group = group(kind).setRequired(true);
        FormTester form = form(group);
        form.submit();
        assertThat(tester.getTagByWicketId("error").getValue()).contains("required");
        form = tester.newFormTester("form");
        form.setValue("choice:input", "forged");
        form.submit();
        assertThat(tester.getTagByWicketId("error").getValue()).isNotEmpty();
        form = tester.newFormTester("form");
        if (kind == Kind.SINGLE) form.select("choice:input", 1);
        else form.selectMultiple("choice:input", new int[]{0, 2}, true);
        form.submit();
        tester.assertNoErrorMessage();
        assertThat(group.getModelObject()).isEqualTo(kind == Kind.SINGLE ? "Two" : List.of("One", "Three"));
        assertThat(tester.getTagByWicketId("input").getAttribute("aria-required"))
                .isEqualTo(kind == Kind.SINGLE ? "true" : null);
    }

    @ParameterizedTest
    @EnumSource(Kind.class)
    void disabledGroupsDisableEveryOptionAndIgnoreSubmittedChanges(Kind kind) {
        var group = group(kind);
        group.setEnabled(false);
        FormTester form = form(group);
        form.setValue("choice:input", "1");
        form.submit();
        assertThat(group.getModelObject()).isEqualTo(kind == Kind.SINGLE ? null : List.of());
        assertThat(TagTester.createTagsByAttribute(html(), "class", "sw-toggle-input", false))
                .allSatisfy(option -> assertThat(option.getAttribute("disabled")).isNotNull());
    }

    public record Option(String id, String label) implements Serializable {}

    @ParameterizedTest
    @EnumSource(Kind.class)
    void customRenderersEscapeLabelsAndResolveStableIds(Kind kind) {
        Option first = new Option("first\"<&", "<First>");
        Option second = new Option("second", "<Second>");
        var choices = Model.ofList(List.of(first, second));
        var renderer = new ChoiceRenderer<Option>("label", "id");
        BaseSwToggleGroup<?, ?, ?> group = kind == Kind.SINGLE
                ? new SwToggleChoice<>("choice", "Options", new Model<Option>(), choices, renderer)
                : new SwToggleMultipleChoice<>("choice", "Options", Model.ofList(new ArrayList<Option>()), choices, renderer);
        FormTester form = form(group);
        assertThat(html()).contains("&lt;First&gt;", "first&quot;&lt;&amp;").doesNotContain("<First>");
        if (kind == Kind.SINGLE) form.select("choice:input", 1);
        else form.selectMultiple("choice:input", new int[]{1}, true);
        form.submit();
        assertThat(group.getModelObject()).isEqualTo(kind == Kind.SINGLE ? second : List.of(second));
    }

    @Test
    void optionalMultipleGroupsClearTheExistingCollection() {
        List<String> selected = new ArrayList<>(List.of("One"));
        var group = new SwToggleMultipleChoice<>("choice", "Options", Model.ofList(selected), Model.ofList(List.of("One", "Two")));
        FormTester form = form(group);
        form.selectMultiple("choice:input", new int[0], true);
        form.submit();
        assertThat(selected).isEmpty();
        assertThat(group.getModelObject()).isSameAs(selected);
    }

    @Test
    void invalidSpacingIsRejected() {
        assertThatIllegalArgumentException().isThrownBy(() -> new SwToggleChoice<String>("choice", Model.ofList(List.of()))
                .setSpacing(5));
        assertThatIllegalArgumentException().isThrownBy(() -> new ToggleGroupBehavior().setSpacing(-1));
    }
}
