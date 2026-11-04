package dev.jbaby.shadwick.examples;

import dev.jbaby.shadwick.Sw;
import dev.jbaby.shadwick.components.SwSubmitButton;
import dev.jbaby.shadwick.components.form.SwCheckBoxMultipleChoice;
import dev.jbaby.shadwick.components.form.SwDropdownChoice;
import dev.jbaby.shadwick.components.form.SwListMultipleChoice;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.form.AjaxFormChoiceComponentUpdatingBehavior;
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
import java.util.ArrayList;
import java.util.List;

/** Native selections with normal submission, Ajax feedback and immediate selection callbacks. */
public class SelectPage extends BasePage {
    private static final List<Team> TEAMS = List.of(new Team("design", "Design"),
            new Team("engineering", "Engineering"), new Team("support", "Support"));
    private static final List<String> TOPICS = List.of("Releases", "Guides", "Events");

    public SelectPage(PageParameters parameters) {
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
        ajaxForm.add(SwSubmitButton.onSubmit("save", "Save selections", target -> {
            ajaxSaved.setDefaultModelObject("Saved: " + ajaxForm.getModelObject());
            target.add(ajaxSaved);
        }));
        ajaxForm.add(ajaxSaved);
        add(ajaxForm);

        Settings live = new Settings();
        Label current = new Label("current", (IModel<String>) () -> "Current: " + live);
        current.setOutputMarkupId(true);
        Form<Settings> liveForm = new Form<>("liveForm", new CompoundPropertyModel<>(live));
        var team = new SwDropdownChoice<>("team", Model.ofList(TEAMS), new ChoiceRenderer<>("name", "id"));
        team.getInput().setNullValid(true);
        team.add(new AjaxFormComponentUpdatingBehavior("change") {
            @Override
            protected void onUpdate(AjaxRequestTarget target) { target.add(current); }
        });
        liveForm.add(team);
        liveForm.add(new SwListMultipleChoice<>("topics", Model.ofList(TOPICS)).setMaxRows(3)
                .add(new AjaxFormComponentUpdatingBehavior("change") {
                    @Override
                    protected void onUpdate(AjaxRequestTarget target) { target.add(current); }
                }));
        liveForm.add(new SwCheckBoxMultipleChoice<>("channels", Model.ofList(List.of("Email", "SMS", "In-app")))
                .setInline(true).add(new AjaxFormChoiceComponentUpdatingBehavior() {
                    @Override
                    protected void onUpdate(AjaxRequestTarget target) { target.add(current); }
                }));
        liveForm.add(current);
        add(liveForm);

        var managedTeam = new SwDropdownChoice<>("managedTeam", "Managed team", Model.of(TEAMS.get(0)),
                Model.ofList(TEAMS), new ChoiceRenderer<>("name", "id"));
        managedTeam.setEnabled(false);
        add(managedTeam);
        var managedTopics = new SwListMultipleChoice<>("managedTopics", "Managed topics",
                Model.ofList(new ArrayList<>(List.of("Guides"))), Model.ofList(TOPICS)).setMaxRows(3);
        managedTopics.setEnabled(false);
        add(managedTopics);
        var managedChannels = new SwCheckBoxMultipleChoice<>("managedChannels", "Managed channels",
                Model.ofList(new ArrayList<>(List.of("Email"))), Model.ofList(List.of("Email", "SMS"))).setInline(true);
        managedChannels.setEnabled(false);
        add(managedChannels);
    }

    private static void addFields(Form<Settings> form) {
        form.add(new SwDropdownChoice<>("team", Model.ofList(TEAMS), new ChoiceRenderer<>("name", "id"))
                .setRequired(true).setDescription("Choose a team. Option IDs remain stable across renders."));
        form.add(new SwListMultipleChoice<>("topics", Model.ofList(TOPICS)).setMaxRows(3).setRequired(true)
                .setDescription("Select one or more topics. Use your platform's modifier keys for multiple selections."));
        form.add(new SwCheckBoxMultipleChoice<>("channels", Model.ofList(List.of("Email", "SMS", "In-app")))
                .setRequired(true).setDescription("Choose at least one delivery channel."));
    }

    public record Team(String id, String name) implements Serializable {}

    public static class Settings implements Serializable {
        public Team team;
        public List<String> topics = new ArrayList<>();
        public List<String> channels = new ArrayList<>();

        @Override
        public String toString() {
            return "team " + (team == null ? "none" : team.name()) + "; topics " + String.join(", ", topics)
                    + "; channels " + String.join(", ", channels) + ".";
        }
    }

    @Override
    protected String getTitle() { return "Selections"; }
}
