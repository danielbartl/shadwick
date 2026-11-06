# Toasts

Use `Sw.toast(...)` for a direct notification from an Ajax callback, without creating a Wicket feedback message.
Its overloads follow Oat's message, variant and optional title API. Add the resource behavior once to your page or
base page before using the helpers:

```java
add(Sw.Behaviors.toasts());
add(SwButton.onClick("save", "Save", target -> {
    service.save();
    Sw.toast(target, "Your workspace is ready.", ToastBehavior.Variant.SUCCESS, "Changes saved");
}));
```

## Variants and content

Variants are `ToastBehavior.Variant.DEFAULT`, `SUCCESS`, `INFO`, `WARNING` and `ERROR`. The message is the body;
an optional title appears above it. Both are plain text, including any HTML-looking characters. Error notifications
use an alert live region; other variants use a polite status. Showing a toast leaves keyboard focus in place.

## Duration and dismissal

General toasts close after five seconds. Set a duration on a particular toast, or use zero to keep it until dismissed:

```java
Sw.toast(target, "Review your settings before continuing.", ToastBehavior.Variant.WARNING,
        "Take your time", Duration.ZERO);
Sw.toast(target, "The link is ready.", ToastBehavior.Variant.SUCCESS, "Copied", Duration.ofSeconds(8));
```

Durations are zero or 1–2147483647 milliseconds. Timers pause while the pointer is over the notifications, while a
toast has keyboard focus and while the page is hidden. Resuming leaves at least one second. Keep essential results
visible on the page; choose persistent messages for information that needs more reading time.

## Keyboard and modal behavior

Alt+T focuses the newest toast's close button. The close button or Escape dismisses the focused toast; focus moves
to a neighboring toast, then back to the control it came from after the last one. Escape elsewhere leaves toasts
alone. A named Notifications region holds a scrollable column at the logical bottom/end of the viewport, with
mobile gutters. While a native modal is open, the notifications move into it so they stay usable; they return to the
page when it closes. Removing the viewport clears its timers; navigating away clears old messages, including in
browser history snapshots. Toasts have no interactive action content besides dismissal.

## Full-page notifications and resources

For a full render, keep the same resource behavior and use the script helper from `renderHead`:

```java
response.render(OnDomReadyHeaderItem.forScript(ToastBehavior.script(this,
        "You can continue where you left off.", ToastBehavior.Variant.INFO, "Welcome back", Duration.ZERO)));
```

The component overload and Ajax helpers resolve `ToastBehavior.close` and `ToastBehavior.label` from Wicket
resources (defaults: “Dismiss notification” and “Notifications (Alt+T)”). The pure
`ToastBehavior.script(message, variant, title[, duration])` overloads use English control labels. For local browser
interactions the same runtime is available as
`Shadwick.toast(message, {variant: 'success', title: 'Copied', duration: 5000})`; options also accept `dismissLabel`
and `regionLabel`. `duration: 0` keeps the message. No inline styles or client framework are required.

The **Toasts** showcase (`/toasts`) covers all variants, timing, plain text, Ajax replacement, full-page scripts and
modal context. The landing page uses the library's toast runtime for its demo notifications. Existing
`FeedbackToastsBehavior` continues to handle Wicket messages and fences, with persistent, manually dismissed toasts.
