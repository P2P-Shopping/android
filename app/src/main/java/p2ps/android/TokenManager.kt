package p2ps.android

import android.content.Context
import androidx.core.content.edit
import java.util.Base64

class TokenManager(private val context: Context) {

    companion object {
        private const val PREFS_NAME = "p2ps_auth_prefs"
        private const val KEY_JWT = "jwt_token"
    }

    fun saveToken(token: String) {
        getPrefs().edit { putString(KEY_JWT, token) }
    }

    fun getToken(): String? = getPrefs().getString(KEY_JWT, null)

    fun clearToken() {
        getPrefs().edit { remove(KEY_JWT) }
    }

    fun isTokenValid(): Boolean {
        val token = getToken() ?: return false
        return try {
            val exp = extractExpiry(token)
            val nowSeconds = System.currentTimeMillis() / 1000
            exp > nowSeconds + 30
        } catch (e: Exception) {
            false
        }
    }

    private fun getPrefs() = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    private fun extractExpiry(token: String): Long {
        val parts = token.split(".")
        require(parts.size == 3) { "Invalid JWT structure" }

        val decoder = Base64.getUrlDecoder()
        val payloadJson = String(decoder.decode(parts[1].padToBase64()), Charsets.UTF_8)

        val match = Regex(""""exp"\s*:\s*(\d+)""").find(payloadJson)
            ?: throw IllegalArgumentException("No exp claim found in JWT payload")

        return match.groupValues[1].toLong()
    }

    private fun String.padToBase64(): String {
        val remainder = length % 4
        return if (remainder == 0) this else this + "=".repeat(4 - remainder)
    }
}