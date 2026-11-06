# Ratings

Use `SwRating` to display an aggregate score and `SwRatingField` to collect a whole-star answer in a Wicket form.

```java
add(new SwRating("average", Model.of(4.3)));
form.add(new SwRatingField("rating", "Your rating", ratingModel).setRequired(true));
```

```html
<span wicket:id="average"></span>
<div wicket:id="rating"></div>
```

## Display values

Read-only ratings round to the nearest half-star; 4.3 displays 4½ stars. Null means not rated; zero displays a
zero-star rating. The SVG display mirrors half-stars for right-to-left documents and supplies localized text.

## Editable values

Both components support `setMax(1..10)`. The input binds `Integer` and uses native radio buttons with the standard
field label, description and inline feedback. It supports required/disabled states, ordinary/Ajax form submission
and `CompoundPropertyModel`. Its native child is `rating:input`; submitted values are star numbers `1` through
`max`. Unknown values are rejected rather than clearing or overwriting the model. Access `getInput()` for Wicket
choice-updating behavior. The read-only component is display content and does not submit a value.

## Accessibility and integration

Give the editable field a visible question/label; use its description for instructions that need more context.
Native radio keyboard navigation works without JavaScript. Keep the rating's meaning in text when context matters,
such as an average based on a particular number of reviews. The display uses SVG outlines and shared forced-color
and reduced-motion behavior. Refresh the component over Ajax when its application model changes.
