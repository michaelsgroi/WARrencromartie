package com.michaelsgroi.baseballreference

import com.michaelsgroi.baseballreference.BrWarDaily.Fields

data class SeasonLine(val fields: Map<String, String>) {
    fun fieldValueOrNull(fieldName: Fields): String? {
        val fieldValue = fields[fieldName.fileField]
        if (fieldValue == null || fieldValue == "NULL") {
            return null
        }
        return fieldValue
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
    fun salary() = fieldValueOrNull(Fields.SALARY)?.toLong() ?: 0
    fun seasonsType() = BrWarDaily.SeasonType.valueOf(fieldValue(Fields.SEASON_TYPE).uppercase())
    fun season() = fieldValue(Fields.SEASON).toInt()
}