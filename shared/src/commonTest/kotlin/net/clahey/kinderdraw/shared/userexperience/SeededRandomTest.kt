package net.clahey.kinderdraw.shared.userexperience

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals
import kotlin.test.assertTrue

/** Draws enough values to make an accidental match between two streams vanishingly unlikely. */
private fun sample(seed: Long?, name: String, generation: Int, count: Int = 12): List<Float> {
    val random = namedRandom(seed, name, generation)
    return List(count) { random.nextFloat() }
}

class SeedFromTest {
    // @spec CANVAS-UX-046
    @Test
    fun nothingRequestedMeansNoSeed() {
        assertEquals(null, seedFrom(null))
    }

    // @spec CANVAS-UX-045
    @Test
    fun aNumberIsTheSeedItself() {
        assertEquals(42L, seedFrom(42L))
        assertEquals(42L, seedFrom(42))
    }

    // @spec CANVAS-UX-045
    @Test
    fun aWordIsHashedIntoASeed() {
        assertEquals("banana".hashCode().toLong(), seedFrom("banana"))
    }

    // @spec CANVAS-UX-045
    @Test
    fun theSameWordAlwaysMeansTheSameSeed() {
        assertEquals(seedFrom("banana"), seedFrom("banana"))
        assertNotEquals(seedFrom("banana"), seedFrom("kumquat"))
    }

    // @spec CANVAS-UX-045
    @Test
    fun aNumericWordIsNotTheNumber() {
        // "42" arrives as a string from a command line; it seeds something, but
        // there is no promise it seeds the same thing the number 42 does.
        assertNotEquals(seedFrom(42L), seedFrom("42"))
    }

    // @spec CANVAS-UX-045
    @Test
    fun wordsProduceUsableSeeds() {
        val words = listOf("banana", "kumquat", "toddler", "scribble", "crayon", "spiral")
        val streams = words.map { word -> sample(seedFrom(word), "strokeColor", 0) }

        assertEquals(words.size, streams.distinct().size)
    }
}

class SeededRandomTest {
    // @spec CANVAS-UX-045
    @Test
    fun aSeedMakesTheStreamRepeatable() {
        assertEquals(sample(42L, "strokeColor", 0), sample(42L, "strokeColor", 0))
    }

    // @spec CANVAS-UX-045
    @Test
    fun differentSeedsGiveDifferentStreams() {
        assertNotEquals(sample(42L, "strokeColor", 0), sample(43L, "strokeColor", 0))
    }

    // @spec CANVAS-UX-045
    @Test
    fun adjacentSeedsDoNotOverlapOnTheirFirstValues() {
        // Deriving a stream directly from the seed makes neighbouring seeds share
        // early values, which surfaces when someone tries seeds one after another.
        val firsts = List(24) { sample(500L + it, "strokeColor", 0, count = 1).single() }

        assertEquals(24, firsts.distinct().size)
    }

    // @spec CANVAS-UX-046
    @Test
    fun noSeedLeavesTheStreamUnseeded() {
        assertNotEquals(sample(null, "strokeColor", 0), sample(null, "strokeColor", 0))
    }

    // @spec CANVAS-UX-047
    @Test
    fun namesUnderTheSameSeedAreIndependent() {
        assertNotEquals(sample(42L, "strokeColor", 0), sample(42L, "background", 0))
    }

    // @spec CANVAS-UX-047
    @Test
    fun aNameKeepsItsStreamWhateverOtherNamesExist() {
        // The isolation that matters: adding a second source later must not shift
        // the first one's values, so a name's stream depends on nothing but itself.
        val before = sample(42L, "strokeColor", 0)
        repeat(50) { namedRandom(42L, "background", 0).nextFloat() }

        assertEquals(before, sample(42L, "strokeColor", 0))
    }

    // @spec CANVAS-UX-048
    @Test
    fun eachGenerationIsANewStream() {
        val generations = List(6) { sample(42L, "strokeColor", it) }

        assertEquals(6, generations.distinct().size)
    }

    // @spec CANVAS-UX-049
    @Test
    fun aGenerationIsReconstructedFromItsCountAlone() {
        // What a restore has to work from is the count and nothing else.
        assertEquals(sample(42L, "strokeColor", 3), sample(42L, "strokeColor", 3))
        assertNotEquals(sample(42L, "strokeColor", 3), sample(42L, "strokeColor", 4))
    }

    // @spec CANVAS-UX-049
    @Test
    fun generationsStayDistinctFarFromZero() {
        val generations = List(8) { sample(42L, "strokeColor", 100 + it) }

        assertEquals(8, generations.distinct().size)
    }

    // @spec CANVAS-UX-050
    @Test
    fun aWholeSessionReplaysIdentically() {
        // Six strokes, a recreation, then four more — twice over.
        fun session(): List<Float> {
            val first = namedRandom(42L, "strokeColor", 0)
            val before = List(6) { first.nextFloat() }
            val second = namedRandom(42L, "strokeColor", 1)
            return before + List(4) { second.nextFloat() }
        }

        assertEquals(session(), session())
    }

    // @spec CANVAS-UX-050
    @Test
    fun valuesAreSpreadRatherThanClumped() {
        // Deriving each value from seed-plus-a-counter yields a stratified sequence
        // that bins dead flat; a real stream scatters. Ten bins over 3000 draws have
        // a standard deviation near 16, so a spread this tight indicates structure.
        val random = namedRandom(42L, "strokeColor", 0)
        val bins = IntArray(10)
        repeat(3000) { bins[(random.nextFloat() * 10).toInt().coerceIn(0, 9)]++ }
        val spread = bins.max() - bins.min()

        assertTrue(spread > 10, "bin spread $spread is too flat to be an independent stream")
    }
}
