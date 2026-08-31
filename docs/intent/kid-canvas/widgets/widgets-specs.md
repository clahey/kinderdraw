# Widgets — EARS Specs

## Hit-Testing and Activation

- [x] **CANVAS-WIDGETS-001**: When a pointer touches down inside a control's hit region and the interaction lock grants that control the interaction, the system shall claim that pointer for that control, fixed for the remainder of that pointer's gesture.
- [ ] **CANVAS-WIDGETS-002**: When a pointer's initial down location falls outside every control's hit region, the system shall not claim that pointer for a control even if it's later dragged into one — only an initial down inside a region claims it.
- [ ] **CANVAS-WIDGETS-003**: Once a pointer is claimed by a control, the system shall not reassign it to a different control even if the pointer is dragged into that other control's hit region.
- [x] **CANVAS-WIDGETS-004**: When a control claims a pointer, the system shall show that control's press feedback immediately, regardless of whether the pointer's eventual release activates the control.
- [x] **CANVAS-WIDGETS-005**: When a claimed pointer is released while positioned inside its control's hit region, the system shall activate that control.
- [x] **CANVAS-WIDGETS-006**: When a claimed pointer is released while positioned outside its control's hit region, the system shall still activate that control if the time spent continuously outside the region immediately before release is both under 100ms and less than the time the pointer spent inside the region immediately before that.
- [x] **CANVAS-WIDGETS-007**: When a claimed pointer is released outside its control's hit region and CANVAS-WIDGETS-006's tolerance isn't met, the system shall not activate that control.
- [ ] **CANVAS-WIDGETS-008**: When evaluating whether a claimed pointer's drift outside its region qualifies for CANVAS-WIDGETS-006's tolerance, the system shall measure that drift against the originally-claiming control's own hit region, not any other control's region the pointer may have drifted into.
- [ ] **CANVAS-WIDGETS-009**: The system shall size each control's hit region larger than its visible glyph, so an imprecise touch near but not exactly on the glyph still claims the control.

## Reporting Activation

- [x] **CANVAS-WIDGETS-012**: When a control activates (per Hit-Testing and Activation), the system shall invoke that control's `onActivate` callback exactly once, at the same release that activated it.
- [x] **CANVAS-WIDGETS-013**: When a control's claimed pointer is released without activating it, the system shall not invoke that control's `onActivate` callback.
- [x] **CANVAS-WIDGETS-014**: A control with multiple independent hit regions (e.g. Color Picker's swatches) shall claim, show press feedback, and report `onActivate` independently per region, scoped only to the pointer claimed by that specific region. Independence is about which region a pointer belongs to, not about concurrency: at most one region may hold the interaction at a time, so two regions can never be pressed at once (see CANVAS-UX-004).
- [x] **CANVAS-WIDGETS-023**: The system shall expose a control's press state only to that control's own rendering, reporting it to no caller.
- [x] **CANVAS-WIDGETS-026**: When a control's claimed pointer is released, the system shall end that control's press feedback, regardless of whether that release activates the control.
- [x] **CANVAS-WIDGETS-024**: The system shall invoke `onActivate` as a suspending call from a scope tied to the control's composition rather than to its pointer-event stream, so the release that triggers an activation does not cancel the activation it triggered, and so the hold's release lands back on the UI dispatcher even when the activation itself ran elsewhere (see CANVAS-UX-023).

## Interaction Arbitration Contract

- [x] **CANVAS-WIDGETS-018**: When a pointer touches down inside a control's hit region, the system shall request the interaction from the interaction lock that control was given, before claiming that pointer.
- [x] **CANVAS-WIDGETS-019**: When the interaction lock refuses a control's request, the system shall not claim that pointer, shall show no press feedback for it, and shall consume that gesture's remaining pointer events without requesting the interaction again, until every pointer of that gesture has lifted.
- [x] **CANVAS-WIDGETS-020**: While a control holds the interaction, that control shall ignore every pointer other than the one it claimed, for the remainder of that gesture — a pointer arriving at a different control is refused by the lock under CANVAS-WIDGETS-019 instead.
- [x] **CANVAS-WIDGETS-021**: When a control's claimed pointer is released without activating it, the system shall release that control's hold on the interaction at that release.
- [x] **CANVAS-WIDGETS-022**: When a control's claimed pointer is released and activates it, the system shall keep that control's hold on the interaction until the `onActivate` invocation completes, and shall release it then whether that invocation returned normally, failed, or was cancelled.
- [x] **CANVAS-WIDGETS-025**: When a control's own gesture is cancelled while it holds the interaction, the system shall release that hold.

## Control Catalog

- [ ] **CANVAS-WIDGETS-015**: Button shall expose a single hit region resulting in one activation action.
- [ ] **CANVAS-WIDGETS-016**: Color Picker shall expose one independent hit region per color swatch, each following the same hit-testing and activation rule as Button.

## System Gesture Coexistence

- [A] **CANVAS-WIDGETS-017**: On Android, the system shall register each control's current on-screen hit region as excluded from system edge-gesture navigation, so a touch beginning inside that region is not intercepted as a back-swipe or other left/right-edge system gesture.
