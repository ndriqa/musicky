package com.ndriqa.musicky.features.player.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.ndriqa.musicky.core.data.AudioFeatures
import com.ndriqa.musicky.core.data.FftFeatures
import com.ndriqa.musicky.core.data.VisualizerType
import com.ndriqa.musicky.core.util.extensions.waveformToPath
import com.ndriqa.musicky.core.util.helpers.MockHelper
import com.ndriqa.musicky.ui.theme.MusickyTheme
import com.ndriqa.musicky.ui.theme.PaddingCompact

@Composable
internal fun ColumnScope.SongVisualizer(
    waveform: ByteArray,
    audioFeatures: AudioFeatures,
    fftFeatures: FftFeatures,
    modifier: Modifier = Modifier,
    type: VisualizerType = VisualizerType.LineCenter,
    lineColor: Color = MaterialTheme.colorScheme.onPrimaryContainer
) {
    val lineWidth = when {
        type == VisualizerType.Bars -> 1.dp
        fftFeatures.isBass -> 2.dp
        else -> 1.dp
    }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .weight(1f)
            .padding(PaddingCompact),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            drawPath(
                path = waveform.waveformToPath(
                    width = size.width,
                    height = size.height,
                    visualizerType = type
                ),
                brush = SolidColor(lineColor),
                style = Stroke(
                    width = lineWidth.toPx(),
                    cap = StrokeCap.Round,
                    join = StrokeJoin.Round
                )
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun SongVisualizerPreview() {
    MusickyTheme {
        Column {
            SongVisualizer(
                waveform = MockHelper.getMockWaveform(),
                audioFeatures = MockHelper.getMockAudioFeatures(),
                fftFeatures = MockHelper.getMockFftFeatures()
            )
        }
    }
}