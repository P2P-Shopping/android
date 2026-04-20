package p2ps.android.data

import android.content.Context
import android.content.SharedPreferences

class TelemetryManager(context: Context) {
    private val prefs: SharedPreferences = context.getSharedPreferences("telemetry_prefs", Context.MODE_PRIVATE)

    fun savePing(ping: TelemetryPing) {
        val editor = prefs.edit()

        // Salvăm datele ca un șir de caractere simplu (sau le poți salva individual)
        val dataString = "ID:${ping.storeId}, Item:${ping.itemId}, Type:${ping.triggerType}, Time:${ping.timestamp}, Lat:${ping.lat}, Long:${ping.long}"

        // Folosim timestamp-ul ca cheie unică pentru fiecare salvare
        editor.putString("ping_${ping.timestamp}", dataString)
        editor.apply()

        println("Salvat cu succes: $dataString")
    }
}