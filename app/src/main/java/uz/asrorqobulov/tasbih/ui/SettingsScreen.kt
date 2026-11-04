package uz.asrorqobulov.tasbih.ui

import androidx.compose.material3.rememberTimePickerState
import androidx.compose.material3.TimePicker
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.compose.rememberLauncherForActivityResult
import android.Manifest
import android.app.LocaleManager
import android.os.Build
import android.os.LocaleList
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.DragIndicator
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import kotlinx.coroutines.launch
import uz.asrorqobulov.tasbih.R
import uz.asrorqobulov.tasbih.TasbihApp
import uz.asrorqobulov.tasbih.data.Accent
import uz.asrorqobulov.tasbih.data.AppSettings
import uz.asrorqobulov.tasbih.data.CountMode
import uz.asrorqobulov.tasbih.data.Template
import uz.asrorqobulov.tasbih.data.ThemeMode
import uz.asrorqobulov.tasbih.data.VibStrength
import kotlin.math.roundToInt

@Composable
fun SettingsScreen(app: TasbihApp, s: AppSettings, onTemplates: () -> Unit, onBackup: () -> Unit, onPermissions: () -> Unit) {
    val scope = rememberCoroutineScope()
    val ctx = LocalContext.current
    fun set(f: (AppSettings) -> AppSettings) = scope.launch { app.settings.update(f) }
    var dialog by remember { mutableStateOf<String?>(null) }
    val templates by app.dao.templates().collectAsState(initial = emptyList())
    val notifPermission = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) {}

    Column(Modifier.fillMaxSize().statusBarsPadding().verticalScroll(rememberScrollState()).padding(horizontal = 16.dp)) {
        Text(stringResource(R.string.nav_settings), fontSize = 26.sp, fontWeight = FontWeight.SemiBold, modifier = Modifier.padding(top = 8.dp, bottom = 10.dp))
        TCard(Modifier.fillMaxWidth(), padding = 4) {
            Box(Modifier.padding(horizontal = 10.dp)) { NavRow(stringResource(R.string.templates), "${templates.size}", arrow = true, onClick = onTemplates) }
        }

        SectionHeader(stringResource(R.string.sec_counting))
        Group {
            SwitchRow(stringResource(R.string.vibrate_each), s.vibrateEach) { v -> set { it.copy(vibrateEach = v) } }
            RowDivider()
            NavRow(stringResource(R.string.vib_strength), strengthLabel(s.vibStrength)) { dialog = "strength" }
            RowDivider()
            NavRow(stringResource(R.string.interval), intervalLabel(s.intervalStep)) { dialog = "interval" }
        }

        SectionHeader(stringResource(R.string.sec_goal))
        Group {
            NavRow(stringResource(R.string.daily_goal), goalLabel(s.dailyGoal)) { dialog = "goal" }
            RowDivider()
            NavRow(
                stringResource(R.string.reminder),
                if (s.reminderMin < 0) stringResource(R.string.off) else "%02d:%02d".format(s.reminderMin / 60, s.reminderMin % 60),
            ) { dialog = "reminder" }
        }

        SectionHeader(stringResource(R.string.sec_buttons))
        Group {
            SwitchRow(stringResource(R.string.edge_button), s.edgeButton, sub = stringResource(R.string.edge_button_desc)) { v -> set { it.copy(edgeButton = v) } }
            RowDivider()
            NavRow(stringResource(R.string.combos), arrow = true) { dialog = "combos" }
        }

        SectionHeader(stringResource(R.string.sec_protection))
        Group {
            SwitchRow(stringResource(R.string.proximity), s.proximity) { v -> set { it.copy(proximity = v) } }
            RowDivider()
            SwitchRow(stringResource(R.string.show_when_locked), s.showWhenLocked) { v -> set { it.copy(showWhenLocked = v) } }
        }

        SectionHeader(stringResource(R.string.sec_notifications))
        Group { SwitchRow(stringResource(R.string.dnd), s.dnd, sub = stringResource(R.string.dnd_desc)) { v -> set { it.copy(dnd = v) } } }

        SectionHeader(stringResource(R.string.sec_look))
        Group {
            NavRow(stringResource(R.string.theme), themeLabel(s.theme)) { dialog = "theme" }
            RowDivider()
            Row(Modifier.fillMaxWidth().padding(vertical = 12.dp), verticalAlignment = Alignment.CenterVertically) {
                Text(stringResource(R.string.accent), fontSize = 15.sp, modifier = Modifier.weight(1f))
                Accent.entries.forEach { a ->
                    val c = when (a) {
                        Accent.EMERALD -> Color(0xFF3DBB8E); Accent.GOLD -> Color(0xFFD4B25A)
                        Accent.BLUE -> Color(0xFF5B9BE0); Accent.PURPLE -> Color(0xFF9C84E0)
                        Accent.SYSTEM -> MaterialTheme.colorScheme.surfaceContainerHighest
                    }
                    Box(
                        Modifier
                            .padding(start = 8.dp)
                            .size(26.dp)
                            .border(2.dp, if (s.accent == a) MaterialTheme.colorScheme.onSurface else Color.Transparent, CircleShape)
                            .padding(4.dp)
                            .background(c, CircleShape)
                            .clickable { set { it.copy(accent = a) } },
                        contentAlignment = Alignment.Center,
                    ) { if (a == Accent.SYSTEM) Text("A", fontSize = 10.sp) }
                }
            }
            if (Build.VERSION.SDK_INT >= 33) {
                RowDivider()
                val lm = ctx.getSystemService(LocaleManager::class.java)
                val en = lm.applicationLocales.toLanguageTags().startsWith("en")
                NavRow(stringResource(R.string.language), if (en) "English" else "O'zbekcha") { dialog = "lang" }
            }
        }

        SectionHeader(stringResource(R.string.sec_data))
        Group {
            NavRow(stringResource(R.string.backup), arrow = true, onClick = onBackup)
            RowDivider()
            NavRow(stringResource(R.string.clear_history), color = MaterialTheme.colorScheme.error) { dialog = "clear" }
        }

        SectionHeader(stringResource(R.string.sec_permissions))
        Group { NavRow(stringResource(R.string.permissions_screen), arrow = true, onClick = onPermissions) }

        SectionHeader(stringResource(R.string.about))
        val version = remember { runCatching { ctx.packageManager.getPackageInfo(ctx.packageName, 0).versionName }.getOrNull() ?: "" }
        Group { NavRow(stringResource(R.string.version, version)) {} }
        Spacer(Modifier.height(24.dp))
    }

    when (dialog) {
        "strength" -> Choice(stringResource(R.string.vib_strength), VibStrength.entries.map { it to strengthLabel(it) }, s.vibStrength, { dialog = null }) { v -> set { it.copy(vibStrength = v) } }
        "theme" -> Choice(stringResource(R.string.theme), ThemeMode.entries.map { it to themeLabel(it) }, s.theme, { dialog = null }) { v -> set { it.copy(theme = v) } }
        "interval" -> NumberDialog(stringResource(R.string.interval), listOf(0, 33), s.intervalStep, { intervalLabel(it) }, { dialog = null }) { v -> set { it.copy(intervalStep = v) } }
        "goal" -> NumberDialog(stringResource(R.string.daily_goal), listOf(0, 100, 300, 500, 1000), s.dailyGoal, { goalLabel(it) }, { dialog = null }) { v -> set { it.copy(dailyGoal = v) } }
        "reminder" -> ReminderDialog(s.reminderMin, { dialog = null }) { v ->
            if (v >= 0 && Build.VERSION.SDK_INT >= 33) notifPermission.launch(Manifest.permission.POST_NOTIFICATIONS)
            set { it.copy(reminderMin = v) }
        }
        "lang" -> if (Build.VERSION.SDK_INT >= 33) {
            val lm = ctx.getSystemService(LocaleManager::class.java)
            val cur = if (lm.applicationLocales.toLanguageTags().startsWith("en")) "en" else "uz"
            Choice(stringResource(R.string.language), listOf("uz" to "O'zbekcha", "en" to "English"), cur, { dialog = null }) {
                lm.applicationLocales = LocaleList.forLanguageTags(it)
            }
        }
        "combos" -> AlertDialog(
            onDismissRequest = { dialog = null }, title = { Text(stringResource(R.string.combos)) },
            text = { Hints() }, confirmButton = { TextButton(onClick = { dialog = null }) { Text(stringResource(R.string.ok)) } },
        )
        "clear" -> ConfirmDialog(
            stringResource(R.string.clear_history), stringResource(R.string.clear_history_text), stringResource(R.string.delete), danger = true,
            onConfirm = { scope.launch { app.dao.clearSessions() } }, onDismiss = { dialog = null },
        )
    }
}

