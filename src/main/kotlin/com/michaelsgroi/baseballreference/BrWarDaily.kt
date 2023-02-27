package com.michaelsgroi.baseballreference

import java.io.File
import kotlin.math.roundToInt

class BrWarDaily {

    private val batting: BrWarDailyBatLines = BrWarDailyBatLines(warDailyBatFile)
    private val pitching: BrWarDailyPitchLines = BrWarDailyPitchLines(warDailyPitchFile)

    fun getRosters(): List<Roster> {
        val careers = getCareers().associateBy { it.playerId }
        val seasons = getSeasons()
        val rosters = mutableMapOf<RosterId, Roster>()
        seasons.forEach { playerSeason ->
            playerSeason.teams.forEach { team ->
                val rosterId = RosterId(playerSeason.season, team.lowercase())
                rosters.getOrPut(rosterId) {
                    Roster(rosterId, mutableSetOf())
                }.players.add(careers[playerSeason.playerId]!!)
            }
        }

        return rosters.values.toList()
    }

    fun getSeasons(): List<Season> {
        return getCareers().flatMap { it.seasons() }
    }

    fun getCareers(): List<Career> {
        val seasons = getSeasonLines()

        val careers = seasons.groupBy { it.playerId() }
            .map { entry ->
                val playerId = entry.key
                val seasonList = entry.value
                Career(
                    playerId = playerId,
                    playerName = entry.value.first().playerName(),
                    war = seasonList.sumOf {
                        it.war()
                    },
                    seasonLines = seasonList
                )
            }

        val careerWars = careers.map { it.war }.sortedBy { it }

        careers.forEach { career ->
            val careerPercentileWar = careerWars.percentile(career.war)
            career.warPercentile = careerPercentileWar
        }

        return careers
    }

    private fun List<Double>.percentile(value: Double): Double {
        return (indexOf(value).toDouble() / this.size.toDouble()) * 100
    }

    private fun getSeasonLines(): List<SeasonLine> {
        val batterSeasons = batting.getBatterSeasons()
        val pitchingSeasons = pitching.getPitcherSeasons()
        return batterSeasons + pitchingSeasons
    }

    enum class Fields(var fileField: String) {
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

    enum class SeasonType {
        BATTING, PITCHING
    }

    companion object {
        val majorLeagues = setOf("AL", "NL")
        const val warDailyBatFile = "war_daily_bat.txt"
        const val warDailyPitchFile = "war_daily_pitch.txt"

        fun loadFromCache(filename: String, loader: () -> String): List<String> {
            if (!filename.fileExists()) {
                println("filename=$filename does not exist, retrieving from baseball-reference.com ...")
                filename.writeFile(loader.invoke())
            } else {
                println("filename=$filename exists, using local copy ...")
            }
            return filename.readFile()
        }
    }
}

fun String.readFile(): List<String> {
    return File(this).useLines { it.toList() }
}

fun String.fileExists(): Boolean {
    return File(this).exists()
}

fun String.dirExists(): Boolean {
    val file = File(this)
    val exists = file.exists()
    require(!exists || file.isDirectory) { "File $this is not a directory" }
    return exists
}

fun String.createDirectoryIfNotExists() {
    if (!this.dirExists()) {
        File(this).mkdirs()
    }
}

fun String.writeFile(contents: String) {
    File(this).writeBytes(contents.encodeToByteArray())
}

fun Double.roundToDecimalPlaces(places: Int) = (this * 10 * places).roundToInt() / (10.0 * places)