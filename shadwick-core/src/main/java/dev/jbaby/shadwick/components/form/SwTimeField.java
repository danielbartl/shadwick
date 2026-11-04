package dev.jbaby.shadwick.components.form;

import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;

import java.time.LocalTime;

/**
 * A shadcn-styled form field wrapping a native {@code <input type="time">},
 * bound to a {@link LocalTime} model.
 */
public class SwTimeField extends BaseSwInputField<LocalTime, Html5TextField<LocalTime>, SwTimeField> {

    /**
     * Label looked up by {@code id} in the {@code .properties} files, model inherited
     * from a parent {@code CompoundPropertyModel}.
     */
    public SwTimeField(String id) {
        this(id, null, null, null);
    }

    /** Label looked up by {@code id} in the {@code .properties} files. */
    public SwTimeField(String id, IModel<LocalTime> model) {
        this(id, null, model, null);
    }

    /** Label supplied explicitly; model inherited from a CompoundPropertyModel. */
    public SwTimeField(String id, String label) {
        this(id, Model.of(label), null, null);
    }

    public SwTimeField(String id, String label, IModel<LocalTime> model) {
        this(id, Model.of(label), model, null);
    }

    public SwTimeField(String id, IModel<String> label, IModel<LocalTime> model) {
        this(id, label, model, null);
    }

    public SwTimeField(String id, IModel<String> label, IModel<LocalTime> model, IModel<String> description) {
        super(id, label, model, description);
    }

    @Override
    protected Html5TextField<LocalTime> newInput(String id, IModel<LocalTime> model) {
        return new Html5TextField<>(id, model, LocalTime.class, "time", Html5Converters.time());
    }
}
