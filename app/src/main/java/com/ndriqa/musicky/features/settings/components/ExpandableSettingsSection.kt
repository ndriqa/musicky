package com.ndriqa.musicky.features.settings.components

import androidx.compose.animation.AnimatedContent
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ExpandLess
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.ndriqa.musicky.ui.theme.PaddingMini

@Composable
fun ExpandableSettingsSection(
    compactTitle: String,
    expandedTitle: String,
    compactContent: @Composable (toggleExpand: () -> Unit) -> Unit,
    expandedContent: @Composable (toggleExpand: () -> Unit) -> Unit,
    modifier: Modifier = Modifier,
    isInitiallyExpanded: Boolean = false
) {
    var isExpanded by remember { mutableStateOf(isInitiallyExpanded) }

    fun toggleExpanded() {
        isExpanded = !isExpanded
    }

    SettingsItemSection(
        modifier = modifier.then(
            if (isExpanded) Modifier.clickable { toggleExpanded() }
            else Modifier
        ),
        content = {
            AnimatedContent(targetState = isExpanded, label = "expand") { expanded ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    SettingsItemTitle(title = if (expanded) expandedTitle else compactTitle)

                    if (expanded) {
                        Spacer(modifier = Modifier.weight(1f))
                        Icon(
                            imageVector = Icons.Rounded.ExpandLess,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(PaddingMini))

            AnimatedContent(targetState = isExpanded, label = "expand-content") { expanded ->
                if (expanded) expandedContent(::toggleExpanded)
                else compactContent(::toggleExpanded)
            }
        }
    )
}
