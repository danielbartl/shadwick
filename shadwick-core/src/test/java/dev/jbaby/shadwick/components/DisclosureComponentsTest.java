package dev.jbaby.shadwick.components;

import dev.jbaby.shadwick.Shadwick;
import dev.jbaby.shadwick.Sw;
import dev.jbaby.shadwick.behaviors.ButtonBehavior;
import org.apache.wicket.markup.Markup;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.mock.MockApplication;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.util.tester.TagTester;
import org.apache.wicket.util.tester.WicketTester;
import org.junit.jupiter.api.*;

import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.*;

class DisclosureComponentsTest {
    static final String ACCORDION = "<div class='sw-accordion'><details wicket:id='faq'><summary wicket:id='trigger'><span wicket:id='heading'><span wicket:id='title'></span></span><svg class='sw-accordion-trigger-icon' aria-hidden='true'></svg></summary><div wicket:id='body'><div wicket:id='content'></div></div></details></div>";
    WicketTester tester;
    @BeforeEach void before() {
        tester = new WicketTester(new MockApplication() {
            @Override protected void init() { super.init(); Shadwick.install(this); getCspSettings().blocking().strict(); }
        });
    }
    @AfterEach void after() {
        if (tester.getLastResponse() != null) assertThat(html()).doesNotContain("style=").doesNotContainPattern("\\son[a-z]+=");
        tester.destroy();
    }
    String html() { return tester.getLastResponseAsString(); }
    List<TagTester> tags(String attribute, String value) { return TagTester.createTagsByAttribute(html(), attribute, value, false); }
    SwAccordion<String> accordion() {
        return SwAccordion.of("faq", Model.ofList(List.of("<Shipping>", "Returns")), (item, title) -> {
            item.add(new Label("title", title), new Label("content", "About " + title));
        });
    }
    void render(SwAccordion<?> accordion) { tester.startComponentInPage(accordion, Markup.of(ACCORDION)); }
    void render(SwCollapsible more) { tester.startComponentInPage(more, Markup.of("<details wicket:id='more'></details>")); }

    @Test void accordionIsANativeExclusiveGroupWithAccessibleHeadingsAndEscapedContent() {
        render(accordion().setHeadingLevel(2).setOpenItems(1));
        var rows = tags("wicket:id", "faq");
        assertThat(rows).hasSize(2).allSatisfy(row -> assertThat(row.getName()).isEqualTo("details"));
        assertThat(rows.get(0).getAttribute("name")).startsWith("sw-accordion-").isEqualTo(rows.get(1).getAttribute("name"));
        assertThat(rows.get(0).getAttribute("open")).isNull(); assertThat(rows.get(1).getAttribute("open")).isNotNull();
        assertThat(tags("role", "heading")).hasSize(2).allSatisfy(tag -> assertThat(tag.getAttribute("aria-level")).isEqualTo("2"));
        tester.assertLabel("faq:0:title", "&lt;Shipping&gt;"); tester.assertLabel("faq:1:content", "About Returns");
        assertThat(html()).contains("&lt;Shipping&gt;").doesNotContain("js/shadwick", "role=\"region\"");
    }
    @Test void namesChangeOnEveryRenderAndMultipleSectionsLeaveTheExclusiveGroup() {
        var accordion = accordion(); render(accordion);
        String name = tags("wicket:id", "faq").get(0).getAttribute("name"); render(accordion);
        assertThat(tags("wicket:id", "faq").get(0).getAttribute("name")).isNotEqualTo(name);
        accordion.setExclusive(false).setOpenItems(0, 1); render(accordion);
        assertThat(tags("wicket:id", "faq")).allSatisfy(tag -> {
            assertThat(tag.getAttribute("name")).isNull(); assertThat(tag.getAttribute("open")).isNotNull();
        });
        accordion.setExclusive(true); render(accordion);
        assertThat(tags("open", "open")).hasSize(1);
    }
    @Test void disabledClosedAccordionRowsDoNotRenderTheirContentButOpenOnesDo() {
        var accordion = SwAccordion.of("faq", Model.ofList(List.of("Closed", "Open")), (item, title) -> {
            item.setEnabled(false); item.add(new Label("title", title), new Label("content", "Hidden for " + title));
        }).setOpenItems(1);
        render(accordion);
        assertThat(html()).doesNotContain("Hidden for Closed").contains("Hidden for Open");
        assertThat(tags("wicket:id", "trigger")).allSatisfy(tag -> {
            assertThat(tag.getAttribute("aria-disabled")).isEqualTo("true"); assertThat(tag.getAttribute("tabindex")).isEqualTo("-1");
        });
    }
    @Test void accordionModelsLoadOnceAndDetachAndConfigurationRejectsInvalidValues() {
        AtomicInteger reads = new AtomicInteger(), detaches = new AtomicInteger();
        IModel<List<String>> model = new IModel<>() {
            @Override public List<String> getObject() { reads.incrementAndGet(); return List.of("Question"); }
            @Override public void detach() { detaches.incrementAndGet(); }
        };
        render(Sw.Components.accordion("faq", model, (item, title) -> item.add(new Label("title", title), new Label("content", "Answer"))));
        assertThat(reads.get()).isEqualTo(1); assertThat(detaches.get()).isPositive();
        assertThatIllegalArgumentException().isThrownBy(() -> accordion().setOpenItems(-1));
        assertThatIllegalArgumentException().isThrownBy(() -> accordion().setHeadingLevel(7));
    }
    @Test void collapsibleHasANativeFirstSummaryEscapedLabelAndContentSlot() {
        var more = new SwCollapsible("more", "More <details>").setContent("A <little> more"); render(more);
        var root = tester.getTagByWicketId("more");
        assertThat(root.getName()).isEqualTo("details"); assertThat(root.getAttribute("open")).isNull();
        assertThat(root.getValue().replaceAll("</?wicket:[^>]+>", "").strip()).startsWith("<summary");
        assertThat(html()).contains("More &lt;details&gt;", "A &lt;little&gt; more").doesNotContain("js/shadwick", "aria-expanded", "role=\"button\"");
        assertThat(more.getContent().getId()).isEqualTo(SwCollapsible.CONTENT_ID);
        assertThatIllegalArgumentException().isThrownBy(() -> more.setContent(new Label("wrong", "text")));
    }
    @Test void collapsibleCanBeDrawnAsAButtonAndStartsOpenFromTheServer() {
        render(new SwCollapsible("more", "More").setOpen(true).setVariant(ButtonBehavior.Variant.OUTLINE)
                .setSize(ButtonBehavior.Size.SMALL).setContent("Content"));
        var trigger = tester.getTagByWicketId("trigger");
        assertThat(trigger.getAttribute("class")).contains("sw-button", "sw-collapsible-trigger");
        assertThat(trigger.getAttribute("data-variant")).isEqualTo("outline");
        assertThat(trigger.getAttribute("data-size")).isEqualTo("sm");
        assertThat(tester.getTagByWicketId("more").getAttribute("open")).isNotNull();
    }
    @Test void disabledCollapsibleOmitsClosedContentAndRetainsOpenContent() {
        var more = new SwCollapsible("more", "Unavailable").setContent("Do not reveal while closed"); more.setEnabled(false); render(more);
        assertThat(html()).doesNotContain("Do not reveal while closed");
        assertThat(tester.getTagByWicketId("trigger").getAttribute("aria-disabled")).isEqualTo("true");
        assertThat(tester.getTagByWicketId("trigger").getAttribute("tabindex")).isEqualTo("-1");
        more.setOpen(true); render(more); assertThat(html()).contains("Do not reveal while closed");
    }
}
