---
parent: kid-canvas
prefix: CANVAS-PAINT
---

# Painting

## Context and Design Philosophy

Painting converts the live pointer stream(s) User Experience routes to it — per the User Experience LLD's Input Arbitration, Painting only ever receives a pointer that didn't land on any Widgets control — into stroke data, renders it to the drawing surface as it happens, and holds the accumulated drawing until User Experience asks it to save or clear. Painting doesn't arbitrate input itself and doesn't know about Widgets; its only input is whatever pointer stream it's handed.

Today that's a single pointer at a time — User Experience's arbitration rule hands Painting one live gesture and drops every other concurrent pointer before Painting ever sees it. That's a deliberate simplification for a minimum testable product, not a permanent architectural limit; see Open Questions.

This is the component the HLD's Compose Multiplatform sharing decision is about: the touch-to-stroke logic and its on-screen rendering are the same implementation across Android and Linux (and iOS later), not just specs kept in sync by convention.

## Stroke Model

A stroke is the record of one live pointer's down-to-up sequence: an ordered list of points, plus the color and brush active when the stroke began (read from Active Stroke Settings below). Both are fixed for the stroke's whole duration. Changing the active color mid-stroke can't happen today anyway, since color selection is a Widgets control and Widgets can't receive input while a stroke is live (see Input Arbitration) — but even if it could, an in-progress stroke wouldn't change color or brush retroactively.

A stroke with only a single point — a tap with no movement — is still a stroke, not discarded: its brush renders it as a small mark, so any touch on the canvas leaves a visible trace.

