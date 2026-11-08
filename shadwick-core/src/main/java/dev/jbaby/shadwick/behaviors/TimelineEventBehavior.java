package dev.jbaby.shadwick.behaviors;

import org.apache.wicket.Component;
import org.apache.wicket.markup.ComponentTag;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;

import java.util.Locale;
import java.util.Objects;

/** A decorative event marker. Communicate event status in the event's text as well as its color. */
public class TimelineEventBehavior extends StyledBehavior {
    public enum Variant { DEFAULT, SECONDARY, OUTLINE, DESTRUCTIVE }
    private IModel<Variant> variant = Model.of(Variant.DEFAULT);

    public TimelineEventBehavior() { super("sw-timeline-event"); }
    public TimelineEventBehavior setVariant(Variant variant) { return setVariant(Model.of(Objects.requireNonNull(variant))); }
    public TimelineEventBehavior setVariant(IModel<Variant> variant) { this.variant = Objects.requireNonNull(variant); return this; }

    @Override public void onComponentTag(Component component, ComponentTag tag) {
        if (!tag.getName().equalsIgnoreCase("li"))
            throw new IllegalArgumentException("TimelineEventBehavior requires an <li> element");
        super.onComponentTag(component, tag);
        Variant value = variant.getObject();
        if (value == null || value == Variant.DEFAULT) tag.remove("data-variant");
        else tag.put("data-variant", value.name().toLowerCase(Locale.ROOT));
    }
    @Override public void detach(Component component) { variant.detach(); super.detach(component); }
}
