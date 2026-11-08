# Skeletons and spinners

A skeleton is a decorative placeholder for content still loading. A spinner can provide a named loading status.

```java
add(new SwSkeleton("photo", SkeletonBehavior.Shape.CIRCLE));
add(new SwSpinner("loading").setLabel("Loading projects"));
```

```html
<section aria-busy="true" aria-label="Project list"><div wicket:id="photo"></div></section>
<svg wicket:id="loading"></svg>
```

Skeleton shapes and spinner sizes are listed in the API enums below; the spinner also accepts a size model for changing appearance. `Sw.Behaviors.skeleton()` and `.spinner()` style caller-owned markup. Skeletons are decorative: name
the loading region or provide a nearby status instead of naming every placeholder. Supply `setLabel(...)` for an
informative spinner; an unspecified spinner label uses the localized Loading fallback. Reduced-motion and forced-color rules are shared by skins.

Your application owns async work and refresh scheduling. Replace a loading region with content and clear its
`aria-busy` state when complete. For a numeric amount use [progress](./progress.md); for a pending Ajax action use
[busy controls](./busy-controls.md), which guard repeated activation and restore the action after completion.

For deferred server-rendered content with these skeletons and Wicket’s shared timer, use [lazy-loaded panels](lazy-load.md).
