#!/usr/bin/env python3
"""
Capture the Play Store screenshots from a DEBUG build of KinderDraw.

The canvas holds strokes, not a bitmap, so there is nothing to seed into a
database — the drawing is produced by synthesizing touches through the real
input pipeline. Each scribble is a normalized point list, scaled to whatever
device it runs on and injected as one DOWN / MOVE... / UP gesture.

The target device serial (from `adb devices`) comes from `-s/--serial` or
$ANDROID_SERIAL. Run once per device, with a filename prefix per device
(AVD names are local to each machine, so the script never hardcodes them):

    ./screenshots.py -s emulator-5554 capture shot-      # 1080x1920 phone
    ./screenshots.py -s emulator-5556 capture tablet7-   # 7" tablet
    # or: ANDROID_SERIAL=emulator-5554 ./screenshots.py capture shot-

Shots land in docs/store-listing/screenshots/ (override with OUT_DIR=...).

Primitives are also exposed for manual/one-off use (all honor -s / $ANDROID_SERIAL):
    ./screenshots.py -s <serial> clear
    ./screenshots.py -s <serial> launch
    ./screenshots.py -s <serial> demo on|off
    ./screenshots.py -s <serial> draw <scribble>
    ./screenshots.py -s <serial> shot <name.png>
    ./screenshots.py -s <serial> scribbles            # list what can be drawn
"""

from __future__ import annotations

import argparse
import math
import os
import re
import shutil
import subprocess
import sys
import time
from pathlib import Path

PKG: str = "net.clahey.kinderdraw"
ACTIVITY: str = f"{PKG}/.MainActivity"
EXTRA_RANDOM_SEED: str = "net.clahey.kinderdraw.extra.RANDOM_SEED"
SCRIPT_DIR: Path = Path(__file__).resolve().parent
REPO_ROOT: Path = SCRIPT_DIR.parents[2]  # .claude/skills/screenshots -> repo root
OUT_DIR: Path = Path(os.environ.get("OUT_DIR") or REPO_ROOT / "docs/store-listing/screenshots")

_SIZE_RE: re.Pattern[str] = re.compile(rb"Physical size:\s*(\d+)x(\d+)")


class CaptureError(Exception):
    """A capture operation failed (device unreachable, unknown scribble, ...)."""


# --- scribbles -------------------------------------------------------------
#
# Each returns points in a 0..1 square, origin top-left. They are deliberately
# loose and asymmetric: the app's subject is the mark a toddler actually makes,
# so a clean geometric curve would misrepresent it (see the Publishing LLD's
# icon rationale, which rejects the same tidiness for the same reason).


def _loops(turns: float, cx: float, cy: float, r: float, wobble: float, n: int = 44) -> list[tuple[float, float]]:
    pts = []
    for i in range(n):
        t = i / (n - 1)
        a = t * turns * 2 * math.pi
        rad = r * (0.35 + 0.65 * t) * (1 + wobble * math.sin(a * 3.7))
        pts.append((cx + rad * math.cos(a), cy + rad * math.sin(a) * 1.15))
    return pts


def _zigzag(y0: float, y1: float, n: int = 9) -> list[tuple[float, float]]:
    pts = []
    for i in range(n):
        t = i / (n - 1)
        pts.append((0.14 + 0.72 * t, y0 + (y1 - y0) * t + (0.09 if i % 2 else -0.09)))
    return pts


def _arc(cx: float, cy: float, r: float, a0: float, a1: float, n: int = 26) -> list[tuple[float, float]]:
    return [(cx + r * math.cos(a0 + (a1 - a0) * i / (n - 1)),
             cy + r * math.sin(a0 + (a1 - a0) * i / (n - 1))) for i in range(n)]


# A scribble is a list of strokes; each stroke is one uninterrupted gesture.
SCRIBBLES: dict[str, list[list[tuple[float, float]]]] = {
    "spiral": [_loops(2.4, 0.50, 0.46, 0.30, 0.06)],
    "scribble": [
        _loops(1.8, 0.38, 0.38, 0.24, 0.10),
        _zigzag(0.68, 0.74),
    ],
    "busy": [
        _loops(2.1, 0.36, 0.34, 0.22, 0.08),
        _arc(0.66, 0.52, 0.20, math.pi * 0.8, math.pi * 2.1),
        _zigzag(0.74, 0.80),
        [(0.20, 0.20), (0.34, 0.13), (0.46, 0.22)],
    ],
    "single": [_arc(0.50, 0.48, 0.28, math.pi * 0.75, math.pi * 2.3)],
}


