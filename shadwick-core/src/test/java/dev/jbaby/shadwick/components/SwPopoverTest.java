package dev.jbaby.shadwick.components;

import org.apache.wicket.MarkupContainer;
import org.apache.wicket.markup.IMarkupResourceStreamProvider;
import org.apache.wicket.markup.html.WebPage;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.util.resource.IResourceStream;
import org.apache.wicket.util.resource.StringResourceStream;
import org.apache.wicket.util.tester.TagTester;
import org.apache.wicket.util.tester.WicketTester;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

import static org.assertj.core.api.Assertions.*;

class SwPopoverTest {
    private WicketTester tester;
    @BeforeEach void setUp() { tester = new WicketTester(); }
    @AfterEach void tearDown() { tester.destroy(); }
    private String html() { return tester.getLastResponseAsString(); }

    @Test void anyContentLivesInANamedNonModalPopoverWithANativeTrigger() {
        SwPopover popover = new SwPopover("filters", "Filters", id -> new Label(id, "Only overdue"));
        tester.startComponentInPage(popover);
        TagTester panel = tester.getTagByWicketId("popover"), trigger = tester.getTagByWicketId("trigger");
        assertThat(panel.getAttribute("popover")).isEqualTo("auto");
        assertThat(panel.getAttribute("role")).isEqualTo("dialog");
        assertThat(panel.getAttribute("aria-label")).isEqualTo("Filters");
        assertThat(panel.getAttribute("aria-modal")).isNull();
        assertThat(panel.getAttribute("tabindex")).isEqualTo("-1");
        assertThat(trigger.getAttribute("type")).isEqualTo("button");
        assertThat(trigger.getAttribute("popovertarget")).isEqualTo(panel.getAttribute("id"));
        assertThat(trigger.getAttribute("aria-controls")).isEqualTo(panel.getAttribute("id"));
        assertThat(trigger.getAttribute("aria-haspopup")).isEqualTo("dialog");
        assertThat(trigger.getAttribute("aria-expanded")).isEqualTo("false");
        assertThat(popover.getContent().getOutputMarkupId()).isTrue();
        assertThat(popover.getOutputMarkupId()).isTrue();
        tester.assertLabel("filters:popover:content", "Only overdue");
        tester.assertInvisible("filters:popover:header");
        assertThat(html()).doesNotContain("style=").doesNotContainPattern("\\son[a-z]+=");
    }

    @Test void theFactoryMustReturnTheSuppliedContentId() {
        assertThatIllegalArgumentException().isThrownBy(() -> new SwPopover("p", "Open", id -> new Label("other")));
        assertThatIllegalArgumentException().isThrownBy(() -> new SwPopover("p", "Open", id -> null));
        assertThatNullPointerException().isThrownBy(() -> new SwPopover("p", "Open", null));
    }

    @Test void optionalTitleAndDescriptionNameAndDescribeThePanelAndEscapeText() {
        SwPopover popover = new SwPopover("p", "Open", id -> new Label(id, "Body"))
                .setTitle("Project <details>").setDescription("Only <you> can see this.");
        tester.startComponentInPage(popover);
        TagTester panel = tester.getTagByWicketId("popover");
        assertThat(panel.getAttribute("aria-labelledby")).isEqualTo(tester.getTagByWicketId("title").getAttribute("id"));
        assertThat(panel.getAttribute("aria-describedby")).isEqualTo(tester.getTagByWicketId("description").getAttribute("id"));
        assertThat(panel.getAttribute("aria-label")).isNull();
        assertThat(html()).contains("Project &lt;details&gt;", "Only &lt;you&gt; can see this.");
        popover.setTitle((String) null).setDescription("");
        tester.startComponentInPage(popover);
        assertThat(tester.getTagByWicketId("popover").getAttribute("aria-labelledby")).isNull();
        assertThat(tester.getTagByWicketId("popover").getAttribute("aria-describedby")).isNull();
        assertThat(tester.getTagByWicketId("popover").getAttribute("aria-label")).isEqualTo("Open");
    }

    @ParameterizedTest @EnumSource(SwPopover.Side.class)
    void placementUsesLogicalInlineSides(SwPopover.Side side) {
        tester.startComponentInPage(new SwPopover("p", "Open", id -> new Label(id)).setSide(side));
        String expected = switch (side) { case START -> "left"; case END -> "right"; default -> side.name().toLowerCase(java.util.Locale.ROOT); };
        assertThat(tester.getTagByWicketId("popover").getAttribute("data-side")).isEqualTo(expected);
    }

    @ParameterizedTest @EnumSource(SwPopover.Align.class)
    void alignmentIsRenderedForTheCssAnchor(SwPopover.Align align) {
        tester.startComponentInPage(new SwPopover("p", "Open", id -> new Label(id)).setAlign(align));
        assertThat(tester.getTagByWicketId("popover").getAttribute("data-align")).isEqualTo(align.name().toLowerCase(java.util.Locale.ROOT));
    }

    public static class PopoverPage extends WebPage implements IMarkupResourceStreamProvider {
        final Model<String> text = Model.of("Initial");
        final SwPopover popover = new SwPopover("p", "Open", id -> new Label(id, text));
        public PopoverPage() {
            add(popover);
            add(SwButton.onClick("refresh", "Refresh", target -> { text.setObject("Updated"); target.add(popover.getContent()); }));
            add(SwButton.onClick("close", "Close", popover::close));
        }
        @Override public IResourceStream getMarkupResourceStream(MarkupContainer container, Class<?> containerClass) {
            return new StringResourceStream("<html><body><div wicket:id='p'></div><button wicket:id='refresh'></button>"
                    + "<button wicket:id='close'></button></body></html>");
        }
    }

    @Test void contentUpdatesIndependentlyAndCloseUsesTheQuotedNativePanelId() {
        PopoverPage page = tester.startPage(PopoverPage.class);
        tester.clickLink("refresh");
        tester.assertComponentOnAjaxResponse(page.popover.getContent());
        assertThat(html()).contains("Updated").doesNotContain("popovertarget=");
        tester.clickLink("close");
        assertThat(html()).contains("Shadwick.closePopover(\"" + page.popover.get("popover").getMarkupId() + "\")");
    }

    @Test void reactiveHeaderModelsDetachAndContentCanBeReplaced() {
        int[] detached = {0, 0};
        IModel<String> title = new Model<>("Title") { @Override public void detach() { detached[0]++; } };
        IModel<String> description = new Model<>("Description") { @Override public void detach() { detached[1]++; } };
        SwPopover popover = new SwPopover("p", Model.of("Open"), id -> new Label(id, "Old"))
                .setTitle(title).setDescription(description).setContent(new Label(SwPopover.CONTENT_ID, "New"));
        tester.startComponentInPage(popover);
        tester.assertLabel("p:popover:content", "New");
        assertThat(detached[0]).isPositive(); assertThat(detached[1]).isPositive();
        assertThatNullPointerException().isThrownBy(() -> popover.setSide(null));
        assertThatNullPointerException().isThrownBy(() -> popover.setAlign(null));
    }
}
