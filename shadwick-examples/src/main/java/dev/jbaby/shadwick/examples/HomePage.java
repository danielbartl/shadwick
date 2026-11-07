package dev.jbaby.shadwick.examples;

import org.apache.wicket.AttributeModifier;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.link.BookmarkablePageLink;
import org.apache.wicket.markup.html.list.ListItem;
import org.apache.wicket.markup.html.list.ListView;
import org.apache.wicket.request.mapper.parameter.PageParameters;

/** The component catalog; Clover Studio is the application's homepage. */
public class HomePage extends BasePage {
    public HomePage(PageParameters parameters) {
        super(parameters);
        add(new BookmarkablePageLink<>("studioLink", StudioPage.class));
        add(new Label("exampleCount", CatalogPages.ALL.size()), new Label("groupCount", CatalogPages.GROUPS.size()));
        add(new ListView<>("groups", CatalogPages.GROUPS) {
            @Override protected void populateItem(ListItem<CatalogPages.Group> group) {
                group.add(new Label("heading", group.getModelObject().title()), new Label("count", group.getModelObject().examples().size()));
                group.add(new ListView<>("examples", group.getModelObject().examples()) {
                    @Override protected void populateItem(ListItem<CatalogPages.Example> item) {
                        var example = item.getModelObject();
                        item.add(AttributeModifier.replace("data-catalog-search", example.title() + " " + example.description()));
                        var link = new BookmarkablePageLink<Void>("link", example.page(), CatalogPages.preferences(parameters));
                        link.add(new Label("name", example.title()), new Label("description", example.description()));
                        item.add(link);
                    }
                });
            }
        });
    }
    @Override protected String getTitle() { return "Component catalog"; }
}
