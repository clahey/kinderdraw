package net.clahey.kinderdraw.shared.paintingstyle

/** Always samples the same value — see the Painting Style LLD's Distributions section. */
class ConstantDistribution(private val value: Float) : Distribution {
    // @spec CANVAS-STYLE-005
    override fun sample(): Float = value
}
