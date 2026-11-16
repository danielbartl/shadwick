package dev.jbaby.shadwick.components.form;

import org.apache.wicket.Component;
import org.apache.wicket.behavior.Behavior;
import org.apache.wicket.markup.ComponentTag;
import org.apache.wicket.markup.html.form.CheckBox;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;

/**
 * A shadcn-styled form field wrapping a native {@code <input type="checkbox">}
 * ({@link CheckBox}). An unchecked input submits false. As with Wicket CheckBox,
 * setRequired does not enforce checked; use a Boolean validator for consent.
 * <p>
 * {@link #setIndeterminate(IModel)} shows the mixed state ("some of a group selected") with a minus. HTML has no
 * attribute for it, only the DOM property, so the input carries {@code data-sw-indeterminate} and Shadwick's script
 * sets the property on load and after Ajax updates; assistive technology then reports the box as mixed. A click
 * clears it, as on every checkbox, and the form still submits only checked or unchecked: recompute the mixed state on
 * the server when you re-render.
 */
public class SwCheckBox extends BaseSwField<Boolean, CheckBox, SwCheckBox> {

    private IndeterminateState indeterminate;

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

    /** Starts the box in the mixed state, or not. */
    public SwCheckBox setIndeterminate(boolean indeterminate) {
        return setIndeterminate(Model.of(indeterminate));
    }

    /**
     * Whether the box shows the mixed state, read on every render: for example, true while only some of a group's
     * boxes are checked. Null counts as false. The model is detached with the field.
     */
    public SwCheckBox setIndeterminate(IModel<Boolean> indeterminate) {
        if (this.indeterminate == null) {
            this.indeterminate = new IndeterminateState();
            getInput().add(this.indeterminate);
        }
        this.indeterminate.model = indeterminate;
        return this;
    }

    /** Whether the box renders in the mixed state. */
    public boolean isIndeterminate() {
        return indeterminate != null && indeterminate.isOn();
    }

    /** Writes the marker Shadwick's script turns into the DOM property; removes it when the state is off. */
    private static final class IndeterminateState extends Behavior {
        private IModel<Boolean> model;

        boolean isOn() {
            return model != null && Boolean.TRUE.equals(model.getObject());
        }

        @Override
        public void onComponentTag(Component component, ComponentTag tag) {
            super.onComponentTag(component, tag);
            if (isOn()) tag.put("data-sw-indeterminate", "true");
            else tag.remove("data-sw-indeterminate");
        }

        @Override
        public void detach(Component component) {
            if (model != null) model.detach();
            super.detach(component);
        }
    }
}
