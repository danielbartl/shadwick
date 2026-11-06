# Feedback

Inline validation, fenced feedback panels and feedback toasts.

```java
// A panel for this form's messages; updates and clears itself during Ajax requests.
form.add(new SwFeedbackPanel("feedback", form));

// On a page: messages outside fenced panels, including session messages after navigation.
add(Sw.Behaviors.feedbackToasts());
```

Both skip the first field error already shown inline. Panels render alerts; errors use the destructive variant,
other levels use the default. Toasts preserve the feedback level, render message text safely and remain until
dismissed. Panel filters and fences work as with Wicket's `FencedFeedbackPanel`; a fenced panel reserves its
container's messages even while hidden. Set up the fences when constructing the page.
