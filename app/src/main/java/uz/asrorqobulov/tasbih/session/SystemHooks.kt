package uz.asrorqobulov.tasbih.session

import android.Manifest
import android.app.NotificationManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.media.AudioAttributes
import android.media.AudioFocusRequest
import android.media.AudioManager
import android.telephony.TelephonyCallback
import android.telephony.TelephonyManager

/** Sessiya paytidagi tizim ulanishlari: audio fokus, qo'ng'iroq, ekran ON/OFF, DND. */
class SystemHooks(
    private val context: Context,
    private val onAuto: (reason: AutoReason, active: Boolean) -> Unit,
    private val onScreen: (on: Boolean) -> Unit,
) {
    private val audio = context.getSystemService(AudioManager::class.java)
    private val telephony = context.getSystemService(TelephonyManager::class.java)
    private val notifications = context.getSystemService(NotificationManager::class.java)
    private val executor = context.mainExecutor

    private var focus: AudioFocusRequest? = null
    private var callState = TelephonyManager.CALL_STATE_IDLE
    private var telephonyCb: TelephonyCallback? = null
    private var screenReceiver: BroadcastReceiver? = null
    private var dndPrev: Int? = null

    private val modeListener = AudioManager.OnModeChangedListener { evaluateCall() }

    fun start(dnd: Boolean) {
        // boshqa ilovalardagi audio to'xtaydi (PLAN 2.2)
        focus = AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN)
            .setAudioAttributes(AudioAttributes.Builder().setUsage(AudioAttributes.USAGE_MEDIA).build())
            .setOnAudioFocusChangeListener { change ->
                when (change) {
                    // budilnik/taymer
                    AudioManager.AUDIOFOCUS_LOSS_TRANSIENT -> onAuto(AutoReason.AUDIO_FOCUS, true)
                    AudioManager.AUDIOFOCUS_GAIN, AudioManager.AUDIOFOCUS_LOSS -> onAuto(AutoReason.AUDIO_FOCUS, false)
                }
            }.build().also { audio.requestAudioFocus(it) }

        audio.addOnModeChangedListener(executor, modeListener)
        if (context.checkSelfPermission(Manifest.permission.READ_PHONE_STATE) == PackageManager.PERMISSION_GRANTED) {
            telephonyCb = object : TelephonyCallback(), TelephonyCallback.CallStateListener {
                override fun onCallStateChanged(state: Int) {
                    callState = state
                    evaluateCall()
                }
            }.also { runCatching { telephony.registerTelephonyCallback(executor, it) } }
        }
        evaluateCall()

        screenReceiver = object : BroadcastReceiver() {
            override fun onReceive(c: Context, i: Intent) = onScreen(i.action == Intent.ACTION_SCREEN_ON)
        }.also {
            context.registerReceiver(
                it,
                IntentFilter().apply { addAction(Intent.ACTION_SCREEN_ON); addAction(Intent.ACTION_SCREEN_OFF) },
                Context.RECEIVER_NOT_EXPORTED,
            )
        }

        if (dnd && notifications.isNotificationPolicyAccessGranted) {
            dndPrev = notifications.currentInterruptionFilter
            notifications.setInterruptionFilter(NotificationManager.INTERRUPTION_FILTER_PRIORITY)
        }
    }

    fun stop() {
        focus?.let { audio.abandonAudioFocusRequest(it) }
        focus = null
        audio.removeOnModeChangedListener(modeListener)
        telephonyCb?.let { runCatching { telephony.unregisterTelephonyCallback(it) } }
        telephonyCb = null
        screenReceiver?.let { runCatching { context.unregisterReceiver(it) } }
        screenReceiver = null
        dndPrev?.let { if (notifications.isNotificationPolicyAccessGranted) notifications.setInterruptionFilter(it) }
        dndPrev = null
    }

    private fun evaluateCall() {
        val busy = audio.mode != AudioManager.MODE_NORMAL || callState != TelephonyManager.CALL_STATE_IDLE
        onAuto(AutoReason.CALL, busy)
    }
}
