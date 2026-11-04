package dev.jbaby.shadwick.examples;

import dev.jbaby.shadwick.Sw;
import dev.jbaby.shadwick.components.SwSubmitButton;
import dev.jbaby.shadwick.components.form.SwEmailField;
import dev.jbaby.shadwick.components.form.SwNumberField;
import dev.jbaby.shadwick.components.form.SwPasswordField;
import dev.jbaby.shadwick.components.form.SwSearchField;
import dev.jbaby.shadwick.components.form.SwTelField;
import dev.jbaby.shadwick.components.form.SwUrlField;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.form.Button;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.model.CompoundPropertyModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.request.mapper.parameter.PageParameters;
import org.apache.wicket.validation.validator.StringValidator;

import java.io.Serializable;
import java.math.BigDecimal;

/** Everyday native input types with Wicket validation, normal and Ajax submission. */
public class InputPage extends BasePage {

    public InputPage(PageParameters parameters) {
        super(parameters);
        Label saved = new Label("saved", Model.of(""));
        Form<Settings> form = new Form<>("form", new CompoundPropertyModel<>(new Settings())) {
            @Override
            protected void onSubmit() {
                saved.setDefaultModelObject("Saved " + getModelObject().email + "; seats " + getModelObject().seats + ".");
            }
        };
        addFields(form);
        form.add(new Button("save").add(Sw.Behaviors.button()));
        form.add(saved);
        add(form);

        Label ajaxSaved = new Label("saved", Model.of(""));
        ajaxSaved.setOutputMarkupId(true);
        Form<Settings> ajaxForm = new Form<>("ajaxForm", new CompoundPropertyModel<>(new Settings()));
        addFields(ajaxForm);
        ajaxForm.add(SwSubmitButton.onSubmit("save", "Save settings", target -> {
            ajaxSaved.setDefaultModelObject("Saved " + ajaxForm.getModelObject().email + "; seats " + ajaxForm.getModelObject().seats + ".");
            target.add(ajaxSaved);
        }));
        ajaxForm.add(ajaxSaved);
        add(ajaxForm);
    }

    private static void addFields(Form<Settings> form) {
        form.add(new SwEmailField("email").setRequired(true).setPlaceholder("you@example.com"));
        form.add(new SwPasswordField("password").setDescription("Required by default. Never rendered back into HTML."));
        form.add(new SwSearchField("search").setPlaceholder("Search projects")
                .addValidator(StringValidator.maximumLength(40)));
        form.add(new SwTelField("telephone").setDescription("International numbers and punctuation are allowed."));
        form.add(new SwUrlField("website").setPlaceholder("https://example.com"));
        form.add(new SwNumberField<Integer>("seats").setRequired(true).setMin(1).setMax(10).setStep(1)
                .setDescription("From 1 to 10 seats."));
        form.add(new SwNumberField<BigDecimal>("budget").setMin(BigDecimal.ZERO).setMax(new BigDecimal("1000"))
                .setStep(new BigDecimal("0.5")).setDescription("Decimal input; step is a browser hint."));
    }

    public static class Settings implements Serializable {
        public String email;
        public String password;
        public String search;
        public String telephone;
        public String website;
        public Integer seats = 2;
        public BigDecimal budget = new BigDecimal("12.5");
    }

    @Override
    protected String getTitle() { return "Inputs"; }
}
