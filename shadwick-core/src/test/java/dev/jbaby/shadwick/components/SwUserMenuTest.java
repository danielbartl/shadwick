package dev.jbaby.shadwick.components;

import org.apache.wicket.MarkupContainer;
import org.apache.wicket.MetaDataKey;
import org.apache.wicket.Session;
import org.apache.wicket.markup.IMarkupResourceStreamProvider;
import org.apache.wicket.markup.html.WebPage;
import org.apache.wicket.mock.MockHomePage;
import org.apache.wicket.model.Model;
import org.apache.wicket.util.resource.IResourceStream;
import org.apache.wicket.util.resource.StringResourceStream;
import org.apache.wicket.util.tester.TagTester;
import org.apache.wicket.util.tester.WicketTester;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Locale;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;

class SwUserMenuTest {

    /** The signed-in user's name, kept in the session as an application would. */
    static final MetaDataKey<String> USER = new MetaDataKey<>() {
    };

    private WicketTester tester;

    @BeforeEach
    void setUp() {
        tester = new WicketTester();
    }

    @AfterEach void tearDown() {
        if (tester.getLastResponse() != null) assertThat(tester.getLastResponseAsString()).doesNotContain("style=").doesNotContainPattern("\\son[a-z]+=");
        tester.destroy();
    }

    /** A page with the user menu; stateless, as a public page or a sign-in page is. */
    public static class MenuPage extends WebPage implements IMarkupResourceStreamProvider {

        public MenuPage() {
            SwUserMenu user = new SwUserMenu("user", () -> Session.get().getMetaData(USER));
            user.setDetail(Model.of("ada@example.com"));
            user.addLink("Profile", ProfilePage.class).setIcon("user");
            user.addLink("Settings", MenuPage.class);
            user.setSignInPage(SignInPage.class);
            add(user);
        }

        @Override
        public IResourceStream getMarkupResourceStream(MarkupContainer container, Class<?> containerClass) {
            return new StringResourceStream("<html><body><div wicket:id='user'></div></body></html>");
        }
    }

    public static class SignInPage extends MenuPage {
    }

    public static class ProfilePage extends MenuPage {
    }

    /** The application's own sign-in page, built on another one. */
    public static class ThemedSignInPage extends SignInPage {
    }

    private void signIn(String name) {
        tester.getSession().setMetaData(USER, name);
    }

    @Test
    void whileSignedOutItIsASignInLink() {
        tester.startPage(MenuPage.class);

        tester.assertInvisible("user:dropdown");
        TagTester signIn = tester.getTagByWicketId("signIn");
        assertThat(signIn.getValue()).contains("Sign in");
        assertThat(signIn.getAttribute("class")).contains("sw-sidebar-menu-button");
        tester.clickLink("user:signIn");
        tester.assertRenderedPage(SignInPage.class);
    }

    @Test
    void theSignInLinkHidesOnTheSignInPage() {
        tester.startPage(SignInPage.class);

        tester.assertInvisible("user:signIn");
        tester.assertInvisible("user:dropdown");

        tester.startPage(ThemedSignInPage.class);
        tester.assertInvisible("user:signIn");
    }

    @Test
    void withoutASignInPageItShowsNothingWhileSignedOut() {
        tester.startComponentInPage(new SwUserMenu("user", Model.of((String) null)));

        tester.assertInvisible("user:dropdown");
        tester.assertInvisible("user:signIn");
    }

    @Test
    void whileSignedInTheButtonShowsInitialsAndNameAndIsNamedForScreenReaders() {
        signIn("Ada Lovelace");
        tester.startPage(MenuPage.class);

        tester.assertInvisible("user:signIn");
        TagTester trigger = tester.getTagByWicketId("trigger");
        assertThat(trigger.getAttribute("aria-label")).isNull();
        assertThat(trigger.getAttribute("popovertarget")).isEqualTo(tester.getTagByWicketId("menu").getAttribute("id"));
        assertThat(trigger.getAttribute("aria-haspopup")).isEqualTo("menu");
        assertThat(trigger.getAttribute("aria-expanded")).isEqualTo("false");
        assertThat(tester.getTagByWicketId("menu").getAttribute("role")).isEqualTo("menu");
        tester.assertLabel("user:dropdown:trigger:avatar:container:initials", "AL");
        tester.assertLabel("user:dropdown:trigger:name", "Ada Lovelace");
    }

