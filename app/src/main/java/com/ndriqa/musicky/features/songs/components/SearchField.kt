package com.ndriqa.musicky.features.songs.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ndriqa.musicky.R
import com.ndriqa.musicky.ui.theme.QuicksandFontFamily

@Composable
internal fun RowScope.SearchField(
    search: String,
    onSearchChange: (String) -> Unit,
    isVisible: Boolean,
    onVisibilityChange: (Boolean) -> Unit,
    focusRequester: FocusRequester,
    onFocusStateChange: (Boolean) -> Unit,
    enabled: Boolean,
    modifier: Modifier = Modifier
) {
    BasicTextField(
        value = search,
        onValueChange = onSearchChange,
        singleLine = true,
        enabled = enabled,
        modifier = modifier
            .then(if (isVisible) Modifier.weight(1f) else Modifier.width(0.dp))
            .focusRequester(focusRequester)
            .onFocusChanged { focusState ->
                onFocusStateChange(focusState.isFocused)
                if (!focusState.isFocused) {
                    onVisibilityChange(false)
                }
            },
        textStyle = LocalTextStyle.current.copy(
            fontFamily = QuicksandFontFamily,
            color = Color.Unspecified,
            fontSize = 14.sp
        ),
        decorationBox = { innerTextField ->
            Box(
                modifier = Modifier
                    .height(44.dp)
                    .background(Color.Transparent, RoundedCornerShape(0.dp))
                    .padding(horizontal = 0.dp),
                contentAlignment = Alignment.CenterStart
            ) {
                if (search.isEmpty()) {
                    Text(
                        text = stringResource(R.string.search),
                        color = Color.Gray
                    )
                }
                innerTextField()
            }
        }
    )
}