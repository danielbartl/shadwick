# Navigation in the header

A handful of pages as a row of links in the site header, with the page as a card below.

For an application with a handful of pages: the pages as a row of links in the site header
(`Placement.HEADER`), the theme toggle and the [user menu](../components/user-menu.md) at its end, and the page as a
card below. On a phone the same navigation opens as a panel from the menu button.

[Open it live](https://shadwick-production.up.railway.app/recipes/header-navigation) · runs in the examples application, in every skin.

## The layout

The header's end holds the theme toggle and the user menu in its header placement (an avatar button),
passed as one `wicket:fragment` through `newTopNavExtra`. The navigation is the same `MenuItem` list as in a sidebar.

<!-- snippet: shadwick-examples/src/main/java/dev/jbaby/shadwick/examples/HeaderNavLayout.java -->
```java
package dev.jbaby.shadwick.examples;

import dev.jbaby.shadwick.behaviors.SidebarBehavior;
import dev.jbaby.shadwick.components.MenuItem;
import dev.jbaby.shadwick.components.SwAppLayout;
import dev.jbaby.shadwick.components.SwThemeToggle;
import dev.jbaby.shadwick.components.SwUserMenu;

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
 * Layout recipe "Navigation in the header": for an application with a handful of pages, the pages as a row of links
 * in the site header and the page as a card below it. On a phone the same navigation opens as a panel.
 */
public abstract class HeaderNavLayout extends SwAppLayout {

    protected HeaderNavLayout(PageParameters parameters) {
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
        return Model.of("Lantern");
    }

    @Override
    protected IModel<List<MenuItem>> sidebarMenuItemsModel() {
        return Model.ofList(List.of(
                page("Overview", "overview").withIcon("house"),
                page("Projects", "projects").withIcon("folder"),
                page("Team", "team").withIcon("users"),
                page("Settings", "settings").withIcon("settings")));
    }

    @Override
    protected boolean isCurrentPage(MenuItem item) {
        return item.pageClass() == getClass() && item.parameters() != null
                && item.parameters().get("view").toString().equals(getPageParameters().get("view").toString("overview"));
    }

    @Override
    protected SidebarBehavior.Placement sidebarPlacement() {
        return SidebarBehavior.Placement.HEADER;
    }

    @Override
    protected Component newTopNavExtra(String id) {
        Fragment actions = new Fragment(id, "actions", this);
        SwUserMenu user = new SwUserMenu("user", Model.of("Lin Wei")).setDetail("lin@example.com")
                .setPlacement(SwUserMenu.Placement.HEADER);
        user.addLink("Settings", getClass(), new PageParameters().set("view", "settings"));
        actions.add(new SwThemeToggle("theme"), user);
        return actions;
    }

    @Override
    protected void onInitialize() {
        super.onInitialize();
        add(new Label("title", title()));
    }

    @Override
    public void renderHead(IHeaderResponse response) {
        super.renderHead(response);
        response.render(CssHeaderItem.forReference(new CssResourceReference(HeaderNavLayout.class, "HeaderNavLayout.css")));
    }
}
```

<!-- snippet: shadwick-examples/src/main/resources/dev/jbaby/shadwick/examples/HeaderNavLayout.html -->
```html
<wicket:extend xmlns:wicket="http://wicket.apache.org">
<div class="app-page">
  <h1 wicket:id="title" class="app-page-title"></h1>
  <wicket:child/>
</div>

<wicket:fragment wicket:id="actions">
  <div class="app-actions"><div wicket:id="theme"></div><div wicket:id="user"></div></div>
</wicket:fragment>
</wicket:extend>
```

## A page

The page shows one of the menu's views, picked by its `view` parameter. Replace it with your own pages.

<!-- snippet: shadwick-examples/src/main/java/dev/jbaby/shadwick/examples/HeaderNavPage.java -->
```java
package dev.jbaby.shadwick.examples;

import org.apache.wicket.request.mapper.parameter.PageParameters;

import java.util.Map;

/** A page in the "Navigation in the header" layout: the view parameter picks which of the menu's pages it shows. */
public class HeaderNavPage extends HeaderNavLayout {

    private static final Map<String, String> TITLES = Map.of(
            "overview", "Overview", "projects", "Projects", "team", "Team", "settings", "Settings");

    public HeaderNavPage(PageParameters parameters) {
        super(parameters);
    }

    @Override
    protected String title() {
        return TITLES.getOrDefault(getPageParameters().get("view").toString("overview"), "Overview");
    }
}
```

<!-- snippet: shadwick-examples/src/main/resources/dev/jbaby/shadwick/examples/HeaderNavPage.html -->
```html
<wicket:extend xmlns:wicket="http://wicket.apache.org">
<p class="app-lead">Four pages fit in the header on a desktop. On a phone the menu button opens them as a panel.</p>
<div class="app-cards">
  <section class="sw-card"><div class="sw-card-content"><p class="app-muted">Active projects</p><p class="app-figure">5</p></div></section>
  <section class="sw-card"><div class="sw-card-content"><p class="app-muted">Team members</p><p class="app-figure">9</p></div></section>
  <section class="sw-card"><div class="sw-card-content"><p class="app-muted">Due this week</p><p class="app-figure">3</p></div></section>
</div>
<p class="app-muted">An example page of a layout recipe.
  <a href="https://danielbartl.github.io/shadwick/docs/layouts/header-navigation/">Read its code in the Shadwick docs</a>.</p>
</wicket:extend>
```

## The CSS

Shadwick's CSS styles the components, not the page around them, and has no utility classes. The rest is the
application's own CSS: plain CSS on the color tokens, so it follows the skin and the dark theme.

<!-- snippet: shadwick-examples/src/main/resources/dev/jbaby/shadwick/examples/HeaderNavLayout.css -->
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

.app-figure {
  margin: 0.25rem 0 0;
  font-size: 1.5rem;
  font-weight: 600;
}

.app-actions {
  display: flex;
  align-items: center;
  gap: 0.5rem;
}

@media (width < 48rem) {
  .app-page {
    padding-inline: 1rem;
  }
}
```

## Variations

- **A sidebar and a top bar.** Keep the sidebar placement and put actions such as a search field or
  notifications in `newTopNavExtra`: the header then runs across the top of the page, beside the sidebar.
