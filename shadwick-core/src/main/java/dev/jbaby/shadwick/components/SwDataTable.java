package dev.jbaby.shadwick.components;

import dev.jbaby.shadwick.behaviors.ButtonBehavior;
import dev.jbaby.shadwick.behaviors.DropdownMenuBehavior;
import dev.jbaby.shadwick.behaviors.DropdownMenuItemBehavior;
import dev.jbaby.shadwick.behaviors.StyledBehavior;
import dev.jbaby.shadwick.behaviors.TableBehavior;
import dev.jbaby.shadwick.components.table.SwActionsColumn;
import dev.jbaby.shadwick.components.table.SwCsvExport;
import dev.jbaby.shadwick.components.table.SwHeadersToolbar;
import dev.jbaby.shadwick.components.table.SwRowDetailsColumn;
import dev.jbaby.shadwick.components.table.SwSelectionColumn;
import dev.jbaby.shadwick.util.SerializableBiConsumer;
import dev.jbaby.shadwick.util.SerializableFunction;

import org.apache.wicket.AttributeModifier;
import org.apache.wicket.Component;
import org.apache.wicket.MarkupContainer;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.markup.html.AjaxFallbackLink;
import org.apache.wicket.behavior.Behavior;
import org.apache.wicket.event.IEvent;
import org.apache.wicket.extensions.markup.html.repeater.data.sort.SortOrder;
import org.apache.wicket.extensions.markup.html.repeater.data.table.*;
import org.apache.wicket.markup.ComponentTag;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.link.Link;
import org.apache.wicket.markup.html.list.ListItem;
import org.apache.wicket.markup.html.list.ListView;
import org.apache.wicket.markup.html.navigation.paging.IPageableItems;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.markup.repeater.Item;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.model.StringResourceModel;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

/**
 * Styled standard Wicket columns/providers, pageable like DataTable. Ajax sorting and paging have
 * native fallbacks. setQuery opts into real bookmarkable navigation with preserved filters and
 * history. Providers own stable sorting, count/row queries and detachable row models. Application
 * callbacks own CRUD and authorization.
 */
public class SwDataTable<T, S> extends Panel implements IPageableItems {

    public static final String TOOLBAR_ID = "toolbar", EMPTY_STATE_ID = "content";
    private final DataTable<T, S> table;
    private final ISortableDataProvider<T, S> provider;
    private final SwSelectionColumn<T, S> selection;
    private final SwRowDetailsColumn<T, S> detailsColumn;
    private final java.util.Set<Integer> hiddenColumns = new java.util.HashSet<>();
    private boolean columnChooser;
    private String csvFileName;
    private SerializableFunction<T, ?> rowKey = row -> row;
    private boolean explicitRowKey;
    private final WebMarkupContainer bulkBar, emptyCell;
    private final TableBehavior appearance = new TableBehavior();
    private final SwPagingNavigator navigator;
    private final SwPaginationSize pageSize;
    private final List<BulkAction<T>> bulkActions = new ArrayList<>();
    private IModel<String> label =
            new StringResourceModel("SwDataTable.label", this).setDefaultValue("Records");
    private TableQuery query;
    private SerializableFunction<S, String> sortKey = property -> property.toString();
    private long maximumPageSize;

