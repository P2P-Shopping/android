package p2ps.android

import android.util.Log
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import p2ps.android.data.TelemetryBatch
import p2ps.android.data.TelemetryPing
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

class ApiClient {
    private val TAG = "ApiClient"

    private val BASE_URL = "http://10.0.2.2:8081/api/"

    private val apiService: ApiService

    init {
        val logging = HttpLoggingInterceptor().apply {
            level = HttpLoggingInterceptor.Level.BODY
        }

        // We add the Header interceptor FIRST, and Logging LAST
        // so we can see the final headers in the Logcat.
        val client = OkHttpClient.Builder()
            .addInterceptor { chain ->
                val request = chain.request().newBuilder()
                    // Matching exactly what works in Postman
                    .addHeader("X-API-Key", "megasuperhyperduperultrasecretAPIkeypassword")
                    .addHeader("X-Device-Id", "megasuperhyperduperultrasecretAPIkeypassword")
                    .build()
                chain.proceed(request)
            }
            .addInterceptor(logging)
            .build()

        val retrofit = Retrofit.Builder()
            .baseUrl(BASE_URL)
            .addConverterFactory(GsonConverterFactory.create())
            .client(client)
            .build()

        apiService = retrofit.create(ApiService::class.java)
    }

    fun sendPing(ping: TelemetryPing) {
        Log.d(TAG, "Dispatching telemetry to Spring Server: ${ping.itemId}")

        apiService.sendPing(ping).enqueue(object : Callback<Unit> {
            override fun onResponse(call: Call<Unit>, response: Response<Unit>) {
                if (response.isSuccessful || response.code() == 202) {
                    Log.i(TAG, "Telemetry synced successfully (HTTP ${response.code()})")
                } else {
                    Log.e(TAG, "Server rejected telemetry: ${response.code()}")
                }
            }

            override fun onFailure(call: Call<Unit>, t: Throwable) {
                Log.e(TAG, "Network error: ${t.message}")
            }
        })
    }

    fun sendBatch(pings: List<TelemetryPing>) {
        val batch = TelemetryBatch(pings)
        apiService.sendBatchPings(batch).enqueue(object : Callback<Unit> {
            override fun onResponse(call: Call<Unit>, response: Response<Unit>) {
                if (response.isSuccessful || response.code() == 202) {
                    Log.i(TAG, "Batch synced successfully")
                }
            }
            override fun onFailure(call: Call<Unit>, t: Throwable) {
                Log.e(TAG, "Batch sync failed: ${t.message}")
            }
        })
    }
}
