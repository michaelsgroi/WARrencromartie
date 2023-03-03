package com.michaelsgroi.warrencromartie

import com.michaelsgroi.baseballreference.BrWarDailyLines
import com.michaelsgroi.baseballreference.Career
import com.michaelsgroi.baseballreference.Roster
import com.michaelsgroi.baseballreference.Roster.RosterId
import com.michaelsgroi.baseballreference.Season
import com.michaelsgroi.baseballreference.SeasonLine
import java.math.RoundingMode.HALF_UP
import kotlin.math.roundToInt

class War {

    private val batting = BrWarDailyLines(warDailyBatFile, SeasonType.BATTING)
    private val pitching = BrWarDailyLines(warDailyPitchFile, SeasonType.PITCHING)

    val rosters: List<Roster> by lazy { this.getRostersInternal() }
    val seasons: List<Season> by lazy { this.getSeasonsInternal() }
    val careers: List<Career> by lazy { this.getCareersInternal() }

    private fun getRostersInternal(): List<Roster> {
        val careersForPlayerId = careers.associateBy { it.playerId }
        return seasons.flatMap { playerSeason ->
            playerSeason.teams.map { team ->
                RosterId(playerSeason.season, team.lowercase()) to careersForPlayerId[playerSeason.playerId]!!
            }
        }.groupBy({ it.first }, { it.second })
            .map { (rosterId, careers) ->
                Roster(rosterId, careers.toSet())
            }
    }

    private fun getSeasonsInternal(): List<Season> {
        return careers.flatMap { it.seasons() }
    }

    private fun getCareersInternal(): List<Career> {
        val playerIdToSeasonLines = getSeasonLines().groupBy { it.playerId() }

        val careerWars = playerIdToSeasonLines.values.map { seasonList ->
            seasonList.sumOf { it.war() }
        }.sorted()

        return playerIdToSeasonLines.map { (playerId, seasonList) ->
            val war = seasonList.sumOf { it.war() }
            Career(
                playerId = playerId,
                playerName = seasonList.first().playerName(),
                war = war,
                seasonLines = seasonList,
                warPercentile = careerWars.percentile(war)
            )
        }
    }

    private fun List<Double>.percentile(value: Double): Double {
        return (indexOf(value).toDouble() / this.size.toDouble()) * 100
    }

    private fun getSeasonLines(): List<SeasonLine> {
        return listOf(batting, pitching).flatMap { it.getSeasons() }
    }

    enum class Fields(val fileField: String) {
        /*
         * name_common,age,mlb_ID,player_ID,year_ID,team_ID,stint_ID,lg_ID,G,runs_above_avg,WAR,salary,
         * teamRpG,oppRpG,path_exponent,waa_win_per,waa_win_per_rep
         */
        WAR("war"),
        PLAYER_NAME("name_common"),
        LEAGUE("lg_id"),
        TEAM_ID("team_id"),
        PLAYER_ID("player_id"),
        SALARY("salary"),
        SEASON_TYPE("season_type"),
        SEASON("year_id")
    }

    enum class SeasonType(val brFilename: String) {
        BATTING("war_daily_bat.txt"),
        PITCHING("war_daily_pitch.txt")
    }

    companion object {
        val majorLeagues = setOf("AL", "NL")
        const val warDailyBatFile = "war_daily_bat.txt"
        const val warDailyPitchFile = "war_daily_pitch.txt"
    }
}

fun Double.roundToDecimalPlaces(places: Int) =
    (if (places == 0) roundToInt() else toBigDecimal().setScale(2, HALF_UP).toDouble()).toString()