package uz.asrorqobulov.tasbih.session

import android.content.Context
import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import android.view.KeyEvent
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import uz.asrorqobulov.tasbih.data.AppSettings
import uz.asrorqobulov.tasbih.data.CountMode
import uz.asrorqobulov.tasbih.data.SessionRecord
import uz.asrorqobulov.tasbih.data.SettingsRepo
import uz.asrorqobulov.tasbih.data.TasbihDao
import uz.asrorqobulov.tasbih.data.Template
import uz.asrorqobulov.tasbih.engine.KeyInterpreter
import uz.asrorqobulov.tasbih.engine.PowerFilter
import uz.asrorqobulov.tasbih.engine.signalFor

enum class State { IDLE, ACTIVE, PAUSED, AUTO_PAUSED }
enum class Source { TAP, VOLUME, POWER }
enum class AutoReason { CALL, AUDIO_FOCUS }

data class SessionResult(val title: String, val counted: Long, val durationMs: Long, val reached: Boolean)

data class SessionUi(
    val state: State = State.IDLE,
    val template: Template? = null,
    val count: Long = 0,
    val sessionCounted: Long = 0,
    val mode: CountMode = CountMode.DEVICE,
    val protectionWanted: Boolean = false,
    val unlocked: Boolean = false,
    val volume: Boolean = true,
    val power: Boolean = true,
    val autoReasons: Set<AutoReason> = emptySet(),
    val endWarning: Boolean = false,
    val hintUntil: Long = 0,
    val result: SessionResult? = null,
) {
    /** Lock qatlami + pinning faqat shu holatda (PLAN 3, 4). */
    val protectionActive get() = state == State.ACTIVE && mode == CountMode.DEVICE && protectionWanted && !unlocked
}

