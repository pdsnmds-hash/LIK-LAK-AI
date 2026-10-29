package bg.liklak.sandbox

import android.content.Context
import androidx.room.*

@Entity(tableName="saved")
data class SavedEntry(@PrimaryKey val key: String, val caseId: String, val kind: String, val input: String, val label: String, val created: Long=System.currentTimeMillis())
@Dao interface SavedDao {
 @Query("SELECT * FROM saved ORDER BY created DESC") fun observe(): kotlinx.coroutines.flow.Flow<List<SavedEntry>>
 @Insert(onConflict=OnConflictStrategy.REPLACE) suspend fun save(entry: SavedEntry)
 @Query("DELETE FROM saved WHERE `key`=:key") suspend fun delete(key: String)
 @Query("DELETE FROM saved") suspend fun clear()
}
@Database(entities=[SavedEntry::class],version=1,exportSchema=true)
abstract class SandboxDatabase: RoomDatabase() {
 abstract fun saved(): SavedDao
 companion object { fun open(context: Context) = Room.databaseBuilder(context,SandboxDatabase::class.java,"sandbox.db").build() }
}
