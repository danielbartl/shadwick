package dev.jbaby.shadwick.components;

import dev.jbaby.shadwick.behaviors.StyledBehavior;
import dev.jbaby.shadwick.behaviors.TabsBehavior;
import dev.jbaby.shadwick.util.SerializableBiConsumer;
import org.apache.wicket.Component;
import org.apache.wicket.behavior.Behavior;
import org.apache.wicket.markup.ComponentTag;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.list.ListItem;
import org.apache.wicket.markup.html.list.ListView;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Oat-style tabs with caller-owned markup and paired populateTab/populatePanel lists.
 * Every panel renders on the server; the browser switches them without a request.
 * Override tabUrl for bookmarkable native links, and visibility/enabled hooks for both lists together.
 */
public abstract class SwTabs<T> extends WebMarkupContainer {
    private final IModel<List<T>> items;
    private final TabsBehavior appearance = new TabsBehavior();
    private final WebMarkupContainer tablist;
    private TabsBehavior.Variant variant = TabsBehavior.Variant.DEFAULT;
    private IModel<String> label;
    private List<T> renderItems = List.of();
    private boolean[] visible = new boolean[0], enabled = new boolean[0];
    private int selected, active = -1;

    public SwTabs(String id, IModel<List<T>> items) {
        super(id); this.items = OncePerRequestModel.of(Objects.requireNonNull(items));
        setOutputMarkupPlaceholderTag(true); add(appearance);
        tablist = new WebMarkupContainer("tablist");
        tablist.add(new StyledBehavior("sw-tabs-list") {
            @Override public void onComponentTag(Component component, ComponentTag tag) {
                super.onComponentTag(component, tag); tag.put("role", "tablist");
                if (label != null) tag.put("aria-label", label.getObject());
                if (appearance.getOrientation() == TabsBehavior.Orientation.VERTICAL) tag.put("aria-orientation", "vertical");
                else tag.remove("aria-orientation");
                if (variant == TabsBehavior.Variant.LINE) tag.put("data-variant", "line"); else tag.remove("data-variant");
            }
        }); add(tablist);
        IModel<List<T>> snapshot = () -> renderItems;
        tablist.add(new ListView<>("tabButtons", snapshot) {
            @Override protected void populateItem(ListItem<T> item) {
                int index = item.getIndex(); item.setVisible(visible[index]);
                item.setMarkupId(triggerId(index)); item.setOutputMarkupId(true);
                item.add(new Behavior() {
                    @Override public void onComponentTag(Component component, ComponentTag tag) {
                        CharSequence url = tabUrl(item.getModelObject(), index);
                        boolean available = enabled[index] && component.isEnabledInHierarchy();
                        tag.setName(url == null ? "button" : "a");
                        tag.append("class", "sw-tabs-trigger", " ");
                        tag.put("role", "tab"); tag.put("aria-controls", panelId(index));
                        tag.put("aria-selected", String.valueOf(index == active));
                        if (url == null) {
                            tag.put("type", "button"); tag.remove("href");
                            if (!available) tag.put("disabled", "disabled"); else tag.remove("disabled");
                        } else {
                            tag.remove("type"); tag.remove("disabled");
                            if (available) tag.put("href", url); else tag.remove("href");
                        }
                        if (!available) { tag.put("aria-disabled", "true"); tag.put("tabindex", "-1"); }
                        else { tag.remove("aria-disabled"); tag.remove("tabindex"); }
                    }
                });
                populateTab(item);
            }
        });
        add(new ListView<>("tabPanels", snapshot) {
            @Override protected void populateItem(ListItem<T> item) {
                int index = item.getIndex(); item.setVisible(visible[index]);
                item.setMarkupId(panelId(index)); item.setOutputMarkupId(true);
                item.add(new Behavior() {
                    @Override public void onComponentTag(Component component, ComponentTag tag) {
                        tag.append("class", "sw-tabs-content", " ");
                        tag.put("role", "tabpanel"); tag.put("aria-labelledby", triggerId(index));
                        tag.put("tabindex", "0");
                        if (index == active) tag.remove("hidden"); else tag.put("hidden", "hidden");
                    }
                });
                populatePanel(item);
            }
        });
    }
    private String triggerId(int index) { return getMarkupId() + "-tab-" + index; }
    private String panelId(int index) { return getMarkupId() + "-panel-" + index; }
    protected abstract void populateTab(ListItem<T> item);
    protected abstract void populatePanel(ListItem<T> item);
    protected boolean isTabVisible(T item, int index) { return true; }
    protected boolean isTabEnabled(T item, int index) { return true; }
    protected CharSequence tabUrl(T item, int index) { return null; }
    /** Configures the initial server selection, not the browser's current selection. */
    public SwTabs<T> setSelectedTab(int index) {
        if (index < 0) throw new IllegalArgumentException("Selected tab index must be non-negative");
        selected = index; return this;
    }
    public SwTabs<T> setOrientation(TabsBehavior.Orientation orientation) { appearance.setOrientation(orientation); return this; }
    public SwTabs<T> setActivation(TabsBehavior.Activation activation) { appearance.setActivation(activation); return this; }
    public SwTabs<T> setVariant(TabsBehavior.Variant variant) { this.variant = Objects.requireNonNull(variant); return this; }
    public SwTabs<T> setLabel(String label) { return setLabel(Model.of(label)); }
    public SwTabs<T> setLabel(IModel<String> label) { this.label = OncePerRequestModel.of(label); return this; }
    public WebMarkupContainer getTabList() { return tablist; }
    @Override protected void onConfigure() {
        super.onConfigure();
        List<T> values = items.getObject(); renderItems = values != null ? new ArrayList<>(values) : List.of();
        visible = new boolean[renderItems.size()]; enabled = new boolean[renderItems.size()]; active = -1;
        int firstVisible = -1;
        for (int i = 0; i < renderItems.size(); i++) {
            visible[i] = isTabVisible(renderItems.get(i), i);
            enabled[i] = isTabEnabled(renderItems.get(i), i) && isEnabledInHierarchy();
            if (visible[i] && firstVisible < 0) firstVisible = i;
            if (visible[i] && enabled[i] && (active < 0 || i == selected)) active = i;
        }
        if (active < 0) active = firstVisible;
        setVisible(firstVisible >= 0);
    }
    @Override protected void onDetach() {
        super.onDetach(); items.detach(); if (label != null) label.detach(); renderItems = List.of();
    }
    public static <T> SwTabs<T> of(String id, IModel<List<T>> items, SerializableBiConsumer<ListItem<T>, T> tab,
                                    SerializableBiConsumer<ListItem<T>, T> panel) {
        Objects.requireNonNull(tab); Objects.requireNonNull(panel);
        return new SwTabs<>(id, items) {
            @Override protected void populateTab(ListItem<T> item) { tab.accept(item, item.getModelObject()); }
            @Override protected void populatePanel(ListItem<T> item) { panel.accept(item, item.getModelObject()); }
        };
    }
}
