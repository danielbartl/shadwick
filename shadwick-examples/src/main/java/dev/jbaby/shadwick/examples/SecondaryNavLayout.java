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
