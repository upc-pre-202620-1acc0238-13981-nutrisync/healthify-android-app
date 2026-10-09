package pe.edu.upc.healthify.features.nutritionalcare

import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import pe.edu.upc.healthify.core.common.error.DomainError
import pe.edu.upc.healthify.core.common.error.domainFailure
import pe.edu.upc.healthify.features.iam.application.usecase.ObserveCurrentUserUseCase
import pe.edu.upc.healthify.features.nutritionalcare.application.usecase.GetOwnRecordUseCase
import pe.edu.upc.healthify.features.nutritionalcare.domain.entity.MyNumbers
import pe.edu.upc.healthify.features.nutritionalcare.domain.entity.PatientOwnRecord
import pe.edu.upc.healthify.features.nutritionalcare.domain.entity.RecordCompliance
import pe.edu.upc.healthify.features.nutritionalcare.domain.repository.PatientRecordRepository
import pe.edu.upc.healthify.features.nutritionalcare.domain.valueobject.PatientId
import pe.edu.upc.healthify.features.nutritionalcare.infrastructure.mapper.toDomain
import pe.edu.upc.healthify.features.nutritionalcare.infrastructure.remote.dto.PatientOwnRecordDto
import pe.edu.upc.healthify.features.nutritionalcare.infrastructure.remote.dto.RecordClinicalWeightDto
import pe.edu.upc.healthify.features.nutritionalcare.infrastructure.remote.dto.RecordComplianceDto
import pe.edu.upc.healthify.features.nutritionalcare.infrastructure.remote.dto.RecordGuidelineDto
import pe.edu.upc.healthify.features.nutritionalcare.infrastructure.remote.dto.RecordMyNumbersDto
import pe.edu.upc.healthify.features.nutritionalcare.infrastructure.remote.dto.RecordNextFollowUpDto
import pe.edu.upc.healthify.features.nutritionalcare.infrastructure.remote.dto.RecordPlanDto
import pe.edu.upc.healthify.features.nutritionalcare.infrastructure.remote.dto.RecordPractitionerDto
import pe.edu.upc.healthify.features.nutritionalcare.infrastructure.remote.dto.RecordReferralDto
import pe.edu.upc.healthify.features.nutritionalcare.presentation.viewmodel.PatientRecordViewModel
import pe.edu.upc.healthify.testing.FakeConnectivityObserver
import pe.edu.upc.healthify.testing.FakeSessionRepository
import pe.edu.upc.healthify.testing.MainDispatcherRule
import java.time.LocalDate

/** PT20 · Mi expediente (RM-4): cada sección que no se puede leer queda vacía sin tumbar el resto. */
class PatientRecordTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    @Test
    fun `a full own record is read section by section`() {
        val record = PatientOwnRecordDto(
            patientId = 12,
            practitioner = RecordPractitionerDto(4, " Lucía Ramos ", "2026-03-12T10:00:00-05:00", "Active"),
            nextFollowUp = RecordNextFollowUpDto(31, "2026-09-18T10:00:00-05:00"),
            myNumbers = RecordMyNumbersDto(
                energyTargetKcal = 1849.6,
                planVersion = 3,
                compliance = RecordComplianceDto("2026-09-09", "2026-09-15", met = 5, total = 7),
                clinicalWeight = RecordClinicalWeightDto(74.2, "2026-03-03T10:00:00-05:00"),
                weightSlopeKgPerWeek = -0.3,
            ),
            plan = RecordPlanDto(
                guidelines = listOf(RecordGuidelineDto(code = "ReduceSalt"), RecordGuidelineDto(custom = "Camina 30 min")),
                restrictions = listOf("ShellfishFree"),
            ),
            referrals = listOf(
                RecordReferralDto(1, "Endocrinología", "2026-08-01T10:00:00-05:00", "Closed"),
                RecordReferralDto(2, "Psicología", "2026-09-01T10:00:00-05:00", "Open"),
            ),
        ).toDomain()

        assertEquals("Lucía Ramos", record.practitioner!!.fullName)
        assertTrue(record.practitioner.isActive)
        assertEquals(31L, record.nextFollowUp!!.followUpId)
        assertEquals(1850, record.myNumbers.energyTargetKcal)
        assertEquals(7, record.myNumbers.compliance!!.rangeDays)
        assertEquals(74.2, record.myNumbers.clinicalWeight!!.kg, 0.0)
        assertEquals(2, record.plan!!.guidelines.size)
        assertEquals(listOf("Psicología"), record.openReferrals.map { it.specialty })
    }

    @Test
    fun `sections that failed on the backend or cannot be read stay empty`() {
        val record = PatientOwnRecordDto(
            patientId = 12,
            practitioner = null,
            nextFollowUp = RecordNextFollowUpDto(31, "no es fecha"),
            myNumbers = RecordMyNumbersDto(
                compliance = RecordComplianceDto("2026-09-15", "2026-09-09", met = 5, total = 7),
                clinicalWeight = RecordClinicalWeightDto(0.0, "2026-03-03T10:00:00-05:00"),
            ),
            plan = RecordPlanDto(),
            referrals = listOf(RecordReferralDto(1, " ", "2026-08-01T10:00:00-05:00")),
        ).toDomain()

        assertNull(record.practitioner)
        assertNull(record.nextFollowUp)
        assertTrue(record.myNumbers.isEmpty)
        assertNull(record.plan)
        assertTrue(record.referrals.isEmpty())
        assertEquals(MyNumbers.Empty, PatientOwnRecordDto(patientId = 12).toDomain().myNumbers)
    }

    @Test
    fun `compliance never counts more met days than days`() {
        assertThrows(IllegalArgumentException::class.java) {
            RecordCompliance(LocalDate.parse("2026-09-09"), LocalDate.parse("2026-09-15"), met = 8, total = 7)
        }
    }

    @Test
    fun `the whole record failing shows the error and offline shows the offline state`() = runTest {
        val repository = object : PatientRecordRepository {
            var result: Result<PatientOwnRecord> = domainFailure(DomainError.Network)
            override suspend fun getOwnRecord(patientId: PatientId) = result
        }
        val viewModel = PatientRecordViewModel(
            observeCurrentUser = ObserveCurrentUserUseCase(FakeSessionRepository()),
            getOwnRecord = GetOwnRecordUseCase(repository),
            connectivityObserver = FakeConnectivityObserver(),
        )

        assertTrue(viewModel.state.value.loadFailed)
        assertTrue(viewModel.state.value.isOffline)

        repository.result = Result.success(PatientOwnRecordDto(patientId = 12).toDomain())
        viewModel.onRetry()

        assertEquals(false, viewModel.state.value.loadFailed)
        assertTrue(viewModel.state.value.record!!.myNumbers.isEmpty)
    }
}
