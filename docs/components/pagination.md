# Pagination

`SwPagingNavigator` accepts any `IPageable`, with Previous/Next, first/last page numbers, the current page's siblings
and ellipses. A gap of one page displays that page. `setSiblings(0..10)` controls the bounded window, without allocating
one item per page. Current links have `aria-current="page"`; disabled links lose their href/tab stop and retain a
named link role. Labels/messages are component resources, and `setLabel(String/model)` distinguishes multiple navigators.

```java
var query = new TableQuery(parameters);
add(new SwPagingNavigator("pages", pageable).setLabel("Project pages")
        .setPageUrl(page -> urlFor(ProjectPage.class, query.page(page))));
add(new SwPaginationSummary("summary", pageableItems));
add(new SwPaginationSize("size", pageableItems).setSizes(10, 20, 50)
        .setSizeUrl(size -> urlFor(ProjectPage.class, query.size(size))));
```

Without URL factories, the navigator/size menu update the pageable over Ajax with Wicket fallback links. Give a
component pageable an output markup ID; override `onAjaxEvent(target)` when a surrounding region or other controls
also need updating. With URL factories they are native links and work without script. `SwPaginationSummary` accepts
`IPageableItems` and locale-formats the visible range, or `setFormat(Format.PAGE)` for “Page 2 of 28”. The size menu
always includes the current size, even when not listed. Both stay independent of footer placement.

`SwPagination<T>` retains OatPagination's caller-owned `nav`/`menu`/`items` structure and `populateItem`/`of` factory:

```html
<nav wicket:id="pagination"><ul wicket:id="menu"><li wicket:id="items">
    <a wicket:id="link"><span wicket:id="label"></span></a>
</li></ul></nav>
```

Use `Sw.Behaviors.pagination()` for a named nav and `paginationLink().setCurrent(...)` for ordinary page links;
attach link behaviors to raw Wicket links, which already handle native hrefs. `Sw.Components.pagination(...)` is the
factory facade. For a list without a total count, compose Previous/Next yourself with this generic API rather than
inventing a page count for `IPageable`.

The `/tables`, `/data`, `/advanced-tables`, `/invoice` and `/pagination` showcases cover native/nested/grouped tables, compact and pinned
columns, stateful/bookmarkable navigation, row CRUD, selection and independent pagers. The landing studio uses the
actual embedded table/pagination CSS with transient browser-only sample interactions; Wicket owns data operations
in a consuming application.
