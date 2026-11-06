package dev.jbaby.shadwick.components;

import dev.jbaby.shadwick.behaviors.ProgressBehavior;
import org.apache.wicket.markup.html.WebComponent;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;

/** Native progress bar. Use on <progress>, give it an accessible label, and use a null value while progress is unknown. */
public class SwProgress extends WebComponent {
    private final ProgressBehavior appearance;
    public SwProgress(String id, Number value) { this(id, Model.of(value)); }
    public SwProgress(String id, IModel<? extends Number> value) { this(id, value, Model.of(100)); }
    public SwProgress(String id, IModel<? extends Number> value, IModel<? extends Number> max) {
        super(id); appearance = new ProgressBehavior(value == null ? Model.of() : value, max == null ? Model.of(100) : max);
        add(appearance); setOutputMarkupId(true);
    }
    public SwProgress setVariant(ProgressBehavior.Variant value) { appearance.setVariant(value); return this; }
    public SwProgress setVariant(IModel<ProgressBehavior.Variant> value) { appearance.setVariant(value); return this; }
    public SwProgress setLabel(String value) { return setLabel(Model.of(value)); }
    public SwProgress setLabel(IModel<String> value) { appearance.setLabel(value); return this; }
}
