package dev.jbaby.shadwick.components.form;

import org.apache.wicket.markup.Markup;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.model.CompoundPropertyModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.util.tester.FormTester;
import org.apache.wicket.util.tester.WicketTester;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.Locale;

import static org.assertj.core.api.Assertions.assertThat;

class EverydayFieldsTest {
    private WicketTester tester;

    @BeforeEach
    void setUp() { tester = new WicketTester(); }

    @AfterEach
    void tearDown() {
        if (tester.getLastResponse() != null) {
            assertThat(tester.getLastResponseAsString()).doesNotContain("style=").doesNotContainPattern("\\son[a-z]+=");
        }
        tester.destroy();
    }

    private BaseSwField<String, ?, ?> field(String type, Model<String> model) {
        return switch (type) {
            case "email" -> new SwEmailField("value", "Email", model);
            case "url" -> new SwUrlField("value", "Website", model);
            case "password" -> new SwPasswordField("value", "Password", model);
            case "search" -> new SwSearchField("value", "Search", model);
            case "tel" -> new SwTelField("value", "Telephone", model);
            default -> throw new IllegalArgumentException(type);
        };
    }

    private FormTester form(BaseSwField<?, ?, ?> field) {
        Form<Void> form = new Form<>("form");
        form.add(field);
        tester.startComponentInPage(form, Markup.of("<form wicket:id='form'><div wicket:id='value'></div></form>"));
        return tester.newFormTester("form");
    }

    @ParameterizedTest
    @ValueSource(strings = {"email", "url", "password", "search", "tel"})
    void nativeTypesShareLabelsDescriptionsAndEscaping(String type) {
        var field = field(type, Model.of("<secret>\"&")).setDescription("Help <text>").setPlaceholder("Type here");
        tester.startComponentInPage(field);
        var input = tester.getTagByWicketId("input");
        assertThat(input.getAttribute("type")).isEqualTo(type);
        assertThat(input.getAttribute("class")).isEqualTo("sw-input");
        assertThat(tester.getTagByWicketId("label").getAttribute("for")).isEqualTo(input.getAttribute("id"));
        assertThat(input.getAttribute("aria-describedby")).contains(tester.getTagByWicketId("description").getAttribute("id"));
        assertThat(tester.getTagByWicketId("description").getValue()).isEqualTo("Help &lt;text&gt;");
        assertThat(input.getAttribute("placeholder")).isEqualTo("Type here");
        if (type.equals("password")) {
            assertThat(tester.getLastResponseAsString()).doesNotContain("secret");
        } else {
            assertThat(tester.getLastResponseAsString()).contains("&lt;secret&gt;");
        }
    }

    @ParameterizedTest
    @ValueSource(strings = {"email", "url"})
    void invalidAddressesAreRejectedOnTheServer(String type) {
        Model<String> model = Model.of("");
        FormTester form = form(field(type, model));
        form.setValue("value:input", "not an address");
        form.submit();
        assertThat(tester.getTagByWicketId("input").getAttribute("aria-invalid")).isEqualTo("true");
        assertThat(tester.getTagByWicketId("error").getValue()).isNotEmpty();
        assertThat(model.getObject()).isEmpty();
    }

    @ParameterizedTest
    @ValueSource(strings = {"email", "url", "search", "tel"})
    void textModelsAcceptValidValues(String type) {
        String value = switch (type) {
            case "email" -> "ada@example.com";
            case "url" -> "https://example.com";
            case "tel" -> "+49 (30) 123-456";
            default -> "wicket components";
        };
        Model<String> model = Model.of("");
        FormTester form = form(field(type, model));
        form.setValue("value:input", value);
        form.submit();
        tester.assertNoErrorMessage();
        assertThat(model.getObject()).isEqualTo(value);
    }

    @Test
    void passwordsAreRequiredByDefaultAndNeverEchoedAfterFailure() {
        var field = new SwPasswordField("value", "Password", Model.of("do-not-echo"));
        assertThat(field.getInput().isRequired()).isTrue();
        FormTester form = form(field);
        assertThat(tester.getLastResponseAsString()).doesNotContain("do-not-echo");
        form.setValue("value:input", "");
        form.submit();
        assertThat(tester.getTagByWicketId("error").getValue()).contains("required");
        assertThat(tester.getLastResponseAsString()).doesNotContain("do-not-echo");
    }

