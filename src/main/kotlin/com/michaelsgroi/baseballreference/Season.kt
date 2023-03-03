package com.michaelsgroi.baseballreference

import com.michaelsgroi.baseballdatabank.BaseballDatabank.RankedAwardShare
import com.michaelsgroi.baseballreference.BrReportFormatter.Companion.leftAlign
import com.michaelsgroi.baseballreference.BrReportFormatter.Companion.rightAlign
import com.michaelsgroi.baseballreference.BrReportFormatter.Field
import com.michaelsgroi.warrencromartie.War.SeasonType
import com.michaelsgroi.warrencromartie.roundToDecimalPlaces
import okhttp3.internal.toImmutableList
import java.util.LinkedList

data class Season(
    val playerId: String,
    val playerName: String,
    val season: Int,
    val teams: Set<String>,
    val leagues: Set<String>,
    val seasons: Set<SeasonType>,
    val war: Double,
    val salary: Long,
    val battingWar: Double,
    val pitchingWar: Double,
    val awardVoting: Set<RankedAwardShare>
) {
    companion object {
        private val seasonFormatterDefaultFields =
            listOf<Field<Season>>(
                Field("#", rightAlign(5)) { index, _ -> "#${(index + 1)}:" },
                Field("name", leftAlign(20)) { _, season -> season.playerName },
                Field("war", rightAlign(20)) { _, season -> season.war.roundToDecimalPlaces(2) },
                Field("year", rightAlign(4)) { _, season -> season.season.toString() },
                Field("teams", leftAlign(256)) { _, season -> season.teams.joinToString(",") },
            )
        fun getSeasonFormatter(includeSalary: Boolean = false): BrReportFormatter<Season> {
            val fieldsLinkedList = LinkedList(seasonFormatterDefaultFields)
            if (includeSalary) {
                fieldsLinkedList.add(
                    3,
                    Field("salary", rightAlign(10)) { _, career -> career.salary.toString() })
            }
            return  BrReportFormatter(fieldsLinkedList.toImmutableList())
        }
    }
}