package com.ndriqa.musicky.features.player.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.style.TextOverflow
import com.ndriqa.musicky.core.data.PlayingState

@Composable
internal fun NextSongUi(
    playingState: PlayingState,
    modifier: Modifier = Modifier
) {
    AnimatedVisibility(playingState.nextSong != null) {
        Text(
            text = buildAnnotatedString {
                append("Coming up: ")
                append(playingState.nextSong?.title)
            },
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}