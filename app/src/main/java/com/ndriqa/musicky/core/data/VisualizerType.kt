package com.ndriqa.musicky.core.data

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.BlurCircular
import androidx.compose.material.icons.rounded.MonitorHeart
import androidx.compose.material.icons.rounded.ViewTimeline
import androidx.compose.material.icons.rounded.Waves
import androidx.compose.ui.graphics.vector.ImageVector

enum class VisualizerType(
    val title: String,
    val icon: ImageVector
) {
    LineCenter(
        title = "Center Line",
        icon = Icons.Rounded.MonitorHeart
    ),
    LineSmooth(
        title = "Smooth Line",
        icon = Icons.Rounded.Waves
    ),
    Bars(
        title = "Bars",
        icon = Icons.Rounded.ViewTimeline
    ),
    Circular(
        title = "Circular",
        icon = Icons.Rounded.BlurCircular
    ),
}