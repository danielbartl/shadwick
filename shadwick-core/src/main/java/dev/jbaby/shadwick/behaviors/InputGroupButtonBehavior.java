package dev.jbaby.shadwick.behaviors;

import org.apache.wicket.Component;
import org.apache.wicket.markup.ComponentTag;
import org.apache.wicket.model.IModel;

/** A small ghost button inside an input-group addon, with the usual button accessibility checks. */
public class InputGroupButtonBehavior extends ButtonBehavior {
    public InputGroupButtonBehavior() { super(Variant.GHOST); super.setSize(Size.SMALL); }

    @Override
    public void onComponentTag(Component component, ComponentTag tag) {
        super.onComponentTag(component, tag);
        tag.append("class", "sw-input-group-button", " ");
    }

    @Override public InputGroupButtonBehavior setVariant(Variant variant) { super.setVariant(variant); return this; }
    @Override public InputGroupButtonBehavior setVariant(IModel<Variant> variant) { super.setVariant(variant); return this; }
    @Override public InputGroupButtonBehavior setSize(Size size) { super.setSize(size); return this; }
    @Override public InputGroupButtonBehavior setSize(IModel<Size> size) { super.setSize(size); return this; }
}
