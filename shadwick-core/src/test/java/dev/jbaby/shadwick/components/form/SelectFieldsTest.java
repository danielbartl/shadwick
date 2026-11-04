package dev.jbaby.shadwick.components.form;

import org.apache.wicket.markup.Markup;
import org.apache.wicket.markup.html.form.ChoiceRenderer;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.model.CompoundPropertyModel;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.util.tester.FormTester;
import org.apache.wicket.util.tester.TagTester;
import org.apache.wicket.util.tester.WicketTester;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class SelectFieldsTest {
    enum Kind { DROPDOWN, LIST, CHECKBOXES }
    private WicketTester tester;

    @BeforeEach
    void setUp() { tester = new WicketTester(); }

    @AfterEach
    void tearDown() {
        assertThat(html()).doesNotContain("style=").doesNotContainPattern("\\son[a-z]+=");
        tester.destroy();
    }

    private String html() { return tester.getLastResponseAsString(); }

    private BaseSwField<?, ?, ?> field(Kind kind, IModel<? extends List<String>> choices) {
        return switch (kind) {
            case DROPDOWN -> new SwDropdownChoice<>("choice", "Team", Model.of("Design"), choices);
            case LIST -> new SwListMultipleChoice<>("choice", "Teams", Model.ofList(new ArrayList<>(List.of("Design"))), choices).setMaxRows(3);
            case CHECKBOXES -> new SwCheckBoxMultipleChoice<>("choice", "Teams", Model.ofList(new ArrayList<>(List.of("Design"))), choices);
        };
    }

    private BaseSwField<?, ?, ?> field(Kind kind) {
        return field(kind, Model.ofList(List.of("Design", "Engineering", "Support")));
    }

    private FormTester formWith(BaseSwField<?, ?, ?> field) {
        Form<Void> form = new Form<>("form");
        form.add(field);
        tester.startComponentInPage(form, Markup.of("<form wicket:id='form'><div wicket:id='choice'></div></form>"));
        return tester.newFormTester("form");
    }

    @ParameterizedTest
    @EnumSource(Kind.class)
    void nativeControlsAreLabelledAndDescribed(Kind kind) {
        var field = field(kind).setDescription("Choose your teams.");
        formWith(field);
        TagTester input = tester.getTagByWicketId("input");
        TagTester label = tester.getTagByWicketId("label");
        assertThat(input.getAttribute("aria-describedby")).isEqualTo(tester.getTagByWicketId("description").getAttribute("id")
                + " " + tester.getTagByWicketId("error").getAttribute("id"));
        if (kind == Kind.CHECKBOXES) {
            assertThat(tester.getTagByWicketId("container").getName()).isEqualTo("fieldset");
            assertThat(label.getName()).isEqualTo("legend");
            assertThat(label.getAttribute("for")).isNull();
            assertThat(input.getAttribute("role")).isEqualTo("group");
            assertThat(input.getAttribute("aria-labelledby")).isEqualTo(label.getAttribute("id"));
            assertThat(TagTester.createTagsByAttribute(html(), "type", "checkbox", false)).hasSize(3).allSatisfy(option -> {
                assertThat(option.getAttribute("class")).isEqualTo("sw-checkbox");
                assertThat(option.getAttribute("aria-describedby")).isEqualTo(input.getAttribute("aria-describedby"));
            });
            assertThat(html()).contains("sw-checkbox-indicator").doesNotContain("<br");
        } else {
            assertThat(input.getName()).isEqualTo("select");
            assertThat(input.getAttribute("class")).isEqualTo("sw-native-select");
            assertThat(label.getAttribute("for")).isEqualTo(input.getAttribute("id"));
            if (kind == Kind.LIST) {
                assertThat(input.getAttribute("multiple")).isNotNull();
                assertThat(input.getAttribute("size")).isEqualTo("3");
                assertThat(html()).doesNotContain("sw-native-select-icon");
            } else {
                assertThat(input.getAttribute("multiple")).isNull();
                assertThat(html()).contains("sw-native-select-icon");
            }
        }
    }

    @ParameterizedTest
    @EnumSource(Kind.class)
    void submissionsSelectTheActualChoiceObjects(Kind kind) {
        var field = field(kind);
        FormTester form = formWith(field);
        if (kind == Kind.DROPDOWN) form.select("choice:input", 2);
        else form.selectMultiple("choice:input", new int[]{1, 2}, true);
        form.submit();
        tester.assertNoErrorMessage();
        assertThat(field.getModelObject()).isEqualTo(kind == Kind.DROPDOWN ? "Support" : List.of("Engineering", "Support"));
    }

    @ParameterizedTest
    @EnumSource(Kind.class)
    void requiredSelectionsRejectEmptyAndForgedSubmissions(Kind kind) {
        var field = field(kind).setRequired(true);
        formWith(field);
        for (String value : new String[]{"", "forged-choice"}) {
            FormTester form = tester.newFormTester("form");
            form.setValue("choice:input", value);
            form.submit();
            assertThat(tester.getTagByWicketId("error").getValue()).isNotEmpty();
            assertThat(field.getModelObject()).isEqualTo(kind == Kind.DROPDOWN ? "Design" : List.of("Design"));
            if (kind == Kind.CHECKBOXES) {
                assertThat(tester.getTagByWicketId("input").getAttribute("aria-required")).isNull();
                assertThat(TagTester.createTagsByAttribute(html(), "type", "checkbox", false))
                        .allSatisfy(option -> assertThat(option.getAttribute("aria-invalid")).isEqualTo("true"));
            }
        }
    }

    @ParameterizedTest
    @EnumSource(Kind.class)
    void disabledSelectionsCannotChangeTheirModels(Kind kind) {
        var field = field(kind);
        field.setEnabled(false);
        FormTester form = formWith(field);
        if (kind == Kind.DROPDOWN) form.select("choice:input", 2);
        else form.selectMultiple("choice:input", new int[]{1, 2}, true);
        form.submit();
        assertThat(field.getModelObject()).isEqualTo(kind == Kind.DROPDOWN ? "Design" : List.of("Design"));
        if (kind == Kind.CHECKBOXES) {
            assertThat(TagTester.createTagsByAttribute(html(), "type", "checkbox", false))
                    .allSatisfy(option -> assertThat(option.getAttribute("disabled")).isNotNull());
        } else {
            assertThat(tester.getTagByWicketId("input").getAttribute("disabled")).isNotNull();
        }
    }

    @ParameterizedTest
    @EnumSource(value = Kind.class, names = {"LIST", "CHECKBOXES"})
    void optionalMultiSelectionsCanBeClearedInPlace(Kind kind) {
        var field = field(kind);
        Object selected = field.getModelObject();
        FormTester form = formWith(field);
        form.selectMultiple("choice:input", new int[0], true);
        form.submit();
        tester.assertNoErrorMessage();
        assertThat(field.getModelObject()).isSameAs(selected).isEqualTo(List.of());
    }

    public record Option(String id, String label) implements Serializable {}

    @ParameterizedTest
    @EnumSource(Kind.class)
    void renderersEscapeLabelsAndIdsAndResolveStableIds(Kind kind) {
        Option first = new Option("a\"<&", "<Design>");
        Option second = new Option("support", "<Support>");
        var choices = Model.ofList(List.of(first, second));
        var renderer = new ChoiceRenderer<Option>("label", "id");
        BaseSwField<?, ?, ?> field = switch (kind) {
            case DROPDOWN -> new SwDropdownChoice<>("choice", "Team", new Model<Option>(), choices, renderer);
            case LIST -> new SwListMultipleChoice<>("choice", "Teams", Model.ofList(new ArrayList<Option>()), choices, renderer);
            case CHECKBOXES -> new SwCheckBoxMultipleChoice<>("choice", "Teams", Model.ofList(new ArrayList<Option>()), choices, renderer);
        };
        FormTester form = formWith(field);
        assertThat(html()).contains("&lt;Design&gt;").contains("a&quot;&lt;&amp;").doesNotContain("<Design>");
        if (kind == Kind.DROPDOWN) form.select("choice:input", 1);
        else form.selectMultiple("choice:input", new int[]{1}, true);
        form.submit();
        assertThat(field.getModelObject()).isEqualTo(kind == Kind.DROPDOWN ? second : List.of(second));
    }

    @ParameterizedTest
    @EnumSource(Kind.class)
    void choiceModelsAreReactiveAndDetached(Kind kind) {
        boolean[] detached = {false};
        Model<ArrayList<String>> choices = new Model<>(new ArrayList<>(List.of("Design"))) {
            @Override
            public void detach() { detached[0] = true; }
        };
        formWith(field(kind, choices));
        assertThat(detached[0]).isTrue();
        choices.setObject(new ArrayList<>(List.of("Design", "Engineering")));
        tester.startPage(tester.getLastRenderedPage());
        assertThat(html()).contains("Engineering");
    }

    @Test
    void anOptionalDropdownCanRenderAndSubmitANullChoice() {
        var field = new SwDropdownChoice<>("choice", "Team", Model.of("Design"), Model.ofList(List.of("Design", "Support")));
        field.getInput().setNullValid(true);
        FormTester form = formWith(field);
        form.setValue("choice:input", "");
        form.submit();
        tester.assertNoErrorMessage();
        assertThat(field.getModelObject()).isNull();
    }

    public static class Settings implements Serializable {
        public String team;
        public List<String> lists = new ArrayList<>();
        public List<String> checks = new ArrayList<>();
    }

    @Test
    void inheritedModelsAndInlineCheckboxGroupsWorkTogether() {
        Settings settings = new Settings();
        Form<Settings> form = new Form<>("form", new CompoundPropertyModel<>(settings));
        var choices = Model.ofList(List.of("Design", "Support"));
        form.add(new SwDropdownChoice<>("team", choices));
        form.add(new SwListMultipleChoice<>("lists", choices));
        form.add(new SwCheckBoxMultipleChoice<>("checks", choices).setInline(true));
        tester.startComponentInPage(form, Markup.of("<form wicket:id='form'><div wicket:id='team'></div>"
                + "<div wicket:id='lists'></div><div wicket:id='checks'></div></form>"));
        assertThat(tester.getTagByWicketId("input").getAttribute("name")).isEqualTo("team:input");
        FormTester ft = tester.newFormTester("form");
        ft.select("team:input", 1);
        ft.selectMultiple("lists:input", new int[]{0, 1}, true);
        ft.selectMultiple("checks:input", new int[]{1}, true);
        ft.submit();
        assertThat(settings.team).isEqualTo("Support");
        assertThat(settings.lists).containsExactly("Design", "Support");
        assertThat(settings.checks).containsExactly("Support");
        assertThat(html()).contains("data-orientation=\"horizontal\"");
    }
}
