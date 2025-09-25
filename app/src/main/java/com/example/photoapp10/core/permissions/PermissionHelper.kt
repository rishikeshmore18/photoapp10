package com.example.photoapp10.core.permissions

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.provider.Settings
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.platform.LocalContext
import com.google.accompanist.permissions.*

@OptIn(ExperimentalPermissionsApi::class)
@Composable
fun CameraPermission(
    onGranted: @Composable () -> Unit,
    rationale: String = "Camera permission is needed to capture photos."
) {
    val context = LocalContext.current
    val permissionState = rememberPermissionState(android.Manifest.permission.CAMERA)

    when {
        permissionState.status.isGranted -> onGranted()
        permissionState.status.shouldShowRationale -> {
            PermissionRationale(
                text = rationale,
                onRequest = { permissionState.launchPermissionRequest() }
            )
        }
        else -> {
            PermissionRationale(
                text = rationale,
                onRequest = { permissionState.launchPermissionRequest() },
                onOpenSettings = {
                    val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                        data = Uri.fromParts("package", context.packageName, null)
                    }
                    context.startActivity(intent)
                }
            )
        }
    }
}

@Composable
private fun PermissionRationale(
    text: String,
    onRequest: () -> Unit,
    onOpenSettings: (() -> Unit)? = null
) {
    androidx.compose.material3.AlertDialog(
        onDismissRequest = {},
        title = { androidx.compose.material3.Text("Permission needed") },
        text = { androidx.compose.material3.Text(text) },
        confirmButton = {
            androidx.compose.material3.TextButton(onClick = onRequest) {
                androidx.compose.material3.Text("Grant")
            }
        },
        dismissButton = {
            if (onOpenSettings != null) {
                androidx.compose.material3.TextButton(onClick = onOpenSettings) {
                    androidx.compose.material3.Text("Settings")
                }
            }
        }
    )
}














