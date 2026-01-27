package com.example.basicvideocapturer

import android.Manifest
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.rememberMultiplePermissionsState

/**
 * A wrapper Composable that acts as a gatekeeper.
 * It checks for Camera and Audio permissions.
 *
 * @param onPermissionsGranted logic to execute once (e.g., initializing a session).
 * @param content The UI to show when permissions are active (e.g., the video screen).
 */
@OptIn(ExperimentalPermissionsApi::class)
@Composable
fun VideoChatPermissionWrapper(
    onPermissionsGranted: () -> Unit,
    content: @Composable () -> Unit
) {
    val permissionsState = rememberMultiplePermissionsState(
        permissions = listOf(
            Manifest.permission.CAMERA,
            Manifest.permission.RECORD_AUDIO
        )
    )

    if (permissionsState.allPermissionsGranted) {
        // 1. Notify parent that permissions are ready (run once)
        LaunchedEffect(Unit) {
            onPermissionsGranted()
        }
        // 2. Show the actual video UI
        content()
    } else {
        // 3. Show a Rationale UI if permissions are missing
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(text = "Camera and Audio permissions are required for video chat.")

            Button(
                onClick = { permissionsState.launchMultiplePermissionRequest() },
                modifier = Modifier.padding(top = 16.dp)
            ) {
                Text("Grant Permissions")
            }
        }

        // Auto-launch the popup on the very first composition
        LaunchedEffect(Unit) {
            if (!permissionsState.allPermissionsGranted) {
                permissionsState.launchMultiplePermissionRequest()
            }
        }
    }
}