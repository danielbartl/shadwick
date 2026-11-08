package dev.jbaby.shadwick.components;

import dev.jbaby.shadwick.behaviors.ButtonGroupBehavior;
import dev.jbaby.shadwick.behaviors.StyledBehavior;
import dev.jbaby.shadwick.util.SerializableBiConsumer;
import org.apache.wicket.Component;
import org.apache.wicket.markup.ComponentTag;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.list.ListItem;
import org.apache.wicket.markup.html.list.ListView;
import org.apache.wicket.model.IModel;

import java.util.List;
import java.util.Objects;

/**
 * A connected group repeating caller-owned {@code items} markup. Add a styled button/link in populateItem.
 * Use {@code <menu wicket:id="group"><li wicket:id="items"><button wicket:id="button"></button></li></menu>}.
 * For fixed controls, attach {@link ButtonGroupBehavior} directly to their parent instead.
 * @param <T> item type
 */
public abstract class SwButtonGroup<T> extends WebMarkupContainer {
    private final ButtonGroupBehavior appearance = new ButtonGroupBehavior();

    public SwButtonGroup(String id, IModel<List<T>> model) {
        super(id);
        add(appearance);
        add(new ListView<T>("items", Objects.requireNonNull(model)) {
            @Override protected void populateItem(ListItem<T> item) {
                item.add(new StyledBehavior("sw-button-group-item") {
                    @Override public void onComponentTag(Component component, ComponentTag tag) {
                        super.onComponentTag(component, tag);
                        // The parent is an action group, so an li wrapper must not claim list-item semantics.
                        if (tag.getName().equalsIgnoreCase("li")) tag.put("role", "presentation");
                    }
                });
                SwButtonGroup.this.populateItem(item);
            }
        });
    }
    /** Add the item's button/link; visibility and enabled state remain caller-owned. */
    protected abstract void populateItem(ListItem<T> item);
    public SwButtonGroup<T> setOrientation(ButtonGroupBehavior.Orientation orientation) {
        appearance.setOrientation(orientation); return this;
    }
    public SwButtonGroup<T> setLabel(String label) { appearance.setLabel(label); return this; }
    public SwButtonGroup<T> setLabel(IModel<String> label) { appearance.setLabel(label); return this; }

    public static <T> SwButtonGroup<T> of(String id, IModel<List<T>> model,
                                         SerializableBiConsumer<ListItem<T>, T> populateItem) {
        Objects.requireNonNull(populateItem);
        return new SwButtonGroup<>(id, model) {
            @Override protected void populateItem(ListItem<T> item) { populateItem.accept(item, item.getModelObject()); }
        };
    }
}
