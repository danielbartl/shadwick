package dev.jbaby.shadwick.components;

import dev.jbaby.shadwick.behaviors.DropdownMenuBehavior;
import dev.jbaby.shadwick.behaviors.DropdownMenuItemBehavior;
import dev.jbaby.shadwick.behaviors.TooltipBehavior;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.apache.wicket.AttributeModifier;
import org.apache.wicket.Component;
import org.apache.wicket.Page;
import org.apache.wicket.behavior.Behavior;
import org.apache.wicket.markup.ComponentTag;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.form.IFormSubmitter;
import org.apache.wicket.markup.html.form.StatelessForm;
import org.apache.wicket.markup.html.link.BookmarkablePageLink;
import org.apache.wicket.markup.html.list.ListItem;
import org.apache.wicket.markup.html.list.ListView;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.model.StringResourceModel;
import org.apache.wicket.protocol.http.FetchMetadataResourceIsolationPolicy;
import org.apache.wicket.protocol.http.IResourceIsolationPolicy;
import org.apache.wicket.request.http.flow.AbortWithHttpErrorCodeException;
import org.apache.wicket.request.mapper.parameter.PageParameters;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Model-backed account menu, following OatUserMenu's API. A null name means signed out;
 * override isSignedIn for an authenticated user without a name. Sidebar placement is the
 * default; HEADER is an avatar-only button. Models are read once per request.
 * Sign out owns a separate, stateless POST form: place the component outside other forms.
 * Override signOut/onSignedOut to integrate the application's authentication.
 */
public class SwUserMenu extends Panel {
    public enum Placement { SIDEBAR, HEADER }

    private static final IResourceIsolationPolicy FROM_ANOTHER_SITE = new FetchMetadataResourceIsolationPolicy();
    private final IModel<String> name;
    private IModel<String> detail;
    private IModel<String> picture;
    private Placement placement = Placement.SIDEBAR;
    private DropdownMenuBehavior.Side side;
    private final List<MenuLink> links = new ArrayList<>();
    private final WebMarkupContainer trigger;
    private final WebMarkupContainer menu;
    private final DropdownMenuBehavior appearance;
    private final StatelessForm<Void> signOutForm;

