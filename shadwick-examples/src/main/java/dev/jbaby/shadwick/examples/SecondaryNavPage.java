package dev.jbaby.shadwick.examples;

import org.apache.wicket.request.mapper.parameter.PageParameters;

import java.util.Map;

/** A page in the "Inset with secondary navigation" layout: the view parameter picks which page it shows. */
public class SecondaryNavPage extends SecondaryNavLayout {

    private static final Map<String, String> TITLES = Map.ofEntries(
            Map.entry("inbox", "Inbox"), Map.entry("calendar", "Calendar"), Map.entry("week", "This week"),
            Map.entry("month", "This month"), Map.entry("reports", "Reports"), Map.entry("sales", "Sales"),
            Map.entry("visits", "Visits"), Map.entry("harbor", "Harbor survey"), Map.entry("orchard", "North orchard"),
            Map.entry("support", "Support"), Map.entry("feedback", "Feedback"), Map.entry("account", "Account"));

    public SecondaryNavPage(PageParameters parameters) {
        super(parameters);
    }

    @Override
    protected String title() {
        return TITLES.getOrDefault(getPageParameters().get("view").toString("inbox"), "Inbox");
    }
}
