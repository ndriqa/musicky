package com.ndriqa.musicky.features.songs

import android.app.Activity
import android.os.Build
import android.provider.MediaStore
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.IntentSenderRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.VerticalDivider
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
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
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.ndriqa.musicky.core.data.Album
import com.ndriqa.musicky.core.data.Song
import com.ndriqa.musicky.features.player.PlayerViewModel
import com.ndriqa.musicky.features.songs.components.AlbumItem
import com.ndriqa.musicky.features.songs.components.NoInfoUi
import com.ndriqa.musicky.features.songs.components.SearchButton
import com.ndriqa.musicky.features.songs.components.SearchField
import com.ndriqa.musicky.features.songs.components.SongItem
import com.ndriqa.musicky.features.songs.components.TabsLayout
import com.ndriqa.musicky.ui.theme.PaddingCompact

@Composable
fun SongsScreen(
    modifier: Modifier = Modifier,
    songsViewModel: SongsViewModel,
    playerViewModel: PlayerViewModel
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

        playerViewModel.setQueue(queue)
        playerViewModel.play(context, song)
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
                setQueue(album.songs)
                play(context)
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


