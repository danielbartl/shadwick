package dev.jbaby.shadwick.components.form;

import org.apache.wicket.Component;

/** A component that renders the first error of its input inline, avoiding duplicate page feedback. */
public interface ShowsErrorsInline {
    boolean showsErrorsOf(Component reporter);
}
