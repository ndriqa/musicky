package com.ndriqa.musicky.features.songs.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import com.ndriqa.musicky.ui.theme.PaddingHalf

@Composable
internal fun RowScope.SearchButton(onSearchToggle: () -> Unit, enabled: Boolean) {
    val contentColor = MaterialTheme.colorScheme.onPrimaryContainer
    Box(
        modifier = Modifier
            .padding(horizontal = PaddingHalf)
            .size(44.dp)
            .clip(RoundedCornerShape(PaddingHalf))
            .then(if (enabled) Modifier.clickable { onSearchToggle() } else Modifier)
    ) {
        Icon(
            imageVector = Icons.Rounded.Search,
            contentDescription = null,
            modifier = Modifier.align(Alignment.Center),
            tint = contentColor.copy(alpha = if (enabled) 1f else 0.3f)
        )
    }
}