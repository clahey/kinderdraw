# Widgets — EARS Specs

## Hit-Testing and Activation

- [x] **CANVAS-WIDGETS-001**: When a pointer touches down inside a control's hit region, the system shall claim that pointer for that control, fixed for the remainder of that pointer's gesture.
- [ ] **CANVAS-WIDGETS-002**: When a pointer's initial down location falls outside every control's hit region, the system shall not claim that pointer for a control even if it's later dragged into one — only an initial down inside a region claims it.
- [ ] **CANVAS-WIDGETS-003**: Once a pointer is claimed by a control, the system shall not reassign it to a different control even if the pointer is dragged into that other control's hit region.
- [x] **CANVAS-WIDGETS-004**: When a control claims a pointer, the system shall show that control's press feedback immediately, regardless of whether the pointer's eventual release activates the control.
- [x] **CANVAS-WIDGETS-005**: When a claimed pointer is released while positioned inside its control's hit region, the system shall activate that control.
- [x] **CANVAS-WIDGETS-006**: When a claimed pointer is released while positioned outside its control's hit region, the system shall still activate that control if the time spent continuously outside the region immediately before release is both under 100ms and less than the time the pointer spent inside the region immediately before that.
- [x] **CANVAS-WIDGETS-007**: When a claimed pointer is released outside its control's hit region and CANVAS-WIDGETS-006's tolerance isn't met, the system shall not activate that control.
- [ ] **CANVAS-WIDGETS-008**: When evaluating whether a claimed pointer's drift outside its region qualifies for CANVAS-WIDGETS-006's tolerance, the system shall measure that drift against the originally-claiming control's own hit region, not any other control's region the pointer may have drifted into.
- [ ] **CANVAS-WIDGETS-009**: The system shall size each control's hit region larger than its visible glyph, so an imprecise touch near but not exactly on the glyph still claims the control.

## Reporting Press State

- [x] **CANVAS-WIDGETS-010**: When a control claims a pointer, the system shall invoke that control's `onPressedChange` callback with `true`.
- [x] **CANVAS-WIDGETS-011**: When a control's claimed pointer is released, the system shall invoke that control's `onPressedChange` callback with `false`, regardless of whether that release activates the control.
- [x] **CANVAS-WIDGETS-012**: When a control activates (per Hit-Testing and Activation), the system shall invoke that control's `onActivate` callback exactly once, at the same release that activated it.
- [x] **CANVAS-WIDGETS-013**: When a control's claimed pointer is released without activating it, the system shall not invoke that control's `onActivate` callback.
- [x] **CANVAS-WIDGETS-014**: A control with multiple independent hit regions (e.g. Color Picker's swatches) shall report `onPressedChange`/`onActivate` independently per region, scoped only to the pointer claimed by that specific region.

## Control Catalog

- [ ] **CANVAS-WIDGETS-015**: Button shall expose a single hit region resulting in one activation action.
- [ ] **CANVAS-WIDGETS-016**: Color Picker shall expose one independent hit region per color swatch, each following the same hit-testing and activation rule as Button.
