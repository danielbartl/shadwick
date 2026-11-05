package dev.jbaby.shadwick.components;

import dev.jbaby.shadwick.behaviors.PaginationBehavior;
import dev.jbaby.shadwick.behaviors.StyledBehavior;
import dev.jbaby.shadwick.util.SerializableBiConsumer;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.list.ListItem;
import org.apache.wicket.markup.html.list.ListView;
import org.apache.wicket.model.IModel;
import java.util.List;
import java.util.Objects;

/** Caller-owned nav/menu/items markup, with OatPagination's list constructor and factory API. */
public abstract class SwPagination<T> extends WebMarkupContainer {
    private final PaginationBehavior appearance = new PaginationBehavior();
    public SwPagination(String id, IModel<List<T>> model) {
        super(id); add(appearance);
        var menu = new WebMarkupContainer("menu"); menu.add(new StyledBehavior("sw-pagination-content")); add(menu);
        menu.add(new ListView<T>("items", Objects.requireNonNull(model)) {
            @Override protected void populateItem(ListItem<T> item) {
                item.add(new StyledBehavior("sw-pagination-item")); SwPagination.this.populateItem(item);
            }
        });
    }
    protected abstract void populateItem(ListItem<T> item);
    public SwPagination<T> setLabel(String label) { appearance.setLabel(label); return this; }
    public SwPagination<T> setLabel(IModel<String> label) { appearance.setLabel(label); return this; }
    public static <T> SwPagination<T> of(String id, IModel<List<T>> model, SerializableBiConsumer<ListItem<T>, T> populateItem) {
        Objects.requireNonNull(populateItem);
        return new SwPagination<>(id, model) {
            @Override protected void populateItem(ListItem<T> item) { populateItem.accept(item, item.getModelObject()); }
        };
    }
}
