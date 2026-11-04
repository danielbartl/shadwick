package dev.jbaby.shadwick.components.form;

import dev.jbaby.shadwick.behaviors.ToggleBehavior;
import dev.jbaby.shadwick.components.SwIcon;
import org.apache.wicket.AttributeModifier;
import org.apache.wicket.markup.html.TransparentWebMarkupContainer;
import org.apache.wicket.markup.html.form.CheckBox;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;

/**
 * A native checkbox drawn as a toggle button with its label wrapped around it
 * ({@link CheckBox}). An unchecked input submits false. As with Wicket CheckBox,
 * setRequired does not enforce checked; use a Boolean validator for consent.
 */
public class SwToggle extends BaseSwField<Boolean, CheckBox, SwToggle> {
    private final ToggleBehavior style = new ToggleBehavior();
    private boolean iconOnly;

    /**
     * Label looked up by {@code id} in the {@code .properties} files, model inherited
     * from a parent {@code CompoundPropertyModel}.
     */
    public SwToggle(String id) {
        this(id, null, null, null);
    }

    /** Label looked up by {@code id} in the {@code .properties} files. */
    public SwToggle(String id, IModel<Boolean> model) {
        this(id, null, model, null);
    }

    /** Model inherited from a parent CompoundPropertyModel. */
    public SwToggle(String id, String label) {
        this(id, Model.of(label), null, null);
    }

    public SwToggle(String id, String label, IModel<Boolean> model) {
        this(id, Model.of(label), model, null);
    }

    public SwToggle(String id, IModel<String> label, IModel<Boolean> model) {
        this(id, label, model, null);
    }

    public SwToggle(String id, IModel<String> label, IModel<Boolean> model, IModel<String> description) {
        super(id, label, model, description);
        TransparentWebMarkupContainer toggle = new TransparentWebMarkupContainer("toggle");
        toggle.add(style);
        add(toggle);
        add(SwIcon.ofNullable("icon", null));
        get("label").add(AttributeModifier.replace("class", (IModel<String>) () -> iconOnly ? "sw-toggle-caption-hidden" : null));
    }

    public SwToggle setVariant(ToggleBehavior.Variant variant) { style.setVariant(variant); return this; }
    public SwToggle setSize(ToggleBehavior.Size size) { style.setSize(size); return this; }

    /** A decorative icon; the text label remains the control's accessible name. */
    public SwToggle setIcon(String icon) { addOrReplace(SwIcon.ofNullable("icon", icon)); return this; }
    public SwToggle setIconOnly(boolean iconOnly) { this.iconOnly = iconOnly; return this; }

    @Override
    protected void onConfigure() {
        super.onConfigure();
        if (iconOnly && !get("icon").isVisible()) throw new IllegalStateException("An icon-only toggle needs an icon");
    }

    @Override
    protected CheckBox newInput(String id, IModel<Boolean> model) {
        return new CheckBox(id, model);
    }
}
