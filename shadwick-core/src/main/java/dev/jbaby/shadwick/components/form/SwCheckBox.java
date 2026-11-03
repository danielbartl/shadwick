package dev.jbaby.shadwick.components.form;

import org.apache.wicket.markup.html.form.CheckBox;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;

/**
 * A shadcn-styled form field wrapping a native {@code <input type="checkbox">}
 * ({@link CheckBox}). An unchecked input submits false. As with Wicket CheckBox,
 * setRequired does not enforce checked; use a Boolean validator for consent.
 */
public class SwCheckBox extends BaseSwField<Boolean, CheckBox, SwCheckBox> {

    /**
     * Label looked up by {@code id} in the {@code .properties} files, model inherited
     * from a parent {@code CompoundPropertyModel}.
     */
    public SwCheckBox(String id) {
        this(id, null, null, null);
    }

    /** Label looked up by {@code id} in the {@code .properties} files. */
    public SwCheckBox(String id, IModel<Boolean> model) {
        this(id, null, model, null);
    }

    /** Model inherited from a parent CompoundPropertyModel. */
    public SwCheckBox(String id, String label) {
        this(id, Model.of(label), null, null);
    }

    public SwCheckBox(String id, String label, IModel<Boolean> model) {
        this(id, Model.of(label), model, null);
    }

    public SwCheckBox(String id, IModel<String> label, IModel<Boolean> model) {
        this(id, label, model, null);
    }

    public SwCheckBox(String id, IModel<String> label, IModel<Boolean> model, IModel<String> description) {
        super(id, label, model, description);
    }

    @Override
    protected CheckBox newInput(String id, IModel<Boolean> model) {
        return new CheckBox(id, model);
    }
}
