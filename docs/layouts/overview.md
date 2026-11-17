# Layout recipes

Whole page frames to copy into an application: the layout, a page that uses it and the CSS around it.

A layout is the frame every page of an application shares: the navigation, the page's header, the skip link, the
place where the page's content goes. In Wicket it is a page class the application's pages extend, usually a subclass
of [`SwAppLayout`](../components/app-layout.md), with markup that wraps the pages' own `<wicket:extend>` content. The
recipes here each have:

- **The layout**: an abstract page class and its markup. The markup holds the parts the layout passes to the
  sidebar and header (a team switcher, a user menu) as `wicket:fragment`s, so a layout is one class and one file.
- **A page**: a page that extends it, passing its title and content. Each recipe's page shows the views of its own
  menu through a `view` parameter, so every link in the preview works.
- **The CSS**: what the layout needs of the application's own CSS. Shadwick's CSS styles the components, not the page
  between them, and has no utility classes, so the page's padding, a logo's square or the cards' grid are yours. It is
  plain CSS on Shadwick's color tokens, so it follows the skin and the dark theme.

Every recipe runs in the examples application, in every skin and both themes, on desktops and phones. The code on
these pages is that application's code, checked to be the same on every build.

## Using one

Copy the layout's class and markup into your application's package, its CSS next to them, and replace the menu with
your own pages:

- **Pages and the current page.** `sidebarMenuItemsModel()` lists your pages as `MenuItem`s; the sidebar marks the one
  shown with `aria-current="page"`, matching its page class and parameters. When pages share a class and differ by a
  parameter, as the recipes' do, override `isCurrentPage` to compare just that parameter.
- **Your own CSS.** The recipes render their CSS from `renderHead`; an application with one stylesheet can merge it
  there instead. Rename the `app-` classes as you like.
- **Your own frame.** The layout is yours from then on: Shadwick does not update it, and nothing in it is configuration.

What every layout keeps: one `main` the first link skips to, the page's language, the theme applied before the page
is painted, and on a phone the same navigation in a panel with the keyboard trapped in it until it closes.

## The recipes

| Recipe | For |
|---|---|
| [Collapses to icons](collapses-to-icons.md) | An application with a team switcher and sections, collapsing to a strip of icons |
| [Grouped by section](grouped-by-section.md) | Documentation: labelled groups of pages, a version switcher and search |
| [Collapsible sub-menus](collapsible-sub-menus.md) | Sections that open and close, in a floating sidebar |
| [Inset with secondary navigation](secondary-navigation.md) | Section pages with sub-menus, projects, help links and the user menu |
| [Table of contents on the end](table-of-contents.md) | A long page's sections at the end of the line, built from the parts directly |
| [Navigation in the header](header-navigation.md) | A handful of pages as a row in the site header |

The [Sidebar](../components/sidebar.md) and [App layout](../components/app-layout.md) guides have every option the
recipes use, and the [App shell](../components/app-shell.md) guide the site header, breadcrumb and skip link.
