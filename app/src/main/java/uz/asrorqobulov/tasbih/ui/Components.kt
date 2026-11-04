package uz.asrorqobulov.tasbih.ui

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.waitForUpOrCancellation
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.KeyboardArrowRight
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import uz.asrorqobulov.tasbih.R
import uz.asrorqobulov.tasbih.data.Template
import java.text.NumberFormat

@Composable
fun templateTitle(t: Template): String =
    if (t.name.isBlank() && t.partSize == 0) stringResource(R.string.unlimited) else t.title

fun fmt(n: Long): String = NumberFormat.getIntegerInstance().format(n)

@Composable
fun TCard(modifier: Modifier = Modifier, padding: Int = 14, onClick: (() -> Unit)? = null, content: @Composable ColumnScope.() -> Unit) {
    Column(
        modifier
            .clip(RoundedCornerShape(16.dp))
            .background(MaterialTheme.colorScheme.surfaceContainer)
            .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(16.dp))
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier)
            .padding(padding.dp),
        content = content,
    )
}

@Composable
fun SectionHeader(text: String) {
    Text(
        text, color = MaterialTheme.colorScheme.primary, fontSize = 13.sp,
        modifier = Modifier.padding(top = 18.dp, bottom = 6.dp, start = 4.dp),
    )
}

@Composable
fun Muted(text: String, modifier: Modifier = Modifier, size: Int = 13) {
    Text(text, color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = size.sp, modifier = modifier)
}

/** Segmentli tanlov (PLAN mockup). */
@Composable
fun <T> Seg(options: List<Pair<T, String>>, selected: T, onSelect: (T) -> Unit, modifier: Modifier = Modifier) {
    Row(
        modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(MaterialTheme.colorScheme.surfaceContainerHigh)
            .padding(3.dp),
        horizontalArrangement = Arrangement.spacedBy(3.dp),
    ) {
        options.forEach { (v, label) ->
            val on = v == selected
            val bg by animateColorAsState(if (on) MaterialTheme.colorScheme.surfaceContainer else MaterialTheme.colorScheme.surfaceContainerHigh, label = "seg")
            val fg by animateColorAsState(if (on) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant, label = "segText")
            Box(
                Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(9.dp))
                    .background(bg)
                    .then(if (on) Modifier.border(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.35f), RoundedCornerShape(9.dp)) else Modifier)
                    .clickable { onSelect(v) }
                    .padding(vertical = 10.dp),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    label, fontSize = 13.sp, textAlign = TextAlign.Center, color = fg,
                    fontWeight = if (on) FontWeight.SemiBold else FontWeight.Normal,
                )
            }
        }
    }
}

@Composable
fun SwitchRow(text: String, checked: Boolean, enabled: Boolean = true, sub: String? = null, onChange: (Boolean) -> Unit) {
    Row(
        Modifier
            .fillMaxWidth()
            .clickable(enabled = enabled) { onChange(!checked) }
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f)) {
            Text(text, fontSize = 15.sp)
            if (sub != null) Muted(sub, size = 12)
        }
        Switch(checked, onChange, enabled = enabled, modifier = Modifier.alpha(if (enabled) 1f else 0.6f))
    }
}

@Composable
fun NavRow(text: String, value: String? = null, color: androidx.compose.ui.graphics.Color? = null, arrow: Boolean = false, onClick: () -> Unit) {
    Row(
        Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(text, fontSize = 15.sp, modifier = Modifier.weight(1f), color = color ?: MaterialTheme.colorScheme.onSurface)
        if (value != null) Muted(value)
        if (arrow) Icon(Icons.AutoMirrored.Outlined.KeyboardArrowRight, null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
fun RowDivider() = HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

@Composable
fun ConfirmDialog(
    title: String, text: String, confirm: String, danger: Boolean = false,
    onConfirm: () -> Unit, onDismiss: () -> Unit,
) {
    // tugmalar ustma-ust — matn bir qatorda sig'adi (mockup tuzatishi)
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = { Text(text) },
        confirmButton = {
            Column(Modifier.fillMaxWidth()) {
                androidx.compose.material3.Button(
                    onClick = { onConfirm(); onDismiss() },
                    modifier = Modifier.fillMaxWidth(),
                    colors = if (danger) androidx.compose.material3.ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.error, contentColor = MaterialTheme.colorScheme.onError,
                    ) else androidx.compose.material3.ButtonDefaults.buttonColors(),
                ) { Text(confirm) }
                TextButton(onClick = onDismiss, modifier = Modifier.fillMaxWidth()) { Text(stringResource(R.string.cancel)) }
            }
        },
    )
}

/** Bosib turish: [ms] dan keyin [onTrigger] (qulf 2 s, chekka tugma 1 s). */
fun Modifier.holdToTrigger(ms: Long, onTrigger: () -> Unit) = pointerInput(ms) {
    awaitEachGesture {
        awaitFirstDown()
        val released = withTimeoutOrNull(ms) { waitForUpOrCancellation() }
        if (released == null) {
            onTrigger()
            waitForUpOrCancellation()
        }
    }
}
