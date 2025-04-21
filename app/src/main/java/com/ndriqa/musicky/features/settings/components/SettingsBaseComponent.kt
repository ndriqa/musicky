package com.ndriqa.musicky.features.settings.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ndriqa.musicky.ui.theme.PaddingCompact
import com.ndriqa.musicky.ui.theme.PaddingMini

@Composable
internal fun SettingsItemTitle(
    title: String
) {
    Text(
        text = title,
        fontSize = 18.sp,
        fontWeight = FontWeight.Bold
    )
}

@Composable
internal fun SettingsItemSection(
    content: @Composable ColumnScope.() -> Unit,
    modifier: Modifier = Modifier
) {
    val cardShape = RoundedCornerShape(PaddingCompact)

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(PaddingCompact)
            .shadow(5.dp, cardShape)
            .clip(cardShape)
            .background(
                color = MaterialTheme.colorScheme.primaryContainer,
                shape = cardShape
            )
            .border(
                width = 1.dp,
                color = MaterialTheme.colorScheme.onPrimaryContainer,
                shape = cardShape
            )
            .padding(PaddingCompact),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(PaddingMini)
    ) { content() }
}