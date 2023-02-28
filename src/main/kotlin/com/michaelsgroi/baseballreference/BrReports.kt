package com.michaelsgroi.baseballreference

import java.time.Duration
import java.time.Instant

// TODO: add column headers to reports
// TODO: cache careers and seasons
class BrReports(private val brWarDaily: BrWarDaily, private val reportDir: String = "reports") {
    init {
        reportDir.createDirectoryIfNotExists()
    }

    fun run() {
        val reports = listOf(
            theSteveBalboniAllStars(),
            theRowlandOfficeAllStars(),
            bottomSeasonWars(10),
            bestOrWorstNOfTeam("bos", 30, true),
            bestOrWorstNOfTeam("bos", 30, false),
            bestOrWorstNOfTeam("nyy", 30, true),
            bestOrWorstNOfTeam("nyy", 30, false),
            bestRosters(1000),
            bestRosters(1000, concise = true),
            bestRostersByFranchise(),
            bestRostersByFranchise(concise = true),
            roster(RosterId(1928, "pha"), concise = true),
            roster(RosterId(1928, "pha")),
            roster(RosterId(2005, "nyy")),
            roster(RosterId(2005, "nyy"), concise = true),
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
            highestSeasonWarWithCareerWarUnder(20, 10),
            highestSeasonWarWithCareerWarUnder(20, 15),
            highestSeasonWarWithCareerWarUnder(20, 20),
            highestSeasonWarWithCareerWarUnder(20, 30),
            lowestSeasonWarWithCareerWarOver(20, 40),
            lowestSeasonWarWithCareerWarOver(20, 50),
            lowestSeasonWarWithCareerWarOver(20, 60),
            lowestSeasonWarWithCareerWarOver(20, 70),
            lowestSeasonWarWithCareerWarOver(20, 80),
            lowestSeasonWarWithCareerWarOver(20, 90),
            lowestSeasonWarWithCareerWarOver(20, 100),
            lowestSeasonWarWithCareerWarOver(20, 110),
            lowestSeasonWarWithCareerWarOver(20, 120),
            lowestSeasonWarWithCareerWarOver(20, 130),
            lowestSeasonWarWithCareerWarOver(20, 140),
            lowestSeasonWarWithCareerWarOver(20, 150),
            lowestSeasonWarWithCareerWarOver(20, 160),
            playersWhoseNameStartsWith("Cecil "),
            playersWhoseNameStartsWith("Babe "),
            playersWhoseNameContains("war"),
        )
        println("running ${reports.size} reports to '$reportDir' directory")
        val startMs = Instant.now().toEpochMilli()
        reports.forEachIndexed { index, report ->
            writeReport(report, report.run())
            val etaDuration = Duration.ofMillis(
                (reports.size - index + 1) *
                        ((Instant.now().toEpochMilli() - startMs) / (index + 1))
            )
            println("report #${(index + 1)} of ${reports.size}: ${report.filename}, estimated time to complete: $etaDuration")
        }
        println("wrote ${reports.size} reports to '$reportDir' directory")
    }

    private fun playersWhoseNameStartsWith(startsWith: String): Report {
        return buildReport(startsWith) {
            brWarDaily.getCareers().filter { it.playerName.lowercase().startsWith(startsWith.lowercase()) }
                .sortedByDescending { it.war }.report()
        }
    }

    private fun playersWhoseNameContains(contains: String) =
        buildReport(contains) {
            brWarDaily.getCareers().filter { contains.lowercase() in it.playerName.lowercase() }
                .sortedByDescending { it.war }.report()
        }

    private fun highestSeasonWarWithCareerWarUnder(topN: Int, maxWar: Int) =
        buildReport(listOf(topN, maxWar)) {
            val careersUnderMaxWar =
                brWarDaily.getCareers().filter { it.war < maxWar }.associateBy { it.playerId }

            brWarDaily.getSeasons()
                .asSequence()
                .filter { it.playerId in careersUnderMaxWar.keys }.sortedByDescending { it.war }.take(topN)
                .map { careersUnderMaxWar[it.playerId]!! }.toList().report(includePeakWar = true)
        }

    private fun lowestSeasonWarWithCareerWarOver(topN: Int, minWar: Int) =
        buildReport(listOf(topN, minWar)) {
            val careersOverMinWar = brWarDaily.getCareers().filter { it.war > minWar }.associateBy { it.playerId }
            val minimumMaxSeasons =
                careersOverMinWar.values.map { career -> career.seasons().maxBy { season -> season.war } }
                    .sortedBy { season -> season.war }
            val careers = minimumMaxSeasons.map { careersOverMinWar[it.playerId]!! }.take(topN)

            careers.report(includePeakWar = true)
        }


    private fun career(playerId: String) =
        buildReport(playerId) {
            brWarDaily.getCareers().first { it.playerId == playerId }.seasons().toList()
                .report(includeSalary = true)
        }

    private fun highestPaidSeasons(topN: Int) =
        buildReport(topN) {
            brWarDaily.getSeasons().sortedByDescending { it.salary }.take(topN).report(includeSalary = true)
        }

