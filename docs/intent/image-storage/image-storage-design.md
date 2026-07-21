---
parent: high-level-design
prefix: IMAGES
---

# Image Storage

## Context and Design Philosophy

Image Storage holds saved drawings — the record a parent browses and manages from the Companion App, and the destination Painting writes a finished drawing to. It's shared in-process by the Android build's Kid Canvas and Companion App (see the HLD's "one shared local data store, not two" decision). The planned Linux app gets its own, separate Image Storage instance, kept behaviorally consistent with Android's via shared EARS specs.

## Data Shape

One entry per saved drawing: an identifier, a creation timestamp, and a rendered raster image. Painting writes new entries; the Companion App lists, reads, and deletes them.

## Reads Are Reactive

The saved-drawing list is reactive/observable, for the same reason as Config: a caller subscribes and is notified of changes rather than re-reading on its own schedule.

## Write Failures

A write can fail — storage full, an I/O error — and Image Storage reports the failure back to Painting rather than crashing. What Painting, and upstream User Experience, does about it is their own decision, not Image Storage's.

## Decisions & Alternatives

| Decision | Chosen | Alternatives Considered | Rationale |
|----------|--------|------------------------|-----------|
| Saved-drawing representation | A rendered raster image only | Vector/stroke data, or a full drawing history, alongside or instead of the image | The HLD's Non-Goals exclude re-editing and undo history, so nothing in the app today needs to reconstruct strokes from a saved drawing — only display and delete it. A rendered image also avoids needing a cross-platform-stable stroke serialization format. Storing full drawing/stroke history — e.g. for a future replay or time-lapse feature — is a possible future enhancement, not needed today. |
| Reactivity | Reactive/observable reads | One-shot reads | Consistent with Config's reactivity, for the same reasons; also removes a class of "companion app shows a stale gallery" bugs. |
| Scope | One Image Storage store per platform app | A single store synced or shared across platforms | Matches the HLD's "one shared local data store, not two" decision, scoped per platform since the Linux app is a wholly separate application. |

## Open Questions & Future Decisions

### Deferred

1. Concrete storage engine/technology isn't chosen here — an implementation decision, likely to differ per platform.
2. What Painting/User Experience does when a save fails is deferred to those components.
3. Whether the Companion App's drawing list needs paging or limits at scale (many saved drawings accumulated over months of use) isn't addressed — deferred until real usage data exists.

## References

- Root HLD: `docs/high-level-design.md` — System Design (shared local data store), Non-Goals (no undo/re-editing).
- `docs/intent/kid-canvas/painting/painting-design.md` — the writer of saved drawings, and the raster-vs-vector format decision.
- `docs/intent/config/config-design.md` — sibling store for settings.
