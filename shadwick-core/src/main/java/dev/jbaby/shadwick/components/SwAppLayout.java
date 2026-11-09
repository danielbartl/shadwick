package dev.jbaby.shadwick.components;

import dev.jbaby.shadwick.behaviors.SidebarBehavior;
import dev.jbaby.shadwick.behaviors.SidebarProviderBehavior;
import dev.jbaby.shadwick.behaviors.StyledBehavior;
import dev.jbaby.shadwick.behaviors.ThemeBehavior;

import org.apache.wicket.AttributeModifier;
import org.apache.wicket.Component;
import org.apache.wicket.markup.html.TransparentWebMarkupContainer;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.WebPage;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.link.AbstractLink;
import org.apache.wicket.markup.html.link.BookmarkablePageLink;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.model.StringResourceModel;
import org.apache.wicket.request.mapper.parameter.PageParameters;

import java.util.List;

/**
 * Reusable application base page, with OatAppLayout's model and composition hooks. Put page content
 * in subclass wicket:extend markup; normal page.add(...) works there. One SwSidebar provides either
 * a desktop column or a header row, and the mobile panel.
 */
public abstract class SwAppLayout extends WebPage {

    private SwSidebar sidebar;
    private SwSiteHeader topNav;
    private WebMarkupContainer main;
    private WebMarkupContainer provider;

    public SwAppLayout() {
        this((PageParameters) null);
    }

    public SwAppLayout(PageParameters parameters) {
        super(parameters);
    }

    public SwAppLayout(IModel<?> model) {
        super(model);
    }

    @Override
    protected void onInitialize() {
        super.onInitialize();
        addDocument();
        boolean header = sidebarPlacement() == SidebarBehavior.Placement.HEADER;
        WebMarkupContainer workspace = addWorkspace(header);
        addTopNavigation(workspace);
        addSidebar(header);
    }

    private void addDocument() {
        TransparentWebMarkupContainer html = new TransparentWebMarkupContainer("html");
        html.add(
                AttributeModifier.replace(
                        "lang", (IModel<String>) () -> getLocale().toLanguageTag()));
        ThemeBehavior theme = newThemeBehavior();
        if (theme != null) html.add(theme);
        add(html, new Label("appTitle", appTitleModel()));
    }

    private WebMarkupContainer addWorkspace(boolean header) {
        provider = new TransparentWebMarkupContainer("provider");
        provider.add(newSidebarProviderBehavior(), new StyledBehavior("sw-app-layout"));
        add(provider);
        WebMarkupContainer workspace = new TransparentWebMarkupContainer("workspace");
        workspace.setRenderBodyOnly(header);
        if (!header) workspace.add(new StyledBehavior("sw-sidebar-inset"));
        provider.add(workspace);
        main = new TransparentWebMarkupContainer("main");
        main.setMarkupId("sw-main");
        if (header) main.add(new StyledBehavior("sw-sidebar-inset"));
        workspace.add(main);
        add(new SwSkipLink("skip", main));
        return workspace;
    }

    private void addTopNavigation(WebMarkupContainer workspace) {
        topNav =
                new SwSiteHeader("topNav")
                        .setInset(sidebarVariant() == SidebarBehavior.Variant.INSET);
        workspace.add(topNav);
        topNav.add(new Label("appName", appNameModel()), newTopNavExtra("topNavExtra"));
    }

    private void addSidebar(boolean header) {
        sidebar = newSidebar(header ? "headerNavigation" : "sidebar");
        sidebar.setPlacement(sidebarPlacement())
                .setVariant(sidebarVariant())
                .setCollapsible(sidebarCollapsible());
        Component brand = newSidebarHeader(SwSidebar.HEADER_ID),
                footer = newSidebarFooter(SwSidebar.FOOTER_ID);
        if (header) {
            brand.add(AttributeModifier.replace("data-panel-only", ""));
            footer.add(AttributeModifier.replace("data-panel-only", ""));
        }
        sidebar.setHeader(brand).setFooter(footer);
        if (header) {
            provider.add(new WebMarkupContainer("sidebar").setVisible(false));
            topNav.add(sidebar);
        } else {
            provider.add(sidebar);
            topNav.add(new WebMarkupContainer("headerNavigation").setVisible(false));
        }
        topNav.add(new SwSidebarTrigger("trigger", sidebar));
    }

