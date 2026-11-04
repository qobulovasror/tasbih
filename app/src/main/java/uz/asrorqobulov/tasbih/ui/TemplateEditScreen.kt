package uz.asrorqobulov.tasbih.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material3.Button
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.launch
import uz.asrorqobulov.tasbih.R
import uz.asrorqobulov.tasbih.TasbihApp
import uz.asrorqobulov.tasbih.data.CountMode
import uz.asrorqobulov.tasbih.data.TargetSignal
import uz.asrorqobulov.tasbih.data.Template

/** Yangi shablon / tahrirlash (PLAN 8.4). id bo'sh — yangi. */
@Composable
fun TemplateEditScreen(app: TasbihApp, id: String, onDone: () -> Unit) {
    val scope = rememberCoroutineScope()
    var base by remember { mutableStateOf<Template?>(null) }
    var name by remember { mutableStateOf("") }
    var size by remember { mutableIntStateOf(33) }
    var custom by remember { mutableStateOf("") }
    var parts by remember { mutableIntStateOf(1) }
    var mode by remember { mutableStateOf(CountMode.DEVICE) }
    var signal by remember { mutableStateOf(TargetSignal.VIBRATION) }
    var confirmDelete by remember { mutableStateOf(false) }
    val presets = listOf(33, 99, 100, 1000, 0)

    LaunchedEffect(id) {
        val t = if (id.isBlank()) null else app.dao.template(id)
        base = t
        if (t != null) {
            name = t.name; parts = t.parts; mode = t.mode; signal = t.signal
            if (t.partSize in presets) size = t.partSize else { size = -1; custom = "${t.partSize}" }
        }
    }

    val effSize = if (size == -1) custom.toIntOrNull()?.coerceIn(1, 1_000_000) ?: 0 else size
    val effParts = if (effSize == 0) 1 else parts
    val preview = Template(name = name.trim(), partSize = effSize, parts = effParts)

    Column(Modifier.fillMaxSize().statusBarsPadding().imePadding()) {
        Row(Modifier.padding(horizontal = 8.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onDone) { Icon(Icons.Outlined.Close, stringResource(R.string.cancel)) }
            Text(
                stringResource(if (base == null) R.string.new_template else R.string.edit_template),
                fontSize = 20.sp, fontWeight = FontWeight.SemiBold, modifier = Modifier.weight(1f),
            )
            if (base != null) IconButton(onClick = { confirmDelete = true }) {
                Icon(Icons.Outlined.Delete, stringResource(R.string.delete), tint = MaterialTheme.colorScheme.error)
            }
        }
        Column(Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(horizontal = 16.dp)) {
            TCard(Modifier.fillMaxWidth(), padding = 16) {
                Muted(stringResource(R.string.preview), Modifier.align(Alignment.CenterHorizontally))
                Text(templateTitle(preview), fontSize = 30.sp, fontWeight = FontWeight.Medium, modifier = Modifier.align(Alignment.CenterHorizontally))
                Muted(
                    when {
                        effSize == 0 -> stringResource(R.string.preview_unlimited)
                        effParts > 1 -> stringResource(R.string.preview_parts, preview.target.toInt(), effSize)
                        else -> stringResource(R.string.preview_single, effSize)
                    },
                    Modifier.align(Alignment.CenterHorizontally),
                )
            }
            Label(R.string.name_optional)
            OutlinedTextField(
                name, { name = it.take(30) }, singleLine = true, modifier = Modifier.fillMaxWidth(),
                placeholder = { Text(stringResource(R.string.name_hint)) }, shape = RoundedCornerShape(12.dp),
            )
            Label(R.string.part_size)
            Seg(presets.map { it to if (it == 0) "∞" else "$it" }, size, { size = it; custom = "" })
            OutlinedTextField(
                custom,
                { v -> custom = v.filter(Char::isDigit).take(7); size = if (custom.isEmpty()) 33 else -1 },
                singleLine = true, modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                placeholder = { Text(stringResource(R.string.custom_number)) }, shape = RoundedCornerShape(12.dp),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            )
            if (effSize > 0) {
                Label(R.string.parts_count)
                TCard(Modifier.fillMaxWidth(), padding = 8) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                        FilledTonalButton(onClick = { if (parts > 1) parts-- }) { Text("−", fontSize = 18.sp) }
                        Text("$parts", fontSize = 22.sp, fontWeight = FontWeight.Medium)
                        FilledTonalButton(onClick = { if (parts < 10) parts++ }) { Text("+", fontSize = 18.sp) }
                    }
                }
            }
            Label(R.string.count_mode)
            Seg(listOf(CountMode.DEVICE to stringResource(R.string.mode_device), CountMode.TAP to stringResource(R.string.mode_tap)), mode, { mode = it })
            if (effSize > 0) {
                Label(R.string.target_signal)
                Seg(
                    listOf(
                        TargetSignal.VIBRATION to stringResource(R.string.sig_vibration),
                        TargetSignal.SOUND to stringResource(R.string.sig_sound),
                        TargetSignal.BOTH to stringResource(R.string.sig_both),
                    ),
                    signal, { signal = it },
                )
            }
            Spacer(Modifier.height(16.dp))
        }
        Button(
            onClick = {
                scope.launch {
                    val pos = base?.position ?: ((app.dao.templatesOnce().maxOfOrNull { it.position } ?: -1) + 1)
                    val t = (base ?: Template(partSize = effSize)).copy(
                        name = name.trim(), partSize = effSize, parts = effParts, mode = mode, signal = signal, position = pos,
                    )
                    app.dao.upsert(t)
                    onDone()
                }
            },
            enabled = size != -1 || effSize > 0,
            modifier = Modifier.fillMaxWidth().navigationBarsPadding().padding(16.dp).height(52.dp),
            shape = RoundedCornerShape(16.dp),
        ) { Text(stringResource(R.string.save), fontSize = 16.sp) }
    }

    if (confirmDelete) base?.let { t ->
        ConfirmDialog(
            stringResource(R.string.delete_title), stringResource(R.string.delete_text, templateTitle(t)), stringResource(R.string.delete),
            danger = true,
            onConfirm = { scope.launch { app.dao.deleteTemplate(t.id); onDone() } },
            onDismiss = { confirmDelete = false },
        )
    }
}

@Composable
private fun Label(res: Int) = Muted(stringResource(res), Modifier.padding(top = 16.dp, bottom = 6.dp))
