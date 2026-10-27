package dev.jbaby.shadwick;

/**
 * What happens when Shadwick detects an accessibility mistake while rendering, such as a button without an accessible
 * name (an icon-only button without an {@code aria-label}).
 *
 * @see ShadwickSettings#setAccessibilityChecks(AccessibilityChecks)
 */
public enum AccessibilityChecks {

    /** The page fails to render, with a message saying how to fix it. The default in development mode. */
    FAIL,

    /** The page renders, and the same message is logged as a warning. The default in deployment mode. */
    WARN,

    /** Nothing is checked. */
    OFF
}
