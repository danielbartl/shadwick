package dev.jbaby.shadwick.util;

import java.io.Serializable;
import java.util.function.Supplier;

/**
 * A serializable supplier for Wicket components.
 */
@FunctionalInterface
public interface SerializableSupplier<T> extends Supplier<T>, Serializable {
}
