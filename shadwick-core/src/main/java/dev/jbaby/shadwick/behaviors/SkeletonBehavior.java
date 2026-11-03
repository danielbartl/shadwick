package dev.jbaby.shadwick.behaviors;

import org.apache.wicket.Component;
import org.apache.wicket.markup.ComponentTag;
import java.util.Objects;

/** A decorative loading placeholder; put an accessible loading status on the surrounding region. */
public class SkeletonBehavior extends StyledBehavior {
    public enum Shape { LINE, BOX, CIRCLE }
    private final Shape shape;

    public SkeletonBehavior() { this(Shape.LINE); }
    public SkeletonBehavior(Shape shape) { super("sw-skeleton"); this.shape = Objects.requireNonNull(shape); }

    @Override
    public void onComponentTag(Component component, ComponentTag tag) {
        super.onComponentTag(component, tag);
        tag.put("aria-hidden", "true");
        tag.put("data-shape", shape.name().toLowerCase(java.util.Locale.ROOT));
    }
}
