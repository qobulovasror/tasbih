package uz.asrorqobulov.tasbih.engine

import org.junit.Assert.assertEquals
import org.junit.Test
import uz.asrorqobulov.tasbih.data.Backup
import uz.asrorqobulov.tasbih.data.BackupFile
import uz.asrorqobulov.tasbih.data.SessionRecord
import uz.asrorqobulov.tasbih.data.Stats
import uz.asrorqobulov.tasbih.data.defaultTemplates
import uz.asrorqobulov.tasbih.reminder.Reminder
import java.time.ZonedDateTime
import java.time.LocalDate
import java.time.ZoneOffset

class DataTest {
    private fun s(day: LocalDate, counted: Long) = SessionRecord(
        templateId = "t", templateTitle = "33",
        startedAt = day.atStartOfDay().toInstant(ZoneOffset.UTC).toEpochMilli() + 3_600_000,
        updatedAt = 0, startCount = 0, endCount = counted, target = 33, counted = counted,
    )

    @Test fun statsTotalsAndStreak() {
        val today = LocalDate.of(2026, 11, 1)
        val st = Stats(listOf(s(today, 100), s(today, 40), s(today.minusDays(1), 60), s(today.minusDays(3), 5)), ZoneOffset.UTC)
        assertEquals(140, st.total(today, today))
        assertEquals(205, st.total(today.minusDays(6), today))
        assertEquals(2, st.streak(today))
        assertEquals(1, Stats(listOf(s(today.minusDays(1), 1)), ZoneOffset.UTC).streak(today))
    }

    @Test fun statsBestAndLongest() {
        val d = LocalDate.of(2026, 11, 1)
        val st = Stats(listOf(s(d, 10), s(d.minusDays(1), 50), s(d.minusDays(2), 5), s(d.minusDays(5), 1), s(d.minusDays(6), 1)), ZoneOffset.UTC)
        assertEquals(d.minusDays(1) to 50L, st.best(d.minusDays(6), d))
        assertEquals(3, st.longestStreak(d.minusDays(6), d))
        assertEquals(2, st.longestStreak(d.minusDays(6), d.minusDays(3)))
        assertEquals(d.minusDays(6), st.firstDay())
    }

    @Test fun reminderNextTime() {
        val now = ZonedDateTime.of(2026, 11, 3, 20, 0, 0, 0, ZoneOffset.UTC)
        assertEquals(now.withHour(21), Reminder.nextAt(now, 21 * 60))
        assertEquals(now.withHour(9).plusDays(1), Reminder.nextAt(now, 9 * 60))
        assertEquals(now.plusDays(1), Reminder.nextAt(now, 20 * 60))
    }

    @Test fun backupRoundTrip() {
        val f = BackupFile(exportedAt = 1, templates = defaultTemplates(), sessions = listOf(s(LocalDate.of(2026, 1, 3), 7)))
        assertEquals(f, Backup.decode(Backup.encode(f)))
        assertEquals("33 × 3", defaultTemplates()[1].title)
    }
}
