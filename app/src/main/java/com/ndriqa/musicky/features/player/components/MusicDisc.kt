package com.ndriqa.musicky.features.player.components

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.MusicNote
import androidx.compose.material.icons.rounded.RadioButtonChecked
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import com.ndriqa.musicky.core.data.PlayingState
import com.ndriqa.musicky.core.util.helpers.MockHelper
import com.ndriqa.musicky.ui.theme.MusicIconArtworkSizeBig
import com.ndriqa.musicky.ui.theme.MusicIconArtworkSizeCompact
import com.ndriqa.musicky.ui.theme.MusickyTheme
import com.ndriqa.musicky.ui.theme.PaddingDefault

@Composable
internal fun ColumnScope.MusicDisc(
    playState: PlayingState,
    modifier: Modifier = Modifier
) {
    val artworkShape = RoundedCornerShape(PaddingDefault)
    val fallbackIcon = rememberVectorPainter(Icons.Rounded.MusicNote)

    Box(
        modifier = modifier
            .size(MusicIconArtworkSizeBig)
            .clip(shape = artworkShape)
            .background(
                color = MaterialTheme.colorScheme.primaryContainer,
                shape = artworkShape
            )
    ) {
        val infiniteTransition = rememberInfiniteTransition(label = "rotation")
        val rotation by infiniteTransition.animateFloat(
            initialValue = 0f,
            targetValue = 360f,
            animationSpec = infiniteRepeatable(
                animation = tween(durationMillis = 5000, easing = LinearEasing),
                repeatMode = RepeatMode.Restart
            ),
            label = "infinite rotation"
        )

        AsyncImage(
            model = playState.currentSong?.artworkUri,
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer {
                    if (playState.isPlaying) {
                        rotationZ = rotation
                    }
                }
            ,
            fallback = fallbackIcon
        )

        Spacer(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    color = MaterialTheme.colorScheme.onPrimaryContainer.copy(
                        alpha = .5F
                    )
                )
        )

        Icon(
            imageVector = Icons.Rounded.RadioButtonChecked,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primaryContainer.copy(alpha = .75f),
            modifier = Modifier.align(Alignment.Center)
        )
    }
}

@Preview(widthDp = 60, heightDp = 60)
@Composable
private fun MusicDiscPreview() {
    MusickyTheme {
        Column {
            MusicDisc(MockHelper.getMockPlayingState())
        }
    }
}