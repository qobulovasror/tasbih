package uz.asrorqobulov.tasbih.data

import android.content.Context
import androidx.room.Dao
import androidx.room.Database
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.Transaction
import androidx.room.Upsert
import kotlinx.coroutines.flow.Flow

@Dao
interface TasbihDao {
    @Query("SELECT * FROM templates ORDER BY position")
    fun templates(): Flow<List<Template>>

    @Query("SELECT * FROM templates ORDER BY position")
    suspend fun templatesOnce(): List<Template>

    @Query("SELECT * FROM templates WHERE id = :id")
    suspend fun template(id: String): Template?

    @Upsert suspend fun upsert(t: Template)
    @Upsert suspend fun upsertTemplates(t: List<Template>)

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertTemplatesIgnore(t: List<Template>)

    @Query("DELETE FROM templates WHERE id = :id")
    suspend fun deleteTemplate(id: String)

    @Query("UPDATE templates SET count = :count WHERE id = :id")
    suspend fun setCount(id: String, count: Long)

    @Query("SELECT * FROM sessions ORDER BY startedAt DESC")
    fun sessions(): Flow<List<SessionRecord>>

    @Query("SELECT * FROM sessions ORDER BY startedAt")
    suspend fun sessionsOnce(): List<SessionRecord>

    @Upsert suspend fun upsertSession(s: SessionRecord)

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertSessionsIgnore(s: List<SessionRecord>)

    /** Jarayon o'lib qolgan sessiyalarni yopish. */
    @Query("UPDATE sessions SET endedAt = updatedAt WHERE endedAt IS NULL")
    suspend fun closeDangling()

    @Query("DELETE FROM sessions") suspend fun clearSessions()
    @Query("DELETE FROM templates") suspend fun clearTemplates()

    @Transaction
    suspend fun replaceAll(t: List<Template>, s: List<SessionRecord>) {
        clearSessions(); clearTemplates()
        upsertTemplates(t)
        insertSessionsIgnore(s)
    }
}

@Database(entities = [Template::class, SessionRecord::class], version = 1)
abstract class TasbihDb : RoomDatabase() {
    abstract fun dao(): TasbihDao

    companion object {
        fun create(context: Context) =
            Room.databaseBuilder(context, TasbihDb::class.java, "tasbih.db").build()
    }
}
