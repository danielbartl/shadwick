# Alerts

Use an alert for a persistent message that belongs in the current page. `SwAlert` is an escaped message;
attach `AlertBehavior` to a container for a heading and longer description.

```java
add(new SwAlert("message", "The project could not be saved.", AlertBehavior.Variant.DESTRUCTIVE));
WebMarkupContainer details = new WebMarkupContainer("details");
details.add(Sw.Behaviors.alert());
add(details);
```

```html
<p wicket:id="message"></p>
<div wicket:id="details"><h2 class="sw-alert-title">Review your settings</h2>
  <p class="sw-alert-description">Choose a billing contact before continuing.</p></div>
```

`setVariant(value/model)` switches between the alert variants. Keep urgent announcements concise and include
an actionable correction when possible. An alert does not validate a form or update itself: for Wicket feedback
messages and automatic Ajax refresh, use [feedback](./feedback.md). Keep important outcomes on the page even when
you also show a [toast](./toasts.md).
