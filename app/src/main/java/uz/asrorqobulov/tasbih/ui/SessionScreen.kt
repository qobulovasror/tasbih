package uz.asrorqobulov.tasbih.ui

import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.os.SystemClock
import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Call
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.LockOpen
import androidx.compose.material.icons.outlined.Pause
import androidx.compose.material.icons.outlined.PlayArrow
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material.icons.outlined.Stop
import androidx.compose.material.icons.automirrored.outlined.Undo
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import uz.asrorqobulov.tasbih.R
import uz.asrorqobulov.tasbih.TasbihApp
import uz.asrorqobulov.tasbih.data.AppSettings
import uz.asrorqobulov.tasbih.data.CountMode
import uz.asrorqobulov.tasbih.data.Template
import uz.asrorqobulov.tasbih.session.Source
import uz.asrorqobulov.tasbih.session.SessionUi
import uz.asrorqobulov.tasbih.session.State
import kotlin.math.cos
import kotlin.math.sin

@Composable
fun SessionScreen(app: TasbihApp, u: SessionUi, s: AppSettings) {
    val t = u.template ?: return
    val session = app.session
    BackHandler { } // sessiya paytida back — tasodifiy chiqishdan himoya
    var confirmReset by remember { mutableStateOf(false) }
    var confirmEnd by remember { mutableStateOf(false) }
    val near = if (u.mode == CountMode.DEVICE && s.proximity) proximityNear() else false
    val device = u.mode == CountMode.DEVICE
    // qorong'i mavzuda qurilma rejimi — sof qora (AMOLED), yorug'da — oddiy fon
    val darkTheme = MaterialTheme.colorScheme.background.luminance() < 0.5f
    val bg = if (device && darkTheme) Color.Black else MaterialTheme.colorScheme.background

    Box(Modifier.fillMaxSize().background(bg)) {
        Column(Modifier.fillMaxSize().statusBarsPadding().navigationBarsPadding()) {
            Row(Modifier.fillMaxWidth().padding(top = 14.dp), horizontalArrangement = Arrangement.Center, verticalAlignment = Alignment.CenterVertically) {
                Muted(templateTitle(t), size = 15)
                Spacer(Modifier.size(6.dp))
                Icon(
                    when (u.state) {
                        State.ACTIVE -> Icons.Outlined.PlayArrow
                        State.AUTO_PAUSED -> Icons.Outlined.Call
                        else -> Icons.Outlined.Pause
                    },
                    null, tint = if (u.state == State.ACTIVE) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(18.dp),
                )
            }
            HintBanner(u)
            if (u.state == State.AUTO_PAUSED) Muted(stringResource(R.string.state_auto), Modifier.align(Alignment.CenterHorizontally).padding(top = 6.dp))
            if (u.endWarning) {
                Text(
                    stringResource(R.string.end_warning), color = MaterialTheme.colorScheme.tertiary,
                    modifier = Modifier.align(Alignment.CenterHorizontally).padding(top = 6.dp),
                )
            }
            if (device) DeviceBody(u, t) else TapBody(app, u, t)
            Controls(app, u, onReset = { confirmReset = true }, onEnd = { confirmEnd = true })
        }

        // Lock qatlami: hamma tegishlarni yutadi, faqat qulf va chekka tugma ishlaydi (PLAN 4)
        if (u.protectionActive) {
            Box(Modifier.fillMaxSize().pointerInput(Unit) { awaitEachGesture { awaitFirstDown().consume() } })
            Column(
                Modifier.align(Alignment.BottomCenter).navigationBarsPadding().padding(bottom = 28.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Box(
                    Modifier
                        .size(64.dp)
                        .border(1.dp, MaterialTheme.colorScheme.outline, CircleShape)
                        .holdToTrigger(2000) { session.setUnlocked(true) },
                    contentAlignment = Alignment.Center,
                ) { Icon(Icons.Outlined.Lock, null, tint = MaterialTheme.colorScheme.onSurfaceVariant) }
                Muted(stringResource(R.string.hold_unlock), Modifier.padding(top = 6.dp), size = 12)
            }
        }
        if (s.edgeButton && device) EdgeButton(u, onTrigger = session::edgePause)

        if (near) Box(Modifier.fillMaxSize().background(Color.Black).pointerInput(Unit) { awaitEachGesture { awaitFirstDown().consume() } })
    }

    if (confirmReset) {
        ConfirmDialog(
            stringResource(R.string.reset_title), stringResource(R.string.reset_session_text), stringResource(R.string.reset),
            onConfirm = session::resetInSession, onDismiss = { confirmReset = false },
        )
    }
    if (confirmEnd) {
        ConfirmDialog(
            stringResource(R.string.end_title), stringResource(R.string.end_text), stringResource(R.string.stop),
            onConfirm = session::end, onDismiss = { confirmEnd = false },
        )
    }
}

@Composable
private fun HintBanner(u: SessionUi) {
    var visible by remember(u.hintUntil) { mutableStateOf(SystemClock.elapsedRealtime() < u.hintUntil) }
    LaunchedEffect(u.hintUntil) {
        val left = u.hintUntil - SystemClock.elapsedRealtime()
        if (left > 0) { delay(left); visible = false }
    }
    if (visible) Hints(Modifier.padding(horizontal = 16.dp, vertical = 10.dp))
}

@Composable
private fun androidx.compose.foundation.layout.ColumnScope.DeviceBody(u: SessionUi, t: Template) {
    val dim = if (u.state == State.ACTIVE) 1f else 0.4f
    val progress = when {
        t.target > 0 -> (u.count.toFloat() / t.target).coerceAtMost(1f)
        else -> 0f
    }
    val anim by animateFloatAsState(progress, label = "ring")
    val reached = t.target > 0 && u.count >= t.target
    Box(Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
        Ring(anim, if (reached) Gold else MaterialTheme.colorScheme.primary, Modifier.size(260.dp).alpha(dim))
        CountText(u, t, dim, 72)
    }
}

@Composable
private fun CountText(u: SessionUi, t: Template, alpha: Float, size: Int) {
    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.alpha(alpha)) {
        Text(fmt(u.count), fontSize = size.sp, fontWeight = FontWeight.Medium, style = Tnum)
        if (t.partSize > 0) Muted(templateSub(t, u.count), size = 15)
    }
}

