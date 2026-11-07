package sa.factory.ruyaacademysmarted.data.repository

import sa.factory.ruyaacademysmarted.domain.repository.RuyaAcademyRepository
import sa.factory.ruyaacademysmarted.hardware.HardwareSensorDriver
import sa.factory.ruyaacademysmarted.model.RuyaAcademyItem
import sa.factory.ruyaacademysmarted.model.RuyaAcademyOrder
import sa.factory.ruyaacademysmarted.model.RuyaAcademyTelemetry
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

class RuyaAcademyRepositoryImpl(
    private val sensorDriver: HardwareSensorDriver
) : RuyaAcademyRepository {

    private val _catalog = MutableStateFlow<List<RuyaAcademyItem>>(listOf(
        RuyaAcademyItem("1", "الباقة التنفيذية المعتمدة", "تكامل مباشر مع Smart Stylus Digitizer 4096-Level + BLE Beacon", 180.0, "معتمد", "الخدمات الرئيسية"),
        RuyaAcademyItem("2", "خدمة المعالجة الفورية", "أداء أصيل بسرعة 60 إطار في الثانية", 95.0, "فوري", "المراقبة الحية"),
        RuyaAcademyItem("3", "باقة الصيانة والدعم السحابي", "تشفير كامل للبيانات المحلية ومزامنة آمنة", 250.0, "شامل", "التقارير الضريبية")
    ))
    override val catalogFlow: Flow<List<RuyaAcademyItem>> = _catalog.asStateFlow()

    private val _cart = MutableStateFlow<List<RuyaAcademyItem>>(emptyList())
    override val activeCart: StateFlow<List<RuyaAcademyItem>> = _cart.asStateFlow()

    override val telemetryFlow: Flow<RuyaAcademyTelemetry> = sensorDriver.observePeripheralStream()
        .map { frame ->
            RuyaAcademyTelemetry(
                peripheralName = frame.peripheralName,
                status = frame.connectionState,
                liveReading = frame.livePayload,
                batteryPct = 96,
                signalDbm = -54
            )
        }

    override fun addToCart(item: RuyaAcademyItem) {
        val current = _cart.value.toMutableList()
        current.add(item)
        _cart.value = current
    }

    override fun removeFromCart(item: RuyaAcademyItem) {
        val current = _cart.value.toMutableList()
        current.remove(item)
        _cart.value = current
    }

    override fun clearCart() {
        _cart.value = emptyList()
    }

    override suspend fun checkout(paymentMethod: String): RuyaAcademyOrder {
        val currentItems = _cart.value.toList()
        val subtotal = currentItems.sumOf { it.priceSar }
        val vat = subtotal * 0.15
        val total = subtotal + vat
        val order = RuyaAcademyOrder(
            orderId = "ORD-${System.currentTimeMillis() % 100000}",
            items = currentItems,
            subtotalSar = subtotal,
            vatSar = vat,
            totalSar = total,
            paymentMethod = paymentMethod
        )
        clearCart()
        return order
    }

    override suspend fun refreshCatalog() {
        // Offline-first sync logic
    }
}
