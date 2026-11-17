package dev.jbaby.shadwick.components;

import org.apache.wicket.MarkupContainer;
import org.apache.wicket.markup.IMarkupResourceStreamProvider;
import org.apache.wicket.markup.html.WebPage;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.repeater.data.ListDataProvider;
import org.apache.wicket.util.resource.IResourceStream;
import org.apache.wicket.util.resource.StringResourceStream;
import org.apache.wicket.util.tester.TagTester;
import org.apache.wicket.util.tester.WicketTester;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Locale;
import java.util.stream.IntStream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/** A batch at a time: Ajax appends only the next one, a plain link renders the page with it, and the count. */
class SwLoadMoreListTest {
    WicketTester tester;

    @BeforeEach void start() { tester = new WicketTester(); tester.getSession().setLocale(Locale.US); }
    @AfterEach void stop() {
        if (tester.getLastResponse() != null)
            assertThat(tester.getLastResponseAsString()).doesNotContain("style=").doesNotContainPattern("\\son[a-z]+=");
        tester.destroy();
    }

    String html() { return tester.getLastResponseAsString(); }
    List<TagTester> items() { return TagTester.createTagsByAttribute(html(), "role", "listitem", false); }

    /** Counts how often the provider counts its items. */
    static final class Numbers extends ListDataProvider<Integer> {
        int sizeCalls;
        Numbers(int count) { super(IntStream.rangeClosed(1, count).boxed().toList()); }
        @Override public long size() { sizeCalls++; return super.size(); }
    }

    public static class ListPage extends WebPage implements IMarkupResourceStreamProvider {
        public final Numbers provider;
        public final SwLoadMoreList<Integer> numbers;

        public ListPage() { this(45); }
        public ListPage(int count) {
            provider = new Numbers(count);
            numbers = new SwLoadMoreList<>("numbers", provider, 20, (id, number) -> new Label(id, () -> "Item " + number.getObject()));
            add(numbers);
        }

        @Override
        public IResourceStream getMarkupResourceStream(MarkupContainer container, Class<?> containerClass) {
            return new StringResourceStream("<html><body><div wicket:id='numbers'></div></body></html>");
        }
    }

    @Test void showsABatchAndAppendsTheNext() {
        ListPage page = tester.startPage(ListPage.class);
        assertThat(items()).hasSize(20);
        assertThat(html()).contains(">Load more<").contains("20 of 45").doesNotContain("Item 21<");
        assertThat(tester.getTagByWicketId("list").getAttribute("role")).isEqualTo("list");
        // The count describes the button
        String count = tester.getTagByWicketId("count").getAttribute("id");
        assertThat(tester.getTagByWicketId("more").getAttribute("aria-describedby")).isEqualTo(count);
        // Nothing to announce on a page load
        assertThat(tester.getTagByWicketId("status").getValue()).isEmpty();

        tester.clickLink("numbers:footer:more", true);
        String batch = page.numbers.getMarkupId() + "-batch-1";
        assertThat(html()).contains("Item 21").contains("Item 40").doesNotContain(">Item 1<").doesNotContain(">Item 20<")
                .contains("Shadwick.loadMoreSlot(\"" + page.numbers.get("list").getMarkupId() + "\",\"" + batch + "\")")
                .contains("Shadwick.loadMoreFocus(\"" + batch + "\")")
                .contains("40 of 45").contains("Showing 40 of 45");
        assertThat(page.numbers.getShownCount()).isEqualTo(40);

        tester.clickLink("numbers:footer:more", true);
        assertThat(html()).contains("Item 45").contains("Showing 45 of 45");
        tester.assertInvisible("numbers:footer");
    }

    @Test void withoutJavaScriptTheLinkRendersThePageWithTheNextBatchAndScrollsToIt() {
        ListPage page = tester.startPage(ListPage.class);
        String batch = page.numbers.getMarkupId() + "-batch-1";
        assertThat(tester.getTagByWicketId("more").getAttribute("href")).endsWith("#" + batch);

        tester.clickLink("numbers:footer:more", false);
        assertThat(items()).hasSize(40);
        assertThat(html()).contains("id=\"" + batch + "\"").contains("40 of 45");
        assertThat(tester.getTagByWicketId("more").getAttribute("href")).endsWith("#" + page.numbers.getMarkupId() + "-batch-2");
    }

    @Test void countsOncePerRequestAndNotBeforeRendering() {
        ListPage page = new ListPage();
        assertThat(page.provider.sizeCalls).isZero();
        tester.startPage(page);
        assertThat(page.provider.sizeCalls).isEqualTo(1);
        tester.clickLink("numbers:footer:more", true);
        assertThat(page.provider.sizeCalls).isEqualTo(2);
    }

    @Test void resetStartsAgainAndLoadOnScrollMarksTheButton() {
        tester.startPage(new ListPage(5));
        tester.assertInvisible("numbers:footer");
        assertThat(items()).hasSize(5);

        ListPage page = new ListPage();
        page.numbers.setLoadOnScroll(true);
        tester.startPage(page);
        assertThat(tester.getTagByWicketId("more").getAttribute("data-sw-load-on-scroll")).isNotNull();
        tester.clickLink("numbers:footer:more", true);
        assertThat(page.numbers.getShownCount()).isEqualTo(40);
        page.numbers.reset();
        tester.startPage(page);
        assertThat(items()).hasSize(20);
        assertThat(page.numbers.getShownCount()).isEqualTo(20);
    }

    @Test void aProviderWithFewerItemsThanItCountedEndsTheList() {
        var page = new ListPage();
        // Counts 45, yields 30: the second batch comes up short and there is nothing more to load
        var shrinking = new SwLoadMoreList<>("numbers", new ListDataProvider<>(IntStream.rangeClosed(1, 30).boxed().toList()) {
            @Override public long size() { return 45; }
        }, 20, (id, number) -> new Label(id, () -> "Item " + number.getObject()));
        page.replace(shrinking);
        tester.startPage(page);
        tester.clickLink("numbers:footer:more", true);
        tester.assertInvisible("numbers:footer");
        assertThat(shrinking.getShownCount()).isEqualTo(30);
    }

    @Test void batchSizeAndItemIdsAreChecked() {
        assertThatIllegalArgumentException().isThrownBy(() -> new SwLoadMoreList<>("x", new ListDataProvider<>(List.of(1)), 0, Label::new));
        var page = new ListPage();
        page.replace(new SwLoadMoreList<>("numbers", new ListDataProvider<>(List.of(1)), 5, (id, n) -> new Label("other")));
        assertThatThrownBy(() -> tester.startPage(page)).hasStackTraceContaining("An item must use the id its factory is given");
    }

    @Test void isTranslatedNamedAndSerializes() throws Exception {
        tester.getSession().setLocale(Locale.GERMAN);
        ListPage page = new ListPage();
        page.numbers.setLabel("Zahlen");
        tester.startPage(page);
        assertThat(html()).contains("Mehr laden").contains("20 von 45");
        assertThat(tester.getTagByWicketId("list").getAttribute("aria-label")).isEqualTo("Zahlen");
        try (var out = new java.io.ObjectOutputStream(new java.io.ByteArrayOutputStream())) {
            out.writeObject(tester.getLastRenderedPage());
        }
    }
}
