package dev.jbaby.shadwick.components;

import dev.jbaby.shadwick.behaviors.ButtonBehavior;
import dev.jbaby.shadwick.behaviors.NestedFormButtonBehavior;
import dev.jbaby.shadwick.util.SerializableConsumer;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.markup.html.form.AjaxButton;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;

import java.util.Objects;

/**
 * A shadcn-styled {@link AjaxButton}: submits its form over Ajax, running {@code onSubmit(target)} when the form is
 * valid and {@code onError(target)} when it isn't. Attach it to a {@code <button>} or an
 * {@code <input type="submit">}; the label becomes the button's text (or {@code value}), so
 * {@code <button wicket:id="save"></button>} is enough. Shadwick fields show their own errors, without the handler
 * adding them to the target.
 * <p>
 * In a form nested in another, it renders as {@code type="button"}, so Enter in a field of the outer form can't click
 * it ({@link NestedFormButtonBehavior}); make it the nested form's default button for Enter in that form's fields.
 */
public class SwSubmitButton extends AjaxButton {

    private final ButtonBehavior buttonBehavior = new ButtonBehavior();

    /** A button keeping its markup label. */
    public SwSubmitButton(String id) {
        this(id, (IModel<String>) null);
    }

    public SwSubmitButton(String id, String label) {
        this(id, Model.of(label));
    }

    public SwSubmitButton(String id, IModel<String> label) {
        super(id, label);
        add(buttonBehavior, new NestedFormButtonBehavior());
    }

    /** A button submitting the given form, which needn't contain it. */
    public SwSubmitButton(String id, IModel<String> label, Form<?> form) {
        super(id, label, form);
        add(buttonBehavior, new NestedFormButtonBehavior());
    }

    public SwSubmitButton setVariant(ButtonBehavior.Variant variant) {
        buttonBehavior.setVariant(variant);
        return this;
    }

    /**
     * The button's size; {@link ButtonBehavior.Size#ICON} is square, for a single icon. Name an icon button for
     * screen readers with an {@code aria-label}.
     */
    public SwSubmitButton setSize(ButtonBehavior.Size size) {
        buttonBehavior.setSize(size);
        return this;
    }

    /**
     * A button submitting its form over Ajax, running {@code onSubmit} when the form is valid, without a subclass:
     * {@code form.add(SwSubmitButton.onSubmit("save", "Save", target -> ...))}.
     */
    public static SwSubmitButton onSubmit(String id, String label, SerializableConsumer<AjaxRequestTarget> onSubmit) {
        return onSubmit(id, Model.of(label), onSubmit, null);
    }

    /** A button submitting its form over Ajax, running {@code onSubmit} when the form is valid. */
    public static SwSubmitButton onSubmit(String id, IModel<String> label, SerializableConsumer<AjaxRequestTarget> onSubmit) {
        return onSubmit(id, label, onSubmit, null);
    }

    /**
     * A button submitting its form over Ajax, running {@code onSubmit} when the form is valid and {@code onError}
     * when it isn't, e.g. {@code target -> form.error("Please fix the errors.")}. Shadwick fields show their own
     * errors without it.
     */
    public static SwSubmitButton onSubmit(String id, String label, SerializableConsumer<AjaxRequestTarget> onSubmit,
                                          SerializableConsumer<AjaxRequestTarget> onError) {
        return onSubmit(id, Model.of(label), onSubmit, onError);
    }

    /**
     * A button submitting its form over Ajax, running {@code onSubmit} when the form is valid and {@code onError},
     * if given, when it isn't.
     */
    public static SwSubmitButton onSubmit(String id, IModel<String> label, SerializableConsumer<AjaxRequestTarget> onSubmit,
                                          SerializableConsumer<AjaxRequestTarget> onError) {
        Objects.requireNonNull(onSubmit);
        return new SwSubmitButton(id, label) {
            @Override
            protected void onSubmit(AjaxRequestTarget target) {
                onSubmit.accept(target);
            }

            @Override
            protected void onError(AjaxRequestTarget target) {
                if (onError != null) {
                    onError.accept(target);
                }
            }
        };
    }
}
