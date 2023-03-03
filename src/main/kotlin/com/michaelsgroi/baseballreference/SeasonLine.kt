package com.michaelsgroi.baseballreference

import com.michaelsgroi.warrencromartie.War.Fields
import com.michaelsgroi.warrencromartie.War.SeasonType

data class SeasonLine(val fields: Map<String, String>) {
    fun fieldValueOrNull(fieldName: Fields): String? {
        return fields[fieldName.fileField]?.let { fieldValue -> if (fieldValue == "NULL") null else fieldValue }
    }

    private fun fieldValue(fieldName: Fields): String {
        return fieldValueOrNull(fieldName)
            ?: throw IllegalArgumentException("Field $fieldName for player ${fields[Fields.PLAYER_ID.name] ?: "unavailable"}")
    }

    fun war() = fieldValue(Fields.WAR).toDouble()
    fun playerId() = fieldValue(Fields.PLAYER_ID)
    fun playerName() = fieldValue(Fields.PLAYER_NAME)
    fun league() = fieldValue(Fields.LEAGUE)
    fun team() = fieldValue(Fields.TEAM_ID)
    fun salary() = fieldValueOrNull(Fields.SALARY)?.toLong() ?: 0L
    fun seasonsType() = SeasonType.valueOf(fieldValue(Fields.SEASON_TYPE).uppercase())
    fun season() = fieldValue(Fields.SEASON).toInt()
}