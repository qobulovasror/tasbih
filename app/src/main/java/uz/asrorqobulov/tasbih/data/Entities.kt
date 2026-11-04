package uz.asrorqobulov.tasbih.data

import androidx.room.Entity
import androidx.room.PrimaryKey
import kotlinx.serialization.Serializable
import java.util.UUID

enum class CountMode { DEVICE, TAP }
enum class TargetSignal { VIBRATION, SOUND, BOTH }

/** Shablon: maqsad = partSize × parts; partSize = 0 — cheksiz (PLAN 8.3). */
@Serializable
@Entity(tableName = "templates")
data class Template(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val name: String = "",
    val partSize: Int,
    val parts: Int = 1,
    val mode: CountMode = CountMode.DEVICE,
    val signal: TargetSignal = TargetSignal.VIBRATION,
    val count: Long = 0,
    val position: Int = 0,
    val zikrId: String? = null,
) {
    val target: Long get() = partSize.toLong() * parts
    val title: String
        get() = name.ifBlank {
            when {
                partSize == 0 -> "∞"
                parts > 1 -> "$partSize × $parts"
                else -> "$partSize"
            }
        }
}

@Serializable
@Entity(tableName = "sessions")
data class SessionRecord(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val templateId: String,
    val templateTitle: String,
    val startedAt: Long,
    val updatedAt: Long,
    val endedAt: Long? = null,
    val activeMs: Long = 0,
    val counted: Long = 0,
    val startCount: Long,
    val endCount: Long,
    val target: Long,
    val reached: Boolean = false,
    val fromTap: Long = 0,
    val fromVolume: Long = 0,
    val fromPower: Long = 0,
)

fun defaultTemplates(): List<Template> = listOf(
    Template(partSize = 33),
    Template(partSize = 33, parts = 3, mode = CountMode.TAP),
    Template(partSize = 99),
    Template(partSize = 100),
    Template(partSize = 1000),
    Template(partSize = 0, mode = CountMode.TAP),
).mapIndexed { i, t -> t.copy(position = i) }
