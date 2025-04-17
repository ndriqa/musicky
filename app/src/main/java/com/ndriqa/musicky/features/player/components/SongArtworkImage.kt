package com.ndriqa.musicky.features.player.components

import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.MusicNote
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.tooling.preview.Preview
import coil3.compose.AsyncImage
import com.ndriqa.musicky.core.util.helpers.MockHelper
import com.ndriqa.musicky.ui.theme.MusicIconArtworkSizeBig
import com.ndriqa.musicky.ui.theme.MusickyTheme
import com.ndriqa.musicky.ui.theme.PaddingDefault

@Composable
internal fun ColumnScope.SongArtworkImage(artworkImageUri: Uri?) {
    val artworkShape = RoundedCornerShape(PaddingDefault)
    val fallbackIcon = rememberVectorPainter(Icons.Rounded.MusicNote)

    Box(
        modifier = Modifier
            .weight(1f)
//            .scale(animatedScale)
    ) {
        Box(
            modifier = Modifier
                .size(MusicIconArtworkSizeBig)
                .clip(shape = artworkShape)
                .background(
                    color = MaterialTheme.colorScheme.primaryContainer,
                    shape = artworkShape
                )
                .align(Alignment.Center)
        ) {
            AsyncImage(
                model = artworkImageUri,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize(),
                fallback = fallbackIcon
            )
        }
    }
}

@Preview(widthDp = 200, heightDp = 200)
@Composable
private fun SongArtworkImagePreview() {
    MusickyTheme {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            SongArtworkImage(MockHelper.getMockArtworkUri())
        }
    }
}