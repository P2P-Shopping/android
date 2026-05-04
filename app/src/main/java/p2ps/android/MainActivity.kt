package p2ps.android

import android.Manifest
import android.annotation.SuppressLint
import android.content.pm.PackageManager
import android.os.Bundle
import android.util.Log
import android.webkit.JavascriptInterface
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import kotlinx.coroutines.launch
import p2ps.android.core.TelemetryDispatcher
import p2ps.android.data.DeviceIdManager
import p2ps.android.data.TelemetryManager
import p2ps.android.data.TelemetryPing
import p2ps.android.ui.theme.P2PSAndroidTheme
import java.util.UUID
import android.content.Intent

class MainActivity : ComponentActivity() {
    private lateinit var telemetryManager: TelemetryManager
    private lateinit var telemetryDispatcher: TelemetryDispatcher
    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private lateinit var hardwareManager: HardwareManager

    companion object {
        private const val DEFAULT_STORE_ID = "Lidl_01"
        private const val DEFAULT_ITEM_ID = "Background_Track"
    }

    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val fineGranted = permissions[Manifest.permission.ACCESS_FINE_LOCATION]
            ?: (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED)
        
        val notificationsGranted = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
            permissions[Manifest.permission.POST_NOTIFICATIONS]
                ?: (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED)
        } else true

        if (fineGranted) {
            Toast.makeText(this, "Location access granted!", Toast.LENGTH_SHORT).show()
            startLocationTrackingService()
            launchWebView()
            if (!notificationsGranted) {
                Toast.makeText(this, "Notifications disabled. Service will run silently.", Toast.LENGTH_LONG).show()
            }
        } else {
            Toast.makeText(this, "Permission denied. Please enable from settings.", Toast.LENGTH_LONG).show()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Initialize components
        telemetryManager = TelemetryManager(this)
        val database = p2ps.android.data.AppDatabase.getDatabase(this)
        telemetryDispatcher = TelemetryDispatcher(database.telemetryDao(), this)
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)
        hardwareManager = HardwareManager(telemetryDispatcher)
        
        hardwareManager.initialize()

        enableEdgeToEdge()
        setContent {
            P2PSAndroidTheme {
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    WelcomeScreen(
                        onTriggerClick = {
                            simulateHardwareTrigger()
                        },
                        modifier = Modifier.padding(innerPadding)
                    )
                }
            }
        }

        if (savedInstanceState == null) {
            checkLocationPermission()
        }
    }

    private fun simulateHardwareTrigger() {
        val fineLocation = ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
        if (fineLocation == PackageManager.PERMISSION_GRANTED) {
            onHardwareTriggerReceived("store_ABC", "item_123")
        } else {
            Toast.makeText(this, "Please allow location to simulate trigger", Toast.LENGTH_SHORT).show()
            checkLocationPermission()
        }
    }

    private fun checkLocationPermission() {
        val permissions = mutableListOf(
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.ACCESS_COARSE_LOCATION
        )
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
            permissions.add(Manifest.permission.POST_NOTIFICATIONS)
        }

        val missing = permissions.filter {
            ContextCompat.checkSelfPermission(this, it) != PackageManager.PERMISSION_GRANTED
        }
        
        if (missing.isEmpty()) {
            startLocationTrackingService()
            launchWebView()
        } else {
            requestPermissionLauncher.launch(missing.toTypedArray())
        }
    }

    @SuppressLint("MissingPermission")
    fun onHardwareTriggerReceived(storeId: String, itemId: String, triggerType: String = "HARDWARE") {
        val deviceId = DeviceIdManager.getDeviceId(this)
        fusedLocationClient.getCurrentLocation(Priority.PRIORITY_HIGH_ACCURACY, null)
            .addOnSuccessListener { location ->
                if (location != null) {
                    val ping = TelemetryPing(
                        deviceId = deviceId,
                        storeId = storeId,
                        itemId = itemId,
                        triggerType = triggerType,
                        lat = location.latitude,
                        lng = location.longitude,
                        accuracyMeters = location.accuracy,
                        timestamp = System.currentTimeMillis(),
                        pingId = UUID.randomUUID().toString()
                    )

                    lifecycleScope.launch {
                        telemetryDispatcher.dispatch(ping)
                    }

                    Toast.makeText(this, "Telemetry processed", Toast.LENGTH_SHORT).show()
                } else {
                    Log.e("MainActivity", "Location is null")
                }
            }
            .addOnFailureListener { e ->
                Log.e("MainActivity", "Failed to get location", e)
            }
    }

    private fun startLocationTrackingService() {
        val intent = android.content.Intent(this, p2ps.android.location.LocationService::class.java).apply {
            putExtra("EXTRA_DEVICE_ID", DeviceIdManager.getDeviceId(this@MainActivity))
            putExtra("EXTRA_STORE_ID", DEFAULT_STORE_ID)
            putExtra("EXTRA_ITEM_ID", DEFAULT_ITEM_ID)
        }
        startForegroundService(intent)
    }
    private fun launchWebView() {
        val intent = android.content.Intent(this, WebViewActivity::class.java)
        startActivity(intent)
        finish()
    }
}

@Composable
fun WelcomeScreen(onTriggerClick: () -> Unit, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier.fillMaxSize(),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "P2P Shopping",
            fontSize = 28.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary
        )
        Spacer(modifier = Modifier.height(16.dp))
        Button(onClick = onTriggerClick) {
            Text("Hardware Trigger")
        }
    }
}