    private fun highestPaidSeasonsForTeam(topN: Int, team: String) =
        buildReport(listOf(topN, team)) {
            brWarDaily.getSeasons().filter { team.lowercase() in it.teams.map { team -> team.lowercase() } }
                .sortedByDescending { it.salary }.take(topN).toList().report(includeSalary = true)
        }

    private fun lowestPaidWarSeasonsByWar(topN: Int) =
        buildReport(topN, getLowestPaidWarSeasonsReportFunction(topN) { true })

    private fun lowestPaidWarSeasonsByWarInTheModernEra(topN: Int) =
        buildReport(topN, getLowestPaidWarSeasonsReportFunction(topN) { year -> year >= 1947 })

    private fun lowestPaidWarSeasonsByWarSince2000(topN: Int) =
        buildReport(topN, getLowestPaidWarSeasonsReportFunction(topN) { year -> year >= 2000 })

    private fun getLowestPaidWarSeasonsReportFunction(topN: Int, yearFilter: (Int) -> Boolean): () -> List<String> = {
        brWarDaily.getSeasons().filter { it.salary > 0 && yearFilter(it.season) }
            .sortedByDescending { it.war / it.salary }.take(topN).report(includeSalary = true)
    }

    private fun lowestPaidCareersByWar(topN: Int) =
        buildReport(topN, lowestPaidWarCareersReportFunction(topN) { true })

    private fun lowestPaidCareersByWarInTheModernEra(topN: Int) =
        buildReport(topN, lowestPaidWarCareersReportFunction(topN) { year -> year >= 1947 })

    private fun lowestPaidCareersByWarSince2000(topN: Int) =
        buildReport(topN, lowestPaidWarCareersReportFunction(topN) { year -> year >= 2000 })

    private fun lowestPaidCareersByWarWhoAreCurrentlyActive(topN: Int) =
        buildReport(topN, lowestPaidWarCareersReportFunction(topN) { year -> year >= 2022 })

    private fun lowestPaidWarCareersReportFunction(topN: Int, yearFilter: (Int) -> Boolean): () -> List<String> = {
        brWarDaily.getCareers().filter { it.salary() > 0 && yearFilter(it.seasonLines.maxOf { sl -> sl.season() }) }
            .sortedByDescending { it.war / it.salary() }.take(topN).report(includeSalary = true)
    }

    private fun highestPaidSeasonsByWar(topN: Int) =
        buildReport(topN) {
            val seasons = brWarDaily.getSeasons()
            val lowestWar = seasons.minOf { it.war }
            seasons.filter { it.salary > 0 }.sortedByDescending { it.salary / (it.war - lowestWar) }.take(topN)
                .report(includeSalary = true)
        }

    private fun highestPaidCareersByWar(topN: Int) =
        buildReport(topN) {
            val careers = brWarDaily.getCareers()
            val lowestWar = careers.minOf { it.war }
            careers.filter { it.salary() > 0 }.sortedByDescending { it.salary() / (it.war - lowestWar) }.take(topN)
                .report(includeSalary = true)
        }

    private fun highestPaidCareersByWarOfPlayersWhoAreCurrentlyActive(topN: Int) =
        buildReport(topN) {
            val careers = brWarDaily.getCareers()
            val lowestWar = careers.minOf { it.war }

            careers.filter { it.salary() > 0 && it.seasonLines.maxOf { sl -> sl.season() } >= 2022 }
                .sortedByDescending { it.salary() / (it.war - lowestWar) }.take(topN)
                .report(includeSalary = true)
        }

    private fun highestPaidCareersWithNegativeWar(topN: Int) =
        buildReport(topN) {
            brWarDaily.getCareers().filter { it.war < 0 }.sortedByDescending { it.salary() }.take(topN)
                .report(includeSalary = true)
        }

    private fun theSteveBalboniAllStars() = buildReport {
        brWarDaily.getCareers().sortedWith(compareBy({ it.war }, { it.playerName }))
            .filter { it.seasonCount() >= 10 && (it.war() / it.seasonCount()) < 0.5 }.report()
    }

    private fun theRowlandOfficeAllStars() =
        buildReport {
            brWarDaily.getCareers().sortedBy { it.war() }.filter { it.seasonCount() >= 10 && it.war() < 0.0 }.report()
        }

    private fun bottomSeasonWars(topN: Int) =
        buildReport {
            brWarDaily.getSeasons().sortedBy { it.war }.take(topN).report()
        }

    private fun bestOrWorstNOfTeam(team: String, topN: Int, best: Boolean): Report =
        buildReport(listOf(team, topN, if (best) "Best" else "Worst")) {
            brWarDaily.getCareers().filter {
                team.lowercase() in it.teams().map { team -> team.lowercase() }
            }.sortedWith { o1, o2 ->
                if (best) {
                    o2.war().compareTo(o1.war())
                } else {
                    o1.war().compareTo(o2.war())
                }
            }.take(topN).report()
        }

