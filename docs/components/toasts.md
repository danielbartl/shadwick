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

## Several at once

Toasts stack, as in shadcn/ui's Sonner and Shadleaf. The newest is in front; the others sit behind it, each a little
further back and smaller, with only the front one's content showing. While the pointer is on them, or one of them has
the keyboard focus (Alt+T), they spread out one above the other. At most three show at once; older ones wait, with
their time stopped, and show again when a newer one closes. Where the visitor prefers reduced motion, toasts only fade
in; otherwise they also slide in from the edge.

```java
Shadwick.install(this).setVisibleToasts(5);
```

The count is 1 to 10. The stack's numbers (heights and offsets) live in one constructable stylesheet that Shadwick's
script maintains, never in `style` attributes, so the strict CSP needs nothing extra. A browser without constructable
stylesheets shows the toasts in a plain scrolling column instead, all of them, newest nearest the edge.

## Position

Toasts appear in the bottom-end corner by default. `ShadwickSettings.setToastPosition(...)` puts them in another
corner or in the middle of the top or bottom edge: `ToastBehavior.Position.TOP_START`, `TOP_CENTER`, `TOP_END`,
`BOTTOM_START`, `BOTTOM_CENTER` or `BOTTOM_END`. Start and end follow the text direction, so they swap on a
right-to-left page. On a narrow screen toasts take the full width, less a 1rem gutter, whatever the position.

```java
Shadwick.install(this).setToastPosition(ToastBehavior.Position.TOP_CENTER);
```

The page's `Sw.Behaviors.toasts()` or `Sw.Behaviors.feedbackToasts()` renders these settings. To change them for one
page while it is open, for example from a user's preference, send `ToastBehavior.configure(target, position,
visibleToasts)` in an Ajax response, or render `ToastBehavior.configureScript(position, visibleToasts)` in an
`OnDomReadyHeaderItem`. In the browser the same is `Shadwick.configureToasts({position: 'top-center', visibleToasts: 3})`.
Toasts already showing move at once.

## Keyboard and modal behavior

Alt+T focuses the newest toast's close button. The close button or Escape dismisses the focused toast; focus moves
to a neighboring toast, then back to the control it came from after the last one. Escape elsewhere leaves toasts
alone. Shift+Tab from the newest toast reaches the other shown ones. While a native modal is open, the notifications move into it so they stay usable; they return to the
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