@Composable
private fun Group(content: @Composable () -> Unit) {
    TCard(Modifier.fillMaxWidth(), padding = 4) { Column(Modifier.padding(horizontal = 10.dp)) { content() } }
}

@Composable private fun strengthLabel(v: VibStrength) = stringResource(
    when (v) { VibStrength.LOW -> R.string.strength_low; VibStrength.MEDIUM -> R.string.strength_medium; VibStrength.HIGH -> R.string.strength_high },
)

@Composable private fun themeLabel(v: ThemeMode) = stringResource(
    when (v) { ThemeMode.DARK -> R.string.theme_dark; ThemeMode.LIGHT -> R.string.theme_light; ThemeMode.SYSTEM -> R.string.theme_system },
)

@Composable private fun intervalLabel(v: Int) =
    if (v == 0) stringResource(R.string.interval_off) else stringResource(R.string.interval_every, v)

@Composable private fun goalLabel(v: Int) = if (v == 0) stringResource(R.string.off) else fmt(v.toLong())

@Composable
private fun <T> Choice(title: String, options: List<Pair<T, String>>, selected: T, onDismiss: () -> Unit, onSelect: (T) -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            Column {
                options.forEach { (v, label) ->
                    Row(
                        Modifier.fillMaxWidth().clickable { onSelect(v); onDismiss() }.padding(vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        RadioButton(selected = v == selected, onClick = { onSelect(v); onDismiss() })
                        Text(label)
                    }
                }
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.cancel)) } },
    )
}

