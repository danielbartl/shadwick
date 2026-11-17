package dev.jbaby.shadwick.components;

import dev.jbaby.shadwick.util.ComponentIds;
import dev.jbaby.shadwick.Shadwick;
import dev.jbaby.shadwick.behaviors.ButtonBehavior;
import dev.jbaby.shadwick.behaviors.NestedFormButtonBehavior;
import dev.jbaby.shadwick.util.SerializableConsumer;
import dev.jbaby.shadwick.util.SerializableFunction;

import org.apache.wicket.AttributeModifier;
import org.apache.wicket.Component;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.markup.html.form.AjaxFallbackButton;
import org.apache.wicket.feedback.FeedbackCollector;
import org.apache.wicket.feedback.FeedbackMessage;
import org.apache.wicket.markup.head.IHeaderResponse;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.model.StringResourceModel;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

/**
 * A form filled in over several steps - onboarding, an order, an application - with a row of numbered steps showing
 * where the user is, and Back, Next and Finish buttons. It follows Wicket Oat's {@code OatWizard}:
 * <pre>{@code
 * SwWizard wizard = new SwWizard("wizard") {
 *     @Override
 *     protected void onFinish(AjaxRequestTarget target) {
 *         orders.place(order);
 *         setResponsePage(OrderPlacedPage.class);
 *     }
 * };
 * wizard.addStep("Customer", id -> new CustomerStep(id, order));
 * wizard.addStep("Items", id -> new ItemsStep(id, order));
 * wizard.addStep("Review", id -> new ReviewStep(id, order));
 * add(wizard);
 * }</pre>
 * on {@code <div wicket:id="wizard"></div>}. Each step is a component (usually a panel of Shadwick fields) created
 * when the step is shown, with the id its factory is given, so keep the data in a model all steps share, like
 * {@code order} above. Next validates the current step's fields and shows their errors inline; Back and the numbers of
 * earlier steps go back without validating. {@link #onNext} can check a step further and keep the user on it: an error
 * on one of its fields shows there, one on the form shows in the summary above the step.
 * <p>
 * It works without JavaScript too: the buttons then submit the form normally, and {@link #onNext} and
 * {@link #onFinish} receive {@code null} for their target. Over Ajax the wizard is re-rendered, and the keyboard focus
 * moves to the new step, or to the first field with an error. Don't put it inside another form: a form inside a form is
 * not HTML, so only Ajax could submit it.
 */
public abstract class SwWizard extends Panel {

    /** The id each step's factory is given. */
    public static final String STEP_ID = "step";

    private final List<Step> steps = new ArrayList<>();
    private int current;
    private int shown = -1;
    private Form<Void> form;
    private WebMarkupContainer group;
    private AjaxFallbackButton next, finish;

    private record Step(IModel<String> title, SerializableFunction<String, ? extends Component> factory) implements Serializable {}

    public SwWizard(String id) {
        super(id);
        setOutputMarkupId(true);
    }

    /** Adds a step; the factory creates its component with the id it is given ({@link #STEP_ID}). */
    public SwWizard addStep(String title, SerializableFunction<String, ? extends Component> step) {
        return addStep(Model.of(title), step);
    }

    /** Adds a step; the factory creates its component with the id it is given ({@link #STEP_ID}). */
    public SwWizard addStep(IModel<String> title, SerializableFunction<String, ? extends Component> step) {
        steps.add(new Step(Objects.requireNonNull(title), Objects.requireNonNull(step)));
        return this;
    }

    /** The index of the step shown, from 0. */
    public int getCurrentStep() {
        return current;
    }

    /** Shows a step, e.g. to start somewhere else than at the first. */
    public SwWizard setCurrentStep(int index) {
        if (index < 0 || index >= steps.size()) {
            throw new IndexOutOfBoundsException("No step " + index + " of " + steps.size());
        }
        current = index;
        return this;
    }

    @Override
    protected void onInitialize() {
        super.onInitialize();
        if (steps.isEmpty()) throw new IllegalStateException("A SwWizard needs at least one step");
        add(newSteps());
        form = new Form<>("form") {
            @Override
            protected void onConfigure() {
                super.onConfigure();
                // Enter in a field moves on, as Next does - or finishes, on the last step
                setDefaultButton(isLastStep() ? finish : next);
            }
        };
        add(form);
        form.add(new SwFormErrors("errors", form).setAutofocus(true));
        group = new WebMarkupContainer("group");
        group.setOutputMarkupId(true);
        group.add(AttributeModifier.replace("aria-label", (IModel<String>) this::stepLabel));
        form.add(group);
        group.add(new WebMarkupContainer(STEP_ID));
        form.add(newBack(), next = newNext(), finish = newFinish());
    }

    private SwSteps newSteps() {
        IModel<List<String>> titles = () -> steps.stream().map(step -> step.title().getObject()).toList();
        return new SwSteps("steps", titles, (IModel<Integer>) () -> current)
                .setLabel(new StringResourceModel("SwWizard.steps", this))
                // Only finished steps can be revisited; the others need Next and its validation
                .setOnSelect((target, index) -> show(index, Optional.ofNullable(target)));
    }

