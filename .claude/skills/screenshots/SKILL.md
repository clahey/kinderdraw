---
name: screenshots
description: Draw sample scribbles into a debug build of KinderDraw and capture Play Store screenshots from a device or emulator. Use when the user asks to take store screenshots, populate the canvas with demo drawings, or set up for a listing update.
---

# KinderDraw store screenshots

Puts drawings on the canvas and captures Play Store screenshots from a **debug**
build. `screenshots.py` in this directory drives the whole thing.

## How the drawing gets there

KinderDraw has no database and no content to seed. A drawing is a list of
strokes held in `PaintingState`, so the only way to produce one from outside the
app is to draw it — the script synthesizes touches with
`input motionevent DOWN/MOVE/UP`, one gesture per stroke, through the same
pointer pipeline a finger uses. Nothing is mocked and no debug-only drawing path
is involved, so what the screenshots show is what the app really does.

Scribbles are normalized point lists in `SCRIBBLES` (0..1, origin top-left),
scaled to whatever display the target device reports via `wm size`. They are
deliberately loose and asymmetric rather than clean geometry: the app's subject
is the mark a toddler actually makes.

All the points of one stroke go over in a single `adb shell` round-trip, which
keeps a 50-point scribble at a couple of seconds rather than a couple of
minutes.

Points are inset 6% horizontally and 10% vertically. Without that, a stroke
starting at a screen edge lands in a system gesture zone and the launcher
swallows it instead of the canvas receiving it.

## Why debug builds only

Nothing here needs `run-as`, so this would technically work against a release
install — but screenshots should come from the build being developed, and a
release install on a dev device is the wrong thing to photograph.

## Prerequisites

- `adb` — but you do **not** need it on `PATH`. `screenshots.py` resolves it
  itself: `PATH`, then `$ANDROID_HOME/platform-tools`, then `~/Android/Sdk`
  (Linux) / `~/Library/Android/sdk` (macOS). Only export `ANDROID_HOME` if your
  SDK lives somewhere non-standard.
- A device or emulator with the debug build installed
  (`./gradlew :androidApp:installDebug`).

## Capture

Play requires phone screenshots at **16:9 or 9:16**, each side 320–3840 px, and
wants **≥4 at ≥1080 px per side** for promotion eligibility. The clean target is
**1080×1920 portrait** (exactly 9:16, meets the 1080 minimum).

Use a **natively 16:9** emulator so no cropping is needed. Modern phones (Pixel
5 = 1080×2340 = 19.5:9) are the *wrong* ratio — a raw screenshot would be
rejected or need cropping. Create a **custom 1080×1920 phone** rather than
reusing a named device: Tools → Device Manager → Create Virtual Device → New
Hardware Profile, set the resolution to 1080×1920 (16:9), save, then pick a
recent system image. (The `Screenshot_Phone` AVD is exactly this.)

Run it once per device, selecting the device by adb serial (from `adb devices`)
via `-s/--serial` or `$ANDROID_SERIAL`, plus a filename prefix. It never
hardcodes AVD names, which are local to each machine:

```
./screenshots.py -s <phone-serial>  --seed banana capture shot-      # 1080x1920 phone
./screenshots.py -s <tablet-serial> --seed banana capture tablet7-   # 7" tablet
# or: ANDROID_SERIAL=<serial> ./screenshots.py capture shot-
```

Record the seed alongside the shots — it is what regenerates them later.

Each `capture` run turns on the clean status bar, then for each scribble
force-stops the app, relaunches, draws, and shoots. Shots land in
`docs/store-listing/screenshots/` (override with `OUT_DIR=...`).

Primitives for manual/one-off use, all taking `-s <serial>` or
`$ANDROID_SERIAL`: `clear`, `launch` (honors `--seed`), `demo on|off`,
`draw <scribble>`, `shot <name.png>`, and `scribbles` (lists what can be drawn;
needs no device).

## Clearing the canvas

`clear` just force-stops the app. The drawing lives in saved instance state — it
survives rotation and backgrounding by design (see the Painting LLD's Lifecycle
Survival) but not a force-stop, so this is enough and no `pm clear` is needed.

## Fixing the colours with a seed

Every stroke resolves a fresh random colour, so by default two runs of the same
scribble come out differently coloured. Pass `--seed` to fix them:

```
./screenshots.py -s <serial> --seed banana capture shot-
```

Any word works — the app hashes whatever it is given (see the User Experience
LLD's Seeding the Sampled Colors). Words are worth preferring over numbers
because a seed's whole job is to be written down next to an approved screenshot
and typed back in months later.

Each shot in a `capture` run gets its own derived seed (`<seed>-<scribble>`), so
the set is reproducible without every shot opening on the same colour.

**Colours reproduce exactly; geometry does not, quite.** Two runs at the same
seed produce the same colours, verified, but touch delivery timing varies a
little, so a stroke picks up marginally different points and antialiased edges
shift. Expect shots that are visually identical and differ in a fraction of a
percent of pixels — fine for a listing, not a basis for byte-comparison.

To re-roll, just pass a different seed; no rebuild is involved.

## Maintaining the scribbles

Edit `SCRIBBLES` in `screenshots.py`. Helpers are provided for the three shapes
that read as childlike (`_loops`, `_zigzag`, `_arc`), and a scribble is just a
list of strokes, so a hand-written point list works too. `./screenshots.py
scribbles` lists them without needing a device.

## When the app grows chrome

There is one screen and no `testTag` anywhere, so the script navigates nothing —
it draws and shoots. When controls arrive and shots need to show a button being
pressed or a sheet open, the sibling Trackr repo's `demo-data` skill has the
id-driven navigation to copy: `testTagsAsResourceId` on the app root, then
`uiautomator dump` parsed for a `resource-id`'s bounds, with a poll-and-re-tap
helper for taps dropped during an animation. Nothing here duplicates it, because
none of it can be exercised yet.
