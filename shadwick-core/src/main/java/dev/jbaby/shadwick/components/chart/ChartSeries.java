package dev.jbaby.shadwick.components.chart;

import dev.jbaby.shadwick.util.SerializableFunction;
import java.io.Serializable;
import java.util.Objects;
import org.apache.wicket.model.*;

/** A named series. Null numbers leave gaps; an optional stack groups bars or areas by type. */
public class ChartSeries<T> implements Serializable {
    public enum Type { BAR, LINE, AREA }
    public enum Curve { LINEAR, STEP, MONOTONE }
    final Type type;
    final IModel<String> label;
    final SerializableFunction<T, ? extends Number> value;
    String stack;
    boolean dashed, dots;
    int color;
    double opacity = .35;
    Curve curve = Curve.MONOTONE;
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

    public ChartSeries<T> setDots(boolean value) {
        dots = value;
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
        return this;
    }

    public ChartSeries<T> setFillOpacity(double value) {
        if (!Double.isFinite(value) || value < 0 || value > 1)
            throw new IllegalArgumentException("fill opacity must be 0–1");
        opacity = value;
        return this;
    }
}
