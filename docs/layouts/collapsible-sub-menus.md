# Collapsible sub-menus

Sections that open and close in a floating sidebar, each with its pages in a sub-menu.

A sidebar of sections, each with its pages in a [sub-menu](../components/sidebar.md) (`MenuItem.submenu`):
a section's label opens and closes it, and only the section of the current page is open when a page loads. The
sidebar floats beside the page in a card of its own (`Variant.FLOATING`).

[Open it live](https://shadwick-production.up.railway.app/recipes/collapsible-sub-menus) · runs in the examples application, in every skin.

## The layout

A sub-menu is a native `details` element: it opens and closes without JavaScript, and the browser keeps it
open while the page is shown. The sections are one labelled group, kept open with `sidebarGroupsOpen()`.

<!-- snippet: shadwick-examples/src/main/java/dev/jbaby/shadwick/examples/SubmenuLayout.java -->
```java
package dev.jbaby.shadwick.examples;

import dev.jbaby.shadwick.behaviors.SidebarBehavior;
import dev.jbaby.shadwick.components.MenuItem;
import dev.jbaby.shadwick.components.SwAppLayout;

import org.apache.wicket.Component;
import org.apache.wicket.markup.head.CssHeaderItem;
import org.apache.wicket.markup.head.IHeaderResponse;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.panel.Fragment;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.request.mapper.parameter.PageParameters;
import org.apache.wicket.request.resource.CssResourceReference;

import java.util.List;

/**
 * Layout recipe "Collapsible sub-menus": sections that open and close, each with its pages in a sub-menu, in a
 * floating sidebar. Only the current page's section is open when a page loads; the others open with a click.
 */
public abstract class SubmenuLayout extends SwAppLayout {

    protected SubmenuLayout(PageParameters parameters) {
        super(parameters);
    }

    /** The page's title. */
    protected abstract String title();

    /** The page for a view of this application. */
    protected MenuItem page(String label, String view) {
        return MenuItem.of(label, getClass(), new PageParameters().set("view", view));
    }

    @Override
    protected IModel<String> appNameModel() {
        return Model.of("Northwind");
    }

    @Override
    protected IModel<List<MenuItem>> sidebarMenuItemsModel() {
        return Model.ofList(List.of(MenuItem.group("Guide",
                MenuItem.submenu("Getting started",
                        page("Installation", "installation"),
                        page("Project structure", "structure")).withIcon("package"),
                MenuItem.submenu("Building your app",
                        page("Routing", "routing"),
                        page("Data fetching", "data"),
                        page("Rendering", "rendering"),
                        page("Caching", "caching")).withIcon("file-text"),
                MenuItem.submenu("API reference",
                        page("Components", "components"),
                        page("Functions", "functions")).withIcon("link"),
                MenuItem.submenu("Architecture",
                        page("Accessibility", "accessibility"),
                        page("Supported browsers", "browsers")).withIcon("building-2"))));
    }

    @Override
    protected boolean isCurrentPage(MenuItem item) {
        return item.pageClass() == getClass() && item.parameters() != null
                && item.parameters().get("view").toString().equals(getPageParameters().get("view").toString("installation"));
    }

    @Override
    protected SidebarBehavior.Variant sidebarVariant() {
        return SidebarBehavior.Variant.FLOATING;
    }

    @Override
    protected boolean sidebarGroupsOpen() {
        return true;
    }

    @Override
    protected SidebarBehavior.Collapsible sidebarCollapsible() {
        return SidebarBehavior.Collapsible.OFFCANVAS;
    }

    @Override
    protected Component newSidebarHeader(String id) {
        return new Fragment(id, "brand", this);
    }

    @Override
    protected void onInitialize() {
        super.onInitialize();
        add(new Label("title", title()));
    }

    @Override
    public void renderHead(IHeaderResponse response) {
        super.renderHead(response);
        response.render(CssHeaderItem.forReference(new CssResourceReference(SubmenuLayout.class, "SubmenuLayout.css")));
    }
}
```

<!-- snippet: shadwick-examples/src/main/resources/dev/jbaby/shadwick/examples/SubmenuLayout.html -->
```html
<wicket:extend xmlns:wicket="http://wicket.apache.org">
<div class="app-page">
  <h1 wicket:id="title" class="app-page-title"></h1>
  <wicket:child/>
</div>

<wicket:fragment wicket:id="brand">
  <span class="app-brand"><span class="app-logo" aria-hidden="true">N</span><strong>Northwind</strong></span>
</wicket:fragment>
</wicket:extend>
```

## A page

The page shows one of the menu's views, picked by its `view` parameter. Replace it with your own pages.

<!-- snippet: shadwick-examples/src/main/java/dev/jbaby/shadwick/examples/SubmenuPage.java -->
```java
package dev.jbaby.shadwick.examples;

import org.apache.wicket.request.mapper.parameter.PageParameters;

import java.util.Map;

/** A page in the "Collapsible sub-menus" layout: the view parameter picks which of the menu's pages it shows. */
public class SubmenuPage extends SubmenuLayout {

    private static final Map<String, String> TITLES = Map.of(
            "installation", "Installation", "structure", "Project structure", "routing", "Routing",
            "data", "Data fetching", "rendering", "Rendering", "caching", "Caching", "components", "Components",
            "functions", "Functions", "accessibility", "Accessibility", "browsers", "Supported browsers");

    public SubmenuPage(PageParameters parameters) {
        super(parameters);
    }

    @Override
    protected String title() {
        return TITLES.getOrDefault(getPageParameters().get("view").toString("installation"), "Installation");
    }
}
```

<!-- snippet: shadwick-examples/src/main/resources/dev/jbaby/shadwick/examples/SubmenuPage.html -->
```html
<wicket:extend xmlns:wicket="http://wicket.apache.org">
<p class="app-lead">Open another section in the sidebar to see its pages. The section of the page you are on opens
  by itself.</p>
<div class="app-cards">
  <section class="sw-card"><div class="sw-card-content"><p class="app-muted">Read next</p><p>Project structure</p></div></section>
  <section class="sw-card"><div class="sw-card-content"><p class="app-muted">Updated</p><p>Last week</p></div></section>
</div>
<p class="app-muted">An example page of a layout recipe.
  <a href="https://danielbartl.github.io/shadwick/docs/layouts/collapsible-sub-menus/">Read its code in the Shadwick docs</a>.</p>
</wicket:extend>
```

## The CSS

Shadwick's CSS styles the components, not the page around them, and has no utility classes. The rest is the
application's own CSS: plain CSS on the color tokens, so it follows the skin and the dark theme.

<!-- snippet: shadwick-examples/src/main/resources/dev/jbaby/shadwick/examples/SubmenuLayout.css -->
```css
/* The application's own CSS around the layout: Shadwick styles the components, not the page between them. */
.app-page {
  display: grid;
  gap: 1rem;
  padding: 1.5rem;
}

.app-page-title {
  margin: 0;
  font-size: 1.5rem;
  font-weight: 600;
  line-height: 1.2;
}

.app-lead,
.app-muted {
  margin: 0;
  color: var(--muted-foreground);
}

.app-cards {
  display: grid;
  gap: 1rem;
  grid-template-columns: repeat(auto-fit, minmax(12rem, 1fr));
}

.app-cards p {
  margin: 0;
}

.app-brand {
  display: flex;
  align-items: center;
  gap: 0.5rem;
  padding: 0.25rem 0.5rem;
}

.app-logo {
  display: grid;
  flex: none;
  place-items: center;
  width: 2rem;
  height: 2rem;
  border-radius: calc(var(--radius) - 2px);
  background: var(--primary);
  color: var(--primary-foreground);
  font-weight: 600;
}

@media (width < 48rem) {
  .app-page {
    padding-inline: 1rem;
  }
}
```

## Variations

- **Sub-menus that are always open.** For a short list, use a labelled group per section instead
  (`MenuItem.group`), with `sidebarGroupsOpen()`; see [Grouped by section](grouped-by-section.md).
- **Not floating.** Without `sidebarVariant()` the sidebar is a column along the edge of the screen.
