package dev.jbaby.shadwick.examples;

import org.apache.wicket.AttributeModifier;
import org.apache.wicket.Page;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.link.BookmarkablePageLink;
import org.apache.wicket.markup.html.list.ListItem;
import org.apache.wicket.markup.html.list.ListView;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.request.mapper.parameter.PageParameters;

final class CatalogNavigation extends Panel {
    CatalogNavigation(String id, Class<? extends Page> current, PageParameters parameters) {
        super(id);
        var overview = new BookmarkablePageLink<Void>("overview", HomePage.class, CatalogPages.preferences(parameters));
        if (current == HomePage.class) overview.add(AttributeModifier.replace("aria-current", "page"));
        add(overview);
        add(new ListView<>("groups", CatalogPages.GROUPS) {
            @Override protected void populateItem(ListItem<CatalogPages.Group> group) {
                group.add(new Label("groupTitle", group.getModelObject().title()));
                group.add(new ListView<>("examples", group.getModelObject().examples()) {
                    @Override protected void populateItem(ListItem<CatalogPages.Example> item) {
                        var example = item.getModelObject();
                        item.add(AttributeModifier.replace("data-catalog-search", example.title() + " " + example.description()));
                        var link = new BookmarkablePageLink<Void>("link", example.page(), CatalogPages.preferences(parameters));
                        link.add(new Label("label", example.title()));
                        if (current == example.page()) link.add(AttributeModifier.replace("aria-current", "page"));
                        item.add(link);
                    }
                });
            }
        });
    }
}
