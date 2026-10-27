package dev.jbaby.shadwick.util;

import java.io.Serializable;
import java.util.function.Consumer;

/**
 * A serializable consumer for Wicket components.
 */
@FunctionalInterface
public interface SerializableConsumer<T> extends Consumer<T>, Serializable {
}
