package com.michaelsgroi.baseballreference

data class Roster(val rosterId: RosterId, val players: Set<Career>) {
    companion object {
        val rosterFormatter = BrReportFormatter<Roster>(
            listOf(
                BrReportFormatter.Field("#", 5, true) { index, _ -> "#${(index + 1)}:" },
//                BrReportFormatter.Field("playerId", 10, false) { _, career -> career.playerName },
//                BrReportFormatter.Field("playerName", 20, true) { _, career -> career.war.roundToDecimalPlaces(2) },
//                BrReportFormatter.Field("seasons", 7, true) { _, career -> career.seasons().size.toString() },
//                BrReportFormatter.Field("", 11, false) { _, career -> "(${career.seasonRange()})" },
//                BrReportFormatter.Field("teams", 256, false) { _, career -> career.teams().joinToString(", ") },
            )
        )
    }
}

data class RosterId(val season: Int, val team: String)