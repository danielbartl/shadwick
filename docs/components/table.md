# Native tables

`SwTable` styles caller-owned native table markup. Wrap a wide table in `sw-table-container` (or add
`Sw.Behaviors.tableContainer()` to a Wicket container):

```java
add(new SwTable("report").setSize(TableBehavior.Size.SMALL).setStickyFirstColumn(true));
```

```html
<div class="sw-table-container">
    <table wicket:id="report">
        <caption>Quarterly report</caption>
        <thead><tr><th scope="col">Customer</th><th scope="col" data-align="end">Total</th></tr></thead>
        <tbody><tr><th scope="row">Ada Lovelace</th><td data-align="end">€125.00</td></tr></tbody>
    </table>
</div>
```

`DEFAULT` and `SMALL` control density. `data-align="center"`/`"end"` align native header/body cells. Direct-child CSS
keeps nested tables independent. Ordinary `colgroup`, `scope="colgroup"`, `rowspan`, `colspan`, `thead`, `tbody`,
`tfoot` and `caption` work as HTML does. Sticky first cells use logical positioning for RTL, follow the row tint,
and return to normal flow in print. For a table on a card, set `--sw-table-bg: var(--card)` in your stylesheet.

A wide table scrolls inside its wrapper. Focusable cell content scrolls into view with Tab. For a text-only report,
a named wrapper with `tabindex="0"` gives explicit cross-browser keyboard access; the `/tables` showcase does this
for its wide quarterly report. `Sw.Behaviors.table()` styles existing Wicket table components with the same options.
