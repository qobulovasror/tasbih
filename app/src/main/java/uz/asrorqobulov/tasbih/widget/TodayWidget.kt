package uz.asrorqobulov.tasbih.widget

import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.view.View
import android.widget.RemoteViews
import kotlinx.coroutines.launch
import uz.asrorqobulov.tasbih.R
import uz.asrorqobulov.tasbih.TasbihApp
import uz.asrorqobulov.tasbih.data.Stats
import uz.asrorqobulov.tasbih.ui.MainActivity
import uz.asrorqobulov.tasbih.ui.fmt
import java.time.LocalDate

/** Bosh ekran vidjeti (PLAN 11.2): bugungi son, maqsad yoki seriya, ▶ — tez boshlash. */
class TodayWidget : AppWidgetProvider() {
    override fun onUpdate(context: Context, manager: AppWidgetManager, ids: IntArray) {
        val app = TasbihApp.get(context)
        app.scope.launch { render(context, Stats(app.dao.sessionsOnce()), app.settings.current().dailyGoal) }
    }

    companion object {
        fun render(context: Context, stats: Stats, goal: Int) {
            val m = AppWidgetManager.getInstance(context)
            val ids = m.getAppWidgetIds(ComponentName(context, TodayWidget::class.java))
            if (ids.isEmpty()) return
            val today = LocalDate.now()
            val n = stats.total(today, today)
            val v = RemoteViews(context.packageName, R.layout.widget_today).apply {
                setTextViewText(R.id.count, fmt(n))
                if (goal > 0) {
                    setViewVisibility(R.id.progress, View.VISIBLE)
                    setProgressBar(R.id.progress, goal, n.coerceAtMost(goal.toLong()).toInt(), false)
                    setTextViewText(
                        R.id.sub,
                        if (n >= goal) "✓ " + context.getString(R.string.goal_done) else "/ " + fmt(goal.toLong()),
                    )
                } else {
                    setViewVisibility(R.id.progress, View.GONE)
                    setTextViewText(R.id.sub, "🔥 ${stats.streak(today)} " + context.getString(R.string.days))
                }
                setOnClickPendingIntent(R.id.root, MainActivity.openIntent(context, quick = false))
                setOnClickPendingIntent(R.id.play, MainActivity.openIntent(context, quick = true))
            }
            m.updateAppWidget(ids, v)
        }
    }
}
