package dev.jbaby.shadwick.components;

import dev.jbaby.shadwick.behaviors.TimelineBehavior;
import dev.jbaby.shadwick.behaviors.TimelineEventBehavior;
import dev.jbaby.shadwick.util.SerializableFunction;
import org.apache.wicket.AttributeModifier;
import org.apache.wicket.markup.ComponentTag;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.list.ListItem;
import org.apache.wicket.markup.html.list.ListView;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.LoadableDetachableModel;
import org.apache.wicket.model.Model;

import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.temporal.TemporalAccessor;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * An ordered history with escaped titles, optional time/description and decorative markers.
 * Events stay in supplied order. No script, automatic sorting, polling or live region is added.
 * @param <T> event type
 */
public class SwTimeline<T> extends Panel {
    private SerializableFunction<T, ?> description;
    private SerializableFunction<T, TimelineEventBehavior.Variant> variant;
    private ZoneId timeZone = ZoneOffset.UTC;
    private final IModel<? extends List<T>> events;

    /** The time extractor may be null to omit times; null event lists render an empty history. */
    public SwTimeline(String id, IModel<? extends List<T>> events, SerializableFunction<T, ?> title,
                       SerializableFunction<T, ? extends TemporalAccessor> time) {
        super(id); Objects.requireNonNull(events); Objects.requireNonNull(title); setOutputMarkupId(true);
        this.events = events;
        var history = new WebMarkupContainer("history"); history.add(new TimelineBehavior()); add(history);
        IModel<List<T>> snapshot = new LoadableDetachableModel<>() {
            @Override protected List<T> load() { var values = events.getObject(); return values == null ? List.of() : new ArrayList<>(values); }
        };
        history.add(new ListView<T>("events", snapshot) {
            @Override protected void populateItem(ListItem<T> item) {
                T event = item.getModelObject();
                var marker = variant == null ? null : variant.apply(event);
                item.add(new TimelineEventBehavior().setVariant(marker == null ? TimelineEventBehavior.Variant.DEFAULT : marker));
                item.add(new Label("title", text(title.apply(event))));
                TemporalAccessor when = time == null ? null : time.apply(event);
                String shown = when == null ? "" : DisplayValues.temporal(when, getLocale(), timeZone);
                String machine = when == null ? null : DisplayValues.datetime(when);
                var timeLabel = new Label("time", shown) {
                    @Override protected void onComponentTag(ComponentTag tag) {
                        super.onComponentTag(tag);
                        if (machine == null) tag.setName("span");
                    }
                };
                timeLabel.setVisible(when != null);
                if (machine != null) timeLabel.add(AttributeModifier.replace("datetime", machine));
                item.add(timeLabel);
                String detail = description == null ? "" : text(description.apply(event));
                item.add(new Label("description", Model.of(detail)).setVisible(!detail.isBlank()));
            }
        });
    }
    private static String text(Object value) { return value == null ? "" : value.toString(); }
    public SwTimeline<T> setDescription(SerializableFunction<T, ?> description) { this.description = description; return this; }
    public SwTimeline<T> setVariant(SerializableFunction<T, TimelineEventBehavior.Variant> variant) { this.variant = variant; return this; }
    /** Zone for instant-bearing values; local calendar and wall-clock values are unchanged. Defaults to UTC. */
    public SwTimeline<T> setTimeZone(ZoneId timeZone) { this.timeZone = Objects.requireNonNull(timeZone); return this; }
    @Override protected void onDetach() {
        try { events.detach(); } finally { super.onDetach(); }
    }
}
