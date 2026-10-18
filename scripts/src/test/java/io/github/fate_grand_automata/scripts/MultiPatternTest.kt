package io.github.fate_grand_automata.scripts

import assertk.assertThat
import assertk.assertions.isEqualTo
import io.github.lib_automata.Match
import io.github.lib_automata.MultiPattern
import io.github.lib_automata.Pattern
import io.github.lib_automata.Region
import io.github.lib_automata.Size
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlin.test.Test

class MultiPatternTest {
    @Test
    fun testMultiPatternFindMatches() {
        val pattern1 = mockk<Pattern>()
        val pattern2 = mockk<Pattern>()
        val template = mockk<Pattern>()

        val match1 = Match(Region(10, 20, 30, 40), 0.9)
        val match2 = Match(Region(50, 60, 30, 40), 0.85)

        every { pattern1.findMatches(template, 0.7) } returns sequenceOf(match1)
        every { pattern2.findMatches(template, 0.7) } returns sequenceOf(match2)

        val multiPattern = MultiPattern(listOf(pattern1, pattern2))
        val results = multiPattern.findMatches(template, 0.7).toList()

        assertThat(results).isEqualTo(listOf(match1, match2))
    }

    @Test
    fun testMultiPatternClosesAll() {
        val pattern1 = mockk<Pattern>(relaxed = true)
        val pattern2 = mockk<Pattern>(relaxed = true)

        val multiPattern = MultiPattern(listOf(pattern1, pattern2))
        multiPattern.close()

        verify(exactly = 1) { pattern1.close() }
        verify(exactly = 1) { pattern2.close() }
    }
}
