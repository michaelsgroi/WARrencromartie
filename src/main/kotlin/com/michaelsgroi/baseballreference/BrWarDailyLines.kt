package com.michaelsgroi.baseballreference

import com.michaelsgroi.baseballreference.BrWarDaily.Companion.FILE_EXPIRATION
import com.michaelsgroi.baseballreference.BrWarDaily.Companion.MAJOR_LEAGUES
import com.michaelsgroi.baseballreference.BrWarDaily.Fields.WAR
import java.io.File
import java.time.Duration

class BrWarDailyLines(
    private val filename: String,
    private val seasonType: BrWarDaily.SeasonType,
    private val expiration: Duration = FILE_EXPIRATION,
) {
    fun getSeasons(): List<SeasonLine> {
        val seasons = deserializeToSeasons(getWarDailyFile())
        return seasons.filter {
            it.fieldValueOrNull(WAR) != null && it.league() in MAJOR_LEAGUES
        }
    }

    fun getCareers(): List<Career> = getCareersInternal(getSeasons())

    private fun deserializeToSeasons(warDailyLines: List<String>): List<SeasonLine> {
        val fields = warDailyLines[0].lowercase().split(",")
        val seasonLines = warDailyLines.subList(1, warDailyLines.size)
        return seasonLines.map {
            val fieldValues = it.split(",")
            val fieldsMap =
                (
                    (fields zip fieldValues) +
                        (BrWarDaily.Fields.SEASON_TYPE.fileField to seasonType.name.lowercase())
                ).toMap()
            SeasonLine(fieldsMap)
        }
    }

    private fun getCareersInternal(seasonLines: List<SeasonLine>): List<Career> =
        seasonLines
            .groupBy { it.playerId() }
            .map { (playerId, seasonList) ->
                Career(
                    playerId = playerId,
                    playerName = seasonList.first().playerName(),
                    seasonLines = seasonList,
                )
            }

    private fun getWarDailyFile(): List<String> =
        BrWarDaily.loadFromCache(filename, expiration) {
            BrWarDaily.downloadAll(expiration)
            filename.readFile().joinToString("\n")
        }
}
