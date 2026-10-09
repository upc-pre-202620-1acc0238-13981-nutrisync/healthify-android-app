package pe.edu.upc.healthify.features.nutritionalcare.infrastructure.remote

import pe.edu.upc.healthify.features.nutritionalcare.infrastructure.remote.dto.ConsultationDto
import pe.edu.upc.healthify.features.nutritionalcare.infrastructure.remote.dto.DiagnosisRequestDto
import pe.edu.upc.healthify.features.nutritionalcare.infrastructure.remote.dto.DiagnosisSuggestionDto
import pe.edu.upc.healthify.features.nutritionalcare.infrastructure.remote.dto.GuidelineSuggestionsDto
import pe.edu.upc.healthify.features.nutritionalcare.infrastructure.remote.dto.MeasurementRequestDto
import pe.edu.upc.healthify.features.nutritionalcare.infrastructure.remote.dto.NutritionPlanDto
import pe.edu.upc.healthify.features.nutritionalcare.infrastructure.remote.dto.PatientBaselineDto
import pe.edu.upc.healthify.features.nutritionalcare.infrastructure.remote.dto.PatientBaselineRequestDto
import pe.edu.upc.healthify.features.nutritionalcare.infrastructure.remote.dto.PatientSummaryDto
import pe.edu.upc.healthify.features.nutritionalcare.infrastructure.remote.dto.PractitionerRecordDto
import pe.edu.upc.healthify.features.nutritionalcare.infrastructure.remote.dto.PrescribeTargetsRequestDto
import pe.edu.upc.healthify.features.nutritionalcare.infrastructure.remote.dto.PublicationRequestDto
import pe.edu.upc.healthify.features.nutritionalcare.infrastructure.remote.dto.StartConsultationRequestDto
import pe.edu.upc.healthify.features.nutritionalcare.infrastructure.remote.dto.TargetProposalDto
import pe.edu.upc.healthify.features.nutritionalcare.infrastructure.remote.dto.TargetProposalRequestDto
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.POST
import retrofit2.http.PUT
import retrofit2.http.Path
import retrofit2.http.Query

/** NC-1 · datos base (solo `Practitioner`; `403` sin vínculo activo). */
interface PatientBaselineService {

    /** `404 BaselineNotFound` antes de registrarlos (PAC-0). */
    @GET("patients/{patientId}/baseline")
    suspend fun get(@Path("patientId") patientId: Long): PatientBaselineDto

    /** `201` · `409 BaselineAlreadyRecorded` · `400 InvalidBirthDate`/`InvalidHeight`/`UnknownMedicalCondition`. */
    @POST("patients/{patientId}/baseline")
    suspend fun record(@Path("patientId") patientId: Long, @Body body: PatientBaselineRequestDto): PatientBaselineDto

    @PUT("patients/{patientId}/baseline")
    suspend fun update(@Path("patientId") patientId: Long, @Body body: PatientBaselineRequestDto): PatientBaselineDto
}

/** NC-2 a NC-7 · la consulta guiada (solo `Practitioner`). */
interface ConsultationService {

    /** `201` · `409 ConsultationAlreadyInProgress` · `422 BaselineRequired`. */
    @POST("patients/{patientId}/consultations")
    suspend fun start(@Path("patientId") patientId: Long, @Body body: StartConsultationRequestDto): ConsultationDto

    /** `404 ConsultationNotFound` si no hay una en curso. */
    @GET("patients/{patientId}/consultations/in-progress")
    suspend fun getInProgress(@Path("patientId") patientId: Long): ConsultationDto

    @PUT("consultations/{consultationId}/measurement")
    suspend fun recordMeasurement(
        @Path("consultationId") consultationId: Long,
        @Body body: MeasurementRequestDto,
    ): ConsultationDto

    @POST("consultations/{consultationId}/diagnosis-suggestion")
    suspend fun suggestDiagnosis(@Path("consultationId") consultationId: Long): DiagnosisSuggestionDto

    @PUT("consultations/{consultationId}/diagnosis")
    suspend fun issueDiagnosis(
        @Path("consultationId") consultationId: Long,
        @Body body: DiagnosisRequestDto,
    ): ConsultationDto

    @POST("consultations/{consultationId}/target-proposal")
    suspend fun proposeTargets(
        @Path("consultationId") consultationId: Long,
        @Body body: TargetProposalRequestDto,
    ): TargetProposalDto

    /** `400 OverrideReasonRequired` si se escriben valores propios sin razón. */
    @PUT("consultations/{consultationId}/targets")
    suspend fun prescribeTargets(
        @Path("consultationId") consultationId: Long,
        @Body body: PrescribeTargetsRequestDto,
    ): ConsultationDto

    @POST("consultations/{consultationId}/guideline-suggestions")
    suspend fun suggestGuidelines(@Path("consultationId") consultationId: Long): GuidelineSuggestionsDto

    @PUT("consultations/{consultationId}/publication-draft")
    suspend fun saveDraft(
        @Path("consultationId") consultationId: Long,
        @Body body: PublicationRequestDto,
    ): ConsultationDto

    /** Con la misma `Idempotency-Key` el backend devuelve el mismo resultado: nunca publica dos versiones. */
    @POST("consultations/{consultationId}/publication")
    suspend fun publish(
        @Path("consultationId") consultationId: Long,
        @Header("Idempotency-Key") idempotencyKey: String,
        @Body body: PublicationRequestDto,
    ): ConsultationDto
}

/** Vistas del paciente para el profesional (RM-2, RM-4, PAC-4). `403` sin vínculo activo. */
interface PractitionerPatientService {

    @GET("patients/{patientId}/summary")
    suspend fun getSummary(@Path("patientId") patientId: Long): PatientSummaryDto

    @GET("patients/{patientId}/record")
    suspend fun getRecord(@Path("patientId") patientId: Long, @Query("days") days: Int): PractitionerRecordDto

    @GET("patients/{patientId}/nutrition-plans")
    suspend fun getPlans(@Path("patientId") patientId: Long): List<NutritionPlanDto>
}
