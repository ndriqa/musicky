package com.ndriqa.musicky.features.player.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.ui.Modifier
import com.ndriqa.musicky.core.data.VisualizerType
import com.ndriqa.musicky.core.util.extensions.correctRotation
import com.ndriqa.musicky.ui.theme.PaddingCompact
import com.ndriqa.musicky.ui.theme.PaddingMini

@Composable
internal fun VisualizerSelector(
    selectedVisualizerType: VisualizerType,
    onVisualizerChange: (VisualizerType) -> Unit,
    modifier: Modifier = Modifier
) {
    Box(modifier = modifier) {
        var showVisualSelectorDropdown by remember { mutableStateOf(false) }

        Button(
            onClick = { showVisualSelectorDropdown = true },
            contentPadding = PaddingValues(PaddingMini)
        ) {
            Icon(
                imageVector = selectedVisualizerType.icon,
                contentDescription = null,
                modifier = Modifier.correctRotation(selectedVisualizerType)
            )
        }

        DropdownMenu(
            expanded = showVisualSelectorDropdown,
            onDismissRequest = { showVisualSelectorDropdown = false },
            shape = RoundedCornerShape(PaddingCompact)
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
                        contentDescription = null,
                        modifier = Modifier.correctRotation(type)
                    ) }
                )
            }
        }
    }
}