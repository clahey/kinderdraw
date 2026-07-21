---
parent: kid-canvas
prefix: CANVAS-UX
---

# User Experience

## Context and Design Philosophy

User Experience is Kid Canvas's composition root: it arranges Widgets (buttons, color picker, and similar chrome) and Painting (the drawing surface) into one screen, and owns whatever behavior belongs to the screen as a whole rather than to either child on its own — which controls are present, what happens when a lifecycle action fires, how touches are arbitrated between drawing and chrome, and how the screen answers a toddler's action immediately. Widgets and Painting don't depend on each other; User Experience is the only component that depends on both.

The toddler-usability tenet governs every decision here: no confirmation dialogs, no state a toddler has to interpret before continuing, no visible-but-inert affordance that invites a tap that does nothing, and no path by which the toddler's own hands can accidentally leave the drawing or leave the app.

## Screen Composition

The screen has two layers. Painting fills the entire screen as the drawing surface. Widgets are composed as chrome on top of it, anchored along the edges so a control never sits over the active drawing area and never requires reaching across the screen to a fixed toolbar band. User Experience owns which controls are present and how they're arranged relative to each other and to the drawing surface underneath; each control's own rendering, hit-testing, and activation belong to Widgets.

## Input Arbitration

User Experience arbitrates all raw pointer input before it reaches Widgets or Painting: at most one pointer's gesture is live on the whole screen at a time.

When a new pointer touches down and no gesture is currently live, User Experience offers it to Widgets first. If the touch lands inside some control's hit region, Widgets claims it and that pointer becomes the live gesture — a control activation, per the Widgets LLD's own hit-testing and activation rules. If no control claims it, User Experience routes the pointer to Painting instead, starting a stroke: a touch that misses every control's hit region becomes a canvas stroke rather than a discarded touch, even when the toddler's aim was probably a nearby control — there's no attempt to guess intent and redirect an imprecise touch to a nearby control.

While a gesture is live — whether a Widgets control-press or a Painting stroke — any other pointer that touches down is ignored outright: it's never offered to Widgets or Painting, and it can't claim a control or start a stroke. The live gesture ends when its pointer lifts, at which point the next pointer-down is free to start a new one.

This is one rule, not two independent ones — a live stroke blocking Widgets and a live control-press blocking Painting both fall out of the same single-live-gesture arbiter, rather than being cases that could drift out of sync with each other.

This also means a Widgets action, including New Picture, can only start once no stroke is active on Painting — there's no case where a lifecycle action fires mid-stroke.

## Feature Gating

User Experience reads Config's resolved per-feature values (see the Config LLD's Resolved Features) — never the underlying age-slider value or custom toggle map directly — and uses them to decide which Widgets controls are composed onto the screen and which optional behaviors (such as whether OS back navigation is ever re-enabled — see OS Navigation and Process Lifecycle) are active. A feature that's off is omitted entirely rather than shown disabled: a visible-but-inert control invites a tap that does nothing, which the toddler-usability tenet treats as a broken interaction rather than a safe default.

Today the configuration gates the canvas feature set as a single bundle (color picker, New Picture) — there's no per-feature independence yet. Finer-grained gating is deferred until the feature set grows past what one bundle covers.

## Lifecycle Behavior

### New Picture

Triggered from its Widgets button, which — per Input Arbitration above — is only reachable when no stroke is active on Painting. The action runs a fixed sequence:

1. User Experience asks Painting whether the current drawing is empty. Painting owns this determination; User Experience owns what happens as a result.
2. If the drawing isn't empty, User Experience calls Painting's `save()` (see the Painting LLD), which writes the drawing to Image Storage. If it is empty, this step is skipped — no blank drawing is ever written.
3. Painting's canvas is cleared.

No confirmation dialog is shown, per the HLD's reversible-actions-default-to-forgiving tenet — the toddler can't parse or dismiss a confirmation prompt, and whenever there was content to lose, it's already recoverable because it was saved before the clear. The sequence doesn't surface an intermediate state that requires a response before drawing can resume.

### OS Navigation and Process Lifecycle

The OS back gesture/button is consumed and ignored — it never navigates the toddler out of the kid canvas — at least under younger-age configurations; whether an older-age bundle ever re-enables it is deferred (see Feature Gating and Open Questions). This holds regardless of whether device-locking from the HLD's first-launch adult setup dialog is active — it's a baseline default on its own.

The current, in-progress drawing must survive any lifecycle event the OS handles through its own saved-instance-state mechanism — configuration changes, brief backgrounding, process death within that scope — the same category of survival Compose's `rememberSaveable` (or its Compose Multiplatform equivalent) provides for ordinary UI state. This is a distinct mechanism from the New Picture auto-save path above: routine OS lifecycle churn isn't a completed drawing worth writing to the permanent store, it's mid-work state that should simply still be there when the screen returns.

## Interaction Feedback

