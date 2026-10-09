package pe.edu.upc.healthify.features.foodcatalog

import app.cash.turbine.test
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import pe.edu.upc.healthify.core.common.error.DomainError
import pe.edu.upc.healthify.features.foodcatalog.application.usecase.BrowseFoodCatalogUseCase
import pe.edu.upc.healthify.features.foodcatalog.application.usecase.CreateLocalFoodUseCase
import pe.edu.upc.healthify.features.foodcatalog.domain.entity.LocalFoodField
import pe.edu.upc.healthify.features.foodcatalog.domain.entity.LocalFoodValidation
import pe.edu.upc.healthify.features.foodcatalog.infrastructure.mapper.toDto
import pe.edu.upc.healthify.features.foodcatalog.infrastructure.remote.dto.CreateLocalOverrideRequestDto
import pe.edu.upc.healthify.features.foodcatalog.presentation.viewmodel.AddLocalFoodEvent
import pe.edu.upc.healthify.features.foodcatalog.presentation.viewmodel.AddLocalFoodViewModel
import pe.edu.upc.healthify.features.foodcatalog.presentation.viewmodel.FoodCatalogViewModel
import pe.edu.upc.healthify.testing.FakeConnectivityObserver
import pe.edu.upc.healthify.testing.FakeReferenceFoodRepository
import pe.edu.upc.healthify.testing.MainDispatcherRule
import pe.edu.upc.healthify.testing.failureOf
import pe.edu.upc.healthify.testing.referenceFood

@OptIn(ExperimentalCoroutinesApi::class)
class LocalFoodCatalogTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val foods = FakeReferenceFoodRepository()
    private val connectivity = FakeConnectivityObserver()

    @Test
    fun `a local food needs a name with letters and nutrients within the backend ranges`() {
        val invalid = LocalFoodValidation.of("123", "", "120", "20", "-1") as LocalFoodValidation.Invalid
        assertEquals(setOf(LocalFoodField.NAME, LocalFoodField.ENERGY, LocalFoodField.PROTEIN, LocalFoodField.FAT), invalid.fields)

        val valid = LocalFoodValidation.of(" Chaufa de cuy ", "180", "12,5", "20", "6") as LocalFoodValidation.Valid
        assertEquals(
            CreateLocalOverrideRequestDto("Chaufa de cuy", 180.0, 12.5, 20.0, 6.0),
            valid.food.toDto(),
        )
        assertTrue(LocalFoodValidation.of("Cuy", "951", "1", "1", "1") is LocalFoodValidation.Invalid)
    }

    @Test
    fun `PR15 lists the catalog without text and searches after a short pause`() = runTest {
        foods.remote = Result.success(listOf(referenceFood(1, "Papa amarilla"), referenceFood(2, "Cuy al horno", override = true)))
        val viewModel = FoodCatalogViewModel(BrowseFoodCatalogUseCase(foods), connectivity)
        assertEquals(listOf<String?>(null), foods.browsedTerms)
        assertEquals(listOf(false, true), viewModel.state.value.results?.map { it.isLocal })

        viewModel.onQueryChange("c")
        viewModel.onQueryChange("cuy")
        advanceTimeBy(301)
        assertEquals(listOf(null, "cuy"), foods.browsedTerms)
    }

    @Test
    fun `PR15 offline without results needs a connection`() {
        foods.remote = failureOf(DomainError.Network)

        val viewModel = FoodCatalogViewModel(BrowseFoodCatalogUseCase(foods), connectivity)

        assertTrue(viewModel.state.value.needsConnection)
    }

    @Test
    fun `PR15_1 a duplicated name is shown on the name field and a valid food is created`() = runTest {
        val viewModel = AddLocalFoodViewModel(CreateLocalFoodUseCase(foods), connectivity)
        viewModel.onSave()
        assertEquals(LocalFoodField.entries.toSet(), viewModel.state.value.invalidFields)

        viewModel.onNameChange("Cuy al horno")
        viewModel.onEnergyChange("176")
        viewModel.onProteinChange("19")
        viewModel.onCarbChange("0")
        viewModel.onFatChange("11")
        foods.createResult = failureOf(DomainError.Conflict("DuplicatedLocalOverride"))
        viewModel.onSave()
        assertTrue(viewModel.state.value.duplicatedName)

        viewModel.onNameChange("Cuy chactado")
        assertFalse(viewModel.state.value.duplicatedName)
        foods.createResult = null
        viewModel.events.test {
            viewModel.onSave()
            assertEquals(AddLocalFoodEvent.Added("Cuy chactado"), awaitItem())
        }
    }
}
