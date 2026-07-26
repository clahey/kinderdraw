package net.clahey.kinderdraw.shared.paintingstyle

/**
 * A pluggable numeric sampling strategy — see the Painting Style LLD's
 * Distributions section.
 */
interface Distribution {
    fun sample(): Float
}
