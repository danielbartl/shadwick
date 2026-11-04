package dev.jbaby.shadwick.behaviors;

import org.apache.wicket.Component;
import org.apache.wicket.markup.ComponentTag;
import org.apache.wicket.model.IModel;

import java.util.Objects;

/** Menu styling and semantics on a Wicket button/link; checked state is read from the server model. */
public class DropdownMenuItemBehavior extends StyledBehavior {
    public enum Variant { DEFAULT, DESTRUCTIVE }
    private final String role;
    private final IModel<Boolean> checked;
    private Variant variant = Variant.DEFAULT;
    private boolean inset;

    public DropdownMenuItemBehavior() { this("menuitem", "sw-dropdown-menu-item", null); }
    public DropdownMenuItemBehavior(Variant variant) { this(); setVariant(variant); }
    private DropdownMenuItemBehavior(String role, String css, IModel<Boolean> checked) {
        super(css); this.role = role; this.checked = checked;
    }
    public static DropdownMenuItemBehavior checkbox(IModel<Boolean> checked) {
        return new DropdownMenuItemBehavior("menuitemcheckbox", "sw-dropdown-menu-checkbox-item", Objects.requireNonNull(checked));
    }
    public static DropdownMenuItemBehavior radio(IModel<Boolean> checked) {
        return new DropdownMenuItemBehavior("menuitemradio", "sw-dropdown-menu-radio-item", Objects.requireNonNull(checked));
    }
    public DropdownMenuItemBehavior setVariant(Variant variant) { this.variant = Objects.requireNonNull(variant); return this; }
    public DropdownMenuItemBehavior setInset(boolean inset) { this.inset = inset; return this; }

    @Override public void onComponentTag(Component component, ComponentTag tag) {
        super.onComponentTag(component, tag);
        tag.put("role", role);
        if (checked != null) tag.put("aria-checked", String.valueOf(Boolean.TRUE.equals(checked.getObject())));
        if (checked == null && variant == Variant.DESTRUCTIVE) tag.put("data-variant", "destructive");
        if (inset) tag.put("data-inset", "true");
        if (!component.isEnabledInHierarchy()) {
            tag.put("aria-disabled", "true");
            if (tag.getName().equalsIgnoreCase("button") || tag.getName().equalsIgnoreCase("input")) tag.put("disabled", "disabled");
            if (tag.getName().equalsIgnoreCase("a")) tag.remove("href");
        }
    }

    @Override public void detach(Component component) { if (checked != null) checked.detach(); super.detach(component); }
}
