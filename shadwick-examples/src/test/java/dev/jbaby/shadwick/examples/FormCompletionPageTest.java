package dev.jbaby.shadwick.examples;

import dev.jbaby.shadwick.components.SwFieldset;
import dev.jbaby.shadwick.components.form.*;
import org.apache.wicket.util.tester.WicketTester;
import org.junit.jupiter.api.*;
import java.math.BigDecimal;
import java.util.Locale;
import static org.assertj.core.api.Assertions.assertThat;

class FormCompletionPageTest {
    WicketTester tester;
    @BeforeEach void start() { tester = new WicketTester(new ExamplesApplication()); tester.getSession().setLocale(Locale.US); }
    @AfterEach void stop() { tester.destroy(); }
    private static final String PRICING = "pricing:pricing_body:";
    @Test void faviconRequestsAreStatelessResourcesAndCannotChangeTheCatalogLocale() {
        tester.getSession().setLocale(Locale.FRENCH);
        tester.executeUrl("favicon.ico");
        assertThat(tester.getLastResponse().getContentType()).contains("image/svg+xml");
        assertThat(tester.getLastResponseAsString()).contains("<svg");
        assertThat(tester.getSession().getLocale()).isEqualTo(Locale.FRENCH);
    }
    @Test void inheritedModelsAndNativeSubmitUpdateTheWholeQuote() {
        tester.startPage(FormCompletionPage.class); var form = tester.newFormTester("form");
        form.setValue(PRICING + "budget:input", "1234.565"); form.setValue(PRICING + "tax:input", "7.5%");
        form.setValue(PRICING + "priority:input", "8"); form.setValue(PRICING + "reference:input:code", "QUOTE");
        form.submit("save"); tester.assertNoErrorMessage();
        assertThat(tester.getLastResponseAsString()).contains("Saved budget 1234.56; tax 7.5%; priority 8; reference QUOTE-1.");
    }
    @Test void serializedPageRetainsFactoriesConvertersAndAjaxCallbacks() {
        tester.startPage(FormCompletionPage.class);
        var serializer = tester.getApplication().getFrameworkSettings().getSerializer();
        var restored = (FormCompletionPage)serializer.deserialize(serializer.serialize(tester.getLastRenderedPage()));
        tester.startPage(restored);
        tester.clickLink("ajaxForm:replace");
        var f = tester.newFormTester("ajaxForm"); f.setValue(PRICING + "budget:input", "20.555");
        f.setValue(PRICING + "reference:input:code", "NEW");
        tester.executeAjaxEvent("ajaxForm:save", "click"); tester.assertNoErrorMessage();
        assertThat(tester.getLastResponseAsString()).contains("Saved budget 20.56", "reference NEW-2");
    }
    @Test void invalidNestedInputShowsInlineAndCanBeCorrectedAfterAjaxReplacement() {
        tester.startPage(FormCompletionPage.class); var f = tester.newFormTester("ajaxForm");
        f.setValue(PRICING + "reference:input:code", "x"); tester.executeAjaxEvent("ajaxForm:save", "click");
        var custom = tester.getComponentFromLastRenderedPage("ajaxForm:" + PRICING + "reference");
        tester.assertComponentOnAjaxResponse(custom);
        assertThat(tester.getLastResponseAsString()).contains("minimum", "aria-invalid=\"true\"");
        tester.clickLink("ajaxForm:replace");
        f = tester.newFormTester("ajaxForm"); f.setValue(PRICING + "reference:input:code", "FIXED");
        tester.executeAjaxEvent("ajaxForm:save", "click"); tester.assertNoErrorMessage();
        assertThat(tester.getLastResponseAsString()).contains("reference FIXED-2");
    }
    @Test void disabledFieldsetRejectsTamperedChildSubmission() {
        tester.startPage(FormCompletionPage.class); tester.clickLink("form:toggle");
        var group = (SwFieldset)tester.getComponentFromLastRenderedPage("form:pricing");
        assertThat(group.isEnabled()).isFalse(); var input = tester.newFormTester("form");
        input.setValue(PRICING + "budget:input", "999"); input.submit("save");
        var money = (SwMoneyField)tester.getComponentFromLastRenderedPage("form:" + PRICING + "budget");
        assertThat(money.getModelObject()).isEqualTo(new BigDecimal("1250.00"));
    }
}
