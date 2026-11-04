package uz.asrorqobulov.tasbih.session

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioManager
import android.media.ToneGenerator
import android.os.Build
import android.os.VibrationAttributes
import android.os.VibrationEffect
import android.os.VibratorManager
import uz.asrorqobulov.tasbih.data.TargetSignal
import uz.asrorqobulov.tasbih.data.VibStrength
import uz.asrorqobulov.tasbih.engine.Signal

/** Vibratsiya va ovoz naqshlari (PLAN 5.4). */
class Feedback(context: Context) {
    enum class Event { START, PAUSE, WARNING, END }

    private val vibrator = context.getSystemService(VibratorManager::class.java).defaultVibrator
    private val audio = context.getSystemService(AudioManager::class.java)
    private val tone by lazy { ToneGenerator(AudioManager.STREAM_NOTIFICATION, 90) }

    var strength = VibStrength.MEDIUM
    var vibrateEach = true

    fun count(signal: Signal, target: TargetSignal) {
        when (signal) {
            Signal.COUNT -> if (vibrateEach) pulses(1, 20)
            Signal.STEP -> pulses(2, 40)
            Signal.TARGET -> {
                val silent = audio.ringerMode != AudioManager.RINGER_MODE_NORMAL
                if (target != TargetSignal.SOUND || silent) pulses(1, 400)
                if (target != TargetSignal.VIBRATION && !silent) tone.startTone(ToneGenerator.TONE_PROP_ACK, 250)
            }
        }
    }

    fun event(e: Event) = when (e) {
        Event.START -> pulses(1, 120)
        Event.PAUSE -> pulses(2, 120)
        Event.WARNING -> pulses(1, 40)
        Event.END -> pulses(3, 60)
    }

    private fun pulses(n: Int, ms: Long) {
        // ponytail: amplitudalar taxminiy, Redmi'da sozlanadi
        val amp = when (strength) { VibStrength.LOW -> 90; VibStrength.MEDIUM -> 180; VibStrength.HIGH -> 255 }
        val timings = LongArray(n * 2) { if (it % 2 == 0) (if (it == 0) 0 else 90) else ms }
        val amps = IntArray(n * 2) { if (it % 2 == 0) 0 else amp }
        val effect = VibrationEffect.createWaveform(timings, amps, -1)
        // ALARM usage — ekran o'chiq va jim rejimda ham vibratsiya bo'lishi uchun
        if (Build.VERSION.SDK_INT >= 33) {
            vibrator.vibrate(effect, VibrationAttributes.createForUsage(VibrationAttributes.USAGE_ALARM))
        } else {
            @Suppress("DEPRECATION")
            vibrator.vibrate(effect, AudioAttributes.Builder().setUsage(AudioAttributes.USAGE_ALARM).build())
        }
    }
}
