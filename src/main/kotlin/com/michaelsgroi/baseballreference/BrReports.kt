package com.michaelsgroi.baseballreference

import com.michaelsgroi.baseballreference.Career.Companion.getCareerFormatter
import com.michaelsgroi.baseballreference.Roster.Companion.getRosterFormatter
import com.michaelsgroi.baseballreference.Roster.RosterId
import com.michaelsgroi.baseballreference.Season.Companion.getSeasonFormatter
import com.michaelsgroi.baseballreference.Verbosity.CONCISE
import com.michaelsgroi.baseballreference.Verbosity.VERBOSE
import java.time.Duration
import java.time.Instant

class BrReports(private val brWarDaily: BrWarDaily, private val reportDir: String = "reports") {
    init {
        reportDir.createDirectoryIfNotExists()
    }

    fun run() {
        val reports = listOf(
            bestTopNSeasons(3, 50),
            bestTopNSeasons(5, 50),
            bestTopNSeasons(7, 50),
            theSteveBalboniAllStars(),
            theRowlandOfficeAllStars(),
            topSeasonWars(10),
            bottomSeasonWars(10),
            bestOrWorstNOfTeam("bos", 30, true),
            bestOrWorstNOfTeam("bos", 30, false),
            bestOrWorstNOfTeam("nyy", 30, true),
            bestOrWorstNOfTeam("nyy", 30, false),
            bestRosters(1000),
            bestRosters(1000, CONCISE),
            bestRostersByFranchise(),
            bestRostersByFranchise(CONCISE),
            roster(RosterId(1928, "pha"), CONCISE),
            roster(RosterId(1928, "pha")),
            roster(RosterId(2005, "nyy")),
            roster(RosterId(2005, "nyy"), CONCISE),
            roster(RosterId(1959, "mln")),
            roster(RosterId(1996, "cle")),
            highestPaidSeasons(20),
            highestPaidSeasonsForTeam(20, "bos"),
            highestPaidSeasonsForTeam(20, "nyy"),
            highestPaidSeasonsByWar(20),
            highestPaidCareersByWar(20),
            highestPaidCareersByWarOfPlayersWhoAreCurrentlyActive(20),
            highestPaidCareersWithNegativeWar(20),
            lowestPaidWarSeasonsByWar(20),
            lowestPaidWarSeasonsByWarInTheModernEra(20),
            lowestPaidWarSeasonsByWarSince2000(20),
            lowestPaidCareersByWar(20),
            lowestPaidCareersByWarInTheModernEra(20),
            lowestPaidCareersByWarSince2000(20),
            lowestPaidCareersByWarWhoAreCurrentlyActive(20),
            career("ruthba01"),
            highestPeakSeasonWarWithCareerWarUnder(20, 10),
            highestPeakSeasonWarWithCareerWarUnder(20, 15),
            highestPeakSeasonWarWithCareerWarUnder(20, 20),
            highestPeakSeasonWarWithCareerWarUnder(20, 30),
            lowestPeakSeasonWarWithCareerWarOver(20, 40),
            lowestPeakSeasonWarWithCareerWarOver(20, 50),
            lowestPeakSeasonWarWithCareerWarOver(20, 60),
            lowestPeakSeasonWarWithCareerWarOver(20, 70),
            lowestPeakSeasonWarWithCareerWarOver(20, 80),
            lowestPeakSeasonWarWithCareerWarOver(20, 90),
            lowestPeakSeasonWarWithCareerWarOver(20, 100),
            lowestPeakSeasonWarWithCareerWarOver(20, 110),
            lowestPeakSeasonWarWithCareerWarOver(20, 120),
            lowestPeakSeasonWarWithCareerWarOver(20, 130),
            lowestPeakSeasonWarWithCareerWarOver(20, 140),
            lowestPeakSeasonWarWithCareerWarOver(20, 150),
            lowestPeakSeasonWarWithCareerWarOver(20, 160),
            playersWhoseNameStartsWith("Cecil "),
            playersWhoseNameStartsWith("Babe "),
            playersWhoseNameContains("war"),
        )
        println("running ${reports.size} reports to '$reportDir' directory")
        val startMs = Instant.now().toEpochMilli()
        reports.forEachIndexed { index, report ->
            writeReport(report)
            val etaDuration = Duration.ofMillis(
                (reports.size - index + 1) *
                        ((Instant.now().toEpochMilli() - startMs) / (index + 1))
            )
            println("report #${(index + 1)} of ${reports.size}: ${report.filename}, estimated time to complete: $etaDuration")
        }
        println("wrote ${reports.size} reports to '$reportDir' directory")
    }

