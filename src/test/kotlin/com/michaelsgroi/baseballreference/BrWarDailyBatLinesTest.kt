package com.michaelsgroi.baseballreference

import com.michaelsgroi.baseballreference.BrWarDailyBatLines.Career
import com.michaelsgroi.baseballreference.BrWarDailyBatLines.Season
import kotlin.test.Test

class BrWarDailyBatLinesTest {

    private val testee: BrWarDailyBatLines = BrWarDailyBatLines()

    @Test
    fun testTopCareerWars() {
        val playersByWarDescending = testee.getBatterCareers(warDailyBatFile).sortedByDescending { it.careerWar() }
        val topPlayersByWar =
            playersByWarDescending.take(5).associate { it.careerWar() to it.playerName }.toSortedMap(reverseOrder())
        val expectedTop5PlayersByWar = mapOf(
            162.8 to "Barry Bonds",
            157.25 to "Babe Ruth",
            156.15 to "Willie Mays",
            151.4 to "Ty Cobb",
            143.0 to "Henry Aaron"
        )
        assert(topPlayersByWar == expectedTop5PlayersByWar)
    }

    @Test
    fun steveBalboniAllStars() {
        val allCareers = testee.getBatterCareers(warDailyBatFile)
        val steveBalboniCareers = allCareers.filter {
            val seasonCount = it.seasonCount()
            val careerWar = it.careerWar()
            val warPerSeason = careerWar / seasonCount
            seasonCount >= 10 && warPerSeason < 0.5
        }.sortedBy { it.careerWar() }
        val longestCareer = steveBalboniCareers.maxByOrNull { it.seasonCount() }!!

        // assert
        val expectedCount = 500
        assert(steveBalboniCareers.size == expectedCount)
        { "expected $expectedCount, got ${steveBalboniCareers.size}" }
        val expectedPlayer = "Doug Flynn"
        assert(steveBalboniCareers.first().playerName == expectedPlayer)
        { "expected $expectedPlayer, got ${steveBalboniCareers.first().playerName}" }
        val expectedLongestCareerPlayer = "Luke Sewell"
        assert(longestCareer.playerName == expectedLongestCareerPlayer)
        { "expected $expectedLongestCareerPlayer, got ${longestCareer.playerName}" }

        // print
        println("Steve Balboni AllStars(${steveBalboniCareers.size}) ...")
        steveBalboniCareers.printCareers()
        println("longest career ... ${longestCareer.playerName}, seasons=${longestCareer.seasonCount()}, war=${longestCareer.careerWar()}")
    }

    @Test
    fun rowlandOfficeAllStars() {
        val allCareers = testee.getBatterCareers(warDailyBatFile).sortedBy { it.careerWar() }
        val rowlandOfficeCareers = allCareers.filter { it.seasonCount() >= 10 && it.careerWar() < 0.0 }
        val longestCareer = rowlandOfficeCareers.maxByOrNull { it.seasonCount() }!!

        // assert
        val expectedCount = 126
        assert(rowlandOfficeCareers.size == expectedCount) { "expected $expectedCount, got ${rowlandOfficeCareers.size}" }
        val expectedPlayer = "Doug Flynn"
        assert(rowlandOfficeCareers.first().playerName == expectedPlayer) { "expected $expectedPlayer, got ${rowlandOfficeCareers.first().playerName}" }
        val expectedLongestCareerPlayer = "Juan Castro"
        assert(longestCareer.playerName == expectedLongestCareerPlayer)
        { "expected $expectedLongestCareerPlayer, got ${longestCareer.playerName}" }

        // print
        println("Rowland Office AllStars(${rowlandOfficeCareers.size}) ...")
        rowlandOfficeCareers.printCareers()
        println("longest career ... ${longestCareer.playerName}, seasons=${longestCareer.seasonCount()}, war=${longestCareer.careerWar()}")
    }

    @Test
    fun bottomWarCareers() {
        val allCareers = testee.getBatterCareers(warDailyBatFile).sortedBy { it.careerWar() }
        val negativeWarCareers = allCareers.filter { it.careerWar() < 0.0 }

        // assert
        val expectedCount = 4533
        assert(negativeWarCareers.size == expectedCount) { "expected $expectedCount, got ${negativeWarCareers.size}" }
        val expectedPlayer = "Jim Levey"
        assert(negativeWarCareers.first().playerName == expectedPlayer) { "expected $expectedPlayer, got ${negativeWarCareers.first().playerName}" }

        // print
        println("Negative WAR careers(${negativeWarCareers.size}) ...")
        negativeWarCareers.printCareers()
    }

    @Test
    fun bottomWarsPerSeason() {
        val careers = testee.getBatterCareers(warDailyBatFile)
            .filter { it.seasonCount() >= 10 }.sortedBy {
            it.careerWar() / it.seasonCount()
        }
        // assert
        val expectedPlayer = "Doug Flynn"
        assert(careers.first().playerName == expectedPlayer) { "expected $expectedPlayer, got ${careers.first().playerName}" }

        // print
        println("Career war/season ...: ${careers.size} ...")
        careers.filterIndexed { index, _ -> index < 10 }.printCareers()
    }

    @Test
    fun bottomSeasonWars() {
        val seasons = testee.getBatterSeasons(warDailyBatFile).sortedBy { it.war() }.take(20)

        // assert
        val player = "Jerry Royster"
        assert(seasons.first().playerName() == player) { "expected $player, got ${seasons.first().playerName()}" }

        // print
        seasons.printSeasons()
    }

    @Test
    fun steveBalboniAllStarsBySeasonsPlayed() {
        val allCareers = testee.getBatterCareers(warDailyBatFile)
        val steveBalboniCareers = allCareers.filter {
            val seasonCount = it.seasonCount()
            val careerWar = it.careerWar()
            val warPerSeason = careerWar / seasonCount
            warPerSeason < 0.5
        }.sortedBy { (_, value) -> value }

        steveBalboniCareers.groupBy { it.seasonCount() }.map { it.key to it.value.size }.toMap().toSortedMap().forEach {
            println("${it.key} seasons: ${it.value}")
        }
    }

    companion object {
        private const val warDailyBatFile = "war_daily_bat.txt"
    }

    private fun List<Career>.printCareers() {
        this.forEachIndexed { index, it ->
            println("#${index + 1}: " +
                    "war=${it.careerWar()}, " +
                    "war/season=${(it.careerWar() / it.seasonCount()).roundToDecimalPlaces(2)}, " +
                    "name=${it.playerName}, " +
                    "seasons=${it.seasonCount()} (${it.seasonRange()})")
        }
    }

    private fun List<Season>.printSeasons() {
        this.forEachIndexed { index, it ->
            println("#${index + 1}: " +
                    "war=${it.war()}, " +
                    "season=${it.season()}, " +
                    "name=${it.playerName()}")
        }
    }

}