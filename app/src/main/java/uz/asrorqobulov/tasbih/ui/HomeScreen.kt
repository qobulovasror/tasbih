package uz.asrorqobulov.tasbih.ui

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.LocalFireDepartment
import androidx.compose.material.icons.outlined.PanTool
import androidx.compose.material.icons.outlined.PhoneAndroid
import androidx.compose.material.icons.outlined.PlayArrow
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material.icons.automirrored.outlined.TrendingDown
import androidx.compose.material.icons.automirrored.outlined.TrendingUp
import androidx.compose.material.icons.outlined.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.LifecycleResumeEffect
import kotlinx.coroutines.launch
import uz.asrorqobulov.tasbih.R
import uz.asrorqobulov.tasbih.TasbihApp
import uz.asrorqobulov.tasbih.data.AppSettings
import uz.asrorqobulov.tasbih.data.CountMode
import uz.asrorqobulov.tasbih.data.Stats
import uz.asrorqobulov.tasbih.data.TargetSignal
import uz.asrorqobulov.tasbih.data.Template
import uz.asrorqobulov.tasbih.service.CounterAccessibilityService
import java.time.LocalDate

@Composable
fun HomeScreen(app: TasbihApp, s: AppSettings, onEdit: (String?) -> Unit, onStats: () -> Unit, onPermissions: () -> Unit) {
    val templates by app.dao.templates().collectAsState(initial = emptyList())
    val sessions by app.dao.sessions().collectAsState(initial = emptyList())
    val ctx = LocalContext.current
    var a11y by remember { mutableStateOf(true) }
    LifecycleResumeEffect(Unit) {
        a11y = CounterAccessibilityService.isEnabled(ctx)
        onPauseOrDispose { }
    }
    var prepareId by rememberSaveable { mutableStateOf<String?>(null) }
    val quick by app.quickStart.collectAsState()
    LaunchedEffect(quick) {
        if (!quick) return@LaunchedEffect
        val ts = app.dao.templatesOnce()
        val last = app.dao.sessionsOnce().lastOrNull()?.templateId
        prepareId = (ts.firstOrNull { it.id == last } ?: ts.firstOrNull())?.id
        app.quickStart.value = false
    }

    val stats = remember(sessions) { Stats(sessions) }
    val today = LocalDate.now()
    val todayTotal = stats.total(today, today)
    val yesterday = stats.total(today.minusDays(1), today.minusDays(1))

    Box(Modifier.fillMaxSize()) {
        LazyVerticalGrid(
            columns = GridCells.Fixed(2),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 96.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
            modifier = Modifier.statusBarsPadding(),
        ) {
            item(span = { GridItemSpan(2) }) {
                Text(stringResource(R.string.app_name), fontSize = 26.sp, fontWeight = FontWeight.SemiBold, modifier = Modifier.padding(top = 8.dp, bottom = 6.dp))
            }
            if (!a11y && templates.any { it.mode == CountMode.DEVICE }) {
                item(span = { GridItemSpan(2) }) {
                    Row(
                        Modifier
                            .clip(RoundedCornerShape(14.dp))
                            .background(MaterialTheme.colorScheme.tertiary.copy(alpha = 0.18f))
                            .clickable(onClick = onPermissions)
                            .padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Icon(Icons.Outlined.Warning, null, tint = MaterialTheme.colorScheme.tertiary)
                        Text(stringResource(R.string.a11y_banner), fontSize = 13.sp, modifier = Modifier.padding(start = 10.dp).weight(1f))
                        Text(stringResource(R.string.setup), color = MaterialTheme.colorScheme.tertiary, fontSize = 13.sp)
                    }
                }
            }
            item(span = { GridItemSpan(2) }) {
                TodayCard(todayTotal, yesterday, stats.streak(today), s.dailyGoal, onStats)
            }
            items(templates, key = { it.id }) { t ->
                TemplateCard(app, t, onClick = { prepareId = t.id }, onEdit = { onEdit(t.id) })
            }
        }
        FloatingActionButton(
            onClick = { onEdit(null) },
            containerColor = MaterialTheme.colorScheme.primary,
            modifier = Modifier.align(Alignment.BottomEnd).padding(16.dp),
        ) { Icon(Icons.Outlined.Add, stringResource(R.string.new_template)) }
    }

    templates.firstOrNull { it.id == prepareId }?.let { t ->
        PrepareSheet(app, t, s, a11y, onDismiss = { prepareId = null }, onPermissions = { prepareId = null; onPermissions() })
    }
}

