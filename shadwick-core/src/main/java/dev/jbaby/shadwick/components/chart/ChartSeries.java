package dev.jbaby.shadwick.components.chart;

import dev.jbaby.shadwick.util.SerializableFunction;
import java.io.Serializable;
import java.util.Objects;
import org.apache.wicket.model.*;

/** A named series. Null numbers leave gaps; an optional stack groups bars or areas by type. */
public class ChartSeries<T> implements Serializable {
    public enum Type { BAR, LINE, AREA }
    public enum Curve { LINEAR, STEP, MONOTONE, NATURAL, STEP_BEFORE, STEP_AFTER, STEP_MIDDLE }
    public enum ValueLabels { NONE, LAST, HIGHEST_AND_LAST, ALL }
    final Type type;
    final IModel<String> label;
    final SerializableFunction<T, ? extends Number> value;
    String stack;
    ChartSeries<T> join;
    boolean dashed, dots;
    int color;
    String lightColor, darkColor;
    double[] barRadius;
    double strokeWidth = 2;
    double opacity = .35;
    Curve curve = Curve.MONOTONE;
    ValueLabels valueLabels = ValueLabels.NONE;
    private ChartSeries(Type type, IModel<String> label, SerializableFunction<T, ? extends Number> value) {
        this.type = type;
        this.label = Objects.requireNonNull(label);
        this.value = Objects.requireNonNull(value);
    }

    public static <T> ChartSeries<T> bar(String label, SerializableFunction<T, ? extends Number> value) {
        return bar(Model.of(label), value);
    }

    public static <T> ChartSeries<T> bar(IModel<String> label, SerializableFunction<T, ? extends Number> value) {
        return new ChartSeries<>(Type.BAR, label, value);
    }

    public static <T> ChartSeries<T> line(String label, SerializableFunction<T, ? extends Number> value) {
        return line(Model.of(label), value);
    }

    public static <T> ChartSeries<T> line(IModel<String> label, SerializableFunction<T, ? extends Number> value) {
        return new ChartSeries<>(Type.LINE, label, value);
    }

    public static <T> ChartSeries<T> area(String label, SerializableFunction<T, ? extends Number> value) {
        return area(Model.of(label), value);
    }

    public static <T> ChartSeries<T> area(IModel<String> label, SerializableFunction<T, ? extends Number> value) {
        return new ChartSeries<>(Type.AREA, label, value);
    }

    public ChartSeries<T> setStack(String value) {
        stack = value;
        return this;
    }

    public ChartSeries<T> setDashed(boolean value) {
        dashed = value;
        return this;
    }

    /** Continue a source line/area at its last real value before this dashed series starts. Null clears the join. */
    public ChartSeries<T> setJoin(ChartSeries<T> source) {
        join = source;
        return this;
    }

    public ChartSeries<T> setDots(boolean value) {
        dots = value;
        return this;
    }

    /** Select displayed values from this series. Labels use the chart formatter; nulls are skipped. */
    public ChartSeries<T> setValueLabels(ValueLabels value) {
        valueLabels = Objects.requireNonNull(value);
        return this;
    }

    public ChartSeries<T> setCurve(Curve value) {
        curve = Objects.requireNonNull(value);
        return this;
    }
    /** The chart token, from 1 to 5. Zero selects the next token automatically. */
    public ChartSeries<T> setColor(int value) {
        if (value < 0 || value > 5)
            throw new IllegalArgumentException("color must be 0–5");
        color = value;
        lightColor = darkColor = null;
        return this;
    }

    /** A CSS color for both modes. Use setColor(int) to restore the chart palette. */
    public ChartSeries<T> setColor(String value) {
        return setColors(value, value);
    }

    /** Independent light/dark CSS colors. A null dark color uses the light color. */
    public ChartSeries<T> setColors(String light, String dark) {
        String checkedLight = ChartAppearance.color(light), checkedDark = dark == null ? checkedLight : ChartAppearance.color(dark);
        lightColor = checkedLight; darkColor = checkedDark;
        return this;
    }

    /** Line/area stroke width in pixels, from 0 to 20; the default is 2. */
    public ChartSeries<T> setStrokeWidth(double value) {
        if (!Double.isFinite(value) || value < 0 || value > 20)
            throw new IllegalArgumentException("Stroke width must be between 0 and 20");
        strokeWidth = value;
        return this;
    }

    /** A bar's four corner radii in native pixels, in either chart orientation. */
    public ChartSeries<T> setBarRadius(double value) {
        return setBarRadius(value, value, value, value);
    }

    /** Physical top-left, top-right, bottom-right and bottom-left radii, including in RTL. */
    public ChartSeries<T> setBarRadius(double topLeft, double topRight, double bottomRight, double bottomLeft) {
        if (type != Type.BAR) throw new IllegalStateException("Bar radius applies only to bar series");
        double[] values = {topLeft, topRight, bottomRight, bottomLeft};
        for (double value : values)
            if (!Double.isFinite(value) || value < 0 || value > 1000)
                throw new IllegalArgumentException("Bar radii must be between 0 and 1000");
        barRadius = values;
        return this;
    }

    public ChartSeries<T> setFillOpacity(double value) {
        if (!Double.isFinite(value) || value < 0 || value > 1)
            throw new IllegalArgumentException("fill opacity must be 0–1");
        opacity = value;
        return this;
    }
}
