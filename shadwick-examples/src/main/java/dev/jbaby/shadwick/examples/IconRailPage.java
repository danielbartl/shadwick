package dev.jbaby.shadwick.examples;

import dev.jbaby.shadwick.components.MenuItem;

import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.request.mapper.parameter.PageParameters;

import java.util.List;
import java.util.Map;

/** A page in the "Collapses to icons" layout: the view parameter picks which of the menu's pages it shows. */
public class IconRailPage extends IconRailLayout {

    private static final Map<String, String> TITLES = Map.of(
            "dashboard", "Dashboard", "revenue", "Revenue", "customers", "Customers", "general", "General",
            "billing", "Billing", "website", "Website relaunch", "mobile", "Mobile app");

    public IconRailPage(PageParameters parameters) {
        super(parameters);
    }

    private String view() {
        String view = getPageParameters().get("view").toString("dashboard");
        return TITLES.containsKey(view) ? view : "dashboard";
    }

    @Override
    protected String title() {
        return TITLES.get(view());
    }

    @Override
    protected List<MenuItem> breadcrumb() {
        return switch (view()) {
            case "revenue", "customers" -> List.of(page("Reports", "revenue"));
            case "general", "billing" -> List.of(page("Settings", "general"));
            case "website", "mobile" -> List.of(page("Projects", "website"));
            default -> List.of();
        };
    }

    @Override
    protected void onInitialize() {
        super.onInitialize();
        add(new Label("summary", "Everything for " + title().toLowerCase() + " in one place."));
    }
}
