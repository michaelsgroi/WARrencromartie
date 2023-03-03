package com.michaelsgroi.baseballreference

import com.michaelsgroi.warrencromartie.CachedFile.Companion.loadFromCache
import com.michaelsgroi.warrencromartie.Constants.Companion.fileExpiration
import com.michaelsgroi.warrencromartie.War
import com.michaelsgroi.warrencromartie.War.Companion.majorLeagues
import com.michaelsgroi.warrencromartie.War.Fields.WAR
import okhttp3.HttpUrl
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.IOException
import java.time.Duration

class BrWarDailyLines(
    private val filename: String,
    private val seasonType: War.SeasonType,
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

    private fun deserializeToSeasons(lines: List<String>): List<SeasonLine> {
        // get header
        val fields = lines[0].lowercase().split(",")

        // get season lines
        val seasonLines = lines.subList(1, lines.size)

        // map to season objects
        return seasonLines.map {
            val fieldValues = it.split(",")
            val fieldsMap =
                ((fields zip fieldValues) +
                    (War.Fields.SEASON_TYPE.fileField to seasonType.name.lowercase())).toMap()
            SeasonLine(fieldsMap)
        }
    }

    private val url = HttpUrl.Builder()
        .scheme("https")
        .host("www.baseball-reference.com")
        .addPathSegment("data")
        .addPathSegment(seasonType.brFilename)
        .build()

    private fun getWarDailyFile(): List<String> {
        return loadFromCache(filename, expiration) {
            OkHttpClient().newCall(
                Request.Builder()
                    .url(url)
                    .get()
                    .build()
            ).execute().use { response ->
                if (!response.isSuccessful) throw IOException("Unexpected code $response")
                response.body!!.use { it.string() }
            }
        }
    }
}
