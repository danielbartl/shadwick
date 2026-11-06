# Internationalization

Built-in messages ship in **English, German, Spanish, French, Italian, Japanese, Dutch, Portuguese and Croatian**
(`en`, `de`, `es`, `fr`, `it`, `ja`, `nl`, `pt`, `hr`). They cover dialog actions, accessible names, menus, upload
feedback, calendar/date-picker controls, combobox searches, data-table pagination and library validation.
Wicket resolves them using the component/session locale, with language fallback for regional locales such as
`de-AT`, `pt-BR` or `hr-HR`, then English for unsupported languages. No extra setup or dependency is needed:

```java
Session.get().setLocale(Locale.GERMANY); // de-DE
// Croatian, with Croatia’s date/number conventions:
// Session.get().setLocale(Locale.forLanguageTag("hr-HR"));
```

Set the document's language too. `SwAppLayout` does this automatically; for your own page markup:

```java
html.add(AttributeModifier.replace("lang", (IModel<String>) () -> getLocale().toLanguageTag()));
```

Calendar month/day names, week rules, formatted dates, chart numbers and pagination numbers follow the locale.
Use a regional locale such as `de-DE` for country-specific week rules and date/number conventions.
An explicit `setCalendarLocale(...)` controls date presentation while messages continue to follow the component
locale. Native date/time values keep their ISO submission format. A native input's browser chrome follows the
browser/OS language. Ajax replacements resolve messages again when rendered.

Your field labels, option text, column headings, row actions, titles and notification bodies remain application
content. Translate them with Wicket models/resources as usual. Explicit labels and placeholders take precedence
over library defaults, including intentionally empty combobox placeholders.

Override defaults in your page/base-page resource bundle using the same keys. For example, alongside
`MyPage.java`, add `MyPage_de.utf8.properties`:

```properties
SwDialog.cancel=Verwerfen
SwCombobox.placeholder=Stadt suchen…
SwDataTable.actionsHeading=Aktionen
```

The library uses Wicket's normal resource-loader order. To give your application's bundle priority across all
pages, explicitly prepend Wicket's class loader in `init()` (as in Wicket Oat):

```java
getResourceSettings().getStringResourceLoaders().add(0,
        new ClassStringResourceLoader(MyApplication.class));
```

Then put overrides in `MyApplication_de.utf8.properties`. New language bundles can use the same keys in a page,
base-page or prioritized application bundle. Keep `${label}`/`${format}` validation variables and `{0}`/`{1}`
message arguments intact. The shipped UTF-8 bundles under `dev/jbaby/shadwick` document the available keys.

Context-aware toast helpers (`Sw.toast(target, ...)` or `ToastBehavior.script(component, ...)`) translate controls;
the pure script builder without a component uses English defaults. Direct JavaScript callers supply their own
`dismissLabel` and `regionLabel` options.

The runnable showcase has a language picker on every standard example page and a dedicated
`/internationalization` page. Open `/internationalization?lang=ja` or `/calendar?lang=de-AT`; language selection
persists in that Wicket session. The picker works without JavaScript. The showcase's example prose and the static
marketing site are English.
