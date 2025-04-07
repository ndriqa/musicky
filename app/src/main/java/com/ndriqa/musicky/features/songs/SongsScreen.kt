package com.ndriqa.musicky.features.songs

import android.app.Activity
import android.content.ContentUris
import android.os.Build
import android.provider.MediaStore
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.IntentSenderRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.MoreVert
import androidx.compose.material.icons.rounded.MusicNote
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MenuDefaults
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.VerticalDivider
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.net.toUri
import androidx.hilt.navigation.compose.hiltViewModel
import coil3.compose.AsyncImage
import coil3.request.CachePolicy
import coil3.request.ImageRequest
import coil3.request.crossfade
import coil3.toUri
import com.ndriqa.musicky.R
import com.ndriqa.musicky.core.data.Song
import com.ndriqa.musicky.core.util.extensions.findActivity
import com.ndriqa.musicky.core.util.extensions.toFormattedTime
import com.ndriqa.musicky.features.player.PlayerViewModel
import com.ndriqa.musicky.ui.theme.MusicIconArtworkSizeCompact
import com.ndriqa.musicky.ui.theme.PaddingCompact
import com.ndriqa.musicky.ui.theme.PaddingDefault
import com.ndriqa.musicky.ui.theme.PaddingHalf
import com.ndriqa.musicky.ui.theme.PaddingNano

private const val DELIMITER = " - "

private enum class Tabs {
    Songs, Albums
}

private val TabIndicatorAnimationSpec = spring<Dp>(
    stiffness = Spring.StiffnessMedium,
    dampingRatio = Spring.DampingRatioNoBouncy
)

@Composable
fun SongsScreen(
    modifier: Modifier = Modifier,
    songsViewModel: SongsViewModel = hiltViewModel(),
    playerViewModel: PlayerViewModel = hiltViewModel()
) {
    val songs by songsViewModel.songs.collectAsState()
    val currentPlayState by playerViewModel.playingState.collectAsState()
    val currentPlayingSong by remember { derivedStateOf { currentPlayState.currentSong } }

    val context = LocalContext.current
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.StartIntentSenderForResult()) {
        if (it.resultCode == Activity.RESULT_OK) {
            songsViewModel.startLoadingSongs(context)
        }
    }
    val requestedSongToBeDeleted by songsViewModel.requestScopedDelete.collectAsState(null)

    var selectedTabIndex by rememberSaveable { mutableIntStateOf(Tabs.Songs.ordinal) }

    fun playSong(song: Song) {
        val songIndex = songs.indexOf(song)
        val nextSongs = songs.subList(songIndex, songs.size)
        val prevSongs = songs.subList(0, songIndex)
        val queue = nextSongs + prevSongs
        playerViewModel.apply {
            setQueue(queue)
            play()
        }
    }

    fun deleteSong(song: Song) {
        songsViewModel.tryDeleteSongFile(context, song)
    }

    LaunchedEffect(requestedSongToBeDeleted) {
        requestedSongToBeDeleted?.let { uri ->
            val pendingIntent = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                MediaStore.createDeleteRequest(context.contentResolver, listOf(uri))
            } else null
            pendingIntent?.let { launcher.launch(IntentSenderRequest.Builder(it).build()) }
            songsViewModel.clearDeleteRequest()
        }
    }

    Column {
        Row(
            verticalAlignment = Alignment.CenterVertically
        ) {
            TabsLayout(
                selectedTabIndex = selectedTabIndex,
                onTabChange = { tab -> selectedTabIndex = tab.ordinal },
                modifier = Modifier.weight(1F)
            )

            VerticalDivider(
                thickness = 1.dp,
                modifier = Modifier.height(30.dp)
            )

            Box(
                modifier = Modifier
                    .padding(horizontal = PaddingHalf)
                    .size(44.dp)
                    .clip(RoundedCornerShape(PaddingHalf))
                    .clickable { }
            ) {
                Icon(
                    imageVector = Icons.Rounded.Search,
                    contentDescription = null,
                    modifier = Modifier.align(Alignment.Center)
                )
            }
        }

        LazyColumn(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1F),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            itemsIndexed(
                items = songs,
                key = { _, song -> song.id }
            ) { index, song ->
                SongItem(
                    song = song,
                    isPlaying = currentPlayingSong == song,
                    onSongTap = ::playSong,
                    onAddNextInQueue = {},
                    onDeleteSong = ::deleteSong
                )
            }
        }
    }
}

