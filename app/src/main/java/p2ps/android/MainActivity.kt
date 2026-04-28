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
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import p2ps.android.core.TelemetryDispatcher
import p2ps.android.data.TelemetryManager
import p2ps.android.data.TelemetryPing
import p2ps.android.R
import p2ps.android.ui.theme.P2PSAndroidTheme

class MainActivity : ComponentActivity() {

    private lateinit var telemetryManager: TelemetryManager
    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private lateinit var hardwareManager: HardwareManager

    companion object {
        private const val DEFAULT_DEVICE_ID = "usr_DEMO"
        private const val DEFAULT_STORE_ID = "Lidl_01"
        private const val DEFAULT_ITEM_ID = "Background_Track"
    }

    // Handles permissions and UI/State updates ONLY.
    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val fineGranted =
            permissions[Manifest.permission.ACCESS_FINE_LOCATION]
                ?: (ContextCompat.checkSelfPermission(
                    this,
                    Manifest.permission.ACCESS_FINE_LOCATION
                ) == PackageManager.PERMISSION_GRANTED)
        val notificationsGranted =
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
                permissions[Manifest.permission.POST_NOTIFICATIONS]
                    ?: (ContextCompat.checkSelfPermission(
                        this,
                        Manifest.permission.POST_NOTIFICATIONS
                    ) == PackageManager.PERMISSION_GRANTED)
            } else true

        if (fineGranted) {
            Toast.makeText(this, "Location access granted!", Toast.LENGTH_SHORT).show()
            startLocationTrackingService()

            if (!notificationsGranted) {
                Toast.makeText(this, "Notifications disabled. Service will run silently.", Toast.LENGTH_LONG).show()
            }
            sendResultToWeb("Granted")

        } else {
            Toast.makeText(this, "Permission denied. Please enable from settings.", Toast.LENGTH_LONG).show()
            sendResultToWeb("Denied")
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        telemetryManager = TelemetryManager(this)
        telemetryDispatcher = p2ps.android.core.TelemetryDispatcher(ApiClient(), telemetryManager)
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)

        val apiClient = ApiClient(this)
        val telemetryDispatcher = TelemetryDispatcher(apiClient, telemetryManager)
        hardwareManager = HardwareManager(telemetryDispatcher)
        
        // Initialize Hardware SDK
        hardwareManager.initialize()

        if (savedInstanceState == null) {
            checkLocationPermission()
        }

        enableEdgeToEdge()
        setContent {
            P2PSAndroidTheme {
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    WelcomeScreen(
                        onTriggerClick = {
                            val fineLocation = ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                            val notificationsGranted = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
                                ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED
                            } else true

                            if (fineLocation == PackageManager.PERMISSION_GRANTED) {

                                if (!notificationsGranted) {
                                    Toast.makeText(this, "Telemetry active (without notifications)", Toast.LENGTH_SHORT).show()
                                } else {
                                    Toast.makeText(this, "Telemetry Started", Toast.LENGTH_SHORT).show()
                                }

                                onHardwareTriggerReceived("store_ABC", "item_123")
                                startLocationTrackingService()

                            } else {
                                Toast.makeText(this, "Please allow location to simulate trigger", Toast.LENGTH_SHORT).show()
                                checkLocationPermission()
                            }
                        },
                        modifier = Modifier.padding(innerPadding)
                    )
                }
            }
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
            sendResultToWeb("Granted")
            startLocationTrackingService()
            return
        }
        requestPermissionLauncher.launch(missing.toTypedArray())
    }

    /**
     * Entry point for the hardware trigger.
     * Builds the exact JSON-mapped payload after fetching real GPS data.
     */
    @SuppressLint("MissingPermission")
    fun onHardwareTriggerReceived(storeId: String, itemId: String, deviceId: String = "usr_DEMO", triggerType: String = "HARDWARE") {
        val hasFineLocation = ContextCompat.checkSelfPermission(
            this, Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
        val hasCoarseLocation = ContextCompat.checkSelfPermission(
            this, Manifest.permission.ACCESS_COARSE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED

        if (!hasFineLocation && !hasCoarseLocation) {
            println("Telemetry Error: Cannot trigger ping, location permissions are missing.")
            return
        }

        // Fetch the most accurate current location
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
                        timestamp = System.currentTimeMillis()
                    )

                    telemetryDispatcher.dispatch(ping)

                    runOnUiThread {
                        Toast.makeText(this, "Telemetry processed: ${location.latitude}, ${location.longitude}", Toast.LENGTH_SHORT).show()
                    }
                    println("Telemetry Success: Ping saved for device $deviceId")
                } else {
                    println("Telemetry Error: Location returned null.")
                    runOnUiThread {
                        Toast.makeText(this, "Error: Location is null", Toast.LENGTH_SHORT).show()
                    }
                }
            }
            .addOnFailureListener { exception ->
                println("Telemetry Error: Failed to retrieve location - ${exception.localizedMessage}")
                runOnUiThread {
                    Toast.makeText(this, "Error: ${exception.localizedMessage}", Toast.LENGTH_SHORT).show()
                }
            }
    }

    private fun sendResultToWeb(result: String) {
        println("Bridge Result: $result")
    }

    inner class WebAppInterface {
        @JavascriptInterface
        fun requestLocationPermission() {
            runOnUiThread { checkLocationPermission() }
        }
    }

    private fun startLocationTrackingService() {
        val intent = android.content.Intent(this, p2ps.android.location.LocationService::class.java).apply {
            putExtra("EXTRA_DEVICE_ID", DEFAULT_DEVICE_ID)
            putExtra("EXTRA_STORE_ID", DEFAULT_STORE_ID)
            putExtra("EXTRA_ITEM_ID", DEFAULT_ITEM_ID)
        }
        startForegroundService(intent)
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