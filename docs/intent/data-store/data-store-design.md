---
parent: high-level-design
prefix: STORE
---

# Data Store

## Context and Design Philosophy

Data Store is the single shared on-device store an Android build's Kid Canvas and Companion App both read and write, in-process, with no sync layer between them (see the HLD's "one shared local data store, not two" decision). The planned Linux app gets its own, separate local store — a different instance of this same design, not a shared one — kept behaviorally consistent with Android's via shared EARS specs rather than shared data.

It holds three domains: saved drawings, the age-adaptive UX configuration, and small app-level settings such as whether the first-launch adult setup dialog has been dismissed for good.

## Data Domains

- **Drawings** — one entry per saved drawing: an identifier, a creation timestamp, and a rendered raster image. No vector/stroke data is persisted — see Decisions.
- **UX Configuration** — a single active mode (age slider or custom per-feature toggles), the age value when in slider mode, and the per-feature toggle state when in custom mode. Only one mode is active at a time.
- **App Settings** — currently just the first-launch adult setup dialog's dismissed state (see the HLD's Approach): whether the "don't show again" toggle has been set.

## Read/Write API Surface

| Caller | Operation | Domain |
|--------|-----------|--------|
| Companion App | List saved drawings; read a drawing's image; delete a drawing | Drawings |
| Companion App | Read UX configuration; write UX configuration (either mode) | UX Configuration |
| Painting (Kid Canvas) | Write a new saved drawing | Drawings |
| User Experience (Kid Canvas) | Read UX configuration | UX Configuration |
| User Experience (Kid Canvas) | Write UX configuration, age-slider mode only — the first-launch dialog offers only an age slider, not the full custom per-feature toggle UI (see the HLD's Approach) | UX Configuration |
| User Experience (Kid Canvas) | Read and write the first-launch dialog's dismissed state | App Settings |

## Reactivity

Reads of UX Configuration are reactive (observable), not one-shot: a caller subscribes and is notified of subsequent changes rather than re-reading on its own schedule. This resolves the User Experience LLD's open question about live vs. launch-time config reads — the companion app and kid canvas share one process and could in principle both be visible at once (e.g. a resizable- or multi-window platform), and a stale in-memory config would contradict the goal of a parent being able to tune canvas features and have it take effect. Drawing-list reads for the Companion App follow the same reactive shape for consistency, though only the Companion App itself both reads and displays that list today.

## Failure Handling Surface

Writes (a saved drawing, a UX configuration change) can fail — storage full, an I/O error — and report failure back to the caller rather than crashing. What a caller does in response to a failed write is that caller's own decision, not Data Store's; see the User Experience LLD's open question on auto-save failure handling.

## Decisions & Alternatives

| Decision | Chosen | Alternatives Considered | Rationale |
|----------|--------|------------------------|-----------|
| Saved-drawing representation | A rendered raster image only | Vector/stroke data alongside or instead of the image | The HLD's Non-Goals exclude re-editing and undo history, so nothing in the app ever needs to reconstruct strokes from a saved drawing — only display and delete it. A rendered image also avoids needing a cross-platform-stable stroke serialization format. |
| UX Configuration read reactivity | Reactive/observable | One-shot reads, re-fetched on each screen's own schedule | Cheap within a single process, and directly resolves the risk of a stale config value if the two screens are ever visible at the same time — a plain one-shot read can't guarantee that. |
| Store scope | One store per platform app — Android's shared by both screens in-process; Linux's is its own, separate instance | A single store synced or shared across platforms | Matches the HLD's explicit "one shared local data store, not two" decision, scoped per platform since the Linux app is a wholly separate application with its own process. |

## Open Questions & Future Decisions

### Deferred

1. Concrete storage engine/technology (e.g. a SQL database for drawings, a key-value or proto store for configuration, or a single unified engine) isn't chosen here — an implementation decision, likely to differ per platform.
2. What a caller does when a write fails (retry, drop silently, surface to the adult) is deferred to the calling component, not decided by Data Store.
3. Whether the Companion App's drawing list needs paging or limits at scale (many saved drawings accumulated over months of use) isn't addressed — deferred until real usage data exists.

## References

- Root HLD: `docs/high-level-design.md` — System Design (shared local data store), Approach (age-adaptive canvas UX, first-launch adult setup), Key Design Decisions ("one shared local data store, not two").
- `docs/intent/kid-canvas/user-experience/user-experience-design.md` — Feature Gating (UX config consumption), the resolved open question on live vs. launch-time config reads.
- `docs/intent/kid-canvas/painting/painting-design.md` — saved-drawing write path and format.