    @SuppressWarnings("unchecked")
    public SwDataTable(
            String id,
            List<? extends IColumn<T, S>> columns,
            ISortableDataProvider<T, S> provider,
            long rowsPerPage) {
        super(id);
        Objects.requireNonNull(provider);
        Objects.requireNonNull(columns);
        if (columns.isEmpty())
            throw new IllegalArgumentException("A data table needs at least one column");
        if (rowsPerPage <= 0) throw new IllegalArgumentException("Rows per page must be positive");
        this.provider = provider;
        maximumPageSize = Math.max(200, rowsPerPage);
        add(new StyledBehavior("sw-data-table"));
        setOutputMarkupId(true);
        selection =
                (SwSelectionColumn<T, S>)
                        columns.stream()
                                .filter(SwSelectionColumn.class::isInstance)
                                .findFirst()
                                .orElse(null);
        if (columns.stream().filter(SwRowDetailsColumn.class::isInstance).count() > 1)
            throw new IllegalArgumentException("A table supports one row details column");
        detailsColumn =
                (SwRowDetailsColumn<T, S>)
                        columns.stream()
                                .filter(SwRowDetailsColumn.class::isInstance)
                                .findFirst()
                                .orElse(null);
        table = new RowsTable("table", List.copyOf(columns), provider, rowsPerPage);
        table.setOutputMarkupId(true);
        table.add(appearance);
        table.add(
                AttributeModifier.replace(
                        "aria-label",
                        (IModel<String>) () -> label == null ? null : label.getObject()));
        add(table);
        emptyCell = (WebMarkupContainer) table.getBody().get("empty:cell");
        addHeaders();
        add(new WebMarkupContainer(TOOLBAR_ID).setVisible(false));
        addTools();
        bulkBar = newBulkActions();
        add(bulkBar);
        add(new SwPaginationSummary("summary", this));
        pageSize = newPageSize();
        add(pageSize);
        navigator = newNavigator();
        add(navigator);
    }

    private void addHeaders() {
        table.addTopToolbar(
                new SwHeadersToolbar<S>(table, provider) {

                    @Override
                    protected void configureHeader(Component header, int index) {
                        header.add(new ColumnVisibility(index));
                    }

                    @Override
                    protected CharSequence sortUrl(S property, SortOrder next) {
                        return query == null
                                ? null
                                : bookmarkable(
                                        query.sort(
                                                sortKey.apply(property),
                                                next == SortOrder.ASCENDING));
                    }

                    @Override
                    protected void onSortChanged(Optional<AjaxRequestTarget> target) {
                        target.ifPresent(t -> t.add(SwDataTable.this));
                    }
                });
    }

    private void addTools() {
        var tools =
                new WebMarkupContainer("tools") {

                    @Override
                    protected void onConfigure() {
                        super.onConfigure();
                        setVisible(columnChooser || csvFileName != null);
                    }
                };
        add(tools);
        var chooser =
                new WebMarkupContainer("chooser") {

                    @Override
                    protected void onConfigure() {
                        super.onConfigure();
                        setVisible(columnChooser && !choosableColumns().isEmpty());
                    }
                };
        tools.add(chooser);
        var trigger = new WebMarkupContainer("trigger");
        trigger.add(
                new ButtonBehavior(ButtonBehavior.Variant.OUTLINE)
                        .setSize(ButtonBehavior.Size.SMALL));
        trigger.add(new Label("label", new StringResourceModel("SwDataTable.columns", this)));
        chooser.add(trigger);
        var menu = new WebMarkupContainer("menu");
        menu.add(new DropdownMenuBehavior(trigger).setAlign(DropdownMenuBehavior.Align.END));
        chooser.add(menu);
        menu.add(
                new ListView<Integer>("columns", (IModel<List<Integer>>) this::choosableColumns) {

                    @Override
                    protected void populateItem(ListItem<Integer> item) {
                        item.setRenderBodyOnly(true);
                        int index = item.getModelObject();
                        var link =
                                new AjaxFallbackLink<Void>("toggle") {

                                    @Override
                                    public boolean isEnabled() {
                                        return super.isEnabled()
                                                && (!isColumnVisible(index)
                                                        || choosableColumns().stream()
                                                                        .filter(
                                                                                SwDataTable.this
                                                                                        ::isColumnVisible)
                                                                        .count()
                                                                > 1);
                                    }

                                    @Override
                                    public void onClick(Optional<AjaxRequestTarget> target) {
                                        setColumnVisible(index, !isColumnVisible(index));
                                        target.ifPresent(
                                                t -> {
                                                    t.add(SwDataTable.this);
                                                    t.focusComponent(trigger);
                                                });
                                    }
                                };
                        link.add(DropdownMenuItemBehavior.checkbox(() -> isColumnVisible(index)));
                        link.add(new Label("label", columnHeading(index)));
                        item.add(link);
                    }
                });
        var export =
                new Link<Void>("export") {

                    @Override
                    protected void onConfigure() {
                        super.onConfigure();
                        setVisible(csvFileName != null);
                    }

                    @Override
                    public void onClick() {
                        SwCsvExport.download(
                                getRequestCycle(),
                                provider,
                                table.getColumns(),
                                csvFileName,
                                getLocale());
                    }
                };
        export.add(
                new ButtonBehavior(ButtonBehavior.Variant.OUTLINE)
                        .setSize(ButtonBehavior.Size.SMALL));
        export.add(new Label("label", new StringResourceModel("SwDataTable.exportCsv", this)));
        tools.add(export);
    }

