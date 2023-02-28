package com.michaelsgroi.baseballreference

import com.michaelsgroi.baseballreference.BrWarDaily.Companion.fileExpiration
import com.michaelsgroi.baseballreference.BrWarDaily.Companion.loadFromCache
import com.michaelsgroi.baseballreference.BrWarDaily.Companion.majorLeagues
import com.michaelsgroi.baseballreference.BrWarDaily.Companion.warDailyBatFile
import com.michaelsgroi.baseballreference.BrWarDaily.Fields.WAR
import okhttp3.HttpUrl
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.IOException
import java.time.Duration

class BrWarDailyBatLines(
    private val filename: String = warDailyBatFile,
    private val expiration: Duration = fileExpiration
) {

    fun getBatterSeasons(): List<SeasonLine> {
        // get seasons
        val seasons = deserializeToBattingSeasons(getWarDailyBatFile())

        // filter for seasons for position players with war values
        val filteredSeasons = seasons.filter {
            it.fieldValueOrNull(WAR) != null && it.league() in majorLeagues
        }

        return filteredSeasons
    }

    fun getBatterCareers(): List<Career> {
        // get seasons
        val seasons = getBatterSeasons()

        // get player careers
        return getBatterCareersInternal(seasons)
    }

    private fun deserializeToBattingSeasons(warDailyBatLines: List<String>): List<SeasonLine> {
        // get header
        val battingFields = warDailyBatLines[0].lowercase().split(",")

        // get season lines
        val seasonLines = warDailyBatLines.subList(1, warDailyBatLines.size)

        // map to season objects
        return seasonLines.map {
            val fieldValues = it.split(",")
            val fields = battingFields.zip(fieldValues).toMap().toMutableMap()
            fields[BrWarDaily.Fields.SEASON_TYPE.fileField] = BrWarDaily.SeasonType.BATTING.name.lowercase()
            val seasonLine = SeasonLine(fields)
            seasonLine
        }
    }

    private fun getBatterCareersInternal(seasonLines: List<SeasonLine>): List<Career> {
        // group player's season
        val seasonByPlayer = seasonLines.groupBy { it.playerId() }

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
                    seasonLines = seasonList
                )
            }
    }

    private val warDailyBatUrl = HttpUrl.Builder()
        .scheme("https")
        .host("www.baseball-reference.com")
        .addPathSegment("data")
        .addPathSegment("/war_daily_bat.txt")
        .build()

    private fun getWarDailyBatFile(): List<String> {
        return loadFromCache(filename, expiration) {
            OkHttpClient().newCall(
                Request.Builder()
                    .url(warDailyBatUrl)
                    .get()
                    .build()
            ).execute().use { response ->
                if (!response.isSuccessful) throw IOException("Unexpected code $response")
                response.body!!.use { it.string() }
            }
        }
    }

}