    private fun bestTopNSeasons(topNSeasons: Int, topNPlayers: Int): Report<Career> {
        return buildReport(listOf(topNSeasons, topNPlayers), getCareerFormatter()) {
            brWarDaily.careers.map { career ->
                career.copy(seasonsPredicate = {
                    it.season in career.seasons().sortedByDescending { season -> season.war }.take(topNSeasons)
                        .map { season -> season.season }.toSet()
                })
            }.sortedByDescending { career -> career.war() }.take(topNPlayers)
        }
    }

    private fun playersWhoseNameStartsWith(startsWith: String): Report<Career> {
        return buildReport(startsWith, getCareerFormatter()) {
            brWarDaily.careers.filter { it.playerName.lowercase().startsWith(startsWith.lowercase()) }
                .sortedByDescending { it.war() }
        }
    }

    private fun playersWhoseNameContains(contains: String) =
        buildReport(contains, getCareerFormatter()) {
            brWarDaily.careers.filter { contains.lowercase() in it.playerName.lowercase() }
                .sortedByDescending { it.war() }
        }

    private fun highestPeakSeasonWarWithCareerWarUnder(topN: Int, maxWar: Int) =
        buildReport(listOf(topN, maxWar), getCareerFormatter(includePeakWar = true)) {
            val careersUnderMaxWar =
                brWarDaily.careers.filter { it.war() < maxWar }.associateBy { it.playerId }

            brWarDaily.seasons
                .asSequence()
                .filter { it.playerId in careersUnderMaxWar.keys }.sortedByDescending { it.war }.take(topN)
                .map { careersUnderMaxWar[it.playerId]!! }.toList()
        }

    private fun lowestPeakSeasonWarWithCareerWarOver(topN: Int, minWar: Int) =
        buildReport(listOf(topN, minWar), getCareerFormatter(includePeakWar = true)) {
            val careersOverMinWar = brWarDaily.careers.filter { it.war() > minWar }.associateBy { it.playerId }
            val minimumMaxSeasons =
                careersOverMinWar.values.map { career -> career.seasons().maxBy { season -> season.war } }
                    .sortedBy { season -> season.war }
            minimumMaxSeasons.map { careersOverMinWar[it.playerId]!! }.take(topN)
        }


    private fun career(playerId: String) =
        buildReport(playerId, getSeasonFormatter()) {
            brWarDaily.careers.first { it.playerId == playerId }.seasons().toList()
        }

    private fun highestPaidSeasons(topN: Int) =
        buildReport(topN, getSeasonFormatter(includeSalary = true)) {
            brWarDaily.seasons.sortedByDescending { it.salary }.take(topN)
        }

    private fun highestPaidSeasonsForTeam(topN: Int, team: String) =
        buildReport(listOf(topN, team), getSeasonFormatter(includeSalary = true)) {
            brWarDaily.seasons.filter { team.lowercase() in it.teams.map { team -> team.lowercase() } }
                .sortedByDescending { it.salary }.take(topN).toList()
        }

    private fun lowestPaidWarSeasonsByWar(topN: Int) =
        buildReport(
            topN,
            getSeasonFormatter(includeSalary = true),
            getLowestPaidWarSeasonsReportFunction(topN) { true })

