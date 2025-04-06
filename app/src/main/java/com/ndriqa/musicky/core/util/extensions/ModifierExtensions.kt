package com.ndriqa.musicky.core.util.extensions

import androidx.compose.foundation.border
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.ndriqa.musicky.ui.theme.SoftRed

fun Modifier.debugBorder(): Modifier {
    return border(width = 1.dp, color = SoftRed)
}