package uz.asrorqobulov.tasbih.data

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

@Serializable
data class BackupFile(
    val version: Int = 1,
    val exportedAt: Long,
    val templates: List<Template>,
    val sessions: List<SessionRecord>,
    val settings: Map<String, String> = emptyMap(),
)

object Backup {
    private val json = Json { prettyPrint = true; ignoreUnknownKeys = true; encodeDefaults = true }

    fun encode(f: BackupFile): String = json.encodeToString(BackupFile.serializer(), f)
    fun decode(text: String): BackupFile = json.decodeFromString(BackupFile.serializer(), text)

    fun settingsToMap(s: AppSettings) = mapOf(
        "vibrateEach" to "${s.vibrateEach}", "vibStrength" to s.vibStrength.name,
        "intervalStep" to "${s.intervalStep}", "edgeButton" to "${s.edgeButton}",
        "proximity" to "${s.proximity}", "showWhenLocked" to "${s.showWhenLocked}",
        "dnd" to "${s.dnd}", "theme" to s.theme.name, "accent" to s.accent.name,
        "protection" to "${s.protection}", "volumeCount" to "${s.volumeCount}",
        "powerCount" to "${s.powerCount}",
        "dailyGoal" to "${s.dailyGoal}", "reminderMin" to "${s.reminderMin}",
    )

    fun settingsFromMap(m: Map<String, String>, base: AppSettings): AppSettings {
        fun b(k: String, d: Boolean) = m[k]?.toBooleanStrictOrNull() ?: d
        return base.copy(
            vibrateEach = b("vibrateEach", base.vibrateEach),
            vibStrength = VibStrength.entries.firstOrNull { it.name == m["vibStrength"] } ?: base.vibStrength,
            intervalStep = m["intervalStep"]?.toIntOrNull() ?: base.intervalStep,
            edgeButton = b("edgeButton", base.edgeButton),
            proximity = b("proximity", base.proximity),
            showWhenLocked = b("showWhenLocked", base.showWhenLocked),
            dnd = b("dnd", base.dnd),
            theme = ThemeMode.entries.firstOrNull { it.name == m["theme"] } ?: base.theme,
            accent = Accent.entries.firstOrNull { it.name == m["accent"] } ?: base.accent,
            protection = b("protection", base.protection),
            volumeCount = b("volumeCount", base.volumeCount),
            powerCount = b("powerCount", base.powerCount),
            dailyGoal = m["dailyGoal"]?.toIntOrNull() ?: base.dailyGoal,
            reminderMin = m["reminderMin"]?.toIntOrNull() ?: base.reminderMin,
        )
    }
}
