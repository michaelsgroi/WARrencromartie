package com.michaelsgroi.baseballreference

import kotlin.test.Test

class BrWarDailyPitchLinesTest {

    private val testee: BrWarDailyPitchLines = BrWarDailyPitchLines()

    @Test
    fun testTopCareerWars() {
        val playersByWarDescending = testee.getPitcherCareers().sortedByDescending { it.pitchingWar() }
        val topPlayersByWar =
            playersByWarDescending.take(5).associate { it.pitchingWar() to it.playerName }.toSortedMap(reverseOrder())
        val expectedTop5PlayersByWar = mapOf(
            165.6 to "Cy Young",
            152.25 to "Walter Johnson",
            138.65 to "Roger Clemens",
            116.7 to "Kid Nichols",
            116.2 to "Pete Alexander"
        )
        assert(topPlayersByWar == expectedTop5PlayersByWar) { "Expected $expectedTop5PlayersByWar, but was $topPlayersByWar" }
    }

}