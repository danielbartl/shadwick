# Table of contents on the end

A page's sections in a sidebar at the end of the line, built from the parts directly.

A table of contents in a sidebar at the end of the line: the right in a left-to-right page, the left in a
right-to-left one (`Side.END`). Its entries are links to the page's sections, and its trigger is at the end of the
header. On a desktop the trigger hides it (`Collapsible.OFFCANVAS`); on a phone it opens it as a panel.

This layout is built on a plain `WebPage` from the parts `SwAppLayout` uses: `SwSkipLink`, the sidebar provider,
`SwSidebar`, `SwSiteHeader`, `SwSidebarTrigger` and `SwThemeToggle`. Start from it when a frame doesn't fit
`SwAppLayout`'s.

[Open it live](https://shadwick-production.up.railway.app/recipes/table-of-contents) · runs in the examples application, in every skin.

## The layout

The entries are `MenuItem`s, so the sidebar draws them, but `newMenuLink` makes each a link to its section
(`#site`) rather than to a page. Each section has `tabindex="-1"`, so following a link also moves the keyboard focus
there. The layout's markup is a whole document: the skip link comes first, the table of contents after the page, so
it comes after the content in the reading order too.

<!-- snippet: shadwick-examples/src/main/java/dev/jbaby/shadwick/examples/TableOfContentsLayout.java -->
```java
package dev.jbaby.shadwick.examples;

import dev.jbaby.shadwick.Sw;
import dev.jbaby.shadwick.behaviors.SidebarBehavior;
import dev.jbaby.shadwick.behaviors.ThemeBehavior;
import dev.jbaby.shadwick.components.MenuItem;
import dev.jbaby.shadwick.components.SwSidebar;
import dev.jbaby.shadwick.components.SwSidebarTrigger;
import dev.jbaby.shadwick.components.SwSiteHeader;
import dev.jbaby.shadwick.components.SwSkipLink;
import dev.jbaby.shadwick.components.SwThemeToggle;

import org.apache.wicket.AttributeModifier;
import org.apache.wicket.Component;
import org.apache.wicket.markup.head.CssHeaderItem;
import org.apache.wicket.markup.head.IHeaderResponse;
import org.apache.wicket.markup.html.TransparentWebMarkupContainer;
import org.apache.wicket.markup.html.WebPage;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.link.AbstractLink;
import org.apache.wicket.markup.html.link.ExternalLink;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.request.mapper.parameter.PageParameters;
import org.apache.wicket.request.resource.CssResourceReference;

import java.io.Serializable;
import java.util.List;

/**
 * Layout recipe "On the end, with a table of contents": the page's sections in a sidebar at the end of the line (the
 * right in a left-to-right page), with its trigger at the end of the header. Composed from the building blocks on a
 * plain WebPage, for the frames {@code SwAppLayout} doesn't shape.
 */
public abstract class TableOfContentsLayout extends WebPage {

    /** One section of the page: its heading and the id of the element it starts with. */
    protected record Section(String anchor, String title) implements Serializable {}

    protected TableOfContentsLayout(PageParameters parameters) {
        super(parameters);
    }

    /** The page's title. */
    protected abstract String title();

    /** The page's sections, in order; their elements carry the anchors as ids. */
    protected abstract List<Section> sections();

    @Override
    protected void onInitialize() {
        super.onInitialize();
        TransparentWebMarkupContainer html = new TransparentWebMarkupContainer("html");
        html.add(AttributeModifier.replace("lang", (IModel<String>) () -> getLocale().toLanguageTag()));
        html.add(new ThemeBehavior());
        add(html, new Label("documentTitle", title() + " · Field guide"));

        TransparentWebMarkupContainer main = new TransparentWebMarkupContainer("main");
        main.setMarkupId("main");
        add(new SwSkipLink("skip", main));
        TransparentWebMarkupContainer provider = new TransparentWebMarkupContainer("provider");
        provider.add(Sw.Behaviors.sidebarProvider().setCookieName("app-contents"));
        add(provider);

        SwSidebar contents = newContents();
        provider.add(contents);
        TransparentWebMarkupContainer inset = new TransparentWebMarkupContainer("inset");
        inset.add(Sw.Behaviors.sidebarInset());
        provider.add(inset);
        SwSiteHeader header = new SwSiteHeader("header");
        header.add(new Label("name", "Field guide"), new SwThemeToggle("theme"), new SwSidebarTrigger("trigger", contents));
        inset.add(header, main);
        main.add(new Label("title", title()));
    }

    private SwSidebar newContents() {
        List<MenuItem> entries = sections().stream()
                .map(section -> MenuItem.of(section.title(), getClass(), new PageParameters().set("section", section.anchor())))
                .toList();
        SwSidebar contents = new SwSidebar("contents", Model.ofList(List.of(MenuItem.group(Model.of("On this page"), entries)))) {
            // Each entry is a link to its section on this page, not to another page
            @Override
            protected AbstractLink newMenuLink(String id, MenuItem item) {
                return new ExternalLink(id, "#" + item.parameters().get("section").toString());
            }

            @Override
            protected Component newMenuIcon(String id, MenuItem item) {
                return super.newMenuIcon(id, item).setVisible(false);
            }
        };
        contents.setSide(SidebarBehavior.Side.END).setCollapsible(SidebarBehavior.Collapsible.OFFCANVAS).setGroupsOpen(true);
        contents.setLabel("On this page");
        return contents;
    }

    @Override
    public void renderHead(IHeaderResponse response) {
        super.renderHead(response);
        response.render(CssHeaderItem.forReference(new CssResourceReference(TableOfContentsLayout.class, "TableOfContentsLayout.css")));
    }
}
```

<!-- snippet: shadwick-examples/src/main/resources/dev/jbaby/shadwick/examples/TableOfContentsLayout.html -->
```html
<!doctype html>
<html wicket:id="html" xmlns:wicket="http://wicket.apache.org">
<head>
  <meta charset="utf-8">
  <meta name="viewport" content="width=device-width, initial-scale=1">
  <title wicket:id="documentTitle">Field guide</title>
</head>
<body>
<a wicket:id="skip"></a>
<div wicket:id="provider" class="app-provider">
  <div wicket:id="inset">
    <header wicket:id="header">
      <strong wicket:id="name"></strong>
      <div class="sw-site-header-end"><div wicket:id="theme"></div><button wicket:id="trigger" type="button"></button></div>
    </header>
    <main wicket:id="main" class="app-article">
      <h1 wicket:id="title" class="app-page-title"></h1>
      <wicket:child/>
    </main>
  </div>
  <nav wicket:id="contents"></nav>
</div>
</body>
</html>
```

## A page

The page shows one of the menu's views, picked by its `view` parameter. Replace it with your own pages.

<!-- snippet: shadwick-examples/src/main/java/dev/jbaby/shadwick/examples/TableOfContentsPage.java -->
```java
package dev.jbaby.shadwick.examples;

import org.apache.wicket.request.mapper.parameter.PageParameters;

import java.util.List;

/** A long page in the "On the end, with a table of contents" layout. */
public class TableOfContentsPage extends TableOfContentsLayout {

    public TableOfContentsPage(PageParameters parameters) {
        super(parameters);
    }

    @Override
    protected String title() {
        return "Planting an orchard";
    }

    @Override
    protected List<Section> sections() {
        return List.of(new Section("site", "Choosing a site"), new Section("soil", "Preparing the soil"),
                new Section("trees", "Picking trees"), new Section("planting", "Planting"),
                new Section("care", "The first year"));
    }
}
```

<!-- snippet: shadwick-examples/src/main/resources/dev/jbaby/shadwick/examples/TableOfContentsPage.html -->
```html
<wicket:extend xmlns:wicket="http://wicket.apache.org">
<p class="app-lead">A guide in five parts. The table of contents at the end of the line follows you down the page.</p>
<section id="site" tabindex="-1"><h2>Choosing a site</h2><p>Fruit trees want sun for most of the day and a little
  shelter from the wind. A gentle slope lets cold air run off on a frosty night.</p></section>
<section id="soil" tabindex="-1"><h2>Preparing the soil</h2><p>Loosen the ground a spade deep and work in compost.
  Heavy clay drains better with a raised bed.</p></section>
<section id="trees" tabindex="-1"><h2>Picking trees</h2><p>Choose varieties that ripen at different times, and
  check which ones pollinate each other.</p></section>
<section id="planting" tabindex="-1"><h2>Planting</h2><p>Plant in late autumn or early spring, with the graft above
  the soil. Water well and stake the young tree.</p></section>
<section id="care" tabindex="-1"><h2>The first year</h2><p>Keep the ground around the trunk free of grass, water in
  dry weeks and leave the pruning for the winter.</p></section>
<p class="app-muted">An example page of a layout recipe.
  <a href="https://danielbartl.github.io/shadwick/docs/layouts/table-of-contents/">Read its code in the Shadwick docs</a>.</p>
</wicket:extend>
```

## The CSS

Shadwick's CSS styles the components, not the page around them, and has no utility classes. The rest is the
application's own CSS: plain CSS on the color tokens, so it follows the skin and the dark theme.

<!-- snippet: shadwick-examples/src/main/resources/dev/jbaby/shadwick/examples/TableOfContentsLayout.css -->
```css
/* The application's own CSS around the layout: Shadwick styles the components, not the page between them. */
.app-provider {
  box-sizing: border-box;
}

.app-article {
  display: grid;
  gap: 1rem;
  max-width: 44rem;
  padding: 1.5rem;
  line-height: 1.6;
}

.app-article :is(h2, p) {
  margin: 0;
}

.app-article section {
  display: grid;
  gap: 0.5rem;
  scroll-margin-top: 1rem;
}

.app-page-title {
  margin: 0;
  font-size: 1.75rem;
  font-weight: 600;
  line-height: 1.2;
}

.app-article h2 {
  font-size: 1.125rem;
  font-weight: 600;
}

.app-lead,
.app-muted {
  color: var(--muted-foreground);
}

@media (width < 48rem) {
  .app-article {
    padding-inline: 1rem;
  }
}
```

## Variations

- **At the start.** Without `setSide(Side.END)`, the sidebar is at the start of the line, like the other recipes.
