package sa.factory.ruyaacademysmarted.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface RuyaAcademyDao {
    @Query("SELECT * FROM smart_education_items_cache ORDER BY id ASC")
    fun observeAllItems(): Flow<List<RuyaAcademyEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertItems(items: List<RuyaAcademyEntity>)

    @Query("DELETE FROM smart_education_items_cache")
    suspend fun clearAll()
}
