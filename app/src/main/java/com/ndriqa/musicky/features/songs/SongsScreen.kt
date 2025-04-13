package com.ndriqa.musicky.features.songs

import android.app.Activity
import android.os.Build
import android.provider.MediaStore
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.IntentSenderRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.StringRes
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Album
import androidx.compose.material.icons.rounded.MoreVert
import androidx.compose.material.icons.rounded.MusicNote
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.Velocity
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import coil3.compose.AsyncImage
import coil3.request.CachePolicy
import coil3.request.ImageRequest
import coil3.request.crossfade
import com.ndriqa.musicky.R
import com.ndriqa.musicky.core.data.Album
import com.ndriqa.musicky.core.data.Song
import com.ndriqa.musicky.core.util.extensions.toFormattedTime
import com.ndriqa.musicky.features.player.PlayerViewModel
import com.ndriqa.musicky.ui.theme.MusicIconArtworkSizeCompact
import com.ndriqa.musicky.ui.theme.PaddingCompact
import com.ndriqa.musicky.ui.theme.PaddingDefault
import com.ndriqa.musicky.ui.theme.PaddingHalf
import com.ndriqa.musicky.ui.theme.PaddingMini
import com.ndriqa.musicky.ui.theme.PaddingNano
import com.ndriqa.musicky.ui.theme.QuicksandFontFamily

private const val DELIMITER = " - "

