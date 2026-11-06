package dev.jbaby.shadwick.components.form;

import org.apache.wicket.Component;
import org.apache.wicket.feedback.FeedbackMessage;
import org.apache.wicket.feedback.IFeedbackMessageFilter;

/**
 * Accepts feedback that Shadwick form fields and {@link ShowsErrorsInline} components don't already show inline. Each
 * {@link BaseSwField} shows the first error reported by its wrapped form component, so
 * a page-level feedback panel or toast would only repeat it; the wrapped field owns that message. Used by default by
 * {@code SwFeedbackPanel} and {@code FeedbackToastsBehavior}.
 */
public class NotShownInlineFilter implements IFeedbackMessageFilter {

    @Override
    public boolean accept(FeedbackMessage message) {
        Component reporter = message.getReporter();
        if (reporter == null || !message.isError()) {
            return true;
        }
        if (reporter.getFeedbackMessages().first(FeedbackMessage.ERROR) != message) {
            return true;
        }
        BaseSwField<?, ?, ?> field = reporter.findParent(BaseSwField.class);
        if (field != null && field.getInput() == reporter) {
            return false;
        }
        for (Component parent = reporter.getParent(); parent != null; parent = parent.getParent())
            if (parent instanceof ShowsErrorsInline inline && inline.showsErrorsOf(reporter)) return false;
        return true;
    }
}
