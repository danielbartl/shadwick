package dev.jbaby.shadwick;

import dev.jbaby.shadwick.behaviors.ButtonBehavior;
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
