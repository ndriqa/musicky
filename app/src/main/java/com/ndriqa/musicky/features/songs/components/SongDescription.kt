package com.ndriqa.musicky.features.songs.components

import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.sp
import com.ndriqa.musicky.features.songs.DELIMITER

@Composable
internal fun SongDescription(
    artist: String,
    modifier: Modifier = Modifier,
    timer: String? = null
) {
    Text(
        modifier = modifier,
        text = "${timer?.let { "$it$DELIMITER" } ?: ""}$artist",
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
        fontSize = 13.sp
    )
}