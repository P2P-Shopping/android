package p2ps.android.fcm

import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import com.google.android.gms.tasks.Task
import com.google.firebase.messaging.FirebaseMessaging
import io.mockk.*
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

class FcmTokenManagerTest {

    private lateinit var mockContext: Context
    private lateinit var mockPrefs: SharedPreferences
    private lateinit var mockEditor: SharedPreferences.Editor

    @Before
    fun setUp() {
        mockkStatic(Log::class)
        every { Log.d(any(), any()) } returns 0
        every { Log.e(any(), any()) } returns 0
        every { Log.e(any(), any(), any()) } returns 0

        mockEditor = mockk(relaxed = true)
        mockPrefs = mockk {
            every { edit() } returns mockEditor
        }
        mockContext = mockk {
            every { getSharedPreferences("p2ps_fcm_prefs", Context.MODE_PRIVATE) } returns mockPrefs
        }
    }

    @After
    fun tearDown() {
        unmockkAll()
    }

    // ── saveToken ─────────────────────────────────────────────────────────────

    @Test
    fun saveToken_writesTokenToPrefs() {
        FcmTokenManager.saveToken(mockContext, "test_fcm_token_xyz")
        verify { mockEditor.putString("fcm_token", "test_fcm_token_xyz") }
    }

    @Test
    fun saveToken_callsApply() {
        FcmTokenManager.saveToken(mockContext, "token_abc")
        verify { mockEditor.apply() }
    }

    @Test
    fun saveToken_withEmptyToken_stillWrites() {
        FcmTokenManager.saveToken(mockContext, "")
        verify { mockEditor.putString("fcm_token", "") }
    }

    @Test
    fun saveToken_withLongToken_doesNotThrow() {
        val longToken = "a".repeat(500)
        try {
            FcmTokenManager.saveToken(mockContext, longToken)
        } catch (e: Exception) {
            fail("Should not throw: ${e.message}")
        }
    }

    // ── getStoredToken ────────────────────────────────────────────────────────

    @Test
    fun getStoredToken_returnsNull_whenNothingStored() {
        every { mockPrefs.getString("fcm_token", null) } returns null

        val result = FcmTokenManager.getStoredToken(mockContext)

        assertNull(result)
    }

    @Test
    fun getStoredToken_returnsStoredToken() {
        every { mockPrefs.getString("fcm_token", null) } returns "stored_token_123"

        val result = FcmTokenManager.getStoredToken(mockContext)

        assertEquals("stored_token_123", result)
    }

    @Test
    fun getStoredToken_afterSave_returnsCorrectValue() {
        var stored: String? = null
        every { mockEditor.putString("fcm_token", any()) } answers {
            stored = secondArg()
            mockEditor
        }
        every { mockPrefs.getString("fcm_token", null) } answers { stored }

        FcmTokenManager.saveToken(mockContext, "my_token")
        val result = FcmTokenManager.getStoredToken(mockContext)

        assertEquals("my_token", result)
    }

    @Test
    fun getStoredToken_doesNotReturnDefault_whenKeyAbsent() {
        every { mockPrefs.getString("fcm_token", null) } returns null

        val result = FcmTokenManager.getStoredToken(mockContext)

        assertNull(result)
        assertNotEquals("default", result)
    }

    // ── fetchToken ────────────────────────────────────────────────────────────

    @Test
    fun fetchToken_whenFirebaseSucceeds_returnsToken() = kotlinx.coroutines.runBlocking {
        val mockTask = mockk<Task<String>>()
        every { mockTask.isSuccessful } returns true
        every { mockTask.result } returns "firebase_token_abc"
        every { mockTask.addOnCompleteListener(any()) } answers {
            firstArg<com.google.android.gms.tasks.OnCompleteListener<String>>().onComplete(mockTask)
            mockTask
        }

        mockkStatic(FirebaseMessaging::class)
        val mockFcm = mockk<FirebaseMessaging>()
        every { FirebaseMessaging.getInstance() } returns mockFcm
        every { mockFcm.token } returns mockTask

        val result = FcmTokenManager.fetchToken()

        assertEquals("firebase_token_abc", result)
    }

    @Test
    fun fetchToken_whenFirebaseFails_returnsNull() = kotlinx.coroutines.runBlocking {
        val mockTask = mockk<Task<String>>()
        every { mockTask.isSuccessful } returns false
        every { mockTask.exception } returns RuntimeException("Firebase error")
        every { mockTask.addOnCompleteListener(any()) } answers {
            firstArg<com.google.android.gms.tasks.OnCompleteListener<String>>().onComplete(mockTask)
            mockTask
        }

        mockkStatic(FirebaseMessaging::class)
        val mockFcm = mockk<FirebaseMessaging>()
        every { FirebaseMessaging.getInstance() } returns mockFcm
        every { mockFcm.token } returns mockTask

        val result = FcmTokenManager.fetchToken()

        assertNull(result)
    }
}