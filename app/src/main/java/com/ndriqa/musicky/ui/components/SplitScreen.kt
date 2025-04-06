package com.ndriqa.musicky.ui.components

import android.content.res.Configuration
import androidx.compose.animation.AnimatedContent
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.unit.Dp
import com.ndriqa.musicky.ui.theme.PaddingDefault

@Composable
fun SplitScreen(
    primaryContent: @Composable BoxScope.() -> Unit,
    secondaryContent: @Composable BoxScope.() -> Unit,
    primaryContentRatio: Float = 1F,
    secondaryContentRatio: Float = 1F,
    primaryContentPadding: Dp = PaddingDefault,
    secondaryContentPadding: Dp = PaddingDefault,
    modifier: Modifier = Modifier
) {
    val configuration = LocalConfiguration.current
    val isLandscape = configuration.orientation == Configuration.ORIENTATION_LANDSCAPE

    AnimatedContent(isLandscape, label = "splitScreen") { landscape ->
        val containerModifier = modifier.fillMaxSize()
        if (landscape) {
            Row(modifier = containerModifier) {
                Box(
                    modifier = Modifier
                        .weight(primaryContentRatio)
                        .fillMaxHeight()
                        .padding(primaryContentPadding)
                ) { primaryContent() }

                Box(
                    modifier = Modifier
                        .weight(secondaryContentRatio)
                        .fillMaxHeight()
                        .padding(secondaryContentPadding)
                ) { secondaryContent() }
            }
        } else {
            Column(modifier = containerModifier) {
                Box(
                    modifier = Modifier
                        .weight(primaryContentRatio)
                        .fillMaxWidth()
                        .padding(primaryContentPadding)
                ) { primaryContent() }

                Box(
                    modifier = Modifier
                        .weight(secondaryContentRatio)
                        .fillMaxWidth()
                        .padding(secondaryContentPadding)
                ) { secondaryContent() }
            }
        }
    }
}