@Composable
private fun TodayCard(today: Long, yesterday: Long, streak: Int, goal: Int, onClick: () -> Unit) {
    TCard(Modifier.fillMaxWidth(), padding = 16, onClick = onClick) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Muted(stringResource(R.string.today))
                Text(fmt(today), fontSize = 32.sp, fontWeight = FontWeight.Medium, style = Tnum)
                val diff = today - yesterday
                val (icon, text, color) = when {
                    diff > 0 -> Triple(Icons.AutoMirrored.Outlined.TrendingUp, stringResource(R.string.vs_yesterday_more, fmt(diff)), MaterialTheme.colorScheme.primary)
                    diff < 0 -> Triple(Icons.AutoMirrored.Outlined.TrendingDown, stringResource(R.string.vs_yesterday_less, fmt(-diff)), MaterialTheme.colorScheme.onSurfaceVariant)
                    else -> Triple(null, stringResource(R.string.vs_yesterday_equal), MaterialTheme.colorScheme.onSurfaceVariant)
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (icon != null) Icon(icon, null, tint = color, modifier = Modifier.height(16.dp).padding(end = 4.dp))
                    Text(text, fontSize = 13.sp, color = color)
                }
            }
            Box(Modifier.width(1.dp).height(64.dp).background(MaterialTheme.colorScheme.outlineVariant))
            Column(Modifier.padding(start = 16.dp, end = 4.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                Icon(Icons.Outlined.LocalFireDepartment, null, tint = Gold)
                Text("$streak", fontSize = 22.sp, fontWeight = FontWeight.Medium)
                Muted(stringResource(R.string.days), size = 12)
            }
        }
        if (goal > 0) {
            Row(Modifier.padding(top = 12.dp), verticalAlignment = Alignment.CenterVertically) {
                LinearProgressIndicator(
                    progress = { (today.toFloat() / goal).coerceAtMost(1f) },
                    modifier = Modifier.weight(1f).height(6.dp),
                    color = if (today >= goal) Gold else MaterialTheme.colorScheme.primary,
                    trackColor = MaterialTheme.colorScheme.surfaceContainerHighest, drawStopIndicator = {},
                )
                Muted(
                    if (today >= goal) "✓ " + stringResource(R.string.goal_done) else "${fmt(today)} / ${fmt(goal.toLong())}",
                    Modifier.padding(start = 10.dp), size = 12,
                )
            }
        }
    }
}

