package dev.jbaby.shadwick.util;

import java.io.Serializable;
import java.util.function.BiConsumer;

/**
 * A serializable bi-consumer for Wicket components.
 */
@FunctionalInterface
public interface SerializableBiConsumer<T, U> extends BiConsumer<T, U>, Serializable {
}
