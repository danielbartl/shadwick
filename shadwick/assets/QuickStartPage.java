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

    public QuickStartPage() {
        Label status = new Label("status", receipt);
        status.setOutputMarkupId(true);
        add(status);

        Form<Void> form = new Form<>("form") {
            @Override
            protected void onSubmit() {
                // Replace this demo result with an authorized application service call.
                receipt.setObject("Saved profile for " + name.getObject() + ".");
            }
        };
        add(form);
        form.add(new SwTextField<>("name", "Your name", name).setRequired(true));
        form.add(SwSubmitButton.onSubmit("save", "Save with Ajax",
                target -> target.add(status), target -> target.add(status))
                .setBusyIndicator(true));
        form.add(new Button("saveNative")
                .add(Sw.Behaviors.button(ButtonBehavior.Variant.OUTLINE)));
    }
}
