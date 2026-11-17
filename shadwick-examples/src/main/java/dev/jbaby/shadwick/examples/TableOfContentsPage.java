package dev.jbaby.shadwick.examples;

import org.apache.wicket.request.mapper.parameter.PageParameters;

import java.util.List;

/** A long page in the "On the end, with a table of contents" layout. */
public class TableOfContentsPage extends TableOfContentsLayout {

    public TableOfContentsPage(PageParameters parameters) {
        super(parameters);
    }

    @Override
    protected String title() {
        return "Planting an orchard";
    }

    @Override
    protected List<Section> sections() {
        return List.of(new Section("site", "Choosing a site"), new Section("soil", "Preparing the soil"),
                new Section("trees", "Picking trees"), new Section("planting", "Planting"),
                new Section("care", "The first year"));
    }
}
