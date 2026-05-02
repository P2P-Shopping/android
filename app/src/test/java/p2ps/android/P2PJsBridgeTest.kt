package p2ps.android

import android.content.Context
import android.content.SharedPreferences
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
import android.provider.Settings
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

class P2PJsBridgeTest {

    private lateinit var mockContext: Context
    private lateinit var mockPrefs: SharedPreferences
    private lateinit var mockEditor: SharedPreferences.Editor
    private lateinit var bridge: P2PJsBridge

    @Before
    fun setUp() {
        mockEditor = mockk(relaxed = true)
        mockPrefs = mockk {
            every { getString("device_id", null) } returns null
            every { edit() } returns mockEditor
        }
        mockContext = mockk {
            every { getSharedPreferences("p2ps_device_prefs", Context.MODE_PRIVATE) } returns mockPrefs
            every { contentResolver } returns mockk()
        }

        // Mock static Settings.Secure.getString corect
        mockkStatic(Settings.Secure::class)
        every {
            Settings.Secure.getString(any(), Settings.Secure.ANDROID_ID)
        } returns "test_android_id_12345"

        bridge = P2PJsBridge(mockContext)
    }

    // --- getPlatform ---

    @Test
    fun `getPlatform returns android`() {
        assertEquals("android", bridge.getPlatform())
    }

    // --- getDeviceId ---

    @Test
    fun `getDeviceId returns cached id when available`() {
        every { mockPrefs.getString("device_id", null) } returns "cached-id-abc"

        val id = bridge.getDeviceId()

        assertEquals("cached-id-abc", id)
    }

    @Test
    fun `getDeviceId returns non-blank string`() {
        val id = bridge.getDeviceId()

        assertTrue("Device ID must not be blank", id.isNotBlank())
    }

    @Test
    fun `getDeviceId is consistent across calls within same session`() {
        var storedId: String? = null
        every { mockEditor.putString("device_id", any()) } answers {
            storedId = secondArg()
            mockEditor
        }
        every { mockPrefs.getString("device_id", null) } answers { storedId }

        val first = bridge.getDeviceId()
        val second = bridge.getDeviceId()

        assertEquals("ID must be stable across calls", first, second)
    }
}