class Adb:
    """A thin adb transport bound to one device serial.

    The adb binary is resolved once per execution and cached on the class, so
    even multiple devices share a single lookup.
    """

    _bin: str | None = None  # cached path to the adb executable

    def __init__(self, serial: str) -> None:
        self.serial = serial
        self._size: tuple[int, int] | None = None

    @classmethod
    def binary(cls) -> str:
        if cls._bin is None:
            found = shutil.which("adb")
            if not found:  # SDK need not be on PATH
                for cand in (
                    Path(os.environ.get("ANDROID_HOME", "")) / "platform-tools" / "adb",
                    Path.home() / "Android/Sdk/platform-tools/adb",
                    Path.home() / "Library/Android/sdk/platform-tools/adb",
                ):
                    if os.access(cand, os.X_OK):
                        found = str(cand)
                        break
            if not found:
                raise FileNotFoundError("adb not found on PATH or in a common SDK location")
            cls._bin = found
        return cls._bin

    def run(self, *args: str, check: bool = False, capture: bool = False) -> subprocess.CompletedProcess[bytes]:
        return subprocess.run([self.binary(), "-s", self.serial, *args],
                              check=check, capture_output=capture)

    def out(self, *args: str) -> bytes:
        """Raw stdout bytes of an adb call."""
        return self.run(*args, capture=True).stdout

    def shell(self, *args: str, check: bool = False, capture: bool = False) -> subprocess.CompletedProcess[bytes]:
        return self.run("shell", *args, check=check, capture=capture)

    def require(self) -> None:
        if self.run("get-state", capture=True).returncode != 0:
            raise CaptureError(f"no device at '{self.serial}' (see `adb devices`)")

    def size(self) -> tuple[int, int]:
        """Display size in pixels, so normalized scribbles land the same on any device."""
        if self._size is None:
            m = _SIZE_RE.search(self.out("shell", "wm", "size"))
            if not m:
                raise CaptureError(f"could not read display size from {self.serial}")
            self._size = (int(m.group(1)), int(m.group(2)))
        return self._size


def draw(adb: Adb, name: str) -> None:
    """Inject one scribble as real touches, one gesture per stroke."""
    strokes = SCRIBBLES.get(name)
    if strokes is None:
        raise CaptureError(f"unknown scribble '{name}' (try: {', '.join(sorted(SCRIBBLES))})")
    adb.require()
    w, h = adb.size()
    # Inset so no point lands in a system gesture zone, where the launcher would
    # swallow the touch instead of the canvas receiving it.
    mx, my = int(w * 0.06), int(h * 0.10)
    iw, ih = w - 2 * mx, h - 2 * my

    for stroke in strokes:
        pts = [(mx + int(x * iw), my + int(y * ih)) for x, y in stroke]
        cmds = [f"input motionevent DOWN {pts[0][0]} {pts[0][1]}"]
        cmds += [f"input motionevent MOVE {x} {y}" for x, y in pts[1:]]
        cmds.append(f"input motionevent UP {pts[-1][0]} {pts[-1][1]}")
        # One adb round-trip per stroke; each `input` still spawns its own
        # process on device, so a long stroke takes a few seconds.
        adb.shell("; ".join(cmds), check=True, capture=True)
    print(f"drew '{name}' ({len(strokes)} stroke(s)) on {adb.serial}")


def clear(adb: Adb) -> None:
    """Empty the canvas. The drawing lives in saved instance state, so a force-stop drops it."""
    adb.require()
    adb.shell("am", "force-stop", PKG, check=True)
    print("canvas cleared (process stopped).")


