package dev.jbaby.shadwick.components;

import dev.jbaby.shadwick.behaviors.SkeletonBehavior;
import org.apache.wicket.markup.html.WebMarkupContainer;

/** A loading placeholder; an application's class can override its default dimensions. */
public class SwSkeleton extends WebMarkupContainer {
    public SwSkeleton(String id) { this(id, SkeletonBehavior.Shape.LINE); }
    public SwSkeleton(String id, SkeletonBehavior.Shape shape) { super(id); add(new SkeletonBehavior(shape)); }
}
