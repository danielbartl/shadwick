package dev.jbaby.shadwick.examples;

import dev.jbaby.shadwick.ShadwickSettings;
import dev.jbaby.shadwick.Skin;
import dev.jbaby.shadwick.Sw;
import dev.jbaby.shadwick.ThemeMode;
import dev.jbaby.shadwick.behaviors.ButtonBehavior;
import dev.jbaby.shadwick.behaviors.SidebarBehavior;
import dev.jbaby.shadwick.behaviors.SidebarProviderBehavior;
import dev.jbaby.shadwick.behaviors.ThemeBehavior;
import dev.jbaby.shadwick.components.MenuItem;
import dev.jbaby.shadwick.components.SwAppLayout;
import dev.jbaby.shadwick.components.SwButton;
import org.apache.wicket.AttributeModifier;
import org.apache.wicket.Component;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.markup.html.AjaxLink;
import org.apache.wicket.markup.head.CssHeaderItem;
import org.apache.wicket.markup.head.IHeaderResponse;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.link.BookmarkablePageLink;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.request.mapper.parameter.PageParameters;
import org.apache.wicket.request.resource.CssResourceReference;

import java.util.List;
import java.util.Map;

/** An application using the reusable base page, with the same content in sidebar/header compositions. */
public class AppLayoutPage extends SwAppLayout {
    private final Model<Integer> unread = Model.of(4);
    private final Model<String> status = Model.of("Your workspace is up to date.");

    public AppLayoutPage(PageParameters parameters) {
        super(parameters);
        add(new ExampleSourcePanel("exampleSource", getClass()));
        for (Skin skin : Skin.values()) if (skin.id().equals(parameters.get("skin").toString())) ShadwickSettings.get().setCurrentSkin(skin);
        add(new Label("heading", (IModel<String>) this::viewTitle));
        Label update = new Label("status", status); update.setOutputMarkupId(true); add(update);
        add(new Label("layoutName", sidebarPlacement() == SidebarBehavior.Placement.HEADER ? "Navigation in the header" : "Space for everything"));
        Class<? extends AppLayoutPage> other = this instanceof HeaderLayoutPage ? AppLayoutPage.class : HeaderLayoutPage.class;
        add(new BookmarkablePageLink<Void>("otherLayout", other,
                new PageParameters(parameters)).add(Sw.Behaviors.button(ButtonBehavior.Variant.OUTLINE)));
        add(new BookmarkablePageLink<Void>("showcase", HomePage.class).add(Sw.Behaviors.button(ButtonBehavior.Variant.GHOST)));
        add(SwButton.onClick("refreshMenu", "Refresh navigation", target -> {
            unread.setObject(unread.getObject() + 1); status.setObject("Navigation refreshed. Your panel stays open.");
            target.add(getSidebar().getMenu(), update);
        }).setVariant(ButtonBehavior.Variant.OUTLINE));
        add(SwButton.onClick("replaceSidebar", "Replace navigation", target -> target.add(getSidebar()))
                .setVariant(ButtonBehavior.Variant.OUTLINE));
        for (Skin skin : Skin.values()) add(new BookmarkablePageLink<Void>(skin.id(), getClass(),
                new PageParameters(parameters).set("skin", skin.id())).add(Sw.Behaviors.button(ButtonBehavior.Variant.GHOST).setSize(ButtonBehavior.Size.SMALL)));
    }

    private String viewTitle() {
        return Map.of("overview", "A good place to start.", "inbox", "A little room for conversation.",
                "projects", "Ideas, taking shape.", "recent", "Pick up where you left off.", "favorites", "Keep the good ones close.",
                "settings", "Make it your workspace.", "billing", "The practical details.", "members", "Good company.")
                .getOrDefault(getPageParameters().get("view").toString("overview"), "A good place to start.");
    }
    private MenuItem page(String label, String view, String icon) {
        return MenuItem.of(label, getClass(), new PageParameters(getPageParameters()).set("view", view)).withIcon(icon);
    }
    @Override protected IModel<String> appNameModel() { return Model.of("Clover workspace"); }
    @Override protected IModel<String> appTitleModel() { return Model.of(viewTitle() + " · Shadwick"); }
    @Override protected IModel<List<MenuItem>> sidebarMenuItemsModel() {
        MenuItem[] projects = {page("Recent", "recent", "clock"), page("Favorites", "favorites", "heart")};
        MenuItem project = sidebarPlacement() == SidebarBehavior.Placement.HEADER
                ? page("Projects", "projects", "folder").withChildren(projects)
                : MenuItem.submenu("Projects", projects).withIcon("folder");
        return Model.ofList(List.of(MenuItem.group("Workspace", page("Overview", "overview", "house"),
                page("Inbox", "inbox", "mail").withBadge(unread), project,
                page("Settings", "settings", "settings").withChildren(page("Billing", "billing", "credit-card"), page("Members", "members", "users"))),
                MenuItem.group("Explore", MenuItem.of("Component showcase", HomePage.class).withIcon("package"))
                        .withPanelOnly(sidebarPlacement() == SidebarBehavior.Placement.HEADER)));
    }
    @Override protected boolean isCurrentPage(MenuItem item) {
        return item.pageClass() == getClass() && item.parameters() != null
                && item.parameters().get("view").toString().equals(getPageParameters().get("view").toString("overview"));
    }
    @Override protected ThemeBehavior newThemeBehavior() {
        ThemeBehavior theme = new ThemeBehavior().setStorageKey("shadwick-example-theme");
        String mode = getPageParameters().get("mode").toString();
        if ("dark".equals(mode)) theme.setDefaultMode(ThemeMode.DARK);
        if ("light".equals(mode)) theme.setDefaultMode(ThemeMode.LIGHT);
        return theme;
    }
    @Override protected SidebarProviderBehavior newSidebarProviderBehavior() {
        return new SidebarProviderBehavior().setCookieName("shadwick-layout-sidebar");
    }
    @Override protected Component newMenuAction(String id, MenuItem item) {
        if (!"Inbox".equals(item.label().getObject())) return super.newMenuAction(id, item);
        return new AjaxLink<Void>(id) {
            @Override public void onClick(AjaxRequestTarget target) {
                unread.setObject(0); target.add(getSidebar().getMenu());
                Sw.toast(target, "All caught up. A little more room to focus.");
            }
        }.add(AttributeModifier.replace("aria-label", "Mark Inbox as read"), AttributeModifier.replace("data-show-on-hover", ""));
    }
    @Override protected Component newGroupAction(String id, MenuItem item) {
        if (!"Workspace".equals(item.label().getObject())) return super.newGroupAction(id, item);
        return new AjaxLink<Void>(id) {
            @Override public void onClick(AjaxRequestTarget target) {
                unread.setObject(unread.getObject() + 1); target.add(getSidebar().getMenu());
                Sw.toast(target, "A new workspace update is waiting in your inbox.");
            }
        }.add(AttributeModifier.replace("aria-label", "Add workspace update"));
    }
    @Override public void renderHead(IHeaderResponse response) {
        super.renderHead(response);
        response.render(CssHeaderItem.forReference(new CssResourceReference(BasePage.class, "examples.css")));
    }
    @Override protected void onInitialize() {
        super.onInitialize();
        getSidebar().setMarkupId("layout-navigation");
        getProvider().setMarkupId("layout-provider"); getProvider().setOutputMarkupId(true);
        getTopNav().get("trigger").setMarkupId("layout-trigger");
        add(Sw.Behaviors.toasts());
    }
}
