# Brand

Kinderdraw draws its accent colors from the shared **Your Average Chris**
brand palette — the same palette Trackr uses. That palette is shared Your
Average Chris brand material, reproduced here for now; once it moves to its
shared home this file will link out to it instead. Treat this document as the
single source of truth for brand color within kinderdraw — define values
here, and reference this file everywhere else.

## Palette

| Name | Hex | Role |
|---|---|---|
| Brand light blue | `#47AADC` | Gradient start / accent |
| Brand dark navy | `#04325C` | Gradient end / primary brand seed |
| Brand yellow | `#FCD214` | Highlight — the one branded detail meant to draw the eye, not a fill area |
| Brand darker yellow | `#EBC413` | The brand yellow at 92% brightness (V), same hue and saturation — for warm accents on light surfaces where the bright yellow washes out |
| Brand green | `#148244` | Positive/privacy accent |

## Kinderdraw's use of the palette

Unlike Trackr, kinderdraw doesn't have a mark of its own built from this
palette yet — no gradient, no distinguishing per-app treatment. Today's only
use is New Picture's sun icon, in brand yellow, on the button's own neutral
gray chrome (see the Widgets LLD's Control Catalog) — per the palette table
above, yellow specifically stays a highlight rather than a fill; that's not
a constraint on the rest of the palette, which later controls are free to
use more broadly (a fill, a background, whatever the control calls for).

## Keeping copies in sync

Markdown can't be imported by Kotlin, so hex values are physically duplicated
at each place that renders them. Every such copy carries a `docs/brand.md`
pointer in a nearby comment, so:

```
rg docs/brand.md
```

lists every file that must be updated by hand when a brand color changes.
Change the value here first, then walk that list. The copies that hold
literal hex:

- `shared/src/commonMain/kotlin/net/clahey/kinderdraw/shared/userexperience/KidCanvasScreen.kt` — New Picture's sun-icon tint
