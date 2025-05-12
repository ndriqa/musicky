package com.ndriqa.musicky.features.settings.components

import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Apps
import androidx.compose.material.icons.rounded.MonetizationOn
import androidx.compose.material.icons.rounded.Star
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.ndriqa.musicky.R
import com.ndriqa.musicky.ui.theme.MusickyTheme
import com.ndriqa.musicky.ui.theme.PaddingCompact

@Composable
internal fun SettingNdriqa(
    modifier: Modifier = Modifier,
    onPlayStoreRate: () -> Unit,
    onDonate: () -> Unit,
    onNdriqaApps: () -> Unit,
) {
    SettingsItemSection(
        content = {
            SettingsItemTitle(stringResource(R.string.ndriqa_title))
            SettingsItemText(stringResource(R.string.ndriqa_message))
            SettingsItemText(stringResource(R.string.ndriqa_call_for_help))

            CoolDivider()
            ReachOutItem(
                icon = Icons.Rounded.Star,
                text = stringResource(R.string.rate_musicky_on_play_store),
                onCardClick = onPlayStoreRate
            )

            CoolDivider()
            ReachOutItem(
                icon = Icons.Rounded.MonetizationOn,
                text = stringResource(R.string.donate_on_ko_fi),
                onCardClick = onDonate
            )

            CoolDivider()
            ReachOutItem(
                icon = Icons.Rounded.Apps,
                text = stringResource(R.string.check_out_my_other_apps),
                onCardClick = onNdriqaApps
            )
        }
    )
}

@Composable
private fun ColumnScope.CoolDivider() {
    HorizontalDivider(
        thickness = 0.5.dp,
        color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.3f),
        modifier = Modifier
            .fillMaxWidth()
            .padding(PaddingCompact)
    )
}

@Composable
private fun ColumnScope.ReachOutItem(
    icon: ImageVector,
    text: String,
    onCardClick: () -> Unit
) {
    val miniCardShape = RoundedCornerShape(PaddingCompact)
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(miniCardShape)
            .border(
                width = 1.dp,
                color = MaterialTheme.colorScheme.onPrimaryContainer,
                shape = miniCardShape
            )
            .clickable { onCardClick() }
            .padding(PaddingCompact)
    ) {
        Icon(imageVector = icon, contentDescription = null)
        Spacer(modifier = Modifier.size(PaddingCompact))
        Text(text)
    }
}

@Preview
@Composable
private fun SettingNdriqaPreview() {
    MusickyTheme {
        Column {
            SettingNdriqa(
                onPlayStoreRate = { },
                onDonate = { },
                onNdriqaApps = { },
            )
        }
    }
}