/** Tayyor variantlar + o'z soni (oraliq signal, kunlik maqsad). */
@Composable
private fun NumberDialog(title: String, presets: List<Int>, current: Int, label: @Composable (Int) -> String, onDismiss: () -> Unit, onSet: (Int) -> Unit) {
    var custom by remember { mutableStateOf(if (current !in presets) "$current" else "") }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            Column {
                presets.forEach { v ->
                    Row(Modifier.fillMaxWidth().clickable { onSet(v); onDismiss() }, verticalAlignment = Alignment.CenterVertically) {
                        RadioButton(selected = current == v, onClick = { onSet(v); onDismiss() })
                        Text(label(v))
                    }
                }
                OutlinedTextField(
                    custom, { custom = it.filter(Char::isDigit).take(5) }, singleLine = true,
                    placeholder = { Text(stringResource(R.string.custom_number)) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.padding(top = 8.dp),
                )
            }
        },
        confirmButton = {
            TextButton(onClick = { custom.toIntOrNull()?.takeIf { it > 0 }?.let(onSet); onDismiss() }) { Text(stringResource(R.string.save)) }
        },
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ReminderDialog(current: Int, onDismiss: () -> Unit, onSet: (Int) -> Unit) {
    val start = if (current >= 0) current else 21 * 60
    val state = rememberTimePickerState(start / 60, start % 60, is24Hour = true)
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.reminder)) },
        text = {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Muted(stringResource(R.string.reminder_desc), Modifier.padding(bottom = 12.dp))
                TimePicker(state)
            }
        },
        confirmButton = {
            TextButton(onClick = { onSet(state.hour * 60 + state.minute); onDismiss() }) { Text(stringResource(R.string.save)) }
        },
        dismissButton = {
            if (current >= 0) TextButton(onClick = { onSet(-1); onDismiss() }) { Text(stringResource(R.string.turn_off), color = MaterialTheme.colorScheme.error) }
            else TextButton(onClick = onDismiss) { Text(stringResource(R.string.cancel)) }
        },
    )
}

