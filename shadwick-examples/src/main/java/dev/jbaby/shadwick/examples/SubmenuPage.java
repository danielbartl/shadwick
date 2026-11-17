package dev.jbaby.shadwick.examples;

import org.apache.wicket.request.mapper.parameter.PageParameters;

import java.util.Map;

/** A page in the "Collapsible sub-menus" layout: the view parameter picks which of the menu's pages it shows. */
public class SubmenuPage extends SubmenuLayout {

    private static final Map<String, String> TITLES = Map.of(
            "installation", "Installation", "structure", "Project structure", "routing", "Routing",
            "data", "Data fetching", "rendering", "Rendering", "caching", "Caching", "components", "Components",
            "functions", "Functions", "accessibility", "Accessibility", "browsers", "Supported browsers");

    public SubmenuPage(PageParameters parameters) {
        super(parameters);
    }

    @Override
    protected String title() {
        return TITLES.getOrDefault(getPageParameters().get("view").toString("installation"), "Installation");
    }
}
