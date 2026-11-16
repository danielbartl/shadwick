package dev.jbaby.shadwick.examples;

import dev.jbaby.shadwick.Sw;
import dev.jbaby.shadwick.behaviors.SwitchBehavior;
import dev.jbaby.shadwick.components.SwSubmitButton;
import dev.jbaby.shadwick.components.form.SwCheckBox;
import dev.jbaby.shadwick.components.form.SwRadioChoice;
import dev.jbaby.shadwick.components.form.SwSwitch;

import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.form.AjaxFormChoiceComponentUpdatingBehavior;
import org.apache.wicket.ajax.form.AjaxFormComponentUpdatingBehavior;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.form.Button;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.model.CompoundPropertyModel;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.request.mapper.parameter.PageParameters;
import org.apache.wicket.validation.ValidationError;

import java.io.Serializable;
import java.util.List;

/**
 * Boolean and single-choice fields with normal submit, Ajax validation and immediate selection
 * callbacks.
 */
public class ChoicePage extends BasePage {

    public ChoicePage(PageParameters parameters) {
        super(parameters);
    }

    @Override
    protected void onInitialize() {
        super.onInitialize();
        add(normalForm());
        add(ajaxForm());
        add(liveForm());
        add(mixedForm());
        addFieldVariants();
    }

    private Form<Preferences> normalForm() {
        Label saved = new Label("saved", Model.of(""));
        Form<Preferences> form =
                new Form<>("form", new CompoundPropertyModel<>(new Preferences())) {

                    @Override
                    protected void onSubmit() {
                        saved.setDefaultModelObject("Saved: " + getModelObject());
                    }
                };
        addFields(form);
        form.add(new Button("save").add(Sw.Behaviors.button()));
        form.add(saved);
        return form;
    }

    private Form<Preferences> ajaxForm() {
        Label ajaxSaved = new Label("saved", Model.of(""));
        ajaxSaved.setOutputMarkupId(true);
        Form<Preferences> ajaxForm =
                new Form<>("ajaxForm", new CompoundPropertyModel<>(new Preferences()));
        addFields(ajaxForm);
        ajaxForm.add(
                SwSubmitButton.onSubmit(
                        "save",
                        "Save preferences",
                        target -> {
                            ajaxSaved.setDefaultModelObject("Saved: " + ajaxForm.getModelObject());
                            target.add(ajaxSaved);
                        }));
        ajaxForm.add(ajaxSaved);
        return ajaxForm;
    }

    private Form<Void> liveForm() {
        Model<Boolean> notifications = Model.of(false);
        Model<String> frequency = Model.of("Weekly");
        Label current =
                new Label(
                        "current",
                        (IModel<String>)
                                () ->
                                        "Notifications "
                                                + (notifications.getObject() ? "on" : "off")
                                                + "; frequency "
                                                + frequency.getObject()
                                                + ".");
        current.setOutputMarkupId(true);
        Form<Void> liveForm = new Form<>("liveForm");
        liveForm.add(
                new SwSwitch("notifications", "Instant notifications", notifications)
                        .setDescription("Changes update the server immediately.")
                        .add(
                                new AjaxFormComponentUpdatingBehavior("change") {

                                    @Override
                                    protected void onUpdate(AjaxRequestTarget target) {
                                        target.add(current);
                                    }
                                }));
        liveForm.add(
                new SwRadioChoice<>(
                                "frequency",
                                "Update frequency",
                                frequency,
                                Model.ofList(List.of("Daily", "Weekly", "Monthly")))
                        .setInline(true)
                        .add(
                                new AjaxFormChoiceComponentUpdatingBehavior() {

                                    @Override
                                    protected void onUpdate(AjaxRequestTarget target) {
                                        target.add(current);
                                    }
                                }));
        liveForm.add(current);
        return liveForm;
    }

