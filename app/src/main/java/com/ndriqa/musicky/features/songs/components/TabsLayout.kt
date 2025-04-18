package com.ndriqa.musicky.features.songs.components

import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import com.ndriqa.musicky.features.songs.MusicTab
import com.ndriqa.musicky.features.songs.TabIndicatorAnimationSpec
import com.ndriqa.musicky.ui.theme.PaddingDefault

@Composable
internal fun TabsLayout(
    selectedTabIndex: Int,
    onTabChange: (MusicTab) -> Unit,
    isCompact: Boolean,
    modifier: Modifier = Modifier
) {
    val tabs = if (isCompact) listOf(MusicTab.Songs) else MusicTab.entries

    TabRow(
        selectedTabIndex = selectedTabIndex,
        containerColor = Color.Transparent,
        indicator = { tabPositions ->
            val tabPosition = tabPositions[selectedTabIndex]

            val fullWidth by animateDpAsState(
                targetValue = tabPosition.width,
                animationSpec = TabIndicatorAnimationSpec
            )
            val tabOffset by animateDpAsState(
                targetValue = tabPosition.left,
                animationSpec = TabIndicatorAnimationSpec
            )

            val indicatorWidth = fullWidth / 2
            val indicatorStart = tabOffset + (fullWidth - indicatorWidth) / 2

            Box(
                Modifier
                    .wrapContentSize(Alignment.BottomStart)
                    .offset { IntOffset(indicatorStart.roundToPx(), 0) }
                    .width(indicatorWidth)
                    .height(3.dp)
                    .background(
                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                        shape = RoundedCornerShape(1.5.dp)
                    )
            )
        },
        divider = { },
        modifier = modifier
    ) {
        tabs.forEach { tab ->
            val isSelected = selectedTabIndex == tab.ordinal
            Tab(
                selected = isSelected,
                onClick = { onTabChange(tab) },
            ) {
                if (isCompact) {
                    Icon(
                        imageVector = tab.icon,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onPrimaryContainer,
                        modifier = Modifier.padding(PaddingDefault),
                    )
                } else {
                    Text(
                        text = tab.title,
                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                        modifier = Modifier.padding(PaddingDefault),
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                    )
                }
            }
        }
    }
}