package com.michaelsgroi.baseballreference

import com.michaelsgroi.baseballreference.BrWarDaily.SeasonType.BATTING
import com.michaelsgroi.baseballreference.BrWarDaily.SeasonType.PITCHING
import kotlin.math.roundToLong

data class Career(
    val playerId: String,
    val playerName: String,
    val war: Double,
    val seasonLines: List<SeasonLine>,
    val warPercentile: Double = 0.0,
) {
    fun war() = seasons().sumOf { it.war }.roundToDecimalPlaces(2)

    fun seasonRange() = "${seasonLines.minOf { it.season() }}-${seasonLines.maxOf { it.season() }}"

    fun seasonCount() = seasons().size

    fun teams() = seasons().flatMap { it.teams }.toSet()

    fun salary() = seasons().sumOf { it.salary }

    fun battingWar() = seasons().sumOf { it.battingWar }.roundToDecimalPlaces(2)

    fun pitchingWar() = seasons().sumOf { it.pitchingWar }.roundToDecimalPlaces(2)

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
    }.values

    override fun toString(): String {
        return "{ " +
            "\"war\":${this.war()}, " +
            "\"pitchingWar\":${this.pitchingWar()}, " +
            "\"battingWar\":${this.battingWar()}, " +
            "\"war/season\":${(this.war() / this.seasonCount()).roundToDecimalPlaces(2)}, " +
            "\"id\":\"${this.playerId}\", " +
            "\"name\":\"${this.playerName}\", " +
            "\"seasons\":${this.seasonCount()}, " +
            "\"seasonsRange\":\"${this.seasonRange()}\"" +
            "}"
    }
}