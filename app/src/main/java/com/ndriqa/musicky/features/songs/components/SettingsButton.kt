package com.ndriqa.musicky.features.songs.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import com.ndriqa.musicky.ui.theme.PaddingHalf
import com.ndriqa.musicky.ui.theme.TopBarButtonsSize

@Composable
internal fun RowScope.SettingsButton(onSettingsClicked: () -> Unit) {
    val contentColor = MaterialTheme.colorScheme.onPrimaryContainer
    Box(
        modifier = Modifier
            .padding(horizontal = PaddingHalf)
            .size(TopBarButtonsSize)
            .clip(RoundedCornerShape(PaddingHalf))
            .clickable { onSettingsClicked() }
    ) {
        Icon(
            imageVector = Icons.Rounded.Settings,
            contentDescription = null,
            modifier = Modifier.align(Alignment.Center),
            tint = contentColor
        )
    }
}