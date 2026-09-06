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
| Brand light navy | `#0757A1` | The brand dark navy at 175% brightness (V), same hue and saturation — the light end of the launcher icon's field gradient |
| Brand yellow | `#FCD214` | Highlight — the one branded detail meant to draw the eye, not a fill area |
| Brand darker yellow | `#EBC413` | The brand yellow at 92% brightness (V), same hue and saturation — for warm accents on light surfaces where the bright yellow washes out |
| Brand green | `#148244` | Positive/privacy accent |

## Kinderdraw's use of the palette

Kinderdraw's mark is the app icon: a childlike scribble in brand light blue and
brand green ending in a brand yellow dot, on a field that ramps from brand light
navy down to brand dark navy. The Publishing LLD's Store Listing Assets says
what the mark means and which of its properties are load-bearing.

The only other use so far is New Picture's sun icon, in brand yellow, on the
button's own neutral gray chrome (see the Widgets LLD's Control Catalog) — per
the palette table above, yellow specifically stays a highlight rather than a
fill; that's not a constraint on the rest of the palette, which later controls
are free to use more broadly (a fill, a background, whatever the control calls
for).

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
- `androidApp/src/main/res/drawable/ic_launcher_background.xml` — the launcher icon's field
- `androidApp/src/main/res/drawable/ic_launcher_foreground.xml` — the launcher icon's scribble and dot
- `docs/store-listing/icon-512.svg` — the same mark again, as the Play store icon's master

`ic_launcher_monochrome.xml` is deliberately absent: the themed-icon layer is
tinted by the system, so it holds no brand hex to keep in sync. It does share
the foreground's path geometry — see the Publishing LLD's Store Listing Assets.
