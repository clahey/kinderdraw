package net.clahey.kinderdraw.shared.paintingstyle

/** Returns a scripted sequence of values, one per [sample] call. */
class FakeDistribution(private val values: List<Float>) : Distribution {
    private var index = 0
    override fun sample(): Float = values[index++]
}
