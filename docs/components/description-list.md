# Description lists

`SwDescriptionList` shows a record’s fields as native `dl`/`dt`/`dd` relationships, with labels beside values when
the list is at least 28rem wide and above them in narrower containers. It follows Wicket Oat’s `addItem` and
`addProperty` API. Attach it to a `dl`, using the markup below.

## Model-backed record details

```java
var details = new SwDescriptionList("details", customerModel)
        .addProperty("name")
        .addProperty("address.city")
        .addItem("Balance", balanceModel)
        .addItem(Model.of("Status"), id -> new SwBadge(id, "Active"));
add(details);
```

```html
<h2 id="details-heading">Customer details</h2>
<dl wicket:id="details" aria-labelledby="details-heading"></dl>
```

The record model may be an ordinary model or `CompoundPropertyModel`. `addProperty("address.city")` reads that
path from the component’s current model object. Its label comes from the resource key `address.city`, falling
back to the property path. Use `addItem(IModel<String>, IModel<?>)` for a translated label independent of a property.

String labels and values are escaped. Null or blank values display `—`. Number values use locale grouping and
retain fractional values, including `BigDecimal` precision; this is general number formatting, not currency
formatting. Supply a formatted string model or a custom value component for application-specific money/units.
Local dates use the locale’s medium date style, local times use its short time style and date-times combine both.
Instant, offset and zoned date-times display in UTC by default; `setTimeZone(ZoneId)` chooses another zone.
Local calendar and wall-clock values do not shift. Other values use Wicket’s registered converters.

Each value model is read once per request and detached at the end, including hidden rows. Normal Wicket ownership
applies to the record, label and custom-component models. Serializable factories and callback captures follow the
[Wicket integration](../guides/wicket-integration.md) rules.

## Composed values and updates

The custom-component overload takes `SerializableFunction<String, ? extends Component>`. Use the supplied
`SwDescriptionList.VALUE_ID` (`value`); a null component or a mismatching ID is rejected. The component renders on
a `span`. A badge or label can use that span directly; for a link or richer markup, return a Panel whose own markup
contains the native anchor/content. The `/record-display` example includes a `ContactLink` panel and its source.

The list has an output markup ID, so `target.add(details)` updates current property values and label resources.
Changing a record model does not require rebuilding the field declarations. Append additional `addItem` rows
before rendering; generated row IDs are not an integration contract.

## Caller-owned markup

`Sw.Behaviors.descriptionList()` applies the same styling to a native `dl`. Supply `sw-description-list-item` rows
with one `dt` and one `dd`, and let ordinary Wicket components own their values.

```html
<dl wicket:id="details">
  <div class="sw-description-list-item"><dt>Customer</dt><dd>Fern &amp; Field</dd></div>
</dl>
```

The component adds no roles or tab stops. Native links and controls inside values keep their usual keyboard
behavior. Text wraps even for long unbroken identifiers. It needs no JavaScript, retains the relationships for
screen readers, follows logical text direction and avoids splitting individual rows when printed. Skin-specific
type, border weight and spacing use the existing theme tokens.

For the accompanying account history, see [Timelines](timeline.md).
