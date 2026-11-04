package uz.asrorqobulov.tasbih.service

import android.accessibilityservice.AccessibilityService
import android.content.ComponentName
import android.content.Context
import android.provider.Settings
import android.view.KeyEvent
import android.view.accessibility.AccessibilityEvent
import uz.asrorqobulov.tasbih.TasbihApp

/** Volume tugmalar va bildirishnoma hodisalarini SessionManager'ga uzatadi. Mantiq u yerda. */
class CounterAccessibilityService : AccessibilityService() {

    override fun onKeyEvent(event: KeyEvent): Boolean {
        if (event.keyCode != KeyEvent.KEYCODE_VOLUME_UP && event.keyCode != KeyEvent.KEYCODE_VOLUME_DOWN) return false
        return TasbihApp.get(this).session.onVolumeKey(event)
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent) {
        if (event.eventType == AccessibilityEvent.TYPE_NOTIFICATION_STATE_CHANGED && event.packageName != packageName) {
            TasbihApp.get(this).session.onNotification()
        }
    }

    override fun onInterrupt() = Unit

    companion object {
        fun isEnabled(context: Context): Boolean {
            val enabled = Settings.Secure.getString(
                context.contentResolver, Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES,
            ) ?: return false
            val c = ComponentName(context, CounterAccessibilityService::class.java)
            return enabled.split(':').any {
                it.equals(c.flattenToString(), true) || it.equals(c.flattenToShortString(), true)
            }
        }
    }
}