    private AjaxFallbackButton newBack() {
        AjaxFallbackButton back = new AjaxFallbackButton("back", new StringResourceModel("SwWizard.back", this), null) {
            @Override
            protected void onSubmit(Optional<AjaxRequestTarget> target) {
                show(current - 1, target);
            }

            @Override
            protected void onConfigure() {
                super.onConfigure();
                setVisible(current > 0);
            }
        };
        back.setDefaultFormProcessing(false);
        back.add(new ButtonBehavior(ButtonBehavior.Variant.OUTLINE), new NestedFormButtonBehavior());
        return back;
    }

    private AjaxFallbackButton newNext() {
        AjaxFallbackButton button = new AjaxFallbackButton("next", new StringResourceModel("SwWizard.next", this), null) {
            @Override
            protected void onSubmit(Optional<AjaxRequestTarget> target) {
                onNext(current, target.orElse(null));
                if (hasErrors()) target.ifPresent(t -> t.add(SwWizard.this));
                else show(current + 1, target);
            }

            @Override
            protected void onError(Optional<AjaxRequestTarget> target) {
                target.ifPresent(t -> t.add(SwWizard.this));
            }

            @Override
            protected void onConfigure() {
                super.onConfigure();
                setVisible(!isLastStep());
            }
        };
        button.add(new ButtonBehavior(), new NestedFormButtonBehavior());
        return button;
    }

    private AjaxFallbackButton newFinish() {
        AjaxFallbackButton button = new AjaxFallbackButton("finish", new StringResourceModel("SwWizard.finish", this), null) {
            @Override
            protected void onSubmit(Optional<AjaxRequestTarget> target) {
                onNext(current, target.orElse(null));
                if (hasErrors()) target.ifPresent(t -> t.add(SwWizard.this));
                else onFinish(target.orElse(null));
            }

            @Override
            protected void onError(Optional<AjaxRequestTarget> target) {
                target.ifPresent(t -> t.add(SwWizard.this));
            }

            @Override
            protected void onConfigure() {
                super.onConfigure();
                setVisible(isLastStep());
            }
        };
        button.add(new ButtonBehavior(), new NestedFormButtonBehavior());
        return button;
    }

    private boolean isLastStep() {
        return current == steps.size() - 1;
    }

    private String stepLabel() {
        return new StringResourceModel("SwWizard.step", this)
                .setParameters(current + 1, steps.size(), steps.get(current).title().getObject()).getString();
    }

    /** Moves to a step; over Ajax the wizard is re-rendered and the new step takes the focus. */
    private void show(int index, Optional<AjaxRequestTarget> target) {
        setCurrentStep(index);
        target.ifPresent(t -> {
            t.add(this);
            t.focusComponent(group);
        });
    }

    @Override
    protected void onConfigure() {
        super.onConfigure();
        // The step's component is created when it is shown, so it starts from the shared model each time
        if (shown != current) {
            Component body = ComponentIds.fromFactory(steps.get(current).factory().apply(STEP_ID), STEP_ID, "A wizard step");
            group.addOrReplace(body);
            shown = current;
        }
    }

    /**
     * Called when Next (or Finish) is clicked and the step's fields are valid. Report an error on a component - e.g.
     * {@code emailField.getInput().error("Already registered")} - or on the form to keep the user on the step.
     *
     * @param step the index of the step being left, from 0
     * @param target the Ajax request, or {@code null} when the form was submitted without JavaScript
     */
    protected void onNext(int step, AjaxRequestTarget target) {
    }

    /**
     * Called when Finish is clicked on the last step and its fields are valid.
     *
     * @param target the Ajax request, or {@code null} when the form was submitted without JavaScript
     */
    protected abstract void onFinish(AjaxRequestTarget target);

    /** The wizard's form, e.g. to report an error of a step as a whole from {@link #onNext}. */
    public Form<Void> getForm() {
        return form;
    }

    private boolean hasErrors() {
        return !new FeedbackCollector(this).collect(FeedbackMessage::isError).isEmpty();
    }

    @Override
    public void renderHead(IHeaderResponse response) {
        super.renderHead(response);
        Shadwick.renderResources(response);
    }

    @Override
    protected void onDetach() {
        // SwSteps reads the titles through a model of its own, so detach them here
        steps.forEach(step -> step.title().detach());
        super.onDetach();
    }

    /** A wizard without a subclass; {@code onFinish} runs after the last step. Add the steps with {@link #addStep}. */
    public static SwWizard onFinish(String id, SerializableConsumer<AjaxRequestTarget> onFinish) {
        Objects.requireNonNull(onFinish);
        return new SwWizard(id) {
            @Override
            protected void onFinish(AjaxRequestTarget target) {
                onFinish.accept(target);
            }
        };
    }
}
