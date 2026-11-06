package dev.jbaby.shadwick.components.form;

import org.apache.wicket.markup.Markup;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.model.CompoundPropertyModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.util.tester.WicketTester;
import org.junit.jupiter.api.*;
import java.io.Serializable;
import java.util.Locale;
import static org.assertj.core.api.Assertions.*;

class SwRatingFieldTest {
    WicketTester tester;
    @BeforeEach void setup() { tester = new WicketTester(); }
    @AfterEach void cleanup() { tester.destroy(); }
    void start(SwRatingField rating) {
        Form<Void> form = new Form<>("form"); form.add(rating);
        tester.startComponentInPage(form, Markup.of("<form wicket:id='form'><div wicket:id='rating'></div></form>"));
    }
    @Test void nativeRadioValuesSubmitWholeStarsAndRequiredErrorsStayInline() {
        Model<Integer> value = Model.of(); var rating = new SwRatingField("rating", "Service", value).setRequired(true);
        start(rating); tester.newFormTester("form").submit();
        assertThat(tester.getLastResponseAsString()).contains("Service", "is required", "aria-invalid=\"true\"");
        var form = tester.newFormTester("form"); form.setValue("rating:input", "4"); form.submit();
        tester.assertNoErrorMessage(); assertThat(value.getObject()).isEqualTo(4);
    }
    @Test void forgedAndOutOfRangeValuesCannotReachTheModel() {
        Model<Integer> value = Model.of(3); var rating = new SwRatingField("rating", "Service", value);
        start(rating); var form = tester.newFormTester("form"); form.setValue("rating:input", "9"); form.submit();
        assertThat(value.getObject()).isEqualTo(3); assertThat(rating.getInput().isValid()).isFalse();
    }
    @Test void starsAreLocalizedAndCustomMaximumsHaveNativeLabels() {
        tester.getSession().setLocale(Locale.forLanguageTag("hr")); start(new SwRatingField("rating", "Ocjena", Model.of(7)).setMax(10));
        String html = tester.getLastResponseAsString();
        assertThat(html).contains("Broj zvjezdica: 7", "value=\"10\"", "role=\"radiogroup\"", "<legend");
        assertThat(html).doesNotContain("style=").doesNotContainPattern("\\son[a-z]+=");
        assertThatIllegalArgumentException().isThrownBy(() -> new SwRatingField("rating").setMax(0));
    }
    public static class EscapingPage extends org.apache.wicket.markup.html.WebPage implements org.apache.wicket.markup.IMarkupResourceStreamProvider {
        public EscapingPage() { add(new SwRatingField("rating", "Rating", Model.of())); }
        @Override public org.apache.wicket.util.resource.IResourceStream getMarkupResourceStream(org.apache.wicket.MarkupContainer container, Class<?> type) {
            return new org.apache.wicket.util.resource.StringResourceStream("<html><body><div wicket:id='rating'></div></body></html>");
        }
    }
    @Test void applicationResourceTextIsEscapedInsideTheFixedStarMarkup() {
        tester.startPage(EscapingPage.class);
        assertThat(tester.getLastResponseAsString()).contains("&lt;img src=x&gt; Stars: 1").doesNotContain("<img src=x>");
    }
    public static class Survey implements Serializable { public Integer rating; }
    @Test void fieldsInheritTheirValueFromCompoundPropertyModelsAndIgnoreDisabledSubmissions() {
        Survey survey = new Survey(); Form<Survey> form = new Form<>("form", new CompoundPropertyModel<>(survey));
        SwRatingField rating = new SwRatingField("rating"); form.add(rating);
        tester.startComponentInPage(form, Markup.of("<form wicket:id='form'><div wicket:id='rating'></div></form>"));
        var submit = tester.newFormTester("form"); submit.setValue("rating:input", "2"); submit.submit();
        assertThat(survey.rating).isEqualTo(2);
        rating.setEnabled(false); tester.startComponentInPage(form, Markup.of("<form wicket:id='form'><div wicket:id='rating'></div></form>"));
        tester.getRequest().setParameter(rating.getInput().getInputName(), "5"); tester.submitForm("form");
        assertThat(survey.rating).isEqualTo(2);
    }
}
