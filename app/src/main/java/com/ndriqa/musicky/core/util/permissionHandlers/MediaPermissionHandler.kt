package com.ndriqa.musicky.core.util.permissionHandlers

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.provider.Settings
import android.widget.Toast
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

private const val STORAGE_SETTINGS_NAME = "permission_settings"

@Composable
fun MediaPermissionHandler(
    onPermissionGranted: () -> Unit,
    onPermissionDenied: () -> Unit,
    onRetryContent: @Composable (requestPermission: () -> Unit) -> Unit
) {
    val activity = LocalActivity.current
    val context = LocalContext.current
    val showDialog = remember { mutableStateOf(false) }
    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            onPermissionGranted()
        } else {
            Toast.makeText(
                context,
                context.getString(R.string.need_the_permission_to_load_songs),
                Toast.LENGTH_SHORT
            ).show()
            onPermissionDenied()
        }
    }

    val permission = remember {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            Manifest.permission.READ_MEDIA_AUDIO
        } else {
            Manifest.permission.READ_EXTERNAL_STORAGE
        }
    }

    val contentColor = MaterialTheme.colorScheme.onPrimaryContainer

    LaunchedEffect(Unit) {
        val granted = ContextCompat.checkSelfPermission(context, permission) == PackageManager.PERMISSION_GRANTED
        if (!granted) {
            showDialog.value = true
        } else {
            onPermissionGranted()
        }
    }

    if (showDialog.value) {
        AlertDialog(
            onDismissRequest = { showDialog.value = false },
            title = { Text(stringResource(R.string.enable_media)) },
            text = {
                Text(stringResource(R.string.enable_media_message))
            },
            confirmButton = {
                TextButton(onClick = {
                    showDialog.value = false
                    val shouldShow = activity?.let {
                        ActivityCompat.shouldShowRequestPermissionRationale(
                            it, permission
                        )
                    } == true

                    if (shouldShow) {
                        permissionLauncher.launch(permission)
                    } else {
                        Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
                            .apply { data = Uri.fromParts("package", context.packageName, null) }
                            .highlightSettingsTo(STORAGE_SETTINGS_NAME)
                            .also { context.startActivity(it) }
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

    onRetryContent { showDialog.value = true }
}