    private fun lowestPaidWarSeasonsByWarInTheModernEra(topN: Int) =
        buildReport(
            topN,
            getSeasonFormatter(includeSalary = true),
            getLowestPaidWarSeasonsReportFunction(topN) { year -> year >= 1947 })

    private fun lowestPaidWarSeasonsByWarSince2000(topN: Int) =
        buildReport(
            topN,
            getSeasonFormatter(includeSalary = true),
            getLowestPaidWarSeasonsReportFunction(topN) { year -> year >= 2000 })

    private fun getLowestPaidWarSeasonsReportFunction(topN: Int, yearFilter: (Int) -> Boolean): () -> List<Season> = {
        brWarDaily.seasons.filter { it.salary > 0 && yearFilter(it.season) }
            .sortedByDescending { it.war / it.salary }.take(topN)
    }

    private fun lowestPaidCareersByWar(topN: Int) =
        buildReport(topN, getCareerFormatter(includeSalary = true), lowestPaidWarCareersReportFunction(topN) { true })

    private fun lowestPaidCareersByWarInTheModernEra(topN: Int) =
        buildReport(
            topN,
            getCareerFormatter(includeSalary = true),
            lowestPaidWarCareersReportFunction(topN) { year -> year >= 1947 })

    private fun lowestPaidCareersByWarSince2000(topN: Int) =
        buildReport(
            topN,
            getCareerFormatter(includeSalary = true),
            lowestPaidWarCareersReportFunction(topN) { year -> year >= 2000 })

    private fun lowestPaidCareersByWarWhoAreCurrentlyActive(topN: Int) =
        buildReport(
            topN,
            getCareerFormatter(includeSalary = true),
            lowestPaidWarCareersReportFunction(topN) { year -> year >= 2022 })

    private fun lowestPaidWarCareersReportFunction(topN: Int, yearFilter: (Int) -> Boolean): () -> List<Career> = {
        brWarDaily.careers.filter { it.salary() > 0 && yearFilter(it.seasons().maxOf { sl -> sl.season }) }
            .sortedByDescending { it.war() / it.salary() }.take(topN)
    }

    private fun highestPaidSeasonsByWar(topN: Int) =
        buildReport(topN, getSeasonFormatter(includeSalary = true)) {
            val seasons = brWarDaily.seasons
            val lowestWar = seasons.minOf { it.war }
            seasons.filter { it.salary > 0 }.sortedByDescending { it.salary / (it.war - lowestWar) }.take(topN)
        }

    private fun highestPaidCareersByWar(topN: Int) =
        buildReport(topN, getCareerFormatter(includeSalary = true)) {
            val careers = brWarDaily.careers
            val lowestWar = careers.minOf { it.war() }
            careers.filter { it.salary() > 0 }.sortedByDescending { it.salary() / (it.war() - lowestWar) }.take(topN)
        }

    private fun highestPaidCareersByWarOfPlayersWhoAreCurrentlyActive(topN: Int) =
        buildReport(topN, getCareerFormatter(includeSalary = true)) {
            val careers = brWarDaily.careers
            val lowestWar = careers.minOf { it.war() }

            careers.filter { it.salary() > 0 && it.seasons().maxOf { sl -> sl.season } >= 2022 }
                .sortedByDescending { it.salary() / (it.war() - lowestWar) }.take(topN)
        }

    private fun highestPaidCareersWithNegativeWar(topN: Int) =
        buildReport(topN, getCareerFormatter(includeSalary = true)) {
            brWarDaily.careers.filter { it.war() < 0 }.sortedByDescending { it.salary() }.take(topN)
        }

    private fun theSteveBalboniAllStars() = buildReport(getCareerFormatter()) {
        brWarDaily.careers.sortedWith(compareBy({ it.war() }, { it.playerName }))
            .filter { it.seasonCount() >= 10 && (it.war() / it.seasonCount()) < 0.5 }
    }

    private fun theRowlandOfficeAllStars() =
        buildReport(getCareerFormatter()) {
            brWarDaily.careers.sortedBy { it.war() }.filter { it.seasonCount() >= 10 && it.war() < 0.0 }
        }

