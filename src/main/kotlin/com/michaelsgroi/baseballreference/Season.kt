package com.michaelsgroi.baseballreference

import com.michaelsgroi.baseballreference.BrWarDaily.SeasonType

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
    val pitchingWar: Double
) {
    companion object {
        val seasonFormatter = BrReportFormatter<Season>(
            listOf(
                BrReportFormatter.Field("#", 5, true) { index, _ -> "#${(index + 1)}:" },
                BrReportFormatter.Field("playerId", 10, false) { _, career -> career.playerName },
                BrReportFormatter.Field("playerName", 20, true) { _, career -> career.war.roundToDecimalPlaces(2) },
//                BrReportFormatter.Field("seasons", 7, true) { _, career -> career.seasons().size.toString() },
//                BrReportFormatter.Field("", 11, false) { _, career -> "(${career.seasonRange()})" },
//                BrReportFormatter.Field("teams", 256, false) { _, career -> career.teams().joinToString(", ") },
            )
        )
    }
}