package com.michaelsgroi.baseballreference

import kotlin.test.Test

// TODO msgroi combine with BrWarDailyBatLinesTest
class BrWarDailyPitchLinesTest {

    private val testee = BrWarDailyLines(BrWarDaily.warDailyPitchFile, BrWarDaily.SeasonType.PITCHING)

    @Test
    fun testTopCareerWars() {
        val playersByWarDescending = testee.getCareers().sortedByDescending { it.pitchingWar() }
        val topPlayersByWar =
            playersByWarDescending.take(5).associate { it.playerName to it.pitchingWar().roundToDecimalPlaces(1).toDouble() }
        val expectedTop5PlayersByWar = mapOf(
            "Cy Young" to 165.6,
            "Walter Johnson" to 152.3,
            "Roger Clemens" to 138.7,
            "Kid Nichols" to 116.7,
            "Pete Alexander" to 116.2
        )
        assert(topPlayersByWar == expectedTop5PlayersByWar) { "Expected $expectedTop5PlayersByWar, but was $topPlayersByWar" }
    }

}