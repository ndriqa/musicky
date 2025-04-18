package com.ndriqa.musicky.features.songs

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Album
import androidx.compose.material.icons.rounded.MusicNote
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.Dp

internal const val DELIMITER = " - "

internal enum class MusicTab(
    val title: String,
    val icon: ImageVector
) {
    Songs(
        title = "Songs",
        icon = Icons.Rounded.MusicNote
    ),
    Albums(
        title = "Albums",
        icon = Icons.Rounded.Album
    )
}

internal val TabIndicatorAnimationSpec = spring<Dp>(
    stiffness = Spring.StiffnessMedium,
    dampingRatio = Spring.DampingRatioNoBouncy
)