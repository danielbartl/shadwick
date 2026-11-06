# Data tables

## Columns and providers

`SwDataTable<T,S>` follows Wicket Oat's constructor and wraps Wicket's `DataTable`, using standard `IColumn<T,S>` and
`ISortableDataProvider<T,S>`. The library now includes Wicket's official `wicket-extensions` module at the same managed
version as `wicket-core`; applications still add only Shadwick. No Spring Data or client table framework is needed.

```java
IModel<Set<Long>> selectedIds = new SetModel<>(new HashSet<>());
List<IColumn<Invoice, String>> columns = new ArrayList<>();
columns.add(new SwSelectionColumn<Invoice, String>(selectedIds, Invoice::id,
        ids -> invoices.findAllById(ids)).setRowLabel(Invoice::number));
columns.add(new LambdaColumn<>(Model.of("Invoice"), "number", Invoice::number));
columns.add(new SwNumberColumn<Invoice, String>(Model.of("Amount"), "amount", Invoice::amount)
        .setCurrency(Currency.getInstance("EUR")));
columns.add(new SwDateColumn<>(Model.of("Due"), "due", Invoice::due));

var actions = new SwActionsColumn<Invoice, String>()
        .setRowLabel(Invoice::number).setRowKey(Invoice::id);
actions.addAction("Edit invoice", (target, row) -> editDialog.open(target));
actions.addAction("Delete invoice", (target, row) -> confirmation.ask(target,
        "Delete " + row.number() + "?", "This cannot be undone.", confirmed -> {
            invoices.delete(row.id());
            confirmed.add(table);
        })).setVariant(DropdownMenuItemBehavior.Variant.DESTRUCTIVE);
columns.add(actions);

// Assign a field on your page: private SwDataTable<Invoice, String> table;
table = new SwDataTable<>("invoices", columns, provider, 10)
        .setLabel("Invoices").setPaginationLabel("Invoice pages")
        .setPageSizes(10, 20, 50);
add(table);
```

```html
<div wicket:id="invoices"></div>
```

The snippet's callbacks can refer to your table field. Providers perform sorting/filtering/paging on the server:
`iterator(first,count)` returns that slice, `size()` counts the same filtered rows, and `model(row)` should return a
detachable model that reloads by key. Sort by a unique key last, including when the primary sort is descending,
so equal primary values have a stable order across pages. Allow only supported properties in repository queries.

Headers report `aria-sort` on the sorted `th` and show the direction. Sorting returns to page zero. Sorting and
paging use Ajax with normal Wicket links as fallbacks; summaries and controls update with the rows. `getTable()`
exposes the underlying Wicket table, and `addTopToolbar`/`addBottomToolbar` accept standard Wicket toolbars.
`SwHeadersToolbar` can also be added to a caller-owned Wicket `DataTable`.

`SwDataTable` implements `IPageableItems`: `getCurrentPage()`/`setCurrentPage(...)` are zero-based, and the page/item
count and items-per-page methods delegate to Wicket. Page size must be positive. Empty results retain one displayed
pagination page with disabled Previous/Next and a `0 of 0` summary.

Use `setCaption(String/model)` for a visible native caption, `setLabel(String/model)` for the table's accessible
name, `setSize(TableBehavior.Size.SMALL)` for compact rows, or `setStickyFirstColumn(true)` to pin the first column.
`setToolbar(id -> component)` adds caller-owned content above the table (`TOOLBAR_ID`); `setEmptyState(id -> component)`
replaces the default message inside a body cell spanning the columns (`EMPTY_STATE_ID`). Repeated calls replace the
slot. The table owns its responsive scroll wrapper and separate summary/size/pagination footer.

The column helpers in `dev.jbaby.shadwick.components.table` compose with ordinary Wicket columns:

- `SwBadgeColumn` renders escaped text with a row-dependent `BadgeBehavior.Variant`.
- `SwNumberColumn` formats numbers/currencies for the Wicket locale, aligns them to the end, and accepts fixed or
  row-dependent currency plus `setFractionDigits(min,max)`.
