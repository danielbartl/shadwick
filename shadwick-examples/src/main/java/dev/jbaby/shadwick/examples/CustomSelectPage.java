package dev.jbaby.shadwick.examples;

import dev.jbaby.shadwick.Sw;
import dev.jbaby.shadwick.behaviors.SelectBehavior;
import dev.jbaby.shadwick.components.SwSubmitButton;
import dev.jbaby.shadwick.components.SwButton;
import dev.jbaby.shadwick.components.form.SwSelect;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.form.AjaxFormComponentUpdatingBehavior;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.form.Button;
import org.apache.wicket.markup.html.form.ChoiceRenderer;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.model.CompoundPropertyModel;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.request.mapper.parameter.PageParameters;
import java.io.Serializable;
import java.util.List;
import java.util.stream.IntStream;

/** Single-choice custom pickers, backed by native Wicket form components. */
public class CustomSelectPage extends BasePage {
    private static final List<Workspace> WORKSPACES = List.of(
            new Workspace("design", "Design", "Product", "pencil", false),
            new Workspace("engineering", "Engineering", "Product", "settings", false),
            new Workspace("enterprise", "Enterprise · unavailable", "Product", "building-2", true),
            new Workspace("support", "Support", "Operations", "users", false),
            new Workspace("success", "Success", "Operations", "star", false));
    public record Workspace(String id, String name, String group, String icon, boolean disabled) implements Serializable {}
    public static class Settings implements Serializable { public Workspace workspace; }
    public CustomSelectPage(PageParameters parameters) {
        super(parameters);
        var saved = new Label("saved", Model.of(""));
        var form = new Form<Settings>("form", new CompoundPropertyModel<>(new Settings())) {
            @Override protected void onSubmit() { saved.setDefaultModelObject("Saved: " + getModelObject().workspace.name()); }
        };
        form.add(workspace("workspace", null).setRequired(true));
        form.add(new Button("save").add(Sw.Behaviors.button())); form.add(saved); add(form);
        var ajaxSaved = new Label("saved", Model.of("")); ajaxSaved.setOutputMarkupId(true);
        var ajaxForm = new Form<Settings>("ajaxForm", new CompoundPropertyModel<>(new Settings()));
        ajaxForm.add(workspace("workspace", null).setRequired(true));
        ajaxForm.add(SwSubmitButton.onSubmit("save", "Save workspace", target -> {
            ajaxSaved.setDefaultModelObject("Saved: " + ajaxForm.getModelObject().workspace.name()); target.add(ajaxSaved);
        })); ajaxForm.add(ajaxSaved); add(ajaxForm);
        IModel<Workspace> live = Model.of(WORKSPACES.get(0));
        IModel<List<Workspace>> choices = Model.ofList(WORKSPACES);
        var liveForm = new Form<Void>("liveForm");
        var current = new Label("current", (IModel<String>) () -> "Current: " + (live.getObject() == null ? "none" : live.getObject().name()));
        current.setOutputMarkupId(true);
        var choice = new SwSelect<>("workspace", "Active workspace", live, choices, new ChoiceRenderer<>("name", "id"))
                .setGroup(Workspace::group).setIcon(Workspace::icon).setDisabledWhen(Workspace::disabled)
                .setPlaceholder("No workspace").setSize(SelectBehavior.Size.SMALL);
        choice.getInput().setNullValid(true);
        choice.add(new AjaxFormComponentUpdatingBehavior("change") {
            @Override protected void onUpdate(AjaxRequestTarget target) { target.add(current); }
        });
        liveForm.add(choice, current);
        liveForm.add(SwButton.onClick("refresh", "Refresh choices", target -> {
            choices.setObject(choices.getObject().size() == WORKSPACES.size() ? WORKSPACES.subList(0, 3) : WORKSPACES);
            if (!choices.getObject().contains(live.getObject())) live.setObject(null);
            target.add(choice, current);
        })); add(liveForm);
        add(workspace("dialogWorkspace", Model.of(WORKSPACES.get(0))).setLabel("Dialog workspace"));
        add(workspace("popoverWorkspace", Model.of(WORKSPACES.get(0))).setLabel("Popover workspace"));
        var managed = workspace("managed", Model.of(WORKSPACES.get(0))); managed.setLabel("Managed workspace").setEnabled(false); add(managed);
        var longList = IntStream.rangeClosed(1,40).mapToObj(i -> "Region " + String.format("%02d",i)).toList();
        add(new SwSelect<>("region", "Deployment region", Model.of("Region 20"), Model.ofList(longList))
                .setDescription("A longer list with keyboard paging and native scrolling."));
    }
    private static SwSelect<Workspace> workspace(String id, IModel<Workspace> model) {
        return new SwSelect<>(id, "Workspace", model, Model.ofList(WORKSPACES), new ChoiceRenderer<>("name","id"))
                .setPlaceholder("Choose a workspace").setGroup(Workspace::group).setIcon(Workspace::icon)
                .setDisabledWhen(Workspace::disabled).setSeparatorBefore(w -> w.id().equals("support"))
                .setDescription("Choose a team. Enterprise is currently unavailable.");
    }
    @Override protected String getTitle() { return "Custom select"; }
}
