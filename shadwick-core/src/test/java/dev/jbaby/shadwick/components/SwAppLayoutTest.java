package dev.jbaby.shadwick.components;

import dev.jbaby.shadwick.Shadwick;
import dev.jbaby.shadwick.behaviors.SidebarBehavior;
import dev.jbaby.shadwick.behaviors.SidebarActionBehavior;
import org.apache.wicket.AttributeModifier;
import org.apache.wicket.Component;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.markup.html.AjaxLink;
import org.apache.wicket.authorization.strategies.page.SimplePageAuthorizationStrategy;
import org.apache.wicket.markup.html.WebPage;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.Markup;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.mock.MockApplication;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.request.mapper.parameter.PageParameters;
import org.apache.wicket.util.tester.TagTester;
import org.apache.wicket.util.tester.WicketTester;
import org.junit.jupiter.api.*;

import java.util.List;
import java.util.Locale;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.*;

class SwAppLayoutTest {
    WicketTester tester;
    @BeforeEach void before() {
        tester = new WicketTester(new MockApplication() {
            @Override protected void init() { super.init(); Shadwick.install(this); getCspSettings().blocking().strict(); }
        });
        tester.getApplication().getSecuritySettings().setAuthorizationStrategy(new SimplePageAuthorizationStrategy(Secret.class, WebPage.class) {
            @Override protected boolean isAuthorized() { return false; }
        });
    }
    @AfterEach void after() {
        if (tester.getLastResponse() != null) assertThat(html()).doesNotContain("style=").doesNotContainPattern("\\son[a-z]+=");
        tester.destroy();
    }
    String html() { return tester.getLastResponseAsString(); }
    List<TagTester> tags(String attribute, String value) { return TagTester.createTagsByAttribute(html(), attribute, value, false); }
    public static class Secret extends WebPage {}

    public static class LayoutPage extends SwAppLayout {
        final Model<Integer> count = Model.of(3);
        int actions;
        public LayoutPage() { this(new PageParameters()); }
        public LayoutPage(PageParameters parameters) { super(parameters); add(new Label("heading", "Page content")); }
        public LayoutPage(IModel<?> model) { super(model); add(new Label("heading", "Page content")); }
        @Override protected IModel<List<MenuItem>> sidebarMenuItemsModel() {
            return Model.ofList(List.of(MenuItem.of("Inbox", getClass(), new PageParameters().set("view", "inbox")).withIcon("mail").withBadge(count),
                    MenuItem.group("Workspace", MenuItem.submenu("Projects",
                            MenuItem.of("Recent", getClass(), new PageParameters().set("view", "recent")),
                            MenuItem.of("Secret child", Secret.class)).withIcon("folder"),
                            MenuItem.of("Settings", getClass(), new PageParameters().set("view", "settings")).withIcon("settings")
                                    .withChildren(MenuItem.of("Billing", getClass(), new PageParameters().set("view", "billing")))),
                    MenuItem.submenu("Secret submenu", MenuItem.of("Secret", Secret.class)),
                    MenuItem.of("Phone help", WebPage.class).withPanelOnly(true)));
        }
        @Override protected Component newMenuAction(String id, MenuItem item) {
            if (!"Inbox".equals(item.label().getObject())) return super.newMenuAction(id, item);
            return new AjaxLink<Void>(id) {
                @Override public void onClick(AjaxRequestTarget target) { actions++; count.setObject(0); target.add(getSidebar().getMenu()); }
            }.add(AttributeModifier.replace("aria-label", "Mark Inbox as read"));
        }
        @Override protected Component newGroupAction(String id, MenuItem group) {
            return new AjaxLink<Void>(id) {
                @Override public void onClick(AjaxRequestTarget target) { actions++; count.setObject(5); target.add(getSidebar().getMenu()); }
            }.add(AttributeModifier.replace("aria-label", "Add workspace item"));
        }
    }
    public static class HeaderPage extends LayoutPage {
        public HeaderPage(PageParameters parameters) { super(parameters); }
        @Override protected SidebarBehavior.Placement sidebarPlacement() { return SidebarBehavior.Placement.HEADER; }
    }

