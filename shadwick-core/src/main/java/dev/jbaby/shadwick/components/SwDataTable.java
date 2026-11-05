package dev.jbaby.shadwick.components;

import dev.jbaby.shadwick.behaviors.ButtonBehavior;
import dev.jbaby.shadwick.behaviors.StyledBehavior;
import dev.jbaby.shadwick.behaviors.TableBehavior;
import dev.jbaby.shadwick.components.table.SwHeadersToolbar;
import dev.jbaby.shadwick.components.table.SwSelectionColumn;
import dev.jbaby.shadwick.util.SerializableBiConsumer;
import dev.jbaby.shadwick.util.SerializableFunction;
import org.apache.wicket.AttributeModifier;
import org.apache.wicket.Component;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.event.IEvent;
import org.apache.wicket.extensions.markup.html.repeater.data.sort.SortOrder;
import org.apache.wicket.extensions.markup.html.repeater.data.table.*;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.list.ListItem;
import org.apache.wicket.markup.html.list.ListView;
import org.apache.wicket.markup.html.navigation.paging.IPageableItems;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.model.StringResourceModel;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

/**
 * Styled standard Wicket columns/providers, pageable like DataTable. Ajax sorting and paging have native fallbacks.
 * setQuery opts into real bookmarkable navigation with preserved filters and history. Providers own stable sorting,
 * count/row queries and detachable row models. Application callbacks own CRUD and authorization.
 */
public class SwDataTable<T, S> extends Panel implements IPageableItems {
    public static final String TOOLBAR_ID = "toolbar", EMPTY_STATE_ID = "content";
    private final DataTable<T, S> table;
    private final ISortableDataProvider<T, S> provider;
    private final SwSelectionColumn<T, S> selection;
    private final WebMarkupContainer bulkBar, emptyCell;
    private final TableBehavior appearance = new TableBehavior();
    private final SwPagingNavigator navigator;
    private final SwPaginationSize pageSize;
    private final List<BulkAction<T>> bulkActions = new ArrayList<>();
    private IModel<String> label = Model.of("Records");
    private TableQuery query;
    private SerializableFunction<S, String> sortKey = property -> property.toString();
    private long maximumPageSize;

