package dev.jbaby.shadwick.examples;

import dev.jbaby.shadwick.behaviors.ButtonBehavior;
import dev.jbaby.shadwick.components.SwButton;
import dev.jbaby.shadwick.components.SwConfirmDialog;
import dev.jbaby.shadwick.components.SwDialog;
import dev.jbaby.shadwick.components.SwSheet;
import dev.jbaby.shadwick.components.form.SwTextArea;
import dev.jbaby.shadwick.components.form.SwTextField;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.list.ListItem;
import org.apache.wicket.markup.html.list.ListView;
import org.apache.wicket.markup.html.panel.Fragment;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.request.mapper.parameter.PageParameters;

import java.util.stream.IntStream;

/** Confirmations and four sheet edges, including a confirmation opened over a sheet. */
public class OverlayPage extends BasePage {
    private final Model<Integer> actions = Model.of(0);

    public OverlayPage(PageParameters parameters) {
        super(parameters);
        Label status = new Label("status", Model.of("No action confirmed."));
        status.setOutputMarkupId(true);
        Label count = new Label("count", (IModel<String>) () -> "Confirmed actions: " + actions.getObject());
        count.setOutputMarkupId(true);
        add(status, count);

        SwConfirmDialog confirm = new SwConfirmDialog("confirm");
        add(confirm);
        add(SwButton.onClick("delete", "Delete draft", target -> confirm.setConfirmLabel("Delete draft")
                .setConfirmVariant(ButtonBehavior.Variant.DESTRUCTIVE)
                .ask(target, "Delete this draft?", "This demo only records your answer. No file will be deleted.", t -> {
                    record(status, count, "Draft deletion confirmed.", t);
                })).setVariant(ButtonBehavior.Variant.DESTRUCTIVE));
        add(SwButton.onClick("archive", "Archive draft", target -> confirm.setConfirmLabel("Archive")
                .setConfirmVariant(ButtonBehavior.Variant.DEFAULT)
                .ask(target, "Archive this draft?", "You can bring it back later.", t -> {
                    record(status, count, "Draft archived.", t);
                })).setVariant(ButtonBehavior.Variant.OUTLINE));
        add(SwButton.onClick("chain", "Two-step confirmation", target -> confirm.setConfirmLabel("Continue")
                .setConfirmVariant(ButtonBehavior.Variant.DEFAULT)
                .ask(target, "Continue to the final question?", "The first answer opens another question.", t ->
                        confirm.setConfirmLabel("Confirm final action").ask(t, "Confirm the final action?",
                                "This needs its own answer, even after a double-click.", next ->
                                        record(status, count, "Final action confirmed.", next)))));

        Model<String> project = Model.of("Shadwick");
        SwTextField<String> name = new SwTextField<String>("name", "Project name", project).setRequired(true);
        SwSheet edit = new SwSheet("edit", "Project settings") {
            @Override protected void onConfirm(AjaxRequestTarget target) {
                if ("admin".equalsIgnoreCase(project.getObject())) {
                    name.getInput().error("That project name is reserved.");
                    return;
                }
                record(status, count, "Saved project: " + project.getObject() + ".", target);
            }
        };
        edit.setTrigger("Edit project").setDescription("Edit the project, then save your changes.").setConfirmLabel("Save project");
        Fragment fields = new Fragment(SwDialog.CONTENT_ID, "settings", this);
        fields.add(name, new SwTextArea<String>("notes", "Notes", Model.of("A little more craft. A lot more Wicket.")));
        fields.add(SwButton.onClick("reset", "Review reset", target -> confirm.setConfirmLabel("Reset name")
                .setConfirmVariant(ButtonBehavior.Variant.DEFAULT)
                .ask(target, "Reset the project name?", "The settings sheet stays open underneath this confirmation.", t -> {
                    project.setObject("Shadwick");
                    t.add(name);
                })).setVariant(ButtonBehavior.Variant.OUTLINE));
        fields.add(SwButton.onClick("refresh", "Refresh open sheet", target -> target.add(edit))
                .setVariant(ButtonBehavior.Variant.OUTLINE));
        fields.add(new ListView<Integer>("details", IntStream.rangeClosed(1, 16).boxed().toList()) {
            @Override protected void populateItem(ListItem<Integer> item) {
                item.add(new Label("detail", "Setting " + item.getModelObject() + ": the body scrolls while the title and actions stay visible."));
            }
        });
        edit.setContent(fields);
        add(edit);

        add(new SwSheet("start", "Project details").setSide(SwSheet.Side.START).setTrigger("Start sheet")
                .setDescription("Start and end follow the page text direction.").setFooterVisible(false)
                .setContent(new Label(SwDialog.CONTENT_ID, "A sheet for supporting details. Escape, the close button or the backdrop returns you to the page.")));
        add(new SwSheet("top", "Workspace notice").setSide(SwSheet.Side.TOP).setTrigger("Top sheet")
                .setDescription("A full-width sheet attached to the top edge.").setFooterVisible(false)
                .setContent(new Label(SwDialog.CONTENT_ID, "Your workspace is ready. This is an informational example.")));
        add(new SwSheet("bottom", "Quick actions").setSide(SwSheet.Side.BOTTOM).setTrigger("Bottom sheet")
                .setDescription("A full-width sheet attached to the bottom edge.").setFooterVisible(false)
                .setContent(new Label(SwDialog.CONTENT_ID, "Supporting content can also appear from below. Close this sheet to continue.")));
    }

    private void record(Label status, Label count, String message, AjaxRequestTarget target) {
        actions.setObject(actions.getObject() + 1);
        status.setDefaultModelObject(message);
        target.add(status, count);
    }

    @Override protected String getTitle() { return "Overlays"; }
}
