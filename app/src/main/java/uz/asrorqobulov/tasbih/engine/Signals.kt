package uz.asrorqobulov.tasbih.engine

enum class Signal { COUNT, STEP, TARGET }

/** Yangi hisob uchun qaysi signal: maqsad, oraliq qadam yoki oddiy +1. target/step = 0 — yo'q. */
fun signalFor(count: Long, target: Long, step: Int): Signal = when {
    target > 0 && count == target -> Signal.TARGET
    step > 0 && count > 0 && count % step == 0L -> Signal.STEP
    else -> Signal.COUNT
}
