package dev.jbaby.shadwick.components.form;

import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;

/**
 * A shadcn-styled native month input, bound to a {@code YYYY-MM} string.
 * Submitted values are checked for valid HTML month syntax and calendar values.
 */
public class SwMonthField extends BaseSwInputField<String, Html5TextField<String>, SwMonthField> {

    /**
     * Label looked up by {@code id} in the {@code .properties} files, model inherited
     * from a parent {@code CompoundPropertyModel}.
     */
    public SwMonthField(String id) {
        this(id, null, null, null);
    }

    /** Label looked up by {@code id} in the {@code .properties} files. */
    public SwMonthField(String id, IModel<String> model) {
        this(id, null, model, null);
    }

    /** Label supplied explicitly; model inherited from a CompoundPropertyModel. */
    public SwMonthField(String id, String label) {
        this(id, Model.of(label), null, null);
    }

    public SwMonthField(String id, String label, IModel<String> model) {
        this(id, Model.of(label), model, null);
    }

    public SwMonthField(String id, IModel<String> label, IModel<String> model) {
        this(id, label, model, null);
    }

    public SwMonthField(String id, IModel<String> label, IModel<String> model, IModel<String> description) {
        super(id, label, model, description);
    }

    @Override
    protected Html5TextField<String> newInput(String id, IModel<String> model) {
        return new Html5TextField<>(id, model, String.class, "month", Html5Converters.month());
    }
}
