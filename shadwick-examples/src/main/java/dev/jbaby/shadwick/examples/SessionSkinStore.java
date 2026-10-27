package dev.jbaby.shadwick.examples;

import dev.jbaby.shadwick.Skin;
import dev.jbaby.shadwick.SkinStore;
import org.apache.wicket.MetaDataKey;
import org.apache.wicket.Session;

/** Keeps the skin a visitor picked in their session; until then, they see the app-wide one. */
public class SessionSkinStore implements SkinStore {

    private static final MetaDataKey<Skin> SKIN = new MetaDataKey<>() {};

    @Override
    public Skin load() {
        return Session.exists() ? Session.get().getMetaData(SKIN) : null;
    }

    @Override
    public void save(Skin skin) {
        Session session = Session.get();
        session.bind();
        session.setMetaData(SKIN, skin);
    }
}
