package uz.asrorqobulov.tasbih.ui

import java.time.temporal.TemporalAdjusters
import java.time.temporal.ChronoUnit
import java.time.DayOfWeek
import androidx.compose.ui.graphics.Color
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.horizontalScroll
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.LocalFireDepartment
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import uz.asrorqobulov.tasbih.R
import uz.asrorqobulov.tasbih.TasbihApp
import uz.asrorqobulov.tasbih.data.SessionRecord
import uz.asrorqobulov.tasbih.data.Stats
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import java.time.format.TextStyle
import java.util.Locale

private enum class Period { TODAY, WEEK, MONTH, ALL }

@Composable
fun StatsScreen(app: TasbihApp) {
    val sessions by app.dao.sessions().collectAsState(initial = emptyList())
    var period by rememberSaveable { mutableStateOf(Period.WEEK) }
    var detail by remember { mutableStateOf<SessionRecord?>(null) }
    val stats = remember(sessions) { Stats(sessions) }
    val today = LocalDate.now()
    val from = when (period) {
        Period.TODAY -> today
        Period.WEEK -> today.minusDays(6)
        Period.MONTH -> today.minusDays(29)
        Period.ALL -> LocalDate.MIN
    }
    val inPeriod = sessions.filter { stats.day(it.startedAt) >= from }
    val total = inPeriod.sumOf { it.counted }
    val locale = Locale.getDefault()

    // grafik: Bugun/Hafta — 7 kun, Oy — 30 kun, Jami — 12 oy
    val bars: List<Pair<String, Long>> = when (period) {
        Period.TODAY, Period.WEEK -> (6 downTo 0).map { today.minusDays(it.toLong()) }
            .map { it.dayOfWeek.getDisplayName(TextStyle.SHORT, locale).take(2) to (stats.byDay[it] ?: 0) }
        Period.MONTH -> (29 downTo 0).map { today.minusDays(it.toLong()) }
            .map { (if (it.dayOfMonth % 5 == 0) "${it.dayOfMonth}" else "") to (stats.byDay[it] ?: 0) }
        Period.ALL -> (11 downTo 0).map { today.withDayOfMonth(1).minusMonths(it.toLong()) }.map { m ->
            m.month.getDisplayName(TextStyle.NARROW_STANDALONE, locale) to
                stats.byDay.filterKeys { it.year == m.year && it.month == m.month }.values.sum()
        }
    }
    val byTemplate = inPeriod.groupBy { it.templateTitle }.mapValues { (_, v) -> v.sumOf { it.counted } }
        .entries.sortedByDescending { it.value }

    LazyColumn(
        Modifier.fillMaxSize().statusBarsPadding().padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        item {
            Text(stringResource(R.string.nav_stats), fontSize = 26.sp, fontWeight = FontWeight.SemiBold, modifier = Modifier.padding(top = 8.dp, bottom = 6.dp))
            Seg(
                listOf(
                    Period.TODAY to stringResource(R.string.period_today), Period.WEEK to stringResource(R.string.period_week),
                    Period.MONTH to stringResource(R.string.period_month), Period.ALL to stringResource(R.string.period_all),
                ),
                period, { period = it },
            )
        }
        item {
            Row(verticalAlignment = Alignment.Bottom, modifier = Modifier.padding(top = 8.dp)) {
                Text(fmt(total), fontSize = 40.sp, fontWeight = FontWeight.Medium, style = Tnum, modifier = Modifier.weight(1f))
                Row(
                    Modifier.clip(RoundedCornerShape(10.dp)).background(MaterialTheme.colorScheme.surfaceContainer).padding(horizontal = 10.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(Icons.Outlined.LocalFireDepartment, null, tint = Gold, modifier = Modifier.height(18.dp))
                    Text(" ${stats.streak(today)} " + stringResource(R.string.days), fontSize = 13.sp)
                }
            }
        }
        item { BarChart(bars) }
        item {
            // "Jami" — birinchi sanalgan kundan; o'rtacha kalendar kunlariga bo'linadi
            val start = if (period == Period.ALL) stats.firstDay() ?: today else from
            val days = ChronoUnit.DAYS.between(start, today) + 1
            val best = stats.best(start, today)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                MiniStat(stringResource(R.string.avg_day), fmt(total / days), Modifier.weight(1f))
                MiniStat(stringResource(R.string.best_day), best?.let { fmt(it.second) } ?: "—", Modifier.weight(1f),
                    best?.first?.format(DateTimeFormatter.ofPattern("d MMM", locale)))
                MiniStat(stringResource(R.string.longest_streak), "${stats.longestStreak(start, today)}", Modifier.weight(1f), stringResource(R.string.days))
            }
        }
        item { YearMap(stats, today) }
        if (byTemplate.isNotEmpty()) {
            item { SectionHeader(stringResource(R.string.by_template)) }
            item {
                TCard(Modifier.fillMaxWidth()) {
                    val max = byTemplate.first().value.coerceAtLeast(1)
                    byTemplate.forEachIndexed { i, (name, v) ->
                        if (i > 0) RowDivider()
                        Column(Modifier.padding(vertical = 8.dp)) {
                            Row {
                                Text(name.replace("∞", stringResource(R.string.unlimited)), modifier = Modifier.weight(1f))
                                Text(fmt(v), style = Tnum)
                            }
                            LinearProgressIndicator(
                                progress = { v.toFloat() / max }, modifier = Modifier.fillMaxWidth().padding(top = 6.dp),
                                trackColor = MaterialTheme.colorScheme.surfaceContainerHighest, drawStopIndicator = {},
                            )
                        }
                    }
                }
            }
        }
        item { SectionHeader(stringResource(R.string.sessions)) }
        if (inPeriod.isEmpty()) item { Muted(stringResource(R.string.no_sessions), Modifier.padding(4.dp)) }
        items(inPeriod.take(200), key = { it.id }) { r ->
            val time = Instant.ofEpochMilli(r.startedAt).atZone(ZoneId.systemDefault())
            TCard(Modifier.fillMaxWidth(), padding = 12, onClick = { detail = r }) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(r.templateTitle.replace("∞", stringResource(R.string.unlimited)), modifier = Modifier.weight(1f))
                    Text("+${fmt(r.counted)}", style = Tnum)
                }
                Muted(
                    time.format(DateTimeFormatter.ofLocalizedDateTime(FormatStyle.SHORT)) + " · " +
                        stringResource(R.string.minutes, (r.activeMs / 60_000).toInt()),
                    size = 12,
                )
            }
        }
        item { Box(Modifier.height(16.dp)) }
    }

    detail?.let { r ->
        val zone = ZoneId.systemDefault()
        val f = DateTimeFormatter.ofLocalizedDateTime(FormatStyle.SHORT)
        AlertDialog(
            onDismissRequest = { detail = null },
            title = { Text(r.templateTitle.replace("∞", stringResource(R.string.unlimited))) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(stringResource(R.string.detail_time) + ": " + Instant.ofEpochMilli(r.startedAt).atZone(zone).format(f))
                    Text(stringResource(R.string.detail_duration) + ": " + stringResource(R.string.minutes, (r.activeMs / 60_000).toInt()))
                    Text(stringResource(R.string.detail_counted) + ": +" + fmt(r.counted) + " (${fmt(r.startCount)} → ${fmt(r.endCount)})")
                    if (r.reached) Text(stringResource(R.string.target_reached) + " ✓", color = Gold)
                    Text(stringResource(R.string.detail_sources) + ": " + stringResource(R.string.src_line, r.fromTap.toInt(), r.fromVolume.toInt(), r.fromPower.toInt()))
                }
            },
            confirmButton = { TextButton(onClick = { detail = null }) { Text(stringResource(R.string.ok)) } },
        )
    }
}

