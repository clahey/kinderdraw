# User Experience — EARS Specs

## Screen Composition

- [D] **CANVAS-UX-001**: When the kid canvas screen is composed, the system shall render Painting full-bleed as the drawing surface and compose Widgets controls as chrome anchored along the screen's edges on top of it, so no control ever sits over the drawing area.

## Input Arbitration

- [D] **CANVAS-UX-002**: When a pointer touches down inside some Widgets control's hit region and no gesture is currently live, the system shall let that control claim the pointer as the live gesture, reported through that control's `onPressedChange(true)` callback.
- [D] **CANVAS-UX-003**: When a pointer touches down outside every Widgets control's hit region and no gesture is currently live, the system shall route that pointer to Painting instead, starting a stroke — even when the touch missed a nearby control's hit region.
- [D] **CANVAS-UX-004**: While a gesture is live — a Widgets control between its `onPressedChange(true)` and matching `onPressedChange(false)`, or Painting between its `onStrokeActiveChange(true)` and matching `onStrokeActiveChange(false)` — the system shall block every other pointer's down event from reaching whichever component isn't holding that gesture, so it can't claim a control or start a stroke.
- [D] **CANVAS-UX-005**: When the live gesture ends, the system shall accept only a subsequent pointer-down event as eligible to start the next live gesture — a pointer already held down at that moment, blocked under CANVAS-UX-004, stays blocked until it lifts and touches down again.

## Feature Gating

- [D] **CANVAS-UX-006**: When composing the screen, the system shall include only the Widgets controls and optional behaviors whose backing feature Config resolves as on, omitting the rest entirely rather than rendering them disabled.
- [D] **CANVAS-UX-007**: The system shall gate today's canvas feature set (color picker, New Picture) as a single bundle, resolved as one unit rather than per-feature.
- [D] **CANVAS-UX-008**: When Config's resolved feature values change while a gesture is live, the system shall defer applying the resulting composition change until that gesture ends, rather than recomposing mid-gesture.

## Lifecycle Behavior — New Picture

- [D] **CANVAS-UX-009**: When the New Picture control is activated, the system shall hold the live-gesture slot for the entire sequence below, from activation through the clear step completing, so no other control-press or stroke can start until it finishes.
- [D] **CANVAS-UX-010**: When the New Picture sequence runs, the system shall first ask Painting whether the current drawing is empty.
- [D] **CANVAS-UX-011**: When the New Picture sequence's emptiness check reports the drawing is not empty, the system shall call Painting's save operation (with no id, since this is the drawing's first save) before clearing, writing the drawing to Image Storage.
- [D] **CANVAS-UX-012**: When the New Picture sequence's emptiness check reports the drawing is empty, the system shall skip the save call entirely, writing nothing to Image Storage.
- [D] **CANVAS-UX-013**: When the New Picture sequence reaches its final step, the system shall clear Painting's canvas, regardless of whether a save was performed.
- [D] **CANVAS-UX-014**: The system shall show no confirmation dialog or intermediate state for the New Picture sequence — the toddler can resume drawing immediately once it completes.

## Lifecycle Behavior — OS Navigation and Process Lifecycle

- [D] **CANVAS-UX-015**: When the OS back gesture or button fires while the kid canvas is shown, the system shall consume it and not navigate the toddler out of the kid canvas.
- [D] **CANVAS-UX-016**: When the OS recreates the process's UI within its own saved-instance-state mechanism (a configuration change, brief backgrounding, or process death within that scope), the system shall preserve the current in-progress drawing exactly as it stood, without invoking the New Picture save path. Deferred alongside Painting's CANVAS-PAINT-011/CANVAS-PAINT-015, which this depends on.

## Interaction Feedback

- [D] **CANVAS-UX-017**: When a color swatch is activated, the system shall write that color into `StyleSettings` as the color for the next resolved brush instance, so Painting's next stroke starts in the new color with no separate acknowledgment step.
- [D] **CANVAS-UX-018**: The system shall provide no feedback for the New Picture sequence beyond the cleared canvas itself — no confirmation banner or toast.
