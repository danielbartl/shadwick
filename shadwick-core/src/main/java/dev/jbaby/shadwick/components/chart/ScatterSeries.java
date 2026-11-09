package dev.jbaby.shadwick.components.chart;

import dev.jbaby.shadwick.util.SerializableFunction;
import java.io.Serializable;
import java.util.List;
import java.util.Objects;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;

/** An independently modeled set of named numeric coordinate pairs. */
public final class ScatterSeries<T> implements Serializable {
    public enum Shape { CIRCLE, SQUARE, DIAMOND }
    final IModel<String> label;
    final IModel<? extends List<T>> data;
    final SerializableFunction<T, String> name;
    final SerializableFunction<T, ? extends Number> x, y;
    int color;
    double radius = 4;
    Shape shape = Shape.CIRCLE;

    private ScatterSeries(IModel<String> label, IModel<? extends List<T>> data, SerializableFunction<T, String> name,
                          SerializableFunction<T, ? extends Number> x, SerializableFunction<T, ? extends Number> y) {
        this.label = Objects.requireNonNull(label);
        this.data = Objects.requireNonNull(data);
        this.name = Objects.requireNonNull(name);
        this.x = Objects.requireNonNull(x);
        this.y = Objects.requireNonNull(y);
    }

    public static <T> ScatterSeries<T> of(String label, IModel<? extends List<T>> data, SerializableFunction<T, String> name,
                                         SerializableFunction<T, ? extends Number> x, SerializableFunction<T, ? extends Number> y) {
        return of(Model.of(label), data, name, x, y);
    }

    public static <T> ScatterSeries<T> of(IModel<String> label, IModel<? extends List<T>> data, SerializableFunction<T, String> name,
                                         SerializableFunction<T, ? extends Number> x, SerializableFunction<T, ? extends Number> y) {
        return new ScatterSeries<>(label, data, name, x, y);
    }

    /** Palette token 1–5; zero automatically selects the next token (default). */
    public ScatterSeries<T> setColor(int value) {
        if (value < 0 || value > 5) throw new IllegalArgumentException("Scatter color must be between 0 and 5");
        color = value;
        return this;
    }

    public ScatterSeries<T> setShape(Shape value) {
        shape = Objects.requireNonNull(value);
        return this;
    }

    /** Marker radius in native pixels, from 2 to 12; default four. */
    public ScatterSeries<T> setPointRadius(double value) {
        if (!Double.isFinite(value) || value < 2 || value > 12)
            throw new IllegalArgumentException("Scatter point radius must be between 2 and 12");
        radius = value;
        return this;
    }

    void detach() {
        label.detach();
        data.detach();
    }
}
