package dev.jbaby.shadwick.components;

import dev.jbaby.shadwick.Sw;
import dev.jbaby.shadwick.behaviors.*;
import org.apache.wicket.markup.Markup;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.util.tester.TagTester;
import org.apache.wicket.util.tester.WicketTester;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

import static org.assertj.core.api.Assertions.*;

/** API and rendered semantics, following Wicket Oat's GeneralComponentsTest and icon tests. */
class DisplayComponentsTest {
    private WicketTester tester;

    @BeforeEach
    void setUp() { tester = new WicketTester(); }

    @AfterEach
    void tearDown() {
        if (tester.getLastResponse() != null) {
            assertThat(html()).doesNotContain("style=").doesNotContainPattern("\\son[a-z]+=");
        }
        tester.destroy();
    }

    private String html() { return tester.getLastResponseAsString(); }
    private TagTester tag(String id) { return tester.getTagByWicketId(id); }

    @ParameterizedTest
    @EnumSource(BadgeBehavior.Variant.class)
    void badgesEscapeTheirLabelsAndSupportEveryShadcnVariant(BadgeBehavior.Variant variant) {
        tester.startComponentInPage(new SwBadge("badge", "<New>", variant));
        assertThat(tag("badge").getAttribute("class")).contains("sw-badge");
        assertThat(tag("badge").getAttribute("data-variant")).isEqualTo(variant.value());
        assertThat(tag("badge").getValue()).isEqualTo("&lt;New&gt;");
    }

    @ParameterizedTest
    @EnumSource(AlertBehavior.Variant.class)
    void alertsHaveAnAccessibleRoleAndEscapeMessages(AlertBehavior.Variant variant) {
        tester.startComponentInPage(new SwAlert("alert", "<script>unsafe</script>", variant));
        assertThat(tag("alert").getAttribute("role")).isEqualTo("alert");
        assertThat(tag("alert").getAttribute("data-variant")).isEqualTo(variant.value());
        assertThat(html()).contains("&lt;script&gt;unsafe&lt;/script&gt;");
    }

    @Test
    void aBehaviorCanStyleExistingMarkupWithoutLosingItsClassesOrChildren() {
        WebMarkupContainer card = new WebMarkupContainer("card");
        card.add(new Label("title", "My card"));
        card.add(Sw.Behaviors.card().setSize(CardBehavior.Size.SMALL));
        tester.startComponentInPage(card, Markup.of("<article wicket:id='card' class='app-card'><h2 wicket:id='title' class='sw-card-title'></h2></article>"));
        assertThat(tag("card").getAttribute("class")).isEqualTo("app-card sw-card");
        assertThat(tag("card").getAttribute("data-size")).isEqualTo("sm");
        tester.assertLabel("card:title", "My card");
    }

    @Test
    void cardComponentsKeepCallerOwnedContent() {
        SwCard card = new SwCard("card").setSize(CardBehavior.Size.SMALL);
        card.add(new Label("content", "Details"));
        tester.startComponentInPage(card, Markup.of("<article wicket:id='card'><p wicket:id='content' class='sw-card-content'></p></article>"));
        tester.assertLabel("card:content", "Details");
        assertThat(tag("card").getAttribute("class")).isEqualTo("sw-card");
    }

    @Test
    void badgeVariantsReadAndDetachTheirModelsOnEveryRender() {
        boolean[] detached = {false};
        IModel<BadgeBehavior.Variant> variant = new Model<>(BadgeBehavior.Variant.DESTRUCTIVE) {
            @Override
            public void detach() { detached[0] = true; }
        };
        SwBadge badge = new SwBadge("badge", Model.of("New"), variant);
        tester.startComponentInPage(badge);
        assertThat(tag("badge").getAttribute("data-variant")).isEqualTo("destructive");
        assertThat(detached[0]).isTrue();
        variant.setObject(BadgeBehavior.Variant.DEFAULT);
        tester.startComponentInPage(badge);
        assertThat(tag("badge").getAttribute("data-variant")).isNull();
    }

    @Test
    void aDecorativeSeparatorIsHiddenFromAssistiveTechnology() {
        tester.startComponentInPage(new SwSeparator("separator"));
        assertThat(tag("separator").getAttribute("role")).isEqualTo("none");
        assertThat(tag("separator").getAttribute("aria-orientation")).isNull();
    }

    @ParameterizedTest
    @EnumSource(SeparatorBehavior.Orientation.class)
    void aSemanticSeparatorExposesItsOrientation(SeparatorBehavior.Orientation orientation) {
        tester.startComponentInPage(new SwSeparator("separator").setDecorative(false).setOrientation(orientation));
        assertThat(tag("separator").getAttribute("role")).isEqualTo("separator");
        assertThat(tag("separator").getAttribute("aria-orientation"))
                .isEqualTo(orientation == SeparatorBehavior.Orientation.VERTICAL ? "vertical" : null);
    }

    @Test
    void keyboardKeysEscapeTextAndKeepKbdMarkup() {
        tester.startComponentInPage(new SwKbd("key", "<"), Markup.of("<kbd wicket:id='key'></kbd>"));
        assertThat(tag("key").getName()).isEqualTo("kbd");
        assertThat(tag("key").getValue()).isEqualTo("&lt;");
    }

    @ParameterizedTest
    @EnumSource(SkeletonBehavior.Shape.class)
    void loadingPlaceholdersAreDecorative(SkeletonBehavior.Shape shape) {
        tester.startComponentInPage(new SwSkeleton("skeleton", shape));
        assertThat(tag("skeleton").getAttribute("aria-hidden")).isEqualTo("true");
    }

