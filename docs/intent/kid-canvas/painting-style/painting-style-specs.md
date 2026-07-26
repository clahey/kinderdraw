# Painting Style — EARS Specs

## Point

- [x] **CANVAS-STYLE-015**: The system shall store each captured point as a fraction of the drawing surface's width and height at the moment it was captured, and render it at the corresponding position of the drawing surface's current width and height, so a stroke captured before a proportional resize (e.g. a device rotation preserved under CANVAS-PAINT-011) still renders at the same relative position afterward.

## Brushes

- [x] **CANVAS-STYLE-001**: The system shall only provide `Brush` implementations whose `Stroke` can render incrementally — extending the visible rendering as each new point is captured, without requiring the stroke's full, final point list in advance.
- [x] **CANVAS-STYLE-002**: When a stroke created by `DefaultBrush` renders, the system shall draw a fixed-width solid line connecting its captured points as a polyline, with no curve-fitting or smoothing.
- [x] **CANVAS-STYLE-011**: When a `Stroke`'s `restart()` is called, the system shall produce a new `Stroke` continuing from the current stroke's last captured point, carrying forward the same brush and its already-resolved color — never querying the brush's `ColorSource` again — rather than constructing a fresh one.
- [x] **CANVAS-STYLE-012**: `AbstractSimpleBrush` shall resolve its color from its `ColorSource` exactly once per stroke, at that stroke's own creation; the resulting `Stroke` shall hold that color fixed for its entire rendering lifetime, never resolving a new one from the brush's `ColorSource` again.

## Color Sources

- [x] **CANVAS-STYLE-003**: When `ConstantColor` is queried via `getNextColor()`, the system shall return the color it was constructed with, every time.
- [x] **CANVAS-STYLE-004**: When `RandomColor` is queried via `getNextColor()`, the system shall sample a fresh value from each of its hue, saturation, and value `Distribution`s and compose them into a color, independently of any earlier query.
- [x] **CANVAS-STYLE-010**: When `RandomColor` samples its saturation or value `Distribution`, the system shall clamp the sampled value into `[0, 1]` before composing the resulting color, regardless of what the `Distribution` returns.

## Distributions

- [x] **CANVAS-STYLE-005**: When `ConstantDistribution` is sampled, the system shall return the value it was constructed with, every time.
- [x] **CANVAS-STYLE-006**: When `UniformDistribution` is sampled, the system shall return a value uniformly distributed across its configured `[min, max]` range.
- [x] **CANVAS-STYLE-007**: When `LinearDistribution` is sampled, the system shall return a value from its configured `[min, max]` range whose density is linear across that range, matching the ratio between its `weightAtMin` and `weightAtMax` parameters.
- [x] **CANVAS-STYLE-008**: When `LinearDistribution` is constructed with a negative `weightAtMin` or `weightAtMax`, or with both equal to zero, the system shall reject the construction rather than produce an ill-defined distribution.
- [x] **CANVAS-STYLE-009**: When `UniformDistribution`, `LinearDistribution`, or `PowerDistribution` is constructed with `min` greater than `max`, the system shall reject the construction rather than produce an inverted or negative-width range.
- [x] **CANVAS-STYLE-013**: When `PowerDistribution` is sampled, the system shall return a value from its configured `[min, max]` range whose density is proportional to `y^(exponent - 1)` — biased toward `max` for `exponent > 1`, toward `min` for `exponent < 1`, and uniform at `exponent = 1`.
- [x] **CANVAS-STYLE-014**: When `PowerDistribution` is constructed with an `exponent` that is zero or negative, the system shall reject the construction rather than produce an undefined distribution.
