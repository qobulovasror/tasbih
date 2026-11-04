package uz.asrorqobulov.tasbih.data

import android.content.Context
import androidx.datastore.preferences.core.MutablePreferences
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.core.stringSetPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

enum class VibStrength { LOW, MEDIUM, HIGH }
enum class ThemeMode { DARK, LIGHT, SYSTEM }
enum class Accent { EMERALD, GOLD, BLUE, PURPLE, SYSTEM }

data class AppSettings(
    val vibrateEach: Boolean = true,
    val vibStrength: VibStrength = VibStrength.MEDIUM,
    val intervalStep: Int = 33,
    val edgeButton: Boolean = false,
    val proximity: Boolean = true,
    val showWhenLocked: Boolean = true,
    val dnd: Boolean = false,
    val theme: ThemeMode = ThemeMode.DARK,
    val accent: Accent = Accent.EMERALD,
    val protection: Boolean = true,
    val volumeCount: Boolean = true,
    val powerCount: Boolean = true,
    val onboardingDone: Boolean = false,
    val checklist: Set<String> = emptySet(),
    val lastBackup: Long = 0,
    val dailyGoal: Int = 0, // 0 — o'chiq
    val reminderMin: Int = -1, // kun boshidan daqiqa; -1 — o'chiq
)

private val Context.store by preferencesDataStore("settings")

class SettingsRepo(private val context: Context) {
    private object K {
        val vibrateEach = booleanPreferencesKey("vibrateEach")
        val vibStrength = stringPreferencesKey("vibStrength")
        val intervalStep = intPreferencesKey("intervalStep")
        val edgeButton = booleanPreferencesKey("edgeButton")
        val proximity = booleanPreferencesKey("proximity")
        val showWhenLocked = booleanPreferencesKey("showWhenLocked")
        val dnd = booleanPreferencesKey("dnd")
        val theme = stringPreferencesKey("theme")
        val accent = stringPreferencesKey("accent")
        val protection = booleanPreferencesKey("protection")
        val volumeCount = booleanPreferencesKey("volumeCount")
        val powerCount = booleanPreferencesKey("powerCount")
        val onboardingDone = booleanPreferencesKey("onboardingDone")
        val checklist = stringSetPreferencesKey("checklist")
        val lastBackup = longPreferencesKey("lastBackup")
        val dailyGoal = intPreferencesKey("dailyGoal")
        val reminderMin = intPreferencesKey("reminderMin")
    }

    val flow: Flow<AppSettings> = context.store.data.map { it.toSettings() }

    suspend fun current(): AppSettings = flow.first()

    suspend fun update(block: (AppSettings) -> AppSettings) {
        context.store.edit { p -> p.write(block(p.toSettings())) }
    }

    private fun Preferences.toSettings() = AppSettings().let { d ->
        AppSettings(
            vibrateEach = this[K.vibrateEach] ?: d.vibrateEach,
            vibStrength = enumOr(this[K.vibStrength], d.vibStrength),
            intervalStep = this[K.intervalStep] ?: d.intervalStep,
            edgeButton = this[K.edgeButton] ?: d.edgeButton,
            proximity = this[K.proximity] ?: d.proximity,
            showWhenLocked = this[K.showWhenLocked] ?: d.showWhenLocked,
            dnd = this[K.dnd] ?: d.dnd,
            theme = enumOr(this[K.theme], d.theme),
            accent = enumOr(this[K.accent], d.accent),
            protection = this[K.protection] ?: d.protection,
            volumeCount = this[K.volumeCount] ?: d.volumeCount,
            powerCount = this[K.powerCount] ?: d.powerCount,
            onboardingDone = this[K.onboardingDone] ?: d.onboardingDone,
            checklist = this[K.checklist] ?: d.checklist,
            lastBackup = this[K.lastBackup] ?: d.lastBackup,
            dailyGoal = this[K.dailyGoal] ?: d.dailyGoal,
            reminderMin = this[K.reminderMin] ?: d.reminderMin,
        )
    }

    private fun MutablePreferences.write(s: AppSettings) {
        this[K.vibrateEach] = s.vibrateEach
        this[K.vibStrength] = s.vibStrength.name
        this[K.intervalStep] = s.intervalStep
        this[K.edgeButton] = s.edgeButton
        this[K.proximity] = s.proximity
        this[K.showWhenLocked] = s.showWhenLocked
        this[K.dnd] = s.dnd
        this[K.theme] = s.theme.name
        this[K.accent] = s.accent.name
        this[K.protection] = s.protection
        // kamida bitta tugma doim yoqiq (PLAN 2.1)
        this[K.volumeCount] = s.volumeCount || !s.powerCount
        this[K.powerCount] = s.powerCount
        this[K.onboardingDone] = s.onboardingDone
        this[K.checklist] = s.checklist
        this[K.lastBackup] = s.lastBackup
        this[K.dailyGoal] = s.dailyGoal
        this[K.reminderMin] = s.reminderMin
    }
}

private inline fun <reified E : Enum<E>> enumOr(name: String?, default: E): E =
    name?.let { n -> enumValues<E>().firstOrNull { it.name == n } } ?: default
