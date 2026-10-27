package dev.jbaby.shadwick.examples;

import org.apache.wicket.MetaDataKey;
import org.apache.wicket.Session;

/** Whether a visitor picked dark mode, kept in their session. Shadwick's own theme support comes later. */
final class Appearance {

    private static final MetaDataKey<Boolean> DARK = new MetaDataKey<>() {};

    private Appearance() {
    }

    static boolean isDark() {
        return Session.exists() && Boolean.TRUE.equals(Session.get().getMetaData(DARK));
    }

    static void setDark(boolean dark) {
        Session session = Session.get();
        session.bind();
        session.setMetaData(DARK, dark);
    }
}
