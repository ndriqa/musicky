package com.ndriqa.musicky.features.player.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.ndriqa.musicky.core.data.VisualizerType

@Composable
fun SongTopBar(
    selectedVisualizerType: VisualizerType,
    onSettingsClick: () -> Unit,
    onVisualizerChange: (VisualizerType) -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        var showVisualSelectorDropdown by remember { mutableStateOf(false) }

        Box {
            Button(onClick = { showVisualSelectorDropdown = true }) {
                Icon(
                    imageVector = selectedVisualizerType.icon,
                    contentDescription = null,
                )
            }

            DropdownMenu(
                expanded = showVisualSelectorDropdown,
                onDismissRequest = { showVisualSelectorDropdown = false }
            ) {
                VisualizerType.entries.forEach { type ->
                    DropdownMenuItem(
                        text = { Text(type.title) },
                        onClick = {
                            onVisualizerChange(type)
                            showVisualSelectorDropdown = false
                        },
                        leadingIcon = { Icon(
                            imageVector = type.icon,
                            contentDescription = null
                        ) }
                    )
                }
            }
        }

        Button(onClick = onSettingsClick) {
            Icon(
                imageVector = Icons.Rounded.Settings,
                contentDescription = null,
            )
        }
    }
}