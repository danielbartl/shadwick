package dev.jbaby.shadwick.behaviors;

import dev.jbaby.shadwick.Shadwick;
import org.apache.wicket.Component;
import org.apache.wicket.markup.ComponentTag;
import org.apache.wicket.markup.head.IHeaderResponse;
import org.apache.wicket.markup.head.JavaScriptHeaderItem;

import java.util.Objects;

/** A caller-owned tabs root. Its tablist/triggers/panels supply the standard ARIA relationships and initial state. */
public class TabsBehavior extends StyledBehavior {
    public enum Orientation { HORIZONTAL, VERTICAL }
    public enum Activation { AUTOMATIC, MANUAL }
    public enum Variant { DEFAULT, LINE }
    private Orientation orientation = Orientation.HORIZONTAL;
    private Activation activation = Activation.AUTOMATIC;
    public TabsBehavior() { super("sw-tabs"); }
    public TabsBehavior setOrientation(Orientation orientation) { this.orientation = Objects.requireNonNull(orientation); return this; }
    public TabsBehavior setActivation(Activation activation) { this.activation = Objects.requireNonNull(activation); return this; }
    public Orientation getOrientation() { return orientation; }
    @Override public void onComponentTag(Component component, ComponentTag tag) {
        super.onComponentTag(component, tag);
        if (orientation == Orientation.VERTICAL) tag.put("data-orientation", "vertical"); else tag.remove("data-orientation");
        if (activation == Activation.MANUAL) tag.put("data-activation", "manual"); else tag.remove("data-activation");
    }
    @Override public void renderHead(Component component, IHeaderResponse response) {
        super.renderHead(component, response);
        response.render(JavaScriptHeaderItem.forReference(Shadwick.SHADWICK_JS));
    }
}
