package dev.jbaby.shadwick.components.form;

import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;

import java.time.LocalDateTime;

/**
 * A shadcn-styled form field wrapping a native {@code <input type="datetime-local">},
 * bound to a {@link LocalDateTime} model.
 */
public class SwDateTimeLocalField extends BaseSwInputField<LocalDateTime, Html5TextField<LocalDateTime>, SwDateTimeLocalField> {

    /**
     * Label looked up by {@code id} in the {@code .properties} files, model inherited
     * from a parent {@code CompoundPropertyModel}.
     */
    public SwDateTimeLocalField(String id) {
        this(id, null, null, null);
    }

    /** Label looked up by {@code id} in the {@code .properties} files. */
    public SwDateTimeLocalField(String id, IModel<LocalDateTime> model) {
        this(id, null, model, null);
    }

    /** Label supplied explicitly; model inherited from a CompoundPropertyModel. */
    public SwDateTimeLocalField(String id, String label) {
        this(id, Model.of(label), null, null);
    }

    public SwDateTimeLocalField(String id, String label, IModel<LocalDateTime> model) {
        this(id, Model.of(label), model, null);
    }

    public SwDateTimeLocalField(String id, IModel<String> label, IModel<LocalDateTime> model) {
        this(id, label, model, null);
    }

    public SwDateTimeLocalField(String id, IModel<String> label, IModel<LocalDateTime> model, IModel<String> description) {
        super(id, label, model, description);
    }

    @Override
    protected Html5TextField<LocalDateTime> newInput(String id, IModel<LocalDateTime> model) {
        return new Html5TextField<>(id, model, LocalDateTime.class, "datetime-local", Html5Converters.dateTimeLocal());
    }
}
