---
parent: kid-canvas
prefix: CANVAS-PAINT
---

# Painting

## Context and Design Philosophy

Painting converts the single live pointer stream User Experience routes to it — per the User Experience LLD's Input Arbitration, Painting only ever receives a pointer that didn't land on any Widgets control, and only one live pointer at a time — into stroke data, renders it to the drawing surface as it happens, and holds the accumulated drawing until User Experience asks it to save or clear. Painting doesn't arbitrate input itself and doesn't know about Widgets; its only input is the single pointer stream it's handed.

This is the component the HLD's Compose Multiplatform sharing decision is about: the touch-to-stroke logic and its on-screen rendering are the same implementation across Android and Linux (and iOS later), not just specs kept in sync by convention.

## Stroke Model

A stroke is the record of one live pointer's down-to-up sequence: an ordered list of points, plus the color and brush active when the stroke began (color set by User Experience's color-selection wiring — see the User Experience LLD's Interaction Feedback; brush per Brushes below). Both are fixed for the stroke's whole duration. Changing the active color mid-stroke can't happen today anyway, since color selection is a Widgets control and Widgets can't receive input while a stroke is live (see Input Arbitration) — but even if it could, an in-progress stroke wouldn't change color or brush retroactively.

A stroke with only a single point — a tap with no movement — is still a stroke, not discarded: its brush renders it as a small mark, so any touch on the canvas leaves a visible trace.

The drawing is the ordered set of all strokes recorded since the canvas was last cleared.

## Brushes

A brush owns everything about how a stroke's captured points become rendered pixels: line width, shape, interpolation between points, and any other visual effect. Painting itself only captures point sequences and delegates their rendering to the stroke's brush — it holds no rendering logic of its own. A brush must be able to render incrementally, extending the visible stroke as each new point arrives, since Painting always renders progressively (see Rendering).

Exactly one brush exists today — a fixed-width solid line connecting points as a simple polyline, with no curve-fitting or smoothing — and nothing outside Painting can select a different one: there's no brush-picker control in Widgets, and no brush field in the shared UX configuration. Every stroke uses this single default. The brush concept exists in Painting's own architecture specifically so a second brush can be added later as a new implementation, without restructuring how strokes are captured or rendered; exposing brush choice as a product feature (a control, an age-gated bundle, or otherwise) is a separate, later decision — see Open Questions.

## Rendering

Painting renders progressively as a stroke is drawn — each new point extends the visible stroke as it arrives, via the stroke's brush, rather than waiting for pointer-up to render anything. The drawing surface's background is a visual-design decision, not fixed here.

## Save and Clear

Painting exposes three operations to User Experience, which alone decides when to call them (see the User Experience LLD's Lifecycle Behavior):

- `isEmpty()` — true only if no strokes have been recorded since the last clear.
- `save()` — renders the current drawing to a raster image and writes it to Image Storage as a new saved drawing (see the Image Storage LLD). Painting owns the write itself; it's the component with the direct relationship to Image Storage.
- `clear()` — discards all recorded strokes and resets the visible drawing surface to blank.

User Experience calls `isEmpty()` first, calls `save()` only if it returns false, then always calls `clear()`. Painting itself never decides whether to save — only whether there's anything to save.

The current, uncleared drawing must also survive the OS-managed lifecycle events described in the User Experience LLD (rotation, brief backgrounding, saved-instance-state-scoped process death) — that's a property of Painting's in-memory stroke state, not a call to `save()`, since routine lifecycle churn shouldn't produce a permanently saved drawing (see User Experience's OS Navigation and Process Lifecycle).

## Decisions & Alternatives

| Decision | Chosen | Alternatives Considered | Rationale |
|----------|--------|------------------------|-----------|
| Zero-movement tap | Recorded as a single-point stroke, rendered as a mark by its brush | Discard taps with no movement as not a real stroke | A toddler tapping the canvas expects a visible mark for any touch; discarding silent no-ops contradicts the immediate-response philosophy from the User Experience LLD. |
| Stroke rendering | Progressive, point-by-point as the stroke is drawn | Render only on pointer-up, once the full stroke is known | The toddler is actively touching the screen; waiting until lift to show anything would mean no visible response while they're mid-action. |
| Brush as a first-class, pluggable concept, with a single implementation and no selection UI | Painting's architecture defines a brush abstraction now; exactly one brush (fixed-width solid polyline, no smoothing) is implemented, and nothing exposes a way to choose between brushes | Hardcode the single line-rendering algorithm directly into Painting with no abstraction; build out a brush-selection UI now alongside the abstraction | Keeps adding a second brush later to a matter of a new implementation rather than restructuring Painting, without committing to any product surface (control, config field) before there's a real need for one. |
| Who writes a saved drawing to Image Storage | Painting itself, on a `save()` call from User Experience | User Experience writes to Image Storage directly, using drawing data pulled from Painting | Matches the Kid Canvas sub-HLD's system diagram, where Painting owns the "writes drawings" edge to the store, and keeps Image Storage access confined to the component that already owns the drawing data. |
| Saved-drawing format | A rendered raster image | Vector/stroke data | The HLD's Non-Goals exclude re-editing or undo history, so nothing ever needs to reconstruct strokes from a saved drawing — only display and delete it (see the Image Storage LLD). |

## Open Questions & Future Decisions

### Deferred

1. The default brush's line width and the drawing surface's background/appearance are visual-design decisions, not fixed here.
2. Whether the default brush needs path smoothing is deferred until its unsmoothed polyline is actually seen in practice.
3. Whether and how brush choice ever becomes user-facing — a Widgets control, an age-gated bundle entry, or otherwise — is not decided; today it's purely an internal architectural seam with a single implementation behind it.
4. How the shared Compose Multiplatform canvas embeds inside the native GTK+ shell on Linux is an open technical question at the HLD level, inherited here without further resolution.

## References

- Parent sub-HLD: `docs/intent/kid-canvas/kid-canvas-design.md` — defines Painting as converting a pointer/touch sequence into stroke data and rendering it.
- Root HLD: `docs/high-level-design.md` — Approach (Compose Multiplatform canvas sharing), Key Design Decisions (canvas implemented once, shared across platforms).
- Sibling: `docs/intent/kid-canvas/user-experience/user-experience-design.md` — Input Arbitration (source of Painting's single pointer stream), Lifecycle Behavior (when save/clear are called), OS Navigation and Process Lifecycle (saved-instance-state survival).
- `docs/intent/image-storage/image-storage-design.md` — saved-drawing storage shape and write API.
