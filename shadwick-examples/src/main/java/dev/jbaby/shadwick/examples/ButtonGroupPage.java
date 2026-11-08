package dev.jbaby.shadwick.examples;

import dev.jbaby.shadwick.Sw;
import dev.jbaby.shadwick.behaviors.ButtonBehavior;
import dev.jbaby.shadwick.behaviors.ButtonGroupBehavior;
import dev.jbaby.shadwick.behaviors.DropdownMenuItemBehavior;
import dev.jbaby.shadwick.components.*;
import dev.jbaby.shadwick.components.form.SwTextField;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.form.Button;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.markup.html.list.ListItem;
import org.apache.wicket.markup.html.list.ListView;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.request.mapper.parameter.PageParameters;

import java.util.Arrays;
import java.util.List;

/** Connected independent actions and a primary Ajax command with related actions. */
public class ButtonGroupPage extends BasePage {
    private final Model<String> result = Model.of("Choose an action.");
    private final Label receipt = new Label("receipt", result);
    private int actions, revision = 1, submissions;
    public ButtonGroupPage(PageParameters parameters) {
        super(parameters);
        receipt.setOutputMarkupId(true); add(receipt);
        add(SwButtonGroup.of("group", Model.ofList(List.of("Previous", "Locked", "Next")), (item, value) -> {
            item.add(SwButton.onClick("button", value, target -> report(target, value))
                    .setVariant(ButtonBehavior.Variant.OUTLINE).setSize(ButtonBehavior.Size.SMALL).setEnabled(!value.equals("Locked")));
        }).setLabel("Browse records"));
        var vertical = new WebMarkupContainer("vertical");
        vertical.add(Sw.Behaviors.buttonGroup().setLabel("Export format").setOrientation(ButtonGroupBehavior.Orientation.VERTICAL));
        vertical.add(SwButton.onClick("pdf", "PDF", target -> report(target, "Exported PDF")).setVariant(ButtonBehavior.Variant.OUTLINE));
        vertical.add(SwButton.onClick("csv", "CSV", target -> report(target, "Exported CSV")).setVariant(ButtonBehavior.Variant.OUTLINE));
        add(vertical);

        var split = new SwSplitButton("split", (IModel<String>) () -> "Save revision " + revision,
                target -> report(target, "Saved revision " + revision));
        var draft = split.addAction("Save as draft", target -> report(target, "Draft saved"));
        split.addAction("Send for review", target -> report(target, "Sent for review")).setEnabled(false);
        split.addAction("Discard", target -> report(target, "Discarded draft")).setVariant(DropdownMenuItemBehavior.Variant.DESTRUCTIVE);
        add(split);
        add(SwButton.onClick("refresh", "Refresh actions", target -> { revision++; target.add(split); }).setVariant(ButtonBehavior.Variant.OUTLINE));
        Model<Boolean> available = Model.of(true);
        add(SwButton.onClick("toggle", "Toggle draft action", target -> {
            available.setObject(!available.getObject()); draft.setVisible(available.getObject()); target.add(split);
        }).setVariant(ButtonBehavior.Variant.OUTLINE));
        add(new SwSplitButton("disabled", "Managed action", target -> report(target, "Unexpected managed action"))
                .setEnabled(false));
        var disabled = (SwSplitButton) get("disabled"); disabled.addAction("Managed alternative", target -> {});
        add(new SwSplitButton("empty", "Primary only", target -> report(target, "Primary only")));

        add(new ListView<ButtonBehavior.Variant>("variants", Arrays.asList(ButtonBehavior.Variant.values())) {
            @Override protected void populateItem(ListItem<ButtonBehavior.Variant> item) {
                String name = item.getModelObject().name().toLowerCase(java.util.Locale.ROOT);
                var action = new SwSplitButton("example", name, target -> report(target, name)).setVariant(item.getModelObject());
                action.addAction("Alternative " + name, target -> report(target, "Alternative " + name)); item.add(action);
            }
        });
        add(new ListView<ButtonBehavior.Size>("sizes", List.of(ButtonBehavior.Size.SMALL, ButtonBehavior.Size.DEFAULT, ButtonBehavior.Size.LARGE)) {
            @Override protected void populateItem(ListItem<ButtonBehavior.Size> item) {
                var action = new SwSplitButton("example", item.getModelObject().name().toLowerCase(java.util.Locale.ROOT), target -> {})
                        .setSize(item.getModelObject()).setVariant(ButtonBehavior.Variant.OUTLINE);
                action.addAction("Alternative", target -> report(target, "Size example")); item.add(action);
            }
        });

        Model<String> title = Model.of(""); Model<String> saved = Model.of("No form submission yet.");
        var savedLabel = new Label("saved", saved); savedLabel.setOutputMarkupId(true);
        var form = new Form<Void>("form") {
            @Override protected void onSubmit() { submissions++; saved.setObject("Saved " + title.getObject() + " (submission " + submissions + ")."); }
        };
        form.add(new SwTextField<>("title", "Quote title", title).setRequired(true));
        var formActions = new WebMarkupContainer("formActions"); formActions.add(Sw.Behaviors.buttonGroup().setLabel("Save quote"));
        formActions.add(new Button("nativeSave").add(Sw.Behaviors.button(ButtonBehavior.Variant.OUTLINE).setSize(ButtonBehavior.Size.SMALL)));
        formActions.add(SwSubmitButton.onSubmit("ajaxSave", "Save (Ajax)", target -> target.add(savedLabel)).setVariant(ButtonBehavior.Variant.OUTLINE).setSize(ButtonBehavior.Size.SMALL));
        formActions.add(new WebMarkupContainer("reset").add(Sw.Behaviors.button(ButtonBehavior.Variant.OUTLINE).setSize(ButtonBehavior.Size.SMALL)));
        form.add(formActions, savedLabel);
        var commands = new SwSplitButton("commands", "Preview", target -> report(target, "Preview opened"))
                .setVariant(ButtonBehavior.Variant.SECONDARY);
        commands.addAction("Preview draft", target -> report(target, "Draft preview opened")); form.add(commands);
        add(form);
    }
    private void report(AjaxRequestTarget target, String message) {
        result.setObject(message + " (action " + ++actions + ")."); target.add(receipt);
    }
    @Override protected String getTitle() { return "Button groups & split buttons"; }
}
