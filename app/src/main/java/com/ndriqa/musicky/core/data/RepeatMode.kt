package com.ndriqa.musicky.core.data

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CodeOff
import androidx.compose.material.icons.rounded.Repeat
import androidx.compose.material.icons.rounded.RepeatOne
import androidx.compose.ui.graphics.vector.ImageVector

enum class RepeatMode(
    val icon: ImageVector
) {
    All(icon = Icons.Rounded.Repeat),
    One(icon = Icons.Rounded.RepeatOne),
    None(icon = Icons.Rounded.CodeOff)
}