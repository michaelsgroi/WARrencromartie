package com.michaelsgroi.baseballreference

import com.michaelsgroi.baseballreference.BrWarDaily.Companion.fileExpiration
import com.michaelsgroi.baseballreference.BrWarDaily.Companion.majorLeagues
import com.michaelsgroi.baseballreference.BrWarDaily.Fields.WAR
import com.michaelsgroi.baseballreference.CachedFile.Companion.loadFromCache
import okhttp3.HttpUrl
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.IOException
import java.time.Duration

class BrWarDailyLines(
    private val filename: String,
    private val seasonType: BrWarDaily.SeasonType,
    private val expiration: Duration = fileExpiration
) {

    fun getSeasons(): List<SeasonLine> {
        // get seasons
        val seasons = deserializeToSeasons(getWarDailyFile())

        // filter for seasons for position players with war values
        val filteredSeasons = seasons.filter {
            it.fieldValueOrNull(WAR) != null && it.league() in majorLeagues
        }

        return filteredSeasons
    }

    fun getCareers(): List<Career> {
        // get seasons
        val seasons = getSeasons()

        // get player careers
        return getCareersInternal(seasons)
    }

    private fun deserializeToSeasons(warDailyLines: List<String>): List<SeasonLine> {
        // get header
        val fields = warDailyLines[0].lowercase().split(",")

        // get season lines
        val seasonLines = warDailyLines.subList(1, warDailyLines.size)

        // map to season objects
        return seasonLines.map {
            val fieldValues = it.split(",")
            val fieldsMap =
                ((fields zip fieldValues) +
                    (BrWarDaily.Fields.SEASON_TYPE.fileField to seasonType.name.lowercase())).toMap()
            SeasonLine(fieldsMap)
        }
    }

    private fun getCareersInternal(seasonLines: List<SeasonLine>): List<Career> {
        // group player's season
        val seasonByPlayer = seasonLines.groupBy { it.playerId() }

        // summarize player careers
        return seasonByPlayer
            .map { (playerId, seasonList) ->
                Career(
                    playerId = playerId,
                    playerName = seasonList.first().playerName(),
                    war = seasonList.sumOf {
                        it.war()
                    },
                    seasonLines = seasonList
                )
            }
    }

    private val warDailyUrl = HttpUrl.Builder()
        .scheme("https")
        .host("www.baseball-reference.com")
        .addPathSegment("data")
        .addPathSegment(seasonType.brFilename)
        .build()

    private fun getWarDailyFile(): List<String> {
        return loadFromCache(filename, expiration) {
            OkHttpClient().newCall(
                Request.Builder()
                    .url(warDailyUrl)
                    .get()
                    .build()
            ).execute().use { response ->
                if (!response.isSuccessful) throw IOException("Unexpected code $response")
                response.body!!.use { it.string() }
            }
        }
    }
}
