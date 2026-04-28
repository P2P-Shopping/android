package p2ps.android

import android.util.Log
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import p2ps.android.data.TelemetryPing
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

class ApiClient {
    private val TAG = "ApiClient"
    private val BASE_URL = "http://10.0.2.2:8081/api/"
    private val API_KEY = "megasuperhyperduperultrasecretAPIkeypassword" // Should come from BuildConfig in production

    private val apiService: ApiService

    init {
        val logging = HttpLoggingInterceptor().apply {
            level = HttpLoggingInterceptor.Level.BODY
        }
        val client = OkHttpClient.Builder()
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
        Log.i(TAG, "Attempting to send real telemetry data to backend...")
        
        val call = apiService.sendPing(API_KEY, ping.deviceId, ping)
        
        call.enqueue(object : Callback<Unit> {
            override fun onResponse(call: Call<Unit>, response: Response<Unit>) {
                if (response.isSuccessful) {
                    Log.i(TAG, "Telemetry synced successfully (HTTP ${response.code()})")
                } else {
                    Log.e(TAG, "Server rejected telemetry: ${response.code()}")
                    val errorBody = response.errorBody()?.string()
                    if (errorBody != null) {
                        Log.e(TAG, "Error body: $errorBody")
                    }
                }
            }

            override fun onFailure(call: Call<Unit>, t: Throwable) {
                Log.e(TAG, "Failed to connect to telemetry server", t)
            }
        })
    }
}
