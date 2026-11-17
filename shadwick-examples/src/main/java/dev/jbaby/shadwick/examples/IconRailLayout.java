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
