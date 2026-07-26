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

At most one pointer's gesture is live on the whole screen at a time. User Experience achieves this with two mechanisms working together, rather than one central loop that manually classifies every touch: which of Widgets or Painting *claims* a fresh touch is left to Compose's own pointer dispatch; keeping every other pointer *out* of whichever component didn't claim it is User Experience's own job.

**Claiming.** Widgets is composed on top of Painting (see Screen Composition), and each does its own ordinary hit-testing on whatever pointer Compose delivers to it — Widgets via each control's own hit region (see the Widgets LLD's Hit-Testing and Activation), Painting via covering the full screen beneath. A touch inside some control's hit region is claimed by Widgets, which reports it through that control's `onPressedChange(true)` callback; a touch that misses every control falls through to Painting underneath, starting a stroke. There's no attempt to guess intent and redirect an imprecise touch to a nearby control — a touch that misses becomes a canvas stroke rather than a discarded touch, even when the toddler's aim was probably a nearby control.

**Blocking.** For as long as one component holds a live gesture — a Widgets control between its `onPressedChange(true)` and matching `onPressedChange(false)`, or Painting between its `onStrokeActiveChange(true)` and matching `onStrokeActiveChange(false)` (see the Painting LLD's Reporting Stroke State) — User Experience composes a transparent, pointer-consuming layer over the *other* component, for that duration only. A new pointer-down landing on that layer is consumed there and never reaches the covered component, so it can never claim a control or start a stroke. Because Compose fixes a pointer's target at its own down event, a pointer swallowed by the blocking layer stays swallowed for the rest of its own gesture even if the live gesture ends while it's still held down — only a fresh pointer-down after that is eligible to start the next gesture.

This also means a Widgets action, including New Picture, can only start once no stroke is active on Painting — there's no case where a lifecycle action fires mid-stroke. New Picture then holds the live-gesture slot for its own entire sequence (see Lifecycle Behavior), not just the button press that triggered it: User Experience keeps the blocking layer up over both Widgets and Painting from activation through `clear()` completing, spanning the asynchronous `save()` call, so a touch during that window is blocked outright rather than accepted as a new stroke that the sequence's clear() step would immediately erase.

## Feature Gating

User Experience reads Config's resolved per-feature values (see the Config LLD's Resolved Features) — never the underlying age-slider value or custom toggle map directly — and uses them to decide which Widgets controls are composed onto the screen and which optional behaviors (such as whether OS back navigation is ever re-enabled — see OS Navigation and Process Lifecycle) are active. A feature that's off is omitted entirely rather than shown disabled: a visible-but-inert control invites a tap that does nothing, which the toddler-usability tenet treats as a broken interaction rather than a safe default.

Today the configuration gates the canvas feature set as a single bundle (color picker, New Picture) — there's no per-feature independence yet. Finer-grained gating is deferred until the feature set grows past what one bundle covers.

A resolved-feature change that arrives while a gesture is live (per Input Arbitration) doesn't recompose the screen immediately — the live gesture, including New Picture's own held sequence, finishes first, and the new composition applies starting with the next gesture. A control or color swatch never disappears out from under a pointer that's already down on it.

## Lifecycle Behavior

### New Picture

Triggered from its Widgets button, which — per Input Arbitration above — is only reachable when no stroke is active on Painting. The action runs a fixed sequence:

1. User Experience asks Painting whether the current drawing is empty. Painting owns this determination; User Experience owns what happens as a result.
2. If the drawing isn't empty, User Experience calls Painting's `save()` (see the Painting LLD), which writes the drawing to Image Storage. If it is empty, this step is skipped — no blank drawing is ever written.
3. Painting's canvas is cleared.

No confirmation dialog is shown, per the HLD's reversible-actions-default-to-forgiving tenet — the toddler can't parse or dismiss a confirmation prompt, and whenever there was content to lose, it's already recoverable because it was saved before the clear. The sequence doesn't surface an intermediate state that requires a response before drawing can resume.