@Composable
private fun SongItem(
    song: Song,
    isPlaying: Boolean,
    onSongTap: (Song) -> Unit,
    onAddNextInQueue: (Song) -> Unit,
    onDeleteSong: (Song) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val iconTint =
        if (isPlaying) MaterialTheme.colorScheme.primaryContainer
        else MaterialTheme.colorScheme.onPrimaryContainer

    val iconBackground =
        if (isPlaying) MaterialTheme.colorScheme.onPrimaryContainer
        else MaterialTheme.colorScheme.primary

    val songBackground =
        if (isPlaying) MaterialTheme.colorScheme.primary
        else Color.Transparent

    val iconShape = RoundedCornerShape(PaddingCompact)
    val fallbackIcon = rememberVectorPainter(Icons.Rounded.MusicNote)

    Row(
        modifier = modifier
            .fillMaxWidth()
            .clickable { onSongTap(song) }
            .background(color = songBackground)
            .padding(horizontal = PaddingCompact, vertical = PaddingHalf),
        verticalAlignment = Alignment.CenterVertically
    ) {
        var dropDownExpanded by remember { mutableStateOf(false) }

        Box(
            modifier = Modifier
                .size(MusicIconArtworkSizeCompact)
                .clip(shape = iconShape)
                .background(color = iconBackground, shape = iconShape)
        ) {
            val imageRequest = ImageRequest.Builder(context)
                .data(song.artworkUri)
                .crossfade(true)
                .diskCachePolicy(CachePolicy.ENABLED) // enable disk caching
                .memoryCachePolicy(CachePolicy.ENABLED) // enable memory caching
                .build()

            AsyncImage(
                model = imageRequest,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize(),
                fallback = fallbackIcon
            )
        }

        Column(
            modifier = Modifier
                .weight(1F)
                .padding(start = PaddingCompact),
            horizontalAlignment = Alignment.Start,
            verticalArrangement = Arrangement.spacedBy(space = PaddingNano, alignment = Alignment.CenterVertically)
        ) {
            SongTitle(text = song.title,)
            SongDescription(artist = song.artist, timer = song.duration.toFormattedTime())
        }

        Box(
            modifier = Modifier
                .size(MusicIconArtworkSizeCompact)
                .clip(shape = iconShape)
                .clickable(onClick = { dropDownExpanded = true })
        ) {

            Icon(
                imageVector = Icons.Rounded.MoreVert,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onPrimaryContainer,
                modifier = Modifier.align(Alignment.Center)
            )

            DropdownMenu(
                expanded = dropDownExpanded,
                onDismissRequest = { dropDownExpanded = false },
                offset = DpOffset(0.dp, PaddingCompact),
                containerColor = MaterialTheme.colorScheme.primary
            ) {
                DropdownMenuItem(
                    text = { Text(stringResource(R.string.add_next_in_queue)) },
                    onClick = {
                        dropDownExpanded = false
                        onAddNextInQueue(song)
                    }
                )
                DropdownMenuItem(
                    text = { Text(stringResource(R.string.delete_song)) },
                    onClick = {
                        dropDownExpanded = false
                        onDeleteSong(song)
                    }
                )
            }
        }
    }
}

@Composable
private fun SongDescription(
    artist: String,
    modifier: Modifier = Modifier,
    timer: String? = null
) {
    Text(
        modifier = modifier,
        text = "${timer?.let { "$it$DELIMITER" } ?: ""}$artist",
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
        fontSize = 13.sp
    )
}

@Composable
private fun SongTitle(text: String, modifier: Modifier = Modifier) {
    Text(
        modifier = modifier,
        text = text,
        fontWeight = FontWeight.Bold,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis
    )
}

@Composable
private fun TabsLayout(
    selectedTabIndex: Int,
    onTabChange: (Tabs) -> Unit,
    modifier: Modifier = Modifier
) {
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
        Tabs.entries.forEach { tab ->
            val isSelected = selectedTabIndex == tab.ordinal
            Tab(
                selected = isSelected,
                onClick = { onTabChange(tab) },
            ) { Text(
                text = tab.name,
                color = MaterialTheme.colorScheme.onPrimaryContainer,
                modifier = Modifier.padding(PaddingDefault),
                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
            ) }
        }
    }
}