package dev.jbaby.shadwick.components;

import dev.jbaby.shadwick.behaviors.ButtonBehavior;
import dev.jbaby.shadwick.behaviors.SidebarBehavior;
import dev.jbaby.shadwick.behaviors.TooltipBehavior;
import org.apache.wicket.AttributeModifier;
import org.apache.wicket.Component;
import org.apache.wicket.markup.ComponentTag;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.link.AbstractLink;
import org.apache.wicket.markup.html.link.BookmarkablePageLink;
import org.apache.wicket.markup.html.list.ListItem;
import org.apache.wicket.markup.html.list.ListView;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.model.StringResourceModel;

import java.util.List;
import java.util.Objects;

/**
 * Navigation links and collapsible groups, using OatAppLayout's MenuItem and link hooks.
 * Bind to a nav directly inside a sidebar provider; pair it with a sidebar inset and trigger.
 * Desktop state is browser-owned. Replacing the whole sidebar closes its mobile panel;
 * {@link #getMenu()} can be updated in place for badges. Navigation uses ordinary links.
 */
public class SwSidebar extends Panel {

    public static final String HEADER_ID = "header";
    public static final String FOOTER_ID = "footer";

    private final SidebarBehavior appearance = new SidebarBehavior();
    private final WebMarkupContainer menu = new WebMarkupContainer("menu");
    private IModel<String> label;

    public SwSidebar(String id) {
        this(id, Model.ofList(List.of()));
    }

    public SwSidebar(String id, IModel<List<MenuItem>> items) {
        super(id);
        add(appearance);
        add(new WebMarkupContainer(HEADER_ID).setVisible(false), new WebMarkupContainer(FOOTER_ID).setVisible(false));
        menu.setOutputMarkupId(true);
        add(menu);
        menu.add(new ListView<>("items", OncePerRequestModel.of(Objects.requireNonNull(items))) {
            @Override
            protected void populateItem(ListItem<MenuItem> item) {
                MenuItem entry = item.getModelObject();
                if (entry.isGroup()) {
                    item.add(new WebMarkupContainer("link").setVisible(false), new WebMarkupContainer("menuBadge").setVisible(false));
                    item.add(newMenuGroup("group", entry));
                    item.setVisible(entry.items().stream().anyMatch(SwSidebar.this::isAuthorized));
                } else {
                    populateMenuLink(item);
                    item.add(new WebMarkupContainer("group").setVisible(false));
                }
            }
        });
        WebMarkupContainer close = new WebMarkupContainer("close");
        close.add(AttributeModifier.replace("popovertarget", (IModel<String>) this::getMarkupId));
        close.add(AttributeModifier.replace("aria-label", new StringResourceModel("SwSidebar.close", this)
                .setDefaultValue("Close sidebar")));
        close.add(new ButtonBehavior(ButtonBehavior.Variant.GHOST).setSize(ButtonBehavior.Size.ICON));
        add(close);
    }

    private WebMarkupContainer newMenuGroup(String id, MenuItem group) {
        WebMarkupContainer container = new WebMarkupContainer(id);
        WebMarkupContainer details = new WebMarkupContainer("details");
        container.add(details);
        Label label = new Label("label", group.label());
        label.setOutputMarkupId(true);
        container.add(AttributeModifier.replace("aria-labelledby", (IModel<String>) label::getMarkupId));
        details.add(label, new SwIcon("groupIcon", group.icon() != null ? group.icon() : Model.of("folder")));
        details.add(AttributeModifier.replace("open", (IModel<String>) () ->
                group.items().stream().anyMatch(this::isCurrentPage) ? "open" : null));
        details.add(new ListView<>("groupItems", Model.ofList(group.items())) {
            @Override
            protected void populateItem(ListItem<MenuItem> item) {
                populateMenuLink(item);
            }
        });
        return container;
    }

    private void populateMenuLink(ListItem<MenuItem> item) {
        MenuItem entry = item.getModelObject();
        IModel<String> text = OncePerRequestModel.of(entry.label());
        AbstractLink link = newMenuLink("link", entry);
        link.add(new Label("label", text), newMenuIcon("icon", entry));
        link.add(AttributeModifier.replace("aria-current", (IModel<String>) () -> isCurrentPage(entry) ? "page" : null));
        link.add(AttributeModifier.replace("data-sw-sidebar-hint", ""));
        link.add(new TooltipBehavior(text).setMode(TooltipBehavior.Mode.LABEL).setPlacement(TooltipBehavior.Placement.RIGHT));
        Label badge = new Label("menuBadge", entry.badge() != null ? entry.badge() : new Model<>()) {
            @Override
            protected void onConfigure() {
                super.onConfigure();
                Object value = getDefaultModelObject();
                setVisible(value != null && !value.toString().isBlank()
                        && (!(value instanceof Number number) || number.doubleValue() != 0));
            }
        };
        badge.setOutputMarkupId(true);
        link.add(AttributeModifier.replace("aria-describedby", (IModel<String>) () -> {
            badge.configure();
            return badge.isVisible() ? badge.getMarkupId() : null;
        }));
        item.add(link, badge);
        item.setVisible(isAuthorized(entry));
    }

    /** Override for custom navigation, including Ajax links; supply the link with this id. */
    protected AbstractLink newMenuLink(String id, MenuItem item) {
        return new BookmarkablePageLink<Void>(id, item.pageClass(), item.parameters());
    }

    /** Override for application-owned icon markup. */
    protected Component newMenuIcon(String id, MenuItem item) {
        return new SwIcon(id, item.icon() != null ? item.icon() : Model.of("circle"));
    }

    /** Exact page class and, when supplied, page parameters; override for custom route matching. */
    protected boolean isCurrentPage(MenuItem item) {
        return CurrentPage.isLinkedBy(getPage(), item.pageClass(), item.parameters());
    }

    protected boolean isAuthorized(MenuItem item) {
        return getApplication().getSecuritySettings().getAuthorizationStrategy().isInstantiationAuthorized(item.pageClass());
    }

    public SwSidebar setSide(SidebarBehavior.Side side) {
        appearance.setSide(side);
        return this;
    }

    public SwSidebar setVariant(SidebarBehavior.Variant variant) {
        appearance.setVariant(variant);
        return this;
    }

    public SwSidebar setCollapsible(SidebarBehavior.Collapsible collapsible) {
        appearance.setCollapsible(collapsible);
        return this;
    }

    public SwSidebar setLabel(String label) {
        return setLabel(Model.of(label));
    }

    public SwSidebar setLabel(IModel<String> label) {
        this.label = label;
        return this;
    }

    /** Ajax target for updating links/badges while keeping an open mobile panel. */
    public WebMarkupContainer getMenu() {
        return menu;
    }

    public SwSidebar setHeader(Component header) {
        return slot(header, HEADER_ID);
    }

    public SwSidebar setFooter(Component footer) {
        return slot(footer, FOOTER_ID);
    }

    private SwSidebar slot(Component component, String id) {
        if (!id.equals(component.getId())) {
            throw new IllegalArgumentException("The sidebar slot must use id \"" + id + "\"");
        }
        replace(component);
        return this;
    }

    @Override
    protected void onComponentTag(ComponentTag tag) {
        checkComponentTag(tag, "nav");
        super.onComponentTag(tag);
        if (label != null) {
            tag.put("aria-label", label.getObject());
        } else if (tag.getAttribute("aria-label") == null && tag.getAttribute("aria-labelledby") == null) {
            tag.put("aria-label", getString("SwSidebar.label"));
        }
    }

    @Override
    protected void onDetach() {
        if (label != null) label.detach();
        super.onDetach();
    }
}
