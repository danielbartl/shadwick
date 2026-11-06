# Meters

A meter describes a known measurement, such as storage use, within a range. Its low/high/optimum values describe
quality bands; use [progress](./progress.md) for an unfinished task or unknown work.

```java
add(new SwMeter("storage", storageModel).setMax(100).setLow(60).setHigh(85).setOptimum(20)
        .setLabel("Storage used"));
```

```html
<meter wicket:id="storage"></meter>
```

## Bounds and thresholds

The default bounds are 0 to 1. The full model constructor is `(id, value, min, max, low, high, optimum)`.
A null reading uses the minimum; finite readings are clamped. Nonfinite numbers, invalid bounds and inconsistent
thresholds fail clearly. Native `min`, `max`, `low`, `high` and `optimum` determine optimum, suboptimum and critical
bands with matching styles. Choose thresholds that reflect the meaning of the measurement.

`Sw.Behaviors.meter()` styles caller-owned native markup, preserving its bounds and values.

## Accessibility and integration

Provide a meaningful name with `setLabel(...)`, `aria-label`, `aria-labelledby` or an associated label. Include
units in nearby visible text when a number alone is ambiguous. Native meter markup works without JavaScript and
keeps machine-formatted numeric attributes in every locale. Update the model and refresh the output-ID component
when a measurement changes; measurement storage and refresh scheduling remain application-owned.
