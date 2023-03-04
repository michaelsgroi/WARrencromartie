package com.michaelsgroi.baseballreference

import com.michaelsgroi.baseballdatabank.BaseballDatabank.Award
import com.michaelsgroi.baseballdatabank.BaseballDatabank.Award.CYA
import com.michaelsgroi.baseballdatabank.BaseballDatabank.Award.MVP
import com.michaelsgroi.baseballdatabank.BaseballDatabank.RankedAwardShare
import com.michaelsgroi.baseballdatabank.awardVotes
import com.michaelsgroi.baseballdatabank.awardWinCount
import com.michaelsgroi.baseballreference.BrReportFormatter.Companion.asIs
import com.michaelsgroi.baseballreference.BrReportFormatter.Companion.leftAlign
import com.michaelsgroi.baseballreference.BrReportFormatter.Companion.rightAlign
import com.michaelsgroi.baseballreference.BrReportFormatter.Field
import com.michaelsgroi.baseballreference.Verbosity.CONCISE
import com.michaelsgroi.baseballreference.Verbosity.VERBOSE
import com.michaelsgroi.warrencromartie.War.SeasonType.BATTING
import com.michaelsgroi.warrencromartie.War.SeasonType.PITCHING
import com.michaelsgroi.warrencromartie.roundToDecimalPlaces
import okhttp3.internal.toImmutableList
import java.util.LinkedList
import kotlin.math.roundToLong

