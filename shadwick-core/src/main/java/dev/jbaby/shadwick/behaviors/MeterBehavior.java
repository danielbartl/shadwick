package dev.jbaby.shadwick.behaviors;

import dev.jbaby.shadwick.util.RangeValues;
import org.apache.wicket.Component;
import org.apache.wicket.markup.ComponentTag;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;

/** Native meter styling with optional model-driven bounds and thresholds. */
public class MeterBehavior extends StyledBehavior {
    private IModel<? extends Number> value, min, max, low, high, optimum;
    private IModel<String> label;
    public MeterBehavior() { super("sw-meter"); }
    public MeterBehavior(IModel<? extends Number> value, IModel<? extends Number> min, IModel<? extends Number> max,
                         IModel<? extends Number> low, IModel<? extends Number> high, IModel<? extends Number> optimum) {
        this(); this.value = value; this.min = min; this.max = max; this.low = low; this.high = high; this.optimum = optimum;
    }
    public MeterBehavior setMin(IModel<? extends Number> value) { min = value; return this; }
    public MeterBehavior setMax(IModel<? extends Number> value) { max = value; return this; }
    public MeterBehavior setLow(IModel<? extends Number> value) { low = value; return this; }
    public MeterBehavior setHigh(IModel<? extends Number> value) { high = value; return this; }
    public MeterBehavior setOptimum(IModel<? extends Number> value) { optimum = value; return this; }
    public MeterBehavior setLabel(IModel<String> value) { label = value; return this; }
    private void attribute(ComponentTag tag, String name, IModel<? extends Number> model) {
        if (model == null) return;
        Number number = model.getObject();
        if (number == null) tag.remove(name); else tag.put(name, RangeValues.attribute(RangeValues.finite(number, "Meter " + name)));
    }
    private double number(ComponentTag tag, String name, double fallback) {
        String text = tag.getAttribute(name); return text == null ? fallback : RangeValues.finite(Double.valueOf(text), "Meter " + name);
    }
    @Override public void onComponentTag(Component component, ComponentTag tag) {
        super.onComponentTag(component, tag);
        if (!"meter".equals(tag.getName())) throw new IllegalArgumentException("MeterBehavior requires <meter>");
        attribute(tag, "min", min); attribute(tag, "max", max); attribute(tag, "low", low); attribute(tag, "high", high); attribute(tag, "optimum", optimum);
        double lower = number(tag, "min", 0), upper = number(tag, "max", 1);
        if (upper <= lower) throw new IllegalArgumentException("Meter maximum must be greater than its minimum");
        double lo = number(tag, "low", lower), hi = number(tag, "high", upper), best = number(tag, "optimum", lower / 2 + upper / 2);
        if (lo < lower || hi > upper || lo > hi || best < lower || best > upper)
            throw new IllegalArgumentException("Meter thresholds must satisfy min <= low <= high <= max, with optimum within bounds");
        Number reading = value == null ? null : value.getObject();
        double current = value == null ? number(tag, "value", lower) : reading == null ? lower : RangeValues.finite(reading, "Meter value");
        current = RangeValues.clamp(current, lower, upper); tag.put("value", RangeValues.attribute(current));
        // Match HTML's three native quality bands, including an optimum outside the low/high interval.
        String state = best < lo ? current <= lo ? "optimum" : current <= hi ? "suboptimum" : "critical"
                : best > hi ? current >= hi ? "optimum" : current >= lo ? "suboptimum" : "critical"
                : current >= lo && current <= hi ? "optimum" : "suboptimum";
        tag.put("data-state", state);
        if (label != null) { String name = label.getObject(); if (name != null) tag.put("aria-label", name); }
    }
    @Override public void detach(Component component) {
        for (IModel<?> model : new IModel<?>[]{value, min, max, low, high, optimum, label}) if (model != null) model.detach();
        super.detach(component);
    }
}
