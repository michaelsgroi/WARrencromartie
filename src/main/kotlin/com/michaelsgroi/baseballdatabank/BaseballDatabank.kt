package com.michaelsgroi.baseballdatabank

import com.michaelsgroi.baseballdatabank.BaseballDatabank.*
import com.michaelsgroi.baseballdatabank.BaseballDatabank.AwardSharePlayerFields.AWARD
import com.michaelsgroi.baseballdatabank.BaseballDatabank.AwardSharePlayerFields.LEAGUE
import com.michaelsgroi.baseballdatabank.BaseballDatabank.AwardSharePlayerFields.PLAYER_ID
import com.michaelsgroi.baseballdatabank.BaseballDatabank.AwardSharePlayerFields.POINTS_WON
import com.michaelsgroi.baseballdatabank.BaseballDatabank.AwardSharePlayerFields.YEAR
import com.michaelsgroi.baseballdatabank.BaseballDatabank.BaseballDatabankFile.AwardsSharePlayers
import com.michaelsgroi.warrencromartie.CachedFile
import com.michaelsgroi.warrencromartie.Constants.Companion.fileExpiration
import okhttp3.HttpUrl
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.IOException
import java.time.Duration

class BaseballDatabank(private val filename: String, private val expiration: Duration = fileExpiration) {

    fun getRankedAwardSharePlayers(): List<RankedAwardShare> {
        return deserializeToAwardSharePlayers(getFile(AwardsSharePlayers)).groupBy {
            AwardSeason(
                it.season(),
                it.league(),
                it.awardName()
            )
        }
            .flatMap {
                val season = it.key.season
                val league = it.key.league
                val award = it.key.award
                val lines = it.value
                val ordered = lines.sortedByDescending { line -> line.pointsWon() }
                ordered.mapIndexed { index, line ->
                    RankedAwardShare(
                        award,
                        season,
                        league,
                        line.playerId(),
                        line.pointsWon(),
                        index + 1,
                        lines.size
                    )
                }
            }
    }

    data class RankedAwardShare(
        val awardName: String,
        val season: Int,
        val league: String,
        val playerId: String,
        val votePoints: Double,
        val voteRank: Int?,
        val totalVotes: Int
    ) {
        fun award() = Award.fromString(awardName)
        fun isWinner(): Boolean = voteRank == 1
    }

    private data class AwardSeason(
        val season: Int,
        val league: String,
        val award: String
    )

    enum class Award(val fullName: String) {
        CYA("Cy Young"),
        MVP("MVP"),
        ROY("Rookie of the Year");

        companion object {
            fun fromString(str: String): Award {
                return values().firstOrNull {
                    it.fullName.lowercase() == str.lowercase()
                } ?: throw IllegalArgumentException("encountered invalid award=$str")
            }
        }
    }

    private fun deserializeToAwardSharePlayers(lines: List<String>): List<AwardSharePlayerLine> {
        // get header
        val fields = lines[0].lowercase().split(",")

        // get season lines
        val rows = lines.subList(1, lines.size)

        // map to season objects
        return rows.map {
            val fieldValues = it.split(",")
            val fieldsMap =
                ((fields zip fieldValues)).toMap()
            AwardSharePlayerLine(fieldsMap)
        }
    }

    data class AwardSharePlayerLine(val fields: Map<String, String>) {
        private fun fieldValueOrNull(fieldName: AwardSharePlayerFields): String? {
            return fields[fieldName.fileField]?.let { fieldValue -> if (fieldValue == "NULL") null else fieldValue }
        }

        private fun fieldValue(fieldName: AwardSharePlayerFields): String {
            return fieldValueOrNull(fieldName)
                ?: throw IllegalArgumentException("could not found $fieldName for player=${fields[PLAYER_ID.name] ?: "[unavailable]"}, fields=$fields")
        }

        fun awardName() = fieldValue(AWARD) // TODO enum?
        fun season() = fieldValue(YEAR).toInt()
        fun league() = fieldValue(LEAGUE)
        fun playerId() = fieldValue(PLAYER_ID)
        fun pointsWon() = fieldValue(POINTS_WON).toDouble()
    }

    enum class AwardSharePlayerFields(val fileField: String) {
        /*
         * awardID,yearID,lgID,playerID,pointsWon,pointsMax,pointsMax
         */
        AWARD("awardid"),
        YEAR("yearid"),
        LEAGUE("lgid"),
        PLAYER_ID("playerid"),
        POINTS_WON("pointswon"),
    }

    enum class BaseballDatabankFile(val fileName: String) {
        AwardsSharePlayers("AwardsSharePlayers.csv"),
    }

    private fun getFile(baseballDatabankFile: BaseballDatabankFile): List<String> {
        return CachedFile.loadFromCache(filename, expiration) {
            OkHttpClient().newCall(
                Request.Builder()
                    .url(
                        HttpUrl.Builder()
                            .scheme("https")
                            .host("raw.githubusercontent.com")
                            .encodedPath("/chadwickbureau/baseballdatabank/master/contrib/${baseballDatabankFile.fileName}")
                            .build()
                    )
                    .get()
                    .build()
            ).execute().use { response ->
                if (!response.isSuccessful) throw IOException("Unexpected code $response")
                response.body!!.use { it.string() }
            }
        }
    }

}

fun Set<RankedAwardShare>.awardWinCount(award: Award) = awardWinCount(setOf(award))
fun Set<RankedAwardShare>.awardWinCount(awards: Set<Award>) = filter { awards.contains(it.award()) }.count { it.isWinner() }

fun Set<RankedAwardShare>.awardVotes(award: Award) = awardVotes(setOf(award))
fun Set<RankedAwardShare>.awardVotes(awards: Set<Award>) = filter { awards.contains(it.award()) }.sumOf {
    it.votePoints
}