@Composable
private fun Ring(progress: Float, color: Color, modifier: Modifier) {
    val track = MaterialTheme.colorScheme.surfaceContainerHighest
    Canvas(modifier) {
        val w = 12.dp.toPx()
        drawArc(track, 0f, 360f, false, style = Stroke(w), topLeft = Offset(w / 2, w / 2), size = size.copy(size.width - w, size.height - w))
        drawArc(color, -90f, 360f * progress, false, style = Stroke(w, cap = StrokeCap.Round), topLeft = Offset(w / 2, w / 2), size = size.copy(size.width - w, size.height - w))
    }
}

/** Oddiy tasbih: maqsad/qism ≤ 100 bo'lsa donalar, aks holda halqa (PLAN 8.1). */
@Composable
private fun androidx.compose.foundation.layout.ColumnScope.TapBody(app: TasbihApp, u: SessionUi, t: Template) {
    val active = u.state == State.ACTIVE
    val beads = t.partSize in 1..100
    val reached = t.target > 0 && u.count >= t.target
    Box(Modifier.fillMaxWidth().height(280.dp), contentAlignment = Alignment.Center) {
        if (beads) {
            val filled = when {
                reached -> t.partSize
                else -> (u.count % t.partSize).toInt()
            }
            Beads(t.partSize, filled, if (reached) Gold else MaterialTheme.colorScheme.primary, Modifier.size(250.dp))
        } else {
            Ring(if (t.target > 0) (u.count.toFloat() / t.target).coerceAtMost(1f) else 0f, MaterialTheme.colorScheme.primary, Modifier.size(250.dp))
        }
        CountText(u, t, if (active) 1f else 0.4f, 56)
    }
    val source = remember { MutableInteractionSource() }
    val pressed by source.collectIsPressedAsState()
    Box(Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
        Box(
            Modifier
                .fillMaxWidth(0.62f)
                .aspectRatio(1f)
                .scale(if (pressed) 0.94f else 1f)
                .background(MaterialTheme.colorScheme.surfaceContainer, CircleShape)
                .clickable(interactionSource = source, indication = null, enabled = active) { app.session.count(Source.TAP) },
            contentAlignment = Alignment.Center,
        ) { Text(stringResource(R.string.tap_here), color = MaterialTheme.colorScheme.primary, fontSize = 16.sp) }
    }
}

