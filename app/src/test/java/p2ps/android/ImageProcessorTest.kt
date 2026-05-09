package p2ps.android

import android.content.Context
import android.net.Uri
import androidx.test.core.app.ApplicationProvider
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import p2ps.android.core.ImageProcessor

//import p2ps.android.data.ImageProcessor

@RunWith(RobolectricTestRunner::class)
class ImageProcessorTest {

    @Test
    fun testCompressImage_withRobolectricDummy_returnsBase64String() {
        // 1. Setup
        val context = ApplicationProvider.getApplicationContext<Context>()

        // Robolectric va intercepta acest URI și ne va oferi o imagine dummy validă
        val dummyUri = Uri.parse("content://dummy/path")

        // 2. Action
        val result = ImageProcessor.processAndCompressImage(context, dummyUri)
        // 3. Assertion
        // Acum ne așteptăm să NU fie null și să fie o imagine JPEG în format Base64
        assertNotNull("Rezultatul nu ar trebui să fie null deoarece Robolectric oferă o imagine fallback", result)
        assertTrue("Rezultatul trebuie să înceapă cu antetul de imagine Base64", result?.startsWith("data:image/jpeg;base64,") == true)
    }
}