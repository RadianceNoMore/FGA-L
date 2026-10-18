package io.github.lib_automata

import java.io.OutputStream

/**
 * A composite [Pattern] containing multiple alternate candidate patterns (e.g. for hybrid JP/EN language support).
 * When searched for in an image via [findMatches], any match against any sub-pattern is returned.
 */
class MultiPattern(
    val patterns: List<Pattern>,
    override var tag: String = ""
) : Pattern {
    override val width get() = patterns.firstOrNull()?.width ?: 0
    override val height get() = patterns.firstOrNull()?.height ?: 0

    override fun resize(size: Size): Pattern =
        MultiPattern(patterns.map { it.resize(size) }, tag)

    override fun resize(target: Pattern, size: Size) {
        patterns.firstOrNull()?.resize(target, size)
    }

    override fun findMatches(template: Pattern, similarity: Double): Sequence<Match> =
        patterns.asSequence().flatMap { it.findMatches(template, similarity) }

    override fun crop(region: Region): Pattern =
        MultiPattern(patterns.map { it.crop(region) }, tag)

    override fun save(stream: OutputStream) {
        patterns.firstOrNull()?.save(stream)
    }

    override fun copy(): Pattern =
        MultiPattern(patterns.map { it.copy() }, tag)

    override fun threshold(value: Double): Pattern =
        MultiPattern(patterns.map { it.threshold(value) }, tag)

    override fun isWhite(): Boolean =
        patterns.any { it.isWhite() }

    override fun isBlack(): Boolean =
        patterns.any { it.isBlack() }

    override fun floodFill(x: Double, y: Double, maxDiff: Double, newValue: Double): Pattern =
        MultiPattern(patterns.map { it.floodFill(x, y, maxDiff, newValue) }, tag)

    override fun fillText(): Pattern =
        MultiPattern(patterns.map { it.fillText() }, tag)

    override fun close() {
        for (pattern in patterns) {
            pattern.close()
        }
    }
}
