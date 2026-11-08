# Lazy-loaded panels

`SwLazyLoadPanel<T>` shows decorative skeletons while Wicket defers creating an expensive component until a later
request. It extends Wicket’s `AjaxLazyLoadPanel` and follows Wicket Oat’s constructor, factory and placeholder API.
It starts no threads or jobs. Content construction runs in the request that loads it, using Wicket’s normal page lock.

## Create content after the page response

```java
add(SwLazyLoadPanel.of("revenue", id -> new RevenuePanel(id, reports.revenueThisYear()))
        .setPlaceholder(SkeletonBehavior.Shape.LINE, 3)
        .setLoadingLabel("Loading annual revenue"));
```

```html
<div wicket:id="revenue"></div>
```

The factory receives `SwLazyLoadPanel.CONTENT_ID` (`content`) and must return a component with that ID. Null
components and wrong IDs are rejected. `Sw.Components.lazyLoad(...)` is the same factory. Subclasses can use the
constructors with or without an `IModel<?>` and implement `getLazyLoadComponent(String)` directly, including a more
specific component type through `SwLazyLoadPanel<T>`.

The initial response contains three skeleton lines and one localized loading status. `setPlaceholder(shape, count)`
accepts `LINE`, `BOX` or `CIRCLE` and a positive count; configure it before the first render. Existing skin classes
control skeleton dimensions and motion. Application classes can set dimensions appropriate to the future content.
`setLoadingLabel` accepts a string or model; null/blank values use the library’s localized default. Models detach
even after loading finishes or while the panel is hidden. The inherited component model follows normal Wicket rules.

## Readiness and polling

By default, the content is ready immediately and is created by the first timer request. A timer installed by
Shadwick starts after approximately 100ms. All Wicket lazy-load panels on a page share one timer; if a standard
Wicket lazy panel installed it first, that existing schedule is retained. While panels are still waiting, Wicket
uses the shortest `getUpdateInterval()` among visible pending panels, one second by default. The timer stops and
removes itself after all visible panels finish. Adding or showing a new panel through Ajax installs a timer again
when needed. An existing pending timer keeps its schedule when another panel is added.

For application-owned background work, store a serializable job identifier or detachable model and override the
readiness check. Keep that check inexpensive; a false result leaves the skeletons in place and requests another
poll. Override `getUpdateInterval()` for another positive interval, from 1ms through 2147483647ms. Avoid aggressive
polling because each request acquires the page lock.

```java
add(new SwLazyLoadPanel<Component>("report") {
    @Override protected boolean isContentReady() {
        return reportService.isReady(jobId);
    }
    @Override public Component getLazyLoadComponent(String id) {
        return new ReportPanel(id, reportService.result(jobId));
    }
});
```

`isLoaded()` is inherited and has a side effect: if content is ready, it creates it. Do not call it merely to read
a loading flag while rendering the initial page. The library tracks `aria-busy` independently to preserve deferral.
If you override `onContentLoaded(T, Optional<AjaxRequestTarget>)`, call `super.onContentLoaded(...)` so the busy
state clears. The optional target is absent for native requests.

## Failures, retry and replacement

Handle expected service failures inside the application factory and return an error Panel containing a retry
action. Retry by replacing the entire `SwLazyLoadPanel` with a fresh instance and adding that instance to the Ajax
target. The `/lazy-load` demo includes this flow and both content-panel source files.

Unexpected factory/readiness exceptions follow Wicket’s normal exception handling. This wrapper does not catch
them, retry silently or add an error state to Wicket’s inherited lifecycle. In particular, Wicket marks a panel
loaded before invoking its content factory; a factory that throws needs a fresh panel to retry. Do not attempt to
reset it by removing its content child or by invoking `isLoaded()` again.

Replacing a pending panel makes the shared timer visit the new component in the current tree, rather than the old
instance. Application jobs, cancellation and authorization remain application-owned. Hiding a component prevents
timer loading; disabling a display panel is not a substitute for authorization inside the data service.

## Accessibility and native fallback

The panel exposes `aria-busy="true"` while waiting and `false` once content replaces the placeholder. Skeletons
are decorative (`aria-hidden`); one visible `role="status"` text names the loading state. Loading and native-link
messages ship in all nine languages and can be overridden through Wicket resources. The panel adds no tab stop,
automatic focus move or live region around the loaded content. Name the outer region through application markup
when that helps identify it, and use a separate status receipt for an important completion announcement.

Without JavaScript, a native `Load content` link appears in the placeholder. It makes a normal Wicket request,
checks readiness and creates the same content when ready. If readiness is false, the loading state and link remain
available for another manual check. Loaded native links/forms work normally; their Ajax-only controls still need
JavaScript. Overriding `getLoadingComponent` replaces the entire default placeholder, including its status and
native fallback, so supply those semantics yourself.

All nine skins reuse their existing skeletons, reduced-motion and forced-color rules. The shared Wicket timer uses
the existing strict CSP; this component adds no client runtime, inline styles, external requests or dependency.
See [Skeletons and spinners](loading.md), [Wicket integration](../guides/wicket-integration.md) and
[Compatibility](../guides/compatibility.md) for the underlying contracts.
