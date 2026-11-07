package sa.factory.ruyaacademysmarted.domain.usecase

import sa.factory.ruyaacademysmarted.domain.repository.RuyaAcademyRepository
import sa.factory.ruyaacademysmarted.model.RuyaAcademyItem
import sa.factory.ruyaacademysmarted.model.RuyaAcademyOrder
import sa.factory.ruyaacademysmarted.model.RuyaAcademyTelemetry
import kotlinx.coroutines.flow.Flow

class GetCatalogUseCase(private val repository: RuyaAcademyRepository) {
    operator fun invoke(): Flow<List<RuyaAcademyItem>> = repository.catalogFlow
}

class ManageCartUseCase(private val repository: RuyaAcademyRepository) {
    val cart = repository.activeCart
    fun add(item: RuyaAcademyItem) = repository.addToCart(item)
    fun remove(item: RuyaAcademyItem) = repository.removeFromCart(item)
    fun clear() = repository.clearCart()
}

class ProcessCheckoutUseCase(private val repository: RuyaAcademyRepository) {
    suspend operator fun invoke(paymentMethod: String = "Mada Contactless EMV"): RuyaAcademyOrder =
        repository.checkout(paymentMethod)
}

class ObserveHardwareTelemetryUseCase(private val repository: RuyaAcademyRepository) {
    operator fun invoke(): Flow<RuyaAcademyTelemetry> = repository.telemetryFlow
}
