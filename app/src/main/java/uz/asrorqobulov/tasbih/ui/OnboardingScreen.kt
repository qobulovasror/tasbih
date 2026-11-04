package uz.asrorqobulov.tasbih.ui

import android.Manifest
import android.app.LocaleManager
import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.LocaleList
import android.os.PowerManager
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.ErrorOutline
import androidx.compose.material.icons.outlined.Pause
import androidx.compose.material.icons.outlined.PhoneAndroid
import androidx.compose.material.icons.outlined.Stop
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
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
import uz.asrorqobulov.tasbih.service.CounterAccessibilityService

/** Kirish — 4 qadam (PLAN 8.4). */
@Composable
fun OnboardingScreen(app: TasbihApp, s: AppSettings) {
    var step by rememberSaveable { mutableIntStateOf(0) }
    val scope = rememberCoroutineScope()
    val templates by app.dao.templates().collectAsState(initial = emptyList())
    var removed by rememberSaveable { mutableStateOf(setOf<String>()) }
    val finish: () -> Unit = {
        scope.launch {
            removed.forEach { app.dao.deleteTemplate(it) }
            app.settings.update { it.copy(onboardingDone = true) }
        }
    }

    Column(Modifier.fillMaxSize().statusBarsPadding().navigationBarsPadding()) {
        Row(Modifier.fillMaxWidth().padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Row(Modifier.weight(1f), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                repeat(4) { i ->
                    Box(
                        Modifier.height(6.dp).width(if (i == step) 18.dp else 6.dp).clip(RoundedCornerShape(3.dp))
                            .background(if (i == step) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceContainerHighest),
                    )
                }
            }
            TextButton(onClick = finish) { Text(stringResource(R.string.skip)) }
        }
        Column(Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(horizontal = 16.dp)) {
            when (step) {
                0 -> LanguageStep()
                1 -> HowStep()
                2 -> PermissionsContent(app, s, showTitle = true)
                else -> {
                    Title(R.string.ob_tpl_title)
                    Muted(stringResource(R.string.ob_tpl_desc), Modifier.padding(bottom = 12.dp))
                    TCard(Modifier.fillMaxWidth(), padding = 4) {
                        templates.forEach { t ->
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Checkbox(t.id !in removed, { keep -> removed = if (keep) removed - t.id else removed + t.id })
                                Text(templateTitle(t), fontSize = 16.sp, modifier = Modifier.weight(1f))
                                Muted(templateSub(t), Modifier.padding(end = 12.dp))
                            }
                        }
                    }
                }
            }
        }
        Row(Modifier.fillMaxWidth().padding(16.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            if (step > 0) OutlinedButton(onClick = { step-- }, modifier = Modifier.weight(1f).height(52.dp)) { Text(stringResource(R.string.back)) }
            Button(
                onClick = { if (step < 3) step++ else finish() },
                modifier = Modifier.weight(2f).height(52.dp), shape = RoundedCornerShape(16.dp),
            ) { Text(stringResource(if (step < 3) R.string.next else R.string.done)) }
        }
    }
}

@Composable
private fun Title(res: Int) =
    Text(stringResource(res), fontSize = 26.sp, fontWeight = FontWeight.SemiBold, modifier = Modifier.padding(top = 8.dp, bottom = 6.dp))

@Composable
private fun LanguageStep() {
    val ctx = LocalContext.current
    Title(R.string.ob_lang_title)
    Muted(stringResource(R.string.ob_lang_desc), Modifier.padding(bottom = 16.dp))
    if (Build.VERSION.SDK_INT >= 33) {
        val lm = ctx.getSystemService(LocaleManager::class.java)
        var cur by remember { mutableStateOf(if (lm.applicationLocales.toLanguageTags().startsWith("en")) "en" else "uz") }
        Seg(listOf("uz" to "O'zbekcha", "en" to "English"), cur, {
            cur = it
            lm.applicationLocales = LocaleList.forLanguageTags(it)
        })
    }
}

@Composable
private fun HowStep() {
    Title(R.string.ob_how_title)
    listOf(
        Icons.Outlined.PhoneAndroid to R.string.ob_how_1,
        Icons.Outlined.Pause to R.string.hint_pause,
        Icons.Outlined.Stop to R.string.hint_end,
    ).forEach { (icon, text) ->
        TCard(Modifier.fillMaxWidth().padding(top = 10.dp), padding = 16) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(icon, null, tint = MaterialTheme.colorScheme.primary)
                Text(stringResource(text), modifier = Modifier.padding(start = 14.dp))
            }
        }
    }
}