    @Test void inheritedContentAndShellLandmarksRenderWithLocaleAndEarlyTheme() {
        tester.getSession().setLocale(Locale.GERMANY);
        LayoutPage page = tester.startPage(LayoutPage.class);
        tester.assertLabel("heading", "Page content"); tester.assertLabel("appTitle", "Shadwick-Anwendung");
        assertThat(TagTester.createTagByName(html(), "html").getAttribute("lang")).isEqualTo("de-DE");
        assertThat(tags("wicket:id", "main")).singleElement().satisfies(tag -> {
            assertThat(tag.getName()).isEqualTo("main"); assertThat(tag.getAttribute("id")).isEqualTo("sw-main");
            assertThat(tag.getAttribute("tabindex")).isEqualTo("-1");
        });
        assertThat(tester.getTagByWicketId("skip").getAttribute("href")).isEqualTo("#sw-main");
        assertThat(html().indexOf("js/theme")).isLessThan(html().indexOf("css/shadwick"));
        assertThat(page.getProvider()).isNotNull(); assertThat(page.getTopNav()).isNotNull();
        assertThat(tags("popover", "auto")).hasSize(2); // one sidebar and the appearance menu
    }

    @Test void parametersSelectAuthorizedChildrenAndOpenBothGroupAndSubmenu() {
        LayoutPage page = tester.startPage(LayoutPage.class, new PageParameters().set("view", "recent"));
        assertThat(page.getPageParameters().get("view").toString()).isEqualTo("recent");
        assertThat(tags("aria-current", "page")).singleElement().satisfies(tag -> assertThat(tag.getValue()).contains("Recent"));
        assertThat(tags("wicket:id", "submenu").get(0).getAttribute("open")).isNotNull();
        assertThat(tester.getTagByWicketId("details").getAttribute("open")).isNotNull();
        assertThat(html()).doesNotContain("Secret child", "Secret submenu", ">Secret<");
        assertThat(tester.getTagByWicketId("sidebar").getValue()).doesNotContain("role=\"menuitem");
    }

    @Test void aSectionPageKeepsItsOwnLinkAndHasANamedSiblingDisclosure() {
        tester.startPage(LayoutPage.class, new PageParameters().set("view", "billing"));
        assertThat(tags("aria-current", "page")).singleElement().satisfies(tag -> assertThat(tag.getValue()).contains("Billing"));
        var summaries = tags("wicket:id", "summary");
        assertThat(summaries.get(0).getAttribute("aria-label")).isNull();
        assertThat(summaries.get(1).getAttribute("aria-label")).isEqualTo("Settings pages");
        assertThat(html()).contains("view=settings", "sw-sidebar-menu-sub-collapsible");
        assertThat(tags("data-panel-only", "")).hasSize(1);
    }

    @Test void itemAndGroupActionsRefreshBadgesWithoutReplacingTheNavigation() {
        LayoutPage page = tester.startPage(LayoutPage.class);
        String prefix = page.getSidebar().getPageRelativePath();
        tester.clickLink(prefix + ":menu:items:0:action");
        assertThat(page.actions).isEqualTo(1); tester.assertComponentOnAjaxResponse(page.getSidebar().getMenu());
        assertThat(html()).doesNotContain("sw-sidebar-menu-badge");
        tester.clickLink(prefix + ":menu:items:1:group:groupAction");
        assertThat(page.actions).isEqualTo(2); assertThat(html()).contains(">5</span>");
    }

    @Test void headerPlacementUsesOneNavigationInsideTheHeaderAndAMainBelowIt() {
        HeaderPage page = tester.startPage(HeaderPage.class, new PageParameters());
        assertThat(page.getSidebar().getParent()).isSameAs(page.getTopNav());
        assertThat(tester.getTagByWicketId("headerNavigation").getAttribute("data-placement")).isEqualTo("header");
        assertThat(html()).contains("sw-sidebar-inset").doesNotContain("wicket:id=\"sidebar\"");
        assertThat(tags("data-panel-only", "")).hasSize(2); // brand plus the phone-only help link
        assertThat(tags("wicket:id", "main")).hasSize(1);
    }

