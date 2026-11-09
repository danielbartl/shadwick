package dev.jbaby.shadwick.examples;

import dev.jbaby.shadwick.Sw;
import dev.jbaby.shadwick.components.*;
import dev.jbaby.shadwick.components.form.SwTextField;

import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.form.*;
import org.apache.wicket.model.*;
import org.apache.wicket.request.mapper.parameter.PageParameters;

import java.util.List;

/** Application-owned validation/navigation composed with a steps indicator. */
public class StepsPage extends BasePage {

    private static final List<String> STAGES = List.of("Details", "Review", "Publish");
    private final Model<Integer> current = Model.of(1);
    private SwTextField<String> name;
    private Label receipt;
    private SwSteps workflow;

    public StepsPage(PageParameters parameters) {
        super(parameters);
    }

    @Override
    protected void onInitialize() {
        super.onInitialize();
        receipt = new Label("receipt", Model.of("Review the project before publishing."));
        name = new SwTextField<>("name", "Project name", Model.of("Website redesign"));
        add(
                new SwSteps(
                                "shipping",
                                List.of("Ordered", "Paid", "Shipped", "Delivered"),
                                Model.of(2))
                        .setLabel("Order status"));
        addWorkflowForm();
        addProgressStates();
        addLayoutVariants();
    }

    private void addWorkflowForm() {
        var form = new Form<Void>("form");
        add(form);
        workflow = newWorkflow();
        form.add(workflow);
        form.add(
                name.setRequired(true)
                        .setDescription(
                                "Next validates this field. Selecting a completed step goes back"
                                    + " without submitting it."));
        receipt.setOutputMarkupId(true);
        form.add(receipt);
        form.add(
                new Button("normal") {

                    @Override
                    public void onSubmit() {
                        advance(null);
                    }
                }.add(Sw.Behaviors.button()));
        form.add(SwSubmitButton.onSubmit("next", "Next with Ajax", this::advance));
        form.add(
                SwButton.onClick(
                        "reset",
                        "Start again",
                        target -> {
                            current.setObject(0);
                            receipt.setDefaultModelObject("Back at project details.");
                            target.add(workflow, receipt);
                        }));
        form.add(
                SwButton.onClick(
                        "finish",
                        "Mark complete",
                        target -> {
                            current.setObject(STAGES.size());
                            receipt.setDefaultModelObject("All stages complete.");
                            target.add(workflow, receipt);
                        }));
        form.add(
                SwButton.onClick(
                        "replace",
                        "Replace indicator",
                        target -> {
                            workflow = newWorkflow();
                            form.addOrReplace(workflow);
                            target.add(workflow);
                        }));
    }

    private void addProgressStates() {
        add(new SwSteps("notStarted", List.of("Prepare", "Send", "Confirm"), Model.of()));
        add(new SwSteps("complete", List.of("Prepare", "Send", "Confirm"), Model.of(3)));
        add(new SwSteps("single", List.of("One step"), Model.of(0)));
        add(new SwSteps("empty", List.of(), Model.of(0)));
    }

    private void addLayoutVariants() {
        add(
                new SwSteps(
                                "compact",
                                List.of(
                                        "Gather the project requirements",
                                        "Review the accessibility and language requirements",
                                        "Publish and monitor the project"),
                                Model.of(1))
                        .setLabel("Sidebar process"));
        add(
                new SwSteps(
                                "longTitles",
                                List.of(
                                        "An unusually long first stage that wraps without obscuring"
                                            + " the next marker",
                                        "averylongunbrokentitle".repeat(5),
                                        "<script>literal title</script>",
                                        "Final approval"),
                                Model.of(2))
                        .setLabel("Long process titles"));
        var disabled =
                new SwSteps("disabled", STAGES, Model.of(2))
                        .setLabel("Read-only process")
                        .setOnSelect(
                                (target, index) -> receipt.setDefaultModelObject("Unavailable"));
        disabled.setEnabled(false);
        add(disabled);
    }

    private SwSteps newWorkflow() {
        return new SwSteps("steps", STAGES, current)
                .setLabel("Project stages")
                .setOnSelect(
                        (target, index) -> {
                            current.setObject(index);
                            receipt.setDefaultModelObject("Returned to " + STAGES.get(index) + ".");
                            target.add(workflow, receipt);
                            // Focus belongs to this workflow, not to the indicator component.
                            target.focusComponent(name.getInput());
                        });
    }

    private void advance(AjaxRequestTarget target) {
        current.setObject(Math.min(STAGES.size(), current.getObject() + 1));
        receipt.setDefaultModelObject(
                current.getObject() == STAGES.size()
                        ? "All stages complete."
                        : "Now at " + STAGES.get(current.getObject()) + ".");
        if (target != null) target.add(workflow, receipt);
    }

    @Override
    protected String getTitle() {
        return "Steps indicator";
    }
}
