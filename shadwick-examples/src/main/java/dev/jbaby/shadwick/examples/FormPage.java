package dev.jbaby.shadwick.examples;

import dev.jbaby.shadwick.Sw;
import dev.jbaby.shadwick.components.SwFormErrors;
import dev.jbaby.shadwick.components.SwSubmitButton;
import dev.jbaby.shadwick.components.form.BaseSwField;
import dev.jbaby.shadwick.components.form.SwPasswordField;
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
    }

    @Override
    protected void onInitialize() {
        super.onInitialize();
        add(normalForm());
        add(ajaxForm());
        add(settingsForm());
        add(signUpForm());
    }

    /** Labels beside their fields while the form is at least 28rem wide; stacked when it is narrower. */
    private Form<Void> settingsForm() {
        Label saved = new Label("saved", Model.of(""));
        Form<Void> form = new Form<>("settingsForm") {
            @Override
            protected void onSubmit() {
                saved.setDefaultModelObject("Settings saved.");
            }
        };
        form.add(Sw.Behaviors.fieldGroup());
        form.add(new SwTextField<>("displayName", "Display name", Model.of("Ada Lovelace"))
                .setDescription("Shown next to your comments.")
                .setRequired(true)
                .setOrientation(BaseSwField.Orientation.RESPONSIVE));
        form.add(new SwTextField<>("workspace", "Workspace", Model.of("Analytical Engines"))
                .setDescription("Everyone you invite joins it.")
                .setOrientation(BaseSwField.Orientation.RESPONSIVE));
        form.add(new SwTextArea<>("signature", "Email signature", Model.of(""))
                .setOrientation(BaseSwField.Orientation.RESPONSIVE));
        form.add(new Button("save").add(Sw.Behaviors.button()));
        form.add(saved);
        return form;
    }

    /**
     * One summary for the form's errors: the rule the form checks as a whole (the two passwords) first, then each
     * field's error as a link to the field, which still shows its own. It takes the focus after a submit.
     */
    private Form<Void> signUpForm() {
        Label created = new Label("created", Model.of(""));
        created.setOutputMarkupId(true);
        Form<Void> form = new Form<>("signUpForm") {
            @Override
            protected void onSubmit() {
                created.setDefaultModelObject("Account created.");
            }
        };
        form.add(new SwFormErrors("errors", form)
                .setShow(SwFormErrors.Show.ALL)
                .setHeadingLevel(3)
                .setAutofocus(true));
        SwTextField<String> email = new SwTextField<String>("email", "Email", Model.of(""))
                .setRequired(true);
        email.addValidator(EmailAddressValidator.getInstance());
        SwPasswordField password = new SwPasswordField("password", "Password", Model.of(""));
        SwPasswordField repeat = new SwPasswordField("repeat", "Repeat password", Model.of(""));
        form.add(email, password, repeat);
        form.add(new PasswordsMatch(password, repeat));
        form.add(new Button("create").add(Sw.Behaviors.button()));
        // The form's onSubmit sets the message after a valid submit; the Ajax button only shows it
        form.add(SwSubmitButton.onSubmit("createAjax", "Create account (Ajax)", target -> target.add(created))
                .setVariant(dev.jbaby.shadwick.behaviors.ButtonBehavior.Variant.OUTLINE));
        form.add(created);
        return form;
    }

    /** An error of the form as a whole, which no single field shows: the two passwords differ. */
    private static final class PasswordsMatch extends org.apache.wicket.markup.html.form.validation.AbstractFormValidator {
        private final SwPasswordField password;
        private final SwPasswordField repeat;

        PasswordsMatch(SwPasswordField password, SwPasswordField repeat) {
            this.password = password;
            this.repeat = repeat;
        }

        @Override
        public org.apache.wicket.markup.html.form.FormComponent<?>[] getDependentFormComponents() {
            return new org.apache.wicket.markup.html.form.FormComponent<?>[] {password.getInput(), repeat.getInput()};
        }

        @Override
        public void validate(Form<?> form) {
            if (!java.util.Objects.equals(password.getInput().getConvertedInput(), repeat.getInput().getConvertedInput())) {
                form.error("The two passwords don't match.");
            }
        }
    }

    private Form<Profile> normalForm() {
        // A normal submit: the page renders again, with each field's error inline
        Label saved = new Label("saved", Model.of(""));
        Form<Profile> form =
                new Form<>("form", new CompoundPropertyModel<>(new Profile())) {

                    @Override
                    protected void onSubmit() {
                        saved.setDefaultModelObject("Saved " + getModelObject() + ".");
                    }
                };
        addFields(form);
        form.add(new Button("save").add(Sw.Behaviors.button()));
        form.add(saved);
        return form;
    }

    private Form<Profile> ajaxForm() {
        // An Ajax submit: the fields show their errors without the handler adding them
        Label ajaxSaved = new Label("saved", Model.of(""));
        ajaxSaved.setOutputMarkupId(true);
        Form<Profile> ajaxForm = new Form<>("ajaxForm", new CompoundPropertyModel<>(new Profile()));
        addFields(ajaxForm);
        ajaxForm.add(
                SwSubmitButton.onSubmit(
                        "save",
                        "Save",
                        target -> {
                            ajaxSaved.setDefaultModelObject(
                                    "Saved " + ajaxForm.getModelObject() + ".");
                            target.add(ajaxSaved);
                        }));
        ajaxForm.add(ajaxSaved);
        return ajaxForm;
    }

    /** Labels from FormPage.properties, models from the form's CompoundPropertyModel. */
    private static void addFields(Form<Profile> form) {
        form.add(name(), email(), bio());
    }

    private static SwTextField<String> name() {
        return new SwTextField<String>("name")
                .setRequired(true)
                .addToInput(AttributeModifier.replace("autocomplete", "name"));
    }

    private static SwTextField<String> email() {
        return new SwTextField<String>("email")
                .setRequired(true)
                .addValidator(EmailAddressValidator.getInstance())
                .setDescription("We never share it.")
                .setPlaceholder("you@example.com")
                .addToInput(AttributeModifier.replace("autocomplete", "email"));
    }

    private static SwTextArea<String> bio() {
        return new SwTextArea<String>("bio")
                .addValidator(StringValidator.maximumLength(160))
                .setDescription("Up to 160 characters.");
    }

    @Override
    protected String getTitle() {
        return "Form";
    }
}
