package pe.edu.upc.healthify.core.network.auth

import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import kotlinx.serialization.json.Json
import mockwebserver3.Dispatcher
import mockwebserver3.MockResponse
import mockwebserver3.MockWebServer
import mockwebserver3.RecordedRequest
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.Response
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Before
import org.junit.Test
import pe.edu.upc.healthify.core.network.AcceptLanguageInterceptor
import pe.edu.upc.healthify.features.iam.infrastructure.remote.AuthenticationService
import pe.edu.upc.healthify.features.iam.infrastructure.remote.RetrofitTokenRefresher
import retrofit2.Retrofit
import retrofit2.converter.kotlinx.serialization.asConverterFactory
import java.io.IOException
import java.util.Collections
import java.util.concurrent.CountDownLatch
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicInteger

/**
 * Pila real de OkHttp (Accept-Language + AuthInterceptor + TokenRefreshAuthenticator) contra MockWebServer,
 * con el refresher real de `iam` (Retrofit anónimo).
 */
class TokenRefreshAuthenticatorTest {

    private lateinit var server: MockWebServer
    private lateinit var tokenStore: FakeSessionTokenStore
    private lateinit var expiryNotifier: SessionExpiryNotifier
    private lateinit var client: OkHttpClient

    private val refreshCalls = AtomicInteger()
    private val refreshBodies = Collections.synchronizedList(mutableListOf<String>())
    private val protectedAuthHeaders = Collections.synchronizedList(mutableListOf<String?>())

    /** Respuesta del endpoint de refresh; por defecto, el par nuevo. */
    @Volatile
    private var refreshResponse: () -> MockResponse = {
        json(200, """{"token":"$NEW_ACCESS","refreshToken":"$NEW_REFRESH","expiresAt":"2026-10-08T10:00:00Z"}""")
    }

    /** Bloquea los 401 hasta que lleguen N peticiones con el token viejo (para forzar concurrencia real). */
    @Volatile
    private var staleTokenGate: CountDownLatch? = null

    @Before
    fun setUp() {
        server = MockWebServer()
        server.dispatcher = object : Dispatcher() {
            override fun dispatch(request: RecordedRequest): MockResponse {
                val path = request.url.encodedPath
                return when {
                    path.endsWith("authentication/token-refreshes") -> {
                        refreshCalls.incrementAndGet()
                        refreshBodies += request.body?.utf8().orEmpty()
                        refreshResponse()
                    }
                    path.endsWith("authentication/sign-in") ->
                        json(401, """{"status":401,"code":"InvalidCredentials"}""")
                    else -> {
                        val auth = request.headers["Authorization"]
                        protectedAuthHeaders += auth
                        if (auth == "Bearer $NEW_ACCESS") {
                            json(200, """{"ok":true}""")
                        } else {
                            staleTokenGate?.let { gate ->
                                gate.countDown()
                                gate.await(5, TimeUnit.SECONDS)
                            }
                            json(401, """{"status":401,"code":"AuthenticationRequired"}""")
                        }
                    }
                }
            }
        }
        server.start()

        tokenStore = FakeSessionTokenStore(accessToken = OLD_ACCESS, refreshToken = OLD_REFRESH)
        expiryNotifier = SessionExpiryNotifier()
        val baseUrl = server.url("/api/v1/")

        val anonymousClient = OkHttpClient.Builder()
            .addInterceptor(AcceptLanguageInterceptor { "en" })
            .build()
        val service = Retrofit.Builder()
            .baseUrl(baseUrl)
            .client(anonymousClient)
            .addConverterFactory(JSON.asConverterFactory("application/json".toMediaType()))
            .build()
            .create(AuthenticationService::class.java)
        val coordinator = TokenRefreshCoordinator(tokenStore, RetrofitTokenRefresher(service), expiryNotifier)

        client = anonymousClient.newBuilder()
            .addInterceptor(AuthInterceptor(tokenStore))
            .authenticator(TokenRefreshAuthenticator(coordinator))
            .build()
    }

    @After
    fun tearDown() {
        server.close()
    }

    @Test
    fun `adds bearer and accept-language to protected requests`() {
        runBlocking { tokenStore.save(NEW_ACCESS, NEW_REFRESH) }

        client.get("patients/7/active-targets").use { assertEquals(200, it.code) }

        val recorded = server.takeRequest()
        assertEquals("Bearer $NEW_ACCESS", recorded.headers["Authorization"])
        assertEquals("en", recorded.headers["Accept-Language"])
    }

