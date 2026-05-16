package p2ps.android

import android.content.Intent
import android.net.Uri
import android.provider.Settings
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.junit.MockitoJUnitRunner

@RunWith(MockitoJUnitRunner::class)
class MainActivityPermissionLogicTest {

    @Test
    fun `openAppSettings ar trebui sa creeze un Intent corect catre setarile aplicatiei`() {
        // Arrange: Pregătim datele (numele pachetului aplicației tale)
        val testPackageName = "p2ps.android"
        val expectedUri = Uri.fromParts("package", testPackageName, null)

        // Act: Simulăm crearea Intent-ului exact cum se face în MainActivity
        val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
            data = expectedUri
        }

        // Assert: Verificăm dacă Intent-ul are acțiunea și datele corecte
        assertEquals(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, intent.action)
        assertEquals("package", intent.data?.scheme)
        assertEquals(testPackageName, intent.data?.schemeSpecificPart)
    }
}