package dev.jbaby.shadwick.components;

import dev.jbaby.shadwick.Sw;
import dev.jbaby.shadwick.behaviors.SidebarBehavior;
import dev.jbaby.shadwick.behaviors.SidebarProviderBehavior;
import jakarta.servlet.http.Cookie;
import org.apache.wicket.MarkupContainer;
import org.apache.wicket.authorization.strategies.page.SimplePageAuthorizationStrategy;
import org.apache.wicket.markup.IMarkupResourceStreamProvider;
import org.apache.wicket.markup.Markup;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.WebPage;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.request.mapper.parameter.PageParameters;
import org.apache.wicket.util.resource.IResourceStream;
import org.apache.wicket.util.resource.StringResourceStream;
import org.apache.wicket.util.tester.TagTester;
import org.apache.wicket.util.tester.WicketTester;
import org.junit.jupiter.api.*;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import static org.assertj.core.api.Assertions.*;

class SwSidebarTest {
    WicketTester tester;
    @BeforeEach void before() { tester = new WicketTester(); }
    @AfterEach void after() {
        if (tester.getLastResponse() != null) assertThat(html()).doesNotContain("style=").doesNotContainPattern("\\son[a-z]+=");
        tester.destroy();
    }
    String html() { return tester.getLastResponseAsString(); }
    void render(SwSidebar sidebar) { tester.startComponentInPage(sidebar, Markup.of("<nav wicket:id='sidebar'></nav>")); }
    List<TagTester> tags(String attr, String value) { return TagTester.createTagsByAttribute(html(), attr, value, false); }
    @Test void groupedSidebarSurvivesPageSerializationAndCanRenderAgain() {
        tester.startPage(NavigationPage.class);
        var serializer = tester.getApplication().getFrameworkSettings().getSerializer();
        byte[] bytes = serializer.serialize(tester.getLastRenderedPage());
        assertThat(bytes).isNotNull();
        var restored = (WebPage) serializer.deserialize(bytes);
        assertThat(restored).isNotNull();
        tester.startPage(restored);
        assertThat(html()).contains("Workspace", "Current");
        assertThat(tester.getTagByWicketId("close").getAttribute("popovertarget")).isEqualTo(tester.getTagByWicketId("sidebar").getAttribute("id"));
        assertThat(tester.getTagByWicketId("group").getAttribute("aria-labelledby")).isEqualTo(tester.getTagByWicketId("label").getAttribute("id"));
    }
    @Test void nativeNavigationLinksHaveNamesIconsBadgesAndNoMenuRoles() {
        render(new SwSidebar("sidebar", Model.ofList(List.of(MenuItem.of("<Inbox>", WebPage.class).withBadge("3").withIcon("mail")))));
        TagTester nav = tester.getTagByWicketId("sidebar"), link = tester.getTagByWicketId("link"), badge = tester.getTagByWicketId("menuBadge");
        assertThat(nav.getName()).isEqualTo("nav");
        assertThat(nav.getAttribute("popover")).isEqualTo("auto");
        assertThat(nav.getAttribute("aria-label")).isEqualTo("Main navigation");
        assertThat(link.getAttribute("href")).isNotBlank();
        assertThat(link.getAttribute("data-tooltip-mode")).isEqualTo("label");
        assertThat(link.getAttribute("aria-describedby")).isEqualTo(badge.getAttribute("id"));
        assertThat(html()).contains("&lt;Inbox&gt;").doesNotContain("role=\"menu");
    }
    @Test void zeroNullAndBlankBadgesAreHidden() {
        render(new SwSidebar("sidebar", Model.ofList(List.of(MenuItem.of("A", WebPage.class).withBadge(Model.of(0)),
                MenuItem.of("B", WebPage.class).withBadge("  "), MenuItem.of("C", WebPage.class)))));
        assertThat(tags("wicket:id", "menuBadge")).isEmpty();
        assertThat(tags("wicket:id", "link")).allSatisfy(tag -> assertThat(tag.getAttribute("aria-describedby")).isNull());
    }
    @Test void groupsUseNativeDetailsAndBecomeVisibleWhenAnAuthorizedChildExists() {
        render(new SwSidebar("sidebar", Model.ofList(List.of(MenuItem.group("Team", MenuItem.of("Members", WebPage.class))))));
        assertThat(tester.getTagByWicketId("details").getName()).isEqualTo("details");
        assertThat(tester.getTagByWicketId("group").getAttribute("aria-labelledby")).isNotBlank();
        assertThat(html()).contains("Members", "Team");
    }
    @Test void authorizationFiltersLinksAndEmptyGroups() {
        tester.getApplication().getSecuritySettings().setAuthorizationStrategy(new SimplePageAuthorizationStrategy(PrivatePage.class, WebPage.class) {
            @Override protected boolean isAuthorized() { return false; }
        });
        render(new SwSidebar("sidebar", Model.ofList(List.of(MenuItem.of("Secret", PrivatePage.class),
                MenuItem.group("Hidden group", MenuItem.of("Private", PrivatePage.class)),
                MenuItem.group("Mixed", MenuItem.of("Public", WebPage.class), MenuItem.of("Hidden child", PrivatePage.class))))));
        assertThat(html()).contains("Mixed", "Public").doesNotContain("Secret", "Hidden group", "Hidden child", ">Private<");
    }
    public static class PrivatePage extends WebPage {}
    public static class NavigationPage extends WebPage implements IMarkupResourceStreamProvider {
        final Model<Integer> count = Model.of(4);
        final SwSidebar sidebar;
        public NavigationPage() { this(new PageParameters()); }
        public NavigationPage(PageParameters parameters) {
            super(parameters);
            sidebar = new SwSidebar("sidebar", Model.ofList(List.of(MenuItem.group("Workspace",
                    MenuItem.of("Current", NavigationPage.class, new PageParameters().set("tab", "inbox")).withBadge(count),
                    MenuItem.of("Other", NavigationPage.class, new PageParameters().set("tab", "other"))))));
            var provider = new WebMarkupContainer("provider"); provider.add(Sw.Behaviors.sidebarProvider()); add(provider); provider.add(sidebar);
            provider.add(new SwSidebarTrigger("trigger", sidebar));
            provider.add(SwButton.onClick("read", "Read", target -> { count.setObject(0); target.add(sidebar.getMenu()); }));
        }
        @Override public IResourceStream getMarkupResourceStream(MarkupContainer container, Class<?> type) {
            return new StringResourceStream("<html><body><div wicket:id='provider'><nav wicket:id='sidebar'></nav><button wicket:id='trigger'></button><button wicket:id='read'></button></div></body></html>");
        }
    }
    @Test void currentPageMatchesParametersAndOpensItsGroup() {
        tester.startPage(NavigationPage.class, new PageParameters().set("tab", "inbox"));
        assertThat(tags("aria-current", "page")).singleElement().satisfies(tag -> assertThat(tag.getValue()).contains("Current"));
        assertThat(tester.getTagByWicketId("details").getAttribute("open")).isNotNull();
        tester.startPage(NavigationPage.class, new PageParameters().set("tab", "absent"));
        assertThat(tags("aria-current", "page")).isEmpty();
        assertThat(tester.getTagByWicketId("details").getAttribute("open")).isNull();
    }
    @Test void badgesCanRefreshInPlaceOverAjax() {
        NavigationPage page = tester.startPage(NavigationPage.class);
        tester.clickLink("provider:read"); tester.assertComponentOnAjaxResponse(page.sidebar.getMenu());
        assertThat(html()).doesNotContain("sw-sidebar-menu-badge");
        assertThat(html()).contains("sw-sidebar-menu");
    }
    @Test void nativeTriggerAndCloseTargetTheSameStableId() {
        tester.startPage(NavigationPage.class);
        String id = tester.getTagByWicketId("sidebar").getAttribute("id");
        assertThat(tester.getTagByWicketId("trigger").getAttribute("popovertarget")).isEqualTo(id);
        assertThat(tester.getTagByWicketId("trigger").getAttribute("aria-controls")).isEqualTo(id);
        assertThat(tester.getTagByWicketId("close").getAttribute("popovertarget")).isEqualTo(id);
        assertThat(tester.getTagByWicketId("close").getAttribute("popovertargetaction")).isEqualTo("hide");
    }
    @Test void callerCanChooseAppearanceAndAccessibleName() {
        render(new SwSidebar("sidebar").setSide(SidebarBehavior.Side.END).setVariant(SidebarBehavior.Variant.FLOATING)
                .setCollapsible(SidebarBehavior.Collapsible.ICON).setLabel("Project navigation"));
        TagTester nav = tester.getTagByWicketId("sidebar");
        assertThat(nav.getAttribute("data-side")).isEqualTo("right");
        assertThat(nav.getAttribute("data-variant")).isEqualTo("floating");
        assertThat(nav.getAttribute("data-collapsible")).isEqualTo("icon");
        assertThat(nav.getAttribute("aria-label")).isEqualTo("Project navigation");
    }
    @Test void slotsRequireTheDocumentedIds() {
        SwSidebar sidebar = new SwSidebar("sidebar");
        assertThatIllegalArgumentException().isThrownBy(() -> sidebar.setHeader(new WebMarkupContainer("wrong")));
        sidebar.setHeader(new WebMarkupContainer(SwSidebar.HEADER_ID)); sidebar.setFooter(new WebMarkupContainer(SwSidebar.FOOTER_ID));
        render(sidebar); tester.assertVisible("sidebar:header"); tester.assertVisible("sidebar:footer");
    }
    @Test void itemsAreReadOnceAndDetached() {
        AtomicInteger reads = new AtomicInteger(), detaches = new AtomicInteger();
        IModel<List<MenuItem>> items = new IModel<>() {
            @Override public List<MenuItem> getObject() { reads.incrementAndGet(); return List.of(MenuItem.of("Home", WebPage.class)); }
            @Override public void detach() { detaches.incrementAndGet(); }
        };
        render(new SwSidebar("sidebar", items)); assertThat(reads.get()).isEqualTo(1); assertThat(detaches.get()).isPositive();
    }
    @Test void menuItemKeepsOatConstructorsAndRejectsInvalidGroups() {
        MenuItem entry = new MenuItem(Model.of("A"), WebPage.class, null, null, null).withIcon("house").withBadge("New");
        assertThat(entry.icon().getObject()).isEqualTo("house"); assertThat(entry.badge().getObject()).isEqualTo("New");
        assertThatIllegalArgumentException().isThrownBy(() -> MenuItem.group("Empty"));
        assertThatIllegalArgumentException().isThrownBy(() -> MenuItem.group("Nested", MenuItem.group("Child", entry)));
        assertThatIllegalStateException().isThrownBy(() -> MenuItem.group("G", entry).withBadge("1"));
        assertThatNullPointerException().isThrownBy(() -> MenuItem.of("Missing page", null));
    }
    private void provider(SidebarProviderBehavior behavior, String cookie) {
        if (cookie != null) tester.getRequest().addCookie(new Cookie("sw-sidebar-state", cookie));
        tester.startComponentInPage(new WebMarkupContainer("provider").add(behavior), Markup.of("<div wicket:id='provider'></div>"));
    }
    @Test void providerRestoresOnlyValidCookieValues() {
        provider(new SidebarProviderBehavior(), "collapsed");
        assertThat(tester.getTagByWicketId("provider").getAttribute("data-state")).isEqualTo("collapsed");
        provider(new SidebarProviderBehavior(), "invalid");
        assertThat(tester.getTagByWicketId("provider").getAttribute("data-state")).isEqualTo("expanded");
    }
    @Test void persistenceCanBeDisabledAndCookieNamesAreValidated() {
        provider(new SidebarProviderBehavior().setRememberState(false).setDefaultState(SidebarProviderBehavior.State.COLLAPSED), "expanded");
        assertThat(tester.getTagByWicketId("provider").getAttribute("data-state")).isEqualTo("collapsed");
        assertThat(tester.getTagByWicketId("provider").getAttribute("data-sidebar-remember")).isEqualTo("false");
        assertThatIllegalArgumentException().isThrownBy(() -> new SidebarProviderBehavior().setCookieName("a;path=/"));
    }
    @Test void customAjaxLinksUseTheSameAuthorizationAndNavigationAnatomy() {
        AtomicInteger clicks = new AtomicInteger();
        SwSidebar sidebar = new SwSidebar("sidebar", Model.ofList(List.of(MenuItem.of("Refresh", WebPage.class)))) {
            @Override protected org.apache.wicket.markup.html.link.AbstractLink newMenuLink(String id, MenuItem entry) {
                return new org.apache.wicket.ajax.markup.html.AjaxLink<Void>(id) {
                    @Override public void onClick(org.apache.wicket.ajax.AjaxRequestTarget target) {
                        clicks.incrementAndGet(); target.add(getMenu());
                    }
                };
            }
        };
        render(sidebar); tester.clickLink("sidebar:menu:items:0:link");
        assertThat(clicks.get()).isEqualTo(1); tester.assertComponentOnAjaxResponse(sidebar.getMenu());
        assertThat(html()).contains("Refresh", "sw-sidebar-menu-button");
    }
    @Test void independentProvidersCanUseDistinctPreferenceCookies() {
        tester.getRequest().addCookie(new Cookie("project-navigation", "collapsed"));
        provider(new SidebarProviderBehavior().setCookieName("project-navigation"), "expanded");
        assertThat(tester.getTagByWicketId("provider").getAttribute("data-state")).isEqualTo("collapsed");
        assertThat(tester.getTagByWicketId("provider").getAttribute("data-sidebar-cookie")).isEqualTo("project-navigation");
    }
    @Test void callerOwnedNavigationAndTriggersKeepTheirAccessibleNames() {
        var nav = new WebMarkupContainer("nav"); nav.add(Sw.Behaviors.sidebar());
        tester.startComponentInPage(nav, Markup.of("<nav wicket:id='nav' aria-labelledby='workspace-title'></nav>"));
        assertThat(tester.getTagByWicketId("nav").getAttribute("aria-label")).isNull();
        assertThat(tester.getTagByWicketId("nav").getAttribute("aria-labelledby")).isEqualTo("workspace-title");
        var trigger = new WebMarkupContainer("trigger"); trigger.add(Sw.Behaviors.sidebarTrigger(nav));
        tester.startComponentInPage(trigger, Markup.of("<button wicket:id='trigger' aria-label='Open workspace navigation'></button>"));
        assertThat(tester.getTagByWicketId("trigger").getAttribute("aria-label")).isEqualTo("Open workspace navigation");
    }

}