    @Test
    void theMenuShowsTheNameTheDetailTheLinksAndSignOut() {
        signIn("Ada Lovelace");
        tester.startPage(ProfilePage.class);

        tester.assertLabel("user:dropdown:menu:menuName", "Ada Lovelace");
        tester.assertLabel("user:dropdown:menu:detail", "ada@example.com");
        List<TagTester> items = TagTester.createTagsByAttribute(tester.getLastResponseAsString(), "role", "menuitem", false);
        assertThat(items).extracting(item -> item.getChild("wicket:id", "label").getValue())
                .containsExactly("Profile", "Settings", "Sign out");
        // The link to the page shown is marked as the current one
        assertThat(items).extracting(item -> item.getAttribute("aria-current")).containsExactly("page", null, null);
        assertThat(items.get(0).getChild("wicket:id", "icon").getName()).isEqualTo("svg");
        assertThat(items.get(1).getChild("wicket:id", "icon")).isNull();
        tester.assertVisible("user:dropdown:menu:linksEnd");
    }

    @Test
    void withoutLinksOrDetailTheMenuHoldsTheNameAndSignOut() {
        tester.startComponentInPage(new SwUserMenu("user", Model.of("Ada Lovelace")));

        tester.assertInvisible("user:dropdown:menu:detail");
        tester.assertInvisible("user:dropdown:menu:linksEnd");
        assertThat(TagTester.createTagsByAttribute(tester.getLastResponseAsString(), "role", "menuitem", false)).hasSize(1);
    }

    @Test
    void signOutInvalidatesTheSessionAndShowsTheHomePage() {
        signIn("Ada Lovelace");
        tester.startPage(MenuPage.class);

        tester.submitForm("user:signOut");

        tester.assertRenderedPage(MockHomePage.class);
        assertThat(tester.getSession().getMetaData(USER)).isNull();
    }

    @Test
    void anotherSitesPageCantSignTheUserOut() {
        signIn("Ada Lovelace");
        tester.startPage(MenuPage.class);
        // A form on another site posting here: the browser sends the session cookie along.
        // In lower case, as WicketTester's request, unlike a servlet container, matches
        // header names exactly.
        tester.getRequest().addHeader("sec-fetch-site", "cross-site");

        tester.submitForm("user:signOut");

        assertThat(tester.getLastResponse().getStatus()).isEqualTo(403);
        assertThat(tester.getSession().getMetaData(USER)).isEqualTo("Ada Lovelace");
    }

    @Test
    void signOutFromTheApplicationsOwnPageWorks() {
        signIn("Ada Lovelace");
        tester.startPage(MenuPage.class);
        tester.getRequest().addHeader("sec-fetch-site", "same-origin");

        tester.submitForm("user:signOut");

        tester.assertRenderedPage(MockHomePage.class);
        assertThat(tester.getSession().getMetaData(USER)).isNull();
    }

    @Test
    void signOutIsAPost() {
        signIn("Ada Lovelace");
        tester.startPage(MenuPage.class);

        TagTester form = tester.getTagByWicketId("signOut");
        assertThat(form.getName()).isEqualTo("form");
        assertThat(form.getAttribute("method")).isEqualTo("post");
    }

    @Test
    void signOutWorksOnAPageStillOpenAfterTheSessionExpired() {
        signIn("Ada Lovelace");
        tester.startPage(MenuPage.class);
        // Signed out meanwhile: the stateless page is built again, without the menu
        signIn(null);

        tester.submitForm("user:signOut");

        tester.assertRenderedPage(MockHomePage.class);
    }

    @Test
    void signOutAndWhatFollowsCanBeOverridden() {
        StringBuilder calls = new StringBuilder();
        tester.startComponentInPage(new SwUserMenu("user", Model.of("Ada Lovelace")) {
            @Override
            protected void signOut() {
                calls.append("signOut ");
            }

            @Override
            protected void onSignedOut() {
                calls.append("onSignedOut");
            }
        });

        tester.submitForm("user:signOut");

        assertThat(calls).hasToString("signOut onSignedOut");
    }

