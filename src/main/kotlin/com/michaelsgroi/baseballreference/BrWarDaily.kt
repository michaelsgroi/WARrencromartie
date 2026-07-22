package com.michaelsgroi.baseballreference

import com.michaelsgroi.baseballreference.Roster.RosterId
import java.io.File
import java.io.IOException
import java.math.RoundingMode.HALF_UP
import java.time.Duration
import java.time.Instant
import java.util.zip.ZipInputStream
import kotlin.math.roundToInt
import okhttp3.OkHttpClient
import okhttp3.Request

class BrWarDaily(
    expiration: Duration = FILE_EXPIRATION,
) {
    private val batting = BrWarDailyLines(WAR_DAILY_BAT_FILE, SeasonType.BATTING, expiration)
    private val pitching = BrWarDailyLines(WAR_DAILY_PITCH_FILE, SeasonType.PITCHING, expiration)

    val rosters: List<Roster> by lazy { this.getRostersInternal() }
    val seasons: List<Season> by lazy { this.getSeasonsInternal() }
    val careers: List<Career> by lazy { this.getCareersInternal() }

    private fun getRostersInternal(): List<Roster> {
        val careersForPlayerId = careers.associateBy { it.playerId }
        return seasons
            .flatMap { playerSeason ->
                playerSeason.teams.map { team ->
                    RosterId(playerSeason.season, team.lowercase()) to careersForPlayerId[playerSeason.playerId]!!
                }
            }.groupBy({ it.first }, { it.second })
            .map { (rosterId, careers) ->
                Roster(rosterId, careers.toSet())
            }
    }

    private fun getSeasonsInternal(): List<Season> = careers.flatMap { it.seasons() }

    private fun getCareersInternal(): List<Career> =
        getSeasonLines().groupBy { it.playerId() }.map { (playerId, seasonList) ->
            Career(
                playerId = playerId,
                playerName = seasonList.first().playerName(),
                seasonLines = seasonList,
            )
        }

    private fun getSeasonLines(): List<SeasonLine> = listOf(batting, pitching).flatMap { it.getSeasons() }

    enum class Fields(
        val fileField: String,
    ) {
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
        SEASON("year_id"),
    }

    enum class SeasonType(
        val brFilename: String,
    ) {
        BATTING("war_daily_bat.txt"),
        PITCHING("war_daily_pitch.txt"),
    }

    companion object {
        val MAJOR_LEAGUES = setOf("AL", "NL")
        const val WAR_DAILY_BAT_FILE = "war_daily_bat.txt"
        const val WAR_DAILY_PITCH_FILE = "war_daily_pitch.txt"
        val FILE_EXPIRATION: Duration = Duration.ofDays(7)

        fun downloadAll(expiration: Duration = FILE_EXPIRATION) {
            val files = listOf(WAR_DAILY_BAT_FILE, WAR_DAILY_PITCH_FILE)
            if (files.all { it.fileExists() && !it.fileExpired(expiration) }) return

            val client = OkHttpClient()
            val indexUrl = "https://www.baseball-reference.com/data/"
            val html = client.newCall(Request.Builder().url(indexUrl).get().build())
                .execute().use { if (!it.isSuccessful) throw IOException("Unexpected code $it"); it.body.string() }
            val zipUrl = indexUrl + (Regex("""war_archive-\d{4}-\d{2}-\d{2}\.zip""").findAll(html)
                .map { it.value }.maxOrNull() ?: throw IOException("No war_archive-*.zip found at $indexUrl"))
            println("downloading $zipUrl ...")
            val zipBytes = client.newCall(Request.Builder().url(zipUrl).get().build())
                .execute().use { if (!it.isSuccessful) throw IOException("Unexpected code $it"); it.body.bytes() }
            val extracted = mutableMapOf<String, String>()
            ZipInputStream(zipBytes.inputStream()).use { zip ->
                generateSequence { zip.nextEntry }.forEach { entry ->
                    if (SeasonType.entries.any { it.brFilename == entry.name }) {
                        extracted[entry.name] = zip.bufferedReader().readText()
                    }
                }
            }
            for (seasonType in SeasonType.entries) {
                val text = extracted[seasonType.brFilename]
                    ?: throw IOException("${seasonType.brFilename} not found in $zipUrl")
                seasonType.brFilename.writeFile(text)
                println("wrote ${seasonType.brFilename}")
            }
        }

        fun loadFromCache(
            filename: String,
            expiration: Duration,
            loader: () -> String,
        ): List<String> {
            if (!filename.fileExists()) {
                println("filename=$filename does not exist, retrieving from baseball-reference.com ...")
                filename.writeFile(loader())
            } else {
                if (filename.fileExpired(expiration)) {
                    println("filename=$filename exists but is expired, retrieving from baseball-reference.com ...")
                    filename.writeFile(loader())
                }
            }
            return filename.readFile()
        }
    }
}

fun String.readFile(): List<String> = File(this).useLines { it.toList() }

fun String.fileExists(): Boolean = File(this).exists()

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
