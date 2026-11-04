package dev.jbaby.shadwick.behaviors;

import org.apache.wicket.Component;
import org.apache.wicket.WicketRuntimeException;
import org.apache.wicket.behavior.Behavior;
import org.apache.wicket.markup.ComponentTag;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Objects;

/** Native fragment navigation on an anchor. A supplied component gets an id and, if needed, tabindex=-1. */
public class SkipLinkBehavior extends StyledBehavior {
    private final Component target;
    private final String targetId;
    private Behavior focusable;
    private Component owner;
    public SkipLinkBehavior() { this("main"); }
    public SkipLinkBehavior(String targetId) {
        super("sw-skip-link");
        if (Objects.requireNonNull(targetId).isBlank()) throw new IllegalArgumentException("A skip link needs a target id");
        this.targetId = targetId; target = null;
    }
    public SkipLinkBehavior(Component target) {
        super("sw-skip-link"); this.target = Objects.requireNonNull(target); targetId = null;
    }
    @Override public void bind(Component component) {
        super.bind(component);
        if (owner != null) throw new IllegalStateException("Use one SkipLinkBehavior per link");
        owner = component;
        if (target != null) {
            target.setOutputMarkupId(true);
            focusable = new Behavior() {
                @Override public void onComponentTag(Component component, ComponentTag tag) {
                    String name = tag.getName().toLowerCase(java.util.Locale.ROOT);
                    boolean nativeControl = switch (name) {
                        case "button", "input", "select", "textarea", "summary", "iframe" -> true;
                        case "a", "area" -> tag.getAttribute("href") != null;
                        default -> tag.getAttribute("contenteditable") != null && !"false".equals(tag.getAttribute("contenteditable"));
                    };
                    if (tag.getAttribute("tabindex") == null && !nativeControl) tag.put("tabindex", "-1");
                }
            };
            target.add(focusable);
        }
    }
    @Override public void unbind(Component component) {
        if (focusable != null) target.remove(focusable);
        focusable = null; owner = null;
        super.unbind(component);
    }
    @Override public void onComponentTag(Component component, ComponentTag tag) {
        super.onComponentTag(component, tag);
        if (!"a".equalsIgnoreCase(tag.getName())) throw new WicketRuntimeException("SkipLinkBehavior requires an <a> element");
        String id = target != null ? target.getMarkupId() : targetId;
        tag.put("href", "#" + URLEncoder.encode(id, StandardCharsets.UTF_8).replace("+", "%20"));
    }
}
