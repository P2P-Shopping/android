package p2ps.android

import android.util.Log
class HardwareManager {
    private val TAG = "HardwareManager"
    private var isInitialized = false

    private val locationBridge = NativeLocationBridge()
    private val apiClient = ApiClient()

    // 1. Inițializarea SDK-ului
    fun initialize() {
        if (isInitialized) return

        Log.d(TAG, "Initializing Hardware SDK Scaffolding...")

        // Verificam dacă s-a conectat cu succes înainte să zicem că e gata
        if (connectToDevice()) {
            isInitialized = true
        } else {
            Log.e(TAG, "Hardware SDK initialization failed.")
        }
    }
    // Conectarea la dispozitiv
    // Am modificat functia să returneze True daca totul e ok sau altfel False
    private fun connectToDevice(): Boolean {
        return try {
            Log.d(TAG, "Attempting to connect to hardware API...")
            // Simulăm succesul
            Log.i(TAG, "Hardware connection established successfully.")
            true
        } catch (e: Exception) {
            Log.e(TAG, "Hardware connection failed.", e)
            false
        }
    }

    // Functia de Ping
    fun sendLocationPing(lat: Double, lng: Double) {
        if (!isInitialized) {
            Log.e(TAG, "Cannot send ping: Hardware not initialized.")
            return
        }
        Log.d(TAG, "Hardware Event: Sending location ping")
    }

    //Trigger-ul principal
    fun handleHardwareTrigger(storeId: String, itemId: String, triggerType: String = "BUTTON_PRESS") {
        if (!isInitialized) {
            Log.e(TAG, "Trigger failed: Hardware not initialized.")
            return
        }

        Log.i(TAG, "Hardware Trigger Detected!")

        val rawData = mapOf(
            "storeId" to storeId,
            "itemId" to itemId,
            "triggerType" to triggerType,
            "timestamp" to System.currentTimeMillis()
        )

        if (javaClass.desiredAssertionStatus()) {
            Log.d(TAG, "Raw data extracted: keys=${rawData.keys}")
        }

        dispatchTelemetry(rawData)
    }

    private fun dispatchTelemetry(data: Map<String, Any>) {
        //Preluam locația
        val locationData = locationBridge.getAccurateLocation()

        // Construim DTO-ul complet
        val fullTelemetryDto = data.toMutableMap()
        fullTelemetryDto.putAll(locationData)

        // Trimitem ping-ul cu toate datele colectate
        apiClient.sendPing(fullTelemetryDto)

        if (javaClass.desiredAssertionStatus()) {
            Log.i(TAG, "Telemetry flow completed for: keys=${fullTelemetryDto.keys}")
        }
    }
}