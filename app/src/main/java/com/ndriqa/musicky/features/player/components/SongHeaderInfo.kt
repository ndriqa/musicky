package com.ndriqa.musicky.features.player.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ndriqa.musicky.core.data.Song
import com.ndriqa.musicky.core.util.helpers.MockHelper
import com.ndriqa.musicky.ui.theme.MusickyTheme
import com.ndriqa.musicky.ui.theme.PaddingMini

@Composable
internal fun ColumnScope.SongHeaderInfo(song: Song) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .weight(1F)
            .heightIn(max = 200.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(PaddingMini, Alignment.CenterVertically)
    ) {
        Text(
            text = song.title,
            fontSize = 25.sp,
            color = MaterialTheme.colorScheme.onPrimaryContainer,
            modifier = Modifier.fillMaxWidth(),
            textAlign = TextAlign.Center
        )
        Text(
            text = song.artist,
            fontSize = 19.sp,
            color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = .5f),
            modifier = Modifier.fillMaxWidth(),
            textAlign = TextAlign.Center
        )
    }
}

@Preview(showBackground = true, heightDp = 200)
@Composable
private fun SongHeaderInfoPreview() {
    MusickyTheme {
        Column {
            SongHeaderInfo(MockHelper.getMockSong())
        }
    }
}