    @Test
    void itKeepsAPageStateless() {
        signIn("Ada Lovelace");
        MenuPage page = tester.startPage(MenuPage.class);

        assertThat(page.isPageStateless()).isTrue();
    }

    @Test
    void itFollowsTheSignedInUserOnEveryRender() {
        tester.startPage(MenuPage.class);
        tester.assertInvisible("user:dropdown");

        signIn("Grace Hopper");
        tester.startPage(tester.getLastRenderedPage());

        tester.assertLabel("user:dropdown:trigger:name", "Grace Hopper");
        tester.assertLabel("user:dropdown:trigger:avatar:container:initials", "GH");
    }

    @Test
    void someoneSignedInWithoutANameGetsTheMenu() {
        tester.startComponentInPage(new SwUserMenu("user", Model.of((String) null)) {
            @Override
            protected boolean isSignedIn() {
                return true;
            }
        });

        tester.assertVisible("user:dropdown");
        tester.assertLabel("user:dropdown:trigger:name", "Your account");
        tester.assertLabel("user:dropdown:trigger:avatar:container:initials", "?");
    }

    @Test
    void theModelsAreReadOncePerRequest() {
        AtomicInteger nameReads = new AtomicInteger();
        AtomicInteger pictureReads = new AtomicInteger();
        tester.startComponentInPage(new SwUserMenu("user", () -> {
            nameReads.incrementAndGet();
            return "Ada Lovelace";
        }).setPicture(() -> {
            pictureReads.incrementAndGet();
            return "/ada.png";
        }));

        assertThat(nameReads).hasValue(1);
        assertThat(pictureReads).hasValue(1);
    }

    @Test
    void aPictureCoversTheInitials() {
        tester.startComponentInPage(new SwUserMenu("user", Model.of("Ada Lovelace")).setPicture(Model.of("/ada.png")));

        TagTester img = tester.getTagByWicketId("img");
        assertThat(img.getAttribute("src")).isEqualTo("/ada.png");
        assertThat(img.getAttribute("alt")).isEmpty();
        tester.assertLabel("user:dropdown:trigger:avatar:container:initials", "AL");
    }

    @Test
    void anUnknownIconIsRefusedRightAway() {
        SwUserMenu menu = new SwUserMenu("user", Model.of("Ada Lovelace"));

        assertThatIllegalArgumentException().isThrownBy(() -> menu.addLink("Profile", MenuPage.class).setIcon("no-such-icon"));
    }
    @Test void headerPlacementNamesTheAvatarAndOpensBelowIt() {
        var user = new SwUserMenu("user", Model.of("Ada Lovelace")).setPlacement(SwUserMenu.Placement.HEADER);
        tester.startComponentInPage(user);
        assertThat(tester.getTagByWicketId("trigger").getAttribute("aria-label")).isEqualTo("Account: Ada Lovelace");
        assertThat(tester.getTagByWicketId("trigger").getAttribute("class")).contains("sw-button").doesNotContain("sw-sidebar-menu-button");
        assertThat(tester.getTagByWicketId("menu").getAttribute("data-side")).isEqualTo("bottom");
        tester.assertInvisible("user:dropdown:trigger:name");
        assertThat(tester.getTagByWicketId("initials").getAttribute("data-initials")).isEqualTo("AL");
        assertThat(tester.getTagByWicketId("menu").getAttribute("data-align")).isEqualTo("end");
        user.setPlacement(SwUserMenu.Placement.SIDEBAR);
        tester.startComponentInPage(user);
        assertThat(tester.getTagByWicketId("trigger").getAttribute("aria-label")).isNull();
        assertThat(tester.getTagByWicketId("menu").getAttribute("data-side")).isEqualTo("right");
        assertThat(tester.getTagByWicketId("trigger").getAttribute("data-sw-sidebar-hint")).isEmpty();
    }

    @Test void signOutSubmitsTheSeparateHiddenFormRatherThanPuttingAFormInAMenu() {
        tester.startComponentInPage(new SwUserMenu("user", Model.of("Ada")));
        TagTester form = tester.getTagByWicketId("signOut"), button = tester.getTagByWicketId("signOutButton");
        assertThat(form.getAttribute("hidden")).isNotNull();
        assertThat(button.getAttribute("type")).isEqualTo("submit");
        assertThat(button.getAttribute("form")).isEqualTo(form.getAttribute("id"));
        assertThat(tester.getTagByWicketId("menu").getValue()).doesNotContain("<form");
    }

