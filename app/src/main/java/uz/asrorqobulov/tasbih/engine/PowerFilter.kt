package uz.asrorqobulov.tasbih.engine

/**
 * Ekran ON/OFF o'tishlaridan power bosishlarini ajratadi (PLAN 5.2). Har o'tish = +1.
 * Vaqtlar — SystemClock.elapsedRealtime (uyqu paytida ham yuradi).
 */
class PowerFilter(
    private val minGapMs: Long = 250,
    private val notificationQuietMs: Long = 1500,
    private val keyQuietMs: Long = 400,
) {
    private var lastTransition = Long.MIN_VALUE / 2
    private var lastNotification = Long.MIN_VALUE / 2

    fun onNotification(time: Long) {
        lastNotification = time
    }

    fun onScreen(on: Boolean, time: Long, lastVolumeKey: Long, volumeHeld: Boolean): Boolean {
        val prev = lastTransition
        lastTransition = time
        if (time - prev < minGapMs) return false
        if (on && time - lastNotification < notificationQuietMs) return false
        // Power + Vol = screenshot
        if (volumeHeld || time - lastVolumeKey < keyQuietMs) return false
        return true
    }
}
