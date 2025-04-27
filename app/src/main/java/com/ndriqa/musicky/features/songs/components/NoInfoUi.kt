package com.ndriqa.musicky.features.songs.components

import androidx.annotation.StringRes
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.MusicNote
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.sp
import com.ndriqa.musicky.R
import com.ndriqa.musicky.features.songs.MusicTab
import com.ndriqa.musicky.ui.theme.MusicIconArtworkSizeCompact
import com.ndriqa.musicky.ui.theme.PaddingCompact

@Composable
internal fun ColumnScope.NoInfoUi(
    selectedTab: MusicTab,
    modifier: Modifier = Modifier
) {
    @StringRes val noInfoTextResId = when(selectedTab) {
        MusicTab.Songs -> R.string.where_the_songs_at
        MusicTab.Albums -> R.string.where_the_albums_at
    }
    val contentColor = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = .7f)

    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(PaddingCompact, Alignment.CenterVertically)
    ) {
        Icon(
            imageVector = Icons.Rounded.MusicNote,
            modifier = Modifier.size(MusicIconArtworkSizeCompact),
            contentDescription = null,
            tint = contentColor
        )

        Text(
            text = stringResource(noInfoTextResId),
            modifier = Modifier.fillMaxWidth(),
            textAlign = TextAlign.Center,
            fontSize = 20.sp,
            color = contentColor
        )
    }
}