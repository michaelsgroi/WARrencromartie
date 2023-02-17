package com.michaelsgroi.baseballreference

import com.michaelsgroi.baseballreference.BrWarDailyBatLines.Fields.IS_PITCHER
import com.michaelsgroi.baseballreference.BrWarDailyBatLines.Fields.LEAGUE
import com.michaelsgroi.baseballreference.BrWarDailyBatLines.Fields.PLAYER_ID
import com.michaelsgroi.baseballreference.BrWarDailyBatLines.Fields.PLAYER_NAME
import com.michaelsgroi.baseballreference.BrWarDailyBatLines.Fields.SEASON
import com.michaelsgroi.baseballreference.BrWarDailyBatLines.Fields.WAR
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File
import java.io.IOException
import kotlin.math.roundToInt

class BrWarDailyBatLines {

    private val majorLeagues = setOf("AL", "NL")

    fun getBatterSeasons(filename: String): List<Season> {
        return getBatterSeasons(getWarDailyBatFile(filename))
    }

    fun getBatterCareers(filename: String): List<Career> {
        return getBatterCareers(getWarDailyBatFile(filename))
    }

    private fun getBatterSeasons(warDailyBatLines: List<String>): List<Season> {
        // get seasons
        val seasons = deserializeToSeasons(warDailyBatLines)

        // filter for seasons for position players with war values
        val filteredSeasons = seasons.filter {
            it.fieldValueOrNull(WAR) != null
                    && !it.isPitcher()
                    && majorLeagues.contains(it.league())
        }

        return filteredSeasons
    }

    private fun getBatterCareers(warDailyBatLines: List<String>): List<Career> {
        // get seasons
        val seasons = getBatterSeasons(warDailyBatLines)

        // get player careers
        return getBatterCareersInternal(seasons)
    }

    private fun getWarDailyBatFile(filename: String): List<String> {
        if (!filename.fileExists()) {
            println("filename=$filename does not exist, retrieving from baseball-reference.com ...")
            filename.writeFile(retrieveWarDailyBat())
        } else {
            println("filename=$filename exists, using local copy ...")
        }
        return filename.readFile()
    }

    private fun deserializeToSeasons(warDailyBatLines: List<String>): List<Season> {
        // get header
        val battingFields = warDailyBatLines[0].lowercase().split(",")

        // get season lines
        val seasonLines = warDailyBatLines.subList(1, warDailyBatLines.size)

        // map to season objects
        return seasonLines.map { Season(battingFields.zip(it.split(",")).toMap()) }
    }

    private fun getBatterCareersInternal(seasons: List<Season>): List<Career> {
        // group player's season
        val seasonByPlayer = seasons.groupBy { it.playerId() }

        // summarize player careers
        return seasonByPlayer
            .map { entry ->
                val playerId = entry.key
                val seasonList = entry.value
                Career(
                    playerId = playerId,
                    playerName = entry.value.first().playerName(),
                    war = seasonList.sumOf {
                        it.war()
                    },
                    seasons = seasonList
                )
            }
    }

    private fun retrieveWarDailyBat(): String {
        return OkHttpClient().newCall(
            Request.Builder()
                .url("https://www.baseball-reference.com/data/war_daily_bat.txt")
                .get()
                .build()
        ).execute().use { response ->
            if (!response.isSuccessful) throw IOException("Unexpected code $response")
            response.body!!.string()
        }
    }

    enum class Fields(var fileField: String) {
        WAR("war"),
        PLAYER_NAME("name_common"),
        LEAGUE("lg_id"),
        PLAYER_ID("player_id"),
        IS_PITCHER("pitcher"),
        SEASON("year_id")
    }

    data class Season(val fields: Map<String, String>) {
        fun fieldValueOrNull(fieldName: Fields): String? {
            val fieldValue = fields[fieldName.fileField]
            if (fieldValue == null || fieldValue == "NULL") {
                return null
            }
            return fieldValue
        }
        private fun fieldValue(fieldName: Fields): String {
            return fieldValueOrNull(fieldName)
                ?: throw IllegalArgumentException("Field $fieldName for player ${fields[PLAYER_ID.name] ?: "unavailable"}")
        }
        fun war() = fieldValue(WAR).toDouble()
        fun playerId() = fieldValue(PLAYER_ID)
        fun playerName() = fieldValue(PLAYER_NAME)
        fun league() = fieldValue(LEAGUE)
        fun isPitcher(): Boolean = fieldValue(IS_PITCHER).lowercase() == "y"
        fun season(): Int = fieldValue(SEASON).toInt()
    }

    data class Career(val playerId: String, val playerName: String, val war: Double, val seasons: List<Season>) {
        fun careerWar(): Double = seasons.sumOf { it.war() }.roundToDecimalPlaces(2)

        fun seasonRange(): String {
            return "${seasons.minOf { it.season() }}-${seasons.maxOf { it.season()}}"
        }

        fun seasonCount(): Int {
            return seasons.map { it.season() }.distinct().size
        }
    }

}

fun String.readFile(): List<String> {
    return File(this).useLines { it.toList() }
}

fun String.fileExists(): Boolean {
    return File(this).exists()
}

fun String.writeFile(contents: String) {
    File(this).writeBytes(contents.encodeToByteArray())
}

fun Double.roundToDecimalPlaces(places: Int) = (this * 10 * places).roundToInt() / (10.0 * places)