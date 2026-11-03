package dev.jbaby.shadwick;

import dev.jbaby.shadwick.behaviors.ButtonBehavior;
import dev.jbaby.shadwick.behaviors.AlertBehavior;
import dev.jbaby.shadwick.behaviors.BadgeBehavior;
import dev.jbaby.shadwick.behaviors.CardBehavior;
import dev.jbaby.shadwick.behaviors.FeedbackToastsBehavior;
import dev.jbaby.shadwick.behaviors.SeparatorBehavior;
import dev.jbaby.shadwick.behaviors.SkeletonBehavior;
import dev.jbaby.shadwick.behaviors.SpinnerBehavior;
import dev.jbaby.shadwick.behaviors.StyledBehavior;
import dev.jbaby.shadwick.components.SwButton;
import dev.jbaby.shadwick.components.SwSubmitButton;
import dev.jbaby.shadwick.util.SerializableConsumer;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.model.IModel;

/**
 * Shortcuts for Shadwick behaviors ({@link Behaviors}) and for the components driven by a callback
 * ({@link Components}, also available as static factories on the classes themselves, e.g.
 * {@link SwButton#onClick(String, String, SerializableConsumer)}). Every other component is created with its
 * constructor, like any Wicket component.
 */
public final class Sw {

    private Sw() {
    }

    /** Factory for Shadwick behaviors, which style an existing Wicket component. */
    public static final class Behaviors {

        private Behaviors() {
        }

        public static ButtonBehavior button() {
            return new ButtonBehavior();
        }

        public static ButtonBehavior button(ButtonBehavior.Variant variant) {
            return new ButtonBehavior(variant);
        }

        public static CardBehavior card() { return new CardBehavior(); }
        public static BadgeBehavior badge() { return new BadgeBehavior(); }
        public static BadgeBehavior badge(BadgeBehavior.Variant variant) { return new BadgeBehavior(variant); }
        public static BadgeBehavior badge(IModel<BadgeBehavior.Variant> variant) { return new BadgeBehavior(variant); }
        public static AlertBehavior alert() { return new AlertBehavior(); }
        public static AlertBehavior alert(AlertBehavior.Variant variant) { return new AlertBehavior(variant); }
        public static AlertBehavior alert(IModel<AlertBehavior.Variant> variant) { return new AlertBehavior(variant); }
        public static SeparatorBehavior separator() { return new SeparatorBehavior(); }
        public static StyledBehavior kbd() { return new StyledBehavior("sw-kbd"); }
        public static SkeletonBehavior skeleton() { return new SkeletonBehavior(); }
        public static SkeletonBehavior skeleton(SkeletonBehavior.Shape shape) { return new SkeletonBehavior(shape); }
        public static SpinnerBehavior spinner() { return new SpinnerBehavior(); }
        public static SpinnerBehavior spinner(SpinnerBehavior.Size size) { return new SpinnerBehavior(size); }
        public static FeedbackToastsBehavior feedbackToasts() { return new FeedbackToastsBehavior(); }
        public static FeedbackToastsBehavior feedbackToasts(org.apache.wicket.feedback.IFeedbackMessageFilter filter) {
            return new FeedbackToastsBehavior(filter);
        }
    }

    /** Factory for the Shadwick components driven by a callback. */
    public static final class Components {

        private Components() {
        }

        public static SwButton button(String id, String label, SerializableConsumer<AjaxRequestTarget> onClick) {
            return SwButton.onClick(id, label, onClick);
        }

        public static SwButton button(String id, IModel<String> label, SerializableConsumer<AjaxRequestTarget> onClick) {
            return SwButton.onClick(id, label, onClick);
        }

        public static SwSubmitButton submitButton(String id, String label, SerializableConsumer<AjaxRequestTarget> onSubmit) {
            return SwSubmitButton.onSubmit(id, label, onSubmit);
        }

        public static SwSubmitButton submitButton(String id, IModel<String> label, SerializableConsumer<AjaxRequestTarget> onSubmit) {
            return SwSubmitButton.onSubmit(id, label, onSubmit);
        }

        public static SwSubmitButton submitButton(String id, String label, SerializableConsumer<AjaxRequestTarget> onSubmit,
                                                  SerializableConsumer<AjaxRequestTarget> onError) {
            return SwSubmitButton.onSubmit(id, label, onSubmit, onError);
        }

        public static SwSubmitButton submitButton(String id, IModel<String> label, SerializableConsumer<AjaxRequestTarget> onSubmit,
                                                  SerializableConsumer<AjaxRequestTarget> onError) {
            return SwSubmitButton.onSubmit(id, label, onSubmit, onError);
        }
    }
}
