---
parent: kid-canvas
prefix: CANVAS-WIDGETS
---

# Widgets

## Context and Design Philosophy

Widgets implements KidWidgets, the library of on-screen controls (buttons, color picker, and similar chrome) used everywhere on the kid canvas. Standard Compose gesture recognizers (`clickable()`, Material's ripple/gesture stack) assume adult motor control — a precise tap, a drag-cancel-on-outside-release convention, timing tuned for an adult's touch. Toddlers can't reliably produce that input, so every control here reads raw pointer events directly (`Modifier.pointerInput` / `awaitPointerEventScope`) and defines its own hit-testing and activation from scratch, rather than composing standard clickable modifiers.

Widgets doesn't decide *whether* a given touch is even its own to interpret — User Experience arbitrates that upstream, handing Widgets a pointer only when it lands inside some control's hit region and no other gesture is currently live elsewhere on screen (see the User Experience LLD's Input Arbitration section). Widgets' own job is narrower: given a pointer it's already been handed, decide reliably which control (if any) it activates.

## Hit-Testing and Activation

Each control activates on pointer-down inside its hit region — not on pointer-up-within-bounds the way `clickable()` works. A toddler doesn't need to lift precisely inside the target the way a drag-cancel-on-outside-release convention would require; the moment a touch lands inside the region is itself the trigger, and the control's action fires immediately at that moment.

Only a pointer's initial down location is evaluated. Dragging an already-down pointer into a control's hit region never activates it — activation is decided once, at down, not continuously as the pointer moves. Symmetrically, once a pointer has activated a control at down, that pointer is consumed for the rest of its gesture: further movement or a delayed lift doesn't retrigger the control, and dragging that same pointer into a different control's region doesn't activate the second control either.

Hit regions are sized generously beyond each control's visible glyph, tolerant of imprecise placement — the visible control and its tappable area are not the same rectangle. Exact sizing is a visual-design decision, not fixed here.

## Control Catalog

Two controls exist today:

- **Button** — a single hit region, one activation action (e.g. New Picture).
- **Color Picker** — multiple hit regions, one per color swatch, each an independent activation target following the same hit-testing and activation rule as Button; activating a swatch selects that color.

Both are built on the same underlying raw-pointer hit-testing and activation primitive described above — Widgets doesn't have a separate mechanism per control type. A new control follows the same primitive without needing its own hit-testing logic.

## Interaction Arbitration Contract

Widgets doesn't arbitrate against Painting itself. User Experience owns a single global arbiter (see the User Experience LLD's Input Arbitration section): at most one pointer's gesture is live on the whole screen at a time, and Widgets is only ever handed a pointer-down that User Experience has already determined lands inside some control's hit region with no other gesture currently live. Because of that upstream arbitration, Widgets never observes a second, concurrent pointer while it's tracking one — the re-trigger and cross-control drag rule above only has to reason about a single pointer's own events over time, never about competing simultaneous pointers. Widgets doesn't know about Painting or strokes at all; it depends on nothing beyond the pointer it's handed.

## Decisions & Alternatives

| Decision | Chosen | Alternatives Considered | Rationale |
|----------|--------|------------------------|-----------|
| Activation trigger | Pointer-down inside the hit region | Pointer-up-within-bounds (standard `clickable()` semantics); pointer-up regardless of drag position | Matches the toddler-usability tenet's rejection of drag-cancel/precise-release conventions — the touch itself is the signal, not a coordinated down-then-up-in-place gesture a toddler may not complete cleanly. |
| Re-trigger and cross-control drag handling | A pointer's control assignment (if any) is fixed at its own down event; it can never retrigger the same control or activate a different one afterward | Re-evaluate hit-testing continuously as the pointer moves, activating whichever control it's currently over | A single decide-once-at-down rule is simple to reason about and prevents a dragging or resting hand from firing a control repeatedly or "sliding" an activation from one control to another. |
| How Widgets learns it's blocked while another gesture is live | It isn't told anything — it simply never receives that pointer; User Experience's arbiter routes each live pointer to exactly one claimant | An explicit boolean `enabled` gate Widgets checks per pointer; Widgets observing Painting's stroke state directly | Routing a pointer only to its claimant, rather than dispatching it everywhere and gating downstream, means Widgets needs no signal from Painting or User Experience at all — every pointer it sees is unambiguously its own. |
| Hold/long-press activation | Not supported by any control today — every control is a single down-triggered tap | Support a hold/long-press variant for some controls | The HLD explicitly rejects a long-press requirement for toddler-facing controls, and no current control needs one. Not a permanent constraint on the library itself — a future control could still add one if a genuine need arises. |

## Open Questions & Future Decisions

### Deferred

1. Exact hit-region sizing/tolerance beyond the visible glyph is a visual-design decision, not fixed here.
2. Full control catalog beyond Button and Color Picker isn't enumerated ahead of need — new controls follow the existing primitive.

## References

- Parent sub-HLD: `docs/intent/kid-canvas/kid-canvas-design.md` — defines Widgets as implementing the KidWidgets library.
- Root HLD: `docs/high-level-design.md` — Approach (raw pointer input on kid canvas controls), Tenets (toddler usability over platform convention).
- Sibling: `docs/intent/kid-canvas/user-experience/user-experience-design.md` — Input Arbitration (the global arbiter Widgets is downstream of), Screen Composition (which controls are placed where).
