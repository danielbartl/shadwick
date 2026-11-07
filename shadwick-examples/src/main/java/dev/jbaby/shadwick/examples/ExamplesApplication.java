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

        mount(new StudioPageMapper());
        mountPage("/buttons", ButtonPage.class);
        mountPage("/form", FormPage.class);
        mountPage("/inputs", InputPage.class);
        mountPage("/selections", SelectPage.class);
        mountPage("/custom-select", CustomSelectPage.class);
        mountPage("/combobox", ComboboxPage.class);
        mountPage("/calendar", CalendarPage.class);
        mountPage("/month-view", MonthViewPage.class);
        mountPage("/charts", ChartPage.class);
        mountPage("/dates", DateTimePage.class);
        mountPage("/groups", GroupPage.class);
        mountPage("/uploads", UploadPage.class);
        mountPage("/choices", ChoicePage.class);
        mountPage("/dialog", DialogPage.class);
        mountPage("/overlays", OverlayPage.class);
        mountPage("/popups", PopupPage.class);
        mountPage("/menus", MenuPage.class);
        mountPage("/toasts", ToastPage.class);
        mountPage("/shell", ShellPage.class);
        mountPage("/sidebar", SidebarPage.class);
        mountPage("/user-menu", UserMenuPage.class);
        mountPage("/theme", ThemePage.class);
        mountPage("/layouts", AppLayoutPage.class);
        mountPage("/header-layout", HeaderLayoutPage.class);
        mountPage("/disclosure", DisclosurePage.class);
        mountPage("/attachments", AttachmentPage.class);
        mountPage("/data", DataTablePage.class);
        mountPage("/advanced-tables", AdvancedTablePage.class);
        mountPage("/tables", TablePage.class);
        mountPage("/pagination", PaginationPage.class);
        mountPage("/invoice", InvoicePage.class);
        mountPage("/demo-sign-in", UserSignInPage.class);
        mountPage("/display", DisplayPage.class);
        mountPage("/status-controls", StatusControlsPage.class);
        mountPage("/feedback", FeedbackPage.class);
        mountPage("/internationalization", InternationalizationPage.class);

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
