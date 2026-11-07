package sa.factory.ruyaacademysmarted.data.remote

import sa.factory.ruyaacademysmarted.model.RuyaAcademyItem
import sa.factory.ruyaacademysmarted.model.RuyaAcademyOrder

interface RuyaAcademyApiService {
    suspend fun fetchCatalog(): List<RuyaAcademyItem>
    suspend fun submitOrder(order: RuyaAcademyOrder): Boolean
}
