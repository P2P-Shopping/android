package p2ps.android

import android.content.Context
import android.content.SharedPreferences
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import java.util.Base64

class TokenManagerTest {

    private lateinit var mockContext: Context
    private lateinit var mockPrefs: SharedPreferences
    private lateinit var mockEditor: SharedPreferences.Editor
    private lateinit var tokenManager: TokenManager

    @Before
    fun setUp() {
        mockEditor = mockk(relaxed = true)
        mockPrefs = mockk(relaxed = true) {
            every { edit() } returns mockEditor
        }
        mockContext = mockk {
            every { getSharedPreferences("p2ps_auth_prefs", Context.MODE_PRIVATE) } returns mockPrefs
        }
        tokenManager = TokenManager(mockContext)
    }

    @Test
    fun `saveToken writes to prefs`() {
        tokenManager.saveToken("my.jwt.token")
        verify { mockEditor.putString("jwt_token", "my.jwt.token") }
    }

    @Test
    fun `getToken returns null when nothing stored`() {
        every { mockPrefs.getString("jwt_token", null) } returns null
        assertNull(tokenManager.getToken())
    }

    @Test
    fun `getToken returns stored value`() {
        every { mockPrefs.getString("jwt_token", null) } returns "stored.jwt.token"
        assertEquals("stored.jwt.token", tokenManager.getToken())
    }

    @Test
    fun `clearToken removes key from prefs`() {
        tokenManager.clearToken()
        verify { mockEditor.remove("jwt_token") }
    }

    @Test
    fun `isTokenValid returns false when no token stored`() {
        every { mockPrefs.getString("jwt_token", null) } returns null
        assertFalse(tokenManager.isTokenValid())
    }

    @Test
    fun `isTokenValid returns true for future expiry`() {
        val futureExp = (System.currentTimeMillis() / 1000) + 3600
        every { mockPrefs.getString("jwt_token", null) } returns buildJwt(futureExp)
        assertTrue(tokenManager.isTokenValid())
    }

    @Test
    fun `isTokenValid returns false for past expiry`() {
        val pastExp = (System.currentTimeMillis() / 1000) - 3600
        every { mockPrefs.getString("jwt_token", null) } returns buildJwt(pastExp)
        assertFalse(tokenManager.isTokenValid())
    }

    @Test
    fun `isTokenValid returns false for expiry within 30-second buffer`() {
        val almostExpired = (System.currentTimeMillis() / 1000) + 10
        every { mockPrefs.getString("jwt_token", null) } returns buildJwt(almostExpired)
        assertFalse(tokenManager.isTokenValid())
    }

    @Test
    fun `isTokenValid returns false for malformed token`() {
        every { mockPrefs.getString("jwt_token", null) } returns "not.a.valid.jwt.at.all"
        assertFalse(tokenManager.isTokenValid())
    }


    private fun buildJwt(exp: Long): String {
        val encoder = Base64.getUrlEncoder().withoutPadding()

        val header = encoder.encodeToString(
            """{"alg":"HS256","typ":"JWT"}""".toByteArray()
        )
        // JSON construit manual ca string — fără org.json
        val payload = encoder.encodeToString(
            """{"exp":$exp,"sub":"user123"}""".toByteArray()
        )
        return "$header.$payload.stub_signature"
    }
}