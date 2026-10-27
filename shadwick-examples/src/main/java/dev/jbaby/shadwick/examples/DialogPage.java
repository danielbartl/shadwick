package dev.jbaby.shadwick.examples;

import dev.jbaby.shadwick.behaviors.ButtonBehavior;
import dev.jbaby.shadwick.components.SwButton;
import dev.jbaby.shadwick.components.SwDialog;
import dev.jbaby.shadwick.components.form.SwTextArea;
import dev.jbaby.shadwick.components.form.SwTextField;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.panel.Fragment;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.model.PropertyModel;
import org.apache.wicket.request.mapper.parameter.PageParameters;

public class DialogPage extends BasePage {

    private final Profile profile = new Profile("Ada Lovelace", "ada@example.com", "Wrote the first program.");

    public DialogPage(PageParameters parameters) {
        super(parameters);
        Label status = new Label("status", Model.of(""));
        status.setOutputMarkupId(true);
        add(status);

        // Opened in the browser by its trigger button, without waiting for the server
        SwDialog about = new SwDialog("about", "About Shadwick", "About Shadwick");
        about.setDescription("shadcn/ui-style components for Apache Wicket.");
        about.setContent(new Label(SwDialog.CONTENT_ID, "Shadwick renders shadcn's markup on the server, styled by one "
                + "stylesheet per skin. Close this with the ×, the Escape key or a click outside."));
        about.setFooterVisible(false);
        add(about);

        // Opened over Ajax, from any handler
        SwDialog delete = new SwDialog("delete", "Delete the project?") {
            @Override
            protected void onConfirm(AjaxRequestTarget target) {
                status.setDefaultModelObject("The project was deleted.");
                target.add(status);
            }
        };
        delete.setDescription("This cannot be undone. Every file in it goes with it.");
        delete.setConfirmLabel(Model.of("Delete")).setConfirmVariant(ButtonBehavior.Variant.DESTRUCTIVE);
        add(delete);
        add(SwButton.onClick("openDelete", "Delete project", delete::open).setVariant(ButtonBehavior.Variant.DESTRUCTIVE));

        // With a form: it stays open while a field has an error
        Label summary = new Label("summary", (IModel<String>) profile::toString);
        summary.setOutputMarkupId(true);
        add(summary);
        SwTextField<String> name = new SwTextField<String>("name", "Name", new PropertyModel<>(profile, "name")).setRequired(true);
        SwDialog edit = new SwDialog("edit", "Edit profile", "Edit profile") {
            @Override
            protected void onConfirm(AjaxRequestTarget target) {
                // An error found here keeps the dialog open too
                if ("admin".equalsIgnoreCase(profile.getName())) {
                    name.getInput().error("That name is reserved.");
                    return;
                }
                status.setDefaultModelObject("Saved " + profile + ".");
                target.add(status, summary);
            }
        };
        edit.setDescription("Make changes to your profile here. Click save when you're done.");
        Fragment fields = new Fragment(SwDialog.CONTENT_ID, "profileFields", this);
        fields.add(name);
        fields.add(new SwTextField<String>("email", "Email", new PropertyModel<>(profile, "email")).setRequired(true));
        fields.add(new SwTextArea<String>("bio", "Bio", new PropertyModel<>(profile, "bio")));
        edit.setContent(fields);
        edit.setConfirmLabel(Model.of("Save changes"));
        add(edit);
    }

    @Override
    protected String getTitle() {
        return "Dialog";
    }
}
