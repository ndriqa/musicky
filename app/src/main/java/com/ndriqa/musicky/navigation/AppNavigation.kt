package com.ndriqa.musicky.navigation

import androidx.annotation.StringRes
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.ndriqa.musicky.R
import com.ndriqa.musicky.core.util.permissionHandlers.MediaPermissionHandler
import com.ndriqa.musicky.core.util.permissionHandlers.NotificationPermissionHandler
import com.ndriqa.musicky.core.util.permissionHandlers.RecordingPermissionHandling
import com.ndriqa.musicky.features.player.HustlePlayer
import com.ndriqa.musicky.features.player.PlayerViewModel
import com.ndriqa.musicky.features.settings.SettingsScreen
import com.ndriqa.musicky.features.settings.SettingsViewModel
import com.ndriqa.musicky.features.songs.SongsScreen
import com.ndriqa.musicky.features.songs.SongsViewModel
import com.ndriqa.musicky.ui.theme.PaddingCompact
import com.ndriqa.musicky.ui.theme.PaddingDefault

@Composable
fun AppNavigation(
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val navController = rememberNavController()
    val songsViewModel: SongsViewModel = hiltViewModel()
    val playerViewModel: PlayerViewModel = hiltViewModel()
    val settingsViewModel: SettingsViewModel = hiltViewModel()

    var hasDeniedMediaPermission by remember { mutableStateOf(false) }
    var hasDeniedNotificationPermission by remember { mutableStateOf(false) }
    var hasDeniedRecordingPermission by remember { mutableStateOf(false) }
    var requestMediaPermission: (() -> Unit)? by remember { mutableStateOf(null) }
    var requestNotificationPermission: (() -> Unit)? by remember { mutableStateOf(null) }
    var requestRecordingPermission: (() -> Unit)? by remember { mutableStateOf(null) }

    val playState by playerViewModel.playingState.collectAsState()
    val minAudioLength by settingsViewModel.minAudioLength.collectAsState()

    val isHustlePlayerVisible by remember { derivedStateOf { playState.currentSong != null } }
    var isHustlePlayerExpanded by remember { mutableStateOf(false) }

    RecordingPermissionHandling(
        onPermissionGranted = { hasDeniedRecordingPermission = false },
        onPermissionDenied = { hasDeniedRecordingPermission = true },
        onRetryContent = { launcher -> requestRecordingPermission = launcher }
    )

    NotificationPermissionHandler(
        onPermissionGranted = { hasDeniedNotificationPermission = false },
        onPermissionDenied = { hasDeniedNotificationPermission = true },
        onRetryContent = { launcher -> requestNotificationPermission = launcher }
    )

    MediaPermissionHandler(
        onPermissionGranted = {
            hasDeniedMediaPermission = false
            songsViewModel.startLoadingSongs(context) },
        onPermissionDenied = { hasDeniedMediaPermission = true },
        onRetryContent = { launcher -> requestMediaPermission = launcher }
    )

    LaunchedEffect(minAudioLength) {
        if (!hasDeniedMediaPermission) {
            songsViewModel.startLoadingSongs(context)
        }
    }

    DisposableEffect(Unit) {
        playerViewModel.apply {
            registerPlayerUpdates(context)
            registerVisualizerUpdates(context)
        }

        onDispose {
            playerViewModel.apply {
                unregisterPlayerUpdates(context)
                unregisterVisualizerUpdates(context)
            }
        }
    }

    Scaffold(
        modifier = Modifier,
        floatingActionButton = { HustlePlayer(
            hasVisualizerRecordingPermission = !hasDeniedRecordingPermission,
            onExpandedUpdate = { isHustlePlayerExpanded = it },
            navController = navController,
            isExpanded = isHustlePlayerExpanded,
            isVisible = isHustlePlayerVisible,
            playerViewModel = playerViewModel,
            settingsViewModel = settingsViewModel
        ) },
        containerColor = Color.Transparent
    ) { paddingValues ->

        if (hasDeniedMediaPermission || hasDeniedNotificationPermission) {
            NoPermissionsPseudoScreen(
                deniedMedia = hasDeniedMediaPermission,
                deniedNotification = hasDeniedNotificationPermission,
                deniedRecording = hasDeniedRecordingPermission,
                onRetryMedia = { requestMediaPermission?.invoke() },
                onRetryNotification = { requestNotificationPermission?.invoke() },
                onRetryRecording = { requestRecordingPermission?.invoke() },
                modifier = Modifier.padding(paddingValues)
            )
        } else {
            val shouldBlur = isHustlePlayerVisible && isHustlePlayerExpanded
            val blurRadius by animateDpAsState(
                targetValue = if (shouldBlur) 10.dp else 0.dp,
                label = "blur radius animation"
            )
//            val blurTint by animateColorAsState(
//                targetValue =
//                    if (shouldBlur) MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.1f)
//                    else Color.Transparent,
//                label = "blur tint animation"
//            )

            Box(modifier = Modifier.fillMaxSize()) {
                NavHost(
                    modifier = Modifier
                        .padding(paddingValues)
                        .then(
                            if (shouldBlur) Modifier.blur(blurRadius)
                            else Modifier),
                    navController = navController,
                    startDestination = Screens.Songs,
                ) {
                    composable<Screens.Songs> {
                        SongsScreen(
                            navController = navController,
                            songsViewModel = songsViewModel,
                            playerViewModel = playerViewModel
                        )
                    }
                    composable<Screens.Settings> {
                        SettingsScreen(
                            navController = navController,
                            settingsViewModel = settingsViewModel,
                            playerViewModel = playerViewModel
                        )
                    }
                }

                if (shouldBlur) {
                    Spacer(modifier = Modifier
                        .fillMaxSize()
                        .clickable { }
                    )
                }
            }
        }
    }
}

@Composable
private fun NoPermissionsPseudoScreen(
    deniedMedia: Boolean,
    deniedNotification: Boolean,
    deniedRecording: Boolean,
    onRetryMedia: () -> Unit,
    onRetryNotification: () -> Unit,
    onRetryRecording: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(PaddingDefault),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(PaddingCompact, Alignment.CenterVertically),
    ) {
        @StringRes val titleResId = when {
            deniedMedia -> R.string.missing_media_permission
            deniedNotification -> R.string.missing_notification_permission
            deniedRecording -> R.string.missing_recording_permission
            else -> null
        }

        @StringRes val messageResId = when {
            deniedMedia -> R.string.missing_permissions_message
            deniedNotification -> R.string.missing_notification_permission_message
            deniedRecording -> R.string.missing_recording_permission_message
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
                when {
                    deniedMedia -> onRetryMedia()
                    deniedNotification -> onRetryNotification()
                    deniedRecording -> onRetryRecording()
                    else -> Unit
                }
            },
        ) {
            Text(
                text = stringResource(R.string.retry),
                color = MaterialTheme.colorScheme.onPrimaryContainer
            )
        }
    }
}
