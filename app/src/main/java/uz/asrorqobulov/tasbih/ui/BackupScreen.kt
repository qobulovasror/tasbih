package uz.asrorqobulov.tasbih.ui

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.Download
import androidx.compose.material.icons.outlined.Upload
import androidx.compose.material3.Button
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import uz.asrorqobulov.tasbih.R
import uz.asrorqobulov.tasbih.TasbihApp
import uz.asrorqobulov.tasbih.data.AppSettings
import uz.asrorqobulov.tasbih.data.Backup
import uz.asrorqobulov.tasbih.data.BackupFile
import java.io.File
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle

private suspend fun snapshot(app: TasbihApp) = BackupFile(
    exportedAt = System.currentTimeMillis(),
    templates = app.dao.templatesOnce(),
    sessions = app.dao.sessionsOnce(),
    settings = Backup.settingsToMap(app.settings.current()),
)

/** Eksport / Import (PLAN 8.4): Birlashtirish (default) yoki Almashtirish (avval avtomatik zaxira). */
@Composable
fun BackupScreen(app: TasbihApp, s: AppSettings, onBack: () -> Unit) {
    val ctx = LocalContext.current
    val scope = rememberCoroutineScope()
    var message by remember { mutableStateOf<String?>(null) }
    var pending by remember { mutableStateOf<BackupFile?>(null) }
    var pendingName by remember { mutableStateOf("") }
    var replace by remember { mutableStateOf(false) }
    val msgSaved = stringResource(R.string.export_done)
    val msgImported = stringResource(R.string.import_done)
    val msgBad = stringResource(R.string.bad_file)

    val exporter = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/json")) { uri: Uri? ->
        uri ?: return@rememberLauncherForActivityResult
        scope.launch {
            val ok = withContext(Dispatchers.IO) {
                runCatching {
                    ctx.contentResolver.openOutputStream(uri)!!.use { it.write(Backup.encode(snapshot(app)).toByteArray()) }
                }.isSuccess
            }
            if (ok) app.settings.update { it.copy(lastBackup = System.currentTimeMillis()) }
            message = if (ok) msgSaved else msgBad
        }
    }
    val importer = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri: Uri? ->
        uri ?: return@rememberLauncherForActivityResult
        scope.launch {
            pending = withContext(Dispatchers.IO) {
                runCatching { ctx.contentResolver.openInputStream(uri)!!.use { Backup.decode(it.readBytes().decodeToString()) } }.getOrNull()
            }
            pendingName = uri.lastPathSegment?.substringAfterLast('/') ?: ""
            message = if (pending == null) msgBad else null
        }
    }

    Column(Modifier.fillMaxSize().statusBarsPadding().verticalScroll(rememberScrollState()).padding(horizontal = 16.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(vertical = 8.dp)) {
            IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Outlined.ArrowBack, null) }
            Text(stringResource(R.string.backup), fontSize = 20.sp, fontWeight = FontWeight.SemiBold)
        }
        TCard(Modifier.fillMaxWidth()) {
            Muted(stringResource(R.string.last_backup))
            Text(
                if (s.lastBackup == 0L) stringResource(R.string.never)
                else Instant.ofEpochMilli(s.lastBackup).atZone(ZoneId.systemDefault()).format(DateTimeFormatter.ofLocalizedDateTime(FormatStyle.MEDIUM, FormatStyle.SHORT)),
            )
        }
        TCard(Modifier.fillMaxWidth().padding(top = 12.dp)) {
            Text(stringResource(R.string.export), fontWeight = FontWeight.Medium)
            Muted(stringResource(R.string.export_desc), Modifier.padding(vertical = 6.dp))
            FilledTonalButton(onClick = { exporter.launch("tasbih-backup-${LocalDate.now()}.json") }, modifier = Modifier.fillMaxWidth()) {
                Icon(Icons.Outlined.Download, null); Text(" " + stringResource(R.string.save_to_file))
            }
        }
        TCard(Modifier.fillMaxWidth().padding(top = 12.dp)) {
            Text(stringResource(R.string.import_title), fontWeight = FontWeight.Medium)
            val p = pending
            if (p == null) {
                FilledTonalButton(onClick = { importer.launch(arrayOf("application/json", "text/plain", "application/octet-stream")) }, modifier = Modifier.fillMaxWidth().padding(top = 8.dp)) {
                    Text(stringResource(R.string.choose_file))
                }
            } else {
                Column(
                    Modifier.fillMaxWidth().padding(vertical = 8.dp).clip(RoundedCornerShape(12.dp))
                        .background(MaterialTheme.colorScheme.surfaceContainerHigh).padding(12.dp),
                ) {
                    Text(pendingName, fontSize = 13.sp)
                    val since = p.sessions.minOfOrNull { it.startedAt }?.let {
                        stringResource(R.string.import_since, Instant.ofEpochMilli(it).atZone(ZoneId.systemDefault()).toLocalDate().toString())
                    } ?: ""
                    Muted(stringResource(R.string.import_preview, p.templates.size, p.sessions.size) + since, size = 12)
                }
                ModeRow(stringResource(R.string.merge), stringResource(R.string.merge_desc), !replace) { replace = false }
                ModeRow(stringResource(R.string.replace), stringResource(R.string.replace_desc), replace) { replace = true }
                if (replace) Text(stringResource(R.string.replace_warn), color = MaterialTheme.colorScheme.tertiary, fontSize = 12.sp)
                Button(
                    onClick = {
                        scope.launch {
                            withContext(Dispatchers.IO) { importBackup(app, p, replace) }
                            pending = null
                            message = msgImported
                        }
                    },
                    modifier = Modifier.fillMaxWidth().padding(top = 12.dp),
                ) { Icon(Icons.Outlined.Upload, null); Text(" " + stringResource(R.string.do_import)) }
            }
        }
        message?.let { Text(it, color = MaterialTheme.colorScheme.primary, modifier = Modifier.padding(16.dp)) }
    }
}

@Composable
private fun ModeRow(title: String, desc: String, selected: Boolean, onClick: () -> Unit) {
    Row(Modifier.fillMaxWidth().clickable(onClick = onClick), verticalAlignment = Alignment.CenterVertically) {
        RadioButton(selected, onClick)
        Text(title, modifier = Modifier.weight(1f))
        Muted(desc, size = 12)
    }
}

private suspend fun importBackup(app: TasbihApp, f: BackupFile, replace: Boolean) {
    if (replace) {
        // qaytarish imkoni uchun avval joriy holat zaxiraga
        val dir = File(app.getExternalFilesDir(null), "backups").apply { mkdirs() }
        File(dir, "auto-${System.currentTimeMillis()}.json").writeText(Backup.encode(snapshot(app)))
        app.dao.replaceAll(f.templates, f.sessions)
        app.settings.update { Backup.settingsFromMap(f.settings, it) }
    } else {
        val start = (app.dao.templatesOnce().maxOfOrNull { it.position } ?: -1) + 1
        app.dao.insertTemplatesIgnore(f.templates.mapIndexed { i, t -> t.copy(position = start + i) })
        app.dao.insertSessionsIgnore(f.sessions)
    }
}
