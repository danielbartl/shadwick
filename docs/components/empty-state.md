# Empty state

Use `SwEmptyState` when a list, search or workspace has no content and the user needs context or a next step.

```java
add(new SwEmptyState("empty", "No projects yet", "Create one to get started.").setIcon("folder"));
```

```html
<div wicket:id="empty"></div>
```

The title and message accept strings or models. A blank message hides its description; `setIcon(null)` hides
media. Use `setContent(component)` with `SwEmptyState.CONTENT_ID` for a panel/fragment containing real actions.
The component rejects an incorrect slot ID. Give action buttons visible names and place the empty-state title in an appropriate page context. The component does not create a project or decide when a list
is empty; visibility and callbacks belong to the application. `SwDataTable.setEmptyState(...)` accepts custom
empty content for a filtered table.
