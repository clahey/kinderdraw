# Image Storage — EARS Specs

## Data Shape

- [ ] **IMAGES-001**: The system shall persist each saved drawing as an entry containing a unique identifier, a creation timestamp, and a rendered raster image.
- [ ] **IMAGES-002**: The system shall expose an operation to create a new saved-drawing entry from a caller-provided raster image, generating a unique identifier and creation timestamp for any the caller doesn't supply.
- [ ] **IMAGES-003**: When the create operation is called with an id that already has an entry, the system shall update that entry's raster image in place rather than creating a second entry.
- [ ] **IMAGES-004**: When the create operation is called on an existing id without an explicit timestamp, the system shall leave that entry's stored timestamp unchanged rather than refreshing it to the write time.
- [ ] **IMAGES-005**: The system shall expose an operation to list all saved-drawing entries.
- [ ] **IMAGES-006**: The system shall expose an operation to read a saved drawing's raster image by its identifier.
- [ ] **IMAGES-007**: The system shall expose an operation to delete a saved-drawing entry by its identifier.

## Scope

- [ ] **IMAGES-008**: The system shall maintain one Image Storage instance per platform app, shared in-process by that platform's Kid Canvas and Companion App.

## Durability

- [ ] **IMAGES-009**: The system shall durably persist saved-drawing entries such that an entry created by one Image Storage instance remains readable by a later Image Storage instance, after the instance that created it no longer exists.

## Android Storage Backend

- [ ] **IMAGES-010**: On Android, the system shall persist saved-drawing entries via the device's shared MediaStore, in a dedicated album distinct from the general camera roll, making them visible in the Photos/Gallery app.
- [ ] **IMAGES-011**: On Android, the system shall store a saved-drawing entry's creation timestamp in its MediaStore entry's `DATE_TAKEN` metadata field.

## Linux Storage Backend

- [ ] **IMAGES-012**: On Linux, the system shall default to storing saved drawings under a dedicated subdirectory of the user's Pictures directory (`~/Pictures/kinderdraw`).
- [ ] **IMAGES-013**: On Linux, the system shall allow the storage location to be configured to a location other than the default.

## Reactivity

- [ ] **IMAGES-014**: When a caller subscribes to the saved-drawing list, the system shall immediately deliver the current list of entries to that subscriber.
- [ ] **IMAGES-015**: When a saved-drawing entry is added or removed by any means — whether through Image Storage's own create/delete operations or otherwise (e.g. directly through the system Gallery/Photos app on Android, or a file manager on Linux) — the system shall notify subscribed callers of the change rather than requiring them to re-read on their own schedule.

## Write Failures

- [ ] **IMAGES-016**: If a create or delete operation fails (e.g. storage full, I/O error), then the system shall report the failure to the caller rather than crashing.
- [ ] **IMAGES-017**: If the read or delete operation is called with an identifier that has no corresponding entry, then the system shall report the failure to the caller.
