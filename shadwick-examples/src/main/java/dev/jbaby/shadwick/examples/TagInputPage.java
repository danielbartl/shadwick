package dev.jbaby.shadwick.examples;

import dev.jbaby.shadwick.Sw;
import dev.jbaby.shadwick.components.SwButton;
import dev.jbaby.shadwick.components.SwSubmitButton;
import dev.jbaby.shadwick.components.form.SwTagInput;

import org.apache.wicket.AttributeModifier;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.form.*;
import org.apache.wicket.model.*;
import org.apache.wicket.request.mapper.parameter.PageParameters;
import org.apache.wicket.validation.ValidationError;

import java.util.*;

/** Native and Ajax forms sharing the same comma-separated tag contract. */
public class TagInputPage extends BasePage {

    private int refreshes;

    public TagInputPage(PageParameters parameters) {
        super(parameters);
    }

    @Override
    protected void onInitialize() {
        super.onInitialize();
        addForm("nativeForm", false);
        addForm("ajaxForm", true);
        var readonly =
                new SwTagInput(
                        "readonly",
                        "Read-only tags",
                        Model.ofList(List.of("reviewed", "approved")));
        readonly.addToInput(AttributeModifier.replace("readonly", "readonly"));
        add(readonly);
        var disabled =
                new SwTagInput("disabled", "Disabled tags", Model.ofList(List.of("archived")));
        disabled.setEnabled(false);
        add(disabled);
        add(
                new SwTagInput(
                        "wrapping",
                        "Long and literal tags",
                        Model.ofList(
                                List.of(
                                        "averylongtagwithoutspaces".repeat(5),
                                        "<script>literal text</script>"))));
    }

    private void addForm(String id, boolean ajax) {
        var model = Model.ofList(new ArrayList<>(List.of("wicket", "java")));
        var saved = new Label("saved", Model.of(""));
        saved.setOutputMarkupId(true);
        Form<Void> form =
                new Form<>(id) {

                    @Override
                    protected void onSubmit() {
                        saved.setDefaultModelObject(receipt(model.getObject()));
                    }
                };
        var fields = new WebMarkupContainer("fields");
        fields.setOutputMarkupId(true);
        fields.add(
                AttributeModifier.replace(
                        "disabled", (IModel<String>) () -> fields.isEnabled() ? null : "disabled"));
        fields.add(field(model));
        form.add(fields);
        if (ajax)
            form.add(
                    SwSubmitButton.onSubmit(
                            "save",
                            "Save with Ajax",
                            target -> {
                                saved.setDefaultModelObject(receipt(model.getObject()));
                                target.add(saved);
                            }));
        else form.add(new Button("save").add(Sw.Behaviors.button()));
        form.add(saved);
        form.add(
                SwButton.onClick(
                        "toggle",
                        "Toggle fieldset",
                        target -> {
                            fields.setEnabled(!fields.isEnabled());
                            target.add(fields);
                        }));
        form.add(
                SwButton.onClick(
                        "replace",
                        "Replace field",
                        target -> {
                            var fresh = field(model);
                            fields.replace(fresh);
                            target.add(fresh);
                        }));
        form.add(
                SwButton.onClick(
                        "preset",
                        "Use preset tags",
                        target -> {
                            model.setObject(new ArrayList<>(List.of("release", "ready")));
                            var fresh = field(model);
                            fields.replace(fresh);
                            target.add(fresh);
                        }));
        form.add(
                SwButton.onClick(
                        "refresh",
                        "Refresh status",
                        target -> {
                            saved.setDefaultModelObject("Status refreshed " + ++refreshes);
                            target.add(saved);
                        }));
        add(form);
    }

    private static String receipt(List<String> tags) {
        return "Saved tags: " + String.join(" | ", tags);
    }

    private static SwTagInput field(IModel<List<String>> model) {
        var field =
                new SwTagInput("tags", "Project tags", model)
                        .setRequired(true)
                        .setDescription("Choose up to three freeform tags. Case is preserved.");
        field.addValidator(
                validatable -> {
                    if (validatable.getValue().size() > 3)
                        validatable.error(
                                new ValidationError().setMessage("Choose at most three tags."));
                });
        return field;
    }

    @Override
    protected String getTitle() {
        return "Freeform tags";
    }
}
