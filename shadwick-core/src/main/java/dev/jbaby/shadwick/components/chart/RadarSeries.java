package dev.jbaby.shadwick.components.chart;

import dev.jbaby.shadwick.util.SerializableFunction;
import java.io.Serializable;
import java.util.Objects;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;

/** Named measurements on a radar chart's shared dimensions and numeric scale. */
public final class RadarSeries<T> implements Serializable {
    final IModel<String> label;
    final SerializableFunction<T, ? extends Number> value;
    int color;
    boolean dashed, dots = true;
    double opacity = .15, strokeWidth = 2;

    private RadarSeries(IModel<String> label, SerializableFunction<T, ? extends Number> value) {
        this.label = Objects.requireNonNull(label);
        this.value = Objects.requireNonNull(value);
    }

    public static <T> RadarSeries<T> of(String label, SerializableFunction<T, ? extends Number> value) {
        return of(Model.of(label), value);
    }

    public static <T> RadarSeries<T> of(IModel<String> label, SerializableFunction<T, ? extends Number> value) {
        return new RadarSeries<>(label, value);
    }

    /** Palette token 1–5; zero selects the next token automatically (default). */
    public RadarSeries<T> setColor(int value) {
        if (value < 0 || value > 5) throw new IllegalArgumentException("Radar color must be between 0 and 5");
        color = value;
        return this;
    }

    public RadarSeries<T> setDashed(boolean value) { dashed = value; return this; }
    public RadarSeries<T> setDots(boolean value) { dots = value; return this; }

    /** Fill opacity for complete polygons, from 0 to 1; default 0.15. */
    public RadarSeries<T> setFillOpacity(double value) {
        if (!Double.isFinite(value) || value < 0 || value > 1)
            throw new IllegalArgumentException("Radar fill opacity must be between 0 and 1");
        opacity = value;
        return this;
    }

    /** Outline width in native pixels, from 0 to 20; default two. */
    public RadarSeries<T> setStrokeWidth(double value) {
        if (!Double.isFinite(value) || value < 0 || value > 20)
            throw new IllegalArgumentException("Radar stroke width must be between 0 and 20");
        strokeWidth = value;
        return this;
    }
}
