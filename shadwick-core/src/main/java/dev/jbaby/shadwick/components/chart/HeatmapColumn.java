package dev.jbaby.shadwick.components.chart;

import dev.jbaby.shadwick.util.SerializableFunction;
import java.io.Serializable;
import java.util.Objects;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;

/** Named measurements in one column of a heatmap's shared color scale. */
public final class HeatmapColumn<T> implements Serializable {
    final IModel<String> label;
    final SerializableFunction<T, ? extends Number> value;

    private HeatmapColumn(IModel<String> label, SerializableFunction<T, ? extends Number> value) {
        this.label = Objects.requireNonNull(label);
        this.value = Objects.requireNonNull(value);
    }
    public static <T> HeatmapColumn<T> of(String label, SerializableFunction<T, ? extends Number> value) {
        return of(Model.of(label), value);
    }
    public static <T> HeatmapColumn<T> of(IModel<String> label, SerializableFunction<T, ? extends Number> value) {
        return new HeatmapColumn<>(label, value);
    }
}