    private fun bestRosters(topN: Int, concise: Boolean = false) =
        buildReport(listOf(topN, concise.concise())) {
            brWarDaily.getRosters().sortedByDescending { roster -> roster.players.sumOf { it.war } }.take(topN)
                .report(concise, roundWarDecimalPlaces = 0)
        }

    private fun bestRostersByFranchise(concise: Boolean = false) =
        buildReport(concise.concise()) {
            val rosters = brWarDaily.getRosters()
            val topRosters =
                rosters.sortedByDescending { roster -> roster.players.sumOf { it.war } }
            val teams = rosters.map { it.rosterId.team }.distinct()
            teams.map { team -> topRosters.first { it.rosterId.team == team } }
                .sortedByDescending { roster -> roster.players.sumOf { it.war } }
                .report(concise, roundWarDecimalPlaces = 0)
        }

    private fun roster(rosterId: RosterId, concise: Boolean = false) =
        buildReport(listOf(rosterId.season, rosterId.team, concise.concise())) {
            brWarDaily.getRosters().first { it.rosterId == rosterId }.players.sortedByDescending { it.war }
                .report(concise = concise)
        }

    private fun writeReport(report: Report, lines: List<String>) {
        val contents = lines.joinToString("\n")
        val header = "${report.name}\n${lines.size} rows\n\n"
        val filename = report.filename
        "$reportDir/$filename".writeFile("$header$contents")
    }

    private fun List<Roster>.report(concise: Boolean = false, roundWarDecimalPlaces: Int = 2): List<String> {
        return if (concise) {
            mapIndexed { index, roster ->
                "${(index + 1)}: " + roster.report(concise, roundWarDecimalPlaces)
            }
        } else {
            val maxLength = this.size.toString().length
            mapIndexed { index, roster ->
                "${("#" + (index + 1)).padStart(maxLength + 1)}: " + roster.report(concise, roundWarDecimalPlaces)
            }
        }
    }

    private fun Roster.report(concise: Boolean = false, roundWarDecimalPlaces: Int = 2): String {
        val season = rosterId.season.toString()
        val team = rosterId.team
        val war = players.sumOf { it.war }.roundToDecimalPlaces(roundWarDecimalPlaces)
        return if (concise) {
            "$season $team $war"
        } else {
            season.padEnd(5) + team.padEnd(4) + war.padStart(7)
        }
    }

    private fun List<Career>.report(
        concise: Boolean = false,
        includeSalary: Boolean = false,
        includePeakWar: Boolean = false
    ): List<String> {
        val maxLength = this.size.toString().length
        return this.mapIndexed { index, career ->
            val prefixNew = if (concise) {
                "${(index + 1)}: "
            } else {
                "${("#" + (index + 1)).padStart(maxLength + 1)}: "
            }
            prefixNew + career.report(
                concise,
                includeSalary,
                includePeakWar
            )
        }
    }

    private fun Career.report(
        concise: Boolean = false,
        includeSalary: Boolean = false,
        includePeakWar: Boolean = false
    ): String {
        if (concise) {
            return "$playerName ${war().roundToDecimalPlaces(0)}"
        }
        return this.playerName.padEnd(24) +
                war().roundToDecimalPlaces(2).padStart(7) +
                (if (includePeakWar) {
                    val peakSeason = seasons().maxBy { it.war }
                    ("${peakSeason.war.roundToDecimalPlaces(2)} (${peakSeason.season})").padStart(15)
                } else "") +
                (if (includeSalary) salary().toString().padStart(15) else "") +
                seasonCount().toString().padStart(3) +
                (" (" + seasonRange() + ") ").padEnd(12) +
                teams().joinToString(",")
    }

    private fun List<Season>.report(includeSalary: Boolean = false): List<String> {
        val maxLength = this.size.toString().length
        return this.mapIndexed { index, career ->
            "${("#" + (index + 1)).padStart(maxLength + 1)}: " + career.report(
                includeSalary
            )
        }
    }

    private fun Season.report(includeSalary: Boolean = false): String =
        playerName.padEnd(24) +
                war.roundToDecimalPlaces(2).padStart(7) +
                (if (includeSalary) salary.toString().padStart(15) else "") +
                season.toString().padStart(5) + " " +
                teams.joinToString(",")

    class Report(
        val name: String,
        val filename: String? = null,
        val run: () -> List<String>
    )

    private fun buildReport(run: () -> List<String>): Report {
        return buildReport(emptyList(), run)
    }

    private fun buildReport(argsString: Any, run: () -> List<String>): Report {
        return buildReport(listOf(argsString), run)
    }

    private fun buildReport(argsStrings: List<Any>, run: () -> List<String>): Report {
        val methodName = getCallerMethod()
        return Report(
            name = "${methodName.toHumanReadable()} ${argsStrings.joinToString(" ").trim()}",
            filename = "${methodName}${
                if (argsStrings.isNotEmpty()) "_" + argsStrings.joinToString("_") {
                    it.toString().lowercase()
                }.trim() else ""
            }.txt".toFileName(),
            run = run
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

    private fun Boolean.concise() = if (this) "concise" else ""

}