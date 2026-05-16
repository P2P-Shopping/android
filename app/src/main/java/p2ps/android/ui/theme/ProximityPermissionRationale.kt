package p2ps.android.ui.theme

import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.painterResource
import p2ps.android.R // Asigură-te că pachetul tău R este importat corect

@Composable
fun ProximityPermissionRationaleDialog(
    onContinueClick: () -> Unit,
    onDismissClick: () -> Unit
) {
    AlertDialog(
        onDismissRequest = {
            // Apelăm onDismissClick și dacă utilizatorul apasă în afara dialogului
            onDismissClick()
        },
        icon = {
            // Folosim iconița de proximitate pe care am creat-o la Task-ul 1
            Icon(
                painter = painterResource(id = R.drawable.ic_proximity_alert),
                contentDescription = "Proximity Icon"
            )
        },
        title = {
            Text(text = "Alerte de proximitate")
        },
        text = {
            Text(text = "Aplicația are nevoie de acces la locația ta în fundal pentru a te notifica atunci când te afli în apropierea unui item de interes, chiar și când nu folosești activ aplicația.")
        },
        confirmButton = {
            Button(
                onClick = onContinueClick
            ) {
                Text("Continue")
            }
        },
        dismissButton = {
            TextButton(
                onClick = onDismissClick
            ) {
                Text("Not now")
            }
        }
    )
}