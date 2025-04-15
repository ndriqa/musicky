package com.ndriqa.musicky.navigation

import android.os.Build
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.StringRes
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.ndriqa.musicky.R
import com.ndriqa.musicky.core.util.extensions.debugBorder
import com.ndriqa.musicky.core.util.permissionHandlers.MediaPermissionHandler
import com.ndriqa.musicky.core.util.permissionHandlers.NotificationPermissionHandler
import com.ndriqa.musicky.core.util.permissionHandlers.RecordingPermissionHandling
import com.ndriqa.musicky.features.player.HustlePlayer
import com.ndriqa.musicky.features.player.PlayerViewModel
import com.ndriqa.musicky.features.songs.SongsScreen
import com.ndriqa.musicky.features.songs.SongsViewModel
import com.ndriqa.musicky.ui.theme.PaddingCompact
import com.ndriqa.musicky.ui.theme.PaddingDefault
import timber.log.Timber

@Composable
fun AppNavigation(
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val navController = rememberNavController()
    val songsViewModel: SongsViewModel = hiltViewModel()
    val playerViewModel: PlayerViewModel = hiltViewModel()

    var hasDeniedMediaPermission by remember { mutableStateOf(false) }
    var hasDeniedNotificationPermission by remember { mutableStateOf(false) }
    var hasDeniedRecordingPermission by remember { mutableStateOf(false) }
    var requestMediaPermission: (() -> Unit)? by remember { mutableStateOf(null) }
    var requestNotificationPermission: (() -> Unit)? by remember { mutableStateOf(null) }
    var requestRecordingPermission: (() -> Unit)? by remember { mutableStateOf(null) }

    RecordingPermissionHandling(
        onPermissionGranted = {
            hasDeniedRecordingPermission = false
            Timber.d("✅ Recording permission granted") },
        onPermissionDenied = {
            hasDeniedRecordingPermission = true
            Timber.w("❌ Recording permission denied") },
        onRetryContent = { launcher -> requestRecordingPermission = launcher }
    )

    NotificationPermissionHandler(
        onPermissionGranted = {
            hasDeniedNotificationPermission = false
            Timber.d("✅ Notification permission granted") },
        onPermissionDenied = {
            hasDeniedNotificationPermission = true
            Timber.w("❌ Notification permission denied") },
        onRetryContent = { launcher -> requestNotificationPermission = launcher }
    )

    MediaPermissionHandler(
        onPermissionGranted = {
            hasDeniedMediaPermission = false
            Timber.d("✅ Media permission granted")
            songsViewModel.startLoadingSongs(context) },
        onPermissionDenied = {
            hasDeniedMediaPermission = true
            Timber.w("❌ Media permission denied") },
        onRetryContent = { launcher -> requestMediaPermission = launcher }
    )

    Scaffold(
        modifier = modifier,
        floatingActionButton = { HustlePlayer(
            hasVisualizerRecordingPermission = !hasDeniedRecordingPermission,
            playerViewModel = playerViewModel
        ) },
        containerColor = Color.Transparent
    ) { paddingValues ->

        if (hasDeniedMediaPermission || hasDeniedNotificationPermission) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .padding(PaddingDefault),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(PaddingCompact, Alignment.CenterVertically)
            ) {
                @StringRes val titleResId = when {
                    hasDeniedMediaPermission -> R.string.missing_media_permission
                    hasDeniedNotificationPermission -> R.string.missing_notification_permission
                    hasDeniedRecordingPermission -> R.string.missing_recording_permission
                    else -> null
                }

                @StringRes val messageResId = when {
                    hasDeniedMediaPermission -> R.string.missing_permissions_message
                    hasDeniedNotificationPermission -> R.string.missing_notification_permission_message
                    hasDeniedRecordingPermission -> R.string.missing_recording_permission_message
                    else -> null
                }

                titleResId?.let {
                    Text(
                        text = stringResource(it),
                        fontSize = 24.sp,
                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                        textAlign = TextAlign.Center
                    )
                }

                messageResId?.let {
                    Text(
                        text = stringResource(it),
                        fontSize = 15.sp,
                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                        textAlign = TextAlign.Center
                    )
                }

                OutlinedButton(
                    onClick = {
                        if (hasDeniedMediaPermission) requestMediaPermission?.invoke()
                        else requestNotificationPermission?.invoke()
                    },
                ) {
                    Text(
                        text = stringResource(R.string.retry),
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                }
            }
        } else {
            NavHost(
                modifier = Modifier.padding(paddingValues),
                navController = navController,
                startDestination = Screens.Songs.route,
            ) {
                composable(Screens.Songs.route) {
                    SongsScreen(
                        songsViewModel = songsViewModel,
                        playerViewModel = playerViewModel
                    )
                }
                composable(Screens.About.route) {  }
                composable(Screens.Options.route) {  }
//            composable(Screens.ScreenTwo.route) { backStackEntry -> }
            }
        }
    }
}
