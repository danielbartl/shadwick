package dev.jbaby.shadwick.behaviors;

import dev.jbaby.shadwick.Shadwick;
import org.apache.wicket.AttributeModifier;
import org.apache.wicket.Component;
import org.apache.wicket.behavior.Behavior;
import org.apache.wicket.markup.ComponentTag;
import org.apache.wicket.markup.head.IHeaderResponse;
import org.apache.wicket.markup.head.JavaScriptHeaderItem;
import org.apache.wicket.model.IModel;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Objects;

/** A native action menu on caller-owned markup, connected to a button trigger. Use one instance per menu. */
public class DropdownMenuBehavior extends StyledBehavior {
    public enum Side { TOP, BOTTOM, START, END }
    public enum Align { START, CENTER, END }
    private final Component trigger;
    private final List<Behavior> triggerAttributes = new ArrayList<>();
    private Component owner;
    private Side side = Side.BOTTOM;
    private Align align = Align.START;

    public DropdownMenuBehavior(Component trigger) {
        super("sw-dropdown-menu-content");
        this.trigger = Objects.requireNonNull(trigger);
    }

    @Override public void bind(Component component) {
        super.bind(component);
        if (owner != null) throw new IllegalStateException("Use one DropdownMenuBehavior per menu");
        owner = component;
        component.setOutputMarkupId(true);
        trigger.setOutputMarkupId(true);
        triggerAttributes.add(AttributeModifier.replace("popovertarget", (IModel<String>) component::getMarkupId));
        triggerAttributes.add(AttributeModifier.replace("aria-controls", (IModel<String>) component::getMarkupId));
        triggerAttributes.add(AttributeModifier.replace("aria-haspopup", "menu"));
        triggerAttributes.add(AttributeModifier.replace("aria-expanded", "false"));
        triggerAttributes.add(AttributeModifier.replace("disabled", (IModel<String>) () ->
                !component.isEnabledInHierarchy() || !trigger.isEnabledInHierarchy() ? "disabled" : null));
        trigger.add(triggerAttributes.toArray(Behavior[]::new));
    }

    @Override public void unbind(Component component) {
        triggerAttributes.forEach(trigger::remove);
        triggerAttributes.clear(); owner = null;
        super.unbind(component);
    }

    @Override public void onComponentTag(Component component, ComponentTag tag) {
        super.onComponentTag(component, tag);
        tag.put("role", "menu"); tag.put("popover", "auto"); tag.put("tabindex", "-1");
        tag.put("aria-labelledby", trigger.getMarkupId());
        tag.put("data-side", switch (side) {
            case START -> "left"; case END -> "right"; default -> side.name().toLowerCase(Locale.ROOT);
        });
        tag.put("data-align", align.name().toLowerCase(Locale.ROOT));
    }

    public DropdownMenuBehavior setSide(Side side) { this.side = Objects.requireNonNull(side); return this; }
    public DropdownMenuBehavior setAlign(Align align) { this.align = Objects.requireNonNull(align); return this; }

    @Override public void renderHead(Component component, IHeaderResponse response) {
        super.renderHead(component, response);
        response.render(JavaScriptHeaderItem.forReference(Shadwick.SHADWICK_JS));
    }
}
