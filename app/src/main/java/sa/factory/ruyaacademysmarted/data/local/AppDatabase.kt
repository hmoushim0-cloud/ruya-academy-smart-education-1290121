package sa.factory.ruyaacademysmarted.data.local

import androidx.room.Database
import androidx.room.RoomDatabase

@Database(entities = [RuyaAcademyEntity::class], version = 1, exportSchema = false)
abstract class AppDatabase : RoomDatabase() {
    abstract fun smart_educationDao(): RuyaAcademyDao
}
