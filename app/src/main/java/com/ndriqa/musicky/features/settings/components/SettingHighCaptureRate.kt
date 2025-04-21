package com.ndriqa.musicky.features.settings.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.ndriqa.musicky.R
import com.ndriqa.musicky.ui.theme.PaddingCompact
import com.ndriqa.musicky.ui.theme.SoftBlue
import com.ndriqa.musicky.ui.theme.SoftRed

@Composable
fun SettingHighCaptureRate(
    highCaptureRate: Boolean,
    onHighCaptureRateToggle: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val contentColor = MaterialTheme.colorScheme.onPrimaryContainer
    val switchShape = RoundedCornerShape(PaddingCompact)

    SettingsItemSection(
        modifier = modifier,
        content = {
            SettingsItemTitle(stringResource(R.string.visualizer_data_rate))
            SettingsItemText(stringResource(R.string.visualizer_data_rate_description))
            SettingsItemText(stringResource(R.string.visualizer_data_rate_warning))
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(switchShape)
                    .border(
                        width = 1.dp,
                        color = contentColor,
                        shape = switchShape
                    )
                    .clickable { onHighCaptureRateToggle() }
            ) {
                val normalColor = if (highCaptureRate) Color.Unspecified else SoftBlue
                val highColor = if (highCaptureRate) SoftRed else Color.Unspecified

                Text(
                    text = stringResource(R.string.normal),
                    color = contentColor,
                    modifier = Modifier
                        .weight(1f)
                        .background(color = normalColor)
                        .padding(PaddingCompact),
                    fontWeight = if (highCaptureRate) FontWeight.Normal else FontWeight.Bold
                )
                Text(
                    text = stringResource(R.string.high),
                    color = contentColor,
                    modifier = Modifier
                        .weight(1f)
                        .background(color = highColor)
                        .padding(PaddingCompact),
                    fontWeight = if (highCaptureRate) FontWeight.Bold else FontWeight.Normal
                )
            }
        }
    )
}