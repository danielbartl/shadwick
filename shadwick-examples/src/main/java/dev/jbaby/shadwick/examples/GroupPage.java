package dev.jbaby.shadwick.examples;

import dev.jbaby.shadwick.Sw;
import dev.jbaby.shadwick.behaviors.ToggleBehavior;
import dev.jbaby.shadwick.behaviors.ToggleGroupBehavior;
import dev.jbaby.shadwick.behaviors.InputGroupAddonBehavior;
import dev.jbaby.shadwick.components.SwButton;
import dev.jbaby.shadwick.components.SwIcon;
import dev.jbaby.shadwick.components.SwSubmitButton;
import dev.jbaby.shadwick.components.form.Html5TextField;
import dev.jbaby.shadwick.components.form.SwNumberField;
import dev.jbaby.shadwick.components.form.SwTextArea;
import dev.jbaby.shadwick.components.form.SwTextField;
import dev.jbaby.shadwick.components.form.SwToggle;
import dev.jbaby.shadwick.components.form.SwToggleChoice;
import dev.jbaby.shadwick.components.form.SwToggleMultipleChoice;
import org.apache.wicket.AttributeModifier;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.form.AjaxFormChoiceComponentUpdatingBehavior;
import org.apache.wicket.ajax.form.AjaxFormComponentUpdatingBehavior;
import org.apache.wicket.ajax.markup.html.AjaxLink;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.form.Button;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.markup.html.form.TextField;
import org.apache.wicket.model.CompoundPropertyModel;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.request.mapper.parameter.PageParameters;
import org.apache.wicket.validation.validator.PatternValidator;
import org.apache.wicket.validation.validator.StringValidator;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

/** Native toggle groups and decorated fields, with normal/Ajax submits and immediate updates. */
public class GroupPage extends BasePage {
    public GroupPage(PageParameters parameters) {
        super(parameters);
        Label saved = new Label("saved", Model.of(""));
        Form<Settings> form = new Form<>("form", new CompoundPropertyModel<>(new Settings())) {
            @Override
            protected void onSubmit() { saved.setDefaultModelObject("Saved: " + getModelObject()); }
        };
        addFields(form);
        form.add(new Button("save").add(Sw.Behaviors.button()));
        form.add(saved);
        add(form);

        Label ajaxSaved = new Label("saved", Model.of(""));
        ajaxSaved.setOutputMarkupId(true);
        Form<Settings> ajaxForm = new Form<>("ajaxForm", new CompoundPropertyModel<>(new Settings()));
        addFields(ajaxForm);
        ajaxForm.add(SwSubmitButton.onSubmit("save", "Save composition", target -> {
            ajaxSaved.setDefaultModelObject("Saved: " + ajaxForm.getModelObject());
            target.add(ajaxSaved);
        }));
        ajaxForm.add(ajaxSaved);
        add(ajaxForm);

        Settings live = new Settings();
        live.alignment = "Left";
        Label current = new Label("current", (IModel<String>) () -> "Live: highlight " + (live.highlight ? "on" : "off")
                + "; alignment " + live.alignment + "; formats " + (live.formats.isEmpty() ? "none" : String.join(", ", live.formats)) + ".");
        current.setOutputMarkupId(true);
        Form<Settings> liveForm = new Form<>("liveForm", new CompoundPropertyModel<>(live));
        liveForm.add(new SwToggle("highlight").setIcon("pencil").setIconOnly(true).setSize(ToggleBehavior.Size.SMALL)
                .setVariant(ToggleBehavior.Variant.OUTLINE).setDescription("Press Space to toggle emphasis.")
                .add(new AjaxFormComponentUpdatingBehavior("change") {
                    @Override protected void onUpdate(AjaxRequestTarget target) { target.add(current); }
                }));
        liveForm.add(new SwToggleChoice<>("alignment", Model.ofList(List.of("Left", "Center", "Right")))
                .setVariant(ToggleBehavior.Variant.OUTLINE).setSpacing(0).setOrientation(ToggleGroupBehavior.Orientation.VERTICAL)
                .add(new AjaxFormChoiceComponentUpdatingBehavior() {
                    @Override protected void onUpdate(AjaxRequestTarget target) { target.add(current); }
                }));
        liveForm.add(new SwToggleMultipleChoice<>("formats", Model.ofList(List.of("Bold", "Italic", "Underline")))
                .setSize(ToggleBehavior.Size.LARGE).setSpacing(1).add(new AjaxFormChoiceComponentUpdatingBehavior() {
                    @Override protected void onUpdate(AjaxRequestTarget target) { target.add(current); }
                }));
        liveForm.add(current);
        add(liveForm);

        addSearchGroup();
        var managed = new SwToggle("managedToggle", "Managed emphasis", Model.of(true)).setVariant(ToggleBehavior.Variant.OUTLINE);
        managed.setEnabled(false);
        add(managed);
        var weight = new SwNumberField<>("managedWeight", "Managed weight", Model.of(5)).setSuffix("kg");
        weight.setEnabled(false);
        add(weight);
    }

