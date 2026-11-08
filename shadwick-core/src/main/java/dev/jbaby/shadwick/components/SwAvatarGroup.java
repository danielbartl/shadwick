package dev.jbaby.shadwick.components;

import dev.jbaby.shadwick.behaviors.AvatarGroupBehavior;
import dev.jbaby.shadwick.behaviors.StyledBehavior;
import dev.jbaby.shadwick.util.SerializableBiConsumer;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.list.ListItem;
import org.apache.wicket.markup.html.list.ListView;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.LoadableDetachableModel;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * An overlapping group repeating caller-owned {@code avatars} markup. Add a {@link SwAvatar},
 * optionally inside a named native link/button, in {@link #populateItem(ListItem)}. For example:
 * {@code <ul wicket:id="group"><li wicket:id="avatars"><span wicket:id="avatar"></span></li></ul>}.
 * Members retain their order and ordinary focus behavior; large groups wrap. The application owns
 * any visible subset, overflow count and member authorization. Null lists render empty.
 * @param <T> member type
 */
public abstract class SwAvatarGroup<T> extends WebMarkupContainer {
    public enum Size {
        SMALL("sm"), DEFAULT(null), LARGE("lg");
        private final String className;
        Size(String className) { this.className = className; }
        public String getClassName() { return className; }
    }
    private final AvatarGroupBehavior appearance = new AvatarGroupBehavior();
    private final IModel<List<T>> members;

    public SwAvatarGroup(String id, IModel<List<T>> model) { this(id, model, Size.DEFAULT); }
    public SwAvatarGroup(String id, IModel<List<T>> model, Size size) {
        super(id);
        members = Objects.requireNonNull(model);
        add(appearance.setSize(size));
        IModel<List<T>> snapshot = new LoadableDetachableModel<>() {
            @Override protected List<T> load() {
                List<T> values = members.getObject();
                return values == null ? List.of() : new ArrayList<>(values);
            }
        };
        add(new ListView<T>("avatars", snapshot) {
            @Override protected void populateItem(ListItem<T> item) {
                item.add(new StyledBehavior("sw-avatar-group-item"));
                SwAvatarGroup.this.populateItem(item);
            }
        });
    }
    protected abstract void populateItem(ListItem<T> item);
    /** Group size governs all nested avatars, including an application-supplied overflow avatar. */
    public SwAvatarGroup<T> setSize(Size size) { appearance.setSize(size); return this; }
    public SwAvatarGroup<T> setLabel(String label) { appearance.setLabel(label); return this; }
    public SwAvatarGroup<T> setLabel(IModel<String> label) { appearance.setLabel(label); return this; }
    @Override protected void onDetach() {
        try { members.detach(); } finally { super.onDetach(); }
    }
    public static <T> SwAvatarGroup<T> of(String id, IModel<List<T>> model, SerializableBiConsumer<ListItem<T>, T> populateItem) {
        return of(id, model, Size.DEFAULT, populateItem);
    }
    public static <T> SwAvatarGroup<T> of(String id, IModel<List<T>> model, Size size, SerializableBiConsumer<ListItem<T>, T> populateItem) {
        Objects.requireNonNull(populateItem);
        return new SwAvatarGroup<T>(id, model, size) {
            @Override protected void populateItem(ListItem<T> item) { populateItem.accept(item, item.getModelObject()); }
        };
    }
}
