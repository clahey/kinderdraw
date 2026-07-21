---
parent: high-level-design
prefix: IMAGES
---

# Image Storage

## Context and Design Philosophy

Image Storage holds saved drawings — the record a parent browses and manages from the Companion App, and the destination Painting writes a finished drawing to. It's declared once as a shared Kotlin interface (see the HLD's cross-platform data-layer boundary decision); Painting and the Companion App depend only on that interface, never on a platform's concrete implementation. On Android it's shared in-process by the Kid Canvas and Companion App (see the HLD's "one shared local data store, not two" decision). The planned Linux app gets its own, separate implementation, kept behaviorally consistent with Android's via shared EARS specs.

## Data Shape

One entry per saved drawing: an identifier, a creation timestamp, and a rendered raster image. The identifier and creation timestamp can each be supplied by the caller creating the entry, or left for Image Storage to generate — an ordinary save doesn't need any storage-specific logic (id generation, clock access) to create an entry, but a caller with its own reason to control either value isn't blocked from supplying one.

Creating with an id that already has an entry updates that entry in place, replacing its raster image, rather than creating a second entry or failing as a conflict. The timestamp is only touched if the caller supplies one explicitly on that call — an update that omits it leaves the entry's existing timestamp untouched rather than refreshing it to the update's write time. A caller that wants to save a drawing incrementally as it's drawn can reuse the same id across repeated creates, keeping the original creation time pinned unless it explicitly chooses to overwrite it.

Painting writes new entries; the Companion App lists, reads, and deletes them.

## Reads Are Reactive

The saved-drawing list is reactive/observable, for the same reason as Config: a caller subscribes and immediately receives the current list, then is notified as entries are added or deleted, rather than re-reading on its own schedule.

## Write Failures

A create or delete call can fail — storage full, an I/O error — and Image Storage reports the failure back to the caller rather than crashing. What the caller does about it is its own decision, not Image Storage's. Reading or deleting by an identifier with no matching entry is reported the same way, as a failure to the caller.

## Decisions & Alternatives

| Decision | Chosen | Alternatives Considered | Rationale |
|----------|--------|------------------------|-----------|
| Saved-drawing representation | A rendered raster image only | Vector/stroke data, or a full drawing history, alongside or instead of the image | The HLD's Non-Goals exclude re-editing and undo history, so nothing in the app today needs to reconstruct strokes from a saved drawing — only display and delete it. A rendered image also avoids needing a cross-platform-stable stroke serialization format. Storing full drawing/stroke history — e.g. for a future replay or time-lapse feature — is a possible future enhancement, not needed today. |
| Reactivity | Reactive/observable reads | One-shot reads | Consistent with Config's reactivity, for the same reasons; also removes a class of "companion app shows a stale gallery" bugs. |
| Scope | One Image Storage store per platform app | A single store synced or shared across platforms | Matches the HLD's "one shared local data store, not two" decision, scoped per platform since the Linux app is a wholly separate application. |
| Id/timestamp assignment at creation | Caller may optionally supply either or both; Image Storage generates any that are omitted | Image Storage always generates both; caller always supplies both | Keeps an ordinary save simple (nothing to generate), while leaving room for a caller that needs to control the id or timestamp itself. |
| Create called with an id that already has an entry | Updates (overwrites) that entry's raster image in place, rather than creating a second entry or failing as a conflict | Reject as a conflict; silently create a second entry under a different id | Makes create idempotent by id, which is what a caller saving a drawing incrementally as it's drawn needs — it can reuse one id across repeated creates without tracking whether an earlier call already landed. |
| Timestamp field on an update | Left unchanged unless the caller explicitly supplies a new value on that call | Always refresh to the update's write time | Keeps "creation timestamp" meaning what its name says — when the entry was first created — even once an id can be reused, without needing a separate last-modified field. A caller that wants the timestamp to move can still do so explicitly. |

## Open Questions & Future Decisions

### Deferred

1. Concrete storage engine/technology isn't chosen here — an implementation decision, likely to differ per platform.
2. What Painting/User Experience does when a save fails is deferred to those components.
3. Whether the Companion App's drawing list needs paging or limits at scale (many saved drawings accumulated over months of use) isn't addressed — deferred until real usage data exists.

## References

- Root HLD: `docs/high-level-design.md` — System Design (shared local data store), Non-Goals (no undo/re-editing), Key Design Decisions (cross-platform data-layer boundary as a shared Kotlin interface).
- `docs/intent/kid-canvas/painting/painting-design.md` — the writer of saved drawings, and the raster-vs-vector format decision.
- `docs/intent/config/config-design.md` — sibling store for settings.
