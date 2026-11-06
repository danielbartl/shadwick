package dev.jbaby.shadwick.components.table;

import dev.jbaby.shadwick.behaviors.ButtonBehavior;
import dev.jbaby.shadwick.behaviors.DropdownMenuBehavior;
import dev.jbaby.shadwick.behaviors.DropdownMenuItemBehavior;
import dev.jbaby.shadwick.behaviors.StyledBehavior;
import dev.jbaby.shadwick.components.SwDropdown;
import dev.jbaby.shadwick.util.SerializableBiConsumer;
import dev.jbaby.shadwick.util.SerializableFunction;
import dev.jbaby.shadwick.util.LocalizedText;
import dev.jbaby.shadwick.components.SwDataTable;
import org.apache.wicket.AttributeModifier;
import org.apache.wicket.Component;
import org.apache.wicket.ajax.AjaxEventBehavior;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.extensions.markup.html.repeater.data.grid.ICellPopulator;
import org.apache.wicket.extensions.markup.html.repeater.data.table.AbstractColumn;
import org.apache.wicket.extensions.markup.html.repeater.data.table.IStyledColumn;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.list.ListItem;
import org.apache.wicket.markup.repeater.Item;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/** Named per-row menus. Predicates are checked again on invocation; application callbacks own authorization. */
public class SwActionsColumn<T, S> extends AbstractColumn<T, S> implements IStyledColumn<T, S> {
    private final List<RowAction<T>> actions = new ArrayList<>();
    private SerializableFunction<T, String> rowLabel = row -> String.valueOf(row);
    private SerializableFunction<T, ?> rowKey;
    private boolean defaultTitle;
    public SwActionsColumn<T, S> setRowKey(SerializableFunction<T, ?> key) { rowKey = Objects.requireNonNull(key); return this; }
    public SwActionsColumn() { this(Model.of("Actions")); defaultTitle = true; }
    public SwActionsColumn(IModel<String> title) { super(title); }
    public SwActionsColumn<T, S> setRowLabel(SerializableFunction<T, String> label) { rowLabel = Objects.requireNonNull(label); return this; }
    public RowAction<T> addAction(String label, SerializableBiConsumer<AjaxRequestTarget, T> callback) { return addAction(Model.of(label), callback); }
    public RowAction<T> addAction(IModel<String> label, SerializableBiConsumer<AjaxRequestTarget, T> callback) {
        var action = new RowAction<>(Objects.requireNonNull(label), Objects.requireNonNull(callback)); actions.add(action); return action;
    }
    @Override public String getCssClass() { return "sw-table-actions sw-table-align-end"; }
    @Override public Component getHeader(String id) {
        Label header = new Label(id);
        header.setDefaultModel(defaultTitle ? (IModel<String>) () -> LocalizedText.get(header, SwDataTable.class, "SwDataTable.actionsHeading", "Actions") : getDisplayModel());
        return header.add(new StyledBehavior("sw-table-column-label"));
    }
    @Override public void populateItem(Item<ICellPopulator<T>> item, String id, IModel<T> row) {
        IModel<List<RowAction<T>>> visible = () -> actions.stream().filter(a -> row.getObject() != null && a.visible(row.getObject())).toList();
        var menu = new SwDropdown<RowAction<T>>(id, "⋯", visible) {
            @Override protected void populateItem(ListItem<RowAction<T>> entry) {
                var action = entry.getModelObject(); entry.add(new Label("label", action.label));
                entry.add(AttributeModifier.replace("data-variant", action.variant == DropdownMenuItemBehavior.Variant.DESTRUCTIVE ? "destructive" : null));
                entry.setEnabled(action.enabled(row.getObject()));
                entry.add(AjaxEventBehavior.onEvent("click", target -> {
                    T current = row.getObject();
                    if (current != null && action.visible(current) && action.enabled(current)) {
                        close(target); action.callback.accept(target, current); target.focusComponent(getTrigger());
                    } else { target.add(item); target.focusComponent(getTrigger()); }
                }));
            }
            @Override protected void onConfigure() { super.onConfigure(); setVisible(!visible.getObject().isEmpty()); }
            @Override protected void onBeforeRender() {
                if (rowKey != null) {
                    var table = findParent(org.apache.wicket.extensions.markup.html.repeater.data.table.DataTable.class);
                    String key = String.valueOf(rowKey.apply(row.getObject()));
                    try {
                        String digest = java.util.HexFormat.of().formatHex(java.security.MessageDigest.getInstance("SHA-256").digest(key.getBytes(java.nio.charset.StandardCharsets.UTF_8)));
                        String prefix = table.getMarkupId() + "-actions-" + table.getColumns().indexOf(SwActionsColumn.this) + "-" + digest;
                        getTrigger().setMarkupId(prefix + "-trigger"); getMenu().setMarkupId(prefix + "-menu");
                    } catch (java.security.NoSuchAlgorithmException impossible) { throw new IllegalStateException(impossible); }
                }
                super.onBeforeRender();
            }
        };
        menu.setTriggerVariant(ButtonBehavior.Variant.GHOST).setAlign(DropdownMenuBehavior.Align.END);
        menu.getTrigger().add(AttributeModifier.replace("data-size", "icon"));
        menu.getTrigger().add(AttributeModifier.replace("aria-label", (IModel<String>) () ->
                LocalizedText.get(menu, SwDataTable.class, "SwDataTable.actions", "Actions for") + " " + rowLabel.apply(row.getObject())));
        item.setOutputMarkupId(true); item.add(menu);
    }
    @Override public void detach() { super.detach(); actions.forEach(a -> a.label.detach()); }
    public static final class RowAction<T> implements Serializable {
        private final IModel<String> label;
        private final SerializableBiConsumer<AjaxRequestTarget, T> callback;
        private DropdownMenuItemBehavior.Variant variant = DropdownMenuItemBehavior.Variant.DEFAULT;
        private SerializableFunction<T, Boolean> visibleWhen, enabledWhen;
        private RowAction(IModel<String> label, SerializableBiConsumer<AjaxRequestTarget, T> callback) { this.label = label; this.callback = callback; }
        public RowAction<T> setVariant(DropdownMenuItemBehavior.Variant variant) { this.variant = Objects.requireNonNull(variant); return this; }
        public RowAction<T> setVisibleWhen(SerializableFunction<T, Boolean> predicate) { visibleWhen = Objects.requireNonNull(predicate); return this; }
        public RowAction<T> setEnabledWhen(SerializableFunction<T, Boolean> predicate) { enabledWhen = Objects.requireNonNull(predicate); return this; }
        private boolean visible(T row) { return visibleWhen == null || Boolean.TRUE.equals(visibleWhen.apply(row)); }
        private boolean enabled(T row) { return enabledWhen == null || Boolean.TRUE.equals(enabledWhen.apply(row)); }
    }
}
