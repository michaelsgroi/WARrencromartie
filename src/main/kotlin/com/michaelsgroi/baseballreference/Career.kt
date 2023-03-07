package com.michaelsgroi.baseballreference

import com.michaelsgroi.baseballreference.BrReportFormatter.Companion.asIs
import com.michaelsgroi.baseballreference.BrReportFormatter.Companion.leftAlign
import com.michaelsgroi.baseballreference.BrReportFormatter.Companion.rightAlign
import com.michaelsgroi.baseballreference.BrReportFormatter.Field
import com.michaelsgroi.baseballreference.BrWarDaily.SeasonType.BATTING
import com.michaelsgroi.baseballreference.BrWarDaily.SeasonType.PITCHING
import com.michaelsgroi.baseballreference.Verbosity.CONCISE
import com.michaelsgroi.baseballreference.Verbosity.VERBOSE
import okhttp3.internal.toImmutableList
import java.util.LinkedList
import kotlin.math.roundToLong

data class Career(
    val playerId: String,
    val playerName: String,
    private val seasonLines: List<SeasonLine>,
    var seasonsPredicate: (Season) -> Boolean = { true },
) {
    fun war() = seasons().sumOf { it.war }

    fun seasonRange(): String {
        return "${seasons().minOf { it.season }}-${seasons().maxOf { it.season }}"
    }

    fun seasonCount() = seasons().size

    fun teams() = seasons().flatMap { it.teams }.toSet()

    fun salary() = seasons().sumOf { it.salary }

    fun battingWar() = seasons().sumOf { it.battingWar }

    fun pitchingWar() = seasons().sumOf { it.pitchingWar }

    fun consecutiveSeasonsOver(minWar: Double): List<Season> {
        val seasonOrdinals = seasons().mapIndexed { index, season -> index + 1 to season }.toMap()
        val seasonsOrdered = seasonOrdinals.filter { it.value.war > minWar }.keys.sorted()
        var groupNum = 0
        val seasonGroups = seasonsOrdered.mapIndexed { index, currentValue ->
            if (!(index != 0 && currentValue - seasonsOrdered[index - 1] == 1)) groupNum++
            currentValue to groupNum
        }.groupBy { it.second }.map { groups -> groups.value.map { seasonOrdinals[it.first]!! } }
        val longestGroupLength = seasonGroups.maxOfOrNull { it.size } ?: return emptyList()
        return seasonGroups.filter { it.size == longestGroupLength }.maxBy { it.sumOf { season -> season.war } }
    }

    fun seasons() = seasonLines.groupBy { it.season() }.mapValues { (season, seasonLines) ->
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
        )
    }.values.filter { seasonsPredicate(it) }

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
        private val careerFormatterDefaultFields = listOf<Field<Career>>(
            Field("#", rightAlign(5)) { index, _ -> "#${(index + 1)}:" },
            Field("name", leftAlign(20)) { _, career -> career.playerName },
            Field("seasons", rightAlign(7)) { _, career -> career.seasons().size.toString() },
            Field("", leftAlign(11)) { _, career ->
                if (career.seasons().isEmpty()) "" else "(${career.seasonRange()})" },
            Field("teams", leftAlign(256)) { _, career -> career.teams().joinToString(",") },
        )

        fun getCareerFormatter(
            verbosity: Verbosity = VERBOSE,
            includeWar: Boolean = true,
            includeAverageWar: Boolean = false,
            includeSalary: Boolean = false,
            includePeakWar: Boolean = false,
        ): BrReportFormatter<Career> = when (verbosity) {
            CONCISE -> {
                BrReportFormatter(
                    fields = listOf(
                        Field("#", asIs(5)) { index, _ -> "#${(index + 1)}:" },
                        Field("name", asIs(20)) { _, career -> career.playerName },
                        Field("war", asIs(10)) { _, career -> career.war().roundToDecimalPlaces(0) }
                    )
                )
            }

            VERBOSE -> {
                val fieldsLinkedList = LinkedList(careerFormatterDefaultFields)
                if (includeWar) {
                    fieldsLinkedList.add(
                        2,
                        Field("war", rightAlign(10)) { _, career -> career.war().roundToDecimalPlaces(2) }
                    )
                }
                if (includeAverageWar) {
                    fieldsLinkedList.add(
                        2,
                        Field("avg war", rightAlign(10)) { _, career -> (career.war() / career.seasons().size).roundToDecimalPlaces(1) }
                    )
                }
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
                BrReportFormatter(fieldsLinkedList.toImmutableList())
            }
        }
    }
}