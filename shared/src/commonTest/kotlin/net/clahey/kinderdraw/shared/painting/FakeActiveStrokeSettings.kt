package net.clahey.kinderdraw.shared.painting

/** A settable, call-counting test double for [ActiveStrokeSettings]. */
class FakeActiveStrokeSettings(var brush: Brush = FakeBrush()) : ActiveStrokeSettings {
    var brushQueryCount = 0
        private set

    override fun getResolvedBrush(): Brush {
        brushQueryCount++
        return brush
    }
}