/** "2-qism · 8/33" yoki "/ 33" (PLAN 8.4). */
@Composable
fun templateSub(t: Template, count: Long = t.count): String = when {
    t.partSize == 0 -> stringResource(R.string.preview_unlimited)
    t.parts > 1 -> {
        val done = count >= t.target
        val part = if (done) t.parts else (count / t.partSize).toInt() + 1
        val inPart = if (done) t.partSize.toLong() else count % t.partSize
        stringResource(R.string.part_progress, part, inPart.toInt(), t.partSize)
    }
    else -> "/ ${fmt(t.target)}"
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun TemplateCard(app: TasbihApp, t: Template, onClick: () -> Unit, onEdit: () -> Unit) {
    var menu by remember { mutableStateOf(false) }
    var confirmDelete by remember { mutableStateOf(false) }
    var confirmReset by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    Box {
        TCard(
            Modifier
                .fillMaxWidth()
                .heightIn(min = 108.dp)
                .clip(RoundedCornerShape(16.dp))
                .combinedClickable(onClick = onClick, onLongClick = { menu = true }),
            padding = 12,
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(templateTitle(t), fontSize = 16.sp, fontWeight = FontWeight.Medium, modifier = Modifier.weight(1f), maxLines = 1)
                Icon(
                    if (t.mode == CountMode.TAP) Icons.Outlined.PanTool else Icons.Outlined.PhoneAndroid, null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.height(16.dp),
                )
            }
            Spacer(Modifier.height(10.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(fmt(t.count), fontSize = 28.sp, fontWeight = FontWeight.Medium, style = Tnum)
                if (t.target > 0 && t.count >= t.target) Icon(Icons.Outlined.Check, null, tint = Gold, modifier = Modifier.padding(start = 6.dp))
            }
            Muted(templateSub(t))
        }
        DropdownMenu(expanded = menu, onDismissRequest = { menu = false }) {
            DropdownMenuItem(text = { Text(stringResource(R.string.edit)) }, onClick = { menu = false; onEdit() })
            DropdownMenuItem(text = { Text(stringResource(R.string.reset_count)) }, onClick = { menu = false; confirmReset = true })
            DropdownMenuItem(
                text = { Text(stringResource(R.string.delete), color = MaterialTheme.colorScheme.error) },
                onClick = { menu = false; confirmDelete = true },
            )
        }
    }
    val title = templateTitle(t)
    if (confirmDelete) {
        ConfirmDialog(
            stringResource(R.string.delete_title), stringResource(R.string.delete_text, title), stringResource(R.string.delete), danger = true,
            onConfirm = { scope.launch { app.dao.deleteTemplate(t.id) } }, onDismiss = { confirmDelete = false },
        )
    }
    if (confirmReset) {
        ConfirmDialog(
            stringResource(R.string.reset_title), stringResource(R.string.reset_text), stringResource(R.string.reset),
            onConfirm = { scope.launch { app.dao.setCount(t.id, 0) } }, onDismiss = { confirmReset = false },
        )
    }
}

/** Tayyorlanish varag'i (PLAN 8.4): rejim va signal — shablonda, himoya/tugmalar — umumiy. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun PrepareSheet(
    app: TasbihApp, t: Template, s: AppSettings, a11y: Boolean,
    onDismiss: () -> Unit, onPermissions: () -> Unit,
) {
    val scope = rememberCoroutineScope()
    var confirmReset by remember { mutableStateOf(false) }
    val sheet = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    fun saveT(n: Template) = scope.launch { app.dao.upsert(n) }
    fun saveS(f: (AppSettings) -> AppSettings) = scope.launch { app.settings.update(f) }

    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = sheet, containerColor = MaterialTheme.colorScheme.surfaceContainer) {
        Column(Modifier.padding(horizontal = 18.dp).navigationBarsPadding().padding(bottom = 16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(templateTitle(t), fontSize = 20.sp, fontWeight = FontWeight.SemiBold, modifier = Modifier.weight(1f))
                if (t.target > 0) Muted(stringResource(R.string.target_n, fmt(t.target)))
            }
            Row(Modifier.padding(vertical = 12.dp), verticalAlignment = Alignment.Bottom) {
                Column(Modifier.weight(1f)) {
                    Muted(stringResource(R.string.current_count))
                    Text(fmt(t.count), fontSize = 42.sp, fontWeight = FontWeight.Medium, style = Tnum)
                }
                OutlinedButton(onClick = { confirmReset = true }) {
                    Icon(Icons.Outlined.Refresh, null, modifier = Modifier.height(18.dp))
                    Text(" " + stringResource(R.string.reset))
                }
            }
            Muted(stringResource(R.string.count_mode), Modifier.padding(bottom = 6.dp))
            Seg(
                listOf(CountMode.DEVICE to stringResource(R.string.mode_device), CountMode.TAP to stringResource(R.string.mode_tap)),
                t.mode, { saveT(t.copy(mode = it)) },
            )
            if (t.mode == CountMode.DEVICE) {
                Spacer(Modifier.height(6.dp))
                SwitchRow(stringResource(R.string.protection), s.protection) { v -> saveS { it.copy(protection = v) } }
                val onlyOne = s.volumeCount != s.powerCount
                SwitchRow(stringResource(R.string.volume_count), s.volumeCount, enabled = !(onlyOne && s.volumeCount)) { v -> saveS { it.copy(volumeCount = v) } }
                SwitchRow(stringResource(R.string.power_count), s.powerCount, enabled = !(onlyOne && s.powerCount)) { v -> saveS { it.copy(powerCount = v) } }
                if (onlyOne) Text(stringResource(R.string.one_button_needed), color = MaterialTheme.colorScheme.tertiary, fontSize = 12.sp)
                Hints(Modifier.padding(top = 8.dp))
            }
            if (t.partSize > 0) {
                Muted(stringResource(R.string.target_signal), Modifier.padding(top = 14.dp, bottom = 6.dp))
                Seg(
                    listOf(
                        TargetSignal.VIBRATION to stringResource(R.string.sig_vibration),
                        TargetSignal.SOUND to stringResource(R.string.sig_sound),
                        TargetSignal.BOTH to stringResource(R.string.sig_both),
                    ),
                    t.signal, { saveT(t.copy(signal = it)) },
                )
            }
            val blocked = t.mode == CountMode.DEVICE && !a11y
            if (blocked) {
                Text(
                    stringResource(R.string.needs_a11y), color = MaterialTheme.colorScheme.tertiary, fontSize = 13.sp,
                    modifier = Modifier.padding(top = 12.dp).clickable(onClick = onPermissions),
                )
            }
            Button(
                onClick = {
                    if (blocked) onPermissions() else {
                        app.session.start(t, t.mode, s.protection, s.volumeCount, s.powerCount)
                        onDismiss()
                    }
                },
                modifier = Modifier.fillMaxWidth().padding(top = 16.dp).height(56.dp),
                shape = RoundedCornerShape(16.dp),
            ) {
                Icon(Icons.Outlined.PlayArrow, null)
                Text(" " + stringResource(if (blocked) R.string.setup else R.string.start), fontSize = 16.sp)
            }
        }
    }
    if (confirmReset) {
        ConfirmDialog(
            stringResource(R.string.reset_title), stringResource(R.string.reset_text), stringResource(R.string.reset),
            onConfirm = { scope.launch { app.dao.setCount(t.id, 0) } }, onDismiss = { confirmReset = false },
        )
    }
}

@Composable
fun Hints(modifier: Modifier = Modifier) {
    Column(
        modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(MaterialTheme.colorScheme.surfaceContainerHigh)
            .padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Muted(stringResource(R.string.hint_pause), size = 12)
        Muted(stringResource(R.string.hint_end), size = 12)
        Muted(stringResource(R.string.hint_unlock), size = 12)
    }
}
