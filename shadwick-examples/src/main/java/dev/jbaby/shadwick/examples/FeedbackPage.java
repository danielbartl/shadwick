package dev.jbaby.shadwick.examples;

import dev.jbaby.shadwick.Sw;
import dev.jbaby.shadwick.components.*;
import dev.jbaby.shadwick.components.form.SwTextField;

import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.markup.html.link.Link;
import org.apache.wicket.model.Model;
import org.apache.wicket.request.mapper.parameter.PageParameters;

/** Form feedback is fenced into a panel; messages outside the form appear as toasts. */
public class FeedbackPage extends BasePage {

    public FeedbackPage(PageParameters parameters) {
        super(parameters);
    }

    @Override
    protected void onInitialize() {
        super.onInitialize();
        add(Sw.Behaviors.feedbackToasts());
        addProjectForm();
        addFeedbackActions();
    }

    private void addProjectForm() {
        Form<Void> form = new Form<>("form");
        add(form);
        form.add(new SwFeedbackPanel("feedback", form));
        form.add(new SwTextField<>("name", "Project name", Model.of("")).setRequired(true));
        form.add(
                SwSubmitButton.onSubmit(
                        "save",
                        "Save project",
                        target -> form.success("Project saved."),
                        target -> form.error("Please fix the errors below.")));
    }

    private void addFeedbackActions() {
        add(
                SwButton.onClick(
                        "success",
                        "Show success",
                        target -> success("Your changes have been saved.")));
        add(
                SwButton.onClick(
                        "warning",
                        "Show warning",
                        target -> warn("Review the project settings before continuing.")));
        add(
                SwButton.onClick(
                        "error", "Show error", target -> error("The project could not be saved.")));
        add(
                SwButton.onClick(
                        "literal",
                        "Show literal text",
                        target -> info("<strong>This is plain text.</strong>")));
        add(
                new Link<Void>("fullPage") {

                    @Override
                    public void onClick() {
                        getSession().success("Welcome back.");
                        setResponsePage(FeedbackPage.class);
                    }
                }.add(Sw.Behaviors.button()));
    }

    @Override
    protected String getTitle() {
        return "Feedback";
    }
}