    private WebMarkupContainer newBulkActions() {
        WebMarkupContainer component =
                new WebMarkupContainer("bulkActions") {

                    @Override
                    protected void onConfigure() {
                        super.onConfigure();
                        setVisible(
                                selection != null
                                        && !selection.getSelection().getObject().isEmpty());
                    }
                };
        component.setOutputMarkupPlaceholderTag(true);
        component.add(
                new Label(
                        "count",
                        new StringResourceModel("SwDataTable.selected", this)
                                .setParameters(
                                        (IModel<Integer>)
                                                () ->
                                                        selection == null
                                                                ? 0
                                                                : selection
                                                                        .getSelection()
                                                                        .getObject()
                                                                        .size())));
        component.add(
                new ListView<BulkAction<T>>("actions", Model.ofList(bulkActions)) {

                    @Override
                    protected void populateItem(ListItem<BulkAction<T>> item) {
                        item.setRenderBodyOnly(true);
                        var action = item.getModelObject();
                        item.add(
                                SwButton.onClick(
                                                "action",
                                                action.label,
                                                target ->
                                                        action.callback.accept(
                                                                target,
                                                                selection.getSelectedRows(
                                                                        provider)))
                                        .setVariant(action.variant)
                                        .setSize(ButtonBehavior.Size.SMALL));
                    }
                });
        component.add(
                SwButton.onClick(
                                "clear",
                                new StringResourceModel("SwDataTable.clearSelection", this),
                                this::clearSelection)
                        .setVariant(ButtonBehavior.Variant.GHOST)
                        .setSize(ButtonBehavior.Size.SMALL));
        return component;
    }

    private SwPaginationSize newPageSize() {
        SwPaginationSize component =
                new SwPaginationSize("size", this) {

                    @Override
                    protected void onAjaxEvent(AjaxRequestTarget target) {
                        target.add(SwDataTable.this);
                    }
                };
        return component;
    }

    private SwPagingNavigator newNavigator() {
        SwPagingNavigator component =
                new SwPagingNavigator("navigator", this) {

                    @Override
                    protected void onAjaxEvent(AjaxRequestTarget target) {
                        target.add(SwDataTable.this);
                    }
                };
        return component;
    }

    /**
     * Native navigation uses the current page's class; PageParameters retain unrelated and repeated
     * values.
     */
    private CharSequence bookmarkable(
            org.apache.wicket.request.mapper.parameter.PageParameters parameters) {
        return urlFor(getPage().getClass(), parameters);
    }

    private List<Integer> choosableColumns() {
        return java.util.stream.IntStream.range(0, table.getColumns().size())
                .filter(this::isChoosable)
                .boxed()
                .toList();
    }

    private IModel<String> columnHeading(int index) {
        var column = table.getColumns().get(index);
        if (column instanceof AbstractColumn<?, ?> named) return named.getDisplayModel();
        if (column
                instanceof
                org.apache.wicket.extensions.markup.html.repeater.data.table.export
                                                .IExportableColumn<
                                        ?, ?>
                                named) return named.getDisplayModel();
        return null;
    }

