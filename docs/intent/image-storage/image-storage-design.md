---
parent: high-level-design
prefix: IMAGES
---

# Image Storage

## Context and Design Philosophy

Image Storage holds saved drawings — the record a parent browses and manages from the Companion App, and the destination Painting writes a finished drawing to. It's declared once as a shared Kotlin interface (see the HLD's cross-platform data-layer boundary decision); Painting and the Companion App depend only on that interface, never on a platform's concrete implementation. On Android it's shared in-process by the Kid Canvas and Companion App (see the HLD's "one shared local data store, not two" decision). The planned Linux app gets its own, separate implementation, kept behaviorally consistent with Android's via shared EARS specs.

Whatever backs an implementation, entries persist durably: a saved drawing outlives the process that wrote it, and is still there for a later implementation instance to read — not just held in memory for the lifetime of one running instance.

## Data Shape

One entry per saved drawing: an identifier, a creation timestamp, and a rendered raster image. Create always makes a new entry, generating its identifier and, unless the caller supplies one, its creation timestamp — an ordinary save doesn't need any storage-specific logic (id generation, clock access) to create an entry, but a caller with its own reason to control the timestamp isn't blocked from supplying one. A separate update operation replaces an existing entry's raster image by id instead, and fails if that id has no entry rather than creating one — an id is purely an opaque lookup handle create hands back to the caller, never a value the caller invents; neither the Companion App nor any other consumer ever displays it to the parent, so there's no case where a caller needs to choose its own id rather than reusing one create already gave it. Update's timestamp is only touched if the caller supplies one explicitly on that call — an update that omits it leaves the entry's existing timestamp untouched rather than refreshing it to the update's write time. A caller that wants to save a drawing incrementally as it's drawn creates it once, then updates that same id on each subsequent save, keeping the original creation time pinned unless it explicitly chooses to overwrite it.

Painting writes entries — creating a new one for a drawing saved for the first time, or updating an existing one by id for a drawing saved again (see the Painting LLD's Save and Clear); the Companion App lists, reads, and deletes them.

Create and update both accept the raster image as an `androidx.compose.ui.graphics.ImageBitmap` — Compose Multiplatform's own bitmap type, the same one Painting already renders into — rather than a caller-encoded byte format. Each platform's implementation is responsible for encoding that bitmap into whatever it actually persists (see Android Storage Backend). An identifier is a `String` and a creation timestamp is a `Long` of epoch milliseconds; a caller-supplied timestamp uses this same type.

Every operation that can fail (create, update, read, delete) returns a Kotlin `Result` rather than throwing, uniformly reporting success or the underlying failure to the caller. A failed `Result`'s exception is always Image Storage's own `ImageStorageException`, never a platform-specific type — each backend normalizes whatever its own platform surfaces (a thrown platform exception, or a sentinel value such as a null/zero/empty result signaling "no match") into that one type, so a caller never needs to reason about which platform-specific failure shape produced it.

## Reads Are Reactive

The saved-drawing list is reactive/observable, for the same reason as Config: a caller subscribes and immediately receives the current list, then is notified as entries are added or removed by any means — not just through Image Storage's own create/delete operations — rather than re-reading on its own schedule.

The list is exposed as a `Flow<List<SavedDrawingEntry>>`, where each `SavedDrawingEntry` carries only an id and creation timestamp, not the raster image itself — a subscriber that needs a particular drawing's image reads it separately, on demand, by id.

## Android Storage Backend

Android's implementation persists every saved drawing through the device's shared MediaStore, the same mechanism any camera or gallery app uses, in its own dedicated album (e.g. `Pictures/KinderDraw`) rather than mixed into the general camera roll. MediaStore is the source of truth for these entries, not a mirror of some other private copy: create, update, list, read, and delete all act directly on the MediaStore entry. A saved drawing's identifier is its MediaStore entry's identity, and its creation timestamp is stored in MediaStore's `DATE_TAKEN` metadata field.

This is what makes saved drawings show up in the device's own Photos/Gallery app without any extra step. Whether a drawing is ever backed up off-device isn't Image Storage's call to make: MediaStore-shared media is outside the scope of the app's own Auto Backup declaration, so it follows whatever the user's own photo-backup app (Google Photos or equivalent) does with the device's photo albums generally — including that app's own per-album backup selection, which is exactly the lever a parent who doesn't want KinderDraw's album backed up already has. MediaStore itself is a core Android framework API, present on de-Googled/free-software Android builds as well as stock Android; only cloud backup specifically depends on whether the user has a backup app like Google Photos installed at all, which is their own environment, not a KinderDraw dependency.

Because MediaStore is the source of truth, Android's implementation observes MediaStore's own change notifications for the app's album — a drawing added or removed by any means, including directly through the system Gallery/Photos app, is reflected the same way a call through Image Storage's own API would be.

Every entry Image Storage deletes is one it created itself — Painting is the only writer, and MediaStore tracks each entry's owning app. Per the HLD's minSdk 30 decision, that ownership grants delete/modify access with no OS consent dialog on any supported device, so create and delete both complete without ever prompting the user.

## Linux Storage Backend

Linux's implementation persists saved drawings as files under the user's Pictures directory, in their own subdirectory (default `~/Pictures/KinderDraw`), configurable to a different location. It observes that directory for changes via the platform's filesystem-watch mechanism (inotify, most likely through GLib's `GFileMonitor` given the GTK+ shell), so a drawing added or removed by any means — including directly through a file manager — is reflected the same way Android's MediaStore observation is.

## Write Failures

A create, update, or delete call can fail — storage full, an I/O error — and Image Storage reports the failure back to the caller rather than crashing. What the caller does about it is its own decision, not Image Storage's. Reading, updating, or deleting by an identifier with no matching entry is reported the same way, as a failure to the caller.

## Decisions & Alternatives

| Decision | Chosen | Alternatives Considered | Rationale |
|----------|--------|------------------------|-----------|
| Saved-drawing representation | A rendered raster image only | Vector/stroke data, or a full drawing history, alongside or instead of the image | The HLD's Non-Goals exclude re-editing and undo history, so nothing in the app today needs to reconstruct strokes from a saved drawing — only display and delete it. A rendered image also avoids needing a cross-platform-stable stroke serialization format. Storing full drawing/stroke history — e.g. for a future replay or time-lapse feature — is a possible future enhancement, not needed today. |
| Reactivity | Reactive/observable reads | One-shot reads | Consistent with Config's reactivity, for the same reasons; also removes a class of "companion app shows a stale gallery" bugs. |
| Scope | One Image Storage store per platform app | A single store synced or shared across platforms | Matches the HLD's "one shared local data store, not two" decision, scoped per platform since the Linux app is a wholly separate application. |
| Create vs. update as separate operations | Two operations: `create` always makes a new entry and generates its id; `update` always replaces an existing entry by a caller-supplied id and fails if that id has no entry | One `create` operation, doubling as an update whenever the caller supplies an id that already has an entry | An id is never shown to a person (see Data Shape) and Image Storage is always the one that mints it, so a caller only ever supplies an id to reuse one it already holds — collapsing that into `create`'s own id parameter made "create" ambiguously mean "insert or overwrite" depending on the argument, and MediaStore's row id is provider-assigned on Android besides, so it can't honor a caller-chosen id for a brand-new entry anyway. Splitting the operations makes each name mean one thing. |
| Timestamp field on an update | Left unchanged unless the caller explicitly supplies a new value on that call | Always refresh to the update's write time | Keeps "creation timestamp" meaning what its name says — when the entry was first created — even once the same id is updated repeatedly, without needing a separate last-modified field. A caller that wants the timestamp to move can still do so explicitly. |
| Android storage backend | MediaStore is the source of truth for every saved drawing, unconditionally, in its own dedicated album | App-private storage, with no Photos-app visibility; a Config toggle gating MediaStore vs. private storage per drawing; private storage as the source of truth, separately mirrored into MediaStore | A single source of truth avoids dual-write and delete-sync bugs between two copies. A dedicated album keeps drawings out of the general camera roll, and the user's own photo-backup app (e.g. Google Photos) already offers per-album backup selection — the exact control an in-app toggle would otherwise exist to provide — so no separate KinderDraw-level setting is needed. |
| Creation timestamp's MediaStore mapping | Stored in the MediaStore entry's `DATE_TAKEN` field | Rely on file-system timestamps (`DATE_ADDED`/`DATE_MODIFIED`) instead | `DATE_TAKEN` is set explicitly on insert/update, independent of file-system timestamps, so it can hold whatever value the caller supplied or Image Storage generated (see Id/timestamp assignment above) rather than whatever the file system happens to record. |
| Source of change notifications | Platform-native observation covering changes from any source: MediaStore's own change notifications on Android, a filesystem watch (inotify) on Linux | Only signal on changes made through Image Storage's own create/delete calls | Both platforms have a native way to observe changes to the underlying store from any source, and since that store is the source of truth, a change made outside Image Storage's own API (deleting a drawing from the system Gallery app, or from a file manager) is just as real a change as one made through it — a parent expects the Companion App's list to reflect it either way. |
| Raster image type carried by the interface | `androidx.compose.ui.graphics.ImageBitmap` | A caller-encoded byte format (e.g. PNG `ByteArray`) | Matches the type Painting already renders into via `DrawScope`, so no separate encoding step exists at the call site. Compose Multiplatform's `ImageBitmap` isn't backed by the same pixel representation on every platform (Android's `android.graphics.Bitmap` vs. Skia's bitmap on desktop/JVM), so there's no portable commonMain encoder either way — each backend has to encode to its own persisted format itself regardless of which type crosses the boundary. |
| Identifier and timestamp Kotlin types | `id: String`, `timestamp: Long` (epoch milliseconds) | Typed wrapper classes; a `kotlinx-datetime` `Instant` for the timestamp | Avoids a new dependency for a single field. A `String` id lets each backend encode whatever its own native id type is (e.g. Android's MediaStore row id, a `Long`) trivially, and leaves room for a Linux backend using a filename-derived id. |
| Failure reporting shape | Kotlin `Result<T>` return values on the fallible suspend operations (create, read, delete) | A custom sealed result type; thrown exceptions | Idiomatic for coroutine-based error signaling without introducing a bespoke type; every operation's failure mode is uniform (success, or the underlying failure), so `Result` needs no extra structure. |
| Failure exception type | A single `ImageStorageException`, which every backend normalizes both thrown platform exceptions and non-exception failure signals (e.g. MediaStore's null/zero/empty sentinel returns) into | Let each backend's own platform exception type (e.g. Android's `SecurityException`/`IOException`) surface directly through `Result`'s failure case; a sealed hierarchy of specific failure reasons | Keeps the shared interface platform-agnostic — a caller never needs to know which platform-specific exception type or sentinel value a given backend happens to use for a given failure. No spec today requires a caller to distinguish *why* an operation failed, so a single type is enough; a reason hierarchy can be added later if a real need for one appears. |
| List reactivity's concrete type | `Flow<List<SavedDrawingEntry>>`, where `SavedDrawingEntry` carries only id and creation timestamp | A callback/listener interface; streaming the raster image as part of each entry | `Flow` is already a transitive dependency via Compose's runtime, needing nothing new. Keeping entries metadata-only avoids loading every saved drawing's full image just to enumerate them — `read()` loads an individual image on demand. |
| Handling MediaStore's ownership-based delete/write consent flow on Android | No handling needed — rely on the minSdk 30 floor, since Image Storage only ever deletes entries it created itself, which don't require consent at that floor | Support minSdk 29 or lower and add a `RecoverableSecurityException`/consent-dialog fallback path for the Companion App's delete | The HLD's minSdk 30 decision was made specifically to avoid this fallback: an unowned-item consent flow that can never trigger for Image Storage's own entries isn't worth a lower minSdk. |
| Linux default storage location | `~/Pictures/KinderDraw`, a subdirectory of the user's Pictures directory | A location under the app's own private config/data directory, not the user's Pictures directory | Mirrors the Android decision's spirit — drawings live somewhere the user would naturally look for photos/pictures — even though Linux has no MediaStore-equivalent shared media index to place them in. |

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
