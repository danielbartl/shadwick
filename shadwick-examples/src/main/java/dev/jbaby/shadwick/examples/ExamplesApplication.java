package dev.jbaby.shadwick.examples;

import dev.jbaby.shadwick.Shadwick;
import org.apache.wicket.Page;
import org.apache.wicket.protocol.http.WebApplication;

/** The showcase's Wicket application, also what the page tests run. */
public class ExamplesApplication extends WebApplication {

    @Override
    public Class<? extends Page> getHomePage() {
        return HomePage.class;
    }

    @Override
    protected void init() {
        super.init();

        mountPage("/buttons", ButtonPage.class);
        mountPage("/form", FormPage.class);
        mountPage("/dialog", DialogPage.class);

        // The markup a deployed application sends, also in development mode: Wicket's own
        // tags would be the children of the pages' grid layouts
        getMarkupSettings().setStripWicketTags(true);

        // The skin is vega for every page, unless a visitor picked another one with the
        // skin switcher: kept in the session, so visitors don't change each other's.
        // Wicket's strict, nonce-based Content Security Policy stays as it is.
        Shadwick.install(this)
                .setSkinStore(new SessionSkinStore());
    }
}
