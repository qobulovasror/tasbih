package uz.asrorqobulov.tasbih.service

import android.os.Build
import android.service.quicksettings.Tile
import android.service.quicksettings.TileService
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import uz.asrorqobulov.tasbih.R
import uz.asrorqobulov.tasbih.TasbihApp
import uz.asrorqobulov.tasbih.data.Stats
import uz.asrorqobulov.tasbih.session.State
import uz.asrorqobulov.tasbih.ui.MainActivity
import uz.asrorqobulov.tasbih.ui.fmt
import java.time.LocalDate

/** Quick Settings tugmasi (PLAN 11.3): ostida bugungi son, bosilganda — tez boshlash. */
class QuickTile : TileService() {
    override fun onStartListening() {
        val app = TasbihApp.get(this)
        app.scope.launch {
            val today = LocalDate.now()
            val n = Stats(app.dao.sessionsOnce()).total(today, today)
            withContext(Dispatchers.Main) {
                qsTile?.apply {
                    state = if (app.session.ui.value.state != State.IDLE) Tile.STATE_ACTIVE else Tile.STATE_INACTIVE
                    subtitle = getString(R.string.today) + ": " + fmt(n)
                    updateTile()
                }
            }
        }
    }

    override fun onClick() {
        if (Build.VERSION.SDK_INT >= 34) {
            startActivityAndCollapse(MainActivity.openIntent(this, quick = true))
        } else {
            @Suppress("DEPRECATION")
            startActivityAndCollapse(MainActivity.quickIntent(this))
        }
    }
}