- `SwDateColumn` formats `java.time` values for the locale, with `setStyle`, `setPattern` and `setZone` for instants.
  Null number/date values stay empty.
- `SwBooleanColumn` renders a named, read-only native checkbox, or an empty cell for null.
- `SwLinkColumn.toPage(...)` creates a normal bookmarkable row link; `onClick(...)` runs an Ajax callback.
- `SwActionsColumn` renders a named native dropdown per row. `setRowLabel` makes the name distinguish rows;
  `setRowKey` supplies a unique row key, giving the trigger/menu stable IDs across replacements so focus can return after updates and dialogs.
  Actions support visibility/enabled predicates and a destructive menu variant. Predicates are checked again when
  invoked; application callbacks still own authorization and persistence. Give the provider detachable row models
  so those checks see current records.

`SwSelectionColumn` can keep whole row values, or just keys with an optional key loader. Prefer keys/loaders for
entities: bulk actions get current selected records, including ones hidden by a filter, without scanning the whole
provider. Without a loader, lookup by key scans the provider's current filtered results. `setRowLabel` names each
checkbox; `setSelectableWhen` excludes locked rows from row updates and select-all. The header selects only eligible
rows on the current page and becomes indeterminate for a partial selection, using the existing small runtime.
It reads rendered row models instead of querying the provider again.

```java
table.addBulkAction("Mark selected as paid", (target, rows) -> {
    invoices.markPaid(rows);
    table.clearSelection(target);
});
```

Bulk callbacks receive a fresh list and retain selection until `clearSelection()`/`clearSelection(target)` is called,
so a confirmation may be cancelled. Selection follows the supplied model across stateful Ajax paging/sorting;
bookmarkable navigation creates a new page, so retaining selection there needs an application-owned persistent
selection model. Selection changes bubble a `SwSelectionColumn.SelectionChanged` event with the Ajax target. The
bulk bar's count is not a live region, avoiding duplicate checkbox announcements. Live selection, row-action
callbacks and modal CRUD need JavaScript; native table links, filters and pagination remain usable without it.

## Bookmarkable table state

Opt into native URLs by passing the page's parameters:

```java
table.setQuery(new TableQuery(parameters));
```

The current page class must be reconstructible from `PageParameters`. In this mode sorting and pagination navigate
to real bookmarkable URLs, so reload and browser Back/Forward retain the view. Ajax row actions remain available.
The default `S.toString()` names sort properties; an overload accepts a custom sort-key function. Only keys from the
sortable columns are accepted. Unknown/invalid sorts leave the provider's configured default order. URL page sizes
are capped by `setMaximumPageSize(...)` (default at least 200, or the constructor's larger explicit size).

`TableQuery` is an immutable copy of decoded Wicket `PageParameters`. `page(...)` preserves everything else;
`sort(property,ascending)` and `size(...)` reset the page. `forSearch("q","status")` drops the current page and the
form's own fields, keeping sort, size and unrelated/repeated parameters. Negative/malformed page numbers fall back
to zero; malformed sizes use the configured default. No query value is inserted into HTML as raw markup.

For two tables use `new TableQuery(parameters).qualified("members")`, giving the second table `members_page`,
`members_size` and `members_sort`; the constructor also accepts custom parameter names. Qualifiers change only their
own page, preserving the other table's state. `getParameters()` returns another defensive copy.

`SwQueryParameters` renders escaped hidden inputs for a native GET form, including repeated values:

```java
formContainer.add(new SwQueryParameters("params", query.forSearch("q", "status")));
```

```html
<form wicket:id="form" method="get">
    <label for="invoice-query">Search invoices</label>
    <input id="invoice-query" name="q" type="search">
    <wicket:container wicket:id="params"></wicket:container>
    <button type="submit">Search</button>
</form>
```

Bind that form to a `WebMarkupContainer` with an action pointing at the page's bookmarkable route. These are native
query field names; Wicket form-processing field names are a different API. The `/data` showcase includes the complete
form, URL and provider setup, plus an Ajax mode whose toolbar parameters follow its current sort/size.
