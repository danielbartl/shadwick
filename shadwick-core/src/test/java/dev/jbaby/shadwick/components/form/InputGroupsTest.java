package dev.jbaby.shadwick.components.form;

import dev.jbaby.shadwick.Sw;
import dev.jbaby.shadwick.behaviors.InputGroupAddonBehavior;
import dev.jbaby.shadwick.behaviors.ButtonBehavior;
import org.apache.wicket.AttributeModifier;
import org.apache.wicket.markup.Markup;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.markup.html.form.Button;
import org.apache.wicket.model.CompoundPropertyModel;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.util.tester.FormTester;
import org.apache.wicket.util.tester.WicketTester;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.io.Serializable;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class InputGroupsTest {
    private WicketTester tester;
    @BeforeEach void setUp() { tester = new WicketTester(); }
    @AfterEach void tearDown() {
        if (tester.getLastResponse() != null) {
            assertThat(html()).doesNotContain("style=").doesNotContainPattern("\\son[a-z]+=");
        }
        tester.destroy();
    }
    private String html() { return tester.getLastResponseAsString(); }
    private FormTester form(BaseSwInputField<?, ?, ?> field) {
        Form<Void> form = new Form<>("form");
        form.add(field);
        tester.startComponentInPage(form, Markup.of("<form wicket:id='form'><div wicket:id='value'></div></form>"));
        return tester.newFormTester("form");
    }

    @Test
    void fieldsWithoutAddonsKeepTheirPlainControlAndPath() {
        var field = new SwTextField<>("value", "Website", Model.of("example"));
        form(field);
        assertThat(tester.getTagByWicketId("input").getAttribute("class")).isEqualTo("sw-input");
        assertThat(html()).doesNotContain("class=\"sw-input-group\"", "<datalist");
        tester.assertComponent("form:value:input", org.apache.wicket.markup.html.form.TextField.class);
    }

    @Test
    void prefixAndSuffixDescribeTheControlWithoutChangingItsNameOrValue() {
        var field = new SwTextField<>("value", "Website", Model.of("example"))
                .setPrefix("https://").setSuffix(".com").setDescription("Your public address.");
        form(field);
        var input = tester.getTagByWicketId("input");
        assertThat(input.getAttribute("class")).isEqualTo("sw-input-group-input");
        assertThat(input.getAttribute("value")).isEqualTo("example");
        assertThat(tester.getTagByWicketId("group").getAttribute("role")).isEqualTo("group");
        assertThat(tester.getTagByWicketId("label").getAttribute("for")).isEqualTo(input.getAttribute("id"));
        assertThat(tester.getTagByWicketId("group").getAttribute("aria-labelledby")).isNull();
        for (String addon : List.of("prefix", "suffix")) {
            var tag = tester.getTagByWicketId(addon);
            assertThat(tag.getName()).isEqualTo("span");
            assertThat(tag.getAttribute("for")).isNull();
            assertThat(input.getAttribute("aria-describedby")).contains(tag.getAttribute("id"));
        }
        assertThat(tester.getTagByWicketId("label").getValue()).isEqualTo("Website");
        assertThat(html().indexOf("wicket:id=\"input\"")).isLessThan(html().indexOf("wicket:id=\"prefix\""));
    }

    @Test
    void onlyConfiguredAddonsRenderAndTheyFollowTheirModels() {
        Model<String> unit = Model.of("kg");
        var field = new SwNumberField<>("value", "Weight", Model.of(5)).setSuffix(unit);
        form(field);
        assertThat(tester.getTagByWicketId("prefix")).isNull();
        assertThat(tester.getTagByWicketId("suffix").getValue()).isEqualTo("kg");
        unit.setObject(null);
        tester.startPage(tester.getLastRenderedPage());
        assertThat(tester.getTagByWicketId("suffix")).isNull();
        assertThat(tester.getTagByWicketId("input").getAttribute("class")).isEqualTo("sw-input");
        assertThat(html()).doesNotContain("class=\"sw-input-group\"");
        unit.setObject("lb");
        tester.startPage(tester.getLastRenderedPage());
        assertThat(tester.getTagByWicketId("suffix").getValue()).isEqualTo("lb");
        assertThat(tester.getTagByWicketId("input").getAttribute("class")).isEqualTo("sw-input-group-input");
    }

    @Test
    void groupedFieldsPreserveConversionValidationAndApplicationClasses() {
        Model<Integer> value = Model.of(3);
        var field = new SwNumberField<>("value", "Weight", value).setMin(1).setMax(10).setSuffix("kg")
                .addToInput(AttributeModifier.append("class", "application-control"));
        FormTester form = form(field);
        assertThat(tester.getTagByWicketId("input").getAttribute("class"))
                .contains("application-control", "sw-input-group-input").doesNotContain("sw-input ");
        form.setValue("value:input", "99");
        form.submit();
        assertThat(tester.getTagByWicketId("input").getAttribute("aria-invalid")).isEqualTo("true");
        assertThat(value.getObject()).isEqualTo(3);
        form = tester.newFormTester("form");
        form.setValue("value:input", "7");
        form.submit();
        tester.assertNoErrorMessage();
        assertThat(value.getObject()).isEqualTo(7);
    }

    @Test
    void textareaAddonsUseBlockAlignmentAndRetainTheValue() {
        Model<String> value = Model.of("Notes");
        var field = new SwTextArea<>("value", "Message", value).setPrefix("Draft").setSuffix("Markdown supported");
        FormTester form = form(field);
        assertThat(tester.getTagByWicketId("input").getAttribute("class")).isEqualTo("sw-input-group-textarea");
        assertThat(tester.getTagByWicketId("prefix").getAttribute("data-align")).isEqualTo("block-start");
        assertThat(tester.getTagByWicketId("suffix").getAttribute("data-align")).isEqualTo("block-end");
        form.setValue("value:input", "Updated notes");
        form.submit();
        assertThat(value.getObject()).isEqualTo("Updated notes");
    }

    @Test
    void groupedDatesKeepTheirNativeTypeAndIsoConverter() {
        Model<LocalDate> value = Model.of(LocalDate.of(2026, 11, 4));
        FormTester form = form(new SwDateField("value", "Due date", value).setSuffix("Local date"));
        assertThat(tester.getTagByWicketId("input").getAttribute("type")).isEqualTo("date");
        assertThat(tester.getTagByWicketId("input").getAttribute("value")).isEqualTo("2026-11-04");
        form.setValue("value:input", "2026-12-24");
        form.submit();
        assertThat(value.getObject()).isEqualTo(LocalDate.of(2026, 12, 24));
    }

    @Test
    void addonAndDatalistModelsDetachAndEscapeTheirValues() {
        boolean[] detached = {false, false};
        Model<String> unit = new Model<>("<unit>") {
            @Override public void detach() { detached[0] = true; }
        };
        Model<ArrayList<String>> suggestions = new Model<>(new ArrayList<>(List.of("One", "<Two>\""))) {
            @Override public void detach() { detached[1] = true; }
        };
        var field = new SwTextField<String>("value", "Project", new Model<>()).setSuffix(unit).setSuggestions(suggestions);
        form(field);
        assertThat(html()).contains("&lt;unit&gt;", "&lt;Two&gt;&quot;").doesNotContain("<unit>", "<Two>");
        assertThat(detached).containsExactly(true, true);
        var datalist = tester.getTagByWicketId("suggestions");
        assertThat(tester.getTagByWicketId("input").getAttribute("list")).isEqualTo(datalist.getAttribute("id"));
        suggestions.setObject(new ArrayList<>());
        tester.startPage(tester.getLastRenderedPage());
        assertThat(tester.getTagByWicketId("input").getAttribute("list")).isNull();
        assertThat(html()).doesNotContain("<datalist");
    }

    @Test
    void suggestionsDoNotRestrictSubmittedValues() {
        Model<String> value = Model.of();
        FormTester form = form(new SwTextField<>("value", "Project", value).setSuggestions(List.of("One", "Two")));
        form.setValue("value:input", "Another project");
        form.submit();
        tester.assertNoErrorMessage();
        assertThat(value.getObject()).isEqualTo("Another project");
    }

    public static class Settings implements Serializable { public String value; }

    @Test
    void addonsAndDatalistDoNotInheritCompoundProperties() {
        Settings settings = new Settings();
        Form<Settings> form = new Form<>("form", new CompoundPropertyModel<>(settings));
        form.add(new SwTextField<String>("value").setPrefix("https://"));
        tester.startComponentInPage(form, Markup.of("<form wicket:id='form'><div wicket:id='value'></div></form>"));
        FormTester ft = tester.newFormTester("form");
        ft.setValue("value:input", "example.com");
        ft.submit();
        assertThat(settings.value).isEqualTo("example.com");
        assertThat(tester.getTagByWicketId("suffix")).isNull();
    }

    @ParameterizedTest
    @ValueSource(strings = {"INLINE_START", "INLINE_END", "BLOCK_START", "BLOCK_END"})
    void composableBehaviorsSupplyGroupLayoutAndButtonSemantics(String alignment) {
        WebMarkupContainer group = new WebMarkupContainer("group");
        group.add(Sw.Behaviors.inputGroup());
        WebMarkupContainer addon = new WebMarkupContainer("addon");
        addon.add(Sw.Behaviors.inputGroupAddon().setAlign(InputGroupAddonBehavior.Align.valueOf(alignment)).setBorder(true));
        addon.add(new Button("clear").add(Sw.Behaviors.inputGroupButton().setVariant(ButtonBehavior.Variant.OUTLINE)));
        group.add(addon);
        tester.startComponentInPage(group, Markup.of("<div wicket:id='group' aria-label='Search'><div wicket:id='addon'>"
                + "<button wicket:id='clear'>Clear</button></div></div>"));
        assertThat(tester.getTagByWicketId("group").getAttribute("class")).isEqualTo("sw-input-group");
        assertThat(tester.getTagByWicketId("group").getAttribute("role")).isEqualTo("group");
        String expectedBorder = alignment.equals("BLOCK_START") ? "bottom" : alignment.equals("BLOCK_END") ? "top" : null;
        assertThat(tester.getTagByWicketId("addon").getAttribute("data-border")).isEqualTo(expectedBorder);
        assertThat(tester.getTagByWicketId("clear").getAttribute("class")).contains("sw-button", "sw-input-group-button");
        assertThat(tester.getTagByWicketId("clear").getAttribute("data-size")).isEqualTo("sm");
        assertThat(tester.getTagByWicketId("clear").getAttribute("data-variant")).isEqualTo("outline");
    }
}
