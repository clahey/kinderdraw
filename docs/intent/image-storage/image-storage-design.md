---
parent: high-level-design
prefix: IMAGES
---

# Image Storage

## Context and Design Philosophy

Image Storage holds saved drawings — the record a parent browses and manages from the Companion App, and the destination Painting writes a finished drawing to. It's declared once as a shared Kotlin interface (see the HLD's cross-platform data-layer boundary decision); Painting and the Companion App depend only on that interface, never on a platform's concrete implementation. On Android it's shared in-process by the Kid Canvas and Companion App (see the HLD's "one shared local data store, not two" decision). The planned Linux app gets its own, separate implementation, kept behaviorally consistent with Android's via shared EARS specs.

Whatever backs an implementation, entries persist durably: a saved drawing outlives the process that wrote it, and is still there for a later implementation instance to read — not just held in memory for the lifetime of one running instance.

## Data Shape

One entry per saved drawing: an identifier, a creation timestamp, and a rendered raster image. The identifier and creation timestamp can each be supplied by the caller creating the entry, or left for Image Storage to generate — an ordinary save doesn't need any storage-specific logic (id generation, clock access) to create an entry, but a caller with its own reason to control either value isn't blocked from supplying one.

Creating with an id that already has an entry updates that entry in place, replacing its raster image, rather than creating a second entry or failing as a conflict. The timestamp is only touched if the caller supplies one explicitly on that call — an update that omits it leaves the entry's existing timestamp untouched rather than refreshing it to the update's write time. A caller that wants to save a drawing incrementally as it's drawn can reuse the same id across repeated creates, keeping the original creation time pinned unless it explicitly chooses to overwrite it.

Painting writes new entries; the Companion App lists, reads, and deletes them.

## Reads Are Reactive

The saved-drawing list is reactive/observable, for the same reason as Config: a caller subscribes and immediately receives the current list, then is notified as entries are added or removed by any means — not just through Image Storage's own create/delete operations — rather than re-reading on its own schedule.

## Android Storage Backend

Android's implementation persists every saved drawing through the device's shared MediaStore, the same mechanism any camera or gallery app uses, in its own dedicated album (e.g. `Pictures/kinderdraw`) rather than mixed into the general camera roll. MediaStore is the source of truth for these entries, not a mirror of some other private copy: create, list, read, and delete all act directly on the MediaStore entry. A saved drawing's identifier is its MediaStore entry's identity, and its creation timestamp is stored in MediaStore's `DATE_TAKEN` metadata field.

This is what makes saved drawings show up in the device's own Photos/Gallery app without any extra step. Whether a drawing is ever backed up off-device isn't Image Storage's call to make: MediaStore-shared media is outside the scope of the app's own Auto Backup declaration, so it follows whatever the user's own photo-backup app (Google Photos or equivalent) does with the device's photo albums generally — including that app's own per-album backup selection, which is exactly the lever a parent who doesn't want kinderdraw's album backed up already has. MediaStore itself is a core Android framework API, present on de-Googled/free-software Android builds as well as stock Android; only cloud backup specifically depends on whether the user has a backup app like Google Photos installed at all, which is their own environment, not a kinderdraw dependency.

Because MediaStore is the source of truth, Android's implementation observes MediaStore's own change notifications for the app's album — a drawing added or removed by any means, including directly through the system Gallery/Photos app, is reflected the same way a call through Image Storage's own API would be.

## Linux Storage Backend

Linux's implementation persists saved drawings as files under the user's Pictures directory, in their own subdirectory (default `~/Pictures/kinderdraw`), configurable to a different location. It observes that directory for changes via the platform's filesystem-watch mechanism (inotify, most likely through GLib's `GFileMonitor` given the GTK+ shell), so a drawing added or removed by any means — including directly through a file manager — is reflected the same way Android's MediaStore observation is.

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
| Android storage backend | MediaStore is the source of truth for every saved drawing, unconditionally, in its own dedicated album | App-private storage, with no Photos-app visibility; a Config toggle gating MediaStore vs. private storage per drawing; private storage as the source of truth, separately mirrored into MediaStore | A single source of truth avoids dual-write and delete-sync bugs between two copies. A dedicated album keeps drawings out of the general camera roll, and the user's own photo-backup app (e.g. Google Photos) already offers per-album backup selection — the exact control an in-app toggle would otherwise exist to provide — so no separate kinderdraw-level setting is needed. |
| Creation timestamp's MediaStore mapping | Stored in the MediaStore entry's `DATE_TAKEN` field | Rely on file-system timestamps (`DATE_ADDED`/`DATE_MODIFIED`) instead | `DATE_TAKEN` is set explicitly on insert/update, independent of file-system timestamps, so it can hold whatever value the caller supplied or Image Storage generated (see Id/timestamp assignment above) rather than whatever the file system happens to record. |
| Source of change notifications | Platform-native observation covering changes from any source: MediaStore's own change notifications on Android, a filesystem watch (inotify) on Linux | Only signal on changes made through Image Storage's own create/delete calls | Both platforms have a native way to observe changes to the underlying store from any source, and since that store is the source of truth, a change made outside Image Storage's own API (deleting a drawing from the system Gallery app, or from a file manager) is just as real a change as one made through it — a parent expects the Companion App's list to reflect it either way. |
| Linux default storage location | `~/Pictures/kinderdraw`, a subdirectory of the user's Pictures directory | A location under the app's own private config/data directory, not the user's Pictures directory | Mirrors the Android decision's spirit — drawings live somewhere the user would naturally look for photos/pictures — even though Linux has no MediaStore-equivalent shared media index to place them in. |

## Open Questions & Future Decisions

### Deferred

1. The file format and any accompanying metadata storage for Linux's implementation isn't chosen here — an implementation decision.
2. What Painting/User Experience does when a save fails is deferred to those components.
3. Whether the Companion App's drawing list needs paging or limits at scale (many saved drawings accumulated over months of use) isn't addressed — deferred until real usage data exists.
4. The exact MediaStore album name isn't fixed here — an implementation detail.
5. How a parent reconfigures Linux's storage location (a Companion/shell setting, a config file, an environment variable) isn't decided here.

## References

- Root HLD: `docs/high-level-design.md` — System Design (shared local data store), Non-Goals (no undo/re-editing), Key Design Decisions (cross-platform data-layer boundary as a shared Kotlin interface).
- `docs/intent/kid-canvas/painting/painting-design.md` — the writer of saved drawings, and the raster-vs-vector format decision.
- `docs/intent/config/config-design.md` — sibling store for settings.
