package dev.jbaby.shadwick.behaviors;

import org.apache.wicket.Component;
import org.apache.wicket.markup.ComponentTag;

import java.util.Objects;

/** Places an addon before/after the control, or in a full-width row above/below it. */
public class InputGroupAddonBehavior extends StyledBehavior {
    public enum Align {
        INLINE_START(null), INLINE_END("inline-end"), BLOCK_START("block-start"), BLOCK_END("block-end");
        private final String value;
        Align(String value) { this.value = value; }
    }

    private Align align = Align.INLINE_START;
    private boolean border;

    public InputGroupAddonBehavior() { super("sw-input-group-addon"); }
    public InputGroupAddonBehavior setAlign(Align align) { this.align = Objects.requireNonNull(align); return this; }
    public InputGroupAddonBehavior setBorder(boolean border) { this.border = border; return this; }

    @Override
    public void onComponentTag(Component component, ComponentTag tag) {
        super.onComponentTag(component, tag);
        if (align.value != null) tag.put("data-align", align.value); else tag.remove("data-align");
        tag.remove("data-border");
        if (border && align == Align.BLOCK_START) tag.put("data-border", "bottom");
        if (border && align == Align.BLOCK_END) tag.put("data-border", "top");
    }
}