    @Test
    fun `anonymous endpoints never carry the bearer nor trigger a refresh`() {
        client.post("authentication/sign-in").use { assertEquals(401, it.code) }

        val recorded = server.takeRequest()
        assertNull(recorded.headers["Authorization"])
        assertEquals(0, refreshCalls.get())
        assertEquals(OLD_ACCESS, tokenStore.currentAccessToken)
    }

    @Test
    fun `401 refreshes the pair, stores it and retries with the new token`() {
        client.get("patients/7/active-targets").use { assertEquals(200, it.code) }

        assertEquals(1, refreshCalls.get())
        assertEquals(listOf("""{"refreshToken":"$OLD_REFRESH"}"""), refreshBodies.toList())
        assertEquals(listOf("Bearer $OLD_ACCESS", "Bearer $NEW_ACCESS"), protectedAuthHeaders.toList())
        assertEquals(NEW_ACCESS, tokenStore.currentAccessToken)
        assertEquals(NEW_REFRESH, tokenStore.currentRefreshToken)
    }

    @Test
    fun `simultaneous 401s refresh only once and all requests succeed`() {
        val parallel = 5
        staleTokenGate = CountDownLatch(parallel)
        val executor = Executors.newFixedThreadPool(parallel)
        try {
            val codes = (1..parallel)
                .map { i -> executor.submit<Int> { client.get("patients/$i/active-targets").use { it.code } } }
                .map { it.get(10, TimeUnit.SECONDS) }

            assertEquals(List(parallel) { 200 }, codes)
            assertEquals(1, refreshCalls.get())
            assertEquals(1, tokenStore.saveCount)
            assertEquals(parallel, protectedAuthHeaders.count { it == "Bearer $OLD_ACCESS" })
            assertEquals(parallel, protectedAuthHeaders.count { it == "Bearer $NEW_ACCESS" })
        } finally {
            executor.shutdownNow()
        }
    }

    @Test
    fun `rejected refresh expires the session and does not retry`() = runBlocking<Unit> {
        refreshResponse = { json(401, """{"status":401,"code":"RefreshTokenInvalid"}""") }

        client.get("patients/7/active-targets").use { assertEquals(401, it.code) }

        assertEquals(1, refreshCalls.get())
        assertEquals(listOf("Bearer $OLD_ACCESS"), protectedAuthHeaders.toList())
        assertNull(tokenStore.currentAccessToken)
        assertNull(tokenStore.currentRefreshToken)
        withTimeout(1_000) { expiryNotifier.events.first() }
    }

    @Test
    fun `unreachable refresh fails as a network error and keeps the session`() {
        refreshResponse = { json(503, """{"status":503,"code":"InternalError"}""") }

        try {
            client.get("patients/7/active-targets").close()
            fail("Expected an IOException")
        } catch (e: IOException) {
            assertTrue(e is TokenRefreshUnavailableException)
        }

        assertEquals(OLD_ACCESS, tokenStore.currentAccessToken)
        assertEquals(OLD_REFRESH, tokenStore.currentRefreshToken)
    }

    @Test
    fun `a 401 after the retry does not loop`() {
        refreshResponse = { json(200, """{"token":"still-bad","refreshToken":"r3"}""") }

        client.get("patients/7/active-targets").use { assertEquals(401, it.code) }

        assertEquals(1, refreshCalls.get())
        assertEquals(listOf("Bearer $OLD_ACCESS", "Bearer still-bad"), protectedAuthHeaders.toList())
    }

    @Test
    fun `without session a 401 is returned as is`() {
        runBlocking { tokenStore.clear() }

        client.get("patients/7/active-targets").use { assertEquals(401, it.code) }

        assertEquals(0, refreshCalls.get())
        assertFalse(runBlocking { tokenStore.hasSession.first() })
    }

    private fun OkHttpClient.get(path: String): Response =
        newCall(Request.Builder().url(server.url("/api/v1/$path")).build()).execute()

    private fun OkHttpClient.post(path: String): Response {
        val body = "{}".toRequestBody("application/json".toMediaType())
        return newCall(Request.Builder().url(server.url("/api/v1/$path")).post(body).build()).execute()
    }

    private fun json(code: Int, body: String): MockResponse = MockResponse.Builder()
        .code(code)
        .setHeader("Content-Type", if (code >= 400) "application/problem+json" else "application/json")
        .body(body)
        .build()

    private companion object {
        val JSON = Json { ignoreUnknownKeys = true }
        const val OLD_ACCESS = "access-1"
        const val OLD_REFRESH = "refresh-1"
        const val NEW_ACCESS = "access-2"
        const val NEW_REFRESH = "refresh-2"
    }
}
