package dev.jbaby.shadwick.behaviors;

import dev.jbaby.shadwick.ButtonPage;
import dev.jbaby.shadwick.Sw;
import org.apache.wicket.markup.Markup;
import org.apache.wicket.markup.html.link.Link;
import org.apache.wicket.util.tester.TagTester;
import org.apache.wicket.util.tester.WicketTester;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.ValueSource;

import static org.assertj.core.api.Assertions.assertThat;

class ButtonBehaviorTest {

    private WicketTester tester;

    @BeforeEach
    void setUp() {
        tester = new WicketTester();
    }

    @AfterEach
    void tearDown() {
        tester.destroy();
    }

    private static Link<Void> link(ButtonBehavior behavior) {
        Link<Void> link = new Link<>("button") {
            @Override
            public void onClick() {
            }
        };
        link.add(behavior);
        return link;
    }

    private TagTester render(ButtonBehavior behavior, String tag) {
        tester.startComponentInPage(link(behavior), Markup.of("<" + tag + " wicket:id=\"button\">Save</" + tag + ">"));
        return tester.getTagByWicketId("button");
    }

    @ParameterizedTest
    @ValueSource(strings = {"a", "button"})
    void theDefaultsWriteOnlyTheClass(String tag) {
        TagTester button = render(Sw.Behaviors.button(), tag);

        assertThat(button.getAttribute("class")).isEqualTo("sw-button");
        assertThat(button.getAttribute("data-variant")).isNull();
        assertThat(button.getAttribute("data-size")).isNull();
    }

    @ParameterizedTest
    @EnumSource(value = ButtonBehavior.Variant.class, names = "DEFAULT", mode = EnumSource.Mode.EXCLUDE)
    void everyVariantIsADataAttribute(ButtonBehavior.Variant variant) {
        TagTester button = render(Sw.Behaviors.button(variant), "button");

        assertThat(button.getAttribute("data-variant")).isEqualTo(variant.name().toLowerCase());
    }

    @Test
    void theSizesAreShadcns() {
        assertThat(render(new ButtonBehavior().setSize(ButtonBehavior.Size.SMALL), "a").getAttribute("data-size")).isEqualTo("sm");
        assertThat(render(new ButtonBehavior().setSize(ButtonBehavior.Size.LARGE), "a").getAttribute("data-size")).isEqualTo("lg");
    }

    @Test
    void anIconButtonIsSquare() {
        Link<Void> link = link(new ButtonBehavior(ButtonBehavior.Variant.OUTLINE).setSize(ButtonBehavior.Size.ICON));
        tester.startComponentInPage(link, Markup.of("<button wicket:id=\"button\" aria-label=\"Delete\"><svg></svg></button>"));

        TagTester button = tester.getTagByWicketId("button");
        assertThat(button.getAttribute("data-size")).isEqualTo("icon");
        assertThat(button.getAttribute("data-variant")).isEqualTo("outline");
    }

    @Test
    void theMarkupsOwnClassesStay() {
        tester.startComponentInPage(link(Sw.Behaviors.button()), Markup.of("<a wicket:id=\"button\" class=\"wide\">Save</a>"));

        assertThat(tester.getTagByWicketId("button").getAttribute("class")).isEqualTo("wide sw-button");
    }

    @Test
    void aDisabledLinkIsAriaDisabled() {
        Link<Void> link = link(Sw.Behaviors.button());
        link.setEnabled(false);
        tester.startComponentInPage(link, Markup.of("<a wicket:id=\"button\">Save</a>"));

        TagTester button = tester.getTagByWicketId("button");
        assertThat(button.getAttribute("aria-disabled")).isEqualTo("true");
        assertThat(button.getAttribute("href")).isNull();
    }

    @Test
    void anEnabledLinkIsNotAriaDisabled() {
        assertThat(render(Sw.Behaviors.button(), "a").getAttribute("aria-disabled")).isNull();
    }

    @Test
    void theButtonAddsTheStylesheetWithoutInstall() {
        tester.startPage(ButtonPage.class);

        assertThat(tester.getLastResponseAsString()).containsPattern("css/shadwick-vega[^\"]*\\.css");
    }

    @Test
    void theButtonRendersNoInlineStylesOrHandlers() {
        render(Sw.Behaviors.button(ButtonBehavior.Variant.DESTRUCTIVE).setSize(ButtonBehavior.Size.LARGE), "button");

        assertThat(tester.getLastResponseAsString()).doesNotContain("style=").doesNotContainPattern("\\son[a-z]+=");
    }
}
