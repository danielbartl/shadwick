package dev.jbaby.shadwick.examples;

import dev.jbaby.shadwick.Sw;
import dev.jbaby.shadwick.components.*;
import dev.jbaby.shadwick.components.form.*;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.markup.html.AjaxLink;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.form.*;
import org.apache.wicket.model.*;
import org.apache.wicket.request.mapper.parameter.PageParameters;
import java.io.Serializable;
import java.math.BigDecimal;
import java.util.Currency;

/** A native and Ajax quote form combining the form-completion components. */
public class FormCompletionPage extends BasePage {
    public FormCompletionPage(PageParameters parameters) {
        super(parameters); addForm("form", false); addForm("ajaxForm", true);
    }
    private void addForm(String id, boolean ajax) {
        var saved = new Label("saved", Model.of("")); saved.setOutputMarkupId(true);
        Form<Quote> form = new Form<>(id, new CompoundPropertyModel<>(new Quote())) {
            @Override protected void onSubmit() { if (!ajax) saved.setDefaultModelObject(receipt(getModelObject())); }
        };
        var group = new SwFieldset("pricing", "Quote details").setDescription("Set a budget, tax rate and delivery priority.");
        group.add(new SwMoneyField("budget").setLabel("Budget").setCurrency(Currency.getInstance("EUR")).setMin(BigDecimal.ZERO).setRequired(true)
                .setDescription("Enter an amount using your selected language’s number format."));
        group.add(new SwPercentField("tax").setLabel("Tax rate").setMin(BigDecimal.ZERO).setMax(new BigDecimal("100"))
                .setDescription("19 means 19 percent."));
        group.add(new SwRangeField<Integer>("priority").setLabel("Priority").setMin(0).setMax(10).setStep(1).setDescription("0 is flexible; 10 is urgent."));
        var reference = new SwCustomField<String>("reference", "Project reference", new PropertyModel<>(form.getModel(), "reference"), ReferenceInput::new)
                .setDescription("A short project code, with an optional revision.");
        group.add(reference); form.add(group);
        if (ajax) form.add(SwSubmitButton.onSubmit("save", "Save quote", target -> {
            saved.setDefaultModelObject(receipt(form.getModelObject())); target.add(saved);
        }));
        else form.add(new Button("save").add(Sw.Behaviors.button()));
        form.add(saved);
        form.add(new AjaxLink<Void>("toggle") {
            @Override public void onClick(AjaxRequestTarget target) {
                group.setEnabled(!group.isEnabled()); target.add(group);
            }
        }.add(Sw.Behaviors.button().setVariant(dev.jbaby.shadwick.behaviors.ButtonBehavior.Variant.OUTLINE)));
        form.add(new AjaxLink<Void>("replace") {
            @Override public void onClick(AjaxRequestTarget target) {
                reference.getInput().replace(new TextField<>("revision", Model.of("2")).setLabel(Model.of("Revision")).add(new dev.jbaby.shadwick.behaviors.StyledBehavior("sw-input")));
                target.add(reference);
            }
        }.add(Sw.Behaviors.button().setVariant(dev.jbaby.shadwick.behaviors.ButtonBehavior.Variant.OUTLINE)));
        add(form);
    }
    private static String receipt(Quote quote) {
        return "Saved budget " + quote.budget.toPlainString() + "; tax " + quote.tax.toPlainString()
                + "%; priority " + quote.priority + "; reference " + quote.reference + ".";
    }
    public static class Quote implements Serializable {
        public BigDecimal budget = new BigDecimal("1250.00");
        public BigDecimal tax = new BigDecimal("19");
        public Integer priority = 5;
        public String reference = "STUDIO-1";
    }
    @Override protected String getTitle() { return "Quote fields"; }
}
