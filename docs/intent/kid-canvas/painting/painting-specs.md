# Painting — EARS Specs

## Composable Shape

- [x] **CANVAS-PAINT-018**: When a pointer touches down on the drawing surface, the system shall report a live stroke to its caller via `onStrokeActiveChange(true)`; when that same pointer lifts, ending the stroke, the system shall report `onStrokeActiveChange(false)`.

## Stroke Model

- [x] **CANVAS-PAINT-001**: When a pointer goes down on the drawing surface, the system shall begin recording a stroke as an ordered list of points, querying `StyleSettings` (Painting's active-brush source, owned by User Experience) once at that moment for the active brush and asking it to start the stroke.
- [x] **CANVAS-PAINT-002**: When a pointer sequence ends with no movement (a tap), the system shall still record it as a single-point stroke rather than discarding it.
- [x] **CANVAS-PAINT-003**: The system shall treat the drawing as the ordered set of all strokes recorded since the drawing surface was last cleared.

## Brushes

- [x] **CANVAS-PAINT-004**: The system shall delegate all rendering of a stroke's captured points to that stroke's active brush (line width, shape, color, point interpolation, and any other visual effect), performing no point-to-pixel rendering decisions of its own.

## Rendering

- [x] **CANVAS-PAINT-007**: When a new point is captured for a live stroke, the system shall extend that stroke's visible rendering immediately via the stroke's active brush, rather than waiting until the pointer lifts to render anything.
- [x] **CANVAS-PAINT-016**: The system shall query `StyleSettings`' active background color source for a color when Painting is constructed and again each time the clear operation is called, and shall fill the entire rendered area with that resolved color before drawing any strokes — both when rendering to the visible drawing surface and when rasterizing for the save operation.

## Save and Clear

- [x] **CANVAS-PAINT-008**: The system shall expose an operation that reports true only if no strokes have been recorded since the drawing surface was last cleared.
- [x] **CANVAS-PAINT-009**: When the save operation is called without an id, the system shall render the current drawing to a raster image, write it to Image Storage (see the Image Storage LLD) as a new saved-drawing entry, and return that entry's id to the caller.
- [x] **CANVAS-PAINT-017**: When the save operation is called with an id, the system shall render the current drawing to a raster image, write it to Image Storage as an update to the existing entry identified by that id rather than creating a new entry, and return that same id to the caller.
- [x] **CANVAS-PAINT-012**: If the save operation's write to Image Storage fails, then the system shall report that failure to its own caller rather than treating the drawing as saved.
- [x] **CANVAS-PAINT-010**: When the clear operation is called, the system shall discard all recorded strokes and reset the visible drawing surface to blank.
- [x] **CANVAS-PAINT-013**: When the clear operation is called while a stroke is still in progress (the pointer hasn't lifted), the system shall finalize that stroke's points-so-far as a completed stroke, discard it along with everything else, and immediately begin a new stroke continuing from the same pointer location and carrying forward the interrupted stroke's own brush, without querying `StyleSettings` again. (A subsequent call to the isEmpty check reports false, since that replacement stroke already has one point — see CANVAS-PAINT-002.)

## Lifecycle Survival

- [x] **CANVAS-PAINT-011**: When the OS recreates the process's UI within its own saved-instance-state mechanism — whether from a configuration change, brief backgrounding, or actual process death — the system shall restore the drawing surface with every stroke that had already completed before that moment, plus, if a stroke was still in progress at that moment, that stroke finalized into a completed stroke using whatever points it had captured so far (the same points-so-far finalization CANVAS-PAINT-013 performs for a mid-stroke clear operation, but without beginning a replacement stroke afterward, since no live pointer survives an OS-driven recreation to continue from), without invoking the save operation.
- [x] **CANVAS-PAINT-019**: When the OS recreates the process's UI within its own saved-instance-state mechanism, the system shall restore the drawing surface's background using whatever color was already resolved and in effect immediately before that moment, without querying `StyleSettings`' active background source again.
