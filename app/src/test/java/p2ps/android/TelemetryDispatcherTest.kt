package p2ps.android

import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.Test
import p2ps.android.ApiClient
import p2ps.android.core.TelemetryDispatcher
import p2ps.android.data.TelemetryManager
import p2ps.android.data.TelemetryPing
import android.util.Log
import io.mockk.mockkStatic
import io.mockk.unmockkStatic
import org.junit.After
import org.junit.Before

class TelemetryDispatcherTest {

    // Simulăm clasele de care depinde Dispatcher-ul nostru
    private val apiClient = mockk<ApiClient>(relaxed = true)
    private val telemetryManager = mockk<TelemetryManager>(relaxed = true)

    // Injectăm mock-urile în dispatcher
    private val dispatcher = TelemetryDispatcher(apiClient, telemetryManager)

    @Before
    fun setup() {
        mockkStatic(Log::class)
        every { Log.i(any<String>(), any<String>()) } returns 0
        every { Log.d(any<String>(), any<String>()) } returns 0
        every { Log.w(any<String>(), any<String>()) } returns 0
        every { Log.e(any<String>(), any<String>()) } returns 0
    }

    // Un obiect de date generic pentru test
    private val testPing = TelemetryPing(
        deviceId = "test_id",
        storeId = "store_1",
        itemId = "item_1",
        triggerType = "test",
        lat = 0.0, lng = 0.0, accuracy = 0f,
        timestamp = 123456789L
    )

    @Test
    fun `test dispatch fallback when API fails`() {
        // Forțăm ApiClient să returneze FALSE (ca să testăm logica de fallback)
        every { apiClient.sendPing(any()) } returns false

        // Executăm metoda
        dispatcher.dispatch(testPing)

        // VERIFICARE: S-a apelat savePing?
        // Dacă da, SonarQube marchează liniile de fallback ca fiind "acoperite"
        verify(exactly = 1) { telemetryManager.savePing(testPing) }
    }

    @Test
    fun `test dispatch success when API works`() {
        // Forțăm ApiClient să returneze TRUE
        every { apiClient.sendPing(any()) } returns true

        dispatcher.dispatch(testPing)

        // VERIFICARE: Nu s-a apelat salvarea locală
        verify(exactly = 0) { telemetryManager.savePing(any()) }
    }
}