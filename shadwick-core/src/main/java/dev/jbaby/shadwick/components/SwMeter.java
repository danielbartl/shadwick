package dev.jbaby.shadwick.components;

import dev.jbaby.shadwick.behaviors.MeterBehavior;
import org.apache.wicket.markup.html.WebComponent;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;

/** Native meter for a known measurement (not task progress). Default bounds are 0 and 1. */
public class SwMeter extends WebComponent {
    private final MeterBehavior appearance;
    public SwMeter(String id, Number value) { this(id, Model.of(value)); }
    public SwMeter(String id, IModel<? extends Number> value) { this(id, value, Model.of(0), Model.of(1), null, null, null); }
    public SwMeter(String id, IModel<? extends Number> value, IModel<? extends Number> min, IModel<? extends Number> max,
                   IModel<? extends Number> low, IModel<? extends Number> high, IModel<? extends Number> optimum) {
        super(id); appearance = new MeterBehavior(value == null ? Model.of() : value, min, max, low, high, optimum);
        add(appearance); setOutputMarkupId(true);
    }
    public SwMeter setMin(Number value) { return setMin(Model.of(value)); }
    public SwMeter setMin(IModel<? extends Number> value) { appearance.setMin(value); return this; }
    public SwMeter setMax(Number value) { return setMax(Model.of(value)); }
    public SwMeter setMax(IModel<? extends Number> value) { appearance.setMax(value); return this; }
    public SwMeter setLow(Number value) { return setLow(Model.of(value)); }
    public SwMeter setLow(IModel<? extends Number> value) { appearance.setLow(value); return this; }
    public SwMeter setHigh(Number value) { return setHigh(Model.of(value)); }
    public SwMeter setHigh(IModel<? extends Number> value) { appearance.setHigh(value); return this; }
    public SwMeter setOptimum(Number value) { return setOptimum(Model.of(value)); }
    public SwMeter setOptimum(IModel<? extends Number> value) { appearance.setOptimum(value); return this; }
    public SwMeter setLabel(String value) { return setLabel(Model.of(value)); }
    public SwMeter setLabel(IModel<String> value) { appearance.setLabel(value); return this; }
}
