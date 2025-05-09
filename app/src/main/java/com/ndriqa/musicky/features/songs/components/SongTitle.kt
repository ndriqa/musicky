package com.ndriqa.musicky.features.songs.components

import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow

@Composable
internal fun SongTitle(
    text: String,
    modifier: Modifier = Modifier,
    isPlaying: Boolean = false
) {
    Text(
        modifier = modifier,
        text = text,
        fontWeight = if (isPlaying) FontWeight.ExtraBold else FontWeight.Bold,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis
    )
}