data class Career(
    val playerId: String,
    val playerName: String,
    val war: Double,
    val seasonLines: List<SeasonLine>,
    val awardVoteLines: List<RankedAwardShare>,
    val warPercentile: Double = 0.0,
) {
    private val seasonLinesBySeason: Map<Int, List<SeasonLine>> by lazy { seasonLines.groupBy { it.season() } }
    private val awardVotesBySeason: Map<Int, List<RankedAwardShare>> by lazy { awardVoteLines.groupBy { it.season } }

    init {
        validate()
    }

    private fun validate() {
        // player id's
        val seasonLinePlayerIds = seasonLines.map { it.playerId() }.toSet()
        val awardLinePlayerIds = awardVoteLines.map { it.playerId }.toSet()
        require(
            seasonLinePlayerIds.size == 1
                    && (awardLinePlayerIds.isEmpty() || awardLinePlayerIds.size == 1)
                    && seasonLinePlayerIds.first() == playerId
                    && (awardLinePlayerIds.isEmpty() || awardLinePlayerIds.first() == playerId)
        ) {
            "seasonLinePlayerIds($seasonLinePlayerIds) and " +
                    "awardLinePlayerIds($awardLinePlayerIds) must match parent playerId=$playerId"
        }

        // seasons
        val seasonLineSeasons = seasonLinesBySeason.keys
        val awardVoteSeasons = awardVotesBySeason.keys
        require(seasonLineSeasons.containsAll(awardVoteSeasons))
        { "award vote season($awardVoteSeasons) contains a season not found in season line seasons($seasonLineSeasons)" }

        // leagues
        val seasonLineLeagues = seasonLines.map { it.league() }.toSet()
        val awardLineLeagues = awardVoteLines.map { it.league }.toSet().minus("ML")
        require(seasonLineLeagues intersect awardLineLeagues == awardLineLeagues)
        { "award vote league($seasonLineLeagues) contains a league not found in season line leagues($seasonLineLeagues)" }

        // awards
        awardVotesBySeason.values.forEach { awardLines ->
            val awardNames = awardLines.map { awardLine -> awardLine.awardName }
            require(awardNames.size == awardNames.toSet().size) {
                "duplicate awards found in: $awardNames"
            }
        }
    }

    fun war() = seasons().sumOf { it.war }

    fun seasonRange() = "${seasonLines.minOf { it.season() }}-${seasonLines.maxOf { it.season() }}"

    fun seasonCount() = seasons().size

    fun teams() = seasons().flatMap { it.teams }.toSet()

    fun salary() = seasons().sumOf { it.salary }

    fun battingWar() = seasons().sumOf { it.battingWar }

    fun pitchingWar() = seasons().sumOf { it.pitchingWar }

    fun seasons(): List<Season> {
        return seasonLinesBySeason.mapValues { (season, seasonLines) ->
            Season(
                playerId = playerId,
                playerName = playerName,
                season = season,
                teams = seasonLines.map { seasonLine -> seasonLine.team() }.toSet(),
                leagues = seasonLines.map { seasonLine -> seasonLine.league() }.toSet(),
                seasons = seasonLines.map { seasonLine -> seasonLine.seasonsType() }.toSet(),
                war = seasonLines.sumOf { seasonLine -> seasonLine.war() },
                salary = seasonLines.map { sl -> sl.salary() }.average().roundToLong(),
                battingWar = seasonLines.filter { seasonLine -> seasonLine.seasonsType() == BATTING }
                    .sumOf { seasonLine -> seasonLine.war() },
                pitchingWar = seasonLines.filter { seasonLine -> seasonLine.seasonsType() == PITCHING }
                    .sumOf { seasonLine -> seasonLine.war() },
                awardVoting = awardVotesBySeason[season]?.toSet() ?: emptySet()
            )
        }.values.toList()
    }

    fun awardCount() =
        seasons().sumOf { it.awardVoting.awardWinCount(relevantAwards) }

    fun awardCount(award: Award) =
        seasons().flatMap { it.awardVoting }
            .filter { relevantAwards.contains(it.award()) }.groupBy { it.award() }
            .map { it.key to it.value.toSet().awardWinCount(relevantAwards) }.toMap()[award] ?: 0

    fun awardVotes() = seasons().sumOf { it.awardVoting.awardVotes(setOf(MVP, CYA)) }

    fun awardVotes(award: Award) =
        seasons().flatMap { it.awardVoting }
            .filter { relevantAwards.contains(it.award()) }.groupBy { it.award() }
            .map { it.key to it.value.toSet().awardVotes(relevantAwards) }.toMap()[award] ?: 0.0

    override fun toString(): String {
        return "{ " +
                "\"war\":${this.war().roundToDecimalPlaces(2)}, " +
                "\"pitchingWar\":${this.pitchingWar().roundToDecimalPlaces(2)}, " +
                "\"battingWar\":${this.battingWar().roundToDecimalPlaces(2)}, " +
                "\"war/season\":${(this.war() / this.seasonCount()).roundToDecimalPlaces(2)}, " +
                "\"id\":\"${this.playerId}\", " +
                "\"name\":\"${this.playerName}\", " +
                "\"seasons\":${this.seasonCount()}, " +
                "\"seasonsRange\":\"${this.seasonRange()}\"" +
                "}"
    }

    companion object {
        val relevantAwards = setOf(MVP, CYA)

        private val careerFormatterDefaultFields = listOf<Field<Career>>(
            Field("#", rightAlign(5)) { index, _ -> "#${(index + 1)}:" },
            Field("name", leftAlign(20)) { _, career -> career.playerName },
            Field("war", rightAlign(10)) { _, career -> career.war.roundToDecimalPlaces(2) },
            Field("seasons", rightAlign(7)) { _, career -> career.seasons().size.toString() },
            Field("", leftAlign(11)) { _, career -> "(${career.seasonRange()})" },
            Field("teams", leftAlign(256)) { _, career -> career.teams().joinToString(",") },
        )

        fun getCareerFormatter(
            verbosity: Verbosity = VERBOSE,
            includeSalary: Boolean = false,
            includePeakWar: Boolean = false,
            includeAwardCounts: Boolean = false,
            includeAwardPoints: Boolean = false
        ): BrReportFormatter<Career> = when (verbosity) {
            CONCISE -> {
                BrReportFormatter(
                    fields = listOf(
                        Field("#", asIs(5)) { index, _ -> "#${(index + 1)}:" },
                        Field("name", asIs(20)) { _, career -> career.playerName },
                        Field("war", asIs(10)) { _, career -> career.war.roundToDecimalPlaces(0) }
                    )
                )
            }
            VERBOSE -> {
                val fieldsLinkedList = LinkedList(careerFormatterDefaultFields)
                if (includeSalary) {
                    fieldsLinkedList.add(
                        3,
                        Field("salary", rightAlign(10)) { _, career -> career.salary().toString() })
                }
                if (includePeakWar) {
                    fieldsLinkedList.add(
                        3,
                        Field("peakwar", rightAlign(12)) { _, career ->
                            val peakSeason = career.seasons().maxBy { it.war }
                            "${peakSeason.war.roundToDecimalPlaces(2)} (${peakSeason.season})"
                        })
                }
                if (includeAwardCounts) {
                    fieldsLinkedList.add(
                        5,
                        Field("total", rightAlign(5)) { _, career ->
                            career.awardCount().toString()
                        })
                    fieldsLinkedList.add(
                        5,
                        Field("mvp", rightAlign(3)) { _, career ->
                            career.awardCount(MVP).toString()
                        })
                    fieldsLinkedList.add(
                        5,
                        Field("cya", rightAlign(3)) { _, career ->
                            career.awardCount(CYA).toString()
                        })
                }
                if (includeAwardPoints) {
                    fieldsLinkedList.add(
                        5,
                        Field("total", rightAlign(5)) { _, career ->
                            career.awardVotes().roundToDecimalPlaces(0)
                        })
                    fieldsLinkedList.add(
                        5,
                        Field("mvpv", rightAlign(4)) { _, career ->
                            career.awardVotes(MVP).roundToDecimalPlaces(0)
                        })
                    fieldsLinkedList.add(
                        5,
                        Field("cyav", rightAlign(4)) { _, career ->
                            career.awardVotes(CYA).roundToDecimalPlaces(0)
                        })
                }
                BrReportFormatter(fieldsLinkedList.toImmutableList())
            }
        }
    }
}