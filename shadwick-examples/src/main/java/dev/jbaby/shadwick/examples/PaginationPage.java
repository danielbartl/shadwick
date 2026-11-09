package dev.jbaby.shadwick.examples;

import dev.jbaby.shadwick.components.*;

import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.list.ListItem;
import org.apache.wicket.markup.html.list.ListView;
import org.apache.wicket.markup.html.navigation.paging.IPageableItems;
import org.apache.wicket.request.mapper.parameter.PageParameters;

import java.util.stream.LongStream;

/** Independent pageable controls, real URLs and two qualified lists on one page. */
public class PaginationPage extends BasePage {

    private static class Pages implements IPageableItems {

        private long page, size;
        private final long count;

        Pages(TableQuery query, long count, long size) {
            this.count = count;
            this.size = query.getSize(size, 100);
            page = Math.min(query.getPage(), getPageCount() - 1);
        }

        @Override
        public long getCurrentPage() {
            return page;
        }

        @Override
        public void setCurrentPage(long page) {
            this.page = page;
        }

        @Override
        public long getPageCount() {
            return Math.max(1, 1 + (count - 1) / size);
        }

        @Override
        public long getItemCount() {
            return count;
        }

        @Override
        public long getItemsPerPage() {
            return size;
        }

        @Override
        public void setItemsPerPage(long size) {
            this.size = size;
        }
    }

    public PaginationPage(PageParameters parameters) {
        super(parameters);
    }

    @Override
    protected void onInitialize() {
        super.onInitialize();
        PageParameters parameters = getPageParameters();
        var query = new TableQuery(parameters);
        var pages = new Pages(query, 275, 10);
        add(new SwPaginationSummary("summary", pages));
        add(
                new SwPaginationSize("size", pages)
                        .setSizes(5, 10, 20, 50)
                        .setSizeUrl(size -> urlFor(PaginationPage.class, query.size(size))));
        add(
                new SwPagingNavigator("pages", pages)
                        .setLabel("Project pages")
                        .setPageUrl(page -> urlFor(PaginationPage.class, query.page(page))));
        add(
                new ListView<Long>(
                        "projects",
                        LongStream.range(
                                        pages.page * pages.size,
                                        Math.min(pages.count, (pages.page + 1) * pages.size))
                                .boxed()
                                .toList()) {

                    @Override
                    protected void populateItem(ListItem<Long> item) {
                        item.add(
                                new Label("name", "Project " + (item.getModelObject() + 1)),
                                new Label(
                                        "number",
                                        String.format(
                                                java.util.Locale.ROOT,
                                                "%03d",
                                                item.getModelObject() + 1)));
                    }
                });
        var archiveQuery = query.qualified("archive");
        var archive = new Pages(archiveQuery, 120, 5);
        add(
                new SwPaginationSummary("archiveSummary", archive)
                        .setFormat(SwPaginationSummary.Format.PAGE));
        add(
                new SwPagingNavigator("archive", archive)
                        .setSiblings(0)
                        .setLabel("Archive pages")
                        .setPageUrl(page -> urlFor(PaginationPage.class, archiveQuery.page(page))));
    }

    @Override
    protected String getTitle() {
        return "Pagination";
    }
}
