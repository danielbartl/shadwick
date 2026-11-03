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

/** Boolean and single-choice fields with normal submit, Ajax validation and immediate selection callbacks. */
public class ChoicePage extends BasePage {

    public ChoicePage(PageParameters parameters) {
        super(parameters);
        Label saved = new Label("saved", Model.of(""));
        Form<Preferences> form = new Form<>("form", new CompoundPropertyModel<>(new Preferences())) {
            @Override
            protected void onSubmit() {
                saved.setDefaultModelObject("Saved: " + getModelObject());
            }
        };
        addFields(form);
        form.add(new Button("save").add(Sw.Behaviors.button()));
        form.add(saved);
        add(form);

        Label ajaxSaved = new Label("saved", Model.of(""));
        ajaxSaved.setOutputMarkupId(true);
        Form<Preferences> ajaxForm = new Form<>("ajaxForm", new CompoundPropertyModel<>(new Preferences()));
        addFields(ajaxForm);
        ajaxForm.add(SwSubmitButton.onSubmit("save", "Save preferences", target -> {
            ajaxSaved.setDefaultModelObject("Saved: " + ajaxForm.getModelObject());
            target.add(ajaxSaved);
        }));
        ajaxForm.add(ajaxSaved);
        add(ajaxForm);

        Model<Boolean> notifications = Model.of(false);
        Model<String> frequency = Model.of("Weekly");
        Label current = new Label("current", (IModel<String>) () -> "Notifications "
                + (notifications.getObject() ? "on" : "off") + "; frequency " + frequency.getObject() + ".");
        current.setOutputMarkupId(true);
        Form<Void> liveForm = new Form<>("liveForm");
        liveForm.add(new SwSwitch("notifications", "Instant notifications", notifications)
                .setDescription("Changes update the server immediately.")
                .add(new AjaxFormComponentUpdatingBehavior("change") {
                    @Override
                    protected void onUpdate(AjaxRequestTarget target) { target.add(current); }
                }));
        liveForm.add(new SwRadioChoice<>("frequency", "Update frequency", frequency, Model.ofList(List.of("Daily", "Weekly", "Monthly")))
                .setInline(true).add(new AjaxFormChoiceComponentUpdatingBehavior() {
                    @Override
                    protected void onUpdate(AjaxRequestTarget target) { target.add(current); }
                }));
        liveForm.add(current);
        add(liveForm);

        add(new SwCheckBox("disabledCheckbox", "Managed by your workspace", Model.of(true))
                .setDescription("A disabled checkbox keeps its value.").setEnabled(false));
        add(new SwSwitch("smallSwitch", "Compact switch", Model.of(true)).setSize(SwitchBehavior.Size.SMALL));
        add(new SwSwitch("disabledSwitch", "Administrator setting", Model.of(false)).setEnabled(false));
        add(new SwRadioChoice<>("disabledRadio", "Managed delivery", Model.of("Email"), Model.ofList(List.of("Email", "SMS")))
                .setInline(true).setEnabled(false));
    }

    private static void addFields(Form<Preferences> form) {
        form.add(new SwCheckBox("accepted").setDescription("Required for this example; checked by the server.")
                .addValidator(value -> {
                    // Wicket CheckBox.setRequired(true) allows false; consent needs a validator.
                    if (!Boolean.TRUE.equals(value.getValue())) {
                        value.error(new ValidationError().setMessage("Accept the terms to continue."));
                    }
                }));
        form.add(new SwSwitch("notifications").setDescription("Unchecked is a valid preference."));
        form.add(new SwRadioChoice<String>("contact", Model.ofList(List.of("Email", "SMS", "Phone")))
                .setRequired(true).setDescription("Choose one way to hear from us."));
    }

    public static class Preferences implements Serializable {
        public boolean accepted;
        public boolean notifications;
        public String contact;

        @Override
        public String toString() {
            return "terms accepted; notifications " + (notifications ? "on" : "off") + "; contact " + contact + ".";
        }
    }

    @Override
    protected String getTitle() { return "Choices"; }
}
