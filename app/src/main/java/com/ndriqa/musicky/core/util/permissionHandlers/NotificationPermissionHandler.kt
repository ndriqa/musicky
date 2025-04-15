package com.ndriqa.musicky.core.util.permissionHandlers

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.activity.compose.LocalActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.ndriqa.musicky.R
import com.ndriqa.musicky.core.util.extensions.highlightSettingsTo
import com.ndriqa.musicky.ui.theme.SoftCrimson

private const val NOTIFICATIONS_SETTINGS_NAME = "notification_settings"

@Composable
fun NotificationPermissionHandler(
    onPermissionGranted: () -> Unit = {},
    onPermissionDenied: () -> Unit = {},
    onRetryContent: @Composable (requestPermission: () -> Unit) -> Unit
) {
    val activity = LocalActivity.current
    val context = LocalContext.current
    val showDialog = remember { mutableStateOf(false) }
    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) onPermissionGranted() else onPermissionDenied()
    }
    val contentColor = MaterialTheme.colorScheme.onPrimaryContainer
    val isTiramisu = Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU

    val permission = remember {
        if (isTiramisu) Manifest.permission.POST_NOTIFICATIONS
        else null
    }

    LaunchedEffect(Unit) {
        if (isTiramisu) {
            val hasPermission = ContextCompat.checkSelfPermission(
                context,
                permission!!
            ) == PackageManager.PERMISSION_GRANTED

            if (!hasPermission) {
                showDialog.value = true
            } else {
                onPermissionGranted()
            }
        } else {
            onPermissionGranted()
        }
    }

    if (showDialog.value) {
        AlertDialog(
            onDismissRequest = { showDialog.value = false },
            title = { Text(stringResource(R.string.enable_notifications)) },
            text = { Text(stringResource(R.string.enable_notification_message)) },
            confirmButton = {
                TextButton(onClick = {
                    showDialog.value = false
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                        val shouldShow = activity?.let {
                            ActivityCompat.shouldShowRequestPermissionRationale(
                                it, Manifest.permission.POST_NOTIFICATIONS
                            )
                        } == true

                        permissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)

//                        if (shouldShow) {
//                            permissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
//                        } else {
//                            // user has denied permanently → send to app settings
//                            Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
//                                .apply { data = Uri.fromParts("package", context.packageName, null) }
//                                .highlightSettingsTo(NOTIFICATIONS_SETTINGS_NAME)
//                                .also { context.startActivity(it) }
//                        }
                    } else {
                        onPermissionGranted()
                    }
                }) { Text(
                    text = stringResource(R.string.allow),
                    color = contentColor,
                    fontWeight = FontWeight.Bold
                ) }
            },
            dismissButton = {
                TextButton(onClick = {
                    showDialog.value = false
                    onPermissionDenied()
                }) { Text(
                    text = stringResource(R.string.deny),
                    color = SoftCrimson,
                    fontWeight = FontWeight.Bold
                ) }
            },
            containerColor = MaterialTheme.colorScheme.primary,
            textContentColor = contentColor,
            iconContentColor = contentColor,
            titleContentColor = contentColor,
        )
    }

    onRetryContent { if (isTiramisu) showDialog.value = true }
}
