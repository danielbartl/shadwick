package dev.jbaby.shadwick.behaviors;

import org.apache.wicket.Component;
import org.apache.wicket.markup.ComponentTag;

import java.util.Objects;

/** Styles a native checkbox as a switch. The browser owns its checked state and keyboard interaction. */
public class SwitchBehavior extends StyledBehavior {

    public enum Size { DEFAULT, SMALL }

    private Size size = Size.DEFAULT;

    public SwitchBehavior() {
        super("sw-switch");
    }

    public SwitchBehavior setSize(Size size) {
        this.size = Objects.requireNonNull(size);
        return this;
    }

    @Override
    public void onComponentTag(Component component, ComponentTag tag) {
        super.onComponentTag(component, tag);
        tag.put("role", "switch");
        // Native checkbox semantics expose checked directly; a separate aria-checked could become stale.
        tag.remove("aria-checked");
        if (size == Size.SMALL) {
            tag.put("data-size", "sm");
        } else {
            tag.remove("data-size");
        }
    }
}
