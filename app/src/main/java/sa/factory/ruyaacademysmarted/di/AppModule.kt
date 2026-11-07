package sa.factory.ruyaacademysmarted.di

import sa.factory.ruyaacademysmarted.data.repository.RuyaAcademyRepositoryImpl
import sa.factory.ruyaacademysmarted.domain.repository.RuyaAcademyRepository
import sa.factory.ruyaacademysmarted.domain.usecase.*
import sa.factory.ruyaacademysmarted.hardware.HardwareSensorDriver
import sa.factory.ruyaacademysmarted.ui.viewmodel.RuyaAcademyViewModel

class DiContainer {
    val sensorDriver = HardwareSensorDriver()
    val repository: RuyaAcademyRepository = RuyaAcademyRepositoryImpl(sensorDriver)

    val getCatalogUseCase = GetCatalogUseCase(repository)
    val manageCartUseCase = ManageCartUseCase(repository)
    val processCheckoutUseCase = ProcessCheckoutUseCase(repository)
    val observeHardwareTelemetryUseCase = ObserveHardwareTelemetryUseCase(repository)

    fun provideViewModel(): RuyaAcademyViewModel = RuyaAcademyViewModel(
        getCatalogUseCase,
        manageCartUseCase,
        processCheckoutUseCase,
        observeHardwareTelemetryUseCase
    )
}
