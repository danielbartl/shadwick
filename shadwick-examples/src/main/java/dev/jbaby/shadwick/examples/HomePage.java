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
        add(new BookmarkablePageLink<>("datesLink", DateTimePage.class));
        add(new BookmarkablePageLink<>("groupsLink", GroupPage.class));
        add(new BookmarkablePageLink<>("uploadsLink", UploadPage.class));
        add(new BookmarkablePageLink<>("dialogLink", DialogPage.class));
        add(new BookmarkablePageLink<>("overlaysLink", OverlayPage.class));
        add(new BookmarkablePageLink<>("popupsLink", PopupPage.class));
        add(new BookmarkablePageLink<>("menusLink", MenuPage.class));
        add(new BookmarkablePageLink<>("toastsLink", ToastPage.class));
        add(new BookmarkablePageLink<>("displayLink", DisplayPage.class));
        add(new BookmarkablePageLink<>("feedbackLink", FeedbackPage.class));
    }

    @Override
    protected String getTitle() {
        return "Home";
    }
}
