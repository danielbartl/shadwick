# Forms and partial updates

## Start from working files when useful

The skill includes [QuickStartApplication.java](../assets/QuickStartApplication.java),
[QuickStartPage.java](../assets/QuickStartPage.java) and [QuickStartPage.html](../assets/QuickStartPage.html).
These are the same complete examples compiled and exercised by the library's documentation tests.
Rename the package/class and place Java/HTML according to the consuming project's Wicket resource convention.
In an existing application, reuse its `WebApplication` and add only the needed install/configuration calls.

The example keeps an explicit `IModel<String>`, puts the field and submit buttons in a Wicket `Form`, and
updates a receipt label over Ajax. `Form.onSubmit()` owns the successful model operation. The native submit
button uses that same form callback. Replace the example receipt operation with the application's service call.

## Field ownership

`SwTextField` and related field panels attach to a container such as `<div wicket:id="name"></div>`.
They create the label, native input and inline feedback. Do not add a second label/input with the same ID.
Keep the Java hierarchy aligned with the matching markup and add the field to its actual form.

Use `field.addValidator(...)` for validation. `field.getInput()` exposes the wrapped Wicket form component;
`field.addToInput(...)` targets that input explicitly, for example for an autocomplete attribute. Form-component
updating behaviors added to the field are forwarded to its wrapped input. Other behaviors can target the wrapper.

Use typed models: `LocalDate` for native dates, the declared numeric type for numbers, renderer IDs for choices.
Server conversion and validation still apply to native HTML input attributes. Cross-field validators should use
the wrapped form components. First errors are inline; add feedback panels/toasts where additional messages belong.

## Ajax and navigation

A label or ordinary container needs `setOutputMarkupId(true)` before `target.add(...)`. Use
`setOutputMarkupPlaceholderTag(true)` if a hidden ordinary component needs to become visible later.
Include the affected result and feedback components on both valid and invalid paths as appropriate.

Keep successful edits in the current view when that is the requested behavior. Use an explicit page navigation
only when the action should navigate. Refreshing an entire page or calling `setResponsePage` to update a count
can discard unsaved inputs and introduce redirects. Refresh a result collection when its data/order changes,
while preserving unrelated form controls. Native form navigation can remain a useful fallback.

For nested forms, Wicket renders the inner form as a container. `SwSubmitButton` handles its button type;
set the inner form's default button to scope Enter correctly. Nested submission requires script; do not claim
that an Ajax-only nested flow works without JavaScript. Keep `SwUserMenu` outside other forms because sign-out
owns a form. Use the dialog guide's form/content contract for an editor in `SwDialog`.

Use serializable/detachable page state. Reload entities by stable IDs and recheck business rules in the callback.
Busy indicators prevent repeated UI activation; they do not replace application idempotency or concurrency checks.

Read [Wicket integration](https://danielbartl.github.io/shadwick/docs/guides/wicket-integration/index.md),
[forms](https://danielbartl.github.io/shadwick/docs/components/forms/index.md) and
[dialogs](https://danielbartl.github.io/shadwick/docs/components/dialog/index.md) for the exact contracts.