    @Test void modelConstructorPreservesThePageModelWithoutBindingShellLabelsToIt() {
        LayoutPage page = tester.startPage(new LayoutPage(Model.of("Page model")));
        assertThat(page.getDefaultModelObject()).isEqualTo("Page model");
        tester.assertLabel("appTitle", "Shadwick Application");
    }

    @Test void menuItemExtensionsPreserveExistingFieldsAndRejectUnsupportedTreesWithoutReadingModels() {
        MenuItem leaf = MenuItem.of("Leaf", LayoutPage.class).withBadge("2").withIcon("mail");
        MenuItem parent = MenuItem.of("Parent", LayoutPage.class).withChildren(leaf).withIcon("folder").withPanelOnly(true).withBadge("New");
        assertThat(parent.children()).containsExactly(leaf); assertThat(parent.panelOnly()).isTrue();
        assertThat(parent.badge().getObject()).isEqualTo("New");
        assertThat(new MenuItem(Model.of("Old"), LayoutPage.class, null, null, List.of(), null).children()).isEmpty();
        assertThatIllegalArgumentException().isThrownBy(() -> MenuItem.submenu("Empty"));
        assertThatIllegalArgumentException().isThrownBy(() -> MenuItem.submenu("Too deep", parent));
        assertThatIllegalArgumentException().isThrownBy(() -> MenuItem.submenu("Group", MenuItem.group("G", leaf)));
        IModel<String> unloaded = () -> { throw new AssertionError("Validation must not load a label"); };
        assertThatIllegalArgumentException().isThrownBy(() -> MenuItem.group("Nested", MenuItem.group(unloaded, leaf)));
        assertThatIllegalStateException().isThrownBy(() -> MenuItem.group("G", leaf).withChildren(leaf));
        assertThatIllegalStateException().isThrownBy(() -> MenuItem.submenu("S", leaf).withBadge("1"));
    }

    @Test void deniedSubmenusHideButAnAuthorizedParentPageRemainsWithoutAnEmptyDisclosure() {
        SwSidebar sidebar = new SwSidebar("sidebar", Model.ofList(List.of(
                MenuItem.of("Allowed parent", WebPage.class).withChildren(MenuItem.of("Hidden child", Secret.class)),
                MenuItem.of("Denied parent", Secret.class).withChildren(MenuItem.of("Public child", WebPage.class)),
                MenuItem.group("Empty section", MenuItem.submenu("Empty submenu", MenuItem.of("Private", Secret.class))))));
        tester.startComponentInPage(sidebar, Markup.of("<nav wicket:id='sidebar'></nav>"));
        assertThat(html()).contains("Allowed parent").doesNotContain("Denied parent", "Public child", "Hidden child", "Empty section", "Empty submenu");
        assertThat(tags("wicket:id", "submenu")).isEmpty();
    }

    @Test void callerOwnedActionsHaveEscapedNamesNativeDisabledStateAndDetachedModels() {
        AtomicInteger detaches = new AtomicInteger();
        IModel<String> name = new IModel<>() {
            @Override public String getObject() { return "More for <Inbox>"; }
            @Override public void detach() { detaches.incrementAndGet(); }
        };
        WebMarkupContainer action = new WebMarkupContainer("action"); action.setEnabled(false);
        action.add(new SidebarActionBehavior(SidebarActionBehavior.Kind.MENU).setLabel(name).setShowOnHover(true));
        tester.startComponentInPage(action, Markup.of("<button wicket:id='action'><svg aria-hidden='true'></svg></button>"));
        var tag = tester.getTagByWicketId("action");
        assertThat(tag.getAttribute("aria-label")).isEqualTo("More for <Inbox>");
        assertThat(tag.getAttribute("disabled")).isNotNull();
        assertThat(html()).contains("More for &lt;Inbox&gt;").doesNotContain("sw-button");
        assertThat(detaches.get()).isPositive();
    }
}
