package com.michaelsgroi.baseballreference

import com.michaelsgroi.baseballreference.BrReportFormatter.Companion.leftAlign
import com.michaelsgroi.baseballreference.BrReportFormatter.Companion.asIs
import com.michaelsgroi.baseballreference.BrReportFormatter.Companion.rightAlign
import com.michaelsgroi.baseballreference.BrReportFormatter.Field
import com.michaelsgroi.baseballreference.Verbosity.CONCISE
import com.michaelsgroi.baseballreference.Verbosity.VERBOSE

data class Roster(val rosterId: RosterId, val players: Set<Career>) {
    companion object {
        private val rosterDefaultFields = listOf<Field<Roster>>(
            Field("#", rightAlign(5)) { index, _ -> "#${(index + 1)}:" },
            Field("year", leftAlign(4)) { _, roster -> roster.rosterId.season.toString() },
            Field("team", rightAlign(4)) { _, roster -> roster.rosterId.team },
            Field("war", rightAlign(4)) { _, roster -> roster.players.sumOf { it.war }.roundToDecimalPlaces(0) },
        )
        private val rosterConciseFields = listOf<Field<Roster>>(
            Field("#", asIs(5)) { index, _ -> "${(index + 1)}:" },
            Field("year", asIs(4)) { _, roster -> roster.rosterId.season.toString() },
            Field("team", asIs(4)) { _, roster -> roster.rosterId.team },
            Field("war", asIs(4)) { _, roster -> roster.players.sumOf { it.war }.roundToDecimalPlaces(0) },
        )

        fun getRosterFormatter(verbosity: Verbosity = VERBOSE): BrReportFormatter<Roster> =
            when (verbosity) {
                CONCISE -> BrReportFormatter(rosterConciseFields)
                VERBOSE -> BrReportFormatter(rosterDefaultFields)
            }
    }

    data class RosterId(val season: Int, val team: String)
}