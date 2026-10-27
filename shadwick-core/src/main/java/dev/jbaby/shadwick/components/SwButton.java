package dev.jbaby.shadwick.components;

import dev.jbaby.shadwick.behaviors.ButtonBehavior;
import dev.jbaby.shadwick.util.SerializableConsumer;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.markup.html.AjaxLink;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;

import java.util.Objects;

/**
 * A shadcn button that runs {@link #onClick} over Ajax. Attach it to an {@code <a>} or a {@code <button>}; the label
 * becomes the tag's text, so no inner markup is needed: {@code <button wicket:id="save"></button>}. Without a label,
 * the tag's own body is rendered, e.g. for an icon. Like any {@code AjaxLink}, a {@code <button>} is rendered with
 * {@code type="button"}, so it never submits a surrounding form; to submit a form, use {@link SwSubmitButton}.
 */
public abstract class SwButton extends AjaxLink<Void> {

    private final ButtonBehavior buttonBehavior;

    /** A button rendering its markup body, e.g. an icon. */
    public SwButton(String id) {
        this(id, (IModel<String>) null);
    }

    public SwButton(String id, String label) {
        this(id, Model.of(label));
    }

    public SwButton(String id, IModel<String> labelModel) {
        super(id);
        setBody(labelModel);
        this.buttonBehavior = new ButtonBehavior();
        add(this.buttonBehavior);
    }

    public SwButton setVariant(ButtonBehavior.Variant variant) {
        buttonBehavior.setVariant(variant);
        return this;
    }

    /**
     * The button's size; {@link ButtonBehavior.Size#ICON} is square, for a single icon. Name an icon button for
     * screen readers with an {@code aria-label}.
     */
    public SwButton setSize(ButtonBehavior.Size size) {
        buttonBehavior.setSize(size);
        return this;
    }

    @Override
    public abstract void onClick(AjaxRequestTarget target);

    /**
     * A button running {@code onClick} over Ajax, without a subclass:
     * {@code add(SwButton.onClick("save", "Save", target -> ...))}.
     */
    public static SwButton onClick(String id, String label, SerializableConsumer<AjaxRequestTarget> onClick) {
        return onClick(id, Model.of(label), onClick);
    }

    /** A button running {@code onClick} over Ajax, without a subclass. */
    public static SwButton onClick(String id, IModel<String> label, SerializableConsumer<AjaxRequestTarget> onClick) {
        Objects.requireNonNull(onClick);
        return new SwButton(id, label) {
            @Override
            public void onClick(AjaxRequestTarget target) {
                onClick.accept(target);
            }
        };
    }
}
