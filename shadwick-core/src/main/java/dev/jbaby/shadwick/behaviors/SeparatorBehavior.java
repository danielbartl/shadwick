package dev.jbaby.shadwick.behaviors;

import org.apache.wicket.Component;
import org.apache.wicket.markup.ComponentTag;
import java.util.Objects;

/** A horizontal or vertical divider, decorative by default. A vertical divider needs a flex parent. */
public class SeparatorBehavior extends StyledBehavior {
    public enum Orientation { HORIZONTAL, VERTICAL }
    private Orientation orientation = Orientation.HORIZONTAL;
    private boolean decorative = true;

    public SeparatorBehavior() { super("sw-separator"); }

    public SeparatorBehavior setOrientation(Orientation orientation) {
        this.orientation = Objects.requireNonNull(orientation);
        return this;
    }

    public SeparatorBehavior setDecorative(boolean decorative) { this.decorative = decorative; return this; }

    @Override
    public void onComponentTag(Component component, ComponentTag tag) {
        super.onComponentTag(component, tag);
        tag.put("role", decorative ? "none" : "separator");
        tag.remove("data-orientation");
        tag.remove("aria-orientation");
        if (orientation == Orientation.VERTICAL) {
            tag.put("data-orientation", "vertical");
            if (!decorative) { tag.put("aria-orientation", "vertical"); }
        }
    }
}
