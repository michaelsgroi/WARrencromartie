package com.michaelsgroi.baseballdatabank

import com.michaelsgroi.baseballreference.Career.Companion.relevantAwards
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class BaseballDatabankTest {

    @Test
    fun test() {
        val actualVotesByYear = BaseballDatabank("awardshareplayers.txt").getRankedAwardSharePlayers()
            .filter { voteLine -> voteLine.playerId == "stiebda01" }.groupBy { it.season }
            .mapValues { it.value.toSet().awardVotes(relevantAwards) }
        assertEquals(
            mapOf(
                1982 to 36.0,
                1984 to 5.0,
                1985 to 2.0,
                1990 to 3.0,
                1981 to 1.0
            ), actualVotesByYear
        )
    }

}
