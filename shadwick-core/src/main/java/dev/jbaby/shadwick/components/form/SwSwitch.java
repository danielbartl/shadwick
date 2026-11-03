package dev.jbaby.shadwick.components.form;

import dev.jbaby.shadwick.behaviors.SwitchBehavior;
import org.apache.wicket.markup.html.form.CheckBox;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;

/**
 * A shadcn-styled form field wrapping a native {@code <input type="checkbox">}
 * ({@link CheckBox}) that renders as a shadcn toggle switch ({@code role="switch"})
 * via {@link SwitchBehavior}, instead of a plain checkbox like {@link SwCheckBox}.
 */
public class SwSwitch extends BaseSwField<Boolean, CheckBox, SwSwitch> {

    private final SwitchBehavior behavior = new SwitchBehavior();

    /**
     * Label looked up by {@code id} in the {@code .properties} files, model inherited
     * from a parent {@code CompoundPropertyModel}.
     */
    public SwSwitch(String id) {
        this(id, null, null, null);
    }

    /** Label looked up by {@code id} in the {@code .properties} files. */
    public SwSwitch(String id, IModel<Boolean> model) {
        this(id, null, model, null);
    }

    /** Model inherited from a parent CompoundPropertyModel. */
    public SwSwitch(String id, String label) {
        this(id, Model.of(label), null, null);
    }

    public SwSwitch(String id, String label, IModel<Boolean> model) {
        this(id, Model.of(label), model, null);
    }

    public SwSwitch(String id, IModel<String> label, IModel<Boolean> model) {
        this(id, label, model, null);
    }

    public SwSwitch(String id, IModel<String> label, IModel<Boolean> model, IModel<String> description) {
        super(id, label, model, description);
    }

    /** The track's size; DEFAULT unless SMALL is selected. */
    public SwSwitch setSize(SwitchBehavior.Size size) {
        behavior.setSize(size);
        return this;
    }

    @Override
    protected CheckBox newInput(String id, IModel<Boolean> model) {
        CheckBox checkBox = new CheckBox(id, model);
        checkBox.add(behavior);
        return checkBox;
    }
}
