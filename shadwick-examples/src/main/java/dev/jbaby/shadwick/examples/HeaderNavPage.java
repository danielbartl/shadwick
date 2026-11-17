package dev.jbaby.shadwick.examples;

import org.apache.wicket.request.mapper.parameter.PageParameters;

import java.util.Map;

/** A page in the "Navigation in the header" layout: the view parameter picks which of the menu's pages it shows. */
public class HeaderNavPage extends HeaderNavLayout {

    private static final Map<String, String> TITLES = Map.of(
            "overview", "Overview", "projects", "Projects", "team", "Team", "settings", "Settings");

    public HeaderNavPage(PageParameters parameters) {
        super(parameters);
    }

    @Override
    protected String title() {
        return TITLES.getOrDefault(getPageParameters().get("view").toString("overview"), "Overview");
    }
}
