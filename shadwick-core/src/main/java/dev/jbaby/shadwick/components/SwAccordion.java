package dev.jbaby.shadwick.components;

import dev.jbaby.shadwick.behaviors.AccordionBehavior;
import dev.jbaby.shadwick.behaviors.DisclosureTriggerBehavior;
import dev.jbaby.shadwick.behaviors.StyledBehavior;
import dev.jbaby.shadwick.util.SerializableBiConsumer;
import org.apache.wicket.AttributeModifier;
import org.apache.wicket.markup.html.TransparentWebMarkupContainer;
import org.apache.wicket.markup.html.list.ListItem;
import org.apache.wicket.markup.html.list.ListView;
import org.apache.wicket.model.IModel;

import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Oat-style repeating native details sections. Callback children "title" and "content" stay on each ListItem;
 * transparent "trigger", "heading" and "body" containers provide Shadleaf's disclosure anatomy.
 * Wrap the repeating details in a sw-accordion container, and refresh that wrapper over Ajax.
 */
public abstract class SwAccordion<T> extends ListView<T> {
    private final AccordionBehavior behavior;
    private Set<Integer> openItems = Set.of();
    private int headingLevel = 3;

    public SwAccordion(String id, IModel<List<T>> items) { this(id, items, true); }
    public SwAccordion(String id, IModel<List<T>> items, boolean exclusive) {
        super(id, OncePerRequestModel.of(Objects.requireNonNull(items)));
        behavior = new AccordionBehavior(exclusive); add(behavior);
    }
    public boolean isExclusive() { return behavior.isExclusive(); }
    public SwAccordion<T> setExclusive(boolean exclusive) { behavior.setExclusive(exclusive); return this; }
    /** Initial open indexes; with exclusivity the first in document order wins. These are not client open state. */
    public SwAccordion<T> setOpenItems(int... indexes) {
        if (Arrays.stream(indexes).anyMatch(index -> index < 0)) throw new IllegalArgumentException("Open indexes must be non-negative");
        openItems = Arrays.stream(indexes).boxed().collect(Collectors.toUnmodifiableSet()); return this;
    }
    public SwAccordion<T> setHeadingLevel(int level) {
        if (level < 1 || level > 6) throw new IllegalArgumentException("Heading level must be 1–6");
        headingLevel = level; return this;
    }
    private boolean initiallyOpen(int index) {
        return openItems.contains(index) && (!isExclusive() || openItems.stream().min(Integer::compareTo).orElse(-1) == index);
    }
    @Override protected ListItem<T> newItem(int index, IModel<T> model) {
        ListItem<T> item = super.newItem(index, model);
        item.add(behavior, AttributeModifier.replace("open", (IModel<String>) () -> initiallyOpen(index) ? "open" : null));
        var trigger = new TransparentWebMarkupContainer("trigger");
        trigger.add(new DisclosureTriggerBehavior(DisclosureTriggerBehavior.Kind.ACCORDION));
        var heading = new TransparentWebMarkupContainer("heading");
        heading.add(new StyledBehavior("sw-accordion-trigger-text"), AttributeModifier.replace("role", "heading"),
                AttributeModifier.replace("aria-level", (IModel<String>) () -> String.valueOf(headingLevel)));
        trigger.add(heading); item.add(trigger);
        var body = new TransparentWebMarkupContainer("body") {
            @Override protected void onConfigure() {
                super.onConfigure(); setVisible(item.isEnabledInHierarchy() || initiallyOpen(index));
            }
        };
        body.add(new StyledBehavior("sw-accordion-content")); item.add(body);
        return item;
    }
    public static <T> SwAccordion<T> of(String id, IModel<List<T>> items, SerializableBiConsumer<ListItem<T>, T> populate) {
        return of(id, items, true, populate);
    }
    public static <T> SwAccordion<T> of(String id, IModel<List<T>> items, boolean exclusive, SerializableBiConsumer<ListItem<T>, T> populate) {
        Objects.requireNonNull(populate);
        return new SwAccordion<>(id, items, exclusive) {
            @Override protected void populateItem(ListItem<T> item) { populate.accept(item, item.getModelObject()); }
        };
    }
}