@Composable
private fun Beads(n: Int, filled: Int, color: Color, modifier: Modifier) {
    val empty = MaterialTheme.colorScheme.surfaceContainerHighest
    Canvas(modifier) {
        val r = size.minDimension / 2
        val bead = (Math.PI * r / n).toFloat().coerceAtMost(9.dp.toPx()) * 0.9f
        val ringR = r - bead
        for (i in 0 until n) {
            val a = i * 2 * Math.PI / n - Math.PI / 2
            drawCircle(
                if (i < filled) color else empty, bead,
                Offset(center.x + ringR * cos(a).toFloat(), center.y + ringR * sin(a).toFloat()),
            )
        }
    }
}

@Composable
private fun Controls(app: TasbihApp, u: SessionUi, onReset: () -> Unit, onEnd: () -> Unit) {
    val s = app.session
    when {
        u.state == State.PAUSED || u.state == State.AUTO_PAUSED -> Column(Modifier.padding(16.dp)) {
            Button(onClick = s::resume, enabled = u.state == State.PAUSED, modifier = Modifier.fillMaxWidth().height(52.dp), shape = RoundedCornerShape(16.dp)) {
                Icon(Icons.Outlined.PlayArrow, null); Text(" " + stringResource(R.string.resume))
            }
            if (u.state == State.AUTO_PAUSED) {
                OutlinedButton(onClick = s::pause, modifier = Modifier.fillMaxWidth().padding(top = 8.dp)) { Text(stringResource(R.string.pause)) }
            }
            Row(Modifier.fillMaxWidth().padding(top = 8.dp), horizontalArrangement = Arrangement.SpaceEvenly) {
                IconButton(onClick = s::undo) { Icon(Icons.AutoMirrored.Outlined.Undo, stringResource(R.string.undo)) }
                IconButton(onClick = onReset) { Icon(Icons.Outlined.Refresh, stringResource(R.string.reset)) }
                IconButton(onClick = onEnd) { Icon(Icons.Outlined.Stop, stringResource(R.string.stop)) }
            }
        }
        u.protectionActive -> Spacer(Modifier.height(120.dp)) // qulf belgisi joyi
        else -> Row(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 14.dp), horizontalArrangement = Arrangement.SpaceEvenly) {
            IconButton(onClick = s::undo) { Icon(Icons.AutoMirrored.Outlined.Undo, stringResource(R.string.undo)) }
            IconButton(onClick = onReset) { Icon(Icons.Outlined.Refresh, stringResource(R.string.reset)) }
            IconButton(onClick = s::pause) { Icon(Icons.Outlined.Pause, stringResource(R.string.pause)) }
            if (u.mode == CountMode.DEVICE && u.protectionWanted) {
                IconButton(onClick = { s.setUnlocked(false) }) { Icon(Icons.Outlined.LockOpen, stringResource(R.string.lock)) }
            }
            IconButton(onClick = onEnd) { Icon(Icons.Outlined.Stop, stringResource(R.string.stop)) }
        }
    }
}

/** Chekka tugma — chap yuqori, 1 s bosib turish (PLAN 2.3). */
@Composable
private fun BoxScope.EdgeButton(u: SessionUi, onTrigger: () -> Unit) {
    val big = u.state == State.AUTO_PAUSED
    Box(
        Modifier
            .align(Alignment.TopStart)
            .statusBarsPadding()
            .padding(10.dp)
            .size(if (big) 72.dp else 44.dp)
            .background(if (big) MaterialTheme.colorScheme.tertiary.copy(alpha = 0.3f) else Color.Transparent, RoundedCornerShape(14.dp))
            .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(14.dp))
            .holdToTrigger(1000, onTrigger),
        contentAlignment = Alignment.Center,
    ) { Icon(Icons.Outlined.Pause, null, tint = MaterialTheme.colorScheme.onSurfaceVariant) }
}

@Composable
private fun proximityNear(): Boolean {
    val ctx = LocalContext.current
    var near by remember { mutableStateOf(false) }
    DisposableEffect(Unit) {
        val sm = ctx.getSystemService(SensorManager::class.java)
        val sensor = sm.getDefaultSensor(Sensor.TYPE_PROXIMITY)
        val l = object : SensorEventListener {
            override fun onSensorChanged(e: SensorEvent) {
                near = e.values[0] < (sensor?.maximumRange ?: 5f).coerceAtMost(5f)
            }
            override fun onAccuracyChanged(s: Sensor?, a: Int) = Unit
        }
        sensor?.let { sm.registerListener(l, it, SensorManager.SENSOR_DELAY_NORMAL) }
        onDispose { sm.unregisterListener(l) }
    }
    return near
}
