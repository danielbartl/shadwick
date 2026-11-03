package dev.jbaby.shadwick.examples;

import org.apache.wicket.markup.html.link.BookmarkablePageLink;
import org.apache.wicket.request.mapper.parameter.PageParameters;

public class HomePage extends BasePage {

    public HomePage(PageParameters parameters) {
        super(parameters);
        add(new BookmarkablePageLink<>("buttonsLink", ButtonPage.class));
        add(new BookmarkablePageLink<>("formLink", FormPage.class));
        add(new BookmarkablePageLink<>("choicesLink", ChoicePage.class));
        add(new BookmarkablePageLink<>("dialogLink", DialogPage.class));
        add(new BookmarkablePageLink<>("displayLink", DisplayPage.class));
        add(new BookmarkablePageLink<>("feedbackLink", FeedbackPage.class));
    }

    @Override
    protected String getTitle() {
        return "Home";
    }
}
