package dev.jbaby.shadwick.examples;

import dev.jbaby.shadwick.Sw;
import dev.jbaby.shadwick.components.SwSubmitButton;
import dev.jbaby.shadwick.components.form.SwTextArea;
import dev.jbaby.shadwick.components.form.SwTextField;
import org.apache.wicket.AttributeModifier;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.form.Button;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.model.CompoundPropertyModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.request.mapper.parameter.PageParameters;
import org.apache.wicket.validation.validator.EmailAddressValidator;
import org.apache.wicket.validation.validator.StringValidator;

public class FormPage extends BasePage {

    public FormPage(PageParameters parameters) {
        super(parameters);

        // A normal submit: the page renders again, with each field's error inline
        Label saved = new Label("saved", Model.of(""));
        Form<Profile> form = new Form<>("form", new CompoundPropertyModel<>(new Profile())) {
            @Override
            protected void onSubmit() {
                saved.setDefaultModelObject("Saved " + getModelObject() + ".");
            }
        };
        addFields(form);
        form.add(new Button("save").add(Sw.Behaviors.button()));
        form.add(saved);
        add(form);

        // An Ajax submit: the fields show their errors without the handler adding them
        Label ajaxSaved = new Label("saved", Model.of(""));
        ajaxSaved.setOutputMarkupId(true);
        Form<Profile> ajaxForm = new Form<>("ajaxForm", new CompoundPropertyModel<>(new Profile()));
        addFields(ajaxForm);
        ajaxForm.add(SwSubmitButton.onSubmit("save", "Save", target -> {
            ajaxSaved.setDefaultModelObject("Saved " + ajaxForm.getModelObject() + ".");
            target.add(ajaxSaved);
        }));
        ajaxForm.add(ajaxSaved);
        add(ajaxForm);
    }

    /** Labels from FormPage.properties, models from the form's CompoundPropertyModel. */
    private static void addFields(Form<Profile> form) {
        form.add(new SwTextField<String>("name").setRequired(true)
                .addToInput(AttributeModifier.replace("autocomplete", "name")));
        form.add(new SwTextField<String>("email").setRequired(true).addValidator(EmailAddressValidator.getInstance())
                .setDescription("We never share it.").setPlaceholder("you@example.com")
                .addToInput(AttributeModifier.replace("autocomplete", "email")));
        form.add(new SwTextArea<String>("bio").addValidator(StringValidator.maximumLength(160))
                .setDescription("Up to 160 characters."));
    }

    @Override
    protected String getTitle() {
        return "Form";
    }
}