private fun granted(ctx: Context, p: String) = ctx.checkSelfPermission(p) == PackageManager.PERMISSION_GRANTED

/** Ruxsatlar holati va HyperOS checklist — Kirish 3-qadam va Sozlamalar → Ruxsatlar. */
@Composable
fun PermissionsContent(app: TasbihApp, s: AppSettings, showTitle: Boolean) {
    val ctx = LocalContext.current
    val scope = rememberCoroutineScope()
    var tick by remember { mutableIntStateOf(0) }
    LifecycleResumeEffect(Unit) { tick++; onPauseOrDispose { } }
    val permLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { tick++ }
    fun open(intent: Intent) = runCatching { ctx.startActivity(intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)) }

    tick.let { } // onResume da holatlarni qayta o'qish
    val pm = ctx.getSystemService(PowerManager::class.java)
    val nm = ctx.getSystemService(NotificationManager::class.java)

    if (showTitle) Title(R.string.ob_perm_title)
    Muted(stringResource(R.string.ob_perm_desc), Modifier.padding(bottom = 12.dp))
    TCard(Modifier.fillMaxWidth(), padding = 4) {
        Column(Modifier.padding(horizontal = 10.dp)) {
            PermRow(R.string.perm_a11y, CounterAccessibilityService.isEnabled(ctx), R.string.open) {
                open(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS))
            }
            RowDivider()
            PermRow(R.string.perm_notifications, granted(ctx, Manifest.permission.POST_NOTIFICATIONS), R.string.grant) {
                permLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
            RowDivider()
            PermRow(R.string.perm_phone, granted(ctx, Manifest.permission.READ_PHONE_STATE), R.string.grant) {
                permLauncher.launch(Manifest.permission.READ_PHONE_STATE)
            }
            RowDivider()
            PermRow(R.string.perm_battery, pm.isIgnoringBatteryOptimizations(ctx.packageName), R.string.open) {
                open(Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS, Uri.parse("package:${ctx.packageName}")))
            }
            RowDivider()
            PermRow(R.string.perm_dnd, nm.isNotificationPolicyAccessGranted, R.string.open) {
                open(Intent(Settings.ACTION_NOTIFICATION_POLICY_ACCESS_SETTINGS))
            }
        }
    }
    SectionHeader(stringResource(R.string.hyperos_title))
    TCard(Modifier.fillMaxWidth(), padding = 4) {
        Column(Modifier.padding(horizontal = 10.dp)) {
            val items = listOf(
                "pinning" to R.string.chk_pinning, "camera" to R.string.chk_camera, "quicksnap" to R.string.chk_quicksnap,
                "shortcut" to R.string.chk_shortcut, "wake" to R.string.chk_wake,
            )
            items.forEachIndexed { i, (k, label) ->
                if (i > 0) RowDivider()
                SwitchRow(stringResource(label), k in s.checklist) { on ->
                    scope.launch { app.settings.update { it.copy(checklist = if (on) it.checklist + k else it.checklist - k) } }
                }
            }
            FilledTonalButton(
                onClick = { open(Intent(Settings.ACTION_SETTINGS)) },
                modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
            ) { Text(stringResource(R.string.open_settings)) }
        }
    }
    Muted(stringResource(R.string.chk_hint), Modifier.padding(8.dp), size = 12)
}

@Composable
private fun PermRow(label: Int, ok: Boolean, action: Int, onClick: () -> Unit) {
    Row(Modifier.fillMaxWidth().padding(vertical = 10.dp), verticalAlignment = Alignment.CenterVertically) {
        val (icon: ImageVector, tint) = if (ok) Icons.Outlined.CheckCircle to MaterialTheme.colorScheme.primary
        else Icons.Outlined.ErrorOutline to MaterialTheme.colorScheme.error
        Icon(icon, null, tint = tint, modifier = Modifier.size(20.dp))
        Text(stringResource(label), modifier = Modifier.weight(1f).padding(start = 10.dp))
        if (ok) Muted(stringResource(R.string.granted))
        else FilledTonalButton(onClick = onClick) { Text(stringResource(action), fontSize = 13.sp) }
    }
}

@Composable
fun PermissionsScreen(app: TasbihApp, s: AppSettings, onBack: () -> Unit) {
    Column(Modifier.fillMaxSize().statusBarsPadding().verticalScroll(rememberScrollState()).padding(horizontal = 16.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(vertical = 8.dp)) {
            IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Outlined.ArrowBack, null) }
            Text(stringResource(R.string.permissions_screen), fontSize = 20.sp, fontWeight = FontWeight.SemiBold)
        }
        PermissionsContent(app, s, showTitle = false)
    }
}