The whole sequence runs as one held gesture: per Input Arbitration, User Experience keeps the blocking layer up over both Widgets and Painting from the triggering button-press through step 3 completing — including across the asynchronous `save()` call in step 2. No new stroke or control-press can start partway through.

### OS Navigation and Process Lifecycle

The OS back gesture/button is consumed and ignored — it never navigates the toddler out of the kid canvas — at least under younger-age configurations; whether an older-age bundle ever re-enables it is deferred (see Feature Gating and Open Questions). This holds regardless of whether device-locking from the HLD's first-launch adult setup dialog is active — it's a baseline default on its own.

The current, in-progress drawing must survive any lifecycle event the OS handles through its own saved-instance-state mechanism — configuration changes, brief backgrounding, process death within that scope — the same category of survival Compose's `rememberSaveable` (or its Compose Multiplatform equivalent) provides for ordinary UI state. This is a distinct mechanism from the New Picture auto-save path above: routine OS lifecycle churn isn't a completed drawing worth writing to the permanent store, it's mid-work state that should simply still be there when the screen returns.

## Interaction Feedback

Actions that User Experience orchestrates across components answer the toddler through the action's own visible effect, not through any separate dialog, confirmation, or extra interaction step:

- **Color selection** — Widgets owns the visual mechanism (e.g. which swatch shows as active); User Experience owns `StyleSettings`, the resolved brush and background source Painting reads from (see the Painting LLD) — color is a property of the brush instance `StyleSettings` resolves, not a separate value, so writing the toddler's swatch selection into `StyleSettings` means constructing a freshly colored brush instance for the next resolved-brush query. The next stroke Painting starts is simply in the new color, with no separate acknowledgment step required. Brush *type* has no selection input yet — `StyleSettings` always resolves an instance of Painting's single default brush type, just constructed with a color from a `ColorSource` (see the Painting Style LLD's Color Sources). The drawing surface's background is resolved the same way but has no selection input at all yet — no Widgets control writes into it (see the Painting LLD's Style Settings section for when Painting queries it).

  No swatch-tap wiring exists yet (Widgets has no implementation — see Open Questions), so today's `StyleSettings` implementation resolves both colors from fixed `ColorSource`s rather than anything a toddler's tap has chosen. The background is `ConstantColor(white)`. The stroke color is `RandomColor`, sampling a fresh color for every stroke: hue uniform across the full color wheel, saturation constant at `1.0`, and brightness sampled as a disc's radius via a linear distribution weighted toward the outer edge — the combination that makes the sampled hue/brightness point uniform over the disc's *area*, not just uniform along the radius (see the Painting Style LLD's Distributions).
- **New Picture** — the cleared canvas is itself the feedback: the toddler sees the drawing surface go blank, confirming the action took effect. No separate confirmation banner or toast is shown, consistent with no blocking dialogs on the kid canvas.

Feedback belonging to a single control's own activation (a button's press animation, for example) is owned by Widgets, not here — this section covers only feedback for actions User Experience itself orchestrates across components.

## Decisions & Alternatives

| Decision | Chosen | Alternatives Considered | Rationale |
|----------|--------|------------------------|-----------|
| Gated-off feature representation | Omit the control entirely | Show it disabled/greyed out | A disabled control invites a tap the toddler can't interpret the failure of — omission avoids a dead interaction rather than explaining one. |
| New Picture confirmation | None — auto-save-then-clear | Blocking confirmation dialog; clear-then-offer-undo toast | Direct application of the HLD's reversible-actions-default-to-forgiving tenet: the action is already recoverable via auto-save, so a confirmation step adds friction without adding safety. |
| Screen layering | Painting full-bleed as background; Widgets chrome anchored to the edges on top | Fixed non-overlapping regions (e.g. a dedicated toolbar strip above a smaller canvas area) | Maximizes the reachable drawing area and avoids losing screen real estate to a dedicated toolbar band, while edge anchoring keeps controls reachable without precise fine-motor reach. |
| Input arbitration between Widgets and Painting | Two mechanisms: ordinary Compose z-order dispatch decides which component *claims* a fresh touch (Widgets on top, Painting beneath); a User-Experience-composed transparent blocking layer over the non-claiming component keeps every other pointer out for the live gesture's duration | A single hand-rolled arbiter that inspects every raw pointer itself and manually routes it to Widgets or Painting; per-touch heuristics to classify incidental vs. intentional touches; redirect an imprecise touch to the nearest control it probably meant to hit | Claiming is already exactly what Compose's own hit-testing does for free when Widgets sits above Painting — reimplementing it by hand would just be a slower, bug-prone copy of the toolkit's own dispatch. A blocking layer for exclusivity is the smallest addition on top of that default behavior, and keeps Widgets and Painting mutually unaware of each other (neither needs to know the other exists) since User Experience alone owns composing the blocker. |
| Signal a live gesture's blocking-layer needs | Widgets: each control's own `onPressedChange` callback. Painting: its own `onStrokeActiveChange` callback (see the Painting LLD's Reporting Stroke State), invoked the same way — `true` at stroke start, `false` at stroke end | A dedicated "gesture live" event/state exposed by each component; User Experience inferring stroke liveness by driving Painting's pointer-input calls itself | Painting owns its own pointer input (see the Painting LLD's Composable Shape) rather than being driven by User Experience directly, so it needs the same kind of explicit signal Widgets' controls already provide. Matching callback shapes keeps Input Arbitration symmetric between the two components User Experience composes, rather than each reporting liveness a different way. |
| Auto-save on New Picture when the drawing is empty | Skipped — no save write, canvas still clears | Always auto-save regardless of content | Avoids cluttering the saved-drawings store with blank entries. Ownership stays split: Painting reports whether there's content, User Experience decides what that fact means. |
| OS back gesture | Consumed and ignored, at least under younger-age configurations | Standard OS back behavior (exits the app); mapping the gesture to an in-app action (e.g. New Picture) | A toddler navigating out of the app unsupervised is the failure mode the HLD's first-launch device-locking exists to prevent; ignoring back is a baseline defense independent of whether locking is active. Exact age cutoff, if any, is deferred. |
| Surviving OS-managed lifecycle events (rotation, brief backgrounding, saved-instance-state-scoped process death) | Current drawing persists via the platform's saved-instance-state mechanism, separate from the New Picture auto-save path | Rely on the New Picture auto-save path for this too; accept loss of in-progress work across these events | Routine OS churn isn't a completed drawing worth writing to the permanent store — it's ordinary transient UI state, no different from any other Compose screen's saved-instance-state handling — and shouldn't force a save-then-clear the toddler never asked for. |
| Ownership of `StyleSettings` (Painting's resolved brush and background source) | User Experience implements and owns it, writing the toddler's swatch selection into it as the color to construct the next resolved brush instance with (brush type has no selection input yet, so it always resolves an instance of Painting's single default type); the resolved background has no write path yet, so it's a fixed default | Painting owns an active brush directly as its own mutable state, written via a setter call from User Experience | Matches the Painting LLD's decision to keep Painting's stroke rendering ignorant of how a resolved brush gets decided. User Experience already owns Widgets' color-selection wiring, so it's the natural place to decide — and later evolve — what the resolved brush and background are. |
| New Picture's hold on the arbiter across its asynchronous save | Held for the whole sequence — button-press through `clear()` completing, spanning the `save()` call | Release the arbiter right after the triggering press, before `save()` begins | Painting's `save()` writes to Image Storage and isn't instant. Holding the slot for the whole sequence means a touch made during that window is simply never accepted, rather than accepted as a new stroke and then invisibly erased a moment later by `clear()` — worse for a toddler than the touch just not registering. |
| Feature-gating change arriving during a live gesture | Applied only once the current gesture ends; never recomposes mid-gesture | Recompose immediately, mid-gesture, per Config's normal reactivity | A control or color swatch disappearing out from under a pointer already down on it is a worse toddler experience than a one-gesture-long delay in applying a parent's config change. |

## Open Questions & Future Decisions

### Resolved

1. ✅ User Experience observes UX-config changes live, not just at launch — Config exposes a reactive/observable read API. See the Config LLD's Reads Are Reactive section.
2. ✅ Painting is a hoisted-state Compose pattern owning its own pointer input, reporting stroke liveness via `onStrokeActiveChange` — see the Painting LLD's Composable Shape and Reporting Stroke State.
3. ✅ A resolved brush's color can change automatically between strokes, without a new toddler tap — today's stroke color source is `RandomColor`, sampling a fresh color every stroke. See Interaction Feedback and the Painting Style LLD's Color Sources.

### Deferred

1. What happens if the auto-save on New Picture fails (storage full, write error)? Image Storage reports the failure to the caller, but what User Experience does in response is undefined — today's design assumes the save always succeeds before the canvas clears.
2. Per-feature gating granularity: once the feature set grows past the current single bundle, which features become independently toggle-able needs its own pass — including whether/at what age OS back navigation is ever re-enabled.
3. Exact composition geometry — which edge each Widgets control anchors to, spacing, sizing — is deferred to implementation and visual design, not fixed here.
4. How `StyleSettings`' implementation constructs a correctly typed and colored brush instance once Painting Style supports more than one brush type is that LLD's own open question (see its Open Questions #4) — today's single-brush-type implementation just constructs that one type directly with a color from a `ColorSource`.
5. Ownership of the HLD's first-launch adult setup dialog (age slider, device-lock button, link into the Companion App, "don't show again" toggle) isn't assigned to any Kid Canvas component yet. User Experience, as the kid canvas's composition root, is the likely owner, but this LLD doesn't cover it — today's screen composition and lifecycle behavior are defined without it.
6. What resolved-feature values User Experience reads before Config's UX configuration has ever been written (first run, before any dialog or Companion App write) is Config's own default-resolution question, not decided here — see the Config LLD.
7. Whether the blocking layer should cover the entire screen unconditionally whenever any gesture is live — driven by one shared "a gesture is live" state — rather than the asymmetric "cover only the non-active component" rule in Input Arbitration, isn't decided. A whole-screen rule would give Painting the same externally-visible second-touch protection Widgets needs (today Painting's own single-`pointerId` filtering is its only defense against a second concurrent finger) and would remove the need to determine which side is active before composing the overlay; Compose fixes a pointer's dispatch target at its own down event, so covering the already-active component too wouldn't disrupt its in-progress gesture. Not decided here — the current asymmetric design stands until this gets its own pass.
8. Widgets and the blocking-layer overlay have no implementation yet — `KidCanvasScreen` composes only Painting today (see the Painting LLD's Composable Shape). Every EARS spec describing arbitration, feature gating, lifecycle sequences, or OS navigation is deferred until that composition-root wiring lands.

## References

- Parent sub-HLD: `docs/intent/kid-canvas/kid-canvas-design.md` — defines User Experience as Kid Canvas's composition root.
- Root HLD: `docs/high-level-design.md` — Approach (age-adaptive canvas UX, first-launch adult setup), Tenets (toddler usability over platform convention; reversible actions default to forgiving), System Design (shared data store and UX config flow).
- Sibling: `docs/intent/kid-canvas/painting/painting-design.md` — the stroke model and save/clear operations this LLD's Lifecycle Behavior orchestrates.
- Sibling: `docs/intent/kid-canvas/painting-style/painting-style-design.md` — the `ColorSource`/`Distribution` implementations behind today's `StyleSettings` wiring.
- `docs/intent/config/config-design.md` — UX configuration reactivity, storage shape, and resolved-feature accessors.
- `docs/intent/image-storage/image-storage-design.md` — saved-drawing write failures.
