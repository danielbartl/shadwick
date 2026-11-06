# Icons

`SwIcon` renders a bundled Lucide SVG by registered name. Unnamed icons are decorative.

```java
add(new SwIcon("search", "search").setLabel("Search"));
add(new SwIcon("chevron", "chevron-right"));
```

```html
<svg wicket:id="search"></svg>
<svg wicket:id="chevron"></svg>
```

`SwIcon.names()` lists the registry. Use a fixed registered name or an `IModel<String>` for a changing icon.
`ofNullable(id, name)` creates an icon that is hidden for null; `decorativeMarkup(name)` provides trusted registry
SVG for component APIs that explicitly need markup. Do not put user-supplied SVG into those slots.

Set a label for a standalone meaningful icon; leave an icon inside an already named action decorative. Put an
`aria-label` on an icon-only button itself. Icons have no click or keyboard behavior on their own. Bind to an
`svg` or a wrapper; the component supplies its SVG attributes and body. The registry is bundled, so rendering does
not contact an external icon service.
