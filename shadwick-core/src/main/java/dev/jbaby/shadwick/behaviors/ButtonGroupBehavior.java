package dev.jbaby.shadwick.behaviors;

import org.apache.wicket.Component;
import org.apache.wicket.markup.ComponentTag;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;

import java.util.Locale;
import java.util.Objects;

/** Connected buttons/links on caller-owned markup. Controls keep their ordinary Tab order and semantics. */
public class ButtonGroupBehavior extends StyledBehavior {
    public enum Orientation { HORIZONTAL, VERTICAL }
    private Orientation orientation = Orientation.HORIZONTAL;
    private IModel<String> label;

    public ButtonGroupBehavior() { super("sw-button-group"); }

    public ButtonGroupBehavior setOrientation(Orientation orientation) {
        this.orientation = Objects.requireNonNull(orientation); return this;
    }
    public ButtonGroupBehavior setLabel(String label) { return setLabel(Model.of(label)); }
    public ButtonGroupBehavior setLabel(IModel<String> label) { this.label = label; return this; }

    @Override public void onComponentTag(Component component, ComponentTag tag) {
        super.onComponentTag(component, tag);
        tag.put("role", "group");
        tag.put("data-orientation", orientation.name().toLowerCase(Locale.ROOT));
        if (label != null) tag.put("aria-label", label.getObject());
    }
    @Override public void detach(Component component) {
        if (label != null) label.detach();
        super.detach(component);
    }
}
