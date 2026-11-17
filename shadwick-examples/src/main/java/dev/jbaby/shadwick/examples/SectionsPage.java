package dev.jbaby.shadwick.examples;

import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.request.mapper.parameter.PageParameters;

import java.util.Map;

/** A page in the "Grouped by section" layout: the view parameter picks which of the menu's pages it shows. */
public class SectionsPage extends SectionsLayout {

    private static final Map<String, String> TITLES = Map.of(
            "introduction", "Introduction", "installation", "Installation", "structure", "Project structure",
            "routing", "Routing", "forms", "Forms", "data", "Data", "components", "Components",
            "configuration", "Configuration");

    public SectionsPage(PageParameters parameters) {
        super(parameters);
    }

    @Override
    protected String title() {
        String query = getPageParameters().get("q").toString("");
        if (!query.isBlank()) return "Results for “" + query + "”";
        return TITLES.getOrDefault(getPageParameters().get("view").toString("introduction"), "Introduction");
    }

    @Override
    protected void onInitialize() {
        super.onInitialize();
        add(new Label("lead", "A short guide to " + title().toLowerCase() + ", with an example to start from."));
    }
}
