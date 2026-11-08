# Range field

`SwRangeField<N>` wraps Wicket's native `RangeTextField<N>` with a label, description and inline feedback.
It is a single-value slider. Use a numeric model whose value is initialized to a permitted step inside the chosen range.

```java
form.add(new SwRangeField<Integer>("priority", "Priority", Model.of(5))
        .setMin(0).setMax(10).setStep(1)
        .setDescription("0 is flexible; 10 is urgent."));
```

```html
<div wicket:id="priority"></div>
```

The defaults are minimum 0, maximum 100 and step 1, matching native HTML. Both bounds and step alignment
are checked on the server, including tampered submissions. Steps are measured from the minimum. For example,
minimum `-1.25` and step `0.5` allow `-0.75`, `-0.25` and `0.25`. Use `BigDecimal` for exact decimal steps.
A null bound/step value restores its default; a null model argument is rejected. `step="any"` is not supported.
Minimum must not exceed maximum and step must be positive; invalid model constraints fail when rendered/validated.

Bounds and step accept values or models. Constraint models are detached, including ones passed through
`getInput().setMinimum(...)`, `setMaximum(...)` or `setStep(...)`. Use `.setType(Integer.class)` for an empty
plain model; inherited property models already expose their type. An initialized value avoids the browser
choosing a midpoint that differs from the server's model. Range submissions use HTML machine numbers,
independent of the display locale.

The native slider supplies its accessible current value, keyboard handling, touch behavior, RTL direction
and form reset. Arrow keys change the value; Home and End reach its limits. Provide useful endpoint meaning
in the description. All nine skins use the shared semantic color tokens; this structure is new because Shadleaf
has no range component. Forced colors retain native system-control behavior. No script is required.

Use a number field alongside the slider when a user needs to type a precise amount, and manage synchronization
in your application. Multiple thumbs, a selected interval and a client-side live numeric output are outside this component.
