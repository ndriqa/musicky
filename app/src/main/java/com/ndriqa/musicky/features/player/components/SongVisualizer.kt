package com.ndriqa.musicky.features.player.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.ndriqa.musicky.core.util.extensions.waveformToPath
import com.ndriqa.musicky.core.util.helpers.MockHelper
import com.ndriqa.musicky.features.player.MAX_BYTE_VAL
import com.ndriqa.musicky.ui.theme.MusickyTheme
import com.ndriqa.musicky.ui.theme.PaddingDefault

@Composable
internal fun ColumnScope.SongVisualizer(
    waveform: ByteArray,
    pulse: Boolean = false,
) {
    val lineColor = MaterialTheme.colorScheme.onPrimaryContainer

    Canvas(
        modifier = Modifier
            .fillMaxWidth()
            .height(MAX_BYTE_VAL.dp)
            .padding(horizontal = PaddingDefault),
    ) {
        drawPath(
            path = waveform.waveformToPath(size.width, size.height),
            brush = SolidColor(lineColor),
            style = Stroke(
                width = 1.dp.toPx(),
                cap = StrokeCap.Round,
                join = StrokeJoin.Round
            )
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun SongVisualizerPreview() {
    MusickyTheme {
        Column {
            SongVisualizer(MockHelper.getMockWaveform())
        }
    }
}