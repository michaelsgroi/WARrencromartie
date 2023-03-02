package com.michaelsgroi.baseballreference

import com.michaelsgroi.baseballreference.Roster.RosterId
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.CsvSource

class BrWarDailyTests {

    private val testee = BrWarDaily()

    @Test
    fun topFive2004RedSox() {
        val topPlayersByWar = testee.rosters.first { roster ->
            roster.rosterId == RosterId(2004, "bos")
        }.players.sortedByDescending { it.war() }.take(5)
            .associate { it.playerName to it.war().roundToDecimalPlaces(2).toDouble() }
        val expectedTop5PlayersByWar = mapOf(
            "Pedro Martinez" to 83.9,
            "Curt Schilling" to 79.49,
            "Manny Ramirez" to 69.31,
            "Johnny Damon" to 56.31,
            "David Ortiz" to 55.31
        )
        assertEquals(expectedTop5PlayersByWar, topPlayersByWar) {
            "Expected $expectedTop5PlayersByWar, but was $topPlayersByWar"
        }
    }

    @Test
    fun testBattingPitcher() {
        val career = testee.careers.first { it.playerName == "Pud Galvin" }
        val careerStr = """
            { "war":70.17, "pitchingWar":79.63, "battingWar":-9.46, "war/season":5.85, "id":"galvipu01", "name":"Pud Galvin", "seasons":12, "seasonsRange":"1879-1892"}
        """.trimIndent()
        assertEquals(careerStr, career.toString()) { "Expected $careerStr, but was $career" }
    }

    @Test
    fun testMidSeasonTraded() {
        val career = testee.careers.first { it.playerName == "Donnie Sadler" }
        val careerStr = """
            { "war":-0.88, "pitchingWar":0.0, "battingWar":-0.88, "war/season":-0.11, "id":"sadledo01", "name":"Donnie Sadler", "seasons":8, "seasonsRange":"1998-2007"}
        """.trimIndent()
        assertEquals(careerStr, career.toString()) { "Expected $careerStr, but was $career" }
    }

    @Test
    fun seasonDerivations() {
        testee.careers.forEach { career ->
            val seasonCount = career.seasonCount()
            val seasons = career.seasons()
            assertEquals(seasonCount, seasons.size) { "Expected ${seasons.size} to be $seasonCount" }
            val roundToDecimalPlaces = seasons.sumOf { it.war }.roundToDecimalPlaces(2)
            val war = career.war.roundToDecimalPlaces(2)
            assertEquals(roundToDecimalPlaces, war) { "Expected $war to be $roundToDecimalPlaces" }
        }
    }

    @Test
    fun testTopCareerWars() {
        val playersByWarDescending = testee.careers.sortedByDescending { it.war() }
        val topPlayersByWar =
            playersByWarDescending.take(5).associate { it.playerName to it.war().roundToDecimalPlaces(2).toDouble() }
        val expectedTop5PlayersByWar = mapOf(
            "Babe Ruth" to 183.07,
            "Walter Johnson" to 164.9,
            "Cy Young" to 163.62,
            "Barry Bonds" to 162.79,
            "Willie Mays" to 156.14
        )
        assertEquals(expectedTop5PlayersByWar, topPlayersByWar) {
            "Expected $expectedTop5PlayersByWar, but was $topPlayersByWar"
        }
    }

    @Test
    fun testTopCareerWarsPitching() {
        val testee = BrWarDailyLines(BrWarDaily.warDailyPitchFile, BrWarDaily.SeasonType.PITCHING)
        val playersByWarDescending = testee.getCareers().sortedByDescending { it.pitchingWar() }
        val topPlayersByWar =
            playersByWarDescending.take(5)
                .associate { it.playerName to it.pitchingWar().roundToDecimalPlaces(1).toDouble() }
        val expectedTop5PlayersByWar = mapOf(
            "Cy Young" to 165.59,
            "Walter Johnson" to 152.25,
            "Roger Clemens" to 138.67,
            "Kid Nichols" to 116.71,
            "Pete Alexander" to 116.2
        )
        assertEquals(expectedTop5PlayersByWar, topPlayersByWar) {
            "Expected $expectedTop5PlayersByWar, but was $topPlayersByWar"
        }
    }


    @Test
    fun testTopCareerWarsBatting() {
        val testee = BrWarDailyLines(BrWarDaily.warDailyBatFile, BrWarDaily.SeasonType.BATTING)
        val playersByWarDescending = testee.getCareers().sortedByDescending { it.battingWar() }
        val topPlayersByWar =
            playersByWarDescending.take(5)
                .associate { it.playerName to it.battingWar().roundToDecimalPlaces(1).toDouble() }
        val expectedTop5PlayersByWar = mapOf(
            "Barry Bonds" to 162.79,
            "Babe Ruth" to 162.71,
            "Willie Mays" to 156.14,
            "Ty Cobb" to 151.39,
            "Henry Aaron" to 143.01
        )
        assertEquals(expectedTop5PlayersByWar, topPlayersByWar) {
            "Expected $expectedTop5PlayersByWar, but was $topPlayersByWar"
        }
    }

    @ParameterizedTest
    @CsvSource(
        "-0.005, -0.01",
        "-0.004, 0",
        "0.0, 0",
        "0.004, 0",
        "0.005, 0.01",
    )
    fun roundToDecimalPlaces(input: Double, expected: Double) {
        assertEquals(expected.toString(), input.roundToDecimalPlaces(2))
    }

}