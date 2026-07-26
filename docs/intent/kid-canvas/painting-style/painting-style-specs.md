# Painting Style — EARS Specs

## Brushes

- [ ] **CANVAS-STYLE-001**: The system shall only provide `Brush` implementations whose `Stroke` can render incrementally — extending the visible rendering as each new point is captured, without requiring the stroke's full, final point list in advance.
- [ ] **CANVAS-STYLE-002**: When a stroke created by `DefaultBrush` renders, the system shall draw a fixed-width solid line connecting its captured points as a polyline, with no curve-fitting or smoothing.

## Color Sources

- [ ] **CANVAS-STYLE-003**: When `ConstantColor` is queried via `getNextColor()`, the system shall return the color it was constructed with, every time.
- [ ] **CANVAS-STYLE-004**: When `RandomColor` is queried via `getNextColor()`, the system shall sample a fresh value from each of its hue, saturation, and value `Distribution`s and compose them into a color, independently of any earlier query.
- [ ] **CANVAS-STYLE-010**: When `RandomColor` samples its saturation or value `Distribution`, the system shall clamp the sampled value into `[0, 1]` before composing the resulting color, regardless of what the `Distribution` returns.

## Distributions

- [ ] **CANVAS-STYLE-005**: When `ConstantDistribution` is sampled, the system shall return the value it was constructed with, every time.
- [ ] **CANVAS-STYLE-006**: When `UniformDistribution` is sampled, the system shall return a value uniformly distributed across its configured `[min, max]` range.
- [ ] **CANVAS-STYLE-007**: When `LinearDistribution` is sampled, the system shall return a value from its configured `[min, max]` range whose density is linear across that range, matching the ratio between its `weightAtMin` and `weightAtMax` parameters.
- [ ] **CANVAS-STYLE-008**: When `LinearDistribution` is constructed with a negative `weightAtMin` or `weightAtMax`, or with both equal to zero, the system shall reject the construction rather than produce an ill-defined distribution.
- [ ] **CANVAS-STYLE-009**: When `UniformDistribution` or `LinearDistribution` is constructed with `min` greater than `max`, the system shall reject the construction rather than produce an inverted or negative-width range.
