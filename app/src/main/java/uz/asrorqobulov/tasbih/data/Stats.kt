package uz.asrorqobulov.tasbih.data

import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

/** Statistika faqat sessiyada sanalgan sondan hisoblanadi (PLAN 6.1). Sessiya boshlangan kunga yoziladi. */
class Stats(sessions: List<SessionRecord>, private val zone: ZoneId = ZoneId.systemDefault()) {
    val byDay: Map<LocalDate, Long> = sessions.groupBy { day(it.startedAt) }
        .mapValues { (_, v) -> v.sumOf { it.counted } }

    fun day(millis: Long): LocalDate = Instant.ofEpochMilli(millis).atZone(zone).toLocalDate()

    fun total(from: LocalDate, to: LocalDate): Long =
        byDay.filterKeys { it in from..to }.values.sum()

    /** Bugungacha (bugun hali sanalmagan bo'lsa — kechagacha) ketma-ket kunlar. */
    fun streak(today: LocalDate): Int {
        var d = if ((byDay[today] ?: 0) > 0) today else today.minusDays(1)
        var n = 0
        while ((byDay[d] ?: 0) > 0) { n++; d = d.minusDays(1) }
        return n
    }

    /** Davrdagi eng ko'p sanalgan kun. */
    fun best(from: LocalDate, to: LocalDate): Pair<LocalDate, Long>? =
        byDay.filter { it.key in from..to && it.value > 0 }.maxByOrNull { it.value }?.toPair()

    /** Davrdagi eng uzun ketma-ket kunlar. */
    fun longestStreak(from: LocalDate, to: LocalDate): Int {
        var best = 0
        var run = 0
        var prev: LocalDate? = null
        byDay.keys.filter { it in from..to && (byDay[it] ?: 0) > 0 }.sorted().forEach { d ->
            run = if (prev?.plusDays(1) == d) run + 1 else 1
            best = maxOf(best, run)
            prev = d
        }
        return best
    }

    /** Birinchi sanalgan kun (o'rtacha uchun "Jami" davr boshi). */
    fun firstDay(): LocalDate? = byDay.filterValues { it > 0 }.keys.minOrNull()
}
