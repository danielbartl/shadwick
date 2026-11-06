# App layout

Extend `SwAppLayout` to share a complete application shell across pages. It follows OatAppLayout's constructors and
protected model/factory hooks, using the existing Shadwick components:

```java
public abstract class WorkspaceLayout extends SwAppLayout {
    protected WorkspaceLayout(PageParameters parameters) { super(parameters); }

    @Override protected IModel<String> appNameModel() { return Model.of("My workspace"); }
    @Override protected IModel<List<MenuItem>> sidebarMenuItemsModel() {
        return Model.ofList(List.of(
                MenuItem.of("Overview", OverviewPage.class).withIcon("house"),
                MenuItem.of("Inbox", InboxPage.class).withIcon("mail")));
    }

    // Omit this override for a sidebar with an icon rail.
    @Override protected SidebarBehavior.Placement sidebarPlacement() {
        return SidebarBehavior.Placement.HEADER;
    }
}
```

Concrete pages pass their `PageParameters` to `super`, add their usual components with `add(...)`, and put their
markup in `<wicket:extend>…</wicket:extend>`. A single focusable `<main id="sw-main">` receives that content. The first
link skips to it, `lang` follows the page locale, and the priority theme script applies browser preferences before
paint. The defaults are an inset sidebar, icon collapse, remembered desktop state and a header theme menu. Layout
hooks run in `onInitialize`, after the subclass constructor has initialized its fields. Page spacing and content
typography belong to the application.

Override `appTitleModel()` for the document title (`SwAppLayout.title` by default; the name resource is
`SwAppLayout.name`). `newTopNavExtra(id)` supplies header actions such as a panel containing a theme toggle and
header `SwUserMenu`. `newSidebarHeader(id)` supplies branding, and `newSidebarFooter(id)` supplies a footer such as
`SwUserMenu`; use the supplied IDs. In header placement, sidebar header/footer slots are mobile-only.

`sidebarVariant()` and `sidebarCollapsible()` select the appearance; `newSidebarProviderBehavior()` configures the
cookie, initial state or disabled persistence. `newThemeBehavior()` configures appearance or returns null for
application-owned appearance; also replace the default theme control if the app supplies another. `newSidebar(id)`
can replace the composed sidebar. `newMenuLink`, `newMenuIcon`, `isCurrentPage`, `isAuthorized`, `newMenuAction` and
`newGroupAction` expose the same hooks on the base page. Default route matching uses the exact page class and supplied
parameters; override it when UI query parameters should not affect the current entry.

After initialization, `getSidebar()`, `getTopNav()`, `getMain()` and `getProvider()` expose the shell. Use
`target.add(getSidebar().getMenu())` to refresh navigation in place; a whole sidebar replacement closes its panel.
The **App layouts** showcases (`/layouts` and `/header-layout`) share content and routes across both compositions,
with submenu navigation, badge actions, Ajax refresh/replacement and theme preferences. The landing studio's Layout
selector moves its existing navigation between the two compositions without storing a preference.