    /**
     * Adds a native dropdown of named data columns; selection, details and action columns stay
     * visible.
     */
    public SwDataTable<T, S> setColumnChooser(boolean enabled) {
        if (enabled) requireSimpleHeaders();
        columnChooser = enabled;
        return this;
    }

    /**
     * Null disables CSV. All filtered/sorted rows and all named exportable columns are included,
     * even hidden ones.
     */
    public SwDataTable<T, S> setCsvExport(String fileName) {
        if (fileName != null) SwCsvExport.validateFileName(fileName);
        csvFileName = fileName;
        return this;
    }

    /**
     * Stable, unique, serializable row keys let editors survive rerenders with newly loaded row
     * objects. Defaults to row equality. A row leaving the visible page loses its unfinished
     * editor.
     */
    public SwDataTable<T, S> setRowKey(SerializableFunction<T, ?> key) {
        rowKey = Objects.requireNonNull(key);
        explicitRowKey = true;
        return this;
    }

    /** Visibility is presentation only; never put unauthorized data in the provider or columns. */
    public SwDataTable<T, S> setColumnVisible(int index, boolean visible) {
        checkColumnIndex(index);
        if (!visible) {
            requireSimpleHeaders();
            if (!isChoosable(index))
                throw new IllegalArgumentException(
                        "Structural or unnamed columns cannot be hidden");
            if (!hiddenColumns.contains(index)
                    && choosableColumns().stream().filter(this::isColumnVisible).count() <= 1)
                throw new IllegalArgumentException(
                        "At least one named data column must stay visible");
            hiddenColumns.add(index);
        } else hiddenColumns.remove(index);
        return this;
    }

    public boolean isColumnVisible(int index) {
        checkColumnIndex(index);
        return !hiddenColumns.contains(index);
    }

    public int getVisibleColumnCount() {
        return table.getColumns().size() - hiddenColumns.size();
    }

    private void checkColumnIndex(int index) {
        if (index < 0 || index >= table.getColumns().size())
            throw new IndexOutOfBoundsException("Column: " + index);
    }

    private void requireSimpleHeaders() {
        if (table.getColumns().stream()
                .anyMatch(c -> c.getHeaderColspan() != 1 || c.getHeaderRowspan() != 1))
            throw new IllegalStateException(
                    "Column visibility requires headers without row or column spans");
    }

    private boolean isChoosable(int index) {
        var c = table.getColumns().get(index);
        var heading = columnHeading(index);
        return !(c instanceof SwSelectionColumn<?, ?>
                        || c instanceof SwActionsColumn<?, ?>
                        || c instanceof SwRowDetailsColumn<?, ?>)
                && heading != null
                && heading.getObject() != null
                && !heading.getObject().isBlank();
    }

    private final class ColumnVisibility extends Behavior {

        private final int index;

        ColumnVisibility(int index) {
            this.index = index;
        }

        @Override
        public void onComponentTag(Component component, ComponentTag tag) {
            if (!isColumnVisible(index)) {
                tag.put("hidden", "hidden");
                tag.append("class", "sw-table-column-hidden", " ");
            }
            if (java.util.stream.IntStream.range(0, table.getColumns().size())
                            .filter(SwDataTable.this::isColumnVisible)
                            .findFirst()
                            .orElse(-1)
                    == index) tag.put("data-sw-first-column", "true");
        }
    }

    public SwDataTable<T, S> setQuery(TableQuery query) {
        return setQuery(query, property -> property.toString());
    }