A point is stored as a fraction of the drawing surface's width and height at the moment it was captured (not a raw pixel coordinate), converted to actual pixels only when rendering, against whatever the canvas's current size happens to be. This is what lets a drawing captured before a resize (the drawing surface's dimensions changing — most commonly a device rotation, within the same lifecycle-survival scope described under Save and Clear) keep rendering correctly afterward, without Painting needing to detect the resize and rewrite already-captured points itself.

The drawing is the ordered set of all strokes recorded since the canvas was last cleared.

## Active Stroke Settings

Painting doesn't own or compute its active color or brush — it reads both from Active Stroke Settings, a resolved-value interface it queries once at the moment a stroke begins: `get_resolved_color()`/`get_resolved_brush()`-shaped accessors, mirroring the separation the Config LLD's Resolved Features draws between raw state and the value a consumer actually needs. Painting depends only on that interface, not on how a resolved value is produced. Whether it reflects a toddler's last swatch tap, or, per a future product decision, something that changes on its own between strokes without any tap at all, is entirely Active Stroke Settings' concern — implemented and owned outside Painting, not decided here.

Painting queries both accessors exactly once per stroke, at the down event that begins it, and holds the returned values fixed for that stroke's whole duration regardless of any later change at the source — the same fixed-for-duration guarantee as today, now stated as a property of when Painting reads the interface rather than of when the interface's own value happens to change.

## Brushes

A brush owns everything about how a stroke's captured points become rendered pixels: line width, shape, interpolation between points, and any other visual effect. Painting itself only captures point sequences and delegates their rendering to the stroke's brush — it holds no rendering logic of its own. A brush must be able to render incrementally, extending the visible stroke as each new point arrives, since Painting always renders progressively (see Rendering).

Brush is a plain Kotlin interface internal to Painting — a pluggable rendering strategy, not a cross-platform data-layer boundary like Config or Image Storage's shared interfaces, since Painting itself is shared code with no per-platform variation. The interface exists and is enforced as a real contract from the start, independent of how many implementations currently satisfy it.

Exactly one brush exists today — a fixed-width solid line connecting points as a simple polyline, with no curve-fitting or smoothing — and nothing outside Painting can select a different one: there's no brush-picker control in Widgets, and no brush field in the shared UX configuration. Every stroke uses this single default. The brush concept exists in Painting's own architecture specifically so a second brush can be added later as a new implementation, without restructuring how strokes are captured or rendered; exposing brush choice as a product feature (a control, an age-gated bundle, or otherwise) is a separate, later decision — see Open Questions.

## Rendering

Painting renders progressively as a stroke is drawn — each new point extends the visible stroke as it arrives, via the stroke's brush, rather than waiting for pointer-up to render anything. The drawing surface's background is a visual-design decision, not fixed here.

## Save and Clear

Painting exposes three operations to User Experience, which alone decides when to call them (see the User Experience LLD's Lifecycle Behavior):

- `isEmpty()` — true only if no strokes have been recorded since the last clear.
- `save()` — renders the current drawing to a raster image and writes it to Image Storage as a new saved drawing (see the Image Storage LLD). Painting owns the write itself; it's the component with the direct relationship to Image Storage.
- `clear()` — discards all recorded strokes and resets the visible drawing surface to blank.

User Experience calls `isEmpty()` first, calls `save()` only if it returns false, then always calls `clear()`. Painting itself never decides whether to save — only whether there's anything to save.

If the write to Image Storage fails, `save()` reports that failure to its own caller rather than treating the drawing as saved — a caller that isn't told a save failed can't retry or warn a parent, and User Experience's own auto-save-then-clear sequence must not clear a drawing it never actually persisted.

`clear()` is only ever called when no stroke is active, per User Experience's Input Arbitration — but it's well-defined even if that guarantee didn't hold: a stroke still in progress (the pointer hasn't lifted) is finalized as it currently stands, discarded along with everything else, and immediately replaced by a new stroke continuing from the same pointer location. This is deliberately *not* the same as a real lift-and-touch-again: the replacement stroke carries forward the interrupted stroke's own color and brush rather than querying Active Stroke Settings afresh, since the pointer never actually lifted and nothing about the toddler's held touch changed. This keeps a still-active touch tracked, with its settings undisturbed, rather than silently dropped or arbitrarily re-colored. One consequence worth naming: `isEmpty()` called right after such a mid-stroke `clear()` returns false, since that replacement stroke already has one point — the same zero-movement-tap rule from Stroke Model.

## Lifecycle Survival

The current drawing must also survive the OS-managed lifecycle events described in the User Experience LLD (rotation, brief backgrounding, saved-instance-state-scoped process death) — but what "survive" requires differs by whether the process itself actually ends.

When the OS recreates the UI without ending the process (a configuration change such as rotation, or backgrounding that doesn't reclaim memory), the same in-memory Painting state — completed strokes and any stroke still in progress — simply keeps existing across the recreation. Nothing needs to be written down and read back, since the process never stopped.

When the OS ends the process and later recreates it within its saved-instance-state mechanism, nothing in memory survives, including any pointer sequence that was live at the moment of death: the OS doesn't replay touch events into the recreated process, so an in-progress stroke has no surviving counterpart to resume and is simply absent afterward, as if it had never begun. What does survive is every stroke that had already completed (pointer lifted) before the process ended, restored from whatever was written into the OS's saved-instance-state mechanism before it died.

Either way, restoring the drawing never invokes `save()` — routine lifecycle churn shouldn't produce a permanently saved drawing; only calling `save()` does that.

## Decisions & Alternatives

| Decision | Chosen | Alternatives Considered | Rationale |
|----------|--------|------------------------|-----------|
| Zero-movement tap | Recorded as a single-point stroke, rendered as a mark by its brush | Discard taps with no movement as not a real stroke | A toddler tapping the canvas expects a visible mark for any touch; discarding silent no-ops contradicts the immediate-response philosophy from the User Experience LLD. |
| Stroke rendering | Progressive, point-by-point as the stroke is drawn | Render only on pointer-up, once the full stroke is known | The toddler is actively touching the screen; waiting until lift to show anything would mean no visible response while they're mid-action. |
| Brush as a first-class, pluggable concept, with a single implementation and no selection UI | Painting's architecture defines a brush abstraction now; exactly one brush (fixed-width solid polyline, no smoothing) is implemented, and nothing exposes a way to choose between brushes | Hardcode the single line-rendering algorithm directly into Painting with no abstraction; build out a brush-selection UI now alongside the abstraction | Keeps adding a second brush later to a matter of a new implementation rather than restructuring Painting, without committing to any product surface (control, config field) before there's a real need for one. |
| Who writes a saved drawing to Image Storage | Painting itself, on a `save()` call from User Experience | User Experience writes to Image Storage directly, using drawing data pulled from Painting | Matches the Kid Canvas sub-HLD's system diagram, where Painting owns the "writes drawings" edge to the store, and keeps Image Storage access confined to the component that already owns the drawing data. |
| Saved-drawing format | A rendered raster image | Vector/stroke data | The HLD's Non-Goals exclude re-editing or undo history, so nothing ever needs to reconstruct strokes from a saved drawing — only display and delete it (see the Image Storage LLD). |
| Source of a stroke's active color and brush | Painting queries Active Stroke Settings' resolved accessors (`get_resolved_color()`/`get_resolved_brush()`-shaped) once at each stroke's start | Painting holds active color/brush as its own mutable state, set via a setter call from User Experience | Keeps whatever decides the value — today's manual swatch selection, or any future automatic behavior — entirely outside Painting, mirroring Config's raw/resolved separation. Painting only ever needs the momentary resolved value, not how it was derived. |
| A point's coordinate representation | A fraction of the drawing surface's width/height at capture time, converted to pixels only at render time | A raw pixel coordinate, fixed at capture time | A drawing surface can resize within the scope Lifecycle Survival already promises to preserve across (most commonly a device rotation). Fractional coordinates keep rendering correctly against whatever size the canvas currently is, with no explicit rescale step; raw pixels captured before a resize would render at the wrong position afterward unless Painting detected the resize and rewrote every stored point itself. |
| `save()` failure handling | Reports the failure to its own caller rather than treating the drawing as saved | Silently behave as if the write succeeded | Matches Image Storage's own contract of reporting failures rather than crashing; a caller not told a save failed can't retry or warn a parent, and it must not clear a drawing that was never actually persisted. |
| `clear()` called while a stroke is live | Finalize the in-progress stroke as-is, discard it with everything else, then immediately start a new stroke continuing from the same pointer location and carrying forward the same color and brush | Silently drop the live stroke and stop tracking that pointer; reject/ignore the call while a stroke is live; treat it exactly like a real lift-and-touch-again, re-querying Active Stroke Settings for the replacement stroke | Today's Input Arbitration guarantees this never happens, but a well-defined behavior costs little to specify and avoids leaving a still-active touch untracked if that guarantee ever changes. Re-querying would risk an arbitrarily different color/brush mid-gesture even though the toddler's hand never left the screen — the pointer didn't actually lift, so nothing about the held touch should change. |
| Lifecycle survival scope | Split into two specs: full state (including a live stroke) survives in-process UI recreation for free via plain object retention; only already-completed strokes survive actual process death | One combined spec covering both configuration change and process death uniformly | A live stroke's pointer-down event has no surviving counterpart after a real process restart — the OS doesn't replay touch input into the recreated process — so a single spec promising to preserve it there would describe a scenario the platform can't actually deliver. Splitting keeps each spec's guarantee honest about what's actually possible in each case. |

## Open Questions & Future Decisions

### Deferred

1. Whether Painting should ever track multiple concurrent live strokes (true multi-touch drawing) is open. Today it only ever receives one live pointer, because User Experience's arbitration hands it a single gesture and drops the rest — chosen for implementation simplicity in a minimum testable product, not because multi-touch drawing is undesirable. Supporting it later would mean Painting tracking several in-progress strokes at once, keyed by pointer, not just relaxing the upstream arbitration rule.
2. The default brush's line width and the drawing surface's background/appearance are visual-design decisions, not fixed here.
3. Whether the default brush needs path smoothing is deferred until its unsmoothed polyline is actually seen in practice.
4. Whether and how brush choice ever becomes user-facing — a Widgets control, an age-gated bundle entry, or otherwise — is not decided; today it's purely an internal architectural seam with a single implementation behind it.
5. How the shared Compose Multiplatform canvas embeds inside the native GTK+ shell on Linux is an open technical question at the HLD level, inherited here without further resolution.
6. User Experience owns and implements Active Stroke Settings (see its LLD's Interaction Feedback). Whether a resolved value can ever change on its own between strokes, without a new toddler tap, is that LLD's open question, not Painting's — Painting's read-once-per-stroke behavior already accommodates it either way.
7. Fractional point coordinates keep a drawing rendering correctly across a resize that scales width and height proportionally, but not across one that changes the drawing surface's aspect ratio (e.g. a portrait-to-landscape rotation) — a drawn circle would stretch into an oval rather than staying round. Whether that's acceptable, or the surface should instead letterbox, crop, or something else on an aspect-ratio-changing resize, isn't decided here.
8. Restoring completed strokes after process death needs to reconstruct each stroke's brush, not just its points and color. With exactly one brush implementation today, restoration can always reconstruct that same default brush without recording which brush a stroke used. How a saved stroke identifies its brush once a second brush exists — alongside the still-open question of how brush choice becomes user-facing at all (#4) — isn't decided here.

## References

- Parent sub-HLD: `docs/intent/kid-canvas/kid-canvas-design.md` — defines Painting as converting a pointer/touch sequence into stroke data and rendering it.
- Root HLD: `docs/high-level-design.md` — Approach (Compose Multiplatform canvas sharing), Key Design Decisions (canvas implemented once, shared across platforms).
- Sibling: `docs/intent/kid-canvas/user-experience/user-experience-design.md` — Input Arbitration (source of Painting's single pointer stream), Lifecycle Behavior (when save/clear are called), OS Navigation and Process Lifecycle (saved-instance-state survival), Interaction Feedback (owns Active Stroke Settings' color-selection wiring).
- `docs/intent/image-storage/image-storage-design.md` — saved-drawing storage shape and write API.
