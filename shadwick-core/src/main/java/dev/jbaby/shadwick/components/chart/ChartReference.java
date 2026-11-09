package dev.jbaby.shadwick.components.chart;

import java.io.Serializable;
import java.util.Objects;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;

/** A named numeric target or range. Null model values omit the reference for that request. */
public final class ChartReference implements Serializable {
    final IModel<String> label;
    final IModel<? extends Number> lower, upper;
    int color;
    boolean visible = true, extendDomain = true, dashed = true;
    double opacity = .12;

    private ChartReference(IModel<String> label, IModel<? extends Number> lower, IModel<? extends Number> upper) {
        this.label = Objects.requireNonNull(label);
        this.lower = Objects.requireNonNull(lower);
        this.upper = upper;
    }

    public static ChartReference line(String label, Number value) {
        return line(Model.of(label), Model.of(value));
    }

    public static ChartReference line(String label, IModel<? extends Number> value) {
        return line(Model.of(label), value);
    }

    public static ChartReference line(IModel<String> label, IModel<? extends Number> value) {
        return new ChartReference(label, value, null);
    }

    public static ChartReference band(String label, Number lower, Number upper) {
        return band(Model.of(label), Model.of(lower), Model.of(upper));
    }

    public static ChartReference band(String label, IModel<? extends Number> lower, IModel<? extends Number> upper) {
        return band(Model.of(label), lower, upper);
    }

    public static ChartReference band(IModel<String> label, IModel<? extends Number> lower, IModel<? extends Number> upper) {
        return new ChartReference(label, lower, Objects.requireNonNull(upper));
    }

    /** Zero uses a neutral color (default); 1–5 select chart palette tokens. */
    public ChartReference setColor(int value) {
        if (value < 0 || value > 5) throw new IllegalArgumentException("Reference color must be between 0 and 5");
        color = value;
        return this;
    }

    public ChartReference setVisible(boolean value) {
        visible = value;
        return this;
    }

    /** Include this reference in an absolute chart's scale (default true). False clips to that scale. */
    public ChartReference setExtendDomain(boolean value) {
        extendDomain = value;
        return this;
    }

    /** Reference lines are dashed by default. */
    public ChartReference setDashed(boolean value) {
        if (upper != null) throw new IllegalStateException("Dashed styling applies only to reference lines");
        dashed = value;
        return this;
    }

    /** Reference band fill opacity, from 0 to 1; default 0.12. */
    public ChartReference setFillOpacity(double value) {
        if (upper == null) throw new IllegalStateException("Fill opacity applies only to reference bands");
        if (!Double.isFinite(value) || value < 0 || value > 1)
            throw new IllegalArgumentException("Reference fill opacity must be between 0 and 1");
        opacity = value;
        return this;
    }

    void detach() {
        label.detach();
        lower.detach();
        if (upper != null && upper != lower) upper.detach();
    }
}
