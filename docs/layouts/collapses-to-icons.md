# Collapses to icons

A team switcher, sections with sub-menus and projects beside the page, collapsing to a strip of icons.

A sidebar for an application with more pages than fit in a header row. At the top, a team switcher: a
[dropdown menu](../components/dropdown-menu.md) opened from a large sidebar menu button, with each team a link and the
current one checked, so it works without JavaScript. Then the platform's sections, two of them with
[sub-menus](../components/sidebar.md), the projects, and at the bottom the [user menu](../components/user-menu.md).
The page is a card beside the sidebar (`Variant.INSET`), with a [breadcrumb](../components/app-shell.md) and its title
above its content.

On a desktop the trigger (or Ctrl/⌘+B) collapses the sidebar to a strip of icons (`Collapsible.ICON`), with a tooltip
on every entry, and a cookie remembers the choice. On a phone the trigger opens the same navigation as a panel.

[Open it live](https://shadwick-production.up.railway.app/recipes/collapses-to-icons) · runs in the examples application, in every skin.

## The layout

The layout passes the team switcher as the sidebar's header and the user menu as its footer
(`newSidebarHeader`, `newSidebarFooter`). The switcher's markup is a `wicket:fragment` in the layout's own markup, so a
layout is one Java class and one markup file. `sidebarGroupsOpen()` keeps the Platform and Projects groups open; leave
it out and only the group of the current page opens.

A page passes its title and the levels above it (`title()`, `breadcrumb()`); the layout renders the breadcrumb and the
heading, then the page's markup.

<!-- snippet: shadwick-examples/src/main/java/dev/jbaby/shadwick/examples/IconRailLayout.java -->
```java
package dev.jbaby.shadwick.examples;

import dev.jbaby.shadwick.Sw;
import dev.jbaby.shadwick.behaviors.SidebarBehavior;
import dev.jbaby.shadwick.components.MenuItem;
import dev.jbaby.shadwick.components.SwAppLayout;
import dev.jbaby.shadwick.components.SwBreadcrumb;
import dev.jbaby.shadwick.components.SwUserMenu;

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
 * Layout recipe "Collapses to icons": a team switcher, sections with sub-menus and projects beside the page, which is
 * a card. On a desktop the sidebar collapses to a strip of icons; on a phone it opens as a panel.
 */
public abstract class IconRailLayout extends SwAppLayout {

    private static final List<String> TEAMS = List.of("Acme Inc.", "Acme Studio", "Evergreen Labs");

    protected IconRailLayout(PageParameters parameters) {
        super(parameters);
    }

    /** The page's title, the breadcrumb's last level. */
    protected abstract String title();

    /** The levels above the page, from the top; none for a page at the top. */
    protected List<MenuItem> breadcrumb() {
        return List.of();
    }

    /** The page for a view of this application. */
    protected MenuItem page(String label, String view) {
        return MenuItem.of(label, getClass(), new PageParameters().set("view", view));
    }

    @Override
    protected IModel<String> appNameModel() {
        return Model.of(currentTeam());
    }

    @Override
    protected IModel<List<MenuItem>> sidebarMenuItemsModel() {
        return Model.ofList(List.of(
                MenuItem.group("Platform",
                        page("Dashboard", "dashboard").withIcon("house"),
                        MenuItem.submenu("Reports",
                                page("Revenue", "revenue"),
                                page("Customers", "customers")).withIcon("chart-column"),
                        MenuItem.submenu("Settings",
                                page("General", "general"),
                                page("Billing", "billing")).withIcon("settings")),
                MenuItem.group("Projects",
                        page("Website relaunch", "website").withIcon("folder"),
                        page("Mobile app", "mobile").withIcon("folder"))));
    }

    @Override
    protected boolean isCurrentPage(MenuItem item) {
        return item.pageClass() == getClass() && item.parameters() != null
                && item.parameters().get("view").toString().equals(getPageParameters().get("view").toString("dashboard"));
    }

    @Override
    protected SidebarBehavior.Variant sidebarVariant() {
        return SidebarBehavior.Variant.INSET;
    }

    @Override
    protected boolean sidebarGroupsOpen() {
        return true;
    }

    @Override
    protected SidebarBehavior.Collapsible sidebarCollapsible() {
        return SidebarBehavior.Collapsible.ICON;
    }

    @Override
    protected Component newSidebarHeader(String id) {
        Fragment switcher = new Fragment(id, "teamSwitcher", this);
        WebMarkupContainer trigger = new WebMarkupContainer("trigger");
        trigger.add(Sw.Behaviors.sidebarMenuButton());
        trigger.add(new Label("team", currentTeam()));
        WebMarkupContainer teams = new WebMarkupContainer("teams");
        teams.add(Sw.Behaviors.dropdownMenu(trigger).setSide(dev.jbaby.shadwick.behaviors.DropdownMenuBehavior.Side.BOTTOM));
        teams.add(new ListView<>("teamList", TEAMS) {
            @Override
            protected void populateItem(ListItem<String> item) {
                BookmarkablePageLink<Void> link = new BookmarkablePageLink<>("link", IconRailLayout.this.getClass(),
                        new PageParameters(getPageParameters()).set("team", item.getModelObject()));
                link.add(Sw.Behaviors.dropdownMenuRadioItem(Model.of(item.getModelObject().equals(currentTeam()))));
                link.add(new Label("name", item.getModelObject()));
                item.add(link);
            }
        });
        switcher.add(trigger, teams);
        return switcher;
    }

    @Override
    protected Component newSidebarFooter(String id) {
        SwUserMenu user = new SwUserMenu(id, Model.of("Ada Lovelace")).setDetail("ada@example.com");
        user.addLink("Account", getClass(), new PageParameters().set("view", "general"));
        return user;
    }

    @Override
    protected void onInitialize() {
        super.onInitialize();
        add(SwBreadcrumb.of("breadcrumb", Model.ofList(breadcrumb()), (item, level) -> item.add(
                new BookmarkablePageLink<>("link", level.pageClass(), level.parameters())
                        .add(new Label("label", level.label())))).setCurrent(title()));
        add(new Label("title", title()));
    }

    private String currentTeam() {
        String team = getPageParameters().get("team").toString(TEAMS.get(0));
        return TEAMS.contains(team) ? team : TEAMS.get(0);
    }

    @Override
    public void renderHead(IHeaderResponse response) {
        super.renderHead(response);
        response.render(CssHeaderItem.forReference(new CssResourceReference(IconRailLayout.class, "IconRailLayout.css")));
    }
}
```

<!-- snippet: shadwick-examples/src/main/resources/dev/jbaby/shadwick/examples/IconRailLayout.html -->
```html
<wicket:extend xmlns:wicket="http://wicket.apache.org">
<div class="app-page-header">
  <nav wicket:id="breadcrumb"></nav>
  <h1 wicket:id="title" class="app-page-title"></h1>
</div>
<div class="app-page"><wicket:child/></div>

<wicket:fragment wicket:id="teamSwitcher">
  <button wicket:id="trigger" type="button" data-size="lg">
    <span class="app-logo" aria-hidden="true">A</span>
    <span class="sw-sidebar-menu-button-label"><strong wicket:id="team"></strong><small>Switch team</small></span>
  </button>
  <div wicket:id="teams" aria-label="Teams">
    <wicket:container wicket:id="teamList"><a wicket:id="link"><span wicket:id="name"></span></a></wicket:container>
  </div>
</wicket:fragment>
</wicket:extend>
```

## A page

The page shows one of the menu's views, picked by its `view` parameter. Replace it with your own pages.

<!-- snippet: shadwick-examples/src/main/java/dev/jbaby/shadwick/examples/IconRailPage.java -->
```java
package dev.jbaby.shadwick.examples;

import dev.jbaby.shadwick.components.MenuItem;

import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.request.mapper.parameter.PageParameters;

import java.util.List;
import java.util.Map;

/** A page in the "Collapses to icons" layout: the view parameter picks which of the menu's pages it shows. */
public class IconRailPage extends IconRailLayout {

    private static final Map<String, String> TITLES = Map.of(
            "dashboard", "Dashboard", "revenue", "Revenue", "customers", "Customers", "general", "General",
            "billing", "Billing", "website", "Website relaunch", "mobile", "Mobile app");

    public IconRailPage(PageParameters parameters) {
        super(parameters);
    }

    private String view() {
        String view = getPageParameters().get("view").toString("dashboard");
        return TITLES.containsKey(view) ? view : "dashboard";
    }

    @Override
    protected String title() {
        return TITLES.get(view());
    }

    @Override
    protected List<MenuItem> breadcrumb() {
        return switch (view()) {
            case "revenue", "customers" -> List.of(page("Reports", "revenue"));
            case "general", "billing" -> List.of(page("Settings", "general"));
            case "website", "mobile" -> List.of(page("Projects", "website"));
            default -> List.of();
        };
    }

    @Override
    protected void onInitialize() {
        super.onInitialize();
        add(new Label("summary", "Everything for " + title().toLowerCase() + " in one place."));
    }
}
```

<!-- snippet: shadwick-examples/src/main/resources/dev/jbaby/shadwick/examples/IconRailPage.html -->
```html
<wicket:extend xmlns:wicket="http://wicket.apache.org">
<p wicket:id="summary" class="app-lead"></p>
<div class="app-cards">
  <section class="sw-card"><div class="sw-card-content"><p class="app-muted">This week</p><p class="app-figure">24 updates</p></div></section>
  <section class="sw-card"><div class="sw-card-content"><p class="app-muted">Open tasks</p><p class="app-figure">8</p></div></section>
  <section class="sw-card"><div class="sw-card-content"><p class="app-muted">Team</p><p class="app-figure">12 people</p></div></section>
</div>
<p class="app-muted">An example page of a layout recipe.
  <a href="https://danielbartl.github.io/shadwick/docs/layouts/collapses-to-icons/">Read its code in the Shadwick docs</a>.</p>
</wicket:extend>
```

## The CSS

Shadwick's CSS styles the components, not the page around them, and has no utility classes. The rest is the
application's own CSS: plain CSS on the color tokens, so it follows the skin and the dark theme.

<!-- snippet: shadwick-examples/src/main/resources/dev/jbaby/shadwick/examples/IconRailLayout.css -->
```css
/* The application's own CSS around the layout: Shadwick styles the components, not the page between them. */
.app-page-header {
  display: grid;
  gap: 0.5rem;
  padding: 1.5rem 1.5rem 0;
}

.app-page-title {
  margin: 0;
  font-size: 1.5rem;
  font-weight: 600;
  line-height: 1.2;
}

.app-page {
  display: grid;
  gap: 1.5rem;
  padding: 1rem 1.5rem 1.5rem;
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

/* The team's square in the switcher, which stays in the strip of icons when the sidebar collapses. */
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
  .app-page-header,
  .app-page {
    padding-inline: 1rem;
  }
}
```

## Variations

- **Off-canvas instead of icons.** With `Collapsible.OFFCANVAS` the sidebar moves out of view instead of
  collapsing to a strip; with `Collapsible.NONE` it always stays.
- **Along the edge.** Without `Variant.INSET` the sidebar is a column along the edge of the screen and the page fills
  the rest; `Variant.FLOATING` gives it a card of its own.
