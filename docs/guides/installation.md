# Installation

Built against Wicket 11.0.0 with Java 21 as the minimum and Jakarta Servlet 6.1+ at runtime. Keep Wicket modules aligned with the version declared in
the [project POM](https://github.com/danielbartl/shadwick/blob/main/pom.xml).
Read [Compatibility](./compatibility.md) for the candidate API and stored-page contracts. Until the first release,
install it locally with `./mvnw install`, then:

```xml
<dependency>
    <groupId>dev.jbaby</groupId>
    <artifactId>shadwick</artifactId>
    <version>0.1.0</version>
</dependency>
```

Install it in your application's `init()`:

```java
@Override
protected void init() {
    super.init();
    Shadwick.install(this);                    // vega, shadcn's classic look
    // Shadwick.install(this).setSkin(Skin.LYRA); // boxy, small type, square corners
    // Shadwick.install(this).setSkin(Skin.NOVA); // compact controls, rounder fields, muted footers
}
```

That adds Shadwick's stylesheet to every page. It works under Wicket's default strict, nonce-based Content Security
Policy and adds nothing to it.

Use `getMarkupSettings().setStripWicketTags(true)` for the component markup, as the showcase does. This also keeps
development-only `<wicket:panel>` wrappers out of native `details`/`summary` relationships.


## First page

Create a Wicket page and matching HTML resource. This form uses explicit models, a required name and an
Ajax submit button. The complete sources are compiled and exercised by the repository's documentation tests.

Build the initial component tree in `onInitialize()`, starting with `super.onInitialize()`. Keep the page
structure visible there, and put field configuration and submission handling in plainly named private methods.
The complete example follows this structure.

[Read the complete form example](../components/forms.md#complete-example).

Place the Java and HTML in the same package on the classpath, for example `dev.example.QuickStartPage` and
`dev/example/QuickStartPage.html`. Mount it with `mountPage("/start", QuickStartPage.class)` in your application.
A standard Wicket servlet/filter configuration is still required by the consuming application; Shadwick installs
component resources and behavior, not an HTTP server or authentication system.

## Explore the examples

From this repository:

```sh
./mvnw -B verify
java -jar shadwick-examples/target/shadwick-examples-0.1.0.jar
```

Open `http://localhost:8080`. The examples have skin, appearance and language controls. Their demo data is for
exploration; replace it with your own providers and services. The examples application is separate from the library
and is not published as a Maven dependency. Try the hosted [Clover Studio](https://shadwick-production.up.railway.app/)
and [component catalog](https://shadwick-production.up.railway.app/components), or read the [Studio guide](./clover-studio.md).

## Choose your integration

Use the [standalone bundle](./theming.md#embedded-styles) for the library's page defaults, or the embedded bundle
when your application already supplies its reset and page styles. [Choose a skin](./skins.md) for spacing and shape;
[theme tokens](./theming.md) control colors. Read [Wicket integration](./wicket-integration.md) before composing forms,
Ajax updates or caller-owned markup.

## Complete example

Use this application with the complete form page in the [form guide](../components/forms.md#complete-example).

<!-- snippet: docs/examples/QuickStartApplication.java -->
```java
package dev.example;

import dev.jbaby.shadwick.Shadwick;
import dev.jbaby.shadwick.Skin;
import org.apache.wicket.Page;
import org.apache.wicket.protocol.http.WebApplication;

public class QuickStartApplication extends WebApplication {
    @Override
    public Class<? extends Page> getHomePage() {
        return QuickStartPage.class;
    }

    @Override
    protected void init() {
        super.init();
        Shadwick.install(this).setSkin(Skin.VEGA);
        getMarkupSettings().setStripWicketTags(true);
        mountPage("/start", QuickStartPage.class);
    }
}
```
