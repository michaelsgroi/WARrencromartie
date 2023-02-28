package com.michaelsgroi.baseballreference

import java.io.File
import java.math.RoundingMode.HALF_UP
import java.time.Duration
import java.time.Instant
import kotlin.math.roundToInt

class BrWarDaily {

    private val batting = BrWarDailyLines(warDailyBatFile, SeasonType.BATTING)
    private val pitching = BrWarDailyLines(warDailyPitchFile, SeasonType.PITCHING)

    val rosters: List<Roster> by lazy { this.getRostersInternal() }
    val seasons: List<Season> by lazy { this.getSeasonsInternal() }
    val careers: List<Career> by lazy { this.getCareersInternal() }

    private fun getRostersInternal(): List<Roster> {
        val careers = careers.associateBy { it.playerId }
        return seasons.flatMap { playerSeason ->
            playerSeason.teams.map { team ->
                RosterId(playerSeason.season, team.lowercase()) to careers[playerSeason.playerId]!!
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

        val careerWars = playerIdToSeasonLines.map { (_, seasonList) ->
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
        val fileExpiration: Duration = Duration.ofDays(7)

        fun loadFromCache(filename: String, expiration: Duration, loader: () -> String): List<String> {
            if (!filename.fileExists()) {
                println("filename=$filename does not exist, retrieving from baseball-reference.com ...")
                filename.writeFile(loader.invoke())
            } else {
                if (filename.fileExpired(expiration)) {
                    println("filename=$filename exists but is expired, retrieving from baseball-reference.com ...")
                    filename.writeFile(loader.invoke())
                }
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

fun String.fileExpired(duration: Duration): Boolean {
    require(fileExists()) { "File $this does not exist" }
    val file = File(this)
    val ageMs = Instant.now().toEpochMilli() - file.lastModified()
    return ageMs > duration.toMillis()
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

fun Double.roundToDecimalPlaces(places: Int) =
    (if (places == 0) roundToInt() else toBigDecimal().setScale(2, HALF_UP).toDouble()).toString()