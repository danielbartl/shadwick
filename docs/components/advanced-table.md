# Advanced tables

## Column visibility

`SwDataTable` adds column choices and a native CSV download beside your custom toolbar:

```java
table.setColumnChooser(true)
        .setColumnVisible(4, false)
        .setCsvExport("projects.csv")
        .setRowKey(Project::getId);
```

Indices refer to the original column list. The chooser offers named `AbstractColumn`/`IExportableColumn` columns;
selection, details and action columns stay visible, as does at least one named data column. Column visibility
requires simple headers without row/column spans. Empty and expanded rows span the visible columns.
Visibility is presentation, not access control: providers and column definitions must exclude unauthorized data.

## CSV export

CSV exports **all filtered rows in the provider's current sort order**, including hidden exportable columns,
not just the current page or selection. Columns implement Wicket's `IExportableColumn` and have a nonblank display
model; `LambdaColumn` and Shadwick's formatted/inline-edit columns already do. Structural columns are excluded.
The response is an uncached attachment: UTF-8 with a BOM, CRLF records and RFC 4180 quoting. Numbers, booleans
and Java time values retain machine-readable forms; other values use application converters. Formula-like text
is prefixed with an apostrophe, including formulas after leading whitespace. Providers fetch up to 500 rows per
batch, and value/row/provider models detach even when conversion or writing fails. `SwCsvExport.write(...)` also
supports application-owned streams; it flushes but never closes them. For consistent exports during concurrent
writes, supply a provider with appropriate snapshot/transaction behavior. `setCsvExport(null)` disables the link.

## Expandable row details

Add a lazy details column with an application-owned expansion model:

```java
var details = new SwRowDetailsColumn<Project, String>(
        (id, row) -> new ProjectDetailsPanel(id, row), expandedProjectIds, Project::getId)
        .setRowLabel(Project::getName)
        .setExpandableWhen(Project::canViewDetails);
columns.add(0, details);
```

The factory receives `SwRowDetailsColumn.DETAILS_ID` and a detachable row model. It runs only when an expanded
row is rendered, and its component must use the supplied ID. Use a panel/fragment for content that needs its own
markup. Keys preserve expansion across sorting and paging; without a key function, expansion uses row equality.
`getExpanded()`, `isExpanded(row)` and `setExpanded(row, boolean)` expose the state. A table supports one details
column. Native fallback links expose `aria-expanded`, `aria-controls` and translated names. The optional predicate
is checked again when activating a row; the provider still owns authorization.

## Inline editing

Inline edits use a separate draft, standard Wicket converters and validators:

```java
columns.add(new SwEditableColumn<Project, String, Integer>(Model.of("Hours"), "hours",
        Project::getHours, Project::setHours)
        .setType(Integer.class)
        .setRequired(true)
        .addValidator(RangeValidator.range(1, 80))
        .setEditableWhen(project -> !project.isApproved())
        .setOnSave((target, project) -> {
            projects.save(project); // application-owned authorization and transaction
            if (target != null) target.add(totals);
        }));
```

Text is the default; supply `setType(...)` for numeric or other typed values. Invalid input remains in the editor,
with linked inline feedback that page feedback/toasts do not repeat. Save or Enter validates and commits once;
Cancel or Escape discards the draft and returns focus to the cell. Edit predicates are checked on opening and
saving. A value changed elsewhere while editing produces a conflict error rather than overwriting it; cancel
and reopen to load the new value. Applications must still implement database concurrency checks and transactions.
If the setter/save callback throws, the in-memory value is restored and the exception propagates.

## Models and native submissions

With editable columns, rows retain their editors and server-side validation state while the same row remains
visible during a table render. Set `table.setRowKey(...)` to stable unique serializable IDs when providers reload entities;
the default uses row equality and releases those references on detach. Explicit IDs also avoid reloading old row
models just to compare identity. Retained cells receive fresh provider models. Paging/filtering a row out of the
view discards its unfinished editor. Browser text not submitted to the server may reset during unrelated Ajax
replacement. Bookmarkable navigation constructs a new page, so it also discards editor/chooser/expansion state
unless your application persists that state.

Details, column choices, standalone inline forms and CSV downloads have native fallbacks. A save callback's
target is **null** on a native submit. Inline forms nested in an outer Wicket form require JavaScript and use
`NestedFormButtonBehavior` plus a default button to scope Enter to the cell. Selection and bulk actions continue
to use Ajax. The `/advanced-tables` showcase combines these APIs with filters, locked rows and pagination.