    /**
     * sortKey must uniquely name each sortable column; only properties from those columns are
     * accepted from URLs.
     */
    public SwDataTable<T, S> setQuery(TableQuery query, SerializableFunction<S, String> sortKey) {
        this.query = query;
        this.sortKey = Objects.requireNonNull(sortKey);
        if (query != null) {
            var keys = new java.util.HashSet<String>();
            for (var column : table.getColumns())
                if (column.isSortable()) {
                    String key = sortKey.apply(column.getSortProperty());
                    if (key == null || key.isBlank() || !keys.add(key))
                        throw new IllegalArgumentException(
                                "Sortable columns need distinct, nonblank URL keys");
                    if (key.equals(query.getSortProperty()))
                        provider.getSortState()
                                .setPropertySortOrder(
                                        column.getSortProperty(),
                                        query.isAscending()
                                                ? SortOrder.ASCENDING
                                                : SortOrder.DESCENDING);
                }
            setItemsPerPage(query.getSize(getItemsPerPage(), maximumPageSize));
            setCurrentPage(query.getPage());
        }
        navigator.setPageUrl(query == null ? null : page -> bookmarkable(query.page(page)));
        pageSize.setSizeUrl(query == null ? null : size -> bookmarkable(query.size(size)));
        return this;
    }

    public TableQuery getQuery() {
        return query;
    }

    public SwDataTable<T, S> setMaximumPageSize(long maximum) {
        if (maximum <= 0) throw new IllegalArgumentException("Maximum page size must be positive");
        maximumPageSize = maximum;
        if (query != null) setQuery(query, sortKey);
        return this;
    }

    public SwDataTable<T, S> setPageSizes(long... sizes) {
        pageSize.setSizes(sizes);
        return this;
    }

    public SwDataTable<T, S> setSiblings(int siblings) {
        navigator.setSiblings(siblings);
        return this;
    }

    public SwDataTable<T, S> setPaginationLabel(String label) {
        navigator.setLabel(label);
        return this;
    }

    public SwDataTable<T, S> setPaginationLabel(IModel<String> label) {
        navigator.setLabel(label);
        return this;
    }

    public SwDataTable<T, S> setLabel(String label) {
        return setLabel(Model.of(label));
    }

    public SwDataTable<T, S> setLabel(IModel<String> label) {
        this.label = label;
        return this;
    }

    public SwDataTable<T, S> setCaption(String caption) {
        return setCaption(Model.of(caption));
    }

    public SwDataTable<T, S> setCaption(IModel<String> caption) {
        table.getCaption().setDefaultModel(caption);
        return this;
    }

    public SwDataTable<T, S> setSize(TableBehavior.Size size) {
        appearance.setSize(size);
        return this;
    }

    public SwDataTable<T, S> setStickyFirstColumn(boolean sticky) {
        appearance.setStickyFirstColumn(sticky);
        return this;
    }

    public SwDataTable<T, S> setToolbar(SerializableFunction<String, ? extends Component> factory) {
        var toolbar = factory.apply(TOOLBAR_ID);
        requireId(toolbar, TOOLBAR_ID);
        replace(toolbar);
        return this;
    }

    public SwDataTable<T, S> setEmptyState(
            SerializableFunction<String, ? extends Component> factory) {
        var content = factory.apply(EMPTY_STATE_ID);
        requireId(content, EMPTY_STATE_ID);
        emptyCell.replace(content);
        return this;
    }

    private static void requireId(Component component, String id) {
        if (component == null || !id.equals(component.getId()))
            throw new IllegalArgumentException("The table slot must use id \"" + id + "\"");
    }

    public SwDataTable<T, S> addBulkAction(
            String label, SerializableBiConsumer<AjaxRequestTarget, List<T>> callback) {
        return addBulkAction(Model.of(label), ButtonBehavior.Variant.DEFAULT, callback);
    }

    public SwDataTable<T, S> addBulkAction(
            IModel<String> label,
            ButtonBehavior.Variant variant,
            SerializableBiConsumer<AjaxRequestTarget, List<T>> callback) {
        if (selection == null)
            throw new IllegalStateException("Bulk actions need a SwSelectionColumn");
        bulkActions.add(
                new BulkAction<>(
                        Objects.requireNonNull(label),
                        Objects.requireNonNull(variant),
                        Objects.requireNonNull(callback)));
        return this;
    }