Actions that User Experience orchestrates across components answer the toddler through the action's own visible effect, not through any separate dialog, confirmation, or extra interaction step:

- **Color selection** — Widgets owns the visual mechanism (e.g. which swatch shows as active); User Experience wires the selection straight into Painting's active stroke color, so the next stroke drawn is simply in the new color — no separate acknowledgment step required.
- **New Picture** — the cleared canvas is itself the feedback: the toddler sees the drawing surface go blank, confirming the action took effect. No separate confirmation banner or toast is shown, consistent with no blocking dialogs on the kid canvas.

Feedback belonging to a single control's own activation (a button's press animation, for example) is owned by Widgets, not here — this section covers only feedback for actions User Experience itself orchestrates across components.

## Decisions & Alternatives

| Decision | Chosen | Alternatives Considered | Rationale |
|----------|--------|------------------------|-----------|
| Gated-off feature representation | Omit the control entirely | Show it disabled/greyed out | A disabled control invites a tap the toddler can't interpret the failure of — omission avoids a dead interaction rather than explaining one. |
| New Picture confirmation | None — auto-save-then-clear | Blocking confirmation dialog; clear-then-offer-undo toast | Direct application of the HLD's reversible-actions-default-to-forgiving tenet: the action is already recoverable via auto-save, so a confirmation step adds friction without adding safety. |
| Screen layering | Painting full-bleed as background; Widgets chrome anchored to the edges on top | Fixed non-overlapping regions (e.g. a dedicated toolbar strip above a smaller canvas area) | Maximizes the reachable drawing area and avoids losing screen real estate to a dedicated toolbar band, while edge anchoring keeps controls reachable without precise fine-motor reach. |
| Input arbitration between Widgets and Painting | A single global arbiter: whichever pointer touches down first (on a control or on the canvas) becomes the one live gesture; every other concurrent pointer is ignored outright until it lifts | Two independent rules (stroke blocks Widgets; separately, control-press blocks Painting); per-touch heuristics to classify incidental vs. intentional touches; redirect an imprecise touch to the nearest control it probably meant to hit | Toddlers routinely generate incidental touches (resting palm, second finger) while interacting; one symmetric "only the first pointer is live" rule is simpler and more robust than classifying touches or guessing intent, and it incidentally removes any mid-stroke lifecycle-action ordering hazard. A sloppy touch that misses its likely target becomes whatever it landed on rather than being reinterpreted. |
| Auto-save on New Picture when the drawing is empty | Skipped — no save write, canvas still clears | Always auto-save regardless of content | Avoids cluttering the saved-drawings store with blank entries. Ownership stays split: Painting reports whether there's content, User Experience decides what that fact means. |
| OS back gesture | Consumed and ignored, at least under younger-age configurations | Standard OS back behavior (exits the app); mapping the gesture to an in-app action (e.g. New Picture) | A toddler navigating out of the app unsupervised is the failure mode the HLD's first-launch device-locking exists to prevent; ignoring back is a baseline defense independent of whether locking is active. Exact age cutoff, if any, is deferred. |
| Surviving OS-managed lifecycle events (rotation, brief backgrounding, saved-instance-state-scoped process death) | Current drawing persists via the platform's saved-instance-state mechanism, separate from the New Picture auto-save path | Rely on the New Picture auto-save path for this too; accept loss of in-progress work across these events | Routine OS churn isn't a completed drawing worth writing to the permanent store — it's ordinary transient UI state, no different from any other Compose screen's saved-instance-state handling — and shouldn't force a save-then-clear the toddler never asked for. |

## Open Questions & Future Decisions

### Resolved

1. ✅ User Experience observes UX-config changes live, not just at launch — Config exposes a reactive/observable read API. See the Config LLD's Reads Are Reactive section.

### Deferred

1. What happens if the auto-save on New Picture fails (storage full, write error)? Image Storage reports the failure to the caller, but what User Experience does in response is undefined — today's design assumes the save always succeeds before the canvas clears.
2. Per-feature gating granularity: once the feature set grows past the current single bundle, which features become independently toggle-able needs its own pass — including whether/at what age OS back navigation is ever re-enabled.
3. Exact composition geometry — which edge each Widgets control anchors to, spacing, sizing — is deferred to implementation and visual design, not fixed here.

## References

- Parent sub-HLD: `docs/intent/kid-canvas/kid-canvas-design.md` — defines User Experience as Kid Canvas's composition root.
- Root HLD: `docs/high-level-design.md` — Approach (age-adaptive canvas UX, first-launch adult setup), Tenets (toddler usability over platform convention; reversible actions default to forgiving), System Design (shared data store and UX config flow).
- Sibling: `docs/intent/kid-canvas/painting/painting-design.md` — the stroke model and save/clear operations this LLD's Lifecycle Behavior orchestrates.
- `docs/intent/config/config-design.md` — UX configuration reactivity, storage shape, and resolved-feature accessors.
- `docs/intent/image-storage/image-storage-design.md` — saved-drawing write failures.
