package dev.jbaby.shadwick.examples;

import dev.jbaby.shadwick.Sw;
import dev.jbaby.shadwick.behaviors.DropdownMenuBehavior;
import dev.jbaby.shadwick.behaviors.SidebarBehavior;
import dev.jbaby.shadwick.components.MenuItem;
import dev.jbaby.shadwick.components.SwAppLayout;

import org.apache.wicket.AttributeModifier;
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
 * Layout recipe "Grouped by section": pages in labelled groups, with a version switcher and a search field at the
 * top of the sidebar, as documentation has. On a desktop the sidebar slides out of view; on a phone it is a panel.
 */
public abstract class SectionsLayout extends SwAppLayout {

    private static final List<String> VERSIONS = List.of("2.0", "1.4", "1.3");

    protected SectionsLayout(PageParameters parameters) {
        super(parameters);
    }

    /** The page's title. */
    protected abstract String title();

    /** The page for a view of this documentation. */
    protected MenuItem page(String label, String view) {
        return MenuItem.of(label, getClass(), new PageParameters().set("view", view));
    }

    @Override
    protected IModel<String> appNameModel() {
        return Model.of("Clover Docs");
    }

    @Override
    protected IModel<List<MenuItem>> sidebarMenuItemsModel() {
        return Model.ofList(List.of(
                MenuItem.group("Getting started",
                        page("Introduction", "introduction"),
                        page("Installation", "installation"),
                        page("Project structure", "structure")),
                MenuItem.group("Building your app",
                        page("Routing", "routing"),
                        page("Forms", "forms"),
                        page("Data", "data")),
                MenuItem.group("Reference",
                        page("Components", "components"),
                        page("Configuration", "configuration"))));
    }

    @Override
    protected boolean isCurrentPage(MenuItem item) {
        return item.pageClass() == getClass() && item.parameters() != null
                && item.parameters().get("view").toString().equals(getPageParameters().get("view").toString("introduction"));
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
        Fragment header = new Fragment(id, "versionAndSearch", this);
        WebMarkupContainer trigger = new WebMarkupContainer("trigger");
        trigger.add(Sw.Behaviors.sidebarMenuButton());
        trigger.add(new Label("version", "v" + currentVersion()));
        WebMarkupContainer versions = new WebMarkupContainer("versions");
        versions.add(Sw.Behaviors.dropdownMenu(trigger).setSide(DropdownMenuBehavior.Side.BOTTOM));
        versions.add(new ListView<>("versionList", VERSIONS) {
            @Override
            protected void populateItem(ListItem<String> item) {
                BookmarkablePageLink<Void> link = new BookmarkablePageLink<>("link", SectionsLayout.this.getClass(),
                        new PageParameters(getPageParameters()).set("version", item.getModelObject()));
                link.add(Sw.Behaviors.dropdownMenuRadioItem(Model.of(item.getModelObject().equals(currentVersion()))));
                link.add(new Label("name", "v" + item.getModelObject()));
                item.add(link);
            }
        });
        // A plain GET form: the search works without JavaScript, and its URL can be shared
        WebMarkupContainer search = new WebMarkupContainer("search");
        search.add(AttributeModifier.replace("action", urlFor(getClass(), new PageParameters()).toString()));
        WebMarkupContainer query = new WebMarkupContainer("query");
        query.add(AttributeModifier.replace("value", getPageParameters().get("q").toString("")));
        search.add(query);
        header.add(trigger, versions, search);
        return header;
    }

    @Override
    protected void onInitialize() {
        super.onInitialize();
        add(new Label("title", title()));
    }

    private String currentVersion() {
        String version = getPageParameters().get("version").toString(VERSIONS.get(0));
        return VERSIONS.contains(version) ? version : VERSIONS.get(0);
    }

    @Override
    public void renderHead(IHeaderResponse response) {
        super.renderHead(response);
        response.render(CssHeaderItem.forReference(new CssResourceReference(SectionsLayout.class, "SectionsLayout.css")));
    }
}
