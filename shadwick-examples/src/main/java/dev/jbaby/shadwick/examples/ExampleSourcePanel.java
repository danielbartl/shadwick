package dev.jbaby.shadwick.examples;

import org.apache.wicket.AttributeModifier;
import org.apache.wicket.Page;
import org.apache.wicket.markup.head.IHeaderResponse;
import org.apache.wicket.markup.head.JavaScriptHeaderItem;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.link.ExternalLink;
import org.apache.wicket.markup.html.list.ListItem;
import org.apache.wicket.markup.html.list.ListView;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.model.LoadableDetachableModel;
import org.apache.wicket.request.resource.JavaScriptResourceReference;

final class ExampleSourcePanel extends Panel {
    static final JavaScriptResourceReference SCRIPT = new JavaScriptResourceReference(BasePage.class, "catalog.js");
    ExampleSourcePanel(String id, Class<? extends Page> page) {
        super(id);
        add(new ExternalLink("guide", CatalogPages.guide(page)));
        add(new ListView<>("files", ExampleSources.files(page)) {
            @Override protected void populateItem(ListItem<ExampleSources.File> item) {
                var file = item.getModelObject();
                var disclosure = new org.apache.wicket.markup.html.WebMarkupContainer("disclosure");
                if (item.getIndex() == 0) disclosure.add(AttributeModifier.replace("open", ""));
                disclosure.add(new Label("filename", file.name()), new Label("language", file.language()));
                var code = new Label("code", LoadableDetachableModel.of(() -> ExampleSources.read(file.name())));
                code.setEscapeModelStrings(true);
                disclosure.add(new ExternalLink("github", file.github())
                        .add(AttributeModifier.replace("aria-label", "View file on GitHub: " + file.name())));
                disclosure.add(new org.apache.wicket.markup.html.WebMarkupContainer("copy")
                        .add(AttributeModifier.replace("aria-label", "Copy code: " + file.name())));
                var scroll = new org.apache.wicket.markup.html.WebMarkupContainer("scroll");
                scroll.add(AttributeModifier.replace("aria-label", file.name() + " source code"));
                scroll.add(code); disclosure.add(scroll);
                item.add(disclosure);
            }
        });
    }
    @Override public void renderHead(IHeaderResponse response) {
        super.renderHead(response);
        response.render(JavaScriptHeaderItem.forReference(SCRIPT));
    }
}
