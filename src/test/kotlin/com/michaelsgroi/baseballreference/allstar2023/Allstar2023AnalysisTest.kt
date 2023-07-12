package com.michaelsgroi.baseballreference.allstar2023

import com.michaelsgroi.allstar2023.AllStars2023
import com.michaelsgroi.allstar2023.AllStars2023.AllStar
import com.michaelsgroi.allstar2023.FanGraphsWar
import com.michaelsgroi.allstar2023.FanGraphsWar.PlayerWar
import com.michaelsgroi.baseballreference.BrReportFormatter
import com.michaelsgroi.baseballreference.BrReportFormatter.Field
import com.michaelsgroi.baseballreference.BrReports.Report
import com.michaelsgroi.baseballreference.allstar2023.AllStar2023AnalysisTest.Type.Batter
import com.michaelsgroi.baseballreference.allstar2023.AllStar2023AnalysisTest.Type.Pitcher
import com.michaelsgroi.baseballreference.writeFile
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import kotlin.math.max
import kotlin.math.min

class AllStar2023AnalysisTest {

    @Test
    fun testLoadAllstars() =
        assertEquals(
            AllStar("OF", "Aaron Judge", "NYY"),
            AllStars2023("allstars2023.csv").getAllStars().sortedBy { it.name.lowercase() }.first()
        )

    @Test
    fun testLoadFanGraphsWar() {
        assertEquals(
            PlayerWar("Shohei Ohtani", "LAA",4.3, 1.7),
            FanGraphsWar("fangraphs_war_11-JUL-2023.csv").getPlayerWars().sortedByDescending { it.twar() }.first()
        )
    }

    @Test
    fun test() {
        val allStars = AllStars2023("allstars2023.csv").getAllStars()
        val playerWars = FanGraphsWar("fangraphs_war_11-JUL-2023.csv").getPlayerWars()
        val batterWarsSorted = playerWars.sortedByDescending { it.bwar }
        val pitcherWarsSorted = playerWars.sortedByDescending { it.pwar }

        val batterCount = playerWars.count {
            val batterRank = batterWarsSorted.indexOf(it) + 1
            val pitcherRank = pitcherWarsSorted.indexOf(it) + 1
            batterRank < pitcherRank
        }
        val pitcherCount = playerWars.count {
            val batterRank = batterWarsSorted.indexOf(it) + 1
            val pitcherRank = pitcherWarsSorted.indexOf(it) + 1
            batterRank > pitcherRank
        }

        val warRankings = playerWars.map {
            val batterRank = batterWarsSorted.indexOf(it) + 1
            val pitcherRank = pitcherWarsSorted.indexOf(it) + 1
            val rank = min(batterRank, pitcherRank)
            val type = if (batterRank < pitcherRank) Batter else Pitcher

            AllStarWarRanking(
                type = type,
                allStar = allStars.firstOrNull() { allStar -> allStar.name == it.name },
                playerWar = it,
                rank = rank,
                rankOf = if (type == Batter) batterCount else pitcherCount
            )
        }

        val allStarWarRankings = warRankings.filter { it.allStar != null }
        val allStarWarRankingsSorted = allStarWarRankings.sortedBy { it.rank }

        val maxLengthName = allStarWarRankingsSorted.map { it.playerWar.name.length }.maxOrNull() ?: 0
        val maxLengthTeam = allStarWarRankingsSorted.map { it.playerWar.team.length }.maxOrNull() ?: 0
        println("maxLengthName=$maxLengthName")

        val allstarsbywarrankingReport = Report(
            name = "allstarsbywarranking",
            filename = "allstarsbywarranking",
            run = { allStarWarRankingsSorted },
            formatter = BrReportFormatter(
                fields = listOf(
                    Field("#", BrReportFormatter.rightAlign(5)) { index, _ -> "#${(index + 1)}:" },
                    Field("name", BrReportFormatter.leftAlign(maxLengthName)) { _, allStar -> allStar.playerWar.name },
                    Field("team", BrReportFormatter.leftAlign(max(maxLengthTeam, 4))) { _, allStar -> allStar.playerWar.team },
                    Field(
                        "rank",
                        BrReportFormatter.leftAlign(10)
                    ) { _, allStar -> "${allStar!!.rank} of ${allStar!!.rankOf}" },
                    Field("bwar", BrReportFormatter.leftAlign(4)) { _, allStar -> allStar.playerWar.bwar.toString() },
                    Field("bwar", BrReportFormatter.leftAlign(4)) { _, allStar -> allStar.playerWar.pwar.toString() },
                    Field("twar", BrReportFormatter.leftAlign(4)) { _, allStar -> allStar.playerWar.twar().toString() },
                )
            )
        )
        writeReport(allstarsbywarrankingReport)

        val allStarsByTeam = allStarWarRankings.groupBy { it.playerWar.team }.mapValues { it.value.size }.toList().sortedByDescending { it.second }

        val allStarsByTeamReport = Report(
            name = "allstarsbyteam",
            filename = "allstarsbyteam",
            run = { allStarsByTeam },
            formatter = BrReportFormatter(
                fields = listOf(
                    Field("#", BrReportFormatter.rightAlign(5)) { index, _ -> "#${(index + 1)}:" },
                    Field("team", BrReportFormatter.leftAlign(max(maxLengthTeam, 4))) { _, entry -> entry.first },
                    Field("count", BrReportFormatter.leftAlign(maxLengthTeam)) { _, entry -> entry.second.toString() },
                )
            )
        )
        writeReport(allStarsByTeamReport)
    }

    private fun <T> writeReport(report: Report<T>) {
        val contents = report.formatter.format(report)
        "${report.filename}".writeFile(contents)
    }

    data class AllStarWarRanking(
        val type: Type,
        val allStar: AllStar?,
        val playerWar: PlayerWar,
        val rank: Int,
        val rankOf: Int
    )

    enum class Type {
        Batter, Pitcher
    }
}