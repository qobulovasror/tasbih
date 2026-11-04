package uz.asrorqobulov.tasbih.ui

import android.app.ActivityManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.os.PowerManager
import android.view.KeyEvent
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch
import uz.asrorqobulov.tasbih.TasbihApp
import uz.asrorqobulov.tasbih.data.AppSettings
import uz.asrorqobulov.tasbih.data.CountMode
import uz.asrorqobulov.tasbih.session.SessionUi
import uz.asrorqobulov.tasbih.session.State

class MainActivity : ComponentActivity() {
    private val app get() = TasbihApp.get(this)
    private val handler = Handler(Looper.getMainLooper())
    private var last: SessionUi = SessionUi()

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        if (savedInstanceState == null) handleQuick(intent)
        setContent { TasbihRoot(app) }
        lifecycleScope.launch {
            combine(app.session.ui, app.settings.flow) { u, s -> u to s }.collect { (u, s) -> applyWindow(u, s) }
        }
    }

    /** Ekran o'chmasligi, qulf ekrani ustida ko'rinish, pinning (PLAN 2.2, 4). */
    private fun applyWindow(u: SessionUi, s: AppSettings) {
        last = u
        val running = u.state != State.IDLE
        if (running) window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        else window.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        setShowWhenLocked(running && u.mode == CountMode.DEVICE && s.showWhenLocked)
        syncPinning()
    }

    private fun syncPinning() {
        val pinned = getSystemService(ActivityManager::class.java).lockTaskModeState != ActivityManager.LOCK_TASK_MODE_NONE
        val want = last.protectionActive
        if (want && !pinned && lifecycle.currentState.isAtLeast(Lifecycle.State.RESUMED)) runCatching { startLockTask() }
        if (!want && pinned) runCatching { stopLockTask() }
    }

    /** Ilova ochiq bo'lganda zaxira yo'l: Accessibility yutmagan Volume hodisalari (PLAN 4.2). */
    override fun dispatchKeyEvent(event: KeyEvent): Boolean {
        val vol = event.keyCode == KeyEvent.KEYCODE_VOLUME_UP || event.keyCode == KeyEvent.KEYCODE_VOLUME_DOWN
        if (vol && app.session.onVolumeKey(event)) return true
        return super.dispatchKeyEvent(event)
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        handleQuick(intent)
    }

    /** Vidjet ▶, Quick Settings, eslatma — oxirgi shablonni tayyorlash (PLAN 11.2–11.3). */
    private fun handleQuick(i: Intent?) {
        if (i?.getBooleanExtra(EXTRA_QUICK, false) == true) app.quickStart.value = true
    }

    companion object {
        private const val EXTRA_QUICK = "quick"

        fun quickIntent(context: Context, quick: Boolean = true) = Intent(context, MainActivity::class.java)
            .putExtra(EXTRA_QUICK, quick)
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)

        fun openIntent(context: Context, quick: Boolean): PendingIntent = PendingIntent.getActivity(
            context, if (quick) 1 else 0, quickIntent(context, quick),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
        )
    }

    override fun onResume() {
        super.onResume()
        syncPinning()
    }

    override fun onStop() {
        super.onStop()
        // Ekran yoniq holda Home/boshqa ilova — avtomatik pauza; ekran o'chishi esa sessiyani to'xtatmaydi (PLAN 5.3)
        handler.postDelayed({
            val pm = getSystemService(PowerManager::class.java)
            val stopped = !lifecycle.currentState.isAtLeast(Lifecycle.State.STARTED)
            if (stopped && pm.isInteractive && app.session.ui.value.state == State.ACTIVE) app.session.pause()
        }, 800)
    }
}
