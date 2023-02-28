package com.michaelsgroi.baseballreference

import kotlin.test.Test

class BrWarDailyPitchLinesTest {

    private val testee = BrWarDailyLines(BrWarDaily.warDailyPitchFile, BrWarDaily.SeasonType.PITCHING)

    @Test
    fun testTopCareerWars() {
        val playersByWarDescending = testee.getCareers().sortedByDescending { it.pitchingWar() }
        val topPlayersByWar =
            playersByWarDescending.take(5).associate { it.playerName to it.pitchingWar() }
        val expectedTop5PlayersByWar = mapOf(
            "Cy Young" to 165.6,
            "Walter Johnson" to 152.25,
            "Roger Clemens" to 138.65,
            "Kid Nichols" to 116.7,
            "Pete Alexander" to 116.2
        )
        assert(topPlayersByWar == expectedTop5PlayersByWar) { "Expected $expectedTop5PlayersByWar, but was $topPlayersByWar" }
    }

}