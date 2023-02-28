package com.michaelsgroi.baseballreference

import kotlin.test.Test

class BrWarDailyBatLinesTest {

    private val testee = BrWarDailyLines(BrWarDaily.warDailyBatFile, BrWarDaily.SeasonType.BATTING)

    @Test
    fun testTopCareerWars() {
        val playersByWarDescending = testee.getCareers().sortedByDescending { it.battingWar() }
        val topPlayersByWar =
            playersByWarDescending.take(5).associate { it.battingWar() to it.playerName }.toSortedMap(reverseOrder())
        val expectedTop5PlayersByWar = mapOf(
            162.8 to "Barry Bonds",
            162.7 to "Babe Ruth",
            156.15 to "Willie Mays",
            151.4 to "Ty Cobb",
            143.0 to "Henry Aaron"
        )
        assert(topPlayersByWar == expectedTop5PlayersByWar) { "Expected $expectedTop5PlayersByWar, but was $topPlayersByWar" }
    }

}