def demo(adb: Adb, state: str) -> None:
    adb.require()
    b = ("am", "broadcast", "-a", "com.android.systemui.demo")
    if state == "on":
        adb.shell("settings", "put", "global", "sysui_demo_allowed", "1")
        adb.shell(*b, "-e", "command", "enter", capture=True)
        adb.shell(*b, "-e", "command", "clock", "-e", "hhmm", "1000", capture=True)
        adb.shell(*b, "-e", "command", "battery", "-e", "level", "100",
                  "-e", "plugged", "false", capture=True)
        adb.shell(*b, "-e", "command", "network", "-e", "wifi", "show",
                  "-e", "level", "4", capture=True)
        adb.shell(*b, "-e", "command", "notifications", "-e", "visible", "false", capture=True)
        print("Demo mode on.")
    else:
        adb.shell(*b, "-e", "command", "exit", capture=True)
        print("Demo mode off.")


def launch(adb: Adb, seed: str | None = None) -> None:
    """Start the app, optionally fixing the colors it will draw.

    The seed goes over as a string extra and the app hashes it, so seeds can be
    words. A word survives being written down next to an approved screenshot in
    a way a large signed number does not.
    """
    adb.require()
    extra = ("--es", EXTRA_RANDOM_SEED, seed) if seed else ()
    # -W blocks until the first frame is actually displayed. Without it a cold start
    # returns immediately and an early screencap catches an unpainted (blank) frame.
    adb.shell("am", "start", "-W", "-n", ACTIVITY, *extra, capture=True)
    time.sleep(1)
    print(f"Launched {PKG}" + (f" with seed '{seed}'." if seed else "."))


def shot(adb: Adb, name: str) -> None:
    adb.require()
    OUT_DIR.mkdir(parents=True, exist_ok=True)
    (OUT_DIR / name).write_bytes(adb.out("exec-out", "screencap", "-p"))
    print(f"saved -> {OUT_DIR / name}")


def capture(adb: Adb, prefix: str, seed: str | None = None) -> None:
    """The whole shot set for one device.

    Every shot is the same screen with a different drawing on it — that is all
    the app has today. Each starts from a force-stop so no drawing bleeds into
    the next.

    Each shot gets its own seed, derived from the run's seed and the scribble's
    name, so the set is reproducible without every shot opening on the same
    color. Re-run with the same `--seed` to regenerate an approved set exactly.
    """
    print(f"== capturing on {adb.serial} (prefix '{prefix}') ==")
    demo(adb, "on")
    for index, name in enumerate(("scribble", "spiral", "busy", "single"), start=1):
        clear(adb)
        launch(adb, seed=f"{seed}-{name}" if seed else None)
        draw(adb, name)
        time.sleep(1)  # let the last MOVE render before grabbing the frame
        shot(adb, f"{prefix}{index:02d}-{name}.png")
    demo(adb, "off")


def main() -> None:
    p = argparse.ArgumentParser(description="KinderDraw Play Store screenshot capture.")
    p.add_argument("-s", "--serial", default=os.environ.get("ANDROID_SERIAL"),
                   help="device serial from `adb devices` (default: $ANDROID_SERIAL)")
    p.add_argument("--seed", help="fix the colors drawn; any word. Omit for fresh colors each run")
    sub = p.add_subparsers(dest="cmd", required=True)
    sub.add_parser("clear")
    sub.add_parser("launch")
    sub.add_parser("scribbles")
    sub.add_parser("demo").add_argument("state", choices=["on", "off"])
    sub.add_parser("draw").add_argument("name")
    sub.add_parser("shot").add_argument("name")
    sub.add_parser("capture").add_argument("prefix")
    a = p.parse_args()

    if a.cmd == "scribbles":  # needs no device
        for name, strokes in sorted(SCRIBBLES.items()):
            print(f"{name:10s} {len(strokes)} stroke(s), {sum(len(s) for s in strokes)} points")
        return

    if not a.serial:
        p.error("no device serial: pass --serial/-s or set ANDROID_SERIAL")

    adb = Adb(a.serial)
    actions = {
        "clear": lambda: clear(adb),
        "launch": lambda: launch(adb, a.seed),
        "demo": lambda: demo(adb, a.state),
        "draw": lambda: draw(adb, a.name),
        "shot": lambda: shot(adb, a.name),
        "capture": lambda: capture(adb, a.prefix, a.seed),
    }
    try:
        actions[a.cmd]()
    except (CaptureError, FileNotFoundError, subprocess.CalledProcessError) as e:
        print(f"error: {e}", file=sys.stderr)
        raise SystemExit(1)


if __name__ == "__main__":
    main()
