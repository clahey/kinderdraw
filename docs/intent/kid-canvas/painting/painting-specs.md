# Painting — EARS Specs

## Holding the Lock

- [x] **CANVAS-PAINT-018**: When a pointer touches down while no other pointer is currently down on the drawing surface, the system shall request a hold from the interaction lock it was given; when the last pointer that's currently down lifts, leaving none down, the system shall release the hold it took. A pointer touching down or lifting while at least one other pointer remains down shall neither request nor release a hold. Whether any pointer remains down shall be evaluated once per input event, after every pointer change within that event has been applied, not separately for each individual change within it — so a pointer lifting and a different pointer touching down together in the same input event doesn't spuriously end and restart the gesture.
- [x] **CANVAS-PAINT-022**: When the interaction lock refuses Painting's request, the system shall begin no stroke and apply no pointer change to the drawing, and shall consume that gesture's remaining pointer events without requesting a hold again, until every pointer of that gesture has lifted.
- [x] **CANVAS-PAINT-024**: When an input event carries a pointer that is neither pressed nor newly lifted — a hovering pointer — the system shall neither consume that change nor record it against any stroke, leaving the drawing and the lock untouched.
- [x] **CANVAS-PAINT-023**: When Painting's pointer input is cancelled while it holds the lock, the system shall release that hold, leaving every stroke that was live at that moment untouched in the drawing state.

## Stroke Model

- [x] **CANVAS-PAINT-001**: When a pointer goes down on the drawing surface outside every reserved edge (see CANVAS-PAINT-026), the system shall begin recording a new stroke for that pointer — independent of any other pointer's stroke already in progress — as an ordered list of points, querying `StyleSettings` (Painting's active-brush source, owned by User Experience) once at that moment for the active brush and asking it to start the stroke.
- [x] **CANVAS-PAINT-002**: When a pointer sequence that began outside every reserved edge (see CANVAS-PAINT-026) ends with no movement (a tap), the system shall still record it as a single-point stroke rather than discarding it.
- [x] **CANVAS-PAINT-003**: The system shall treat the drawing as the ordered set of all strokes recorded since the drawing surface was last cleared.
- [x] **CANVAS-PAINT-020**: The system shall track each live stroke independently, keyed by its originating pointer, so that one pointer's movement, brush, or lift never affects any other concurrently-live pointer's own stroke.
- [x] **CANVAS-PAINT-021**: The system shall not impose any upper limit on the number of concurrent live strokes beyond whatever limit the underlying pointer input source itself reports.

## System Gesture Coexistence

In every spec below, the *reserved edges* are the bands along the drawing surface's edges that the platform reports as its mandatory system-gesture insets — the gesture regions it does not permit an app to exclude.

- [x] **CANVAS-PAINT-026**: When a pointer touches down inside a reserved edge, the system shall begin no stroke for that pointer and record no point against the drawing, notwithstanding CANVAS-PAINT-001 and CANVAS-PAINT-002.
- [x] **CANVAS-PAINT-027**: When a pointer touches down inside a reserved edge, the system shall not consume any of that pointer's changes, while still counting it towards the gesture CANVAS-PAINT-018 takes a hold for — so a gesture whose every pointer touched down inside a reserved edge still holds the interaction lock until its last pointer lifts, and such a pointer arriving while a stroke is live leaves that stroke and the hold covering it untouched. Where the interaction lock has refused the gesture outright, CANVAS-PAINT-022's swallowing covers every pointer of it, this one included.
- [x] **CANVAS-PAINT-032**: When a pointer touches down outside every reserved edge while a pointer that touched down inside one is still down, the system shall begin a stroke for it as it would for any other pointer, so a touch resting inside a reserved edge never leaves the rest of the drawing surface unable to start one.
- [x] **CANVAS-PAINT-028**: When a pointer that touched down inside a reserved edge later moves out of it, the system shall still begin no stroke for that pointer, for as long as that pointer remains down.
- [x] **CANVAS-PAINT-029**: When a stroke that began outside every reserved edge moves into one, the system shall go on recording and rendering that stroke's points there as anywhere else, both on the visible drawing surface and in the raster the snapshot operation produces.
- [x] **CANVAS-PAINT-030**: The system shall apply the suppression to each of the drawing surface's four edges independently, according to what the platform reserves along that edge, so an edge the platform reserves nothing along stays open to a stroke start no matter what it reserves along the others.
- [x] **CANVAS-PAINT-031**: The system shall read what the platform reserves as of each touch-down rather than from a fixed measurement, and where the platform reserves nothing on any edge shall leave the entire drawing surface open to a stroke start.

## Brushes

- [x] **CANVAS-PAINT-004**: The system shall delegate all rendering of a stroke's captured points to that stroke's active brush (line width, shape, color, point interpolation, and any other visual effect), performing no point-to-pixel rendering decisions of its own.

## Rendering

- [x] **CANVAS-PAINT-007**: When a new point is captured for a live stroke, the system shall extend that stroke's visible rendering immediately via the stroke's active brush, rather than waiting until the pointer lifts to render anything.
- [x] **CANVAS-PAINT-016**: The system shall query `StyleSettings`' active background color source for a color when Painting is constructed and again each time the clear operation is called, and shall fill the entire rendered area with that resolved color before drawing any strokes — both when rendering to the visible drawing surface and when rasterizing for the save operation.

## Save and Clear

- [x] **CANVAS-PAINT-008**: The system shall expose an operation that reports true only if no strokes have been recorded since the drawing surface was last cleared.
- [x] **CANVAS-PAINT-025**: When the snapshot operation is called, the system shall render the current drawing to a raster image — off screen, at the drawing surface's size as of the most recent render — and return that image to the caller without writing it anywhere or altering the drawing in any way.
- [x] **CANVAS-PAINT-010**: When the clear operation is called, the system shall discard all recorded strokes and reset the visible drawing surface to blank.
- [x] **CANVAS-PAINT-013**: When the clear operation is called while one or more strokes are still in progress (their pointers haven't lifted), the system shall finalize each such stroke's points-so-far as a completed stroke, discard all of them along with everything else, and immediately begin a new stroke per interrupted pointer — each keyed to that same pointer, continuing from its current location and carrying forward that pointer's own already-resolved brush, without querying `StyleSettings` again — for every interrupted pointer, including one whose current location is inside a reserved edge, since CANVAS-PAINT-026 tests a touch-down and a replacement stroke is not one. (A subsequent call to the isEmpty check reports false whenever at least one pointer was still down, since each replacement stroke already has one point — see CANVAS-PAINT-002.)

## Lifecycle Survival

- [x] **CANVAS-PAINT-011**: When the OS recreates the process's UI within its own saved-instance-state mechanism — whether from a configuration change, brief backgrounding, or actual process death — the system shall restore the drawing surface with every stroke that had already completed before that moment, plus, for every stroke still in progress at that moment, that stroke finalized into a completed stroke using whatever points it had captured so far (the same points-so-far finalization CANVAS-PAINT-013 performs per pointer for a mid-stroke clear operation, but without beginning a replacement stroke afterward, since no live pointer survives an OS-driven recreation to continue from), without invoking the save operation.
- [x] **CANVAS-PAINT-019**: When the OS recreates the process's UI within its own saved-instance-state mechanism, the system shall restore the drawing surface's background using whatever color was already resolved and in effect immediately before that moment, without querying `StyleSettings`' active background source again.
