package dev.jbaby.shadwick.components;

import com.github.openjson.JSONObject;
import dev.jbaby.shadwick.behaviors.ButtonBehavior;
import dev.jbaby.shadwick.behaviors.DropdownMenuBehavior;
import dev.jbaby.shadwick.behaviors.DropdownMenuItemBehavior;
import dev.jbaby.shadwick.util.SerializableBiConsumer;
import dev.jbaby.shadwick.util.SerializableFunction;
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

import java.util.List;
import java.util.Objects;

/**
 * A native dropdown action menu with OatDropdown's constructor and factory API.
 * Each repeated button has a "label" child supplied by populateItem. For navigation links or
 * grouped/checked content, compose a menu with DropdownMenuBehavior and DropdownMenuItemBehavior.
 * Replacing the component over Ajax resets it to closed. The browser owns its open state.
 */
public abstract class SwDropdown<T> extends Panel {
    private final WebMarkupContainer trigger;
    private final WebMarkupContainer menu;
    private final DropdownMenuBehavior appearance;
    private final ButtonBehavior triggerStyle = new ButtonBehavior(ButtonBehavior.Variant.OUTLINE);

    public SwDropdown(String id, String triggerLabel, IModel<List<T>> model) { this(id, Model.of(triggerLabel), model); }
    public SwDropdown(String id, IModel<String> triggerLabel, IModel<List<T>> model) {
        super(id);
        Objects.requireNonNull(triggerLabel); Objects.requireNonNull(model);
        setOutputMarkupId(true);
        add(AttributeModifier.append("class", "sw-dropdown"));
        trigger = new WebMarkupContainer("trigger") {
            @Override public boolean isEnabled() {
                List<T> values = model.getObject();
                return super.isEnabled() && values != null && !values.isEmpty();
            }
        };
        trigger.add(new Label("triggerLabel", triggerLabel));
        trigger.add(triggerStyle);
        add(trigger);
        menu = new WebMarkupContainer("menu");
        appearance = new DropdownMenuBehavior(trigger);
        menu.add(appearance);
        menu.add(new ListView<T>("items", model) {
            @Override protected void populateItem(ListItem<T> item) {
                item.add(new DropdownMenuItemBehavior());
                SwDropdown.this.populateItem(item);
            }
        });
        add(menu);
    }

    /** Populate each button, including its "label" child; add callbacks, visibility and enabled state as needed. */
    protected abstract void populateItem(ListItem<T> item);
    public WebMarkupContainer getTrigger() { return trigger; }
    public WebMarkupContainer getMenu() { return menu; }
    public SwDropdown<T> setSide(DropdownMenuBehavior.Side side) { appearance.setSide(side); return this; }
    public SwDropdown<T> setAlign(DropdownMenuBehavior.Align align) { appearance.setAlign(align); return this; }
    public SwDropdown<T> setTriggerVariant(ButtonBehavior.Variant variant) { triggerStyle.setVariant(variant); return this; }
    public SwDropdown<T> close(IPartialPageRequestHandler target) {
        target.appendJavaScript("Shadwick.closePopover(" + JSONObject.quote(menu.getMarkupId()) + ");");
        return this;
    }

    public static <T> SwDropdown<T> of(String id, String triggerLabel, IModel<List<T>> model,
                                      SerializableBiConsumer<ListItem<T>, T> populateItem) {
        return of(id, Model.of(triggerLabel), model, populateItem);
    }
    public static <T> SwDropdown<T> of(String id, IModel<String> triggerLabel, IModel<List<T>> model,
                                      SerializableBiConsumer<ListItem<T>, T> populateItem) {
        Objects.requireNonNull(populateItem);
        return new SwDropdown<>(id, triggerLabel, model) {
            @Override protected void populateItem(ListItem<T> item) { populateItem.accept(item, item.getModelObject()); }
        };
    }
    public static <T> SwDropdown<T> of(String id, String triggerLabel, IModel<List<T>> model,
                                      SerializableFunction<T, String> label, SerializableBiConsumer<AjaxRequestTarget, T> onClick) {
        return of(id, Model.of(triggerLabel), model, label, onClick);
    }
    public static <T> SwDropdown<T> of(String id, IModel<String> triggerLabel, IModel<List<T>> model,
                                      SerializableFunction<T, String> label, SerializableBiConsumer<AjaxRequestTarget, T> onClick) {
        Objects.requireNonNull(label); Objects.requireNonNull(onClick);
        return new SwDropdown<>(id, triggerLabel, model) {
            @Override protected void populateItem(ListItem<T> item) {
                item.add(new Label("label", (IModel<String>) () -> label.apply(item.getModelObject())));
                item.add(AjaxEventBehavior.onEvent("click", target -> {
                    if (!item.isEnabledInHierarchy()) return;
                    onClick.accept(target, item.getModelObject());
                    close(target);
                }));
            }
        };
    }
}
