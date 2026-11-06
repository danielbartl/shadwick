# Text and number inputs

`SwEmailField`, `SwPasswordField`, `SwSearchField`, `SwTelField`, `SwUrlField` and
`SwNumberField<N>` share the same field wrapper, models and inline Ajax feedback:

```java
form.add(new SwEmailField("email").setRequired(true));
form.add(new SwPasswordField("password"));
form.add(new SwSearchField("query").setPlaceholder("Search projects"));
form.add(new SwTelField("telephone"));
form.add(new SwUrlField("website"));
form.add(new SwNumberField<Integer>("seats").setMin(1).setMax(10).setStep(1));
```

This example inherits models from a parent `CompoundPropertyModel`. Bind each field to a `<div wicket:id="...">`.
Email and URL use Wicket's server validators. Search and telephone accept arbitrary text; add application validators
when you need stricter rules. Password fields are required by default, clear their model on detach and never echo
values into HTML, following Wicket's `PasswordTextField`. Use `.setRequired(false)` for an optional password.

Number fields support integer and decimal types (including `BigDecimal`), min/max/step values or models, and native
HTML number formatting independent of the session locale. For an empty plain model, provide `.setType(Integer.class)`
(or the appropriate number class); compound models know their property type. Min/max are enforced by Wicket on the
server. **Step is a browser hint**, not a server validator: add a validator if alignment to the step is a domain rule.
A zero step renders Wicket's `step="any"` convention. Access the native component with `getInput()` for further options.

Native email, URL and number controls may perform browser validation before a submit. The showcase's **Inputs** page
uses `novalidate` on its forms so it can demonstrate Wicket's inline server errors, normal submission and Ajax focus
preservation. After an invalid Ajax submit the password stays in the existing browser input without being sent back
in the response; a full page render leaves it blank.
