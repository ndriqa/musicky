package com.ndriqa.musicky.features.settings.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import com.ndriqa.musicky.R
import com.ndriqa.musicky.core.data.SortingMode
import com.ndriqa.musicky.ui.theme.MusickyTheme
import com.ndriqa.musicky.ui.theme.PaddingCompact

@Composable
internal fun SettingPreferredSortingMode(
    sortingMode: SortingMode,
    onSortingModeChange: (SortingMode) -> Unit,
    modifier: Modifier = Modifier,
    isInitiallyExpanded: Boolean = false
) {

    ExpandableSettingsSection(
        modifier = modifier,
        compactTitle = stringResource(R.string.sorting_mode),
        expandedTitle = stringResource(R.string.is_selected, stringResource(sortingMode.titleRes)),
        compactContent = { toggleExpand ->
            Button(
                onClick = toggleExpand,
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.onPrimaryContainer,
                    contentColor = MaterialTheme.colorScheme.primaryContainer,
                ),
                elevation = ButtonDefaults.elevatedButtonElevation()
            ) {
                Icon(
                    imageVector = sortingMode.icon,
                    contentDescription = null
                )
                Spacer(modifier = Modifier.size(PaddingCompact))
                Text(stringResource(sortingMode.titleRes))
            }
        },
        expandedContent = { toggleExpand ->
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(PaddingCompact)
            ) {
                SortingMode.entries.forEach { mode ->
                    val isModeSelected = mode == sortingMode
                    val buttonColor =
                        if (isModeSelected) ButtonDefaults.outlinedButtonColors(
                            containerColor = MaterialTheme.colorScheme.onPrimaryContainer,
                            contentColor = MaterialTheme.colorScheme.primaryContainer,
                        ) else ButtonDefaults.outlinedButtonColors()

                    val contentColor =
                        if (isModeSelected) MaterialTheme.colorScheme.primaryContainer
                        else MaterialTheme.colorScheme.onPrimaryContainer

                    OutlinedButton(
                        onClick = { onSortingModeChange(mode) },
                        modifier = Modifier.fillMaxWidth(),
                        colors = buttonColor
                    ) {
                        Text(
                            text = stringResource(mode.titleRes),
                            fontWeight = FontWeight.Bold,
                            color = contentColor
                        )
                        Spacer(modifier = Modifier.weight(1f))
                        Icon(
                            imageVector = mode.icon,
                            contentDescription = null,
                            tint = contentColor
                        )
                    }
                }
            }
        },
        isInitiallyExpanded = isInitiallyExpanded
    )
}

@Preview(showBackground = true, name = "Preferred Sorting Mode - Light")
@Composable
private fun PreviewSettingPreferredSortingModeLight() {
    MusickyTheme(darkTheme = false) {
        SettingPreferredSortingMode(
            sortingMode = SortingMode.Default,
            onSortingModeChange = {}
        )
    }
}

@Preview(showBackground = true, name = "Preferred Sorting Mode - Dark")
@Composable
private fun PreviewSettingPreferredSortingModeDark() {
    MusickyTheme(darkTheme = true) {
        SettingPreferredSortingMode(
            sortingMode = SortingMode.NameDesc,
            onSortingModeChange = {}
        )
    }
}

@Preview(showBackground = true, name = "Preferred Sorting Mode - Expanded")
@Composable
private fun PreviewSettingPreferredSortingModeExpanded() {
    MusickyTheme(darkTheme = false) {
        SettingPreferredSortingMode(
            sortingMode = SortingMode.Default,
            onSortingModeChange = {},
            isInitiallyExpanded = true
        )
    }
}