    @ParameterizedTest
    @EnumSource(SpinnerBehavior.Size.class)
    void spinnersAreNamedLoadingStatuses(SpinnerBehavior.Size size) {
        tester.startComponentInPage(new SwSpinner("spinner", size), Markup.of("<svg wicket:id='spinner'></svg>"));
        assertThat(tag("spinner").getAttribute("role")).isEqualTo("status");
        assertThat(tag("spinner").getAttribute("aria-label")).isEqualTo("Loading");
        assertThat(tag("spinner").getAttribute("aria-hidden")).isNull();
        assertThat(tag("spinner").getValue()).contains("<path");
    }

    @Test
    void aSpinnerCanDescribeWhatIsLoadingOnAWrapperTag() {
        tester.startComponentInPage(new SwSpinner("spinner").setLabel("Loading projects"), Markup.of("<span wicket:id='spinner'></span>"));
        assertThat(tag("spinner").getAttribute("aria-label")).isEqualTo("Loading projects");
        assertThat(tag("spinner").getChild("svg").getAttribute("aria-label")).isEqualTo("Loading projects");
    }

    @Test
    void iconsRenderInlineAndAreDecorativeByDefault() {
        tester.startComponentInPage(new SwIcon("icon", "pencil"), Markup.of("<svg wicket:id='icon'></svg>"));
        assertThat(tag("icon").getAttribute("viewBox")).isEqualTo("0 0 24 24");
        assertThat(tag("icon").getAttribute("aria-hidden")).isEqualTo("true");
        assertThat(tag("icon").getValue()).startsWith("<path");
        assertThat(SwIcon.names()).contains("plus", "search", "chart-line", "loader-circle");
    }

    @Test
    void anIconsNameCanChangeBetweenRendersAndItsLabelIsEscaped() {
        Model<String> iconName = Model.of("pencil");
        SwIcon icon = new SwIcon("icon", iconName).setLabel("Delete \"project\"");
        tester.startComponentInPage(icon, Markup.of("<svg wicket:id='icon'></svg>"));
        assertThat(tag("icon").getAttribute("aria-label")).isEqualTo("Delete \"project\"");
        assertThat(html()).contains("aria-label=\"Delete &quot;project&quot;\"");
        iconName.setObject("search");
        tester.startComponentInPage(icon, Markup.of("<svg wicket:id='icon'></svg>"));
        assertThat(tag("icon").getValue()).contains("<circle");
    }

    @Test
    void unknownIconsFailClearlyAndApplicationsCanRegisterTrustedShapes() {
        assertThatIllegalArgumentException().isThrownBy(() -> new SwIcon("icon", "unknown-icon"));
        SwIcon.register("test-brand", "<circle cx=\"12\" cy=\"12\" r=\"10\"/>");
        tester.startComponentInPage(new SwIcon("icon", "test-brand"), Markup.of("<span wicket:id='icon'></span>"));
        assertThat(tag("icon").getChild("svg").getValue()).contains("<circle");
    }

    @Test
    void emptyStateDescriptionsFollowTheirModelsInsteadOfFreezingAtConstruction() {
        Model<String> description = Model.of();
        SwEmptyState empty = new SwEmptyState("empty", Model.of("No projects"), description).setIcon("folder");
        tester.startComponentInPage(empty);
        tester.assertInvisible("empty:message");
        description.setObject("Create your first project.");
        tester.startComponentInPage(empty);
        tester.assertLabel("empty:message", "Create your first project.");
        tester.assertVisible("empty:media");
    }

    @Test
    void avatarsRenderInitialsWhenThereIsNoPicture() {
        tester.startComponentInPage(new SwAvatar("avatar", null, Model.of("AL"), SwAvatar.Size.LARGE).setLabel("Ada Lovelace"));
        tester.assertInvisible("avatar:container:img");
        tester.assertLabel("avatar:container:initials", "AL");
        assertThat(tag("container").getAttribute("data-size")).isEqualTo("lg");
        assertThat(tag("initials").getAttribute("aria-label")).isEqualTo("Ada Lovelace");
    }

    @Test
    void avatarPicturesAndInitialsFollowModelsAcrossRenders() {
        Model<String> url = Model.of("/ada.png");
        Model<String> initials = Model.of("AL");
        SwAvatar avatar = new SwAvatar("avatar", url, initials, SwAvatar.Size.DEFAULT);
        tester.startComponentInPage(avatar);
        assertThat(tag("img").getAttribute("src")).isEqualTo("/ada.png");
        assertThat(tag("img").getAttribute("alt")).isEqualTo("AL");
        url.setObject(" ");
        initials.setObject("GH");
        tester.startComponentInPage(avatar);
        tester.assertInvisible("avatar:container:img");
        tester.assertLabel("avatar:container:initials", "GH");
    }

    @Test
    void initialsUseWholeUnicodeCharactersAndFirstAndLastWords() {
        assertThat(SwAvatar.initialsOf("Ada Augusta Lovelace")).isEqualTo("AL");
        assertThat(SwAvatar.initialsOf("\u3000grace\u3000hopper\u3000")).isEqualTo("GH");
        assertThat(SwAvatar.initialsOf(null)).isEqualTo("?");
        assertThat(SwAvatar.initialsOf("😀 Person")).isEqualTo("😀P");
    }
}