    public SwDataTable<T, S> clearSelection() {
        if (selection != null) selection.getSelection().getObject().clear();
        return this;
    }

    public SwDataTable<T, S> clearSelection(AjaxRequestTarget target) {
        clearSelection();
        target.add(this);
        return this;
    }

    @Override
    public void onEvent(IEvent<?> event) {
        super.onEvent(event);
        if (event.getPayload() instanceof SwSelectionColumn.SelectionChanged changed)
            changed.getTarget().add(bulkBar);
    }

    public DataTable<T, S> getTable() {
        return table;
    }

    public SwDataTable<T, S> addTopToolbar(AbstractToolbar toolbar) {
        table.addTopToolbar(toolbar);
        return this;
    }

    public SwDataTable<T, S> addBottomToolbar(AbstractToolbar toolbar) {
        table.addBottomToolbar(toolbar);
        return this;
    }

    @Override
    public long getCurrentPage() {
        return table.getCurrentPage();
    }

    @Override
    public void setCurrentPage(long page) {
        table.setCurrentPage(page);
    }

    @Override
    public long getPageCount() {
        return table.getPageCount();
    }

    @Override
    public long getItemCount() {
        return table.getItemCount();
    }

    @Override
    public long getItemsPerPage() {
        return table.getItemsPerPage();
    }

    @Override
    public void setItemsPerPage(long items) {
        if (items <= 0) throw new IllegalArgumentException("Rows per page must be positive");
        table.setItemsPerPage(items);
    }

    @Override
    protected void onDetach() {
        super.onDetach();
        if (label != null) label.detach();
        bulkActions.forEach(a -> a.label.detach());
    }

    private record BulkAction<T>(
            IModel<String> label,
            ButtonBehavior.Variant variant,
            SerializableBiConsumer<AjaxRequestTarget, List<T>> callback)
            implements Serializable {}

    private class RowsTable extends DataTable<T, S> {

        @Override
        protected Item<IColumn<T, S>> newCellItem(
                String id, int index, IModel<IColumn<T, S>> model) {
            var item = super.newCellItem(id, index, model);
            item.add(new ColumnVisibility(index));
            return item;
        }

        @Override
        protected Item<T> newRowItem(String id, int index, IModel<T> model) {
            return new Row<>(
                    id, index, model, detailsColumn, SwDataTable.this::getVisibleColumnCount);
        }

