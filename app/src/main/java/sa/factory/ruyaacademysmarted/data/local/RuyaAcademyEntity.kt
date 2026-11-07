package sa.factory.ruyaacademysmarted.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey
import sa.factory.ruyaacademysmarted.model.RuyaAcademyItem

@Entity(tableName = "smart_education_items_cache")
data class RuyaAcademyEntity(
    @PrimaryKey val id: String,
    val title: String,
    val description: String,
    val priceSar: Double,
    val badge: String,
    val category: String,
    val status: String
) {
    fun toDomain(): RuyaAcademyItem = RuyaAcademyItem(
        id = id,
        title = title,
        description = description,
        priceSar = priceSar,
        badge = badge,
        category = category,
        status = status
    )
}
