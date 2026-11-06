package dev.jbaby.shadwick.behaviors;

import dev.jbaby.shadwick.util.RangeValues;
import org.apache.wicket.Component;
import org.apache.wicket.markup.ComponentTag;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;

/** Styles a native progress element; a null value is indeterminate. Modeled components default to a maximum of 100; caller-owned bounds are preserved. */
public class ProgressBehavior extends StyledBehavior {
    public enum Variant { DEFAULT, SECONDARY, DESTRUCTIVE }
    private final IModel<? extends Number> value, max;
    private IModel<Variant> variant = Model.of(Variant.DEFAULT);
    private IModel<String> label;
    public ProgressBehavior() { this(null, null); }
    public ProgressBehavior(IModel<? extends Number> value, IModel<? extends Number> max) {
        super("sw-progress"); this.value = value; this.max = max;
    }
    public ProgressBehavior setVariant(Variant value) { return setVariant(Model.of(value)); }
    public ProgressBehavior setVariant(IModel<Variant> value) { variant = value; return this; }
    public ProgressBehavior setLabel(IModel<String> value) { label = value; return this; }
    @Override public void onComponentTag(Component component, ComponentTag tag) {
        super.onComponentTag(component, tag);
        if (!"progress".equals(tag.getName())) throw new IllegalArgumentException("ProgressBehavior requires <progress>");
        if (max != null) {
            Number maximum = max.getObject();
            double upper = maximum == null ? 100 : RangeValues.finite(maximum, "Progress maximum");
            if (upper <= 0) throw new IllegalArgumentException("Progress maximum must be positive");
            tag.put("max", RangeValues.attribute(upper));
        }
        if (value != null) {
            Number current = value.getObject();
            if (current == null) tag.remove("value");
            else {
                double upper = Double.parseDouble(tag.getAttribute("max") == null ? "1" : tag.getAttribute("max"));
                tag.put("value", RangeValues.attribute(RangeValues.clamp(RangeValues.finite(current, "Progress value"), 0, upper)));
            }
        }
        Variant appearance = variant == null ? null : variant.getObject();
        if (appearance == null || appearance == Variant.DEFAULT) tag.remove("data-variant");
        else tag.put("data-variant", appearance.name().toLowerCase(java.util.Locale.ROOT));
        if (label != null) { String name = label.getObject(); if (name != null) tag.put("aria-label", name); }
    }
    @Override public void detach(Component component) {
        if (value != null) value.detach(); if (max != null) max.detach();
        if (variant != null) variant.detach(); if (label != null) label.detach(); super.detach(component);
    }
}
