package dev.jbaby.shadwick.components;

import dev.jbaby.shadwick.behaviors.BadgeBehavior;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;

/** An HTML-escaped badge. For composed markup, add BadgeBehavior to a container instead. */
public class SwBadge extends Label {
    private final BadgeBehavior behavior;

    public SwBadge(String id) { this(id, (IModel<?>) null); }
    public SwBadge(String id, String text) { this(id, Model.of(text)); }
    public SwBadge(String id, IModel<?> model) { this(id, model, BadgeBehavior.Variant.DEFAULT); }
    public SwBadge(String id, String text, BadgeBehavior.Variant variant) { this(id, Model.of(text), variant); }
    public SwBadge(String id, IModel<?> model, BadgeBehavior.Variant variant) { this(id, model, Model.of(variant)); }
    public SwBadge(String id, IModel<?> model, IModel<BadgeBehavior.Variant> variant) {
        super(id, model);
        behavior = new BadgeBehavior(variant);
        add(behavior);
    }

    public SwBadge setVariant(BadgeBehavior.Variant variant) { behavior.setVariant(variant); return this; }
    public SwBadge setVariant(IModel<BadgeBehavior.Variant> variant) { behavior.setVariant(variant); return this; }
}
