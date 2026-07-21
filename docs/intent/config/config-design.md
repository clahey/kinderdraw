---
parent: high-level-design
prefix: CONFIG
---

# Config

## Context and Design Philosophy

Config is the small, frequently-read settings store shared in-process by the Android build's Kid Canvas and Companion App (see the HLD's "one shared local data store, not two" decision). The planned Linux app gets its own, separate Config instance — a different deployment of this same design, not a shared one — kept behaviorally consistent with Android's via shared EARS specs.

It holds two kinds of settings: the age-adaptive UX configuration, and small app-level flags such as whether the first-launch adult setup dialog has been dismissed for good.

## Data Shape

### Raw Settings

Raw Settings are what's actually persisted, behind a shared interface that each platform's storage backs its own implementation of:

- **UX Configuration** — a single active mode (age slider or custom per-feature toggles), the age value when in slider mode, and the per-feature toggle state when in custom mode. Only one mode is active at a time. Both the Companion App and User Experience can write this; User Experience only ever writes the age-slider variant, since its first-launch dialog offers just an age slider, not the full custom toggle UI (see the HLD's Approach).
- **First-Launch Dialog State** — whether the "don't show again" toggle has been set.

### Resolved Features

No consumer of Config ever needs to know whether a given feature is active because of an age or a custom toggle — it just needs the answer. A resolver, built on top of the Raw Settings interface, exposes one resolved accessor per gated feature (e.g. a `get_resolved_colorPicker()`-shaped call per feature) that returns the effective on/off state regardless of mode: in age-slider mode it maps the age value through a preset bundle; in custom mode it passes the matching raw toggle straight through.

The resolver is a single, platform-agnostic implementation — not reimplemented per platform against each platform's own Raw Settings backing — so Android and Linux can never independently drift on which features a given age enables. This mirrors the HLD's treatment of Painting's touch-to-stroke logic: shared because it's business logic, not a native UI surface, so sharing it doesn't cost the native-feel trade-off the platform-toolkit tenet otherwise protects.

Consumers such as User Experience (see its Feature Gating section) read only resolved values.

## Reads Are Reactive

A caller subscribes to Config and is notified of subsequent changes, rather than re-reading on its own schedule. The companion app and kid canvas share one process and could in principle both be visible at once (e.g. a resizable- or multi-window platform), and a stale in-memory value would contradict a parent's expectation that tuning canvas features takes effect right away.

## Write Failures

A write can fail — storage full, an I/O error — and Config reports the failure back to the caller rather than crashing. What the caller does about it (retry, drop silently, surface to the adult) is that caller's own decision, not Config's.

## Decisions & Alternatives

| Decision | Chosen | Alternatives Considered | Rationale |
|----------|--------|------------------------|-----------|
| Reactivity | Reactive/observable reads | One-shot reads, re-fetched on each screen's own schedule | Cheap within a single process, and directly avoids a stale config value if the two screens are ever visible at the same time. |
| Scope | One Config store per platform app — Android's shared by both screens in-process; Linux's is its own, separate instance | A single store synced or shared across platforms | Matches the HLD's "one shared local data store, not two" decision, scoped per platform since the Linux app is a wholly separate application. |
| Domain grouping | UX configuration and small app-level flags (like the first-launch dialog's dismissed state) share one Config store | A separate store per flag/setting type | Both are small, simple, frequently-read key-value data with the same reactivity and failure-handling needs — splitting them wouldn't reflect a real difference in engineering shape. |
| Age/mode resolution | A single platform-agnostic resolver, built on a shared Raw Settings interface, computing resolved per-feature values | Each platform implements its own age-to-bundle mapping independently; consumers (e.g. User Experience) read Raw Settings directly and compute resolution themselves | The age-to-bundle mapping is business logic, not storage or UI — implementing and sharing it once removes any chance of Android and Linux disagreeing on what a given age enables, and keeps consumers from needing to know about raw mode/age at all. |

## Open Questions & Future Decisions

### Deferred

1. Concrete storage engine/technology isn't chosen here — an implementation decision, likely to differ per platform.
2. What a caller does when a write fails (retry, drop silently, surface to the adult) is deferred to the calling component.
3. The actual age-to-preset-bundle mapping — which ages enable which features — isn't defined here; only that one shared resolver owns computing it.
4. The exact set of resolved accessors (one per gated feature) grows as Kid Canvas's gated feature set grows — see the User Experience and Painting LLDs' own open questions on feature granularity and brush choice.

## References

- Root HLD: `docs/high-level-design.md` — System Design (shared local data store), Approach (age-adaptive canvas UX, first-launch adult setup), Key Design Decisions ("one shared local data store, not two").
- `docs/intent/kid-canvas/user-experience/user-experience-design.md` — Feature Gating (UX config consumption), Lifecycle Behavior (first-launch dialog state).
- `docs/intent/image-storage/image-storage-design.md` — sibling store for saved drawings.
