package com.ndriqa.musicky

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import com.ndriqa.musicky.navigation.AppNavigation
import com.ndriqa.musicky.ui.theme.MusickyTheme
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MusickyTheme(dynamicColor = false) {
                Scaffold(
                    modifier = Modifier.fillMaxSize(),
                    containerColor = MaterialTheme.colorScheme.primaryContainer
                ) { innerPadding ->
                    AppUi(modifier = Modifier.padding(innerPadding))
                }
            }
        }
    }
}

@Composable
fun AppUi(modifier: Modifier) {
    AppNavigation(modifier)
}

@Preview(showBackground = true)
@Composable
fun AppUiPreview() {
    MusickyTheme {
        Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
            AppUi(modifier = Modifier.padding(innerPadding))
        }
    }
}
