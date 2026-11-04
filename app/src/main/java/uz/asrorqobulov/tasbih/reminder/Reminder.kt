package uz.asrorqobulov.tasbih.reminder

import android.app.AlarmManager
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import kotlinx.coroutines.launch
import uz.asrorqobulov.tasbih.R
import uz.asrorqobulov.tasbih.TasbihApp
import uz.asrorqobulov.tasbih.data.Stats
import uz.asrorqobulov.tasbih.ui.MainActivity
import uz.asrorqobulov.tasbih.ui.fmt
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZonedDateTime

/** Kunlik eslatma (PLAN 11.1): aniq bo'lmagan alarm, har kuni qayta rejalanadi. */
object Reminder {
    private const val CHANNEL = "reminder"
    private const val ID = 2

    /** Bugun [minutes] hali kelmagan bo'lsa — bugun, aks holda ertaga. */
    fun nextAt(now: ZonedDateTime, minutes: Int): ZonedDateTime {
        val t = now.with(LocalTime.of(minutes / 60, minutes % 60))
        return if (t.isAfter(now)) t else t.plusDays(1)
    }

    fun schedule(context: Context, minutes: Int) {
        val am = context.getSystemService(AlarmManager::class.java)
        val pi = PendingIntent.getBroadcast(
            context, 0, Intent(context, ReminderReceiver::class.java),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
        )
        am.cancel(pi)
        if (minutes < 0) return
        am.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, nextAt(ZonedDateTime.now(), minutes).toInstant().toEpochMilli(), pi)
    }

    fun show(context: Context, text: String) {
        val nm = context.getSystemService(NotificationManager::class.java)
        nm.createNotificationChannel(
            NotificationChannel(CHANNEL, context.getString(R.string.reminder_channel), NotificationManager.IMPORTANCE_DEFAULT),
        )
        val n = Notification.Builder(context, CHANNEL)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(context.getString(R.string.reminder_title))
            .setContentText(text)
            .setAutoCancel(true)
            .setContentIntent(MainActivity.openIntent(context, quick = true))
            .build()
        nm.notify(ID, n)
    }
}

class ReminderReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val app = TasbihApp.get(context)
        val done = goAsync()
        app.scope.launch {
            try {
                val s = app.settings.current()
                // qayta yuklanish — faqat qayta rejalash
                if (intent.action == null) {
                    val today = LocalDate.now()
                    val total = Stats(app.dao.sessionsOnce()).total(today, today)
                    val text = when {
                        s.dailyGoal > 0 && total < s.dailyGoal -> context.getString(R.string.reminder_goal, fmt(s.dailyGoal - total))
                        s.dailyGoal == 0 && total == 0L -> context.getString(R.string.reminder_none)
                        else -> null
                    }
                    text?.let { Reminder.show(context, it) }
                }
                Reminder.schedule(context, s.reminderMin)
            } finally {
                done.finish()
            }
        }
    }
}
