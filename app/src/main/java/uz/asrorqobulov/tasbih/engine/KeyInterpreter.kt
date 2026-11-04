package uz.asrorqobulov.tasbih.engine

/**
 * Volume tugmalar oqimini amallarga aylantiradi (PLAN 5.1).
 * Vaqtlar — KeyEvent.eventTime (ms). Bitta bosish ACTION_UP da sanaladi, ikkala tugma birga — buyruq.
 */
class KeyInterpreter(
    private val warnAfterMs: Long = 1500,
    private val endAfterMs: Long = 3000,
    private val minGapMs: Long = 80,
) {
    enum class Action { COUNT, TOGGLE_PAUSE, END_WARNING, END }

    private val down = mutableMapOf<Int, Long>()
    private var comboStart = -1L
    private var warned = false
    private var ended = false
    private var lastCount = Long.MIN_VALUE / 2

    val anyDown get() = down.isNotEmpty()
    val comboActive get() = comboStart >= 0

    fun keyDown(code: Int, time: Long, repeat: Int): List<Action> {
        if (repeat > 0 || code in down) return emptyList()
        down[code] = time
        if (down.size >= 2 && comboStart < 0) {
            comboStart = time
            warned = false
            ended = false
        }
        return emptyList()
    }

    /** Kombinatsiya ushlab turilganda taymer orqali chaqiriladi. */
    fun tick(time: Long): List<Action> {
        if (comboStart < 0 || down.size < 2 || ended) return emptyList()
        val held = time - comboStart
        return when {
            held >= endAfterMs -> { ended = true; listOf(Action.END) }
            held >= warnAfterMs && !warned -> { warned = true; listOf(Action.END_WARNING) }
            else -> emptyList()
        }
    }

    fun keyUp(code: Int, time: Long): List<Action> {
        if (code !in down) return emptyList()
        if (comboStart >= 0) {
            val out = if (down.size >= 2) tick(time).toMutableList() else mutableListOf()
            down.remove(code)
            if (down.isEmpty()) {
                // 1.5 s dan keyin qo'yib yuborilsa — tugatish bekor, pauza ham emas
                if (!ended && time - comboStart < warnAfterMs) out += Action.TOGGLE_PAUSE
                comboStart = -1
            }
            return out
        }
        down.remove(code)
        if (time - lastCount < minGapMs) return emptyList()
        lastCount = time
        return listOf(Action.COUNT)
    }

    fun reset() {
        down.clear()
        comboStart = -1
    }
}
