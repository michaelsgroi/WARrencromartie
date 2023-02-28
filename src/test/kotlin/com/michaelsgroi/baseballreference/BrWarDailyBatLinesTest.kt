package com.michaelsgroi.baseballreference

import kotlin.test.Test

class BrWarDailyBatLinesTest {

    private val testee = BrWarDailyLines(BrWarDaily.warDailyBatFile, BrWarDaily.SeasonType.BATTING)

    @Test
    fun testTopCareerWars() {
        val playersByWarDescending = testee.getCareers().sortedByDescending { it.battingWar() }
        val topPlayersByWar =
            playersByWarDescending.take(5).associate { it.playerName to it.battingWar() }
        val expectedTop5PlayersByWar = mapOf(
            "Barry Bonds" to 162.8,
            "Babe Ruth" to 162.7,
            "Willie Mays" to 156.15,
            "Ty Cobb" to 151.4,
            "Henry Aaron" to 143.0
        )
        assert(topPlayersByWar == expectedTop5PlayersByWar) { "Expected $expectedTop5PlayersByWar, but was $topPlayersByWar" }
    }

}