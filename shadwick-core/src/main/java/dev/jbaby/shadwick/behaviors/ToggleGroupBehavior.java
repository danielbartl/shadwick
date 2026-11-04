package dev.jbaby.shadwick.behaviors;

import org.apache.wicket.Component;
import org.apache.wicket.markup.ComponentTag;

import java.util.Objects;

/** Layout for a named group of native toggle labels; the caller supplies group/radiogroup semantics. */
public class ToggleGroupBehavior extends StyledBehavior {
    public enum Orientation { HORIZONTAL, VERTICAL }
    private Orientation orientation = Orientation.HORIZONTAL;
    private int spacing = 2;

    public ToggleGroupBehavior() { super("sw-toggle-group"); }

    public ToggleGroupBehavior setOrientation(Orientation orientation) {
        this.orientation = Objects.requireNonNull(orientation);
        return this;
    }

    /** 0 joins the items, 1–4 use the corresponding spacing step. */
    public ToggleGroupBehavior setSpacing(int spacing) {
        if (spacing < 0 || spacing > 4) throw new IllegalArgumentException("Toggle spacing must be between 0 and 4");
        this.spacing = spacing;
        return this;
    }

    @Override
    public void onComponentTag(Component component, ComponentTag tag) {
        super.onComponentTag(component, tag);
        if (orientation == Orientation.VERTICAL) tag.put("data-orientation", "vertical"); else tag.remove("data-orientation");
        if (spacing != 2) tag.put("data-spacing", spacing); else tag.remove("data-spacing");
    }
}
