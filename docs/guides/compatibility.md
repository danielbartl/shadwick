# Compatibility

Shadwick `0.1.0` is published on Maven Central, with a Wicket 11 / Java 21 baseline.
The released public/protected API is recorded and checked during the normal Maven test run.

## Java and Wicket

The library compiles with `--release 21` and is built against Apache Wicket `11.0.0`. CI tests Java 21 and Java 25.
Use matching versions of `wicket-core`, `wicket-extensions` and any other Wicket modules in your application.
The reviewed target is Wicket 11.0.0; compatibility with other Wicket versions needs its own verification.

The library's automatic module name is `dev.jbaby.shadwick`. This stabilizes its identity for applications that
use automatic modules; the library does not yet declare a `module-info.java` or claim a tested JPMS deployment.
Snapshot users who relied on the filename-derived name `shadwick` must update their module declaration from
`requires shadwick;` to `requires dev.jbaby.shadwick;`. Ordinary classpath applications keep the same imports.

The library depends on Wicket core/extensions and compiles against Servlet API 6.1.0 with provided scope; the host
application supplies the Servlet API at runtime. Spring Boot,
Playwright, axe, Node and Tailwind are not library runtime dependencies. Compiled CSS, browser scripts, markup,
translations and their licenses travel in the library JAR. Read [Installation](./installation.md) for setup.

## Moving from the pre-release Wicket 10 build

Upgrade the build and runtime JDK to Java 21+, align every Wicket module to 11.0.0 and use a Servlet 6.1+
container (for example Tomcat 11). The examples already use Spring Boot 4.1.2, Spring 7 and Tomcat 11;
the Docker build/runtime and release/site workflows use Java 21. The library remains independent of Spring.

[Wicket's migration guide](https://cwiki.apache.org/confluence/spaces/WICKET/pages/462815957/Migration%2Bto%2BWicket%2B11)
explains additional changes for applications using Wicket integrations. In particular, Wicket 11's authenticated
cryptography cannot decrypt Wicket 10 encrypted URLs or stored pages. Deploy with fresh sessions/page stores and
regenerate encrypted links; do not mix Wicket 10 and 11 artifacts or expect serialized pages to cross the upgrade.
Shadwick's public/protected declarations remain checked by the existing 0.1 baseline.

Shadwick fields retain server-side inline validation: their common input configuration removes Wicket 11's
automatically derived native `required`/`pattern` attributes and label-derived placeholder. Explicit placeholders
still work, and applications can opt into native constraints with later `addToInput(AttributeModifier...)` behaviors.
Wicket-managed markup IDs now reject colons; use letters, digits, underscores, hyphens or dots for component IDs
assigned with `setMarkupId`. Native skip-link fragments still support escaped Unicode and colon IDs in caller markup.

## Java API

The 0.1 baseline includes public types, constructors, methods and fields, plus protected subclass hooks.
It records generic signatures, superclass/interface relationships, enum order, record components and scalar
constant values such as `CONTENT_ID`. Compiler-generated bridges and private/package-private implementation
classes are excluded. Public low-level utilities are included so their removal also receives a review.

The baseline is a review guard: a change to an exported declaration requires an explicit update after assessing
its effect on application code. It is not proof of arbitrary cross-version binary or behavioral compatibility.
The normal checks also reject private/package-private types leaking through exported signatures and bytecode
newer than Java 21. Consumer tests in a separate application package exercise fluent form fields, inherited
models, validation, serializable callbacks and chart factories.

Existing Wicket methods remain available through inheritance. Shadwick's own fluent setters return the concrete
component type where documented; inherited Wicket setters retain Wicket's return types. Protected hooks belong
to the subclass contract. Call `super` where the hook documentation requires it, especially lifecycle methods.

String/model overloads support both fixed and translated or detachable values. An untyped `null` can be ambiguous
between overloads. Prefer a constructor that omits the optional value, or use an explicitly typed model according
to that component's documented null semantics. Enum/callback formatter overloads have the same Java constraint;
the [chart guide](../components/charts.md) describes how to clear an override.

## Wicket models and stored pages

Components detach the models they own at the end of a request. Continue to use detachable models for database
or service-backed data. Keep request-local caches transient. Serializable callback interfaces make lambdas and
method references eligible for page serialization; everything they capture must also be serializable.
Avoid capturing service objects, requests, database sessions or an enclosing application object in a callback.

The serialization checks cover round trips within the same application build. Serialized Wicket pages and
sessions are not a migration format between Shadwick versions, application builds or JDK versions. Drain or
invalidate stored pages/sessions when deploying a changed build, unless your application has separately tested
that deployment and its serialization strategy. A fixed `serialVersionUID` alone would not establish that guarantee.

Read [Wicket integration](./wicket-integration.md) for model ownership, validation and Ajax refresh behavior.

## Markup, resources and browsers

Use the documented markup slots, component IDs and `Sw.Behaviors` APIs. The `input` child of a form field and named
content-slot constants are integration contracts. Generated markup IDs, internal child paths, transient JavaScript
state and implementation helpers are not application extension points. Follow guide examples when using the
`sw-` component classes and theme tokens; those guides describe the supported styling surface.

The components use Wicket's strict nonce-based CSP without adding inline styles or handlers. Resource replacements
must keep the matching CSS, scripts, fonts and markup together. The embedded CSS bundle leaves the application's
reset and page styling to the application; see [Theming](./theming.md).

Chromium and Firefox are the automated browser targets. The suite covers keyboard use, narrow layouts, light/dark
skins, native fallbacks and selected no-script paths. This is not a claim that every interactive feature works
without JavaScript or in every historical browser. See [Accessibility](./accessibility.md) and each component's
fallback notes. Native dialogs, popovers and enhanced pickers retain their documented browser requirements.

## Wicket Oat and Shadleaf

Wicket Oat supplies Java API conventions: models, callbacks, factories and ordinary Wicket composition. Shadleaf
supplies markup and visual references. The libraries have different packages, variants, skins, component breadth
and chart APIs. Moving an application involves reviewing its imports, enum choices and component markup;
Shadwick does not promise binary compatibility or a complete drop-in migration from either reference.

Release contributors should read the API-baseline procedure in the repository's documentation contribution guide
before changing an exported declaration. The [release preparation guide](releases.md) describes artifact/signing validation and manual Central staging.
