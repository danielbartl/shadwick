package dev.jbaby.shadwick.behaviors;

import org.apache.wicket.Component;
import org.apache.wicket.markup.ComponentTag;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;

import java.util.Arrays;
import java.util.Objects;
import java.util.stream.Collectors;

/** A named icon action beside a navigation item or group label. Reuses button accessibility checks. */
public class SidebarActionBehavior extends ButtonBehavior {
    public enum Kind { MENU, GROUP }
    private final Kind kind;
    private IModel<String> label;
    private boolean showOnHover;

    public SidebarActionBehavior(Kind kind) { this.kind = Objects.requireNonNull(kind); }
    public SidebarActionBehavior setLabel(String label) { return setLabel(Model.of(label)); }
    public SidebarActionBehavior setLabel(IModel<String> label) { this.label = label; return this; }
    public SidebarActionBehavior setShowOnHover(boolean show) { showOnHover = show; return this; }

    @Override public void onComponentTag(Component component, ComponentTag tag) {
        super.onComponentTag(component, tag);
        String classes = Arrays.stream(tag.getAttribute("class").split("\\s+"))
                .filter(value -> !value.equals("sw-button")).collect(Collectors.joining(" "));
        tag.put("class", classes);
        tag.append("class", kind == Kind.MENU ? "sw-sidebar-menu-action" : "sw-sidebar-group-action", " ");
        if (label != null) tag.put("aria-label", label.getObject());
        if (showOnHover && kind == Kind.MENU) tag.put("data-show-on-hover", "");
        if (tag.getName().equals("button")) {
            if (tag.getAttribute("type") == null) tag.put("type", "button");
            if (!component.isEnabledInHierarchy()) tag.put("disabled", "disabled");
        }
    }

    @Override public void detach(Component component) {
        if (label != null) label.detach();
        super.detach(component);
    }
}