    private fun topSeasonWars(topN: Int) =
        buildReport(getSeasonFormatter()) {
            brWarDaily.seasons.sortedByDescending { it.war }.take(topN)
        }

    private fun bottomSeasonWars(topN: Int) =
        buildReport(getSeasonFormatter()) {
            brWarDaily.seasons.sortedBy { it.war }.take(topN)
        }

    private fun bestOrWorstNOfTeam(team: String, topN: Int, best: Boolean): Report<Career> =
        buildReport(listOf(team, topN, if (best) "Best" else "Worst"), getCareerFormatter()) {
            brWarDaily.careers.filter {
                team.lowercase() in it.teams().map { team -> team.lowercase() }
            }.sortedWith { o1, o2 ->
                if (best) {
                    o2.war().compareTo(o1.war())
                } else {
                    o1.war().compareTo(o2.war())
                }
            }.take(topN)
        }

    private fun bestRosters(topN: Int, verbosity: Verbosity = VERBOSE) =
        buildReport(listOf(topN, verbosity), getRosterFormatter(verbosity)) {
            brWarDaily.rosters.sortedByDescending { roster -> roster.players.sumOf { it.war() } }.take(topN)
        }

    private fun bestRostersByFranchise(verbosity: Verbosity = VERBOSE) =
        buildReport(verbosity, getRosterFormatter(verbosity)) {
            val rosters = brWarDaily.rosters
            val topRosters =
                rosters.sortedByDescending { roster -> roster.players.sumOf { it.war() } }
            val teams = rosters.map { it.rosterId.team }.distinct()
            teams.map { team -> topRosters.first { it.rosterId.team == team } }
                .sortedByDescending { roster -> roster.players.sumOf { it.war() } }

        }

    private fun roster(rosterId: RosterId, verbosity: Verbosity = VERBOSE) =
        buildReport(listOf(rosterId.season, rosterId.team, verbosity), getCareerFormatter(verbosity)) {
            brWarDaily.rosters.first { it.rosterId == rosterId }.players.sortedByDescending { it.war() }
        }

    private fun <T> writeReport(report: Report<T>) {
        val contents = report.formatter.format(report)
        "$reportDir/${report.filename}".writeFile(contents)
    }

    class Report<T>(
        val name: String,
        val filename: String? = null,
        val run: () -> List<T>,
        val formatter: BrReportFormatter<T>
    )

    private fun <T> buildReport(formatter: BrReportFormatter<T>, run: () -> List<T>): Report<T> {
        return buildReport(emptyList(), formatter, run)
    }

    private fun <T> buildReport(argsString: Any, formatter: BrReportFormatter<T>, run: () -> List<T>): Report<T> {
        return buildReport(listOf(argsString), formatter, run)
    }

    private fun <T> buildReport(
        args: List<Any>,
        formatter: BrReportFormatter<T>,
        run: () -> List<T>
    ): Report<T> {
        val methodName = getCallerMethod()
        return Report(
            name = "${methodName.toHumanReadable()} ${args.joinToString(" ").trim()}",
            filename = "${methodName}${
                if (args.isNotEmpty()) "_" + args.joinToString("_") {
                    it.toString().lowercase()
                }.trim() else ""
            }.txt".toFileName(),
            run = run,
            formatter = formatter
        )
    }

    private fun getCallerMethod(): String {
        val methodsInStack = StackWalker.getInstance().walk { frames -> frames.toList() }
        return methodsInStack.takeLast(methodsInStack.size - 1).first { it.methodName != "buildReport" }.methodName
    }

    companion object {
        private val pattern = "(?<=.)[A-Z]".toRegex()

        private fun String.toHumanReadable(): String {
            val replaced = replace(pattern, " $0").lowercase()
            return replaced[0].uppercase() + replaced.substring(1)
        }

        private fun String.toFileName(): String {
            return replace(pattern, "$0").lowercase()
        }
    }
}