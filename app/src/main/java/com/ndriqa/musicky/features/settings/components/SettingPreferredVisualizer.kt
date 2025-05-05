package com.ndriqa.musicky.features.settings.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import com.ndriqa.musicky.R
import com.ndriqa.musicky.core.data.VisualizerType
import com.ndriqa.musicky.core.util.extensions.correctRotation
import com.ndriqa.musicky.ui.theme.PaddingCompact

@Composable
internal fun ColumnScope.SettingPreferredVisualizer(
    preferredVisualizer: VisualizerType,
    onVisualizerTypeUpdate: (VisualizerType) -> Unit,
    modifier: Modifier = Modifier
) {
    SettingsItemSection(
        modifier = modifier,
        content = {
            SettingsItemTitle(stringResource(R.string.preferred_visualizer))
            Box {
                var showVisualSelectorDropdown by remember { mutableStateOf(false) }

                Button(
                    onClick = { showVisualSelectorDropdown = true },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.onPrimaryContainer,
                        contentColor = MaterialTheme.colorScheme.primaryContainer,
                    )
                ) {
                    Icon(
                        imageVector = preferredVisualizer.icon,
                        contentDescription = null,
                        modifier = preferredVisualizer.correctRotation()
                    )
                    Spacer(modifier = Modifier.size(PaddingCompact))
                    Text(preferredVisualizer.title)
                }

                DropdownMenu(
                    expanded = showVisualSelectorDropdown,
                    onDismissRequest = { showVisualSelectorDropdown = false },
                    shape = RoundedCornerShape(PaddingCompact),
                ) {
                    VisualizerType.entries.forEach { type ->
                        DropdownMenuItem(
                            text = { Text(type.title) },
                            onClick = {
                                onVisualizerTypeUpdate(type)
                                showVisualSelectorDropdown = false
                            },
                            leadingIcon = { Icon(
                                imageVector = type.icon,
                                contentDescription = null,
                                modifier = type.correctRotation()
                            ) },
                        )
                    }
                }
            }
        }
    )
}