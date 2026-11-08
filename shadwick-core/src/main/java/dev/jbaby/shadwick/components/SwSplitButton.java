package dev.jbaby.shadwick.components;

import com.github.openjson.JSONObject;
import dev.jbaby.shadwick.behaviors.ButtonBehavior;
import dev.jbaby.shadwick.behaviors.ButtonGroupBehavior;
import dev.jbaby.shadwick.behaviors.DropdownMenuBehavior;
import dev.jbaby.shadwick.behaviors.DropdownMenuItemBehavior;
import dev.jbaby.shadwick.util.SerializableConsumer;
import org.apache.wicket.AttributeModifier;
import org.apache.wicket.ajax.AjaxEventBehavior;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.core.request.handler.IPartialPageRequestHandler;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.list.ListItem;
import org.apache.wicket.markup.html.list.ListView;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.model.StringResourceModel;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * An Ajax primary action joined to a native dropdown of related Ajax commands. Neither part submits a form.
 * Attach to {@code <div wicket:id="save"></div>}; addAction returns a configurable menu entry.
 * The browser owns menu state; replacing this panel over Ajax closes it. For validated form submission,
 * compose {@link SwSubmitButton} with {@link ButtonGroupBehavior} instead.
 */
public class SwSplitButton extends Panel {
    private final List<MenuAction> actions = new ArrayList<>();
    private final SwButton primary;
    private final WebMarkupContainer trigger, menu;
    private final ButtonBehavior triggerStyle = new ButtonBehavior();
    private final DropdownMenuBehavior menuStyle;

    public SwSplitButton(String id, String label, SerializableConsumer<AjaxRequestTarget> onClick) {
        this(id, Model.of(label), onClick);
    }
    public SwSplitButton(String id, IModel<String> label, SerializableConsumer<AjaxRequestTarget> onClick) {
        super(id);
        Objects.requireNonNull(label); Objects.requireNonNull(onClick);
        setOutputMarkupId(true);
        add(new ButtonGroupBehavior().setLabel((IModel<String>) label::getObject));
        add(AttributeModifier.append("class", "sw-split-button"));
        primary = SwButton.onClick("primary", label, onClick);
        add(primary);
        trigger = new WebMarkupContainer("trigger") {
            @Override public boolean isEnabled() {
                return super.isEnabled() && actions.stream().anyMatch(a -> a.visible && a.enabled);
            }
        };
        trigger.add(triggerStyle, AttributeModifier.replace("aria-label",
                new StringResourceModel("SwSplitButton.more", this).setDefaultValue("More actions")));
        add(trigger);
        menu = new WebMarkupContainer("menu");
        menuStyle = new DropdownMenuBehavior(trigger).setAlign(DropdownMenuBehavior.Align.END);
        menu.add(menuStyle);
        menu.add(new ListView<MenuAction>("items", actions) {
            @Override protected void populateItem(ListItem<MenuAction> item) {
                MenuAction action = item.getModelObject();
                item.add(new Label("label", (IModel<String>) action.label::getObject));
                item.add(new DropdownMenuItemBehavior() {
                    @Override public void onConfigure(org.apache.wicket.Component component) {
                        super.onConfigure(component);
                        component.setEnabled(action.enabled); component.setVisible(action.visible);
                        setVariant(action.variant);
                    }
                });
                item.add(AjaxEventBehavior.onEvent("click", target -> {
                    if (!item.isEnabledInHierarchy() || !action.enabled || !action.visible) return;
                    action.onClick.accept(target);
                    close(target);
                }));
            }
        });
        add(menu);
    }

    public MenuAction addAction(String label, SerializableConsumer<AjaxRequestTarget> onClick) {
        return addAction(Model.of(label), onClick);
    }
    public MenuAction addAction(IModel<String> label, SerializableConsumer<AjaxRequestTarget> onClick) {
        MenuAction action = new MenuAction(Objects.requireNonNull(label), Objects.requireNonNull(onClick));
        actions.add(action); return action;
    }
    /** Applies the same appearance to the primary action and the menu trigger. */
    public SwSplitButton setVariant(ButtonBehavior.Variant variant) {
        primary.setVariant(Objects.requireNonNull(variant)); triggerStyle.setVariant(variant); return this;
    }
    public SwSplitButton setSize(ButtonBehavior.Size size) {
        primary.setSize(Objects.requireNonNull(size)); triggerStyle.setSize(size); return this;
    }
    public SwSplitButton setSide(DropdownMenuBehavior.Side side) { menuStyle.setSide(side); return this; }
    public SwSplitButton setAlign(DropdownMenuBehavior.Align align) { menuStyle.setAlign(align); return this; }
    public SwButton getPrimaryButton() { return primary; }
    public WebMarkupContainer getTrigger() { return trigger; }
    public WebMarkupContainer getMenu() { return menu; }
    /** Close the browser menu after an action or from another Ajax callback. */
    public SwSplitButton close(IPartialPageRequestHandler target) {
        target.appendJavaScript("Shadwick.closePopover(" + JSONObject.quote(menu.getMarkupId()) + ");"); return this;
    }
    @Override protected void onDetach() {
        try { actions.forEach(action -> action.label.detach()); } finally { super.onDetach(); }
    }

    /** An action's label model and callback remain attached to this panel, including while invisible. */
    public static final class MenuAction implements Serializable {
        private final IModel<String> label;
        private final SerializableConsumer<AjaxRequestTarget> onClick;
        private DropdownMenuItemBehavior.Variant variant = DropdownMenuItemBehavior.Variant.DEFAULT;
        private boolean enabled = true, visible = true;
        private MenuAction(IModel<String> label, SerializableConsumer<AjaxRequestTarget> onClick) {
            this.label = label; this.onClick = onClick;
        }
        public MenuAction setVariant(DropdownMenuItemBehavior.Variant variant) {
            this.variant = Objects.requireNonNull(variant); return this;
        }
        public MenuAction setEnabled(boolean enabled) { this.enabled = enabled; return this; }
        public MenuAction setVisible(boolean visible) { this.visible = visible; return this; }
    }
}
