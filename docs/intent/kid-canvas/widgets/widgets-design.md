---
parent: kid-canvas
prefix: CANVAS-WIDGETS
---

# Widgets

## Context and Design Philosophy

Widgets implements KidWidgets, the library of on-screen controls (buttons, color picker, and similar chrome) used everywhere on the kid canvas. Standard Compose gesture recognizers (`clickable()`, Material's ripple/gesture stack) assume adult motor control — a precise tap, a drag-cancel-on-outside-release convention, timing tuned for an adult's touch. Toddlers can't reliably produce that input, so every control here reads raw pointer events directly (`Modifier.pointerInput` / `awaitPointerEventScope`) and defines its own hit-testing and activation from scratch, rather than composing standard clickable modifiers.

Widgets doesn't decide *whether* a given touch is even its own to interpret — it just does its own ordinary hit-testing on whatever pointer Compose delivers to it. Keeping that pointer from ever reaching a control while some other gesture is live elsewhere on screen is User Experience's job, done by covering Widgets entirely for that duration rather than by Widgets checking any state of its own (see the User Experience LLD's Input Arbitration section). Widgets' own job is narrower: given a pointer, decide reliably which control (if any) it activates.

## Hit-Testing and Activation

Each control activates on pointer-down inside its hit region — not on pointer-up-within-bounds the way `clickable()` works. A toddler doesn't need to lift precisely inside the target the way a drag-cancel-on-outside-release convention would require; the moment a touch lands inside the region is itself the trigger, and the control's action fires immediately at that moment.

Only a pointer's initial down location is evaluated. Dragging an already-down pointer into a control's hit region never activates it — activation is decided once, at down, not continuously as the pointer moves. Symmetrically, once a pointer has activated a control at down, that pointer is consumed for the rest of its gesture: further movement or a delayed lift doesn't retrigger the control, and dragging that same pointer into a different control's region doesn't activate the second control either.

Hit regions are sized generously beyond each control's visible glyph, tolerant of imprecise placement — the visible control and its tappable area are not the same rectangle. Exact sizing is a visual-design decision, not fixed here.

Every control's hit-testing is ordinary Compose pointer dispatch — no custom pre-arbitration step decides whether a control is even offered a touch (see Interaction Arbitration Contract). A control claims whatever pointer lands inside its own region simply by being the topmost composable there.

## Reporting Press State

Each control instance takes an `onPressedChange: (Boolean) -> Unit` callback, invoked with `true` at the moment its own pointer activates it and `false` when that same pointer lifts. This is the control's entire externally-observable lifecycle signal: the `true` call both fires the control's action and marks a gesture as having started; the `false` call marks that gesture as ended. A composable has no persistent object identity to expose a `Flow` from, so a callback — the same shape Compose's own stateful controls (e.g. `Switch`'s `onCheckedChange`) use — is the idiomatic way to surface this, rather than the control returning or holding onto a reactive stream itself.

A control with multiple independent hit regions (Color Picker) takes one `onPressedChange` per region instance, since each swatch is its own independent activation target — the caller composing a given swatch already knows which color it represents, so nothing needs to flow back through the callback beyond the boolean itself.

## Control Catalog

Two controls exist today:

- **Button** — a single hit region, one activation action (e.g. New Picture).
- **Color Picker** — multiple hit regions, one per color swatch, each an independent activation target following the same hit-testing and activation rule as Button; activating a swatch selects that color.

Both are built on the same underlying raw-pointer hit-testing and activation primitive described above — Widgets doesn't have a separate mechanism per control type. A new control follows the same primitive without needing its own hit-testing logic.

## Interaction Arbitration Contract

Widgets doesn't arbitrate against Painting itself, and doesn't know Painting or strokes exist. Whether a given pointer even reaches a control at all is decided upstream, before Widgets' own hit-testing ever runs: User Experience composes a transparent, pointer-consuming layer over Widgets for the duration of any gesture Painting is holding (see the User Experience LLD's Input Arbitration section), so a pointer that would otherwise land inside a control's hit region never reaches it while a stroke is live. Because of that upstream blocking, Widgets never observes a second, concurrent pointer while it's tracking one — the re-trigger and cross-control drag rule above only has to reason about a single pointer's own events over time, never about competing simultaneous pointers. Widgets depends on nothing beyond the pointer events Compose's own hit-testing delivers to it and the `onPressedChange` callback it's given.

## Decisions & Alternatives

| Decision | Chosen | Alternatives Considered | Rationale |
|----------|--------|------------------------|-----------|
| Activation trigger | Pointer-down inside the hit region | Pointer-up-within-bounds (standard `clickable()` semantics); pointer-up regardless of drag position | Matches the toddler-usability tenet's rejection of drag-cancel/precise-release conventions — the touch itself is the signal, not a coordinated down-then-up-in-place gesture a toddler may not complete cleanly. |
| Re-trigger and cross-control drag handling | A pointer's control assignment (if any) is fixed at its own down event; it can never retrigger the same control or activate a different one afterward | Re-evaluate hit-testing continuously as the pointer moves, activating whichever control it's currently over | A single decide-once-at-down rule is simple to reason about and prevents a dragging or resting hand from firing a control repeatedly or "sliding" an activation from one control to another. |
| How Widgets learns it's blocked while another gesture is live | It isn't told anything — User Experience composes a pointer-consuming transparent layer over all of Widgets for the blocked duration, so a blocked pointer never reaches any control's own hit-testing at all | An explicit boolean `enabled` gate Widgets checks per pointer; Widgets observing Painting's stroke state directly | A covering layer needs no signal from Painting or User Experience to reach Widgets at all — every pointer Widgets' own hit-testing ever sees is unambiguously its own, exactly like a plain, unarbitrated Compose screen would see. |
| Hold/long-press activation | Not supported by any control today — every control is a single down-triggered tap | Support a hold/long-press variant for some controls | The HLD explicitly rejects a long-press requirement for toddler-facing controls, and no current control needs one. Not a permanent constraint on the library itself — a future control could still add one if a genuine need arises. |
| Press-lifecycle signal shape | A single `onPressedChange: (Boolean) -> Unit` callback per control instance, `true` at activation and `false` at that same pointer's lift | A `Flow<Boolean>` the control exposes; separate `onActivate`/`onReleased` callbacks | A composable is a function re-invoked on recomposition, with no persistent object identity to hang a `Flow` off of without extra caller-side machinery, so a callback is the idiomatic shape (matching Compose's own stateful controls, e.g. `Switch`'s `onCheckedChange`). One boolean callback covers both edges without introducing two separately-named events for what is, from Widgets' side, a single up/down lifecycle. |

## Open Questions & Future Decisions

### Deferred

1. Exact hit-region sizing/tolerance beyond the visible glyph is a visual-design decision, not fixed here.
2. Full control catalog beyond Button and Color Picker isn't enumerated ahead of need — new controls follow the existing primitive.

## References

- Parent sub-HLD: `docs/intent/kid-canvas/kid-canvas-design.md` — defines Widgets as implementing the KidWidgets library.
- Root HLD: `docs/high-level-design.md` — Approach (raw pointer input on kid canvas controls), Tenets (toddler usability over platform convention).
- Sibling: `docs/intent/kid-canvas/user-experience/user-experience-design.md` — Input Arbitration (the global arbiter Widgets is downstream of), Screen Composition (which controls are placed where).
