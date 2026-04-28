package p2ps.android

import android.util.Log
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import p2ps.android.data.TelemetryPing
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

class ApiClient {
    companion object {
        private const val TAG = "ApiClient"
        private const val BASE_URL = "http://10.0.2.2:8081/api/"
        private const val API_KEY = "megasuperhyperduperultrasecretAPIkeypassword"
        private const val DEVICE_ID = "usr-DEMO"
    }

    private val apiService: ApiService

    init {
        // Interceptor pentru adăugarea headerelor de autentificare (X-API-Key și X-Device-Id)
        val authInterceptor = Interceptor { chain ->
            val request = chain.request().newBuilder()
                .addHeader("X-API-Key", API_KEY)
                .addHeader("X-Device-Id", DEVICE_ID)
                .build()
            chain.proceed(request)
        }

        val logging = HttpLoggingInterceptor().apply {
            level = HttpLoggingInterceptor.Level.BODY
        }

        val client = OkHttpClient.Builder()
            .addInterceptor(authInterceptor)
            .addInterceptor(logging)
            .build()

        val retrofit = Retrofit.Builder()
            .baseUrl(BASE_URL)
            .client(client)
            .addConverterFactory(GsonConverterFactory.create())
            .build()

        apiService = retrofit.create(ApiService::class.java)
    }

    fun sendPing(ping: TelemetryPing): Boolean {
        return try {
            // Verificăm dacă suntem pe firul principal pentru a evita crash-ul
            if (android.os.Looper.myLooper() == android.os.Looper.getMainLooper()) {
                Log.w(TAG, "Network call attempted on Main Thread! Redirection to background required.")
                return false
            }

            Log.i(TAG, "Attempting POST /api/telemetry/ping")
            val response = apiService.sendPing(ping).execute()
            
            if (response.isSuccessful) {
                Log.i(TAG, "Telemetry accepted (202 Accepted)")
                true
            } else {
                Log.e(TAG, "Server error: ${response.code()} - ${response.message()}")
                false
            }
        } catch (e: Exception) {
            Log.e(TAG, "Network failure: ${e.localizedMessage ?: "Connection error"}")
            false
        }
    }
}
