package com.yaan.core.data.repository

import com.yaan.core.common.dispatcher.CoroutineDispatchers
import com.yaan.core.common.result.YaanResult
import com.yaan.core.data.crypto.InMemoryVaultKeyManager
import com.yaan.core.data.remote.supabase.SupabaseAuthClient
import com.yaan.core.domain.model.AccountTier
import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
import io.ktor.serialization.kotlinx.json.json
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class UserSessionRepositoryTest {

    private val testDispatchers = object : CoroutineDispatchers {
        override val main = Dispatchers.Unconfined
        override val io = Dispatchers.Unconfined
        override val default = Dispatchers.Unconfined
        override val unconfined = Dispatchers.Unconfined
    }

    @Test
    fun `test default profile and profile updating`() = runTest {
        val keyManager = InMemoryVaultKeyManager()
        val repo = DefaultUserSessionRepository(keyManager, testDispatchers)

        val initial = repo.getCurrentProfile()
        assertEquals("Elena Rostova", initial.displayName)
        assertEquals(AccountTier.PRO, initial.tier)
        assertTrue(initial.biometricLockEnabled)

        // Update profile
        val updateRes = repo.updateProfile("Elena R. Senior", "elena@yaan.ai")
        assertTrue(updateRes is YaanResult.Success)
        val updated = repo.getCurrentProfile()
        assertEquals("Elena R. Senior", updated.displayName)
        assertEquals("elena@yaan.ai", updated.email)
    }

    @Test
    fun `test biometric vault lock and unlock lifecycle`() = runTest {
        val keyManager = InMemoryVaultKeyManager()
        val repo = DefaultUserSessionRepository(keyManager, testDispatchers)

        repo.lockVault()
        assertTrue(repo.getCurrentProfile().isVaultLocked)

        val unlockRes = repo.unlockVault()
        assertTrue(unlockRes is YaanResult.Success)
        assertFalse(repo.getCurrentProfile().isVaultLocked)

        repo.setBiometricLock(false)
        assertFalse(repo.getCurrentProfile().biometricLockEnabled)
    }

    @Test
    fun `test passkey registration and qr device pairing`() = runTest {
        val keyManager = InMemoryVaultKeyManager()
        val repo = DefaultUserSessionRepository(keyManager, testDispatchers)

        // Passkey registration
        val passkeyRes = repo.registerPasskey()
        assertTrue(passkeyRes is YaanResult.Success)
        assertTrue(repo.getCurrentProfile().passkeyRegistered)

        // QR Device Pairing
        val pairRes = repo.pairDeviceWithQr("yaan_pair_qr_token_super_secret_999")
        assertTrue(pairRes is YaanResult.Success)
        assertEquals(3, (pairRes as YaanResult.Success).data)
        assertEquals(3, repo.getCurrentProfile().pairedDevicesCount)

        // Malformed QR payload rejected
        val badPairRes = repo.pairDeviceWithQr("too_short")
        assertTrue(badPairRes is YaanResult.Failure)
    }

    @Test
    fun `test logout and crypto-shredding clears keys and resets session`() = runTest {
        val keyManager = InMemoryVaultKeyManager()
        val repo = DefaultUserSessionRepository(keyManager, testDispatchers)

        val initialKey = keyManager.getOrCreateMasterKey()
        assertTrue(initialKey.any { it != 0.toByte() })

        val logoutRes = repo.logoutAndCryptoShred()
        assertTrue(logoutRes is YaanResult.Success)

        val session = repo.getCurrentProfile()
        assertEquals("Anonymous User", session.displayName)
        assertEquals("anonymous:revoked", session.authToken)
        assertTrue(session.isVaultLocked)
        assertFalse(session.isOnboardingCompleted)
        assertFalse(session.ageConfirmed18Plus)
    }

    @Test
    fun `test complete onboarding rejects underage user`() = runTest {
        val keyManager = InMemoryVaultKeyManager()
        val repo = DefaultUserSessionRepository(keyManager, testDispatchers)

        // Reset to fresh/unonboarded state
        repo.logoutAndCryptoShred()
        assertFalse(repo.getCurrentProfile().isOnboardingCompleted)

        // Attempt onboarding with age confirmation false
        val rejectRes = repo.completeOnboarding("Test User", ageConfirmed18Plus = false)
        assertTrue(rejectRes is YaanResult.Failure)
        assertFalse(repo.getCurrentProfile().isOnboardingCompleted)
        assertFalse(repo.getCurrentProfile().ageConfirmed18Plus)
    }

    @Test
    fun `test complete onboarding succeeds for 18+ user`() = runTest {
        val keyManager = InMemoryVaultKeyManager()
        val repo = DefaultUserSessionRepository(keyManager, testDispatchers)

        // Reset to fresh/unonboarded state
        repo.logoutAndCryptoShred()

        // Attempt onboarding with age confirmation true
        val successRes = repo.completeOnboarding("Elena Rostova", ageConfirmed18Plus = true)
        assertTrue(successRes is YaanResult.Success)
        val profile = (successRes as YaanResult.Success).data
        assertEquals("Elena Rostova", profile.displayName)
        assertTrue(profile.isOnboardingCompleted)
        assertTrue(profile.ageConfirmed18Plus)
        assertTrue(repo.getCurrentProfile().isOnboardingCompleted)
    }

    @Test
    fun `test signInWithEmail validation rejects bad inputs`() = runTest {
        val keyManager = InMemoryVaultKeyManager()
        val repo = DefaultUserSessionRepository(keyManager, testDispatchers)

        // Empty email
        val res1 = repo.signInWithEmail("", "secret123")
        assertTrue(res1 is YaanResult.Failure)

        // Invalid email format
        val res2 = repo.signInWithEmail("invalidemail", "secret123")
        assertTrue(res2 is YaanResult.Failure)

        // Short password
        val res3 = repo.signInWithEmail("user@yaan.ai", "123")
        assertTrue(res3 is YaanResult.Failure)
    }

    @Test
    fun `test signUpWithEmail validation rejects underage`() = runTest {
        val keyManager = InMemoryVaultKeyManager()
        val repo = DefaultUserSessionRepository(keyManager, testDispatchers)

        val res = repo.signUpWithEmail("young@yaan.ai", "secret123", "Young User", ageConfirmed18Plus = false)
        assertTrue(res is YaanResult.Failure)
    }

    @Test
    fun `test signInWithEmail offline fallback succeeds with local token`() = runTest {
        val keyManager = InMemoryVaultKeyManager()
        val repo = DefaultUserSessionRepository(keyManager, testDispatchers, supabaseAuthClient = null)

        val res = repo.signInWithEmail("executive@yaan.ai", "secure_pass_123")
        assertTrue(res is YaanResult.Success)
        val profile = (res as YaanResult.Success).data
        assertEquals("executive@yaan.ai", profile.email)
        assertEquals("executive", profile.displayName)
        assertTrue(profile.isOnboardingCompleted)
        assertFalse(profile.isVaultLocked)
    }

    @Test
    fun `test signInWithEmail with mocked Supabase client succeeds`() = runTest {
        val mockEngine = MockEngine { request ->
            when {
                request.url.encodedPath.contains("token") -> {
                    respond(
                        content = """{"access_token":"eyMockJwtToken","token_type":"bearer","user":{"id":"usr_supabase_99","email":"alex@yaan.ai","user_metadata":{"display_name":"Alex Mercer"}}}""",
                        status = HttpStatusCode.OK,
                        headers = headersOf(HttpHeaders.ContentType, "application/json")
                    )
                }
                request.url.encodedPath.contains("profiles") -> {
                    respond(
                        content = """[{"id":"usr_supabase_99","email":"alex@yaan.ai","display_name":"Alex Mercer Profile","tier":"PRO","age_confirmed_18_plus":true}]""",
                        status = HttpStatusCode.OK,
                        headers = headersOf(HttpHeaders.ContentType, "application/json")
                    )
                }
                else -> {
                    respond("{}", HttpStatusCode.OK)
                }
            }
        }

        val httpClient = HttpClient(mockEngine) {
            install(ContentNegotiation) {
                json(Json { ignoreUnknownKeys = true })
            }
        }
        val supabaseClient = SupabaseAuthClient(httpClient = httpClient)
        val keyManager = InMemoryVaultKeyManager()
        val repo = DefaultUserSessionRepository(keyManager, testDispatchers, supabaseAuthClient = supabaseClient)

        val res = repo.signInWithEmail("alex@yaan.ai", "password123")
        assertTrue(res is YaanResult.Success)
        val profile = (res as YaanResult.Success).data
        assertEquals("usr_supabase_99", profile.userId)
        assertEquals("alex@yaan.ai", profile.email)
        assertEquals("Alex Mercer Profile", profile.displayName)
        assertEquals("eyMockJwtToken", profile.authToken)
        assertEquals(AccountTier.PRO, profile.tier)
        assertTrue(profile.ageConfirmed18Plus)
    }
}