/** Sessiya holat mashinasi (PLAN 3). Barcha chaqiruvlar main thread'da. */
@OptIn(ExperimentalCoroutinesApi::class)
class SessionManager(
    context: Context,
    private val dao: TasbihDao,
    settingsRepo: SettingsRepo,
    private val scope: CoroutineScope,
) {
    private val _ui = MutableStateFlow(SessionUi())
    val ui: StateFlow<SessionUi> = _ui

    private val main = Handler(Looper.getMainLooper())
    private val writer = Dispatchers.IO.limitedParallelism(1)
    private val feedback = Feedback(context)
    private val notification = SessionNotification(context)
    private val hooks = SystemHooks(context, ::onAuto, ::onScreen)
    private val keys = KeyInterpreter()
    private val power = PowerFilter()

    private var settings = AppSettings()
    private var record: SessionRecord? = null
    private var activeSince = -1L
    private var activeAccum = 0L
    private var lastVolumeKey = Long.MIN_VALUE / 2
    private var notifyPending = false
    private var lastKeySig: Triple<Int, Int, Long>? = null
    private var lastKeyConsumed = false

    init {
        scope.launch(Dispatchers.Main) {
            settingsRepo.flow.collect {
                settings = it
                feedback.strength = it.vibStrength
                feedback.vibrateEach = it.vibrateEach
            }
        }
    }

    fun start(t: Template, mode: CountMode, protection: Boolean, volume: Boolean, power: Boolean) {
        if (_ui.value.state != State.IDLE) return
        val now = System.currentTimeMillis()
        record = SessionRecord(
            templateId = t.id, templateTitle = t.title, startedAt = now, updatedAt = now,
            startCount = t.count, endCount = t.count, target = t.target,
        )
        activeAccum = 0
        activeSince = SystemClock.elapsedRealtime()
        keys.reset()
        _ui.value = SessionUi(
            state = State.ACTIVE, template = t, count = t.count, mode = mode,
            protectionWanted = protection, volume = volume || !power, power = power,
            hintUntil = if (mode == CountMode.DEVICE) activeSince + 3000 else 0,
        )
        persist()
        // Oddiy tasbihda musiqa va tizimga tegilmaydi (PLAN 0)
        if (mode == CountMode.DEVICE) hooks.start(settings.dnd)
        feedback.event(Feedback.Event.START)
        notifyNow()
    }

    fun count(source: Source) {
        val u = _ui.value
        val t = u.template ?: return
        if (u.state != State.ACTIVE) return
        val n = u.count + 1
        _ui.value = u.copy(count = n, sessionCounted = u.sessionCounted + 1)
        record = record?.let { r ->
            r.copy(
                counted = r.counted + 1, endCount = n, updatedAt = System.currentTimeMillis(),
                reached = r.reached || (t.target > 0 && n >= t.target),
                fromTap = r.fromTap + if (source == Source.TAP) 1 else 0,
                fromVolume = r.fromVolume + if (source == Source.VOLUME) 1 else 0,
                fromPower = r.fromPower + if (source == Source.POWER) 1 else 0,
            )
        }
        persist()
        val step = if (t.parts > 1) t.partSize else settings.intervalStep
        feedback.count(signalFor(n, t.target, step), t.signal)
        notifyThrottled()
    }

    fun undo() {
        val u = _ui.value
        if (u.state == State.IDLE || u.sessionCounted <= 0) return
        val n = u.count - 1
        _ui.value = u.copy(count = n, sessionCounted = u.sessionCounted - 1)
        record = record?.let { it.copy(counted = it.counted - 1, endCount = n, updatedAt = System.currentTimeMillis()) }
        persist()
        notifyThrottled()
    }

    /** Foydalanuvchi buyrug'i: Vol Up+Down qisqa, bildirishnoma tugmasi, ekrandagi tugma. */
    fun userTogglePause() = when (_ui.value.state) {
        State.ACTIVE, State.AUTO_PAUSED -> pause()
        State.PAUSED -> resume()
        State.IDLE -> Unit
    }

    fun pause() {
        val u = _ui.value
        if (u.state != State.ACTIVE && u.state != State.AUTO_PAUSED) return
        stopClock()
        _ui.value = u.copy(state = State.PAUSED, endWarning = false)
        feedback.event(Feedback.Event.PAUSE)
        notifyNow()
    }

    fun resume() {
        val u = _ui.value
        if (u.state != State.PAUSED) return
        val next = if (u.autoReasons.isEmpty()) State.ACTIVE else State.AUTO_PAUSED
        if (next == State.ACTIVE) startClock()
        _ui.value = u.copy(state = next, unlocked = false)
        feedback.event(Feedback.Event.START)
        notifyNow()
    }

    /** Chekka tugma: pauza + himoyani o'chirish (PLAN 2.3). */
    fun edgePause() {
        pause()
        _ui.update { it.copy(unlocked = true) }
    }

    fun setUnlocked(unlocked: Boolean) = _ui.update { it.copy(unlocked = unlocked) }

    /** Sessiya ichida reset: joriy sessiya tarixga yoziladi, yangisi 0 dan (PLAN 2.3). */
    fun resetInSession() {
        val u = _ui.value
        val t = u.template ?: return
        if (u.state == State.IDLE) return
        val running = u.state == State.ACTIVE
        stopClock()
        finishRecord()
        val now = System.currentTimeMillis()
        record = SessionRecord(
            templateId = t.id, templateTitle = t.title, startedAt = now, updatedAt = now,
            startCount = 0, endCount = 0, target = t.target,
        )
        activeAccum = 0
        if (running) startClock()
        _ui.value = u.copy(count = 0, sessionCounted = 0)
        persist()
        notifyNow()
    }

    fun end() {
        val u = _ui.value
        val t = u.template ?: return
        if (u.state == State.IDLE) return
        stopClock()
        val r = finishRecord()
        if (u.mode == CountMode.DEVICE) hooks.stop()
        keys.reset()
        main.removeCallbacksAndMessages(null)
        notifyPending = false
        notification.cancel()
        feedback.event(Feedback.Event.END)
        _ui.value = SessionUi(
            result = SessionResult(t.title, r?.counted ?: 0, r?.activeMs ?: 0, r?.reached ?: false),
        )
    }

    fun dismissResult() = _ui.update { it.copy(result = null) }

    /** Accessibility servisi va Activity'dan. true — hodisa yutiladi (tizim ovozi o'zgarmaydi). */
    fun onVolumeKey(e: KeyEvent): Boolean {
        val u = _ui.value
        if (u.state == State.IDLE || u.mode != CountMode.DEVICE) return false
        // bir hodisa ikki yo'ldan kelsa — bir marta
        val sig = Triple(e.keyCode, e.action, e.eventTime)
        if (sig == lastKeySig) return lastKeyConsumed
        lastKeySig = sig
        lastKeyConsumed = u.state == State.ACTIVE && u.volume
        lastVolumeKey = SystemClock.elapsedRealtime()
        val actions = if (e.action == KeyEvent.ACTION_DOWN) {
            val wasCombo = keys.comboActive
            keys.keyDown(e.keyCode, e.eventTime, e.repeatCount).also {
                if (!wasCombo && keys.comboActive) scheduleComboTicks(e.eventTime)
            }
        } else {
            keys.keyUp(e.keyCode, e.eventTime)
        }
        actions.forEach(::handleKey)
        if (!keys.comboActive && _ui.value.endWarning) _ui.update { it.copy(endWarning = false) }
        return lastKeyConsumed
    }

    fun onNotification() = power.onNotification(SystemClock.elapsedRealtime())

    private fun onScreen(on: Boolean) {
        val u = _ui.value
        val ok = power.onScreen(on, SystemClock.elapsedRealtime(), lastVolumeKey, keys.anyDown)
        if (ok && u.state == State.ACTIVE && u.power) count(Source.POWER)
    }

    private fun handleKey(a: KeyInterpreter.Action) {
        when (a) {
            KeyInterpreter.Action.COUNT -> if (_ui.value.volume) count(Source.VOLUME)
            KeyInterpreter.Action.TOGGLE_PAUSE -> userTogglePause()
            KeyInterpreter.Action.END_WARNING -> {
                feedback.event(Feedback.Event.WARNING)
                _ui.update { it.copy(endWarning = true) }
            }
            KeyInterpreter.Action.END -> end()
        }
    }

    private fun scheduleComboTicks(start: Long) {
        for (at in longArrayOf(start + 1501, start + 3001)) {
            main.postAtTime({ keys.tick(SystemClock.uptimeMillis()).forEach(::handleKey) }, at)
        }
    }

    private fun onAuto(reason: AutoReason, active: Boolean) {
        val u = _ui.value
        if (u.state == State.IDLE) return
        val reasons = if (active) u.autoReasons + reason else u.autoReasons - reason
        if (reasons == u.autoReasons) return
        when {
            active && u.state == State.ACTIVE -> {
                stopClock()
                _ui.value = u.copy(state = State.AUTO_PAUSED, autoReasons = reasons)
                notifyNow()
            }
            !active && reasons.isEmpty() && u.state == State.AUTO_PAUSED -> {
                _ui.value = u.copy(autoReasons = reasons)
                // qo'ng'iroq tugagach 2 s kutib davom (PLAN 5.3)
                main.postDelayed({
                    val v = _ui.value
                    if (v.state == State.AUTO_PAUSED && v.autoReasons.isEmpty()) {
                        startClock()
                        _ui.value = v.copy(state = State.ACTIVE)
                        notifyNow()
                    }
                }, 2000)
            }
            else -> _ui.value = u.copy(autoReasons = reasons)
        }
    }

    private fun startClock() {
        activeSince = SystemClock.elapsedRealtime()
    }

    private fun stopClock() {
        if (activeSince >= 0) activeAccum += SystemClock.elapsedRealtime() - activeSince
        activeSince = -1
    }

    private fun finishRecord(): SessionRecord? {
        val now = System.currentTimeMillis()
        val r = record?.copy(endedAt = now, updatedAt = now, activeMs = activeAccum) ?: return null
        record = r
        persist()
        record = null
        return r
    }

    private fun persist() {
        val r = record ?: return
        val r2 = r.copy(activeMs = activeAccum + if (activeSince >= 0) SystemClock.elapsedRealtime() - activeSince else 0)
        val id = _ui.value.template?.id ?: return
        val count = _ui.value.count
        scope.launch(writer) {
            dao.setCount(id, count)
            dao.upsertSession(r2)
        }
    }

    private fun notifyNow() {
        if (_ui.value.state != State.IDLE) notification.show(_ui.value)
    }

    private fun notifyThrottled() {
        if (notifyPending) return
        notifyPending = true
        main.postDelayed({ notifyPending = false; notifyNow() }, 500)
    }
}
