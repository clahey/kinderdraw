# Painting — EARS Specs

## Stroke Model

- [ ] **CANVAS-PAINT-001**: When a pointer goes down on the drawing surface, the system shall begin recording a stroke as an ordered list of points, querying Active Stroke Settings (Painting's resolved color/brush source, owned by User Experience) once at that moment for the stroke's color and brush, and fixing those returned values for the stroke's entire duration regardless of any later change at the source.
- [ ] **CANVAS-PAINT-002**: When a pointer sequence ends with no movement (a tap), the system shall still record it as a single-point stroke rather than discarding it.
- [ ] **CANVAS-PAINT-003**: The system shall treat the drawing as the ordered set of all strokes recorded since the drawing surface was last cleared.

## Brushes

- [ ] **CANVAS-PAINT-004**: The system shall delegate all rendering of a stroke's captured points to that stroke's active brush (line width, shape, point interpolation, and any other visual effect), performing no point-to-pixel rendering decisions of its own.
- [ ] **CANVAS-PAINT-005**: The system shall only accept brush implementations that can render a stroke incrementally — extending the visible rendering as each new point is captured, without requiring the stroke's full, final point list in advance.
- [ ] **CANVAS-PAINT-006**: The system shall render every stroke using its single default brush (today's only implementation satisfying the brush interface): a fixed-width solid line connecting points as a polyline, with no curve-fitting or smoothing.

## Rendering

- [ ] **CANVAS-PAINT-007**: When a new point is captured for a live stroke, the system shall extend that stroke's visible rendering immediately via the stroke's active brush, rather than waiting until the pointer lifts to render anything.

## Save and Clear

- [ ] **CANVAS-PAINT-008**: The system shall expose an operation that reports true only if no strokes have been recorded since the drawing surface was last cleared.
- [D] **CANVAS-PAINT-009**: When the save operation is called, the system shall render the current drawing to a raster image and write it to Image Storage (see the Image Storage LLD) as a new saved-drawing entry.
- [D] **CANVAS-PAINT-012**: If the save operation's write to Image Storage fails, then the system shall report that failure to its own caller rather than treating the drawing as saved.
- [ ] **CANVAS-PAINT-010**: When the clear operation is called, the system shall discard all recorded strokes and reset the visible drawing surface to blank.
- [ ] **CANVAS-PAINT-013**: When the clear operation is called while a stroke is still in progress (the pointer hasn't lifted), the system shall finalize that stroke's points-so-far as a completed stroke, discard it along with everything else, and immediately begin a new stroke continuing from the same pointer location and carrying forward the interrupted stroke's own color and brush, without querying Active Stroke Settings again. (A subsequent call to the isEmpty check reports false, since that replacement stroke already has one point — see CANVAS-PAINT-002.)

## Lifecycle Survival

- [D] **CANVAS-PAINT-011**: When the OS recreates the process's UI without ending the process itself (e.g. a configuration change such as device rotation, or brief backgrounding that doesn't reclaim the process), the system shall preserve the current drawing exactly as it stood, including any stroke still in progress, and restore the same visible drawing surface, without invoking the save operation.
- [D] **CANVAS-PAINT-015**: When the OS ends the process and later recreates it within the scope of its own saved-instance-state mechanism, the system shall restore the drawing surface with every stroke that had already completed (pointer lifted) before the process ended, without invoking the save operation. A stroke still in progress at the moment the process ended shall not be restored.
- [ ] **CANVAS-PAINT-014**: The system shall store each captured point as a fraction of the drawing surface's width and height at the moment it was captured, and render it at the corresponding position of the drawing surface's current width and height, so a stroke captured before a proportional resize (e.g. a device rotation preserved under CANVAS-PAINT-011) still renders at the same relative position afterward.
