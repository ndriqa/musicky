package com.ndriqa.musicky.core.data

import androidx.annotation.StringRes
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.Sort
import androidx.compose.material.icons.rounded.Event
import androidx.compose.material.icons.rounded.EventRepeat
import androidx.compose.material.icons.rounded.SortByAlpha
import androidx.compose.ui.graphics.vector.ImageVector
import com.ndriqa.musicky.R

enum class SortingMode(
    @StringRes val titleRes: Int,
    val icon: ImageVector,
    val mode: String
) {
    Default(
        titleRes = R.string.sort_default,
        icon = Icons.AutoMirrored.Rounded.Sort,
        mode = "default"
    ),
    DateDesc(
        titleRes = R.string.sort_date_newest,
        icon = Icons.Rounded.Event,
        mode = "date_desc"
    ),
    DateAsc(
        titleRes = R.string.sort_date_oldest,
        icon = Icons.Rounded.EventRepeat,
        mode = "date_asc"
    ),
    NameAsc(
        titleRes = R.string.sort_name_az,
        icon = Icons.Rounded.SortByAlpha,
        mode = "name_asc"
    ),
    NameDesc(
        titleRes = R.string.sort_name_za,
        icon = Icons.Rounded.SortByAlpha,
        mode = "name_desc"
    );
}
