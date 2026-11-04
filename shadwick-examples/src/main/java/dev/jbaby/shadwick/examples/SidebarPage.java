package dev.jbaby.shadwick.examples;

import dev.jbaby.shadwick.Sw;
import dev.jbaby.shadwick.behaviors.ButtonBehavior;
import dev.jbaby.shadwick.behaviors.SidebarBehavior;
import dev.jbaby.shadwick.components.*;
import org.apache.wicket.AttributeModifier;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.image.Image;
import org.apache.wicket.markup.html.link.BookmarkablePageLink;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.model.Model;
import org.apache.wicket.request.mapper.parameter.PageParameters;
import org.apache.wicket.request.resource.PackageResourceReference;
import java.util.List;
import java.util.Locale;

/** Real routes, native disclosure groups, reactive badges and responsive navigation. */
public class SidebarPage extends BasePage {
    public SidebarPage(PageParameters parameters) {
        super(parameters);
        String view = parameters.get("view").toString("Overview");
        Model<Integer> unread = Model.of(3);
        WebMarkupContainer provider = new WebMarkupContainer("provider"); provider.add(Sw.Behaviors.sidebarProvider()); add(provider);
        List<MenuItem> items = List.of(
                entry("Overview", "house", parameters), entry("Inbox", "mail", parameters).withBadge(unread),
                MenuItem.group("Workspace", entry("Projects", "folder", parameters), entry("Activity", "chart-line", parameters),
                        entry("Team", "users", parameters)),
                MenuItem.group("Manage", entry("Settings", "settings", parameters), entry("Help & resources", "circle-help", parameters)));
        SwSidebar sidebar = new SwSidebar("sidebar", Model.ofList(items)) {
            @Override protected boolean isCurrentPage(MenuItem item) { return view.equals(item.parameters().get("view").toString()); }
        };
        sidebar.setSide(option(parameters, "side", SidebarBehavior.Side.START))
                .setVariant(option(parameters, "variant", SidebarBehavior.Variant.INSET))
                .setCollapsible(option(parameters, "collapse", SidebarBehavior.Collapsible.ICON)).setLabel("Workspace sidebar");
        provider.add(sidebar);
        sidebar.setHeader(new Brand(SwSidebar.HEADER_ID));
        Label status = new Label("status", "Three unread messages. Your workspace is ready."); status.setOutputMarkupId(true);
        sidebar.setFooter(new Footer(SwSidebar.FOOTER_ID, sidebar, unread, status));
        WebMarkupContainer inset = new WebMarkupContainer("inset"); inset.setMarkupId("sidebar-content"); inset.add(Sw.Behaviors.sidebarInset()); provider.add(inset);
        addOrReplace(new SwSkipLink("skip", inset));
        SwSiteHeader header = new SwSiteHeader("header").setInset(true); inset.add(header);
        header.add(new SwSidebarTrigger("trigger", sidebar));
        header.add(SwBreadcrumb.of("trail", Model.ofList(List.of("Workspace")), (item, value) ->
                item.add(new BookmarkablePageLink<Void>("link", SidebarPage.class).add(new Label("label", item.getModel()))))
                .setCurrent(view).setLabel("Workspace path"));
        inset.add(new Label("heading", view), status);
        inset.add(SwButton.onClick("refresh", "Refresh sidebar", target -> {
            unread.setObject(3); status.setDefaultModelObject("Sidebar replaced. Three unread messages."); target.add(sidebar, status);
        }).setVariant(ButtonBehavior.Variant.OUTLINE));
        for (String name : List.of("sidebar", "floating", "inset")) inset.add(choice("variant-" + name, parameters, "variant", name));
        for (String name : List.of("offcanvas", "icon", "none")) inset.add(choice("collapse-" + name, parameters, "collapse", name));
        for (String name : List.of("start", "end")) inset.add(choice("side-" + name, parameters, "side", name));
    }
    private static MenuItem entry(String text, String icon, PageParameters parameters) {
        return MenuItem.of(text, SidebarPage.class, new PageParameters(parameters).set("view", text)).withIcon(icon);
    }
    private BookmarkablePageLink<Void> choice(String id, PageParameters parameters, String key, String value) {
        return (BookmarkablePageLink<Void>) new BookmarkablePageLink<Void>(id, SidebarPage.class, new PageParameters(parameters).set(key, value))
                .add(Sw.Behaviors.button(ButtonBehavior.Variant.OUTLINE).setSize(ButtonBehavior.Size.SMALL));
    }
    private static <E extends Enum<E>> E option(PageParameters parameters, String key, E fallback) {
        try { return Enum.valueOf(fallback.getDeclaringClass(), parameters.get(key).toString("").toUpperCase(Locale.ROOT)); }
        catch (IllegalArgumentException ignored) { return fallback; }
    }
    public static class Brand extends Panel {
        public Brand(String id) {
            super(id);
            var home = new BookmarkablePageLink<Void>("home", HomePage.class);
            home.add(new Image("logo", new PackageResourceReference(SidebarPage.class, "clover.svg"))); add(home);
        }
    }
    public static class Footer extends Panel {
        public Footer(String id, SwSidebar sidebar, Model<Integer> unread, Label status) {
            super(id);
            var read = new org.apache.wicket.ajax.markup.html.AjaxLink<Void>("read") {
                @Override public void onClick(org.apache.wicket.ajax.AjaxRequestTarget target) {
                unread.setObject(0); status.setDefaultModelObject("All messages read. Badge updated in place."); target.add(sidebar.getMenu(), status);
                }
            };
            read.add(new SwIcon("icon", "check")); add(read);
        }
    }
    @Override protected String getTitle() { return "Sidebar"; }
}
