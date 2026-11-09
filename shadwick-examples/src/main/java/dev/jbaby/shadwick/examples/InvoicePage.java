package dev.jbaby.shadwick.examples;

import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.link.BookmarkablePageLink;
import org.apache.wicket.request.mapper.parameter.PageParameters;

/**
 * A normal bookmarkable row link, keeping the table's filter/sort/page parameters in its return
 * link.
 */
public class InvoicePage extends BasePage {

    public InvoicePage(PageParameters parameters) {
        super(parameters);
    }

    @Override
    protected void onInitialize() {
        super.onInitialize();
        PageParameters parameters = getPageParameters();
        long id;
        try {
            id = parameters.get("invoice").toLong(1);
        } catch (RuntimeException invalid) {
            id = 1;
        }
        var row = DataTablePage.invoice(id);
        add(new Label("number", row == null ? "Invoice unavailable" : row.number()));
        add(new Label("customer", row == null ? "This invoice was removed." : row.customer()));
        add(new Label("email", row == null ? "" : row.email()));
        add(new Label("amount", row == null ? "" : DataTablePage.money(row.cents(), getLocale())));
        var back = new PageParameters(parameters);
        back.remove("invoice");
        add(new BookmarkablePageLink<>("back", DataTablePage.class, back));
    }

    @Override
    protected String getTitle() {
        return "Invoice details";
    }
}
