package p2ps.android.location

import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import org.junit.Test
import org.mockito.Mockito

class NotificationDismissReceiverTest {

    @Test
    fun `onReceive ar trebui sa anuleze notificarea cand primeste un ID valid`() {
        // Arrange (Pregătirea datelor)
        val mockContext = Mockito.mock(Context::class.java)
        val mockNotificationManager = Mockito.mock(NotificationManager::class.java)
        val mockIntent = Mockito.mock(Intent::class.java)

        val testNotificationId = 1001

        // Folosim Mockito.doReturn(...) pentru a evita confuzia compilatorului Kotlin
        Mockito.doReturn(mockNotificationManager)
            .`when`(mockContext).getSystemService(Context.NOTIFICATION_SERVICE)

        Mockito.doReturn(testNotificationId)
            .`when`(mockIntent).getIntExtra("NOTIFICATION_ID", -1)

        val receiver = NotificationDismissReceiver()

        // Act (Execuția)
        receiver.onReceive(mockContext, mockIntent)

        // Assert (Verificarea)
        Mockito.verify(mockNotificationManager).cancel(testNotificationId)
    }
}