package dev.jbaby.shadwick.components;

import dev.jbaby.shadwick.behaviors.AlertBehavior;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;

/** An HTML-escaped alert. For composed markup, add AlertBehavior to a container instead. */
public class SwAlert extends Label {
    private final AlertBehavior behavior;

    public SwAlert(String id) { this(id, (IModel<?>) null); }
    public SwAlert(String id, String text) { this(id, Model.of(text)); }
    public SwAlert(String id, IModel<?> model) { this(id, model, AlertBehavior.Variant.DEFAULT); }
    public SwAlert(String id, String text, AlertBehavior.Variant variant) { this(id, Model.of(text), variant); }
    public SwAlert(String id, IModel<?> model, AlertBehavior.Variant variant) { this(id, model, Model.of(variant)); }
    public SwAlert(String id, IModel<?> model, IModel<AlertBehavior.Variant> variant) {
        super(id, model);
        behavior = new AlertBehavior(variant);
        add(behavior);
    }

    public SwAlert setVariant(AlertBehavior.Variant variant) { behavior.setVariant(variant); return this; }
    public SwAlert setVariant(IModel<AlertBehavior.Variant> variant) { behavior.setVariant(variant); return this; }
}
