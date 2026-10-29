package pe.edu.upc.healthify.core.network

import kotlinx.coroutines.CancellationException
import kotlinx.serialization.SerializationException
import pe.edu.upc.healthify.core.common.error.DomainError
import pe.edu.upc.healthify.core.common.error.domainFailure
import retrofit2.HttpException
import java.io.IOException

/**
 * Runs a Retrofit call and turns it into a `Result`. Use it in the `*RepositoryImpl`
 * (infrastructure) together with the DTO → domain mapper:
 *
 * ```
 * override suspend fun getUser(id: UserId) = apiCall { service.getUser(id.value) }.map { it.toDomain() }
 * ```
 *
 * Never swallows `CancellationException`.
 */
suspend fun <T> apiCall(block: suspend () -> T): Result<T> =
    try {
        Result.success(block())
    } catch (e: CancellationException) {
        throw e
    } catch (e: HttpException) {
        domainFailure(ProblemDetailsMapper.map(e.code(), e.response()?.errorBody()?.string()))
    } catch (_: IOException) {
        domainFailure(DomainError.Network)
    } catch (_: SerializationException) {
        domainFailure(DomainError.Unexpected("MALFORMED_RESPONSE"))
    }