    @SuppressWarnings("unchecked")
    public SwDataTable(String id, List<? extends IColumn<T, S>> columns, ISortableDataProvider<T, S> provider, long rowsPerPage) {
        super(id); Objects.requireNonNull(provider); Objects.requireNonNull(columns);
        if (columns.isEmpty()) throw new IllegalArgumentException("A data table needs at least one column");
        if (rowsPerPage <= 0) throw new IllegalArgumentException("Rows per page must be positive");
        this.provider = provider; maximumPageSize = Math.max(200, rowsPerPage);
        add(new StyledBehavior("sw-data-table")); setOutputMarkupId(true);
        selection = (SwSelectionColumn<T, S>) columns.stream().filter(SwSelectionColumn.class::isInstance).findFirst().orElse(null);
        table = new RowsTable("table", List.copyOf(columns), provider, rowsPerPage); table.setOutputMarkupId(true); table.add(appearance);
        table.add(AttributeModifier.replace("aria-label", (IModel<String>) () -> label == null ? null : label.getObject()));
        add(table); emptyCell = (WebMarkupContainer) table.getBody().get("empty:cell");
        table.addTopToolbar(new SwHeadersToolbar<S>(table, provider) {
            @Override protected CharSequence sortUrl(S property, SortOrder next) {
                return query == null ? null : bookmarkable(query.sort(sortKey.apply(property), next == SortOrder.ASCENDING));
            }
            @Override protected void onSortChanged(Optional<AjaxRequestTarget> target) { target.ifPresent(t -> t.add(SwDataTable.this)); }
        });
        add(new WebMarkupContainer(TOOLBAR_ID).setVisible(false));
        bulkBar = new WebMarkupContainer("bulkActions") {
            @Override protected void onConfigure() { super.onConfigure(); setVisible(selection != null && !selection.getSelection().getObject().isEmpty()); }
        }; bulkBar.setOutputMarkupPlaceholderTag(true); add(bulkBar);
        bulkBar.add(new Label("count", new StringResourceModel("SwDataTable.selected", this).setParameters((IModel<Integer>) () -> selection == null ? 0 : selection.getSelection().getObject().size())));
        bulkBar.add(new ListView<BulkAction<T>>("actions", Model.ofList(bulkActions)) {
            @Override protected void populateItem(ListItem<BulkAction<T>> item) {
                item.setRenderBodyOnly(true); var action = item.getModelObject();
                item.add(SwButton.onClick("action", action.label, target -> action.callback.accept(target, selection.getSelectedRows(provider)))
                        .setVariant(action.variant).setSize(ButtonBehavior.Size.SMALL));
            }
        });
        bulkBar.add(SwButton.onClick("clear", new StringResourceModel("SwDataTable.clearSelection", this), this::clearSelection)
                .setVariant(ButtonBehavior.Variant.GHOST).setSize(ButtonBehavior.Size.SMALL));
        add(new SwPaginationSummary("summary", this));
        pageSize = new SwPaginationSize("size", this) {
            @Override protected void onAjaxEvent(AjaxRequestTarget target) { target.add(SwDataTable.this); }
        }; add(pageSize);
        navigator = new SwPagingNavigator("navigator", this) {
            @Override protected void onAjaxEvent(AjaxRequestTarget target) { target.add(SwDataTable.this); }
        }; add(navigator);
    }
    /** Native navigation uses the current page's class; PageParameters retain unrelated and repeated values. */
    private CharSequence bookmarkable(org.apache.wicket.request.mapper.parameter.PageParameters parameters) { return urlFor(getPage().getClass(), parameters); }
    public SwDataTable<T, S> setQuery(TableQuery query) { return setQuery(query, property -> property.toString()); }
    /** sortKey must uniquely name each sortable column; only properties from those columns are accepted from URLs. */
    public SwDataTable<T, S> setQuery(TableQuery query, SerializableFunction<S, String> sortKey) {
        this.query = query; this.sortKey = Objects.requireNonNull(sortKey);
        if (query != null) {
            var keys = new java.util.HashSet<String>();
            for (var column : table.getColumns()) if (column.isSortable()) {
                String key = sortKey.apply(column.getSortProperty());
                if (key == null || key.isBlank() || !keys.add(key)) throw new IllegalArgumentException("Sortable columns need distinct, nonblank URL keys");
                if (key.equals(query.getSortProperty())) provider.getSortState().setPropertySortOrder(column.getSortProperty(), query.isAscending() ? SortOrder.ASCENDING : SortOrder.DESCENDING);
            }
            setItemsPerPage(query.getSize(getItemsPerPage(), maximumPageSize)); setCurrentPage(query.getPage());
        }
        navigator.setPageUrl(query == null ? null : page -> bookmarkable(query.page(page)));
        pageSize.setSizeUrl(query == null ? null : size -> bookmarkable(query.size(size)));
        return this;
    }
    public TableQuery getQuery() { return query; }
    public SwDataTable<T, S> setMaximumPageSize(long maximum) {
        if (maximum <= 0) throw new IllegalArgumentException("Maximum page size must be positive");
        maximumPageSize = maximum; if (query != null) setQuery(query, sortKey); return this;
    }
    public SwDataTable<T, S> setPageSizes(long... sizes) { pageSize.setSizes(sizes); return this; }
    public SwDataTable<T, S> setSiblings(int siblings) { navigator.setSiblings(siblings); return this; }
    public SwDataTable<T, S> setPaginationLabel(String label) { navigator.setLabel(label); return this; }
    public SwDataTable<T, S> setPaginationLabel(IModel<String> label) { navigator.setLabel(label); return this; }
    public SwDataTable<T, S> setLabel(String label) { return setLabel(Model.of(label)); }
    public SwDataTable<T, S> setLabel(IModel<String> label) { this.label = label; return this; }
    public SwDataTable<T, S> setCaption(String caption) { return setCaption(Model.of(caption)); }
    public SwDataTable<T, S> setCaption(IModel<String> caption) { table.getCaption().setDefaultModel(caption); return this; }
    public SwDataTable<T, S> setSize(TableBehavior.Size size) { appearance.setSize(size); return this; }
    public SwDataTable<T, S> setStickyFirstColumn(boolean sticky) { appearance.setStickyFirstColumn(sticky); return this; }
    public SwDataTable<T, S> setToolbar(SerializableFunction<String, ? extends Component> factory) {
        var toolbar = factory.apply(TOOLBAR_ID); requireId(toolbar, TOOLBAR_ID); replace(toolbar); return this;
    }
    public SwDataTable<T, S> setEmptyState(SerializableFunction<String, ? extends Component> factory) {
        var content = factory.apply(EMPTY_STATE_ID); requireId(content, EMPTY_STATE_ID); emptyCell.replace(content); return this;
    }
    private static void requireId(Component component, String id) {
        if (component == null || !id.equals(component.getId())) throw new IllegalArgumentException("The table slot must use id \"" + id + "\"");
    }
    public SwDataTable<T, S> addBulkAction(String label, SerializableBiConsumer<AjaxRequestTarget, List<T>> callback) {
        return addBulkAction(Model.of(label), ButtonBehavior.Variant.DEFAULT, callback);
    }
    public SwDataTable<T, S> addBulkAction(IModel<String> label, ButtonBehavior.Variant variant, SerializableBiConsumer<AjaxRequestTarget, List<T>> callback) {
        if (selection == null) throw new IllegalStateException("Bulk actions need a SwSelectionColumn");
        bulkActions.add(new BulkAction<>(Objects.requireNonNull(label), Objects.requireNonNull(variant), Objects.requireNonNull(callback))); return this;
    }
    public SwDataTable<T, S> clearSelection() { if (selection != null) selection.getSelection().getObject().clear(); return this; }
    public SwDataTable<T, S> clearSelection(AjaxRequestTarget target) { clearSelection(); target.add(this); return this; }
    @Override public void onEvent(IEvent<?> event) {
        super.onEvent(event);
        if (event.getPayload() instanceof SwSelectionColumn.SelectionChanged changed) changed.getTarget().add(bulkBar);
    }
    public DataTable<T, S> getTable() { return table; }
    public SwDataTable<T, S> addTopToolbar(AbstractToolbar toolbar) { table.addTopToolbar(toolbar); return this; }
    public SwDataTable<T, S> addBottomToolbar(AbstractToolbar toolbar) { table.addBottomToolbar(toolbar); return this; }
    @Override public long getCurrentPage() { return table.getCurrentPage(); }
    @Override public void setCurrentPage(long page) { table.setCurrentPage(page); }
    @Override public long getPageCount() { return table.getPageCount(); }
    @Override public long getItemCount() { return table.getItemCount(); }
    @Override public long getItemsPerPage() { return table.getItemsPerPage(); }
    @Override public void setItemsPerPage(long items) {
        if (items <= 0) throw new IllegalArgumentException("Rows per page must be positive"); table.setItemsPerPage(items);
    }
    @Override protected void onDetach() {
        super.onDetach(); if (label != null) label.detach(); bulkActions.forEach(a -> a.label.detach());
    }
    private record BulkAction<T>(IModel<String> label, ButtonBehavior.Variant variant, SerializableBiConsumer<AjaxRequestTarget, List<T>> callback) implements Serializable {}
    private class RowsTable extends DataTable<T, S> {
        RowsTable(String id, List<? extends IColumn<T, S>> columns, ISortableDataProvider<T, S> provider, long rowsPerPage) {
            super(id, columns, provider, rowsPerPage);
            var empty = new WebMarkupContainer("empty") {
                @Override protected void onConfigure() { super.onConfigure(); setVisible(getItemCount() == 0); }
            };
            var cell = new WebMarkupContainer("cell"); cell.add(AttributeModifier.replace("colspan", columns.size()));
            cell.add(new Label(EMPTY_STATE_ID, new StringResourceModel("SwDataTable.empty", SwDataTable.this))); empty.add(cell); getBody().add(empty);
        }
    }
}