/** Sozlamalar → Shablonlar: sudrab tartiblash, tahrirlash, o'chirish (PLAN 8.4). */
@Composable
fun TemplatesScreen(app: TasbihApp, onBack: () -> Unit, onEdit: (String?) -> Unit) {
    val scope = rememberCoroutineScope()
    val source by app.dao.templates().collectAsState(initial = emptyList())
    val items = remember { mutableStateListOf<Template>() }
    var dragging by remember { mutableIntStateOf(-1) }
    var offset by remember { mutableFloatStateOf(0f) }
    var rowH by remember { mutableIntStateOf(1) }
    var toDelete by remember { mutableStateOf<Template?>(null) }
    LaunchedEffect(source) { if (dragging < 0) { items.clear(); items.addAll(source) } }

    Box(Modifier.fillMaxSize().statusBarsPadding()) {
        Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(vertical = 8.dp)) {
                IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Outlined.ArrowBack, null) }
                Text(stringResource(R.string.templates), fontSize = 20.sp, fontWeight = FontWeight.SemiBold)
            }
            Muted(stringResource(R.string.templates_hint), Modifier.padding(bottom = 12.dp, start = 4.dp))
            TCard(Modifier.fillMaxWidth(), padding = 4) {
                items.forEachIndexed { i, t -> key(t.id) {
                    val isDrag = i == dragging
                    Row(
                        Modifier
                            .zIndex(if (isDrag) 1f else 0f)
                            .offset { IntOffset(0, if (isDrag) offset.roundToInt() else 0) }
                            .background(if (isDrag) MaterialTheme.colorScheme.surfaceContainerHigh else Color.Transparent)
                            .onSizeChanged { rowH = it.height.coerceAtLeast(1) }
                            .padding(horizontal = 6.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Icon(
                            Icons.Outlined.DragIndicator, null, tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(end = 8.dp).pointerInput(t.id) {
                                detectDragGestures(
                                    onDragStart = { dragging = items.indexOfFirst { it.id == t.id }; offset = 0f },
                                    onDragEnd = {
                                        dragging = -1; offset = 0f
                                        val ordered = items.mapIndexed { idx, x -> x.copy(position = idx) }
                                        scope.launch { app.dao.upsertTemplates(ordered) }
                                    },
                                    onDragCancel = { dragging = -1; offset = 0f },
                                ) { change, d ->
                                    change.consume()
                                    offset += d.y
                                    val idx = dragging
                                    if (offset > rowH / 2f && idx < items.lastIndex) {
                                        items.add(idx + 1, items.removeAt(idx)); dragging = idx + 1; offset -= rowH
                                    } else if (offset < -rowH / 2f && idx > 0) {
                                        items.add(idx - 1, items.removeAt(idx)); dragging = idx - 1; offset += rowH
                                    }
                                }
                            },
                        )
                        Column(Modifier.weight(1f)) {
                            Text(templateTitle(t), fontWeight = FontWeight.Medium)
                            Muted(
                                when {
                                    t.partSize == 0 -> stringResource(R.string.target_info_unlimited)
                                    t.parts > 1 -> stringResource(R.string.target_info_parts, t.target.toInt(), t.parts)
                                    else -> stringResource(R.string.target_info_single, t.target.toInt())
                                } + " · " + stringResource(if (t.mode == CountMode.TAP) R.string.mode_tap else R.string.mode_device),
                                size = 12,
                            )
                        }
                        IconButton(onClick = { onEdit(t.id) }) { Icon(Icons.Outlined.Edit, stringResource(R.string.edit)) }
                        IconButton(onClick = { toDelete = t }) { Icon(Icons.Outlined.Delete, stringResource(R.string.delete)) }
                    }
                } }
            }
            Spacer(Modifier.height(96.dp))
        }
        FloatingActionButton(
            onClick = { onEdit(null) }, containerColor = MaterialTheme.colorScheme.primary,
            modifier = Modifier.align(Alignment.BottomEnd).padding(16.dp),
        ) { Icon(Icons.Outlined.Add, stringResource(R.string.new_template)) }
    }
    toDelete?.let { t ->
        ConfirmDialog(
            stringResource(R.string.delete_title), stringResource(R.string.delete_text, templateTitle(t)), stringResource(R.string.delete), danger = true,
            onConfirm = { scope.launch { app.dao.deleteTemplate(t.id) } }, onDismiss = { toDelete = null },
        )
    }
}
