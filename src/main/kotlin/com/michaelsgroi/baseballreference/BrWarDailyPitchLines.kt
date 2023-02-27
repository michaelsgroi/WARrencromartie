package com.michaelsgroi.baseballreference

import com.michaelsgroi.baseballreference.BrWarDaily.Companion.majorLeagues
import com.michaelsgroi.baseballreference.BrWarDaily.Companion.warDailyPitchFile
import com.michaelsgroi.baseballreference.BrWarDaily.Fields.WAR
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.IOException

class BrWarDailyPitchLines(private val filename: String = warDailyPitchFile) {

    fun getPitcherSeasons(): List<SeasonLine> {
        // get seasons
        val seasons = deserializeToPitchingSeasons(getWarDailyPitchFile())

        // filter for seasons for position players with war values
        val filteredSeasons = seasons.filter {
            it.fieldValueOrNull(WAR) != null && majorLeagues.contains(it.league())
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
            val fields = pitchingFields.zip(fieldValues).toMap().toMutableMap()
            fields[BrWarDaily.Fields.SEASON_TYPE.fileField] = BrWarDaily.SeasonType.PITCHING.name.lowercase()
            val seasonLine = SeasonLine(fields)
            seasonLine
        }
    }

    private fun getPitcherCareersInternal(seasonLines: List<SeasonLine>): List<Career> {
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

    private fun getWarDailyPitchFile(): List<String> {
        return BrWarDaily.loadFromCache(filename) {
            OkHttpClient().newCall(
                Request.Builder()
                    .url("https://www.baseball-reference.com/data/war_daily_pitch.txt")
                    .get()
                    .build()
            ).execute().use { response ->
                if (!response.isSuccessful) throw IOException("Unexpected code $response")
                response.body!!.string()
            }
        }
    }

}
