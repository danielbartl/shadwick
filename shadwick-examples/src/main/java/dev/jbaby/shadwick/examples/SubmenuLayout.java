package dev.jbaby.shadwick.examples;

import dev.jbaby.shadwick.behaviors.SidebarBehavior;
import dev.jbaby.shadwick.components.MenuItem;
import dev.jbaby.shadwick.components.SwAppLayout;

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
 * Layout recipe "Collapsible sub-menus": sections that open and close, each with its pages in a sub-menu, in a
 * floating sidebar. Only the current page's section is open when a page loads; the others open with a click.
 */
public abstract class SubmenuLayout extends SwAppLayout {

    protected SubmenuLayout(PageParameters parameters) {
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
        return Model.of("Northwind");
    }

    @Override
    protected IModel<List<MenuItem>> sidebarMenuItemsModel() {
        return Model.ofList(List.of(MenuItem.group("Guide",
                MenuItem.submenu("Getting started",
                        page("Installation", "installation"),
                        page("Project structure", "structure")).withIcon("package"),
                MenuItem.submenu("Building your app",
                        page("Routing", "routing"),
                        page("Data fetching", "data"),
                        page("Rendering", "rendering"),
                        page("Caching", "caching")).withIcon("file-text"),
                MenuItem.submenu("API reference",
                        page("Components", "components"),
                        page("Functions", "functions")).withIcon("link"),
                MenuItem.submenu("Architecture",
                        page("Accessibility", "accessibility"),
                        page("Supported browsers", "browsers")).withIcon("building-2"))));
    }

    @Override
    protected boolean isCurrentPage(MenuItem item) {
        return item.pageClass() == getClass() && item.parameters() != null
                && item.parameters().get("view").toString().equals(getPageParameters().get("view").toString("installation"));
    }

    @Override
    protected SidebarBehavior.Variant sidebarVariant() {
        return SidebarBehavior.Variant.FLOATING;
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
    protected void onInitialize() {
        super.onInitialize();
        add(new Label("title", title()));
    }

    @Override
    public void renderHead(IHeaderResponse response) {
        super.renderHead(response);
        response.render(CssHeaderItem.forReference(new CssResourceReference(SubmenuLayout.class, "SubmenuLayout.css")));
    }
}
