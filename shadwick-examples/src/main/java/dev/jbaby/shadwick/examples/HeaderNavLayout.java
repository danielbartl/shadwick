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
