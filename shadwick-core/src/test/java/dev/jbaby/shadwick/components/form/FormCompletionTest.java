package dev.jbaby.shadwick.components.form;

import dev.jbaby.shadwick.components.SwFieldset;
import org.apache.wicket.AttributeModifier;
import org.apache.wicket.MarkupContainer;
import org.apache.wicket.markup.IMarkupResourceStreamProvider;
import org.apache.wicket.markup.Markup;
import org.apache.wicket.markup.html.form.*;
import org.apache.wicket.model.*;
import org.apache.wicket.util.resource.*;
import org.apache.wicket.util.tester.WicketTester;
import org.apache.wicket.validation.validator.StringValidator;
import org.junit.jupiter.api.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.*;
import java.math.BigDecimal;
import java.util.*;
import static org.assertj.core.api.Assertions.*;

class FormCompletionTest {
    WicketTester tester;
    @BeforeEach void start() { tester = new WicketTester(); }
    @AfterEach void stop() { tester.destroy(); }
    void render(BaseSwField<?, ?, ?> field) {
        Form<Void> form = new Form<>("form"); form.add(field);
        tester.startComponentInPage(form, Markup.of("<form wicket:id='form'><div wicket:id='value'></div></form>"));
    }
    void submit(String text) {
        var form = tester.newFormTester("form"); form.setValue("value:input", text); form.submit();
    }
    @ParameterizedTest @CsvSource({"en-US,'$1,234.565',1234.56", "de-DE,'1.234,575 EUR',1234.58", "fr-FR,'1 234,50 €',1234.50", "ja-JP,'JPY 1234.5',1234", "hr-HR,'1.234,50 EUR',1234.50"})
    void localizedCurrencyIsExactAndRoundsHalfEven(String locale, String text, String expected) {
        tester.getSession().setLocale(Locale.forLanguageTag(locale));
        var model = Model.of(BigDecimal.ZERO);
        render(new SwMoneyField("value", "Amount", model).setCurrency(Currency.getInstance(locale.startsWith("ja") ? "JPY" : locale.startsWith("en") ? "USD" : "EUR")));
        assertThat(tester.getTagByWicketId("input").getAttribute("type")).isEqualTo("text");
        assertThat(tester.getTagByWicketId("input").getAttribute("inputmode")).isEqualTo("decimal");
        submit(text); tester.assertNoErrorMessage(); assertThat(model.getObject()).isEqualByComparingTo(expected);
    }
    @ParameterizedTest @ValueSource(strings={"12,34.50", "1,23,456", "1$2", "$12$", "12USDUSD", "12junk", "NaN", "Infinity", "1e3", "--12", ".", "$", "USD"})
    void malformedMoneyNeverChangesTheModel(String value) {
        tester.getSession().setLocale(Locale.US); var model = Model.of(new BigDecimal("9.00"));
        render(new SwMoneyField("value", "Amount", model).setCurrency(Currency.getInstance("USD")));
        submit(value); assertThat(model.getObject()).isEqualTo(new BigDecimal("9.00"));
        assertThat(tester.getTagByWicketId("input").getAttribute("aria-invalid")).isEqualTo("true");
        assertThat(tester.getTagByWicketId("error").getValue()).contains("Amount").doesNotContain("${");
    }
    @Test void arbitraryPrecisionAndPseudoCurrencyDoNotUseFloatingPoint() {
        tester.getSession().setLocale(Locale.US); var model = Model.of(BigDecimal.ZERO);
        render(new SwMoneyField("value", "Amount", model).setCurrency(Currency.getInstance("XXX")));
        submit("90071992547409931234567890.125"); tester.assertNoErrorMessage();
        assertThat(model.getObject()).isEqualTo(new BigDecimal("90071992547409931234567890.12"));
    }
    @Test void percentUsesDisplayedUnitsAndCanChangePrecision() {
        tester.getSession().setLocale(Locale.GERMANY); var model = Model.of(BigDecimal.ZERO);
        var field = new SwPercentField("value", "Tax", model).setFractionDigits(3); render(field);
        submit("19,1255 %"); tester.assertNoErrorMessage(); assertThat(model.getObject()).isEqualTo(new BigDecimal("19.126"));
        assertThatThrownBy(() -> field.setFractionDigits(-1)).isInstanceOf(IllegalArgumentException.class);
    }
    @Test void currencyModelAndBoundsCanBeReconfiguredWithoutStaleConvertersOrValidators() {
        tester.getSession().setLocale(Locale.US); var currency = Model.of(Currency.getInstance("USD"));
        var model = Model.of(BigDecimal.TEN); var field = new SwMoneyField("value", "Amount", model).setCurrency(currency);
        field.setMin(BigDecimal.ZERO).setMax(BigDecimal.TEN).setMax(new BigDecimal("100")); render(field);
        currency.setObject(Currency.getInstance("JPY")); submit("25.5"); tester.assertNoErrorMessage();
        assertThat(model.getObject()).isEqualTo(new BigDecimal("26"));
        assertThatThrownBy(() -> field.setMin(new BigDecimal("101"))).isInstanceOf(IllegalArgumentException.class);
        field.setMin((BigDecimal)null).setMax((BigDecimal)null); submit("-500"); tester.assertNoErrorMessage();
        assertThat(model.getObject()).isEqualByComparingTo("-500");
    }
    @Test void requiredEmptyDecimalLeavesThePreviousAmount() {
        var model = Model.of(BigDecimal.TEN); render(new SwMoneyField("value", "Amount", model).setRequired(true));
        submit(""); assertThat(model.getObject()).isEqualTo(BigDecimal.TEN);
        assertThat(tester.getTagByWicketId("error").getValue()).isNotBlank();
    }
    @ParameterizedTest @ValueSource(strings={"-1", "101", "12.5"})
    void rangeDefaultsAreAlsoEnforcedOnTheServer(String invalid) {
        var model = Model.of(new BigDecimal("50")); render(new SwRangeField<>("value", "Volume", model));
        var tag = tester.getTagByWicketId("input");
        assertThat(tag.getAttribute("type")).isEqualTo("range");
        assertThat(tag.getAttribute("min")).isEqualTo("0"); assertThat(tag.getAttribute("max")).isEqualTo("100");
        assertThat(tag.getAttribute("step")).isEqualTo("1"); submit(invalid);
        assertThat(model.getObject()).isEqualTo(new BigDecimal("50"));
        assertThat(tester.getTagByWicketId("input").getAttribute("aria-invalid")).isEqualTo("true");
    }
    @Test void decimalRangeStepsUseTheMinimumAsTheirBaseAndReadChangingModels() {
        var min = Model.of(new BigDecimal("-1.25")); var max = Model.of(new BigDecimal("2.25"));
        var step = Model.of(new BigDecimal("0.5")); var model = Model.of(BigDecimal.ZERO);
        var field = new SwRangeField<>("value", "Offset", model).setMin(min).setMax(max).setStep(step); render(field);
        submit("-0.75"); tester.assertNoErrorMessage(); assertThat(model.getObject()).isEqualByComparingTo("-0.75");
        step.setObject(new BigDecimal("0.25")); submit("0"); tester.assertNoErrorMessage();
        assertThat(model.getObject()).isEqualByComparingTo("0");
        assertThatThrownBy(() -> field.setStep(BigDecimal.ZERO)).isInstanceOf(IllegalArgumentException.class);
    }
    @Test void directNativeRangeConstraintSettersRemainConsistent() {
        var model = Model.of(10); var field = new SwRangeField<>("value", "Volume", model);
        field.getInput().setMinimum(5).setMaximum(15).setStep(5); render(field); submit("7");
        assertThat(model.getObject()).isEqualTo(10);
    }
    @Test void nativeFieldsetLabelsDescriptionsEscapingAndDisabledBody() {
        var group = new SwFieldset("group", "Prices <gross>").setDescription("Before tax");
        group.add(new SwMoneyField("amount", "Amount", Model.of(BigDecimal.TEN)));
        group.add(AttributeModifier.replace("aria-describedby", "outside")); group.setEnabled(false);
        tester.startComponentInPage(group, Markup.of("<fieldset wicket:id='group'><div wicket:id='amount'></div></fieldset>"));
        var tag = tester.getTagByWicketId("group"); assertThat(tag.getAttribute("disabled")).isEqualTo("disabled");
        assertThat(tester.getTagByWicketId("legend").getValue()).isEqualTo("Prices &lt;gross&gt;");
        assertThat(tester.getTagByWicketId("input").getAttribute("disabled")).isEqualTo("disabled");
    }
    @Test void customFieldSurfacesNestedErrorsAndPreservesCallerDescriptions() {
        var model = Model.of("old"); var field = new SwCustomField<>("value", "Reference", model, Compound::new).setDescription("Use three letters");
        render(field); var f = tester.newFormTester("form"); f.setValue("value:input:part", "x"); f.submit();
        assertThat(model.getObject()).isEqualTo("old");
        assertThat(tester.getTagByWicketId("error").getValue()).isNotBlank();
        assertThat(tester.getTagByWicketId("part").getAttribute("aria-invalid")).isEqualTo("true");
        assertThat(tester.getTagByWicketId("part").getAttribute("aria-describedby")).contains("outside", tester.getTagByWicketId("error").getAttribute("id"), tester.getTagByWicketId("description").getAttribute("id"));
        var again = tester.newFormTester("form"); again.setValue("value:input:part", "ABC"); again.submit();
        tester.assertNoErrorMessage(); assertThat(model.getObject()).isEqualTo("ABC");
    }
    @Test void customFactoryMustReturnTheRequestedComponentId() {
        assertThatThrownBy(() -> new SwCustomField<String>("value", (id, model) -> new TextField<>("wrong", model)).getInput()).isInstanceOf(IllegalArgumentException.class);
    }
    @Test void constraintAndCurrencyModelsAreDetachedOncePerRequest() {
        var currency = new CountingModel<>(Currency.getInstance("EUR"));
        render(new SwMoneyField("value", "Amount", Model.of(BigDecimal.TEN)).setCurrency(currency));
        assertThat(currency.detaches).isEqualTo(1);
        var min = new CountingModel<>(0); var max = new CountingModel<>(10); var step = new CountingModel<>(1);
        render(new SwRangeField<>("value", "Volume", Model.of(5)).setMin(min).setMax(max).setStep(step));
        assertThat(min.detaches).isEqualTo(1); assertThat(max.detaches).isEqualTo(1); assertThat(step.detaches).isEqualTo(1);
    }
    @Test void fieldsetDescriptionLinksToTheGroupAndDisappearsWithItsModel() {
        var description = Model.of("Grouped settings"); var group = new SwFieldset("group", "Settings").setDescription(description);
        tester.startComponentInPage(group, Markup.of("<fieldset wicket:id='group' aria-describedby='outside'></fieldset>"));
        assertThat(tester.getTagByWicketId("group").getAttribute("aria-describedby"))
                .contains("outside", tester.getTagByWicketId("description").getAttribute("id"));
        description.setObject(""); tester.startPage(tester.getLastRenderedPage());
        assertThat(tester.getTagByWicketId("group").getAttribute("aria-describedby")).isEqualTo("outside");
    }
    @ParameterizedTest @ValueSource(strings={"-$12.50", "$-12.50", "-12.50 USD"})
    void currencyUnitsPreserveNegativeSigns(String text) {
        tester.getSession().setLocale(Locale.US); var model = Model.of(BigDecimal.ZERO);
        render(new SwMoneyField("value", "Amount", model).setCurrency(Currency.getInstance("USD")));
        submit(text); tester.assertNoErrorMessage(); assertThat(model.getObject()).isEqualTo(new BigDecimal("-12.50"));
    }
    @ParameterizedTest @ValueSource(strings={"%", "19%%", "1%9", "1,23%"})
    void malformedPercentNeverErasesOrChangesTheModel(String text) {
        tester.getSession().setLocale(Locale.US); var model = Model.of(BigDecimal.TEN);
        render(new SwPercentField("value", "Rate", model)); submit(text);
        assertThat(model.getObject()).isEqualTo(BigDecimal.TEN);
        assertThat(tester.getTagByWicketId("input").getAttribute("aria-invalid")).isEqualTo("true");
    }
    @Test void blankOptionalDecimalClearsTheModelAndPercentHasNoImplicitDomainBounds() {
        tester.getSession().setLocale(Locale.US); var model = Model.of(BigDecimal.TEN);
        render(new SwPercentField("value", "Rate", model)); submit("150%"); tester.assertNoErrorMessage();
        assertThat(model.getObject()).isEqualByComparingTo("150"); submit("  "); tester.assertNoErrorMessage();
        assertThat(model.getObject()).isNull();
    }
    @Test void compoundRootErrorsInvalidateItsChildrenAndRemainInline() {
        var field = new SwCustomField<>("value", "Reference", Model.of("ABC"), Compound::new)
                .addValidator(value -> value.error(new org.apache.wicket.validation.ValidationError("Combined reference rejected")));
        render(field); var form = tester.newFormTester("form"); form.setValue("value:input:part", "VALID"); form.submit();
        assertThat(tester.getTagByWicketId("error").getValue()).isEqualTo("Combined reference rejected");
        assertThat(tester.getTagByWicketId("part").getAttribute("aria-invalid")).isEqualTo("true");
    }
    @Test void rangeRejectsInvalidModelConstraintsBeforeRendering() {
        var step = Model.of(0); var field = new SwRangeField<>("value", "Volume", Model.of(5)).setStep(step);
        assertThatThrownBy(() -> render(field)).hasRootCauseInstanceOf(IllegalArgumentException.class);
    }
    private static class CountingModel<T extends java.io.Serializable> extends Model<T> {
        int detaches;
        CountingModel(T value) { super(value); }
        @Override public void detach() { detaches++; }
    }
    public static class Compound extends FormComponentPanel<String> implements IMarkupResourceStreamProvider {
        final TextField<String> part;
        public Compound(String id, IModel<String> model) {
            super(id, model); part = new TextField<>("part", Model.of(model.getObject()));
            part.setLabel(Model.of("Reference")); part.add(StringValidator.minimumLength(3));
            part.add(AttributeModifier.replace("aria-describedby", "outside")); add(part);
        }
        @Override public void convertInput() { setConvertedInput(part.getConvertedInput()); }
        @Override public IResourceStream getMarkupResourceStream(MarkupContainer c, Class<?> type) {
            return new StringResourceStream("<wicket:panel><label for='compound-part'>Code</label><input wicket:id='part' id='compound-part'/><span id='outside'>Three letters</span></wicket:panel>");
        }
    }
}
