package com.michaelsgroi.baseballreference

import org.junit.jupiter.api.Test

class BrWarDailyTests {

    private val testee = BrWarDaily()

    @Test
    fun topFive2004RedSox() {
        val topPlayersByWar = testee.getRosters().first { roster ->
            roster.rosterId == RosterId(2004, "bos")
        }.players.sortedByDescending { it.war() }.take(5).associate { it.playerName to it.war() }
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
        val topPlayersByWar = playersByWarDescending.take(5).associate { it.playerName to it.war() }
        val expectedTop5PlayersByWar = mapOf(
            "Babe Ruth" to 183.05,
            "Walter Johnson" to 164.9,
            "Cy Young" to 163.6,
            "Barry Bonds" to 162.8,
            "Willie Mays" to 156.15
        )
        assert(topPlayersByWar == expectedTop5PlayersByWar) { "Expected $expectedTop5PlayersByWar, but was $topPlayersByWar" }
    }

}