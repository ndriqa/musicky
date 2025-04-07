package com.ndriqa.musicky.navigation

import android.os.Build
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.ndriqa.musicky.core.util.extensions.debugBorder
import com.ndriqa.musicky.features.player.HustlePlayer
import com.ndriqa.musicky.features.player.PlayerViewModel
import com.ndriqa.musicky.features.songs.SongsScreen
import com.ndriqa.musicky.features.songs.SongsViewModel

@Composable
fun AppNavigation(
    modifier: Modifier = Modifier,
) {
    val navController = rememberNavController()
    val songsViewModel: SongsViewModel = hiltViewModel()
    val playerViewModel: PlayerViewModel = hiltViewModel()

    val context = LocalContext.current
    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            songsViewModel.startLoadingSongs(context)
        } else {
            Toast.makeText(context, "Need the permission to load songs", Toast.LENGTH_SHORT).show()
        }
    }

    LaunchedEffect(Unit) {
        val permission = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            android.Manifest.permission.READ_MEDIA_AUDIO
        } else {
            android.Manifest.permission.READ_EXTERNAL_STORAGE
        }
        permissionLauncher.launch(permission)
    }


    Scaffold(
        modifier = modifier,
        floatingActionButton = { HustlePlayer(playerViewModel = playerViewModel) },
        containerColor = Color.Transparent
    ) { paddingValues ->

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
            composable(Screens.ScreenTwo.route) { backStackEntry ->
//                val stateHandle = navController
//                    .previousBackStackEntry
//                    ?.savedStateHandle
//                val sudokuBoard: SudokuBoard? = stateHandle?.get<SudokuBoard>("sudokuBoard")
//                val selectedLevel: Level? = stateHandle?.get<Level>("selectedLevel")
//
//                if (sudokuBoard == null || selectedLevel == null) {
//                    LaunchedEffect(Unit) {
//                        navController.popBackStack(Screens.Home.route, inclusive = false)
//                    }
//                } else {
//                    SudokuScreen(
//                        navController = navController,
//                        sudokuBoard = sudokuBoard,
//                        selectedLevel = selectedLevel,
//                        soundsViewModel = soundsViewModel,
//                        optionsViewModel = optionsViewModel
//                    )
//                }
            }
        }
    }
}
