package com.michaelsgroi.baseballreference

import com.michaelsgroi.baseballreference.BrWarDaily.Companion.fileExpiration
import com.michaelsgroi.baseballreference.BrWarDaily.Companion.majorLeagues
import com.michaelsgroi.baseballreference.BrWarDaily.Companion.warDailyPitchFile
import com.michaelsgroi.baseballreference.BrWarDaily.Fields.WAR
import okhttp3.HttpUrl
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.IOException
import java.time.Duration

class BrWarDailyPitchLines(
    private val filename: String = warDailyPitchFile,
    private val expiration: Duration = fileExpiration
) {

    fun getPitcherSeasons(): List<SeasonLine> {
        // get seasons
        val seasons = deserializeToPitchingSeasons(getWarDailyPitchFile())

        // filter for seasons for position players with war values
        val filteredSeasons = seasons.filter {
            it.fieldValueOrNull(WAR) != null && it.league() in majorLeagues
        }

        return filteredSeasons
    }

    fun getPitcherCareers(): List<Career> {
        // get seasons
        val seasons = getPitcherSeasons()

        // get player careers
        return getPitcherCareersInternal(seasons)
    }

    private fun deserializeToPitchingSeasons(warDailyPitchLines: List<String>): List<SeasonLine> {
        // get header
        val pitchingFields = warDailyPitchLines[0].lowercase().split(",")

        // get season lines
        val seasonLines = warDailyPitchLines.subList(1, warDailyPitchLines.size)

        // map to season objects
        return seasonLines.map {
            val fieldValues = it.split(",")
            val fields = (pitchingFields zip fieldValues).toMap().toMutableMap()
            fields[BrWarDaily.Fields.SEASON_TYPE.fileField] = BrWarDaily.SeasonType.PITCHING.name.lowercase()
            SeasonLine(fields)
        }
    }

    private fun getPitcherCareersInternal(seasonLines: List<SeasonLine>): List<Career> {
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

    private val warDailyPitchUrl = HttpUrl.Builder()
        .scheme("https")
        .host("www.baseball-reference.com")
        .addPathSegment("data")
        .addPathSegment("/war_daily_pitch.txt")
        .build()

    private fun getWarDailyPitchFile(): List<String> {
        return BrWarDaily.loadFromCache(filename, expiration) {
            OkHttpClient().newCall(
                Request.Builder()
                    .url(warDailyPitchUrl)
                    .get()
                    .build()
            ).execute().use { response ->
                if (!response.isSuccessful) throw IOException("Unexpected code $response")
                response.body!!.use { it.string() }
            }
        }
    }

}