    @Test
    void numberBoundsRejectOutOfRangeValuesAndConvertValidInput() {
        Model<Integer> model = Model.of(2);
        var field = new SwNumberField<>("value", "Seats", model).setMin(1).setMax(10).setStep(1);
        for (String invalid : new String[]{"0", "11", "nonsense"}) {
            FormTester form = form(field);
            form.setValue("value:input", invalid);
            form.submit();
            assertThat(tester.getTagByWicketId("error").getValue()).isNotEmpty();
            assertThat(model.getObject()).isEqualTo(2);
            field.getInput().clearInput();
        }
        FormTester form = form(field);
        form.setValue("value:input", "7");
        form.submit();
        tester.assertNoErrorMessage();
        assertThat(model.getObject()).isEqualTo(7);
    }

    @Test
    void decimalValuesUseHtmlNumberFormatEvenInAGermanSession() {
        tester.getSession().setLocale(Locale.GERMANY);
        Model<BigDecimal> model = Model.of(new BigDecimal("12.5"));
        var field = new SwNumberField<>("value", "Budget", model).setMin(BigDecimal.ZERO).setMax(new BigDecimal("100"))
                .setStep(new BigDecimal("0.5"));
        FormTester form = form(field);
        assertThat(tester.getTagByWicketId("input").getAttribute("value")).isEqualTo("12.5");
        assertThat(tester.getTagByWicketId("input").getAttribute("step")).isEqualTo("0.5");
        form.setValue("value:input", "14.5");
        form.submit();
        tester.assertNoErrorMessage();
        assertThat(model.getObject()).isEqualByComparingTo("14.5");
    }

    @Test
    void emptyNumericModelsSupportExplicitTypesAndOptionalBlankInput() {
        Model<Long> model = Model.of();
        var field = new SwNumberField<>("value", "Count", model).setType(Long.class);
        FormTester form = form(field);
        form.setValue("value:input", "123");
        form.submit();
        assertThat(model.getObject()).isEqualTo(123L);
        form = tester.newFormTester("form");
        form.setValue("value:input", "");
        form.submit();
        tester.assertNoErrorMessage();
        assertThat(model.getObject()).isNull();
    }

    @Test
    void numericConstraintModelsAreReactiveAndDetach() {
        boolean[] detached = {false};
        Model<Integer> max = new Model<>(10) {
            @Override
            public void detach() { detached[0] = true; }
        };
        var field = new SwNumberField<>("value", Model.of(2)).setMin(Model.of(0)).setMax(max).setStep(Model.of(2));
        form(field);
        assertThat(detached[0]).isTrue();
        max.setObject(20);
        tester.startPage(tester.getLastRenderedPage());
        assertThat(tester.getTagByWicketId("input").getAttribute("max")).isEqualTo("20");
        field.setStep(0);
        tester.startPage(tester.getLastRenderedPage());
        assertThat(tester.getTagByWicketId("input").getAttribute("step")).isEqualTo("any");
    }

    public static class Settings implements Serializable {
        public Integer value = 3;
    }

    @Test
    void numericModelsCanBeInheritedAndDisabledInputsCannotChangeThem() {
        Settings settings = new Settings();
        Form<Settings> form = new Form<>("form", new CompoundPropertyModel<>(settings));
        var field = new SwNumberField<Integer>("value").setMin(0).setMax(10);
        form.add(field);
        tester.startComponentInPage(form, Markup.of("<form wicket:id='form'><div wicket:id='value'></div></form>"));
        FormTester ft = tester.newFormTester("form");
        ft.setValue("value:input", "8");
        ft.submit();
        assertThat(settings.value).isEqualTo(8);
        field.setEnabled(false);
        tester.startPage(tester.getLastRenderedPage());
        ft = tester.newFormTester("form");
        ft.setValue("value:input", "9");
        ft.submit();
        assertThat(settings.value).isEqualTo(8);
    }
}
