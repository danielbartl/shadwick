# Badges

A badge is a short visible label for a category or status. Its message is HTML-escaped.

```java
add(new SwBadge("status", "Ready", BadgeBehavior.Variant.SECONDARY));
```

```html
<span wicket:id="status"></span>
```

Use `setVariant(...)` with a value or model to change appearance. Available variants come from `BadgeBehavior`;
the generated enum reference below lists them. `Sw.Behaviors.badge()` supports caller-owned composed content.
Keep meaning in text, not only color. A badge does not create a live announcement, submit a value or perform an
action. Use a real link/button when the label is interactive and give that element an action-specific name.
