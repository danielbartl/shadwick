package dev.example;

import dev.jbaby.shadwick.Sw;
import dev.jbaby.shadwick.behaviors.ButtonBehavior;
import dev.jbaby.shadwick.components.SwSubmitButton;
import dev.jbaby.shadwick.components.form.SwTextField;

import org.apache.wicket.markup.html.WebPage;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.form.Button;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;

public class QuickStartPage extends WebPage {

    private final IModel<String> name = Model.of("");
    private final IModel<String> receipt = Model.of("No changes saved yet.");

    @Override
    protected void onInitialize() {
        super.onInitialize();
        Label status = status();
        add(status, profileForm(status));
    }

    private Label status() {
        Label status = new Label("status", receipt);
        status.setOutputMarkupId(true);
        return status;
    }

    private Form<Void> profileForm(Label status) {
        Form<Void> form =
                new Form<>("form") {

                    @Override
                    protected void onSubmit() {
                        saveProfile();
                    }
                };
        form.add(name(), saveButton(status));
        form.add(new Button("saveNative").add(Sw.Behaviors.button(ButtonBehavior.Variant.OUTLINE)));
        return form;
    }

    private void saveProfile() {
        // Replace this demo result with an authorized application service call.
        receipt.setObject("Saved profile for " + name.getObject() + ".");
    }

    private SwTextField<String> name() {
        return new SwTextField<>("name", "Your name", name).setRequired(true);
    }

    private SwSubmitButton saveButton(Label status) {
        return SwSubmitButton.onSubmit(
                        "save",
                        "Save with Ajax",
                        target -> target.add(status),
                        target -> target.add(status))
                .setBusyIndicator(true);
    }
}