private enum class MusicTab(
    val title: String,
    val icon: ImageVector
) {
    Songs(
        title = "Songs",
        icon = Icons.Rounded.MusicNote
    ),
    Albums(
        title = "Albums",
        icon = Icons.Rounded.Album
    )
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
    val allSongs by songsViewModel.allSongs.collectAsState()
    val songs by songsViewModel.songs.collectAsState()
    val albums by songsViewModel.albums.collectAsState()

    val currentPlayState by playerViewModel.playingState.collectAsState()
    val currentPlayingSong by remember { derivedStateOf { currentPlayState.currentSong } }

    val context = LocalContext.current
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.StartIntentSenderForResult()) {
        if (it.resultCode == Activity.RESULT_OK) {
            songsViewModel.startLoadingSongs(context)
        }
    }
    val requestedSongToBeDeleted by songsViewModel.requestScopedDelete.collectAsState(null)

    var selectedTabIndex by rememberSaveable { mutableIntStateOf(MusicTab.Songs.ordinal) }

    val search by songsViewModel.query.collectAsState()
    var isSearchVisible by remember { mutableStateOf(false) }
    val focusRequester = remember { FocusRequester() }
    val focusManager = LocalFocusManager.current
    var searchHasFocus by remember { mutableStateOf(false) }

    fun clearSearchFocus() {
        focusManager.clearFocus()
    }

    fun onTabClick(tab: MusicTab) {
        clearSearchFocus()
        selectedTabIndex = tab.ordinal
    }

    fun playSong(song: Song) {
        clearSearchFocus()
        songsViewModel.resetSearch()
        val songIndex = allSongs.indexOf(song)
        val nextSongs = allSongs.subList(songIndex, allSongs.size)
        val prevSongs = allSongs.subList(0, songIndex)
        val queue = nextSongs + prevSongs
        playerViewModel.apply {
            setQueue(queue)
            play()
        }
    }

    fun deleteSong(song: Song) {
        clearSearchFocus()
        songsViewModel.tryDeleteSongFile(context, song)
    }

    fun toggleSearch() {
        onTabClick(MusicTab.Songs)
        isSearchVisible = !isSearchVisible
        if (isSearchVisible) {
            focusRequester.requestFocus()
        } else clearSearchFocus()
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
        fun onAlbumPlay(album: Album) {
            playerViewModel.apply {
                stop()
                setQueue(album.songs)
                play()
            }
        }

        Row(
            verticalAlignment = Alignment.CenterVertically
        ) {
            TabsLayout(
                selectedTabIndex = selectedTabIndex,
                onTabChange = ::onTabClick,
                isCompact = isSearchVisible,
                modifier = Modifier
                    .then(if (isSearchVisible) Modifier.width(80.dp) else Modifier.weight(1F))
            )

            VerticalDivider(
                thickness = 1.dp,
                modifier = Modifier.height(30.dp)
            )

            Row(
                modifier = Modifier
                    .then(if (isSearchVisible) Modifier.weight(1f) else Modifier)
                    .height(44.dp),
            ) {
                if (isSearchVisible) Spacer(modifier = Modifier.width(PaddingCompact))

                SearchField(
                    search = search,
                    onSearchChange = songsViewModel::onSearch,
                    isVisible = isSearchVisible,
                    onVisibilityChange = { visible -> isSearchVisible = visible },
                    focusRequester = focusRequester,
                    onFocusStateChange = { hasFocus -> searchHasFocus = hasFocus },
                    enabled = selectedTabIndex == MusicTab.Songs.ordinal
                )

                SearchButton(
                    onSearchToggle = ::toggleSearch,
                    enabled = selectedTabIndex == MusicTab.Songs.ordinal
                )
            }
        }

        if (songs.isEmpty()) {
            NoInfoUi(MusicTab.entries[selectedTabIndex])
        } else {
            if (selectedTabIndex == MusicTab.Songs.ordinal) {
                val listState = rememberLazyListState()

                LaunchedEffect(listState) {
                    snapshotFlow { listState.firstVisibleItemScrollOffset }
                        .collect { offset -> clearSearchFocus() }
                }

                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1F),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    state = listState
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
            } else {
                LazyVerticalGrid(
                    columns = GridCells.Adaptive(160.dp),
                    horizontalArrangement = Arrangement.spacedBy(PaddingCompact),
                    verticalArrangement = Arrangement.spacedBy(PaddingCompact),
                    contentPadding = PaddingValues(PaddingCompact),
                ) {

                    items(
                        items = albums,
                        key = { album -> album.name },
                    ) { album ->
                        AlbumItem(
                            album = album,
                            isPlaying = false,
                            onAlbumPlay = ::onAlbumPlay,
                            onAlbumToQueue = {  },
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun RowScope.SearchButton(onSearchToggle: () -> Unit, enabled: Boolean) {
    val contentColor = MaterialTheme.colorScheme.onPrimaryContainer
    Box(
        modifier = Modifier
            .padding(horizontal = PaddingHalf)
            .size(44.dp)
            .clip(RoundedCornerShape(PaddingHalf))
            .then(if (enabled) Modifier.clickable { onSearchToggle() } else Modifier)
    ) {
        Icon(
            imageVector = Icons.Rounded.Search,
            contentDescription = null,
            modifier = Modifier.align(Alignment.Center),
            tint = contentColor.copy(alpha = if (enabled) 1f else 0.3f)
        )
    }
}

@Composable
private fun RowScope.SearchField(
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

@Composable
private fun ColumnScope.NoInfoUi(selectedTab: MusicTab) {
    @StringRes val noInfoTextResId = when(selectedTab) {
        MusicTab.Songs -> R.string.where_the_songs_at
        MusicTab.Albums -> R.string.where_the_albums_at
    }
    val contentColor = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = .7f)

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .weight(1F)
            .padding(PaddingCompact),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(PaddingCompact, Alignment.CenterVertically)
    ) {
        Icon(
            imageVector = Icons.Rounded.MusicNote,
            modifier = Modifier.size(MusicIconArtworkSizeCompact),
            contentDescription = null,
            tint = contentColor
        )

        Text(
            text = stringResource(noInfoTextResId),
            modifier = Modifier.fillMaxWidth(),
            textAlign = TextAlign.Center,
            fontSize = 20.sp,
            color = contentColor
        )
    }
}

@Composable
private fun AlbumItem(
    album: Album,
    isPlaying: Boolean,
    onAlbumPlay: (Album) -> Unit,
    onAlbumToQueue: (Album) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val contentBackground =
        if (isPlaying) MaterialTheme.colorScheme.primaryContainer
        else MaterialTheme.colorScheme.onPrimaryContainer
    val contentTint =
        if (isPlaying) MaterialTheme.colorScheme.onPrimaryContainer
        else MaterialTheme.colorScheme.primary

    var isFlipped by rememberSaveable { mutableStateOf(false) }
    val rotationY by animateFloatAsState(
        targetValue = if (isFlipped) 180f else 0f,
        label = "flipAnimation"
    )
    val cameraDistance = 12f * LocalDensity.current.density

    val albumShape = RoundedCornerShape(PaddingCompact)
    val fallbackIcon = rememberVectorPainter(Icons.Rounded.MusicNote)

    Box(
        modifier = modifier
            .fillMaxWidth()
            .aspectRatio(1f)
            .clip(albumShape)
            .clickable(onClick = { isFlipped = !isFlipped })
            .graphicsLayer {
                this.rotationY = rotationY
                this.cameraDistance = cameraDistance
            }
    ) {
        val imageRequest = ImageRequest.Builder(context)
            .data(album.artworkUri)
            .crossfade(true)
            .diskCachePolicy(CachePolicy.ENABLED) // enable disk caching
            .memoryCachePolicy(CachePolicy.ENABLED) // enable memory caching
            .build()

        val textStyle = TextStyle(
            fontFamily = QuicksandFontFamily,
            shadow = Shadow(
                color = contentBackground,
                offset = Offset(2f, 2f),
                blurRadius = 4f
            )
        )

        if (rotationY < 90) {
            Box(modifier = Modifier.fillMaxSize()) {
                AsyncImage(
                    model = imageRequest,
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize(),
                    fallback = fallbackIcon
                )

                Column(
                    modifier = Modifier
                        .matchParentSize()
                        .background(
                            brush = Brush.verticalGradient(
                                colors = listOf(contentBackground, Color.Transparent),
                                startY = Float.POSITIVE_INFINITY,
                                endY = 0f,
                            )
                        )
                        .padding(PaddingCompact),
                    verticalArrangement = Arrangement.spacedBy(PaddingMini, alignment = Alignment.Bottom),
                ) {
                    Text(
                        text = album.name,
                        fontWeight = FontWeight.Bold,
                        color = contentTint,
                        style = textStyle
                    )

                    Text(
                        text = pluralStringResource(R.plurals.num_songs, album.size, album.size),
                        color = contentTint,
                        style = textStyle
                    )
                }
            }
        } else {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .border(
                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                        width = 1.dp,
                        shape = albumShape
                    )
                    .graphicsLayer { this.rotationY = 180f },
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                val nestedScrollConnection = remember {
                    object : NestedScrollConnection {
                        override suspend fun onPreFling(available: Velocity): Velocity {
                            return available
                        }
                    }
                }

                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .nestedScroll(nestedScrollConnection),
                    contentPadding = PaddingValues(PaddingHalf),
                    verticalArrangement = Arrangement.spacedBy(PaddingMini)
                ) {
                    itemsIndexed(
                        items = album.songs,
                        key = { _, song -> song.id }
                    ) { index, song ->
                        val backgroundColor = MaterialTheme.colorScheme.primary.copy(
                            alpha = 0.7f - (index % 2) * 0.4f
                        )

                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(
                                    color = backgroundColor,
                                    shape = RoundedCornerShape(PaddingMini)
                                )
                                .padding(horizontal = PaddingMini, vertical = PaddingNano),
                            verticalArrangement = Arrangement.spacedBy(PaddingNano),
                        ) {
                            Text(
                                text = song.title,
                                fontSize = 11.sp,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                lineHeight = 12.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = song.artist,
                                fontSize = 9.sp,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                lineHeight = 10.sp
                            )
                        }
                    }
                }

                HorizontalDivider(
                    thickness = 1.dp,
                    color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.3f)
                )
                Button(onClick = { onAlbumPlay(album) }) {
                    Icon(
                        imageVector = Icons.Rounded.PlayArrow,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onPrimaryContainer,
                    )
//                    Text(stringResource(R.string.play_album))
                }
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