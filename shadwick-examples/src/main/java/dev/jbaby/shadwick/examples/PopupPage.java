package dev.jbaby.shadwick.examples;

import dev.jbaby.shadwick.Sw;
import dev.jbaby.shadwick.behaviors.ButtonBehavior;
import dev.jbaby.shadwick.behaviors.TooltipBehavior;
import dev.jbaby.shadwick.components.SwButton;
import dev.jbaby.shadwick.components.SwPopover;
import dev.jbaby.shadwick.components.SwSheet;
import dev.jbaby.shadwick.components.SwSubmitButton;
import dev.jbaby.shadwick.components.form.SwTextField;

import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.markup.html.list.ListItem;
import org.apache.wicket.markup.html.list.ListView;
import org.apache.wicket.markup.html.panel.Fragment;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.request.mapper.parameter.PageParameters;

import java.util.List;

/**
 * Anchored non-modal content and text-only hover/focus hints, including Ajax and nested overlays.
 */
public class PopupPage extends BasePage {

    private final Model<String> project = Model.of("Shadwick");
    private final Model<Integer> refreshes = Model.of(0);
    private SwPopover editor;

    public PopupPage(PageParameters parameters) {
        super(parameters);
    }

    @Override
    protected void onInitialize() {
        super.onInitialize();
        Label status = new Label("status", Model.of("No changes yet."));
        status.setOutputMarkupId(true);
        add(status);
        addEditor(status);
        add(
                new ListView<>("sides", List.of(SwPopover.Side.values())) {

                    @Override
                    protected void populateItem(ListItem<SwPopover.Side> item) {
                        SwPopover.Side side = item.getModelObject();
                        String label =
                                side.name().charAt(0)
                                        + side.name()
                                                .substring(1)
                                                .toLowerCase(java.util.Locale.ROOT);
                        item.add(
                                new SwPopover(
                                                "example",
                                                label + " placement",
                                                id ->
                                                        new Label(
                                                                id,
                                                                "Start and end follow text"
                                                                    + " direction."))
                                        .setTitle(label + " example")
                                        .setSide(side));
                    }
                });
        addTooltips(status);
        add(
                new SwSheet("sheet", "Hints inside a sheet")
                        .setTrigger("Open tooltip sheet")
                        .setFooterVisible(false)
                        .setContent(
                                new SwPopover(
                                                SwSheet.CONTENT_ID,
                                                "More details",
                                                id ->
                                                        new Label(
                                                                id,
                                                                "A non-modal popover inside a modal"
                                                                    + " sheet."))
                                        .setTitle("More details")));
    }

    private void addEditor(Label status) {
        editor =
                new SwPopover(
                                "editor",
                                "Quick edit",
                                id -> {
                                    Fragment content = new Fragment(id, "editorContent", this);
                                    Form<Void> form = new Form<>("form");
                                    SwTextField<String> name =
                                            new SwTextField<String>("name", "Display name", project)
                                                    .setRequired(true)
                                                    .setDescription("Visible to your team.");
                                    name.addToInput(
                                            Sw.Behaviors.tooltip("Use the public project name."));
                                    form.add(name);
                                    form.add(
                                            new SwPopover(
                                                            "nested",
                                                            "Visibility details",
                                                            nestedId ->
                                                                    new Label(
                                                                            nestedId,
                                                                            "Only people in this"
                                                                                + " workspace can"
                                                                                + " see the"
                                                                                + " project."))
                                                    .setTitle("Workspace visibility")
                                                    .setSide(SwPopover.Side.END));
                                    form.add(
                                            SwSubmitButton.onSubmit(
                                                    "save",
                                                    "Apply changes",
                                                    target -> {
                                                        status.setDefaultModelObject(
                                                                "Saved display name: "
                                                                        + project.getObject()
                                                                        + ".");
                                                        target.add(status);
                                                        editor.close(target);
                                                    }));
                                    form.add(
                                            SwButton.onClick(
                                                            "refresh",
                                                            "Refresh content",
                                                            target -> {
                                                                refreshes.setObject(
                                                                        refreshes.getObject() + 1);
                                                                target.add(editor.getContent());
                                                            })
                                                    .setVariant(ButtonBehavior.Variant.OUTLINE));
                                    form.add(
                                            new Label(
                                                    "revision",
                                                    (IModel<String>)
                                                            () ->
                                                                    "Content refreshes: "
                                                                            + refreshes
                                                                                    .getObject()));
                                    content.add(form);
                                    return content;
                                })
                        .setTitle("Quick project edit")
                        .setDescription("Make a small change without leaving the page.");
        add(editor);
        add(
                SwButton.onClick("replace", "Replace popover", target -> target.add(editor))
                        .setVariant(ButtonBehavior.Variant.OUTLINE));
        add(
                SwButton.onClick(
                                "after",
                                "Continue on page",
                                target -> {
                                    status.setDefaultModelObject("The page stayed interactive.");
                                    target.add(status);
                                })
                        .setVariant(ButtonBehavior.Variant.OUTLINE));
    }

    private void addTooltips(Label status) {
        Model<String> hint = Model.of("Download a CSV copy.");
        SwButton export =
                SwButton.onClick(
                                "export",
                                "Export",
                                target -> {
                                    status.setDefaultModelObject("Export preview requested.");
                                    target.add(status);
                                })
                        .setVariant(ButtonBehavior.Variant.OUTLINE);
        export.add(Sw.Behaviors.tooltip(hint));
        add(export);
        add(
                SwButton.onClick(
                                "changeHint",
                                "Update export hint",
                                target -> {
                                    hint.setObject("Updated <CSV> export hint.");
                                    target.add(export);
                                })
                        .setVariant(ButtonBehavior.Variant.OUTLINE));
        add(
                SwButton.onClick(
                                "clearHint",
                                "Remove export hint",
                                target -> {
                                    hint.setObject(null);
                                    target.add(export);
                                })
                        .setVariant(ButtonBehavior.Variant.OUTLINE));
        SwButton copy =
                new SwButton("copy") {

                    @Override
                    public void onClick(AjaxRequestTarget target) {
                        status.setDefaultModelObject("Copy preview requested.");
                        target.add(status);
                    }
                };
        copy.setVariant(ButtonBehavior.Variant.OUTLINE).setSize(ButtonBehavior.Size.ICON);
        copy.add(Sw.Behaviors.tooltip("Copy link").setMode(TooltipBehavior.Mode.LABEL));
        add(copy);
        var disabled =
                SwButton.onClick("disabled", "Managed action", target -> {})
                        .setVariant(ButtonBehavior.Variant.OUTLINE);
        disabled.add(Sw.Behaviors.tooltip("This is managed by your workspace."));
        disabled.setEnabled(false);
        add(disabled);
    }

    @Override
    protected String getTitle() {
        return "Popovers & tooltips";
    }
}
