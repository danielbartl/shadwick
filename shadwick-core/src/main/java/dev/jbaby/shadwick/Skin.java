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
    LYRA,

    /** Compact controls, generous radii and muted card/dialog footers. */
    NOVA,

    /** Pill-shaped controls and generous spacing. */
    MAIA,

    /** Dense interfaces with 28px controls and small type. */
    MIRA,

    /** Soft rounded shapes, borderless fields and luminous shadows. */
    LUMA,

    /** Luma's soft surfaces with smaller controls and tighter spacing. */
    RHEA,

    /** Editorial geometry, uppercase labels and underlined fields. */
    SERA,

    /** Hand-drawn wireframes with locally bundled Balsamiq Sans. */
    SKETCH;

    /** The name used in the stylesheet's file name, e.g. {@code vega} for {@code shadwick-vega.css}. */
    public String id() {
        return name().toLowerCase(Locale.ROOT);
    }
}
