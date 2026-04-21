package p2ps.android

import android.Manifest
import android.annotation.SuppressLint
import android.content.pm.PackageManager
import android.os.Bundle
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
import p2ps.android.ui.theme.P2PSAndroidTheme
import p2ps.android.data.TelemetryPing
import p2ps.android.data.TelemetryManager

class MainActivity : ComponentActivity() {

    private lateinit var telemetryManager: TelemetryManager
    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private val hardwareManager = HardwareManager()

    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val fineGranted = permissions[Manifest.permission.ACCESS_FINE_LOCATION] == true
        val coarseGranted = permissions[Manifest.permission.ACCESS_COARSE_LOCATION] == true

        if (fineGranted || coarseGranted) {
            Toast.makeText(this, getString(R.string.location_permission_granted), Toast.LENGTH_SHORT).show()
        } else {
            Toast.makeText(this, getString(R.string.location_permission_denied), Toast.LENGTH_LONG).show()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        telemetryManager = TelemetryManager(this)
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)
        
        // Initialize Hardware SDK
        hardwareManager.initialize()

        checkLocationPermission()

        if (savedInstanceState == null) {
            // Task #149: Simulation of a startup hardware trigger
            onHardwareTriggerReceived(
                storeId = "Lidl_01",
                itemId = "Mere_Golden_05",
                triggerType = "STARTUP_AUTO_SCAN"
            )
        }

        enableEdgeToEdge()
        setContent {
            P2PSAndroidTheme {
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    WelcomeScreen(
                        onTriggerClick = {
                            onHardwareTriggerReceived("store_ABC", "item_123")
                        },
                        modifier = Modifier.padding(innerPadding)
                    )
                }
            }
        }
    }

    private fun checkLocationPermission() {
        val fineGranted = ContextCompat.checkSelfPermission(
            this, Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
        val coarseGranted = ContextCompat.checkSelfPermission(
            this, Manifest.permission.ACCESS_COARSE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED

        if (fineGranted || coarseGranted) return

        requestPermissionLauncher.launch(
            arrayOf(
                Manifest.permission.ACCESS_FINE_LOCATION,
                Manifest.permission.ACCESS_COARSE_LOCATION
            )
        )
    }

    @SuppressLint("MissingPermission")
    fun onHardwareTriggerReceived(storeId: String, itemId: String, deviceId: String = "usr_DEMO", triggerType: String = "HARDWARE") {
        val hasFineLocation = ContextCompat.checkSelfPermission(
            this, Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
        val hasCoarseLocation = ContextCompat.checkSelfPermission(
            this, Manifest.permission.ACCESS_COARSE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED

        if (!hasFineLocation && !hasCoarseLocation) {
            Toast.makeText(this, "Location permission missing for telemetry", Toast.LENGTH_SHORT).show()
            return
        }

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
                        accuracy = location.accuracy,
                        timestamp = System.currentTimeMillis()
                    )
                    
                    // 1. Save locally via existing TelemetryManager
                    telemetryManager.savePing(ping)
                    
                    // 2. Delegate to HardwareManager flow from Task #149
                    hardwareManager.handleHardwareTrigger(ping)

                    runOnUiThread {
                        Toast.makeText(this, "Telemetry processed: ${location.latitude}, ${location.longitude}", Toast.LENGTH_SHORT).show()
                    }
                }
            }
    }

    inner class WebAppInterface {
        @JavascriptInterface
        fun requestLocationPermission() {
            runOnUiThread { checkLocationPermission() }
        }
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
            Text("Simulate Hardware Trigger")
        }
    }
}
