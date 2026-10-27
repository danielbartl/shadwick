package dev.jbaby.shadwick;

import java.io.Serializable;

/**
 * Keeps a skin other than the app-wide one, e.g. for a showcase that compares skins. Asked on every request, so it
 * can keep the skin per user, per session or per request.
 *
 * @see ShadwickSettings#setSkinStore(SkinStore)
 */
public interface SkinStore extends Serializable {

    /** The skin for the current request, or {@code null} for the app-wide one ({@link ShadwickSettings#getSkin()}). */
    Skin load();

    /** Stores the skin for the current user; {@code null} goes back to the app-wide one. */
    void save(Skin skin);
}
