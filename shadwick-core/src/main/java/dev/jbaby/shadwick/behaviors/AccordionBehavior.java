package dev.jbaby.shadwick.behaviors;

import org.apache.wicket.Component;
import org.apache.wicket.markup.ComponentTag;
import org.apache.wicket.markup.html.list.ListItem;
import org.apache.wicket.markup.html.list.ListView;

import java.util.UUID;

/** Native exclusive details groups. Attach to a ListView and its items, or set a shared name on caller-owned items. */
public class AccordionBehavior extends StyledBehavior {
    private boolean exclusive;
    private String name;
    private transient String generated;

    public AccordionBehavior() { this(true); }
    public AccordionBehavior(boolean exclusive) { super("sw-accordion-item"); this.exclusive = exclusive; }
    public boolean isExclusive() { return exclusive; }
    public AccordionBehavior setExclusive(boolean exclusive) { this.exclusive = exclusive; return this; }
    public AccordionBehavior setName(String name) {
        if (name != null && name.isBlank()) throw new IllegalArgumentException("An accordion name must not be blank");
        this.name = name; return this;
    }
    @Override public void beforeRender(Component component) {
        if (component instanceof ListView<?> || !(component instanceof ListItem<?>)) generated = "sw-accordion-" + UUID.randomUUID();
        super.beforeRender(component);
    }
    protected String getGroupName(Component component) {
        if (name != null) return name;
        if (generated == null) generated = "sw-accordion-" + UUID.randomUUID();
        return generated;
    }
    @Override public void onComponentTag(Component component, ComponentTag tag) {
        if (!"details".equals(tag.getName())) throw new IllegalArgumentException("Accordion items require <details> markup");
        super.onComponentTag(component, tag);
        if (exclusive) tag.put("name", getGroupName(component)); else tag.remove("name");
        if (!component.isEnabledInHierarchy()) tag.put("data-disabled", "true"); else tag.remove("data-disabled");
    }
}