    private void addSearchGroup() {
        TextField<String> query = new Html5TextField<>("query", Model.of("Components"), String.class, "search");
        query.setOutputMarkupId(true);
        query.add(Sw.Behaviors.inputGroupInput());
        Label label = new Label("searchLabel", "Search library");
        label.setOutputMarkupId(true);
        label.add(AttributeModifier.replace("for", (IModel<String>) query::getMarkupId));
        add(label);
        WebMarkupContainer group = new WebMarkupContainer("searchGroup");
        group.setOutputMarkupId(true);
        group.add(Sw.Behaviors.inputGroup());
        group.add(AttributeModifier.replace("aria-label", "Search tools"));
        group.add(query);
        WebMarkupContainer prefix = new WebMarkupContainer("searchPrefix");
        prefix.add(Sw.Behaviors.inputGroupAddon());
        prefix.add(new SwIcon("searchIcon", "search"));
        group.add(prefix);
        WebMarkupContainer suffix = new WebMarkupContainer("searchActions");
        suffix.add(Sw.Behaviors.inputGroupAddon().setAlign(InputGroupAddonBehavior.Align.INLINE_END));
        suffix.add(new AjaxLink<Void>("clearSearch") {
            @Override
            public void onClick(AjaxRequestTarget target) {
                query.setModelObject("");
                query.clearInput();
                target.add(query);
            }
        }.add(Sw.Behaviors.inputGroupButton()).add(AttributeModifier.replace("aria-label", "Clear search")));
        group.add(suffix);
        add(group);
        add(SwButton.onClick("refreshSearch", "Refresh search group", target -> {
            query.setModelObject("Refreshed components");
            query.clearInput();
            target.add(group);
        }));
    }

    private static void addFields(Form<Settings> form) {
        form.add(new SwToggle("highlight").setIcon("pencil").setVariant(ToggleBehavior.Variant.OUTLINE));
        form.add(new SwToggleChoice<>("alignment", Model.ofList(List.of("Left", "Center", "Right")))
                .setRequired(true).setSpacing(0).setVariant(ToggleBehavior.Variant.OUTLINE).setDescription("Choose one alignment."));
        form.add(new SwToggleMultipleChoice<>("formats", Model.ofList(List.of("Bold", "Italic", "Underline")))
                .setRequired(true).setSize(ToggleBehavior.Size.SMALL).setDescription("Choose at least one style."));
        form.add(new SwTextField<String>("workspace").setRequired(true).setPrefix("https://").setSuffix(".example")
                .addValidator(new PatternValidator("[a-z][a-z0-9-]{2,19}"))
                .setDescription("3–20 lowercase letters, digits or hyphens, starting with a letter."));
        form.add(new SwNumberField<Integer>("weight").setRequired(true).setMin(1).setMax(100).setStep(1).setSuffix("kg")
                .setDescription("From 1 to 100 kg."));
        form.add(new SwTextField<String>("project").setSuggestions(List.of("Shadwick", "Atlas", "Nimbus"))
                .setDescription("Native suggestions; you can also enter another project."));
        form.add(new SwTextArea<String>("notes").setPrefix("Draft").setSuffix("Markdown supported")
                .addValidator(StringValidator.maximumLength(200)));
    }

    public static class Settings implements Serializable {
        public boolean highlight;
        public String alignment;
        public List<String> formats = new ArrayList<>();
        public String workspace;
        public Integer weight = 5;
        public String project = "Atlas";
        public String notes;

        @Override
        public String toString() {
            return "highlight " + (highlight ? "on" : "off") + "; alignment " + alignment + "; formats " + String.join(", ", formats)
                    + "; workspace " + workspace + "; weight " + weight + " kg; project " + project + ".";
        }
    }

    @Override protected String getTitle() { return "Toggles & groups"; }
}
