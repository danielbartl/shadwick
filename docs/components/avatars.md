# Avatars

An avatar displays a picture with an initials fallback while loading or after an image fails.

```java
add(new SwAvatar("initials", "AL").setLabel("Ada Lovelace"));
add(new SwAvatar("picture", pictureUrlModel, Model.of("AL"), SwAvatar.Size.SMALL)
        .setLabel("Ada Lovelace"));
```

```html
<span wicket:id="initials"></span>
<span wicket:id="picture"></span>
```

A `String` constructor means initials; an `IModel<String>` constructor means a picture URL. Sizes are `SMALL`,
`DEFAULT` and `LARGE`. `SwAvatar.initials(id, model)` binds changing initials; `initialsOf(name)` derives initials
from the first and last words. Models are read once per request and detached.

Use `setLabel(...)` for the person's full name. Without it, supplied initials name the fallback; a picture-only
avatar defaults to decorative. Do not repeat a full-name announcement if adjacent content already supplies the
same information unnecessarily. The external avatar runtime maintains the fallback after Ajax replacement.
Your application owns image URLs, access control and image CSP allowances; no external image provider is added.
