# Timelines

`SwTimeline<T>` shows events in the supplied order using a native ordered list. Each event has an escaped title,
an optional localized time, an optional description and a decorative marker. It follows Wicket Oat’s constructor,
description and variant-extractor pattern. It adds no polling, sorting, JavaScript or live regions.

```java
add(new SwTimeline<>("history", eventModel, Event::title, Event::at)
        .setDescription(Event::details)
        .setVariant(event -> event.failed()
                ? TimelineEventBehavior.Variant.DESTRUCTIVE
                : TimelineEventBehavior.Variant.DEFAULT)
        .setTimeZone(ZoneId.of("Europe/Berlin")));
```

```html
<div wicket:id="history"></div>
```

## Events, models and Ajax

The model accepts a list of application-owned event objects. The list is snapshotted once per request and detached
even while the timeline is hidden. Event extractors are serializable and called once per rendered event; repeated
children retain display strings rather than the extracted temporal objects. Null lists produce an empty history.
The application decides whether to show an empty-state component alongside it, as the `/record-display` demo does.

A null time extractor, or a null time returned for one event, omits that event’s time. Null/blank descriptions are
omitted. `setDescription(null)` and `setVariant(null)` restore those defaults. Null titles render empty text;
supply meaningful titles for a readable history.

The component has an output markup ID: update the model and call `target.add(history)` to rebuild current events.
Removed events, descriptions, times and marker variants do not survive replacement. Use one application-owned
status region to announce an update when appropriate, rather than making the whole history a live region.

## Dates and time zones

Local dates, times and date-times retain their calendar/wall-clock values. Instant, offset and zoned date-times
display in a configured zone, defaulting to UTC, independently of the server’s default zone. `setTimeZone` accepts
a `ZoneId`. Dates use locale medium style; time-of-day uses short style. Year/month and month/day values have
localized labels as well.

Supported values render in `time` elements with machine-readable `datetime` attributes. Local times/date-times,
instants and offset/zoned date-times use at most millisecond precision there; a zoned date-time’s metadata carries
its original offset without a bracketed region name. The model’s original value is unchanged. Local dates,
year-months and month-days also have their corresponding machine formats. Years outside 1–9999, unsupported
temporal values and historical offsets containing seconds render readable `span` text instead of invalid metadata.
See the [HTML time element](https://html.spec.whatwg.org/multipage/text-level-semantics.html#the-time-element)
and its [time syntax](https://html.spec.whatwg.org/multipage/common-microsyntaxes.html#valid-time-string).

## Markers and accessibility

`TimelineEventBehavior.Variant` is `DEFAULT`, `SECONDARY`, `OUTLINE` or `DESTRUCTIVE`. These affect only the marker.
Communicate status in text, such as “Payment received” or “Delivery failed”, so color is never the only signal.
Markers and connectors are CSS pseudo-elements; they add no screen-reader content or keyboard stops. Native list
semantics retain event order. Dates without a time zone do not pretend to identify an instant.

Text wraps in narrow containers and logical positioning mirrors the timeline in RTL. System colors preserve the
markers/connectors in forced-colors mode; print uses outlines/connectors and avoids splitting individual events.
No animation or additional runtime is needed, including with reduced motion or JavaScript disabled.

## Caller-owned histories

Attach `Sw.Behaviors.timeline()` to an `ol` and `Sw.Behaviors.timelineEvent()` to each `li`. The event behavior
accepts a fixed or model-backed variant and detaches its model. Use `sw-timeline-heading`, `sw-timeline-title`,
`sw-timeline-time` and `sw-timeline-description` for its text parts. Supply your own valid `datetime` values.

Pair a history with [Description lists](description-list.md) for the record’s current state.
