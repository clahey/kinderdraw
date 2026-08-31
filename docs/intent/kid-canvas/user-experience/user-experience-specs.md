# User Experience — EARS Specs

## Screen Composition

- [x] **CANVAS-UX-001**: When the kid canvas screen is composed, the system shall render Painting full-bleed as the drawing surface and compose Widgets controls as chrome anchored along the screen's edges on top of it, so no control ever sits over the drawing area.

## Input Arbitration

- [x] **CANVAS-UX-002**: When a pointer touches down inside some Widgets control's hit region and no component holds the interaction, the system shall grant the interaction to that control, which claims the pointer.
- [x] **CANVAS-UX-003**: When a pointer touches down outside every Widgets control's hit region, and either no component holds the interaction or Painting itself is the holder, the system shall route that pointer to Painting instead, starting a stroke — even when the touch missed a nearby control's hit region.
- [x] **CANVAS-UX-004**: While one component holds the interaction, the system shall refuse it to every other component, so no other Widgets control can claim a pointer and no stroke can start on Painting.
- [x] **CANVAS-UX-005**: When a component is refused the interaction, the system shall keep that refusal in force for the remainder of that component's gesture — a pointer already down when the holder releases stays inert until it lifts and touches down again, and only a subsequent pointer-down is eligible to start the next gesture.
- [x] **CANVAS-UX-020**: The system shall grant the interaction to at most one component at a time, refusing every request made while it is held.
- [x] **CANVAS-UX-021**: The system shall provide no way to release the interaction other than through the hold object a successful request returned, so a component that never acquired the interaction cannot release it.
- [x] **CANVAS-UX-022**: When a hold that has already been released is released again, the system shall leave the interaction's current holder unchanged, so a stale hold cannot free a later holder's interaction.
- [x] **CANVAS-UX-023**: The system shall request and release the interaction only from the UI dispatcher, and shall carry no synchronization of its own. A control's release after a suspending activation satisfies this by running in a composition-tied scope — see CANVAS-WIDGETS-024.
- [x] **CANVAS-UX-024**: When the kid canvas screen is composed, including after an OS-driven recreation, the system shall start with the interaction unheld rather than restoring any hold in effect beforehand.
- [x] **CANVAS-UX-027**: The system shall request the interaction only on an input event carrying at least one pointer newly touching down, and only while the requesting component isn't already holding it — so a hovering pointer never takes the interaction, and a pointer joining a gesture already held doesn't request a second time.
- [x] **CANVAS-UX-026**: When two pointers touch down in the same input event, one inside a Widgets control's hit region and one outside every control's hit region, the system shall grant the interaction to exactly one of them and refuse the other. Which one wins is unspecified — the two requests race, and either outcome is acceptable. Layering decides only which component a *single* pointer reaches at all, per CANVAS-UX-002 and CANVAS-UX-003. The exactly-one-wins half is verified at the lock itself rather than through the screen: the test harness enqueues each touch-down as its own input event, so two pointers arriving in one event can't be constructed from a test.

## Feature Gating

- [D] **CANVAS-UX-006**: When composing the screen, the system shall include only the Widgets controls and optional behaviors whose backing feature Config resolves as on, omitting the rest entirely rather than rendering them disabled.
- [D] **CANVAS-UX-007**: The system shall gate today's canvas feature set (color picker, New Picture) as a single bundle, resolved as one unit rather than per-feature.
- [D] **CANVAS-UX-008**: When Config's resolved feature values change while a gesture is live, the system shall defer applying the resulting composition change until that gesture ends, rather than recomposing mid-gesture.

## Lifecycle Behavior — New Picture

- [ ] **CANVAS-UX-009**: When New Picture's pointer is claimed, the system shall hold the interaction from that moment through that same pointer's release, so no other control-press or stroke can start during the press itself — regardless of whether the release goes on to activate New Picture.
- [ ] **CANVAS-UX-019**: When New Picture activates, the system shall run the sequence below as that activation's own work, under the hold its press already took, keeping that hold until the sequence completes rather than releasing it at the press's own release.
- [x] **CANVAS-UX-010**: When the New Picture sequence runs, the system shall first ask Painting whether the current drawing is empty.
- [x] **CANVAS-UX-011**: When the New Picture sequence's emptiness check reports the drawing is not empty, the system shall call Painting's save operation with no id, so Image Storage creates a new entry, before clearing.
- [x] **CANVAS-UX-012**: When the New Picture sequence's emptiness check reports the drawing is empty, the system shall skip the save call entirely, writing nothing to Image Storage.
- [x] **CANVAS-UX-013**: When the New Picture sequence reaches its final step, the system shall clear Painting's canvas, regardless of whether a save was performed.
- [ ] **CANVAS-UX-014**: The system shall show no confirmation dialog or intermediate state for the New Picture sequence — the toddler can resume drawing immediately once it completes.
- [ ] **CANVAS-UX-025**: When an OS-driven recreation cancels a New Picture sequence before it completes, the system shall leave the current drawing in place on the restored screen and resume no part of that sequence.

## Lifecycle Behavior — OS Navigation and Process Lifecycle

- [D] **CANVAS-UX-015**: When the OS back gesture or button fires while the kid canvas is shown, the system shall consume it and not navigate the toddler out of the kid canvas.
- [D] **CANVAS-UX-016**: When the OS recreates the process's UI within its own saved-instance-state mechanism (a configuration change, brief backgrounding, or process death within that scope), the system shall preserve the current in-progress drawing exactly as it stood, without invoking the New Picture save path. Deferred alongside Painting's CANVAS-PAINT-011 and CANVAS-PAINT-019, which this depends on.

## Interaction Feedback

- [D] **CANVAS-UX-017**: When a color swatch is activated, the system shall write that color into `StyleSettings` as the color for the next resolved brush instance, so Painting's next stroke starts in the new color with no separate acknowledgment step.
- [ ] **CANVAS-UX-018**: The system shall provide no feedback for the New Picture sequence beyond the cleared canvas itself — no confirmation banner or toast.
