# Wicket integration

Shadwick components are Wicket components: construct them with IDs, bind models and supply matching HTML.
Your application owns persistence, authorization, validation rules and request lifecycle choices.

## Components and behaviors

A component such as `SwTextField` owns its label/input/error markup; bind it to a container. A component such as
`SwCard` keeps the markup you provide. A behavior styles or enhances an existing Wicket component.

```java
add(new SwTextField<String>("name", "Name", nameModel));
add(new SwCard("card"));
add(new BookmarkablePageLink<Void>("projects", ProjectsPage.class)
        .add(Sw.Behaviors.button(ButtonBehavior.Variant.OUTLINE)));
```

```html
<div wicket:id="name"></div>
<section wicket:id="card"><div class="sw-card-content">Your markup</div></section>
<a wicket:id="projects">Projects</a>
```

Use component classes such as `sw-card-content` for caller-owned parts. Consumers need no Tailwind build.
`Sw.Behaviors` exposes behavior factories; `Sw.Components` exposes callback-driven factories where available.
A behavior does not turn an ordinary link into an Ajax callback or create a form model for it.

## Models and lifecycle

Pass `IModel<T>` for application data. Explicit models are useful for a small form; `CompoundPropertyModel`
binds fields by ID for a larger object. Labels may use explicit strings, resource models or the field's resource key.
Keep types consistent: a date field binds `LocalDate`, a number field binds the declared number type, and choices
use their renderer's IDs. Do not use a UI label as a persistent entity key.

Use detachable models for database entities and providers for large collections. Shadwick detaches models it owns;
custom panels, provider implementations and application callbacks still need their normal Wicket detach behavior.
Store stable serializable IDs in selections, expanded-row state and `SwDataTable.setRowKey(...)` when loading entities.

Models and Wicket pages can be serialized between requests. Avoid retaining request-scoped streams, uploaded files,
open transactions or service objects that cannot participate in your application's Wicket injection/serialization setup.

## Forms and validation

Add fields to a Wicket `Form`; put a `SwSubmitButton` inside it or associate the button with the form explicitly.
Normal form submission and Ajax submission use Wicket conversion, validation and model update order.
Use `field.addValidator(...)` for a validator and `field.getInput()` to access the wrapped native form component.
`field.addToInput(...)` attaches an arbitrary input behavior. Form-component updating behaviors added through
`field.add(...)` are forwarded by the field wrapper; use the field guide for its exact contract.

First errors appear inline with `aria-invalid` and a linked error description. [Feedback](../components/feedback.md)
panels and toasts avoid repeating those first inline errors. Required status and server-side business rules belong
in validators, not solely in HTML attributes or client JavaScript.

## Ajax replacement

An ordinary Wicket component needs an output markup ID before adding it to an Ajax target:

```java
Label total = new Label("total", totalModel);
total.setOutputMarkupId(true);
add(total);
add(SwButton.onClick("refresh", "Refresh", target -> target.add(total)));
```

Use `setOutputMarkupPlaceholderTag(true)` when an initially invisible ordinary component must later be shown.
Library controls with their own Ajax behavior expose the needed IDs and initialize new native/enhanced content
after replacement. Do not copy generated DOM IDs between component instances or attach the same behavior instance
to several owners when its API says it has one owner.

Replacing a subtree can discard browser-only, unsubmitted text or disclosure state. Submit a draft before replacing
its editor, or keep the editor outside the refreshed region. Data-table editors preserve server-side draft/validation
state while their row remains visible; paging a row out of view discards its unfinished editor.

## Nested forms and overlays

Wicket renders a nested form as a container. A submit button inside it can otherwise be picked when Enter is pressed
in an outer form. `SwSubmitButton` installs `NestedFormButtonBehavior`: it uses `type="button"` when nested. Make the
button the inner form's default button to scope Enter there. Nested inline table forms use the same pattern.

```java
Form<Void> editor = new Form<>("editor");
SwSubmitButton save = new SwSubmitButton("save", "Save", editor);
editor.add(save);
editor.setDefaultButton(save);
```

Nested form submission requires JavaScript for these controls. Standalone native forms work without it. Keep
`SwUserMenu` outside other forms because its sign-out action owns a separate form. A dialog's form and validation
callbacks follow [the dialog guide](../components/dialog.md); a visible overlay is not an authorization check.

## Resources and Content Security Policy

Call `Shadwick.install(this)` during `WebApplication.init()`. Configure skin/bundle there; the library contributes
deduplicated CSS and external runtime resources. Keep Wicket's default nonce-based CSP. Shadwick does not require
inline event handlers or style attributes. Your own scripts, image endpoints and other application assets must obey
your application's policy. Serving a different skin's fonts/CSS from a CDN is an application deployment choice.

Enable `getMarkupSettings().setStripWicketTags(true)` to keep development-only Wicket wrappers out of native
parent/child relationships, especially `details`/`summary`. Use HTML tags required by each component's guide.

## Application responsibilities

Check authorization again in mutation/download callbacks and data providers. Hidden columns, disabled buttons and
omitted menu entries describe UI state; they do not authorize access. Providers own stable sorting, count queries,
filtered exports and database consistency. Applications own upload storage, download authorization, transaction
boundaries and database concurrency control. Busy controls prevent repeated activation of one rendered control;
application-level idempotency is still needed for operations that require it.
