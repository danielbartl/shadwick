package dev.jbaby.shadwick.components;

import dev.jbaby.shadwick.Shadwick;
import dev.jbaby.shadwick.Sw;
import dev.jbaby.shadwick.behaviors.TableBehavior;
import org.apache.wicket.markup.Markup;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.link.ExternalLink;
import org.apache.wicket.markup.html.navigation.paging.IPageableItems;
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

class TableAndPaginationTest {
    WicketTester tester;
    @BeforeEach void before() {
        tester = new WicketTester(new MockApplication() {
            @Override protected void init() { super.init(); Shadwick.install(this); }
        });
    }
    @AfterEach void after() { tester.destroy(); }
    String html() { return tester.getLastResponseAsString(); }
    static class Pages implements IPageableItems {
        long current, count = 270, size = 10;
        @Override public long getCurrentPage() { return current; }
        @Override public void setCurrentPage(long page) { current = page; }
        @Override public long getPageCount() { return count == 0 ? 0 : 1 + (count - 1) / size; }
        @Override public long getItemCount() { return count; }
        @Override public long getItemsPerPage() { return size; }
        @Override public void setItemsPerPage(long size) { this.size = size; }
    }
    @Test void aPlainTableKeepsNativeMarkupWithIndependentDensityAndStickyConfiguration() {
        var table = new SwTable("table").setSize(TableBehavior.Size.SMALL).setStickyFirstColumn(true);
        tester.startComponentInPage(table, Markup.of("<table wicket:id='table'><caption>Invoice report</caption><thead><tr><th scope='col'>Name</th></tr></thead><tbody><tr><td>Ada</td></tr></tbody></table>"));
        assertThat(html()).contains("sw-table", "data-size=\"sm\"", "data-sticky=\"first-column\"", "scope='col'", "Invoice report", "Ada");
        table.setSize(TableBehavior.Size.DEFAULT).setStickyFirstColumn(false);
        tester.startComponentInPage(table, Markup.of("<table wicket:id='table'><tbody><tr><td>Ada</td></tr></tbody></table>"));
        assertThat(html()).doesNotContain("data-size=", "data-sticky=");
    }
    @Test void tablesAndPaginationRejectTheWrongNativeElement() {
        assertThatThrownBy(() -> tester.startComponentInPage(new SwTable("table"), Markup.of("<div wicket:id='table'></div>")))
                .hasStackTraceContaining("requires <table>");
    }
    @Test void pageWindowsShowFirstLastSiblingsAndSingleMissingPagesWithoutHugeLists() {
        assertThat(PageWindow.pages(4, 27, 1)).containsExactly(0L, -1L, 3L, 4L, 5L, -1L, 26L);
        assertThat(PageWindow.pages(2, 27, 1)).containsExactly(0L, 1L, 2L, 3L, -1L, 26L);
        assertThat(PageWindow.pages(3, 7, 1)).containsExactly(0L, 1L, 2L, 3L, 4L, 5L, 6L);
        assertThat(PageWindow.pages(0, 0, 1)).containsExactly(0L);
        assertThat(PageWindow.pages(9, 27, 0)).containsExactly(0L, -1L, 9L, -1L, 26L);
        assertThat(PageWindow.pages(Long.MAX_VALUE, Long.MAX_VALUE, 2)).containsExactly(0L, -1L, Long.MAX_VALUE - 3, Long.MAX_VALUE - 2, Long.MAX_VALUE - 1);
        assertThatIllegalArgumentException().isThrownBy(() -> PageWindow.pages(0, 10, -1));
        assertThatIllegalArgumentException().isThrownBy(() -> PageWindow.pages(0, 10, 11));
    }
    @Test void genericPaginationKeepsOatsCallerOwnedMenuItemsAndFactoryShape() {
        var pages = SwPagination.of("pages", Model.ofList(List.of(1, 2)), (item, number) -> {
            var link = new ExternalLink("link", "/people?page=" + (number - 1));
            link.add(new Label("label", number)); link.add(Sw.Behaviors.paginationLink().setCurrent(number == 1)); item.add(link);
        });
        tester.startComponentInPage(pages, Markup.of("<nav wicket:id='pages'><ul wicket:id='menu'><li wicket:id='items'><a wicket:id='link'><span wicket:id='label'></span></a></li></ul></nav>"));
        assertThat(html()).contains("sw-pagination", "sw-pagination-content", "aria-label=\"Pagination\"", "aria-current=\"page\"", "data-variant=\"outline\"");
    }
    @Test void automaticPaginationHasNativeNamedDisabledCurrentAndEllipsisSemantics() {
        var pageable = new Pages(); pageable.current = 4;
        var pager = new SwPagingNavigator("pager", pageable).setLabel("People pages").setPageUrl(page -> "/people?q=ada&page=" + page);
        tester.startComponentInPage(pager);
        assertThat(html()).contains("<nav", "aria-label=\"People pages\"", "aria-label=\"Go to next page\"", "More pages", "aria-current=\"page\"", "href=\"/people?q=ada&amp;page=5\"");
        assertThat(TagTester.createTagsByAttribute(html(), "class", "sw-pagination-ellipsis", false)).hasSize(2);
        pageable.current = 0; tester.startComponentInPage(pager);
        var previous = TagTester.createTagByAttribute(html(), "aria-label", "Go to previous page");
        assertThat(previous.getAttribute("href")).isNull(); assertThat(previous.getAttribute("aria-disabled")).isEqualTo("true");
        assertThat(previous.getAttribute("role")).isEqualTo("link"); assertThat(previous.getAttribute("tabindex")).isEqualTo("-1");
        pageable.count = 0; tester.startComponentInPage(pager);
        assertThat(TagTester.createTagsByAttribute(html(), "aria-disabled", "true", false)).hasSize(2);
        assertThat(TagTester.createTagsByAttribute(html(), "aria-current", "page", false)).hasSize(1);
    }
    @Test void ajaxPaginationChangesThePageAndReplacesItsOwnNavigation() {
        var pageable = new Pages(); var pager = new SwPagingNavigator("pager", pageable);
        tester.startComponentInPage(pager);
        int nextIndex = PageWindow.pages(0, pageable.getPageCount(), 1).size() + 1;
        tester.executeAjaxEvent("pager:navigation:menu:items:" + nextIndex + ":link", "click");
        assertThat(pageable.current).isEqualTo(1); tester.assertComponentOnAjaxResponse(pager);
        assertThat(html()).contains("Go to next page", "aria-current=\"page\"");
    }
    @Test void nativePagingFallbackAlsoChangesThePageWithoutAjax() {
        var pageable = new Pages(); var pager = new SwPagingNavigator("pager", pageable); tester.startComponentInPage(pager);
        int nextIndex = PageWindow.pages(0, pageable.getPageCount(), 1).size() + 1;
        tester.clickLink("pager:navigation:menu:items:" + nextIndex + ":link", false);
        assertThat(pageable.current).isEqualTo(1);
    }
    @Test void summaryUsesLocaleFormatsEmptyAndPageVariantsAndDoesNotAnnounceEveryRender() {
        var pageable = new Pages(); pageable.count = 12345; pageable.current = 100;
        tester.getSession().setLocale(Locale.US);
        var summary = new SwPaginationSummary("summary", pageable); tester.startComponentInPage(summary);
        assertThat(html()).contains("1,001–1,010 of 12,345").doesNotContain("aria-live", "role=\"status\"");
        tester.getSession().setLocale(Locale.GERMANY); tester.startComponentInPage(summary);
        assertThat(html()).contains("1.001–1.010 of 12.345");
        pageable.count = 0; tester.startComponentInPage(summary); assertThat(html()).contains("0 of 0");
        summary.setFormat(SwPaginationSummary.Format.PAGE); tester.startComponentInPage(summary); assertThat(html()).contains("Page 1 of 1");
    }
    @Test void pageSizeIncludesTheCurrentChoiceAndUsesNativeMenuRadioLinks() {
        var pageable = new Pages(); pageable.size = 7;
        var sizes = new SwPaginationSize("size", pageable).setSizes(10, 20).setSizeUrl(size -> "/people?q=ada&size=" + size);
        tester.startComponentInPage(sizes);
        assertThat(html()).contains("aria-label=\"Rows per page 7\"", "role=\"menu\"", "role=\"menuitemradio\"", "aria-checked=\"true\"", "href=\"/people?q=ada&amp;size=20\"");
        assertThat(TagTester.createTagsByAttribute(html(), "role", "menuitemradio", false)).hasSize(3);
        assertThatIllegalArgumentException().isThrownBy(() -> sizes.setSizes(0, 10));
        assertThatIllegalArgumentException().isThrownBy(() -> sizes.setSizes());
    }
    @Test void ajaxPageSizeResetsToTheFirstPage() {
        var pageable = new Pages(); pageable.current = 4;
        var sizes = new SwPaginationSize("size", pageable).setSizes(10, 20); tester.startComponentInPage(sizes);
        tester.executeAjaxEvent("size:menu:sizes:1:link", "click");
        assertThat(pageable.size).isEqualTo(20); assertThat(pageable.current).isZero(); tester.assertComponentOnAjaxResponse(sizes);
    }
    @Test void queryUpdatesPreserveRepeatedFiltersAndIndexedParametersWithoutMutatingTheInput() {
        var original = new PageParameters().set(0, "team").add("q", "Ada lé").add("tag", "green").add("tag", "blue")
                .set("page", 4).set("sort", "name,desc").set("size", 10);
        var query = new TableQuery(original);
        assertThat(query.getPage()).isEqualTo(4); assertThat(query.getSortProperty()).isEqualTo("name"); assertThat(query.isAscending()).isFalse();
        assertThat(query.page(2).get("page").toLong()).isEqualTo(2);
        var sorted = query.sort("email", true); assertThat(sorted.get("page").isNull()).isTrue();
        assertThat(sorted.get("sort").toString()).isEqualTo("email,asc"); assertThat(sorted.getValues("tag")).hasSize(2);
        assertThat(sorted.get(0).toString()).isEqualTo("team"); assertThat(sorted.get("q").toString()).isEqualTo("Ada lé");
        assertThat(query.size(20).get("page").isNull()).isTrue(); assertThat(query.size(20).get("size").toLong()).isEqualTo(20);
        assertThat(original.get("page").toLong()).isEqualTo(4); assertThat(original.get("sort").toString()).isEqualTo("name,desc");
        query.getParameters().set("q", "changed"); assertThat(query.getParameters().get("q").toString()).isEqualTo("Ada lé");
    }
    @Test void queryClampsNumbersRejectsBadSortDirectionsAndSeparatesTwoTables() {
        var query = new TableQuery(new PageParameters().set("page", "-2").set("size", Long.MAX_VALUE).set("sort", "name,sideways").set("members_page", 3)).qualified("members");
        assertThat(query.getPage()).isEqualTo(3); assertThat(query.page(1).get("page").toString()).isEqualTo("-2");
        var root = new TableQuery(query.getParameters()); assertThat(root.getPage()).isZero(); assertThat(root.getSize(10, 100)).isEqualTo(100); assertThat(root.getSortProperty()).isNull();
        assertThat(new TableQuery(new PageParameters().set("page", "999999999999999999999").set("size", "wrong")).getPage()).isZero();
        assertThat(new TableQuery(new PageParameters().set("size", "wrong")).getSize(10, 100)).isEqualTo(10);
        assertThatIllegalArgumentException().isThrownBy(() -> root.page(-1)); assertThatIllegalArgumentException().isThrownBy(() -> root.size(0));
        assertThatIllegalArgumentException().isThrownBy(() -> new TableQuery(new PageParameters(), "page", "page", "sort"));
    }
    @Test void searchHiddenInputsKeepRepeatedValuesEscapeMarkupAndResetOnlyTheirOwnPage() {
        var query = new TableQuery(new PageParameters().add("q", "old").add("tag", "<green>").add("tag", "\"blue\"").set("page", 5).set("other_page", 2));
        tester.startComponentInPage(new SwQueryParameters("params", query.forSearch("q")));
        assertThat(html()).contains("name=\"tag\"", "value=\"&lt;green&gt;\"", "value=\"&quot;blue&quot;\"", "name=\"other_page\"")
                .doesNotContain("name=\"q\"", "name=\"page\"", "<green>", "style=");
        assertThat(java.util.regex.Pattern.compile("name=\"tag\"").matcher(html()).results().count()).isEqualTo(2);
    }
    @Test void namedPaginationModelsDetach() {
        var detaches = new AtomicInteger();
        IModel<String> label = new IModel<>() {
            @Override public String getObject() { return "People pages"; }
            @Override public void detach() { detaches.incrementAndGet(); }
        };
        tester.startComponentInPage(new SwPagingNavigator("pager", new Pages()).setLabel(label));
        assertThat(detaches.get()).isPositive();
    }
}
