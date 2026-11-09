package dev.jbaby.shadwick.examples;

import dev.jbaby.shadwick.Sw;
import dev.jbaby.shadwick.behaviors.ButtonBehavior;
import dev.jbaby.shadwick.behaviors.SidebarBehavior;
import dev.jbaby.shadwick.components.*;

import org.apache.wicket.MetaDataKey;
import org.apache.wicket.Session;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.model.Model;
import org.apache.wicket.request.mapper.parameter.PageParameters;
import org.apache.wicket.request.resource.PackageResourceReference;

import java.util.List;

/**
 * Demo identity lives in this visitor's session; the library has no authentication-framework
 * dependency.
 */
public class UserMenuPage extends BasePage {

    private static final MetaDataKey<Boolean> SIGNED_OUT = new MetaDataKey<>() {};

    private static final MetaDataKey<String> NAME = new MetaDataKey<>() {};

    private static final MetaDataKey<String> PICTURE = new MetaDataKey<>() {};

    static String name() {
        return Boolean.TRUE.equals(Session.get().getMetaData(SIGNED_OUT))
                ? null
                : Session.get().getMetaData(NAME) == null
                        ? "Alex Morgan"
                        : Session.get().getMetaData(NAME);
    }

    static void signIn() {
        Session.get().bind();
        Session.get().setMetaData(SIGNED_OUT, false);
        Session.get().setMetaData(NAME, "Alex Morgan");
    }

    public UserMenuPage(PageParameters parameters) {
        super(parameters);
    }

    @Override
    protected void onInitialize() {
        super.onInitialize();
        PageParameters parameters = getPageParameters();
        WebMarkupContainer provider = new WebMarkupContainer("provider");
        provider.add(Sw.Behaviors.sidebarProvider());
        add(provider);
        SwSidebar sidebar = addSidebar(provider);
        SwUserMenu footer = account(SwSidebar.FOOTER_ID, parameters);
        sidebar.setFooter(footer);
        footer.setMarkupId("sidebar-user");
        WebMarkupContainer inset = new WebMarkupContainer("inset");
        inset.add(Sw.Behaviors.sidebarInset());
        inset.setMarkupId("account-content");
        provider.add(inset);
        addOrReplace(new SwSkipLink("skip", inset));
        SwUserMenu headerUser = addAccountHeader(inset, sidebar, parameters);
        Label status =
                new Label(
                        "status",
                        parameters.get("signedOut").toBoolean(false)
                                ? "Signed out of the demo workspace."
                                : "Viewing " + parameters.get("view").toString("overview") + ".");
        status.setOutputMarkupId(true);
        inset.add(status);
        addAccountActions(inset, footer, headerUser, status);
    }

    private SwSidebar addSidebar(WebMarkupContainer provider) {
        SwSidebar sidebar =
                new SwSidebar(
                                "sidebar",
                                Model.ofList(
                                        List.of(
                                                MenuItem.of("Overview", UserMenuPage.class)
                                                        .withIcon("house"),
                                                MenuItem.group(
                                                        "Workspace",
                                                        MenuItem.of("Projects", SidebarPage.class)
                                                                .withIcon("folder"),
                                                        MenuItem.of("Preferences", GroupPage.class)
                                                                .withIcon("settings")))))
                        .setCollapsible(SidebarBehavior.Collapsible.ICON)
                        .setVariant(SidebarBehavior.Variant.INSET)
                        .setLabel("Account workspace");
        sidebar.setHeader(new SidebarPage.Brand(SwSidebar.HEADER_ID));
        provider.add(sidebar);
        return sidebar;
    }

    private SwUserMenu account(String id, PageParameters parameters) {
        SwUserMenu user =
                new SwUserMenu(id, UserMenuPage::name) {

                    @Override
                    protected void signOut() {
                        Session.get().setMetaData(SIGNED_OUT, true);
                    }

                    @Override
                    protected void onSignedOut() {
                        setResponsePage(
                                UserMenuPage.class,
                                new PageParameters(parameters).set("signedOut", true));
                    }
                }.setDetail("alex@example.com")
                        .setPicture(() -> Session.get().getMetaData(PICTURE))
                        .setSignInPage(UserSignInPage.class);
        user.addLink(
                        "My account",
                        UserMenuPage.class,
                        new PageParameters(parameters).set("view", "account"))
                .setIcon("user");
        user.addLink(
                        "Settings",
                        UserMenuPage.class,
                        new PageParameters(parameters).set("view", "settings"))
                .setIcon("settings");
        return user;
    }

    private SwUserMenu addAccountHeader(
            WebMarkupContainer inset, SwSidebar sidebar, PageParameters parameters) {
        SwSiteHeader header = new SwSiteHeader("header").setInset(true);
        inset.add(header);
        header.add(new SwSidebarTrigger("trigger", sidebar));
        header.add(new Label("workspace", "Team workspace"));
        WebMarkupContainer end = new WebMarkupContainer("end");
        end.add(Sw.Behaviors.siteHeaderEnd());
        header.add(end);
        SwUserMenu headerUser =
                account("user", parameters).setPlacement(SwUserMenu.Placement.HEADER);
        headerUser.setMarkupId("header-user");
        end.add(headerUser);
        return headerUser;
    }

    private void addAccountActions(
            WebMarkupContainer inset, SwUserMenu footer, SwUserMenu headerUser, Label status) {
        inset.add(
                SwButton.onClick(
                                "rename",
                                "Change account name",
                                target -> {
                                    Session.get()
                                            .setMetaData(
                                                    NAME,
                                                    "Alexandra Morgan from the Design Systems and"
                                                        + " Platform Engineering team");
                                    status.setDefaultModelObject(
                                            "Account refreshed in both menus.");
                                    target.add(footer, headerUser, status);
                                })
                        .setVariant(ButtonBehavior.Variant.OUTLINE));
        inset.add(
                SwButton.onClick(
                                "picture",
                                "Use account picture",
                                target -> {
                                    Session.get()
                                            .setMetaData(
                                                    PICTURE,
                                                    urlFor(
                                                                    new PackageResourceReference(
                                                                            UserMenuPage.class,
                                                                            "clover.svg"),
                                                                    null)
                                                            .toString());
                                    status.setDefaultModelObject("Account picture loaded.");
                                    target.add(footer, headerUser, status);
                                })
                        .setVariant(ButtonBehavior.Variant.OUTLINE));
        inset.add(
                SwButton.onClick(
                                "initials",
                                "Use initials",
                                target -> {
                                    Session.get().setMetaData(PICTURE, null);
                                    status.setDefaultModelObject("Initials restored.");
                                    target.add(footer, headerUser, status);
                                })
                        .setVariant(ButtonBehavior.Variant.OUTLINE));
        inset.add(
                SwButton.onClick(
                                "anonymous",
                                "Show anonymous visitor",
                                target -> {
                                    Session.get().setMetaData(SIGNED_OUT, true);
                                    status.setDefaultModelObject(
                                            "Anonymous visitor: both menus offer Sign in.");
                                    target.add(footer, headerUser, status);
                                })
                        .setVariant(ButtonBehavior.Variant.OUTLINE));
        inset.add(
                SwButton.onClick(
                                "restore",
                                "Restore demo account",
                                target -> {
                                    signIn();
                                    status.setDefaultModelObject("Alex Morgan restored.");
                                    target.add(footer, headerUser, status);
                                })
                        .setVariant(ButtonBehavior.Variant.OUTLINE));
    }

    @Override
    protected String getTitle() {
        return "User menu";
    }
}
