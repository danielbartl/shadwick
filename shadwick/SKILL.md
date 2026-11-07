---
name: shadwick
license: Apache-2.0
description: Build or modify Apache Wicket interfaces with Shadwick components, including matching Java and HTML, forms, Ajax updates, tables, overlays, skins, and localization. Use for applications consuming dev.jbaby:shadwick; general Java or unrelated UI work does not need this skill.
---

# Shadwick

Build server-rendered Wicket interfaces using the existing application's models, markup and request lifecycle.
Shadwick is the Java library `dev.jbaby:shadwick`, with packages under `dev.jbaby.shadwick`. Its shadcn-style
appearance comes from packaged CSS and small component runtimes. Consumers do not need Node or Tailwind.

## Find the right source

- Start with the application's Java/Wicket versions, dependency configuration and existing base page. The current
  library targets Java 17+ and Wicket 10. Check the [installation guide](https://danielbartl.github.io/shadwick/docs/guides/installation/index.md)
  for current coordinates and availability; do not assume a snapshot is downloadable from Maven Central.
- Use [component routing](references/components.md) to find a family and its guide. Read only the relevant guide
  and API signatures. The [documentation index](https://danielbartl.github.io/shadwick/llms.txt) links to Markdown
  guides that include API references derived from Java source.
- In a Shadwick checkout, inspect the matching `docs/components/` guide, `shadwick-core/src/main/java/` type,
  and `shadwick-examples/` page before inventing an overload or markup slot. Otherwise use the published guide
  and [live catalog](https://shadwick-production.up.railway.app/components), whose code panels show the deployed example files.

## Compose the interface

Keep the application's framework and layout. Install `Shadwick.install(this)` in `WebApplication.init()` if it is
not installed already. `getMarkupSettings().setStripWicketTags(true)` keeps development wrappers out of native
component relationships. The library does not require Spring Boot; Clover Studio is an example application.

Pair each component ID with a `wicket:id` at the same hierarchy level. Field panels such as `SwTextField` own
their label/input/error markup and attach to a container; caller-owned components and behaviors retain the
markup described by their guide. Use a Wicket Ajax callback for actions; a button behavior supplies styling.

Use [forms and Ajax](references/forms-and-ajax.md) for field ownership, submission, nested forms and small
replacement targets. Its working templates are included with this skill. Use [data, appearance and locale](references/data-and-appearance.md)
for providers, stable keys, per-user preferences and translated application content.

Keep page state serializable. Bind values with typed `IModel`s, detach database-backed models, and capture
stable entity IDs in callbacks. Persistence, authorization, file storage and business rules remain application-owned.
The session-owned Studio store is demonstration code, not a production persistence layer.

## Preserve the interaction contract

For an in-place action, use the callback's `AjaxRequestTarget` to update the affected components, including
validation/feedback. An intentional navigation may use a page response or native link. Do not turn a local update
into a new page merely to recompute its data. A Wicket Ajax response can contain both rendered component HTML
and scripts that initialize controls; a script contribution alone does not prove that a page reload occurred.

Give ordinary components output markup IDs before targeting them; use a placeholder when an initially invisible
component needs to appear later. Preserve unsubmitted text and browser-only state by keeping unrelated editors
outside replacement regions. Check changed values after callbacks using current models, not stale entity snapshots.

Keep the default strict nonce-based CSP. Use the library's contributed resources and external application scripts;
inline handlers and style attributes are unnecessary. Preserve meaningful labels, unique IDs, keyboard access,
focus return after overlays, inline errors and the documented native fallbacks.

## Check the result

Use the consuming application's normal build and tests. Check both Java and matching HTML, validation errors,
partial updates and any relevant keyboard/mobile behavior. Do not claim native/no-script support for a path that
requires Ajax, such as nested form submission. Prefer a focused regression for a meaningful failure over tests
that restate styling choices.

In the library checkout, `./mvnw -B verify` builds the packaged CSS and runs JVM checks. Browser tests are selected
with `-Pbrowser`; choose the affected class/method rather than assuming the entire catalog matrix is a quick check.
