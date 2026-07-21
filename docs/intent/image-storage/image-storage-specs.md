# Image Storage — EARS Specs

## Data Shape

- [ ] **IMAGES-001**: The system shall persist each saved drawing as an entry containing a unique identifier, a creation timestamp, and a rendered raster image.
- [ ] **IMAGES-002**: The system shall expose an operation to create a new saved-drawing entry from a caller-provided raster image, generating a unique identifier and creation timestamp for any the caller doesn't supply.
- [ ] **IMAGES-003**: The system shall expose an operation to list all saved-drawing entries.
- [ ] **IMAGES-004**: The system shall expose an operation to read a saved drawing's raster image by its identifier.
- [ ] **IMAGES-005**: The system shall expose an operation to delete a saved-drawing entry by its identifier.
- [ ] **IMAGES-011**: When the create operation is called with an id that already has an entry, the system shall update that entry's raster image in place rather than creating a second entry.
- [ ] **IMAGES-012**: When the create operation is called on an existing id without an explicit timestamp, the system shall leave that entry's stored timestamp unchanged rather than refreshing it to the write time.

## Scope

- [ ] **IMAGES-006**: The system shall maintain one Image Storage instance per platform app, shared in-process by that platform's Kid Canvas and Companion App.

## Reactivity

- [ ] **IMAGES-007**: When a caller subscribes to the saved-drawing list, the system shall immediately deliver the current list of entries to that subscriber.
- [ ] **IMAGES-008**: When a saved-drawing entry is added or deleted, the system shall notify subscribed callers of the change rather than requiring them to re-read on their own schedule.

## Write Failures

- [ ] **IMAGES-009**: If a create or delete operation fails (e.g. storage full, I/O error), then the system shall report the failure to the caller rather than crashing.
- [ ] **IMAGES-010**: If the read or delete operation is called with an identifier that has no corresponding entry, then the system shall report the failure to the caller.
