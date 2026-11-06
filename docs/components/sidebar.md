# Sidebar

`SwSidebar` renders native navigation links and disclosure groups using Oat's `MenuItem` API. Bind its `<nav>`
directly inside a provider, beside the content inset:

```java
WebMarkupContainer provider = new WebMarkupContainer("layout");
provider.add(Sw.Behaviors.sidebarProvider());
SwSidebar sidebar = new SwSidebar("sidebar", Model.ofList(List.of(
        MenuItem.of("Overview", OverviewPage.class).withIcon("house"),
        MenuItem.group("Workspace",
                MenuItem.of("Inbox", InboxPage.class).withIcon("mail").withBadge(unreadCount),
                MenuItem.of("Projects", ProjectsPage.class).withIcon("folder")))))
        .setCollapsible(SidebarBehavior.Collapsible.ICON);
WebMarkupContainer content = new WebMarkupContainer("content");
content.add(Sw.Behaviors.sidebarInset());
content.add(new SwSidebarTrigger("toggle", sidebar));
provider.add(sidebar, content);
add(provider);
```

```html
<div wicket:id="layout">
    <nav wicket:id="sidebar"></nav>
    <main wicket:id="content">
        <header><button wicket:id="toggle"></button></header>
        <!-- Page content -->
    </main>
</div>
```

At 48rem and above, the sidebar is a sticky column. `setCollapsible` chooses `OFFCANVAS` (default), `ICON`, or
`NONE`; `setVariant` chooses `SIDEBAR` (default), `FLOATING`, or `INSET`. `setSide(END)` puts it on the logical end,
including RTL. On smaller screens the same navigation opens as a native popover with a close button. The enhanced
panel focuses the current link, makes the background inert, traps Tab, and returns focus on Escape or dismissal.
The trigger and Ctrl/Cmd+B toggle it; the shortcut leaves text fields and modal dialogs alone. Link navigation
closes the mobile panel. Reduced motion, forced colors and printing have dedicated styles.

Desktop collapse is a browser preference, remembered in the `sw-sidebar-state` cookie. Configure the provider
with `setDefaultState`, `setRememberState(false)` or a distinct `setCookieName` for independently remembered
sidebars. The cookie contains only `expanded`/`collapsed` and is not server-side open state. Native mobile opening
and navigation also work without JavaScript; desktop navigation stays expanded in that case. Focus trapping,
background inertness, shortcuts, icon hints and desktop collapse use the shared library script.

Links retain ordinary Tab navigation and have no menu roles. The current page uses `aria-current="page"`, matching
its page class and any supplied parameters exactly. Override `isCurrentPage` for application-specific route matching
or `newMenuLink` for custom links, including Ajax links. Unauthorized pages and groups without authorized children
are omitted. Groups are one level deep and open when one of their children is current. Labels and badges remain
escaped, and a missing icon uses a generic circle. Badges hide when null, blank or numeric zero; visible badges
describe their links, including in the icon rail. Hover/focus hints appear only in the desktop icon rail.

Use `setHeader`/`setFooter` with components named `SwSidebar.HEADER_ID`/`FOOTER_ID` for caller-owned content.
`setLabel(String/IModel<String>)` names the navigation; resource keys are `SwSidebar.label`, `SwSidebar.close` and
`SidebarTriggerBehavior.label`. `target.add(sidebar.getMenu())` refreshes badges/links without closing the mobile
panel. Replacing the whole sidebar or provider closes it and releases background inertness. Caller-owned inert
regions stay inert. The `sw-sidebar-toggle` browser event exposes `{state, open, mobile}`.

`Sw.Behaviors.sidebar()`, `sidebarProvider()`, `sidebarTrigger(component)` and `sidebarInset()` also support
caller-owned anatomy; header/content/footer/menu/menu-item/menu-button/menu-label helpers provide the corresponding
classes. The **Sidebar** showcase (`/sidebar`) demonstrates layouts, real routes and Ajax updates. The landing
studio includes the same responsive navigation with persistence disabled.

## Submenus and actions

Labelled `MenuItem.group(...)` sections can now contain submenus as well as ordinary links. A submenu summary opens
its child links; a page with children keeps its own link and has a separate, named disclosure beside it:

```java
MenuItem.submenu("Projects",
        MenuItem.of("Recent", RecentProjectsPage.class),
        MenuItem.of("Favorites", FavoritesPage.class)).withIcon("folder");

MenuItem.of("Settings", SettingsPage.class).withIcon("settings").withChildren(
        MenuItem.of("Billing", BillingPage.class),
        MenuItem.of("Members", MembersPage.class));
```

Both use native `details`/`summary`, including Enter and Space without JavaScript. Current authorized descendants
open their submenu and labelled section. Activating a submenu's icon in the collapsed rail expands the sidebar and
opens that submenu. Children are leaf page links; labelled groups cannot nest. The existing five/six-argument
`MenuItem` constructors remain available. A denied parent page hides its whole entry; a disclosure without a parent
page hides when it has no authorized children. An authorized parent link stays available when all its children hide.

Override `newMenuAction(String id, MenuItem item)` or `newGroupAction(String id, MenuItem group)` in `SwSidebar`
to return a component bound to a native `<button>`. Defaults are hidden. The supplied body has a decorative
ellipsis for an item and a plus for a group; name the action with `aria-label`, including its context:

```java
@Override
protected Component newMenuAction(String id, MenuItem item) {
    return new AjaxLink<Void>(id) {
        @Override public void onClick(AjaxRequestTarget target) {
            // Update application state, then refresh badges without closing the mobile panel.
            target.add(getMenu());
        }
    }.add(AttributeModifier.replace("aria-label", "Refresh " + item.label().getObject()));
}
```

Action hooks are for ordinary items and group headings; submenu parents use their disclosure control instead.
Actions and badges have separate space at a link's end. Actions hide in the icon rail; group actions sit outside
the group's summary. `data-show-on-hover` keeps an item action visible on keyboard focus and while its menu is open,
and always on mobile. For caller-owned buttons/links, `Sw.Behaviors.sidebarMenuAction()` and `.sidebarGroupAction()`
reuse the same styling and accessible-name checks; `setLabel(String/IModel)` supplies the name and
`setShowOnHover(true)` configures an item action. Existing dropdown behaviors can turn a caller-owned action into a
native menu trigger. The disclosure label resource is `SwSidebar.pages` (“{0} pages”).

## Navigation in the header

`sidebar.setPlacement(SidebarBehavior.Placement.HEADER)` puts the same navigation in a `SwSiteHeader` inside the
provider, with the inset below the header. On desktop it becomes a horizontally scrolling row and never collapses;
its trigger hides, Ctrl/Cmd+B leaves it alone, and it does not change the collapse cookie. On mobile it is the same
panel, with the existing focus and inertness behavior. The `SwAppLayout` base page below composes this automatically.

The desktop row shows labels, badges and item actions. Group headings are visually hidden but still name their
sections. Pageless submenu disclosures and submenu children hide in the row; a parent page link stays available.
This is a row of navigation links, with no hover dropdown navigation widget. Use `MenuItem.withPanelOnly(true)` for
entries or groups that belong only in the mobile panel. Caller-owned parts use `data-panel-only` on the element.
`INSET` makes the page a card below the header; the other variants leave it plain. The row keeps the page within its
viewport when links overflow, including keyboard focus moving to a scrolled link.