    public SwUserMenu(String id, IModel<String> name) {
        super(id);
        this.name = OncePerRequestModel.of(Objects.requireNonNull(name));
        setOutputMarkupId(true);
        add(AttributeModifier.append("class", "sw-user-menu"));
        add(AttributeModifier.replace("data-placement", (IModel<String>) () -> placement == Placement.HEADER ? "header" : null));
        WebMarkupContainer dropdown = new WebMarkupContainer("dropdown") {
            @Override protected void onConfigure() { super.onConfigure(); setVisible(isSignedIn()); }
        };
        add(dropdown);
        trigger = new WebMarkupContainer("trigger");
        trigger.add(triggerStyle(false));
        trigger.add(AttributeModifier.replace("aria-label", (IModel<String>) () -> placement == Placement.HEADER
                ? new StringResourceModel("SwUserMenu.account", SwUserMenu.this).setParameters(displayName()).getObject() : null));
        trigger.add(AttributeModifier.replace("data-sw-sidebar-hint", (IModel<String>) () -> placement == Placement.SIDEBAR ? "" : null));
        trigger.add(new TooltipBehavior((IModel<String>) () -> placement == Placement.SIDEBAR ? displayName() : null)
                .setMode(TooltipBehavior.Mode.LABEL).setPlacement(TooltipBehavior.Placement.RIGHT));
        trigger.add(avatar("avatar"), new Label("name", (IModel<String>) this::displayName) {
            @Override protected void onConfigure() { super.onConfigure(); setVisible(placement == Placement.SIDEBAR); }
        }, detailLabel("triggerDetail"));
        dropdown.add(trigger);
        menu = new WebMarkupContainer("menu");
        appearance = new DropdownMenuBehavior(trigger).setAlign(DropdownMenuBehavior.Align.END);
        menu.add(appearance);
        menu.add(avatar("menuAvatar"), new Label("menuName", (IModel<String>) this::displayName), detailLabel("detail"));
        menu.add(new ListView<>("links", Model.ofList(links)) {
            @Override protected void populateItem(ListItem<MenuLink> item) {
                MenuLink link = item.getModelObject();
                BookmarkablePageLink<Void> anchor = new BookmarkablePageLink<>("link", link.page, link.parameters);
                anchor.add(new DropdownMenuItemBehavior());
                anchor.add(AttributeModifier.replace("aria-current", (IModel<String>) () ->
                        CurrentPage.isLinkedBy(getPage(), link.page, link.parameters) ? "page" : null));
                anchor.add(SwIcon.ofNullable("icon", link.icon), new Label("label", link.label));
                item.add(anchor);
                item.setVisible(authorized(link));
            }
        });
        menu.add(new WebMarkupContainer("linksEnd") {
            @Override protected void onConfigure() { super.onConfigure(); setVisible(links.stream().anyMatch(SwUserMenu.this::authorized)); }
        });
        dropdown.add(menu);
        signOutForm = new StatelessForm<>("signOut") {
            @Override public boolean canCallListener() { return true; }
            @Override public void process(IFormSubmitter submitter) {
                if (!isEnabledInHierarchy()) throw new AbortWithHttpErrorCodeException(HttpServletResponse.SC_FORBIDDEN, "Disabled account menu");
                if (getRequest().getContainerRequest() instanceof HttpServletRequest request) {
                    if (!"POST".equalsIgnoreCase(request.getMethod())) {
                        throw new AbortWithHttpErrorCodeException(HttpServletResponse.SC_METHOD_NOT_ALLOWED, "Sign out requires POST");
                    }
                    if (FROM_ANOTHER_SITE.isRequestAllowed(request, getPage(), IResourceIsolationPolicy.RequestType.LISTENER)
                            == IResourceIsolationPolicy.ResourceIsolationOutcome.DISALLOWED) {
                        throw new AbortWithHttpErrorCodeException(HttpServletResponse.SC_FORBIDDEN, "Sign out from another site");
                    }
                }
                SwUserMenu.this.signOut();
                onSignedOut();
            }
        };
        signOutForm.setOutputMarkupId(true);
        add(signOutForm);
        WebMarkupContainer signOut = new WebMarkupContainer("signOutButton");
        signOut.add(new DropdownMenuItemBehavior());
        signOut.add(AttributeModifier.replace("form", (IModel<String>) signOutForm::getMarkupId));
        signOut.add(new SwIcon("icon", "log-out"), new Label("label", new StringResourceModel("SwUserMenu.signOut", this)));
        menu.add(signOut);
        add(new WebMarkupContainer("signIn").setVisible(false));
    }

    private boolean authorized(MenuLink link) {
        return getApplication().getSecuritySettings().getAuthorizationStrategy().isInstantiationAuthorized(link.page);
    }

    private Behavior triggerStyle(boolean signIn) {
        return new Behavior() {
            @Override public void onComponentTag(Component component, ComponentTag tag) {
                if (placement == Placement.SIDEBAR) {
                    tag.append("class", "sw-sidebar-menu-button", " ");
                    if (!signIn) tag.put("data-size", "lg");
                } else {
                    tag.append("class", "sw-button", " ");
                    tag.put("data-variant", signIn ? "outline" : "ghost");
                    tag.put("data-size", signIn ? "sm" : "icon");
                }
            }
        };
    }

    private SwAvatar avatar(String id) {
        SwAvatar avatar = new SwAvatar(id, (IModel<String>) this::getPicture,
                (IModel<String>) () -> SwAvatar.initialsOf(initialsName()), SwAvatar.Size.DEFAULT).setLabel("");
        avatar.add(AttributeModifier.replace("aria-hidden", "true"));
        // The header fallback is a decorative glyph, not a second text label on the icon button.
        Component fallback = avatar.get("container:initials");
        fallback.setDefaultModel((IModel<String>) () -> placement == Placement.HEADER ? "" : SwAvatar.initialsOf(initialsName()));
        fallback.add(AttributeModifier.replace("data-initials", (IModel<String>) () ->
                placement == Placement.HEADER ? SwAvatar.initialsOf(initialsName()) : null));
        fallback.add(AttributeModifier.remove("title"));
        return avatar;
    }

    private String initialsName() {
        String value = name.getObject();
        if (value != null && !value.isBlank()) return value;
        String valueDetail = getDetail();
        return valueDetail == null ? null : valueDetail.split("@", 2)[0];
    }

    private Label detailLabel(String id) {
        return new Label(id, (IModel<String>) this::getDetail) {
            @Override protected void onConfigure() { super.onConfigure(); setVisible(getDetail() != null && (!"triggerDetail".equals(getId()) || placement == Placement.SIDEBAR)); }
        };
    }

