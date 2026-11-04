package dev.jbaby.shadwick.components;

import org.apache.wicket.Page;
import org.apache.wicket.request.mapper.parameter.PageParameters;

/** The route-matching rule shared by native sidebar links and their disclosure groups. */
final class CurrentPage {

    private CurrentPage() {
    }

    /** The same page class and, if the link has parameters, the same parameters. */
    static boolean isLinkedBy(Page page, Class<? extends Page> pageClass, PageParameters parameters) {
        return page.getClass().equals(pageClass)
                && (parameters == null || parameters.equals(page.getPageParameters()));
    }
}
