package uz.asrorqobulov.tasbih.engine

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import uz.asrorqobulov.tasbih.engine.KeyInterpreter.Action

class EngineTest {
    private val up = 24
    private val dn = 25

    @Test fun singlePressCountsOnUp() {
        val k = KeyInterpreter()
        assertEquals(emptyList<Action>(), k.keyDown(up, 0, 0))
        assertEquals(listOf(Action.COUNT), k.keyUp(up, 100))
    }

    @Test fun autoRepeatCountsOnce() {
        val k = KeyInterpreter()
        k.keyDown(dn, 0, 0)
        k.keyDown(dn, 500, 1)
        k.keyDown(dn, 550, 2)
        assertEquals(listOf(Action.COUNT), k.keyUp(dn, 900))
    }

    @Test fun fastDoublePressBothCountButBounceDropped() {
        val k = KeyInterpreter()
        k.keyDown(up, 0, 0); assertEquals(listOf(Action.COUNT), k.keyUp(up, 60))
        k.keyDown(up, 120, 0); assertEquals(listOf(Action.COUNT), k.keyUp(up, 180))
        k.keyDown(up, 200, 0); assertEquals(emptyList<Action>(), k.keyUp(up, 210))
    }

    @Test fun shortComboTogglesPauseAndDoesNotCount() {
        val k = KeyInterpreter()
        k.keyDown(up, 0, 0); k.keyDown(dn, 30, 0)
        assertEquals(emptyList<Action>(), k.keyUp(up, 300))
        assertEquals(listOf(Action.TOGGLE_PAUSE), k.keyUp(dn, 320))
    }

    @Test fun longComboWarnsThenEnds() {
        val k = KeyInterpreter()
        k.keyDown(up, 0, 0); k.keyDown(dn, 10, 0)
        assertEquals(emptyList<Action>(), k.tick(1000))
        assertEquals(listOf(Action.END_WARNING), k.tick(1600))
        assertEquals(emptyList<Action>(), k.tick(1700))
        assertEquals(listOf(Action.END), k.tick(3100))
        assertEquals(emptyList<Action>(), k.keyUp(up, 3200))
        assertEquals(emptyList<Action>(), k.keyUp(dn, 3210))
    }

    @Test fun comboReleasedAfterWarningCancels() {
        val k = KeyInterpreter()
        k.keyDown(up, 0, 0); k.keyDown(dn, 10, 0)
        k.tick(1600)
        k.keyUp(up, 2000)
        assertEquals(emptyList<Action>(), k.keyUp(dn, 2010))
        k.keyDown(up, 2500, 0)
        assertEquals(listOf(Action.COUNT), k.keyUp(up, 2600))
    }

    @Test fun endFiresOnReleaseIfTimerMissed() {
        val k = KeyInterpreter()
        k.keyDown(up, 0, 0); k.keyDown(dn, 10, 0)
        assertEquals(listOf(Action.END), k.keyUp(up, 3500))
        assertEquals(emptyList<Action>(), k.keyUp(dn, 3510))
    }

    @Test fun powerFilter() {
        val f = PowerFilter()
        val noKey = Long.MIN_VALUE / 2
        assertTrue(f.onScreen(false, 10_000, noKey, false))
        assertFalse(f.onScreen(true, 10_100, noKey, false)) // juda tez
        assertTrue(f.onScreen(false, 11_000, noKey, false))
        f.onNotification(20_000)
        assertFalse(f.onScreen(true, 20_500, noKey, false)) // bildirishnoma yoqdi
        assertTrue(f.onScreen(false, 23_000, noKey, false))
        assertFalse(f.onScreen(true, 24_000, 23_800, false)) // screenshot
        assertFalse(f.onScreen(false, 26_000, noKey, true))
        assertTrue(f.onScreen(true, 27_000, noKey, false))
    }

    @Test fun signals() {
        assertEquals(Signal.COUNT, signalFor(1, 33, 0))
        assertEquals(Signal.TARGET, signalFor(33, 33, 0))
        assertEquals(Signal.COUNT, signalFor(34, 33, 0))
        assertEquals(Signal.STEP, signalFor(33, 99, 33))
        assertEquals(Signal.TARGET, signalFor(99, 99, 33))
        assertEquals(Signal.STEP, signalFor(132, 99, 33))
        assertEquals(Signal.STEP, signalFor(66, 0, 33))
        assertEquals(Signal.COUNT, signalFor(5, 0, 0))
    }
}
