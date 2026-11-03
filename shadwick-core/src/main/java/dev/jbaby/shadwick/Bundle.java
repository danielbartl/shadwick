package dev.jbaby.shadwick;

/** Whether Shadwick owns the page reset or is embedded in an application's existing styles. */
public enum Bundle {
    /** Tokens, components, Tailwind preflight and page base styles (the default). */
    STANDALONE(""),
    /** Tokens and components, with Tailwind's theme values compiled inline and no page reset. */
    EMBEDDED("-embedded");

    private final String suffix;

    Bundle(String suffix) {
        this.suffix = suffix;
    }

    public String suffix() {
        return suffix;
    }
}
