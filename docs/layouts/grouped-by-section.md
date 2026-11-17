# Grouped by section

Pages in labelled groups with a version switcher and a search field, as documentation has.

A sidebar of sections, each a group of pages under its label, for documentation or a knowledge base. At the
top, a version switcher (a dropdown menu of links, the current version checked) and a search field: a plain GET form,
so searching works without JavaScript and the result's address can be shared. Every section stays open
(`sidebarGroupsOpen()`), so the whole table of contents is in view; its groups can still be closed.

On a desktop the trigger slides the sidebar out of view (`Collapsible.OFFCANVAS`); on a phone it opens it as a panel.

[Open it live](https://shadwick-production.up.railway.app/recipes/grouped-by-section) · runs in the examples application, in every skin.

## The layout

The version switcher and the search form are one `wicket:fragment`, the sidebar's header. The search posts
`q` to the page itself, which shows the results; the version is a page parameter like the view.

<!-- snippet: shadwick-examples/src/main/java/dev/jbaby/shadwick/examples/SectionsLayout.java -->
```java
package dev.jbaby.shadwick.examples;

import dev.jbaby.shadwick.Sw;
import dev.jbaby.shadwick.behaviors.DropdownMenuBehavior;
import dev.jbaby.shadwick.behaviors.SidebarBehavior;
import dev.jbaby.shadwick.components.MenuItem;
import dev.jbaby.shadwick.components.SwAppLayout;

import org.apache.wicket.AttributeModifier;
import org.apache.wicket.Component;
import org.apache.wicket.markup.head.CssHeaderItem;
import org.apache.wicket.markup.head.IHeaderResponse;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.link.BookmarkablePageLink;
import org.apache.wicket.markup.html.list.ListItem;
import org.apache.wicket.markup.html.list.ListView;
import org.apache.wicket.markup.html.panel.Fragment;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.request.mapper.parameter.PageParameters;
import org.apache.wicket.request.resource.CssResourceReference;

import java.util.List;

/**
 * Layout recipe "Grouped by section": pages in labelled groups, with a version switcher and a search field at the
 * top of the sidebar, as documentation has. On a desktop the sidebar slides out of view; on a phone it is a panel.
 */
public abstract class SectionsLayout extends SwAppLayout {

    private static final List<String> VERSIONS = List.of("2.0", "1.4", "1.3");

    protected SectionsLayout(PageParameters parameters) {
        super(parameters);
    }

    /** The page's title. */
    protected abstract String title();

    /** The page for a view of this documentation. */
    protected MenuItem page(String label, String view) {
        return MenuItem.of(label, getClass(), new PageParameters().set("view", view));
    }

    @Override
    protected IModel<String> appNameModel() {
        return Model.of("Clover Docs");
    }

    @Override
    protected IModel<List<MenuItem>> sidebarMenuItemsModel() {
        return Model.ofList(List.of(
                MenuItem.group("Getting started",
                        page("Introduction", "introduction"),
                        page("Installation", "installation"),
                        page("Project structure", "structure")),
                MenuItem.group("Building your app",
                        page("Routing", "routing"),
                        page("Forms", "forms"),
                        page("Data", "data")),
                MenuItem.group("Reference",
                        page("Components", "components"),
                        page("Configuration", "configuration"))));
    }

    @Override
    protected boolean isCurrentPage(MenuItem item) {
        return item.pageClass() == getClass() && item.parameters() != null
                && item.parameters().get("view").toString().equals(getPageParameters().get("view").toString("introduction"));
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
        Fragment header = new Fragment(id, "versionAndSearch", this);
        WebMarkupContainer trigger = new WebMarkupContainer("trigger");
        trigger.add(Sw.Behaviors.sidebarMenuButton());
        trigger.add(new Label("version", "v" + currentVersion()));
        WebMarkupContainer versions = new WebMarkupContainer("versions");
        versions.add(Sw.Behaviors.dropdownMenu(trigger).setSide(DropdownMenuBehavior.Side.BOTTOM));
        versions.add(new ListView<>("versionList", VERSIONS) {
            @Override
            protected void populateItem(ListItem<String> item) {
                BookmarkablePageLink<Void> link = new BookmarkablePageLink<>("link", SectionsLayout.this.getClass(),
                        new PageParameters(getPageParameters()).set("version", item.getModelObject()));
                link.add(Sw.Behaviors.dropdownMenuRadioItem(Model.of(item.getModelObject().equals(currentVersion()))));
                link.add(new Label("name", "v" + item.getModelObject()));
                item.add(link);
            }
        });
        // A plain GET form: the search works without JavaScript, and its URL can be shared
        WebMarkupContainer search = new WebMarkupContainer("search");
        search.add(AttributeModifier.replace("action", urlFor(getClass(), new PageParameters()).toString()));
        WebMarkupContainer query = new WebMarkupContainer("query");
        query.add(AttributeModifier.replace("value", getPageParameters().get("q").toString("")));
        search.add(query);
        header.add(trigger, versions, search);
        return header;
    }

    @Override
    protected void onInitialize() {
        super.onInitialize();
        add(new Label("title", title()));
    }

    private String currentVersion() {
        String version = getPageParameters().get("version").toString(VERSIONS.get(0));
        return VERSIONS.contains(version) ? version : VERSIONS.get(0);
    }

    @Override
    public void renderHead(IHeaderResponse response) {
        super.renderHead(response);
        response.render(CssHeaderItem.forReference(new CssResourceReference(SectionsLayout.class, "SectionsLayout.css")));
    }
}
```

<!-- snippet: shadwick-examples/src/main/resources/dev/jbaby/shadwick/examples/SectionsLayout.html -->
```html
<wicket:extend xmlns:wicket="http://wicket.apache.org">
<article class="app-article">
  <h1 wicket:id="title" class="app-page-title"></h1>
  <wicket:child/>
</article>

<wicket:fragment wicket:id="versionAndSearch">
  <button wicket:id="trigger" type="button" data-size="lg">
    <span class="app-logo" aria-hidden="true">C</span>
    <span class="sw-sidebar-menu-button-label"><strong>Clover Docs</strong><small wicket:id="version"></small></span>
  </button>
  <div wicket:id="versions" aria-label="Versions">
    <wicket:container wicket:id="versionList"><a wicket:id="link"><span wicket:id="name"></span></a></wicket:container>
  </div>
  <form wicket:id="search" method="get" role="search" class="app-search">
    <label class="app-visually-hidden" for="docs-search">Search the docs</label>
    <input wicket:id="query" id="docs-search" class="sw-input" type="search" name="q" placeholder="Search the docs">
  </form>
</wicket:fragment>
</wicket:extend>
```

## A page

The page shows one of the menu's views, picked by its `view` parameter. Replace it with your own pages.

<!-- snippet: shadwick-examples/src/main/java/dev/jbaby/shadwick/examples/SectionsPage.java -->
```java
package dev.jbaby.shadwick.examples;

import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.request.mapper.parameter.PageParameters;

import java.util.Map;

/** A page in the "Grouped by section" layout: the view parameter picks which of the menu's pages it shows. */
public class SectionsPage extends SectionsLayout {

    private static final Map<String, String> TITLES = Map.of(
            "introduction", "Introduction", "installation", "Installation", "structure", "Project structure",
            "routing", "Routing", "forms", "Forms", "data", "Data", "components", "Components",
            "configuration", "Configuration");

    public SectionsPage(PageParameters parameters) {
        super(parameters);
    }

    @Override
    protected String title() {
        String query = getPageParameters().get("q").toString("");
        if (!query.isBlank()) return "Results for “" + query + "”";
        return TITLES.getOrDefault(getPageParameters().get("view").toString("introduction"), "Introduction");
    }

    @Override
    protected void onInitialize() {
        super.onInitialize();
        add(new Label("lead", "A short guide to " + title().toLowerCase() + ", with an example to start from."));
    }
}
```

<!-- snippet: shadwick-examples/src/main/resources/dev/jbaby/shadwick/examples/SectionsPage.html -->
```html
<wicket:extend xmlns:wicket="http://wicket.apache.org">
<p wicket:id="lead" class="app-lead"></p>
<h2>Before you start</h2>
<p>Each page of the documentation is in one of the sections in the sidebar. The version at the top switches every
  page to that release, and the search field finds a page by its words.</p>
<h2>An example</h2>
<p>Copy the example into your application and change it from there: it is yours from then on.</p>
<p class="app-muted">An example page of a layout recipe.
  <a href="https://danielbartl.github.io/shadwick/docs/layouts/grouped-by-section/">Read its code in the Shadwick docs</a>.</p>
</wicket:extend>
```

## The CSS

Shadwick's CSS styles the components, not the page around them, and has no utility classes. The rest is the
application's own CSS: plain CSS on the color tokens, so it follows the skin and the dark theme.

<!-- snippet: shadwick-examples/src/main/resources/dev/jbaby/shadwick/examples/SectionsLayout.css -->
```css
/* The application's own CSS around the layout: Shadwick styles the components, not the page between them. */
.app-article {
  display: grid;
  gap: 1rem;
  max-width: 48rem;
  padding: 1.5rem;
  line-height: 1.6;
}

.app-article :is(h2, p) {
  margin: 0;
}

.app-page-title {
  margin: 0;
  font-size: 1.75rem;
  font-weight: 600;
  line-height: 1.2;
}

.app-article h2 {
  margin-top: 0.5rem;
  font-size: 1.125rem;
  font-weight: 600;
}

.app-lead,
.app-muted {
  color: var(--muted-foreground);
}

.app-search {
  margin: 0;
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

.app-visually-hidden {
  position: absolute;
  width: 1px;
  height: 1px;
  overflow: hidden;
  clip-path: inset(50%);
  white-space: nowrap;
}

@media (width < 48rem) {
  .app-article {
    padding-inline: 1rem;
  }
}
```

## Variations

- **Collapsible sections.** Leave out `sidebarGroupsOpen()` and only the section of the current page opens when a
  page loads; the others open with a click on their label. This suits a long documentation.
