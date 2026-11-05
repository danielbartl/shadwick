package dev.jbaby.shadwick.behaviors;

import dev.jbaby.shadwick.Shadwick;
import org.apache.wicket.Component;
import org.apache.wicket.markup.ComponentTag;
import org.apache.wicket.markup.head.IHeaderResponse;
import org.apache.wicket.markup.head.JavaScriptHeaderItem;
import org.apache.wicket.resource.loader.ComponentStringResourceLoader;

import java.util.Locale;
import java.util.Objects;

/** One native nav/popover: desktop column or icon rail, and a mobile panel. */
public class SidebarBehavior extends StyledBehavior {

    public enum Side { START, END }
    public enum Variant { SIDEBAR, FLOATING, INSET }
    public enum Collapsible { OFFCANVAS, ICON, NONE }
    public enum Placement { SIDEBAR, HEADER }

    private Side side = Side.START;
    private Variant variant = Variant.SIDEBAR;
    private Collapsible collapsible = Collapsible.OFFCANVAS;
    private Placement placement = Placement.SIDEBAR;

    public SidebarBehavior() {
        super("sw-sidebar");
    }

    @Override
    public void bind(Component component) {
        super.bind(component);
        component.setOutputMarkupId(true);
    }

    public SidebarBehavior setSide(Side side) {
        this.side = Objects.requireNonNull(side);
        return this;
    }

    public SidebarBehavior setVariant(Variant variant) {
        this.variant = Objects.requireNonNull(variant);
        return this;
    }

    public SidebarBehavior setCollapsible(Collapsible collapsible) {
        this.collapsible = Objects.requireNonNull(collapsible);
        return this;
    }

    public SidebarBehavior setPlacement(Placement placement) {
        this.placement = Objects.requireNonNull(placement);
        return this;
    }

    @Override
    public void onComponentTag(Component component, ComponentTag tag) {
        super.onComponentTag(component, tag);
        tag.put("popover", "auto");
        tag.put("tabindex", "-1");
        if (placement == Placement.HEADER) tag.put("data-placement", "header");
        else tag.remove("data-placement");
        if (side == Side.END) tag.put("data-side", "right");
        else tag.remove("data-side");
        if (variant != Variant.SIDEBAR) tag.put("data-variant", variant.name().toLowerCase(Locale.ROOT));
        else tag.remove("data-variant");
        if (collapsible != Collapsible.OFFCANVAS) tag.put("data-collapsible", collapsible.name().toLowerCase(Locale.ROOT));
        else tag.remove("data-collapsible");
        if (tag.getAttribute("aria-label") == null && tag.getAttribute("aria-labelledby") == null) {
            String fallback = new ComponentStringResourceLoader().loadStringResource(SidebarBehavior.class,
                    "SidebarBehavior.label", component.getLocale(), component.getStyle(), component.getVariation());
            tag.put("aria-label", component.getString("SidebarBehavior.label", null, fallback));
        }
    }

    @Override
    public void renderHead(Component component, IHeaderResponse response) {
        super.renderHead(component, response);
        response.render(JavaScriptHeaderItem.forReference(Shadwick.SHADWICK_JS));
    }
}
