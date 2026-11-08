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

## Avatar groups

`SwAvatarGroup<T>` follows Wicket Oat’s repeat-and-populate API. It owns the `avatars` ListView and styles the
group; your markup and callback supply each avatar and any native links/buttons around it.

```java
add(SwAvatarGroup.of("team", members, SwAvatarGroup.Size.DEFAULT,
        (item, person) -> item.add(new SwAvatar("avatar", person.initials())
                .setLabel(person.fullName())))
        .setLabel("Project team"));
```

```html
<ul wicket:id="team">
  <li wicket:id="avatars"><span wicket:id="avatar"></span></li>
</ul>
```

Constructors accept the ID, `IModel<List<T>>` and optional `SwAvatarGroup.Size`. Subclasses implement
`populateItem(ListItem<T>)`. Both `of(...)` overloads also have `Sw.Components.avatarGroup(...)` equivalents,
with a serializable callback receiving the item and its object. The model is required; a null list renders
empty. Its contents are copied once per request, retaining supplied order, and the source model detaches even
while the group is hidden. Item visibility and disabled actions remain application-owned.

`SMALL`, `DEFAULT` and `LARGE` govern all nested avatars, including children created with a different
`SwAvatar.Size`. They use the existing 24px, 32px and 40px skin dimensions and small/default text sizes. Change
the size with `setSize(...)`. Logical overlap follows LTR/RTL automatically. Large collections wrap with each
row starting at the same logical edge, and a focused member rises above its neighbors. Shapes and skin rings,
including Sketch’s hand-drawn outline, remain in use. The group adds no script or dependency; pictures retain
the existing avatar loading/error behavior, including cached images after Ajax replacement.

## Names, actions and overflow

Name a group through `setLabel(String/IModel<String>)` or application-owned `aria-labelledby`. A list root keeps
explicit `role="list"` for browsers that otherwise suppress native list semantics with `list-style: none`;
other roots default to `role="group"`. Explicit roles are preserved. Label text is escaped and its model detaches.
There is no automatic live region or focus move when the list changes; use an application status receipt if an
update needs an announcement. For Ajax updates, give the group an output markup ID or update a containing
component that has one, as the example does. Re-rendering repopulates the current member list.

For interactive members, put an ordinary named Wicket link or button directly inside the repeated item. Its
accessible name should describe its action and include visible initials, such as “View AL: Ada Lovelace.”
Keeping the initials in the name supports voice input and the label-in-name requirement. Mark its nested avatar panel
`aria-hidden="true"` when the control supplies that full name to avoid duplicate image announcements. Use normal
Tab/Shift+Tab and Enter/Space behavior for the control type; the group adds no toolbar role or arrow-key navigation.
Focus outlines remain visible above neighboring avatars, including forced colors. Do not replace actionable
links with click handlers on an image or list item.

The group renders the supplied list. Choose any visible subset and compute overflow counts in application code,
including authorization and localization. Add an extra `sw-avatar-group-item` wrapper after the repeated items
for an overflow action. A native link/button can contain a `sw-avatar sw-avatar-group-count` span with visible
`+2` text and `aria-hidden="true"`; name the action “Show 2 more members (+2).” A read-only count should instead have
its own complete accessible name. The group does not fetch hidden members or promise that a count is a full roster.
The `/avatar-groups` example implements a working native overflow link and member callbacks, all three sizes,
an empty-state message, Ajax clear/restore/replacement and a twenty-member wrapping group. `/display` keeps the
standalone avatar examples. Native member links and overflow work without JavaScript.

## Fixed markup

For a fixed cluster, attach `AvatarGroupBehavior` through `Sw.Behaviors.avatarGroup()` and give each direct
member the `sw-avatar-group-item` class. Place a normal `SwAvatar` or named link/button inside each wrapper.
The behavior supports the same `setSize(...)` and detachable `setLabel(...)`; population and model ownership
remain with your components. Keep the root’s HTML list/group semantics consistent with its member markup.
