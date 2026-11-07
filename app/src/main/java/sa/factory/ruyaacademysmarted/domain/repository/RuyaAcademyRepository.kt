package sa.factory.ruyaacademysmarted.domain.repository

import sa.factory.ruyaacademysmarted.model.RuyaAcademyItem
import sa.factory.ruyaacademysmarted.model.RuyaAcademyOrder
import sa.factory.ruyaacademysmarted.model.RuyaAcademyTelemetry
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow

interface RuyaAcademyRepository {
    val catalogFlow: Flow<List<RuyaAcademyItem>>
    val activeCart: StateFlow<List<RuyaAcademyItem>>
    val telemetryFlow: Flow<RuyaAcademyTelemetry>
    
    fun addToCart(item: RuyaAcademyItem)
    fun removeFromCart(item: RuyaAcademyItem)
    fun clearCart()
    suspend fun checkout(paymentMethod: String): RuyaAcademyOrder
    suspend fun refreshCatalog()
}
