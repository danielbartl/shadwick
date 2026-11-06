# App shell

App-shell components include reusable sidebar/header layouts, theme controls, a user menu, responsive navigation,
site header, breadcrumb trail and skip link. Named per-user color themes remain later work.

`SwSiteHeader` is a container on caller-owned `<header>` markup, following `SwCard`'s composition API. Add your
branding, ordinary navigation links and actions. `Sw.Behaviors.siteHeaderEnd()` pushes an action container to the
logical end; the row wraps and grows on narrow screens:

```java
SwSiteHeader header = new SwSiteHeader("header");
header.add(new BookmarkablePageLink<>("brand", HomePage.class));
WebMarkupContainer end = new WebMarkupContainer("end");
end.add(Sw.Behaviors.siteHeaderEnd());
header.add(end);
add(header);
```

```html
<header wicket:id="header">
    <a wicket:id="brand">My workspace</a>
    <div wicket:id="end"><button type="button">Save view</button></div>
</header>
```

Use `Sw.Behaviors.siteHeader()` on an existing container. `setInset(true)` removes the header's own background and
bottom border for a header within the page. Native `<header>` placement determines its landmark semantics; no
extra banner role is added. Headers scroll with the page; pair them with a sidebar trigger for responsive navigation.

`SwBreadcrumb<T>` follows OatBreadcrumb's list-model, `populateItem`, `of`, `setCurrent`, `setLabel` and `setMaxItems`
API. Bind it to a `<nav>`. Each item needs a `"link"` child wrapping a `"label"` child:

```java
add(SwBreadcrumb.of("path", Model.ofList(folders), (item, folder) ->
        item.add(new BookmarkablePageLink<>("link", FolderPage.class, folder.parameters())
                .add(new Label("label", folder.name()))))
        .setCurrent("Details")
        .setMaxItems(3));
```

Without current text, the final link gets `aria-current="page"`; `setCurrent(String/IModel<String>)` renders separate
escaped current-page text instead. A null current value returns to the last-link behavior. With more than
`maxItems` levels (minimum 2, counting current text), the first and final levels remain visible and an ellipsis opens
a menu of the middle ones. It reuses the dropdown menu's native popover, keyboard navigation, typeahead, dismissal
and focus return. The ellipsis does not count toward the limit. Labels come from `SwBreadcrumb.label` (Breadcrumb)
and `SwBreadcrumb.more` (Show more levels); `setLabel` or markup ARIA naming can distinguish multiple trails.

Models are read once per request and detached. Re-render the breadcrumb over Ajax to update its links, current
label or collapsed ranges; replacing an open trail closes its menu. Null or empty levels hide the trail, including
when current text is present. A markup placeholder lets a cleared trail reappear over Ajax. The list wraps long
labels, separators are hidden from assistive technology, and chevrons reverse in RTL.

For caller-owned markup, `Sw.Behaviors` also supplies `breadcrumb()`, `breadcrumbList()`, `breadcrumbItem()`,
`breadcrumbLink()`, `breadcrumbPage()` and `breadcrumbSeparator()`. Supply the actual `<nav>/<ol>/<li>/<a>/<span>`
structure and decorative separator content yourself. Explicit `aria-label` or `aria-labelledby` wins over the
standalone behavior's localized `BreadcrumbBehavior.label` fallback.

Put a skip link first in `<body>`, before repeated navigation. With a supplied component target it gets a markup
id and a missing `tabindex="-1"` is added to a non-interactive container; existing tabindex and native controls
retain their behavior:

```java
WebMarkupContainer main = new WebMarkupContainer("content");
main.setMarkupId("main");
add(main, new SwSkipLink("skip", main));
```

```html
<a wicket:id="skip"></a>
<!-- Header/navigation -->
<main wicket:id="content">...</main>
```

`new SwSkipLink("skip")` targets `id="main"`; `new SwSkipLink("skip", "results")` targets another id. For these
id-only forms, give a non-interactive target `tabindex="-1"` yourself. The localized label is `SwSkipLink.label`
(Skip to main content); `setLabel(String/IModel<String>)` replaces it. `Sw.Behaviors.skipLink([target])` enhances an
existing anchor. The link uses native fragment navigation and works without JavaScript: Tab reveals it, Enter
moves focus to the content, and the next Tab continues from there. Forced colors retain visible focus.

The **App shell** showcase (`/shell`) covers composition, real breadcrumb navigation, Ajax updates, empty trails,
long labels, RTL and keyboard access. Every showcase page now has the skip link; the landing studio uses the same
header/breadcrumb styles and a focusable native skip target.