        RowsTable(
                String id,
                List<? extends IColumn<T, S>> columns,
                ISortableDataProvider<T, S> provider,
                long rowsPerPage) {
            super(id, columns, provider, rowsPerPage);
            if (columns.stream()
                    .anyMatch(
                            dev.jbaby.shadwick.components.table.SwEditableColumn.class::isInstance))
                setItemReuseStrategy(
                        new org.apache.wicket.markup.repeater.IItemReuseStrategy() {

                            @Override
                            @SuppressWarnings("unchecked")
                            public <R> java.util.Iterator<Item<R>> getItems(
                                    org.apache.wicket.markup.repeater.IItemFactory<R> factory,
                                    java.util.Iterator<IModel<R>> models,
                                    java.util.Iterator<Item<R>> existing) {
                                var previous = new java.util.HashMap<Object, Item<R>>();
                                existing.forEachRemaining(
                                        item ->
                                                previous.put(
                                                        ((RowModel<R>) item.getModel()).key(),
                                                        item));
                                var seen = new java.util.HashSet<Object>();
                                return new java.util.Iterator<>() {

                                    private int index;

                                    @Override
                                    public boolean hasNext() {
                                        return models.hasNext();
                                    }

                                    @Override
                                    public Item<R> next() {
                                        IModel<R> model = models.next();
                                        Object key;
                                        try {
                                            key =
                                                    Objects.requireNonNull(
                                                            rowKey.apply((T) model.getObject()),
                                                            "Row keys must not be null");
                                            if (explicitRowKey && !(key instanceof Serializable))
                                                throw new IllegalArgumentException(
                                                        "Explicit row keys must be serializable");
                                            if (!seen.add(key))
                                                throw new IllegalStateException(
                                                        "Visible table rows must have distinct"
                                                            + " keys");
                                        } catch (RuntimeException failure) {
                                            model.detach();
                                            throw failure;
                                        }
                                        Item<R> item = previous.remove(key);
                                        if (item == null)
                                            item =
                                                    factory.newItem(
                                                            index,
                                                            new RowModel<>(
                                                                    model, key, explicitRowKey));
                                        else {
                                            ((RowModel<R>) item.getModel()).replace(model);
                                            item.setIndex(index);
                                        }
                                        index++;
                                        return item;
                                    }
                                };
                            }
                        });
            var empty =
                    new WebMarkupContainer("empty") {

                        @Override
                        protected void onConfigure() {
                            super.onConfigure();
                            setVisible(getItemCount() == 0);
                        }
                    };
            var cell = new WebMarkupContainer("cell");
            cell.add(
                    AttributeModifier.replace(
                            "colspan", (IModel<Integer>) SwDataTable.this::getVisibleColumnCount));
            cell.add(
                    new Label(
                            EMPTY_STATE_ID,
                            new StringResourceModel("SwDataTable.empty", SwDataTable.this)));
            empty.add(cell);
            getBody().add(empty);
        }
    }

    /**
     * Stable forwarding model: retained cells see the provider's fresh model on every whole-table
     * render.
     */
    private static final class RowModel<R> implements IModel<R> {

        private IModel<R> delegate;
        private final Object stableKey;
        private transient Object equalityKey;

        RowModel(IModel<R> delegate, Object key, boolean explicit) {
            this.delegate = delegate;
            stableKey = explicit ? key : null;
            equalityKey = explicit ? null : key;
        }

        Object key() {
            if (stableKey != null) return stableKey;
            if (equalityKey == null) equalityKey = delegate.getObject();
            return equalityKey;
        }

        void replace(IModel<R> next) {
            IModel<R> previous = delegate;
            delegate = next;
            equalityKey = null;
            if (previous != next) previous.detach();
        }

        @Override
        public R getObject() {
            return delegate.getObject();
        }

        @Override
        public void setObject(R value) {
            delegate.setObject(value);
        }

        @Override
        public void detach() {
            try {
                delegate.detach();
            } finally {
                equalityKey = null;
            }
        }
    }

    /** Row group with a lazy full-width details row. */
    public static final class Row<T> extends Item<T> {

        private final WebMarkupContainer details;

        Row(
                String id,
                int index,
                IModel<T> model,
                SwRowDetailsColumn<T, ?> column,
                IModel<Integer> colspan) {
            super(id, index, model);
            setRenderBodyOnly(true);
            details =
                    new WebMarkupContainer("details") {

                        @Override
                        protected void onConfigure() {
                            super.onConfigure();
                            boolean expanded =
                                    column != null && column.isExpanded(model.getObject());
                            setVisible(expanded);
                            if (expanded && get("cell").get(SwRowDetailsColumn.DETAILS_ID) == null)
                                ((MarkupContainer) get("cell"))
                                        .add(
                                                column.newDetails(
                                                        SwRowDetailsColumn.DETAILS_ID, model));
                        }
                    };
            details.setOutputMarkupPlaceholderTag(column != null);
            var cell = new WebMarkupContainer("cell");
            cell.add(AttributeModifier.replace("colspan", colspan));
            details.add(cell);
            add(details);
        }

        public WebMarkupContainer getDetails() {
            return details;
        }
    }
}