    /**
     * "All projects" is checked when every project is, mixed (a minus) when only some are, and checks or clears them
     * all. The mixed state is presentation the server recomputes on each update; the box itself submits a boolean.
     */
    private Form<Void> mixedForm() {
        List<Model<Boolean>> projects = List.of(Model.of(true), Model.of(false), Model.of(true));
        List<String> names = List.of("Fern & Field", "Forma Architects", "Sunday Coffee");
        Form<Void> form = new Form<>("mixedForm");
        SwCheckBox all = new SwCheckBox("allProjects", "All projects", allOf(projects));
        all.setIndeterminate((IModel<Boolean>) () -> projects.stream().anyMatch(Model::getObject)
                && !projects.stream().allMatch(Model::getObject));
        all.setOutputMarkupId(true);
        Label summary = new Label("mixedSummary", (IModel<String>) () ->
                projects.stream().filter(Model::getObject).count() + " of " + projects.size() + " projects selected.");
        summary.setOutputMarkupId(true);
        List<SwCheckBox> boxes = new java.util.ArrayList<>();
        for (int i = 0; i < projects.size(); i++) {
            SwCheckBox box = new SwCheckBox("project" + i, names.get(i), projects.get(i));
            box.setOutputMarkupId(true);
            box.add(new AjaxFormComponentUpdatingBehavior("change") {
                @Override
                protected void onUpdate(AjaxRequestTarget target) {
                    target.add(all, summary);
                }
            });
            boxes.add(box);
            form.add(box);
        }
        all.add(new AjaxFormComponentUpdatingBehavior("change") {
            @Override
            protected void onUpdate(AjaxRequestTarget target) {
                boxes.forEach(target::add);
                target.add(all, summary);
            }
        });
        form.add(all, summary);
        return form;
    }

    /** Checked when every project is; setting it checks or clears them all. */
    private static IModel<Boolean> allOf(List<Model<Boolean>> projects) {
        return new IModel<>() {
            @Override
            public Boolean getObject() {
                return projects.stream().allMatch(Model::getObject);
            }

            @Override
            public void setObject(Boolean value) {
                projects.forEach(project -> project.setObject(Boolean.TRUE.equals(value)));
            }
        };
    }

    private void addFieldVariants() {
        add(
                new SwCheckBox("disabledCheckbox", "Managed by your workspace", Model.of(true))
                        .setDescription("A disabled checkbox keeps its value.")
                        .setEnabled(false));
        add(
                new SwSwitch("smallSwitch", "Compact switch", Model.of(true))
                        .setSize(SwitchBehavior.Size.SMALL));
        add(
                new SwSwitch("disabledSwitch", "Administrator setting", Model.of(false))
                        .setEnabled(false));
        add(
                new SwRadioChoice<>(
                                "disabledRadio",
                                "Managed delivery",
                                Model.of("Email"),
                                Model.ofList(List.of("Email", "SMS")))
                        .setInline(true)
                        .setEnabled(false));
    }

    private static void addFields(Form<Preferences> form) {
        form.add(accepted(), notifications(), contact());
    }

    private static SwCheckBox accepted() {
        return new SwCheckBox("accepted")
                .setDescription("Required for this example; checked by the server.")
                .addValidator(
                        value -> {
                            // Wicket CheckBox.setRequired(true) allows false; consent needs a
                            // validator.
                            if (!Boolean.TRUE.equals(value.getValue())) {
                                value.error(
                                        new ValidationError()
                                                .setMessage("Accept the terms to continue."));
                            }
                        });
    }

    private static SwSwitch notifications() {
        return new SwSwitch("notifications").setDescription("Unchecked is a valid preference.");
    }

    private static SwRadioChoice<String> contact() {
        return new SwRadioChoice<String>("contact", Model.ofList(List.of("Email", "SMS", "Phone")))
                .setRequired(true)
                .setDescription("Choose one way to hear from us.");
    }

    public static class Preferences implements Serializable {

        public boolean accepted;
        public boolean notifications;
        public String contact;

        @Override
        public String toString() {
            return "terms accepted; notifications "
                    + (notifications ? "on" : "off")
                    + "; contact "
                    + contact
                    + ".";
        }
    }

    @Override
    protected String getTitle() {
        return "Choices";
    }
}
