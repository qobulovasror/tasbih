package uz.asrorqobulov.tasbih

import android.app.Application
import android.app.LocaleManager
import android.content.Context
import android.os.Build
import android.os.LocaleList
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.conflate
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import uz.asrorqobulov.tasbih.data.SettingsRepo
import uz.asrorqobulov.tasbih.data.Stats
import uz.asrorqobulov.tasbih.data.TasbihDb
import uz.asrorqobulov.tasbih.data.defaultTemplates
import uz.asrorqobulov.tasbih.reminder.Reminder
import uz.asrorqobulov.tasbih.session.SessionManager
import uz.asrorqobulov.tasbih.widget.TodayWidget

class TasbihApp : Application() {
    val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    val dao by lazy { TasbihDb.create(this).dao() }
    val settings by lazy { SettingsRepo(this) }
    lateinit var session: SessionManager
        private set

    /** Tez boshlash so'rovi — Asosiy oynada oxirgi shablon varag'i ochiladi. */
    val quickStart = MutableStateFlow(false)

    override fun onCreate() {
        super.onCreate()
        session = SessionManager(this, dao, settings, scope)
        // default til — o'zbekcha
        if (Build.VERSION.SDK_INT >= 33) {
            val lm = getSystemService(LocaleManager::class.java)
            if (lm.applicationLocales.isEmpty) lm.applicationLocales = LocaleList.forLanguageTags("uz")
        }
        scope.launch {
            dao.closeDangling()
            if (!settings.current().onboardingDone && dao.templatesOnce().isEmpty()) {
                dao.upsertTemplates(defaultTemplates())
            }
        }
        scope.launch {
            settings.flow.map { it.reminderMin }.distinctUntilChanged().collect { Reminder.schedule(this@TasbihApp, it) }
        }
        // vidjet sessiya davomida ham yangilanadi, lekin 1.5 s dan tez emas
        scope.launch {
            dao.sessions().combine(settings.flow.map { it.dailyGoal }.distinctUntilChanged()) { s, g -> s to g }
                .conflate()
                .collect { (s, g) -> TodayWidget.render(this@TasbihApp, Stats(s), g); delay(1500) }
        }
    }

    companion object {
        fun get(context: Context) = context.applicationContext as TasbihApp
    }
}
