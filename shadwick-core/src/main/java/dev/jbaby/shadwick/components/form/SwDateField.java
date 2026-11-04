package dev.jbaby.shadwick.components.form;

import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;

import java.time.LocalDate;

/**
 * A shadcn-styled form field wrapping a native {@code <input type="date">},
 * bound to a {@link LocalDate} model.
 */
public class SwDateField extends BaseSwInputField<LocalDate, Html5TextField<LocalDate>, SwDateField> {

    /**
     * Label looked up by {@code id} in the {@code .properties} files, model inherited
     * from a parent {@code CompoundPropertyModel}.
     */
    public SwDateField(String id) {
        this(id, null, null, null);
    }

    /** Label looked up by {@code id} in the {@code .properties} files. */
    public SwDateField(String id, IModel<LocalDate> model) {
        this(id, null, model, null);
    }

    /** Label supplied explicitly; model inherited from a CompoundPropertyModel. */
    public SwDateField(String id, String label) {
        this(id, Model.of(label), null, null);
    }

    public SwDateField(String id, String label, IModel<LocalDate> model) {
        this(id, Model.of(label), model, null);
    }

    public SwDateField(String id, IModel<String> label, IModel<LocalDate> model) {
        this(id, label, model, null);
    }

    public SwDateField(String id, IModel<String> label, IModel<LocalDate> model, IModel<String> description) {
        super(id, label, model, description);
    }

    @Override
    protected Html5TextField<LocalDate> newInput(String id, IModel<LocalDate> model) {
        return new Html5TextField<>(id, model, LocalDate.class, "date", Html5Converters.date());
    }
}
