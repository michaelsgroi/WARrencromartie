package com.michaelsgroi.baseballreference

import org.junit.jupiter.api.Test

class BrWarDailyTests {

    private val testee = BrWarDaily()

    @Test
    fun topFive2004RedSox() {
        val topPlayersByWar = testee.getRosters().first { roster ->
            roster.rosterId == RosterId(2004, "bos")
        }.players.sortedByDescending { it.war() }.take(5).associate { it.playerName to it.war().roundToDecimalPlaces(2).toDouble() }
        val expectedTop5PlayersByWar = mapOf(
            "Pedro Martinez" to 83.9,
            "Curt Schilling" to 79.5,
            "Manny Ramirez" to 69.3,
            "Johnny Damon" to 56.3,
            "David Ortiz" to 55.3
        )
        assert(topPlayersByWar == expectedTop5PlayersByWar) {
            "Expected $expectedTop5PlayersByWar, but was $topPlayersByWar"
        }
    }

    @Test
    fun testBattingPitcher() {
        val career = testee.getCareers().first { it.playerName == "Pud Galvin" }
        val careerStr = """
            { "war":70.15, "pitchingWar":79.65, "battingWar":-9.45, "war/season":5.85, "id":"galvipu01", "name":"Pud Galvin", "seasons":12, "seasonsRange":"1879-1892"}
        """.trimIndent()
        assert(careerStr == career.toString()) { "Expected $careerStr, but was $career" }
    }

    @Test
    fun testMidSeasonTraded() {
        val career = testee.getCareers().first { it.playerName == "Donnie Sadler" }
        val careerStr = """
            { "war":-0.9, "pitchingWar":0.0, "battingWar":-0.9, "war/season":-0.1, "id":"sadledo01", "name":"Donnie Sadler", "seasons":8, "seasonsRange":"1998-2007"}
        """.trimIndent()
        assert(careerStr == career.toString()) { "Expected $careerStr, but was $career" }
    }

    @Test
    fun seasonDerivations() {
        testee.getCareers().forEach { career ->
            val seasonCount = career.seasonCount()
            val x = career.seasons()
            assert(x.size == seasonCount) { "Expected ${x.size} to be $seasonCount" }
            val roundToDecimalPlaces = x.sumOf { it.war }.roundToDecimalPlaces(2)
            val war = career.war.roundToDecimalPlaces(2)
            assert(war == roundToDecimalPlaces) { "Expected $war to be $roundToDecimalPlaces" }
        }
    }

    @Test
    fun testTopCareerWars() {
        val playersByWarDescending = testee.getCareers().sortedByDescending { it.war() }
        val topPlayersByWar = playersByWarDescending.take(5).associate { it.playerName to it.war().roundToDecimalPlaces(2).toDouble() }
        val expectedTop5PlayersByWar = mapOf(
            "Babe Ruth" to 183.05,
            "Walter Johnson" to 164.9,
            "Cy Young" to 163.6,
            "Barry Bonds" to 162.8,
            "Willie Mays" to 156.15
        )
        assert(topPlayersByWar == expectedTop5PlayersByWar) { "Expected $expectedTop5PlayersByWar, but was $topPlayersByWar" }
    }

    @Test
    fun testTopCareerWarsPitching() {
        val testee = BrWarDailyLines(BrWarDaily.warDailyPitchFile, BrWarDaily.SeasonType.PITCHING)
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


    @Test
    fun testTopCareerWarsBatting() {
        val testee = BrWarDailyLines(BrWarDaily.warDailyBatFile, BrWarDaily.SeasonType.BATTING)
        val playersByWarDescending = testee.getCareers().sortedByDescending { it.battingWar() }
        val topPlayersByWar =
            playersByWarDescending.take(5).associate { it.playerName to it.battingWar().roundToDecimalPlaces(1).toDouble() }
        val expectedTop5PlayersByWar = mapOf(
            "Barry Bonds" to 162.8,
            "Babe Ruth" to 162.7,
            "Willie Mays" to 156.1,
            "Ty Cobb" to 151.4,
            "Henry Aaron" to 143.0
        )
        assert(topPlayersByWar == expectedTop5PlayersByWar) { "Expected $expectedTop5PlayersByWar, but was $topPlayersByWar" }
    }

}