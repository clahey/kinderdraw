package net.clahey.kinderdraw.shared.userexperience

import kotlin.random.Random

/**
 * The seed a caller asked for, or null if it asked for none.
 *
 * Whatever it hands over will do: a number is the seed, anything else is
 * hashed into one. Seeds get chosen by trying them until the colors look
 * right and then written down, so a word has to work as well as a number —
 * and a caller sending the request shouldn't have to match a declared type
 * to be understood. See the User Experience LLD's Seeding the Sampled Colors.
 */
// @spec CANVAS-UX-045, CANVAS-UX-046
fun seedFrom(requested: Any?): Long? = when (requested) {
    null -> null
    is Number -> requested.toLong()
    else -> requested.hashCode().toLong()
}

/**
 * The generator for one named source of randomness on the kid canvas — see
 * the User Experience LLD's Seeding the Sampled Colors.
 *
 * With [seed] null, which is every launch that didn't ask for a particular
 * drawing, this is just [Random.Default] and colors differ every time
 * (CANVAS-UX-046).
 *
 * With a seed, [name] separates this source from every other one, so the
 * draws one source makes can never shift another's values (CANVAS-UX-047).
 * The stream is reached through a second generator that does nothing but
 * hand out stream seeds: [generation] says how many streams to skip, which
 * is what lets a source be rebuilt after an OS-driven recreation from a
 * single saved count (CANVAS-UX-048, CANVAS-UX-049).
 *
 * Going through that second generator, rather than deriving a stream from
 * the seed directly, is also what keeps neighbouring seeds from sharing
 * early values — which matters because trying seeds one after another is
 * exactly how someone looks for colors they like.
 */
// @spec CANVAS-UX-045, CANVAS-UX-046, CANVAS-UX-047, CANVAS-UX-048, CANVAS-UX-049, CANVAS-UX-050
fun namedRandom(seed: Long?, name: String, generation: Int): Random {
    if (seed == null) return Random.Default

    val seeds = Random(seed xor name.hashCode().toLong())
    repeat(generation) { seeds.nextLong() }
    return Random(seeds.nextLong())
}
