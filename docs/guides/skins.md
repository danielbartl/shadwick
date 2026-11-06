# Skins

| Skin | Look |
| --- | --- |
| `Skin.VEGA` | Classic spacing, 36px default controls and subtle control shadows. The default. |
| `Skin.LYRA` | Square corners, small type and thin focus rings. |
| `Skin.NOVA` | Compact 32px controls, rounder fields, tighter spacing and muted card/dialog footers. |
| `Skin.MAIA` | Pill-shaped controls, large radii and generous spacing. |
| `Skin.MIRA` | Dense 28px controls, small type and tighter menus/tables. |
| `Skin.LUMA` | Soft rounded shapes, borderless fields and luminous shadows. |
| `Skin.RHEA` | Luma's soft surfaces with compact 32px controls and tighter spacing. |
| `Skin.SERA` | Editorial square geometry, uppercase labels and underlined 40px fields. |
| `Skin.SKETCH` | Hand-drawn wireframes with uneven corners, pencil borders and Balsamiq Sans. |

Each skin has complete standalone and embedded bundles, including minified versions, and supports light/dark
appearance with the same markup and color tokens. Existing keyboard, forced-color and reduced-motion behavior
is shared. Mira intentionally uses smaller mobile type for dense interfaces. Pick a skin with
`Shadwick.install(this).setSkin(Skin.MAIA)`; the optional `SkinStore` supports per-user selection.
The showcase and landing studio include all nine skin choices.

Sketch bundles Balsamiq Sans (regular/bold Latin WOFF2, SIL Open Font License) in the jar. Font requests stay on
that application's origin; no external font service or CSP change is needed. The standalone bundle gives the
page its handwriting font. The embedded bundle scopes handwriting to `sw-` components, preserving the host's
page typography. Apps copying the CSS elsewhere must also copy its `../fonts/balsamiq-sans/` assets and license.
