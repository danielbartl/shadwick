package dev.jbaby.shadwick.behaviors;

import org.apache.wicket.Component;
import org.apache.wicket.markup.ComponentTag;
import java.util.Objects;

/** Styles a container whose content uses sw-card-header, sw-card-content and sw-card-footer. */
public class CardBehavior extends StyledBehavior {
    public enum Size { DEFAULT, SMALL }
    private Size size = Size.DEFAULT;

    public CardBehavior() { super("sw-card"); }

    public CardBehavior setSize(Size size) {
        this.size = Objects.requireNonNull(size);
        return this;
    }

    @Override
    public void onComponentTag(Component component, ComponentTag tag) {
        super.onComponentTag(component, tag);
        if (size == Size.SMALL) { tag.put("data-size", "sm"); }
        else { tag.remove("data-size"); }
    }
}
