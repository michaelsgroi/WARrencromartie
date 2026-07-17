package com.michaelsgroi.baseballreference

import com.michaelsgroi.baseballreference.BrWarDaily.Companion.FILE_EXPIRATION
import com.michaelsgroi.baseballreference.BrWarDaily.Companion.MAJOR_LEAGUES
import com.michaelsgroi.baseballreference.BrWarDaily.Fields.WAR
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.IOException
import java.time.Duration
import java.util.zip.ZipInputStream

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
            val client = OkHttpClient()
            val zipUrl = latestArchiveUrl(client)
            println("downloading $zipUrl ...")
            client
                .newCall(Request.Builder().url(zipUrl).get().build())
                .execute()
                .use { response ->
                    if (!response.isSuccessful) throw IOException("Unexpected code $response")
                    response.body.byteStream().use { bodyStream ->
                        ZipInputStream(bodyStream).use { zip ->
                            generateSequence { zip.nextEntry }
                                .firstOrNull { it.name == seasonType.brFilename }
                                ?: throw IOException("${seasonType.brFilename} not found in $zipUrl")
                            zip.bufferedReader().readText()
                        }
                    }
                }
        }

    private fun latestArchiveUrl(client: OkHttpClient): String {
        val indexUrl = "https://www.baseball-reference.com/data/"
        val html =
            client
                .newCall(Request.Builder().url(indexUrl).get().build())
                .execute()
                .use { response ->
                    if (!response.isSuccessful) throw IOException("Unexpected code $response")
                    response.body.string()
                }
        val archiveName =
            Regex("""war_archive-\d{4}-\d{2}-\d{2}\.zip""")
                .findAll(html)
                .map { it.value }
                .maxOrNull()
                ?: throw IOException("No war_archive-*.zip found at $indexUrl")
        return "$indexUrl$archiveName"
    }
}
