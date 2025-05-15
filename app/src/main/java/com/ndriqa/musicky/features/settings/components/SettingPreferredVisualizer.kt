package com.ndriqa.musicky.features.settings.components

import androidx.compose.animation.AnimatedContent
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ExpandLess
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ndriqa.musicky.R
import com.ndriqa.musicky.core.data.AudioFeatures
import com.ndriqa.musicky.core.data.FftFeatures
import com.ndriqa.musicky.core.data.VisualizerType
import com.ndriqa.musicky.core.util.extensions.correctRotation
import com.ndriqa.musicky.core.util.helpers.floweryGaussianWaveform
import com.ndriqa.musicky.features.player.components.SongVisualizer
import com.ndriqa.musicky.ui.theme.PaddingCompact
import com.ndriqa.musicky.ui.theme.PaddingMini

@Composable
internal fun SettingPreferredVisualizer(
    preferredVisualizer: VisualizerType,
    onVisualizerTypeUpdate: (VisualizerType) -> Unit,
    modifier: Modifier = Modifier,
) {
    val sampleWaveform by rememberSaveable { mutableStateOf(floweryGaussianWaveform(500)) }
    var isExpanded by remember { mutableStateOf(false) }

    SettingsItemSection(
        modifier = modifier.then(
            if (isExpanded) Modifier.clickable { isExpanded = !isExpanded }
            else Modifier
        ),
        content = {
            AnimatedContent(isExpanded) { expanded ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    val title =
                        if (expanded) preferredVisualizer.title
                        else stringResource(R.string.preferred_visualizer)

                    Text(
                        text = title,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.weight(1f),
                        textAlign = if (expanded) TextAlign.Left else TextAlign.Center
                    )

                    if (expanded) {
                        Icon(
                            imageVector = Icons.Rounded.ExpandLess,
                            contentDescription = null
                        )
                    }
                }
            }
            Spacer(modifier = Modifier.height(PaddingMini))
            Box {
                fun onVisualizerTileClick(type: VisualizerType) {
                    onVisualizerTypeUpdate(type)
                }

                AnimatedContent(isExpanded) { expanded ->
                    if (expanded) {
                        Column(
                            modifier = Modifier.fillMaxWidth(),
                            verticalArrangement = Arrangement.spacedBy(PaddingCompact)
                        ) {
                            VisualizerType
                                .entries
                                .chunked(2)
                                .forEach {
                                    val first = it.first()
                                    val second = it.getOrNull(1)

                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(PaddingCompact)
                                    ) {
                                        VisualizerTypeTile(
                                            waveform = sampleWaveform,
                                            visualizerType = first,
                                            onVisualizerTypeClick = ::onVisualizerTileClick,
                                            isSelected = first == preferredVisualizer
                                        )

                                        second?.let {
                                            VisualizerTypeTile(
                                                waveform = sampleWaveform,
                                                visualizerType = second,
                                                onVisualizerTypeClick = ::onVisualizerTileClick,
                                                isSelected = second == preferredVisualizer
                                            )
                                        }
                                    }
                                }
                        }
                    } else {
                        Button(
                            onClick = { isExpanded = !isExpanded },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.onPrimaryContainer,
                                contentColor = MaterialTheme.colorScheme.primaryContainer,
                            )
                        ) {
                            Icon(
                                imageVector = preferredVisualizer.icon,
                                contentDescription = null,
                                modifier = Modifier.correctRotation(preferredVisualizer)
                            )
                            Spacer(modifier = Modifier.size(PaddingCompact))
                            Text(preferredVisualizer.title)
                        }
                    }
                }
            }
        }
    )
}

@Composable
private fun RowScope.VisualizerTypeTile(
    waveform: ByteArray,
    visualizerType: VisualizerType,
    modifier: Modifier = Modifier,
    onVisualizerTypeClick: (VisualizerType) -> Unit,
    isSelected: Boolean = false
) {
    val contentColor =
        if (isSelected) MaterialTheme.colorScheme.primaryContainer
        else MaterialTheme.colorScheme.onPrimaryContainer
    val containerColor =
        if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer
        else MaterialTheme.colorScheme.primaryContainer
    val cardShape = RoundedCornerShape(PaddingCompact)

    Box(
        modifier = modifier
            .weight(1f)
            .height(150.dp)
            .clip(cardShape)
            .background(
                color = containerColor,
                shape = cardShape
            )
            .border(
                width = 1.dp,
                color = MaterialTheme.colorScheme.onPrimaryContainer,
                shape = cardShape
            )
            .clickable { onVisualizerTypeClick(visualizerType) },
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier.fillMaxSize()
        ) {
            SongVisualizer(
                waveform = waveform,
                audioFeatures = AudioFeatures(),
                fftFeatures = FftFeatures(),
                modifier = Modifier,
                type = visualizerType,
                lineColor = contentColor
            )

            HorizontalDivider(
                thickness = 1.dp,
                modifier = Modifier.fillMaxWidth(),
                color = contentColor
            )

            Text(
                text = visualizerType.title,
                modifier = Modifier
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.onPrimaryContainer),
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.primaryContainer
            )
        }
    }
}