@Composable
private fun MiniStat(label: String, value: String, modifier: Modifier, sub: String? = null) {
    TCard(modifier, padding = 12) {
        Muted(label, size = 11)
        Text(value, fontSize = 20.sp, fontWeight = FontWeight.Medium, style = Tnum, maxLines = 1)
        Muted(sub ?: "", size = 11)
    }
}

/** So'nggi 53 hafta: ustun — hafta (Du..Ya), rang — kunlik son (PLAN 11.4). */
@Composable
private fun YearMap(stats: Stats, today: LocalDate) {
    val start = today.minusWeeks(52).with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))
    val weeks = (ChronoUnit.DAYS.between(start, today) / 7 + 1).toInt()
    val max = stats.byDay.filterKeys { it >= start }.values.maxOrNull()?.coerceAtLeast(1) ?: 1
    val c = MaterialTheme.colorScheme
    fun color(v: Long) = if (v == 0L) c.surfaceContainerHighest else c.primary.copy(alpha = 0.3f + 0.7f * v / max)
    TCard(Modifier.fillMaxWidth()) {
        Text(stringResource(R.string.year_map), fontWeight = FontWeight.Medium, modifier = Modifier.padding(bottom = 10.dp))
        // boshlanishda o'ng chetga (bugun) surilgan
        Row(Modifier.horizontalScroll(rememberScrollState(Int.MAX_VALUE)), horizontalArrangement = Arrangement.spacedBy(3.dp)) {
            repeat(weeks) { w ->
                Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
                    repeat(7) { d ->
                        val day = start.plusDays(w * 7L + d)
                        Box(
                            Modifier.size(12.dp).clip(RoundedCornerShape(3.dp))
                                .background(if (day.isAfter(today)) Color.Transparent else color(stats.byDay[day] ?: 0)),
                        )
                    }
                }
            }
        }
        Row(Modifier.fillMaxWidth().padding(top = 8.dp), horizontalArrangement = Arrangement.End, verticalAlignment = Alignment.CenterVertically) {
            Muted(stringResource(R.string.less), size = 11)
            listOf(0L, max / 4 + 1, max / 2 + 1, max).forEach { v ->
                Box(Modifier.padding(horizontal = 2.dp).size(10.dp).clip(RoundedCornerShape(2.dp)).background(color(v)))
            }
            Muted(stringResource(R.string.more), size = 11)
        }
    }
}

@Composable
private fun BarChart(bars: List<Pair<String, Long>>) {
    val max = bars.maxOf { it.second }.coerceAtLeast(1)
    TCard(Modifier.fillMaxWidth()) {
        Row(Modifier.fillMaxWidth().height(120.dp), horizontalArrangement = Arrangement.spacedBy(4.dp), verticalAlignment = Alignment.Bottom) {
            bars.forEachIndexed { i, (_, v) ->
                val h by animateFloatAsState((v.toFloat() / max).coerceAtLeast(0.02f), tween(400), label = "bar")
                Box(Modifier.weight(1f).fillMaxHeight(), contentAlignment = Alignment.BottomCenter) {
                    Box(
                        Modifier
                            .fillMaxWidth()
                            .fillMaxHeight(h)
                            .clip(RoundedCornerShape(topStart = 5.dp, topEnd = 5.dp, bottomStart = 2.dp, bottomEnd = 2.dp))
                            .background(if (i == bars.lastIndex) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.primaryContainer),
                    )
                }
            }
        }
        Row(Modifier.fillMaxWidth().padding(top = 6.dp), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            bars.forEach { (label, _) ->
                Text(label, fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, textAlign = TextAlign.Center, modifier = Modifier.weight(1f), maxLines = 1, softWrap = false)
            }
        }
    }
}
