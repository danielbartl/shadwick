# Inset with secondary navigation

Sections that are pages of their own with a sub-menu, projects, help links and the user menu.

Sections that are pages of their own, with a chevron beside them that opens their sub-menu
(`withChildren`): Calendar is a page, and its chevron opens This week and This month. Below them the projects, and at
the bottom a secondary navigation (Support, Feedback) above the [user menu](../components/user-menu.md). The page is a
card beside the sidebar (`Variant.INSET`), and the Inbox shows a badge.

[Open it live](https://shadwick-production.up.railway.app/recipes/secondary-navigation) · runs in the examples application, in every skin.

## The layout

The secondary navigation and the user menu are one `wicket:fragment`, the sidebar's footer: the help links
are ordinary page links with the sidebar's menu button behaviors (`Sw.Behaviors.sidebarMenuButton()`), in a `nav` of
their own with a name, so a screen reader lists it apart from the main navigation.

<!-- snippet: shadwick-examples/src/main/java/dev/jbaby/shadwick/examples/SecondaryNavLayout.java -->
```java
package dev.jbaby.shadwick.examples;

import dev.jbaby.shadwick.Sw;
import dev.jbaby.shadwick.behaviors.SidebarBehavior;
import dev.jbaby.shadwick.components.MenuItem;
import dev.jbaby.shadwick.components.SwAppLayout;
import dev.jbaby.shadwick.components.SwIcon;
import dev.jbaby.shadwick.components.SwUserMenu;

import org.apache.wicket.Component;
import org.apache.wicket.markup.head.CssHeaderItem;
import org.apache.wicket.markup.head.IHeaderResponse;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.link.BookmarkablePageLink;
import org.apache.wicket.markup.html.panel.Fragment;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.request.mapper.parameter.PageParameters;
import org.apache.wicket.request.resource.CssResourceReference;

import java.util.List;

/**
 * Layout recipe "Inset with secondary navigation": sections that are pages of their own, with a chevron that opens
 * their sub-menu, then the projects; at the bottom a secondary navigation (support, feedback) above the user menu.
 * The page is a card beside the sidebar.
 */
public abstract class SecondaryNavLayout extends SwAppLayout {

    protected SecondaryNavLayout(PageParameters parameters) {
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
        return Model.of("Fieldwork");
    }

    @Override
    protected IModel<List<MenuItem>> sidebarMenuItemsModel() {
        return Model.ofList(List.of(
                MenuItem.group("Platform",
                        page("Inbox", "inbox").withIcon("mail").withBadge("3"),
                        page("Calendar", "calendar").withIcon("calendar")
                                .withChildren(page("Week", "week"), page("Month", "month")),
                        page("Reports", "reports").withIcon("chart-line")
                                .withChildren(page("Sales", "sales"), page("Visits", "visits"))),
                MenuItem.group("Projects",
                        page("Harbor survey", "harbor").withIcon("map-pin"),
                        page("North orchard", "orchard").withIcon("map-pin"))));
    }

    @Override
    protected boolean isCurrentPage(MenuItem item) {
        return item.pageClass() == getClass() && item.parameters() != null
                && item.parameters().get("view").toString().equals(getPageParameters().get("view").toString("inbox"));
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
        return SidebarBehavior.Collapsible.OFFCANVAS;
    }

    @Override
    protected Component newSidebarHeader(String id) {
        return new Fragment(id, "brand", this);
    }

    @Override
    protected Component newSidebarFooter(String id) {
        Fragment footer = new Fragment(id, "secondary", this);
        footer.add(secondaryLink("support", "Support", "circle-help"), secondaryLink("feedback", "Feedback", "send"));
        SwUserMenu user = new SwUserMenu("user", Model.of("Grace Hopper")).setDetail("grace@example.com");
        user.addLink("Account", getClass(), new PageParameters().set("view", "account"));
        footer.add(user);
        return footer;
    }

    private BookmarkablePageLink<Void> secondaryLink(String view, String label, String icon) {
        BookmarkablePageLink<Void> link = new BookmarkablePageLink<>(view, getClass(), new PageParameters().set("view", view));
        link.add(Sw.Behaviors.sidebarMenuButton());
        link.add(new SwIcon("icon", icon), new Label("label", label).add(Sw.Behaviors.sidebarMenuLabel()));
        return link;
    }

    @Override
    protected void onInitialize() {
        super.onInitialize();
        add(new Label("title", title()));
    }

    @Override
    public void renderHead(IHeaderResponse response) {
        super.renderHead(response);
        response.render(CssHeaderItem.forReference(new CssResourceReference(SecondaryNavLayout.class, "SecondaryNavLayout.css")));
    }
}
```

<!-- snippet: shadwick-examples/src/main/resources/dev/jbaby/shadwick/examples/SecondaryNavLayout.html -->
```html
<wicket:extend xmlns:wicket="http://wicket.apache.org">
<div class="app-page">
  <h1 wicket:id="title" class="app-page-title"></h1>
  <wicket:child/>
</div>

<wicket:fragment wicket:id="brand">
  <span class="app-brand"><span class="app-logo" aria-hidden="true">F</span><strong>Fieldwork</strong></span>
</wicket:fragment>

<wicket:fragment wicket:id="secondary">
  <nav class="app-secondary" aria-label="Help">
    <ul class="sw-sidebar-menu">
      <li class="sw-sidebar-menu-item"><a wicket:id="support" data-size="sm"><svg wicket:id="icon"></svg><span wicket:id="label"></span></a></li>
      <li class="sw-sidebar-menu-item"><a wicket:id="feedback" data-size="sm"><svg wicket:id="icon"></svg><span wicket:id="label"></span></a></li>
    </ul>
  </nav>
  <div wicket:id="user"></div>
</wicket:fragment>
</wicket:extend>
```

## A page

The page shows one of the menu's views, picked by its `view` parameter. Replace it with your own pages.

<!-- snippet: shadwick-examples/src/main/java/dev/jbaby/shadwick/examples/SecondaryNavPage.java -->
```java
package dev.jbaby.shadwick.examples;

import org.apache.wicket.request.mapper.parameter.PageParameters;

import java.util.Map;

/** A page in the "Inset with secondary navigation" layout: the view parameter picks which page it shows. */
public class SecondaryNavPage extends SecondaryNavLayout {

    private static final Map<String, String> TITLES = Map.ofEntries(
            Map.entry("inbox", "Inbox"), Map.entry("calendar", "Calendar"), Map.entry("week", "This week"),
            Map.entry("month", "This month"), Map.entry("reports", "Reports"), Map.entry("sales", "Sales"),
            Map.entry("visits", "Visits"), Map.entry("harbor", "Harbor survey"), Map.entry("orchard", "North orchard"),
            Map.entry("support", "Support"), Map.entry("feedback", "Feedback"), Map.entry("account", "Account"));

    public SecondaryNavPage(PageParameters parameters) {
        super(parameters);
    }

    @Override
    protected String title() {
        return TITLES.getOrDefault(getPageParameters().get("view").toString("inbox"), "Inbox");
    }
}
```

<!-- snippet: shadwick-examples/src/main/resources/dev/jbaby/shadwick/examples/SecondaryNavPage.html -->
```html
<wicket:extend xmlns:wicket="http://wicket.apache.org">
<p class="app-lead">Calendar and Reports are pages of their own; the chevron beside them opens their sub-menu.</p>
<div class="app-cards">
  <section class="sw-card"><div class="sw-card-content"><p class="app-muted">Visits today</p><p class="app-figure">6</p></div></section>
  <section class="sw-card"><div class="sw-card-content"><p class="app-muted">Reports due</p><p class="app-figure">2</p></div></section>
</div>
<p class="app-muted">An example page of a layout recipe.
  <a href="https://danielbartl.github.io/shadwick/docs/layouts/secondary-navigation/">Read its code in the Shadwick docs</a>.</p>
</wicket:extend>
```

## The CSS

Shadwick's CSS styles the components, not the page around them, and has no utility classes. The rest is the
application's own CSS: plain CSS on the color tokens, so it follows the skin and the dark theme.

<!-- snippet: shadwick-examples/src/main/resources/dev/jbaby/shadwick/examples/SecondaryNavLayout.css -->
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

/* The help links sit just above the user menu, a little apart from the main navigation. */
.app-secondary {
  margin-bottom: 0.5rem;
}

.app-secondary .sw-sidebar-menu {
  margin: 0;
  padding: 0;
  list-style: none;
}

@media (width < 48rem) {
  .app-page {
    padding-inline: 1rem;
  }
}
```

## Variations

- **Sections that only open.** `MenuItem.submenu` makes a section's label open its pages instead of being a
  page itself; see [Collapsible sub-menus](collapsible-sub-menus.md).
