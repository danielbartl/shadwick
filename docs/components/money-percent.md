# Money and percentage fields

`SwMoneyField` and `SwPercentField` bind exact `BigDecimal` values to localized text inputs. They share
[labels, descriptions, validators and inline Ajax feedback](forms.md), plus input-group prefixes/suffixes.
They use `type="text"` and `inputmode="decimal"`; the browser does not convert the localized text for you.

```java
form.add(new SwMoneyField("budget", "Budget", Model.of(new BigDecimal("1250.00")))
        .setCurrency(Currency.getInstance("EUR"))
        .setMin(BigDecimal.ZERO));
form.add(new SwPercentField("tax", "Tax rate", Model.of(new BigDecimal("19")))
        .setFractionDigits(2).setMin(BigDecimal.ZERO).setMax(new BigDecimal("100")));
```

```html
<div wicket:id="budget"></div>
<div wicket:id="tax"></div>
```

Without an explicit model these fields inherit `CompoundPropertyModel` properties. Labels also accept
models or fall back to the field ID's resource key. Use `setRequired(true)` when an empty value is invalid.

## Formatting and conversion

Money displays the currency's number of fractional digits: EUR has two, JPY zero. Without a currency,
or for a pseudo-currency without a fraction count, it uses two. A currency or currency model controls the
symbol and locale-dependent addon placement. `setPrefix(...)` and `setSuffix(...)` override those addons;
pass a typed null to hide one. Currency models are detached with the component.

Percentages store the displayed number: **19 means 19%, not 0.19**. They show up to two fractional digits
by default. `setFractionDigits(3)` changes the limit; negative limits are rejected. Both fields round input
with half-even rounding before bound validators run. Thus `12.345` becomes `12.34` at two places, and
`12.355` becomes `12.36`. No conversion goes through a floating-point amount.

Use the component's Java locale: for example `1,234.50` in US English and `1.234,50` in German. Grouping
is optional, but if present it must have complete groups. French grouping accepts ordinary, nonbreaking and
narrow nonbreaking spaces. A single optional currency symbol/code or percent sign is accepted at either edge,
including beside a minus sign. Embedded/repeated units, malformed grouping, exponent notation and trailing
text are rejected. An incomplete number such as `12.` is rejected; use `12` or `12.0`.
The configured currency's symbols follow the running JDK's locale data.

`setMin(BigDecimal)` and `setMax(BigDecimal)` enforce inclusive bounds on the converted value. Repeated calls
replace the previous bound; a typed null clears it. Neither field assumes that money or percentages must be
positive, nor that percentages must stop at 100. Add the domain bounds your application needs.

The converter error messages ship in all nine component languages and can be overridden through normal
Wicket resource lookup (`IConverter.BigDecimal`). `BaseSwDecimalField` is the shared extension point for
other localized decimal units; override fraction limits and optional input-unit stripping.