    private String displayName() {
        String value = name.getObject();
        return value == null || value.isBlank() ? getString("SwUserMenu.unnamed") : value;
    }

    private String getDetail() {
        String value = detail != null ? detail.getObject() : null;
        return value == null || value.isBlank() ? null : value;
    }

    private String getPicture() { return picture != null ? picture.getObject() : null; }
    protected boolean isSignedIn() { return name.getObject() != null; }
    protected void signOut() { getSession().invalidate(); }
    protected void onSignedOut() { setResponsePage(getApplication().getHomePage()); }

    public SwUserMenu setDetail(String detail) { return setDetail(Model.of(detail)); }
    public SwUserMenu setDetail(IModel<String> detail) { this.detail = OncePerRequestModel.of(detail); return this; }
    public SwUserMenu setPicture(String url) { return setPicture(Model.of(url)); }
    public SwUserMenu setPicture(IModel<String> url) { picture = OncePerRequestModel.of(url); return this; }
    public SwUserMenu setPlacement(Placement placement) { this.placement = Objects.requireNonNull(placement); return this; }
    public SwUserMenu setSide(DropdownMenuBehavior.Side side) { this.side = Objects.requireNonNull(side); return this; }
    public SwUserMenu setAlign(DropdownMenuBehavior.Align align) { appearance.setAlign(align); return this; }
    public WebMarkupContainer getTrigger() { return trigger; }
    public WebMarkupContainer getMenu() { return menu; }
    public StatelessForm<Void> getSignOutForm() { return signOutForm; }

    public MenuLink addLink(String label, Class<? extends Page> page) { return addLink(Model.of(label), page, null); }
    public MenuLink addLink(String label, Class<? extends Page> page, PageParameters parameters) { return addLink(Model.of(label), page, parameters); }
    public MenuLink addLink(IModel<String> label, Class<? extends Page> page) { return addLink(label, page, null); }
    public MenuLink addLink(IModel<String> label, Class<? extends Page> page, PageParameters parameters) {
        MenuLink link = new MenuLink(Objects.requireNonNull(label), Objects.requireNonNull(page), parameters);
        links.add(link);
        return link;
    }

    public SwUserMenu setSignInPage(Class<? extends Page> page) {
        Objects.requireNonNull(page);
        BookmarkablePageLink<Void> signIn = new BookmarkablePageLink<>("signIn", page) {
            @Override protected void onConfigure() {
                super.onConfigure();
                setVisible(!isSignedIn() && !page.isInstance(getPage()));
            }
        };
        signIn.add(triggerStyle(true));
        signIn.add(AttributeModifier.replace("data-sw-sidebar-hint", (IModel<String>) () -> placement == Placement.SIDEBAR ? "" : null));
        signIn.add(new TooltipBehavior((IModel<String>) () -> placement == Placement.SIDEBAR ? getString("SwUserMenu.signIn") : null)
                .setMode(TooltipBehavior.Mode.LABEL).setPlacement(TooltipBehavior.Placement.RIGHT));
        signIn.add(new SwIcon("signInIcon", "log-in"), new Label("signInLabel", new StringResourceModel("SwUserMenu.signIn", this)));
        addOrReplace(signIn);
        return this;
    }

    @Override protected void onConfigure() {
        super.onConfigure();
        appearance.setSide(side != null ? side : placement == Placement.SIDEBAR ? DropdownMenuBehavior.Side.END : DropdownMenuBehavior.Side.BOTTOM);
    }

    @Override protected void onDetach() {
        super.onDetach();
        name.detach();
        if (detail != null) detail.detach();
        if (picture != null) picture.detach();
        // Also detach labels on entries hidden by authorization or the anonymous state.
        links.forEach(link -> link.label.detach());
    }

    /** A model-backed page link above Sign out; setIcon accepts a registered Lucide name. */
    public static final class MenuLink implements Serializable {
        private final IModel<String> label;
        private final Class<? extends Page> page;
        private final PageParameters parameters;
        private String icon;
        private MenuLink(IModel<String> label, Class<? extends Page> page, PageParameters parameters) {
            this.label = label; this.page = page; this.parameters = parameters;
        }
        public MenuLink setIcon(String icon) { this.icon = SwIcon.requireKnown(icon); return this; }
    }
}
