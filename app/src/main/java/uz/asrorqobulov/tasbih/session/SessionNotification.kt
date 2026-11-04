package uz.asrorqobulov.tasbih.session

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.graphics.drawable.Icon
import uz.asrorqobulov.tasbih.R
import uz.asrorqobulov.tasbih.TasbihApp
import uz.asrorqobulov.tasbih.ui.MainActivity

class SessionNotification(private val context: Context) {
    private val nm = context.getSystemService(NotificationManager::class.java)

    init {
        nm.createNotificationChannel(
            NotificationChannel(CHANNEL, context.getString(R.string.notif_channel), NotificationManager.IMPORTANCE_LOW)
                .apply { setShowBadge(false) },
        )
    }

    fun show(ui: SessionUi) {
        val t = ui.template ?: return
        val paused = ui.state != State.ACTIVE
        val text = buildString {
            append(ui.count)
            if (t.target > 0) append(" / ").append(t.target)
            if (paused) append(" · ").append(context.getString(R.string.state_paused))
        }
        val n = Notification.Builder(context, CHANNEL)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(t.title.replace("∞", context.getString(R.string.unlimited)))
            .setContentText(text)
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .setContentIntent(
                PendingIntent.getActivity(
                    context, 0, Intent(context, MainActivity::class.java), PendingIntent.FLAG_IMMUTABLE,
                ),
            )
            .addAction(action(ACTION_TOGGLE, if (paused) R.string.resume else R.string.pause))
            .addAction(action(ACTION_STOP, R.string.stop))
            .build()
        nm.notify(ID, n)
    }

    fun cancel() = nm.cancel(ID)

    private fun action(act: String, label: Int) = Notification.Action.Builder(
        Icon.createWithResource(context, R.drawable.ic_notification),
        context.getString(label),
        PendingIntent.getBroadcast(
            context, act.hashCode(),
            Intent(context, SessionActionReceiver::class.java).setAction(act), PendingIntent.FLAG_IMMUTABLE,
        ),
    ).build()

    companion object {
        const val CHANNEL = "session"
        const val ID = 1
        const val ACTION_TOGGLE = "uz.asrorqobulov.tasbih.TOGGLE"
        const val ACTION_STOP = "uz.asrorqobulov.tasbih.STOP"
    }
}

class SessionActionReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val s = TasbihApp.get(context).session
        when (intent.action) {
            SessionNotification.ACTION_TOGGLE -> s.userTogglePause()
            SessionNotification.ACTION_STOP -> s.end()
        }
    }
}
