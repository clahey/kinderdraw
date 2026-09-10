# Image Storage — EARS Specs

## Data Shape

- [A] **IMAGES-001**: The system shall persist each saved drawing as an entry containing a unique identifier, a creation timestamp, and a rendered raster image.
- [A] **IMAGES-002**: The system shall expose an operation to create a new saved-drawing entry from a caller-provided raster image, generating a unique identifier and, unless the caller supplies one, a creation timestamp.
- [A] **IMAGES-003**: The system shall expose an operation to update an existing saved-drawing entry's raster image in place, by identifier, without creating a second entry.
- [A] **IMAGES-004**: When the update operation is called without an explicit timestamp, the system shall leave that entry's stored timestamp unchanged rather than refreshing it to the write time.
- [A] **IMAGES-005**: The system shall expose an operation to list all saved-drawing entries.
- [A] **IMAGES-006**: The system shall expose an operation to read a saved drawing's raster image by its identifier.
- [A] **IMAGES-007**: The system shall expose an operation to delete a saved-drawing entry by its identifier.

## Scope

- [ ] **IMAGES-008**: The system shall maintain one Image Storage instance per platform app, shared in-process by that platform's Kid Canvas and Companion App.

## Durability

- [A] **IMAGES-009**: The system shall durably persist saved-drawing entries such that an entry created by one Image Storage instance remains readable by a later Image Storage instance, after the instance that created it no longer exists.

## Android Storage Backend

- [A] **IMAGES-010**: On Android, the system shall persist saved-drawing entries via the device's shared MediaStore, in a dedicated album distinct from the general camera roll, making them visible in the Photos/Gallery app.
- [A] **IMAGES-011**: On Android, the system shall store a saved-drawing entry's creation timestamp in its MediaStore entry's `DATE_TAKEN` metadata field.
- [A] **IMAGES-018**: On Android, when determining which MediaStore entries belong to the app's album, the system shall compare album paths case-insensitively, so that every entry in the album directory is listed regardless of the casing its own MediaStore record spells that path with.

## Linux Storage Backend

- [ ] **IMAGES-012**: On Linux, the system shall default to storing saved drawings under a dedicated subdirectory of the user's Pictures directory (`~/Pictures/KinderDraw`).
- [ ] **IMAGES-013**: On Linux, the system shall allow the storage location to be configured to a location other than the default.

## Reactivity

- [A] **IMAGES-014**: When a caller subscribes to the saved-drawing list, the system shall immediately deliver the current list of entries to that subscriber.
- [A] **IMAGES-015**: When a saved-drawing entry is added or removed by any means — whether through Image Storage's own create/delete operations or otherwise (e.g. directly through the system Gallery/Photos app on Android, or a file manager on Linux) — the system shall notify subscribed callers of the change rather than requiring them to re-read on their own schedule.

## Write Failures

- [A] **IMAGES-016**: If a create, update, or delete operation fails (e.g. storage full, I/O error), then the system shall report the failure to the caller rather than crashing.
- [A] **IMAGES-019**: If a create operation fails after the entry has been brought into existence but before its image has been written completely, then the system shall remove that entry before reporting the failure, so that a create reported as failed leaves no entry any later list or read can observe.
- [A] **IMAGES-017**: If the read, update, or delete operation is called with an identifier that has no corresponding entry, then the system shall report the failure to the caller.
