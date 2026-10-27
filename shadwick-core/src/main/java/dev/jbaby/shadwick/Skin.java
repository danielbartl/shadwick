package dev.jbaby.shadwick;

import java.util.Locale;

/**
 * An app-wide look: the same markup and tokens, a different geometry, spacing, type size and shadow. Chosen at
 * install with {@link ShadwickSettings#setSkin(Skin)}:
 *
 * <pre>
 * Shadwick.install(this).setSkin(Skin.LYRA);
 * </pre>
 */
public enum Skin {

    /** shadcn/ui's classic look, the default. */
    VEGA,

    /** Boxy and sharp: square corners, small type, a thin focus ring. */
    LYRA;

    /** The name used in the stylesheet's file name, e.g. {@code vega} for {@code shadwick-vega.css}. */
    public String id() {
        return name().toLowerCase(Locale.ROOT);
    }
}
