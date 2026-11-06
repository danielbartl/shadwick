package dev.jbaby.shadwick.examples;

import org.apache.wicket.markup.html.link.BookmarkablePageLink;
import org.apache.wicket.request.mapper.parameter.PageParameters;

public class HomePage extends BasePage {

    public HomePage(PageParameters parameters) {
        super(parameters);
        add(new BookmarkablePageLink<>("buttonsLink", ButtonPage.class));
        add(new BookmarkablePageLink<>("formLink", FormPage.class));
        add(new BookmarkablePageLink<>("inputsLink", InputPage.class));
        add(new BookmarkablePageLink<>("choicesLink", ChoicePage.class));
        add(new BookmarkablePageLink<>("selectionsLink", SelectPage.class));
        add(new BookmarkablePageLink<>("customSelectLink", CustomSelectPage.class));
        add(new BookmarkablePageLink<>("comboboxLink", ComboboxPage.class));
        add(new BookmarkablePageLink<>("calendarLink", CalendarPage.class));
        add(new BookmarkablePageLink<>("monthViewLink", MonthViewPage.class));
        add(new BookmarkablePageLink<>("chartsLink", ChartPage.class));
        add(new BookmarkablePageLink<>("datesLink", DateTimePage.class));
        add(new BookmarkablePageLink<>("groupsLink", GroupPage.class));
        add(new BookmarkablePageLink<>("uploadsLink", UploadPage.class));
        add(new BookmarkablePageLink<>("dialogLink", DialogPage.class));
        add(new BookmarkablePageLink<>("overlaysLink", OverlayPage.class));
        add(new BookmarkablePageLink<>("popupsLink", PopupPage.class));
        add(new BookmarkablePageLink<>("menusLink", MenuPage.class));
        add(new BookmarkablePageLink<>("toastsLink", ToastPage.class));
        add(new BookmarkablePageLink<>("shellLink", ShellPage.class));
        add(new BookmarkablePageLink<>("sidebarLink", SidebarPage.class));
        add(new BookmarkablePageLink<>("userMenuLink", UserMenuPage.class));
        add(new BookmarkablePageLink<>("themeLink", ThemePage.class));
        add(new BookmarkablePageLink<>("layoutsLink", AppLayoutPage.class));
        add(new BookmarkablePageLink<>("disclosureLink", DisclosurePage.class));
        add(new BookmarkablePageLink<>("attachmentLink", AttachmentPage.class));
        add(new BookmarkablePageLink<>("dataLink", DataTablePage.class));
        add(new BookmarkablePageLink<>("advancedTablesLink", AdvancedTablePage.class));
        add(new BookmarkablePageLink<>("tableLink", TablePage.class));
        add(new BookmarkablePageLink<>("paginationLink", PaginationPage.class));
        add(new BookmarkablePageLink<>("displayLink", DisplayPage.class));
        add(new BookmarkablePageLink<>("statusControlsLink", StatusControlsPage.class));
        add(new BookmarkablePageLink<>("feedbackLink", FeedbackPage.class));
        add(new BookmarkablePageLink<>("internationalizationLink", InternationalizationPage.class));
    }

    @Override
    protected String getTitle() {
        return "Home";
    }
}