    @Test void requestsOtherThanPostCannotSignOut() {
        signIn("Ada"); MenuPage page = tester.startPage(MenuPage.class);
        tester.getRequest().setMethod("GET");
        org.assertj.core.api.Assertions.assertThatThrownBy(() -> ((SwUserMenu) page.get("user")).getSignOutForm().process(null))
                .isInstanceOf(org.apache.wicket.request.http.flow.AbortWithHttpErrorCodeException.class);
        assertThat(tester.getSession().getMetaData(USER)).isEqualTo("Ada");
    }

    @Test void aDisabledMenuCannotProcessAForgedSignOut() {
        signIn("Ada"); MenuPage page = tester.startPage(MenuPage.class); page.get("user").setEnabled(false);
        tester.getRequest().setMethod("POST");
        org.assertj.core.api.Assertions.assertThatThrownBy(() -> ((SwUserMenu) page.get("user")).getSignOutForm().process(null))
                .isInstanceOf(org.apache.wicket.request.http.flow.AbortWithHttpErrorCodeException.class);
        assertThat(tester.getSession().getMetaData(USER)).isEqualTo("Ada");
    }

    @Test void detailsPicturesAndNamesAreEscapedAndReadOnce() {
        AtomicInteger reads = new AtomicInteger();
        var user = new SwUserMenu("user", Model.of("<Ada>"))
                .setDetail(() -> { reads.incrementAndGet(); return "<ada@example.com>"; }).setPicture("/ada.png");
        tester.startComponentInPage(user);
        assertThat(reads).hasValue(1);
        assertThat(tester.getLastResponseAsString()).contains("&lt;Ada&gt;", "&lt;ada@example.com&gt;").doesNotContain("<Ada>");
        user.setDetail(" "); tester.startComponentInPage(user);
        tester.assertInvisible("user:dropdown:menu:detail"); tester.assertInvisible("user:dropdown:trigger:triggerDetail");
    }

    @Test void unnamedAccountsCanUseEmailInitialsAndBlankNamesHaveAReadableFallback() {
        tester.startComponentInPage(new SwUserMenu("user", Model.of(" ")).setDetail("ada@example.com"));
        tester.assertLabel("user:dropdown:trigger:name", "Your account");
        tester.assertLabel("user:dropdown:trigger:avatar:container:initials", "A");
    }

    @Test void unauthorizedPageLinksAndTheirExtraSeparatorAreHidden() {
        tester.getApplication().getSecuritySettings().setAuthorizationStrategy(
                new org.apache.wicket.authorization.strategies.page.SimplePageAuthorizationStrategy(ProfilePage.class, MockHomePage.class) {
                    @Override protected boolean isAuthorized() { return false; }
                });
        var user = new SwUserMenu("user", Model.of("Ada")); user.addLink("Private profile", ProfilePage.class);
        tester.startComponentInPage(user);
        assertThat(tester.getLastResponseAsString()).doesNotContain("Private profile");
        tester.assertInvisible("user:dropdown:menu:linksEnd");
    }

    @Test void explicitPlacementAndLocalizationUseWicketResources() {
        var user = new SwUserMenu("user", Model.of("Ada")).setPlacement(SwUserMenu.Placement.HEADER)
                .setSide(dev.jbaby.shadwick.behaviors.DropdownMenuBehavior.Side.START)
                .setAlign(dev.jbaby.shadwick.behaviors.DropdownMenuBehavior.Align.CENTER);
        tester.getApplication().getResourceSettings().getStringResourceLoaders().add(0, new org.apache.wicket.resource.loader.ClassStringResourceLoader(SwUserMenuTest.class));
        tester.startComponentInPage(user);
        assertThat(tester.getTagByWicketId("menu").getAttribute("data-side")).isEqualTo("left");
        assertThat(tester.getTagByWicketId("menu").getAttribute("data-align")).isEqualTo("center");
        assertThat(tester.getTagByWicketId("trigger").getAttribute("aria-label")).isEqualTo("Account for Ada");
    }

}