    protected IModel<String> appTitleModel() {
        return new StringResourceModel("SwAppLayout.title", this);
    }

    protected IModel<String> appNameModel() {
        return new StringResourceModel("SwAppLayout.name", this);
    }

    protected IModel<List<MenuItem>> sidebarMenuItemsModel() {
        return Model.ofList(List.of());
    }

    protected SidebarBehavior.Placement sidebarPlacement() {
        return SidebarBehavior.Placement.SIDEBAR;
    }

    protected SidebarBehavior.Variant sidebarVariant() {
        return SidebarBehavior.Variant.INSET;
    }

    protected SidebarBehavior.Collapsible sidebarCollapsible() {
        return SidebarBehavior.Collapsible.ICON;
    }

    protected SidebarProviderBehavior newSidebarProviderBehavior() {
        return new SidebarProviderBehavior();
    }

    /** Return null to keep application-owned server appearance instead of browser preferences. */
    protected ThemeBehavior newThemeBehavior() {
        return new ThemeBehavior();
    }

    protected Component newTopNavExtra(String id) {
        return new SwThemeToggle(id);
    }

    protected Component newSidebarHeader(String id) {
        return new Label(id, appNameModel());
    }

    protected Component newSidebarFooter(String id) {
        return new WebMarkupContainer(id).setVisible(false);
    }

    protected SwSidebar newSidebar(String id) {
        return new SwSidebar(id, sidebarMenuItemsModel()) {

            @Override
            protected AbstractLink newMenuLink(String id, MenuItem item) {
                return SwAppLayout.this.newMenuLink(id, item);
            }

            @Override
            protected Component newMenuIcon(String id, MenuItem item) {
                return SwAppLayout.this.newMenuIcon(id, item);
            }

            @Override
            protected boolean isCurrentPage(MenuItem item) {
                return SwAppLayout.this.isCurrentPage(item);
            }

            @Override
            protected boolean isAuthorized(MenuItem item) {
                return SwAppLayout.this.isAuthorized(item);
            }

            @Override
            protected Component newMenuAction(String id, MenuItem item) {
                return SwAppLayout.this.newMenuAction(id, item);
            }

            @Override
            protected Component newGroupAction(String id, MenuItem group) {
                return SwAppLayout.this.newGroupAction(id, group);
            }
        };
    }

    protected AbstractLink newMenuLink(String id, MenuItem item) {
        return new BookmarkablePageLink<Void>(id, item.pageClass(), item.parameters());
    }

    protected Component newMenuIcon(String id, MenuItem item) {
        return new SwIcon(id, item.icon() != null ? item.icon() : Model.of("circle"));
    }

    protected boolean isCurrentPage(MenuItem item) {
        return CurrentPage.isLinkedBy(this, item.pageClass(), item.parameters());
    }

    protected boolean isAuthorized(MenuItem item) {
        return getApplication()
                .getSecuritySettings()
                .getAuthorizationStrategy()
                .isInstantiationAuthorized(item.pageClass());
    }

    protected Component newMenuAction(String id, MenuItem item) {
        return new WebMarkupContainer(id).setVisible(false);
    }

    protected Component newGroupAction(String id, MenuItem group) {
        return new WebMarkupContainer(id).setVisible(false);
    }

    public SwSidebar getSidebar() {
        return sidebar;
    }

    public SwSiteHeader getTopNav() {
        return topNav;
    }

    public WebMarkupContainer getMain() {
        return main;
    }

    public WebMarkupContainer getProvider() {
        return provider;
    }
}
