package com.ndriqa.musicky.features.settings.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ndriqa.musicky.ui.theme.PaddingCompact
import com.ndriqa.musicky.ui.theme.PaddingNano

@Composable
fun SettingAppVersion(
    version: String,
    modifier: Modifier = Modifier
) {
    val dropShape = RoundedCornerShape(topStart = PaddingCompact, topEnd = PaddingCompact)
    val containerColor = MaterialTheme.colorScheme.primaryContainer

    Box(
        modifier = modifier.fillMaxWidth(),
        contentAlignment = Alignment.BottomCenter
    ) {
        Text(
            text = version,
            fontWeight = FontWeight.Bold,
            fontSize = 18.sp,
            color = MaterialTheme.colorScheme.onPrimaryContainer,
            modifier = Modifier
                .background(
                    shape = dropShape,
                    brush = Brush.radialGradient(
                        colors = listOf(containerColor, Color.Transparent),
                        radius = 120.dp.value
                    )
                )
                .padding(vertical = PaddingNano, horizontal = PaddingCompact),
        )
    }
}