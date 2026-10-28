package com.asmrhelper.ui.discover

import android.widget.Toast
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import com.asmrhelper.ui.theme.appleClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import com.asmrhelper.R
import com.asmrhelper.data.local.db.entity.PlayHistoryEntity
import com.asmrhelper.domain.model.music.RemoteAlbum
import com.asmrhelper.domain.model.music.RemoteArtist
import com.asmrhelper.domain.model.music.RemoteChart
import com.asmrhelper.domain.model.music.RemotePlaylist
import com.asmrhelper.domain.model.music.RemoteSong
import com.asmrhelper.domain.music.MusicSource
import com.asmrhelper.domain.music.SourceConfig
import com.asmrhelper.ui.theme.DarkBackground
import com.asmrhelper.ui.theme.DarkSurface
import com.asmrhelper.ui.theme.DarkSurfaceVariant
import com.asmrhelper.ui.theme.LocalAccentColor
import com.asmrhelper.ui.theme.SuccessGreen
import com.asmrhelper.ui.theme.TextHint
import com.asmrhelper.ui.theme.TextPrimary
import com.asmrhelper.ui.theme.TextSecondary

/** 发现页：搜索 + 四个标签页（推荐/每日/排行榜/歌单）+ 模块显示自定义。 */
@Composable
fun DiscoverScreen(
    modifier: Modifier = Modifier,
    onPlayRequested: () -> Unit = {},
    searchRequest: Pair<Long, String>? = null,
    onSearchConsumed: () -> Unit = {},
    viewModel: DiscoverViewModel = hiltViewModel()
) {
    val query by viewModel.query.collectAsStateWithLifecycle()
    val searching by viewModel.searching.collectAsStateWithLifecycle()
    val searchResults by viewModel.searchResults.collectAsStateWithLifecycle()
    val artistResults by viewModel.artistResults.collectAsStateWithLifecycle()
    val playlistResults by viewModel.playlistResults.collectAsStateWithLifecycle()
    val albumResults by viewModel.albumResults.collectAsStateWithLifecycle()
    val searchTab by viewModel.searchTab.collectAsStateWithLifecycle()
    val artistDetail by viewModel.artistDetail.collectAsStateWithLifecycle()
    val artistDetailSongs by viewModel.artistDetailSongs.collectAsStateWithLifecycle()
    val playlistDetail by viewModel.playlistDetail.collectAsStateWithLifecycle()
    val playlistDetailSongs by viewModel.playlistDetailSongs.collectAsStateWithLifecycle()
    val albumDetail by viewModel.albumDetail.collectAsStateWithLifecycle()
    val albumDetailSongs by viewModel.albumDetailSongs.collectAsStateWithLifecycle()
    val podcastResults by viewModel.podcastResults.collectAsStateWithLifecycle()
    val podcastDetail by viewModel.podcastDetail.collectAsStateWithLifecycle()
    val podcastPrograms by viewModel.podcastPrograms.collectAsStateWithLifecycle()
    val musicSources by viewModel.musicSources.collectAsStateWithLifecycle()
    val activeSourceId by viewModel.activeSourceId.collectAsStateWithLifecycle()
    val recommendations by viewModel.recommendations.collectAsStateWithLifecycle()
    val charts by viewModel.charts.collectAsStateWithLifecycle()
    val recent by viewModel.recent.collectAsStateWithLifecycle()
    val dailyRecommend by viewModel.dailyRecommend.collectAsStateWithLifecycle()
    val radio by viewModel.radio.collectAsStateWithLifecycle()
    val userPlaylists by viewModel.userPlaylists.collectAsStateWithLifecycle()
    val recommendPlaylists by viewModel.recommendPlaylists.collectAsStateWithLifecycle()
    val dailyPodcasts by viewModel.dailyPodcasts.collectAsStateWithLifecycle()
    val expandedPlaylistId by viewModel.expandedPlaylistId.collectAsStateWithLifecycle()
    val expandedPlaylistSongs by viewModel.expandedPlaylistSongs.collectAsStateWithLifecycle()
    val expandedChartId by viewModel.expandedChartId.collectAsStateWithLifecycle()
    val expandedChartSongs by viewModel.expandedChartSongs.collectAsStateWithLifecycle()
    val messageRes by viewModel.messageRes.collectAsStateWithLifecycle()
    val fetchingSongId by viewModel.fetchingSongId.collectAsStateWithLifecycle()
    val hiddenModules by viewModel.hiddenModules.collectAsStateWithLifecycle()
    val loading by viewModel.loading.collectAsStateWithLifecycle()
    val loadFailed by viewModel.loadFailed.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val isVideo = isVideoSource(activeSourceId)

    var selectedTab by remember { mutableIntStateOf(0) }
    var showSettingsDialog by remember { mutableStateOf(false) }
    var showPlatformDialog by remember { mutableStateOf(false) }

    LaunchedEffect(messageRes) {
        messageRes?.let {
            Toast.makeText(context, context.getString(it), Toast.LENGTH_SHORT).show()
            viewModel.consumeMessage()
        }
    }

    // 切换平台（音源↔视频源）后回到首个标签，避免停留在已不存在的标签
    LaunchedEffect(activeSourceId) {
        selectedTab = 0
        viewModel.setSearchTab(0)
    }

    // 播放页歌曲信息点歌手/专辑 → 携带关键词跳到发现页搜索
    LaunchedEffect(searchRequest) {
        searchRequest?.let { (_, query) ->
            if (query.isNotBlank()) {
                selectedTab = 0
                viewModel.onQueryChange(query)
                onSearchConsumed()
            }
        }
    }

    // 点击某首在线歌：当前可见列表整段成为播放队列
    val playSongs: (List<RemoteSong>, Int) -> Unit = { songs, index ->
        viewModel.playSongs(songs, index)
        onPlayRequested()
    }
    val playRecentEntries: (List<PlayHistoryEntity>, Int) -> Unit = { entries, index ->
        viewModel.playRecent(entries, index)
        onPlayRequested()
    }
    val playRadio: (List<RemoteSong>) -> Unit = { songs ->
        viewModel.playRadio(songs)
        onPlayRequested()
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(DarkBackground)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
        ) {
            SearchBar(
                query = query,
                onQueryChange = viewModel::onQueryChange,
                searching = searching,
                onOpenSettings = { showSettingsDialog = true }
            )

            val openedArtist = artistDetail
            val openedPlaylist = playlistDetail
            val openedAlbum = albumDetail
            val openedPodcast = podcastDetail
            when {
                openedArtist != null -> ArtistDetail(
                    artist = openedArtist,
                    songs = artistDetailSongs,
                    onBack = viewModel::closeArtist,
                    onPlay = playSongs
                )
                openedPlaylist != null -> PlaylistDetail(
                    playlist = openedPlaylist,
                    songs = playlistDetailSongs,
                    onBack = viewModel::closePlaylist,
                    onPlay = playSongs
                )
                openedAlbum != null -> AlbumDetail(
                    album = openedAlbum,
                    songs = albumDetailSongs,
                    onBack = viewModel::closeAlbum,
                    onPlay = playSongs
                )
                openedPodcast != null -> PodcastDetail(
                    podcast = openedPodcast,
                    songs = podcastPrograms,
                    onBack = viewModel::closePodcast,
                    onPlay = playSongs
                )
                query.isNotBlank() -> SearchResults(
                    searching = searching,
                    searchTab = searchTab,
                    labels = if (isVideo) videoSearchTabLabels else searchTabLabels,
                    onTabChange = viewModel::setSearchTab,
                    songs = searchResults,
                    artists = artistResults,
                    playlists = playlistResults,
                    albums = albumResults,
                    podcasts = podcastResults,
                    isVideo = isVideo,
                    onPlay = playSongs,
                    onOpenArtist = viewModel::openArtist,
                    onOpenPlaylist = viewModel::openPlaylist,
                    onOpenAlbum = viewModel::openAlbum,
                    onOpenPodcast = viewModel::openPodcast
                )
                else -> {
                    if (isVideo) {
                        DiscoverTabRow(
                            selectedIndex = selectedTab,
                            labels = videoDiscoverTabLabels
                        ) { selectedTab = it }
                        when (selectedTab) {
                            0 -> VideoHotTab(
                                hot = recommendations,
                                loading = loading,
                                loadFailed = loadFailed,
                                onRefresh = viewModel::loadRecommendations,
                                onPlay = playSongs
                            )
                            1 -> VideoFavoritesTab(
                                userPlaylists = userPlaylists,
                                expandedPlaylistId = expandedPlaylistId,
                                expandedPlaylistSongs = expandedPlaylistSongs,
                                loading = loading,
                                loadFailed = loadFailed,
                                onRetry = viewModel::refresh,
                                onPlay = playSongs,
                                onTogglePlaylist = viewModel::toggleUserPlaylist
                            )
                            else -> VideoFeedTab(
                                feed = radio,
                                loading = loading,
                                loadFailed = loadFailed,
                                onRetry = viewModel::refresh,
                                onPlay = playSongs
                            )
                        }
                    } else {
                        DiscoverTabRow(
                            selectedIndex = selectedTab,
                            labels = discoverTabLabels
                        ) { selectedTab = it }
                        when (selectedTab) {
                            0 -> RecommendTab(recommendations, hiddenModules, loading, loadFailed, viewModel::refresh, playSongs)
                            1 -> DailyTab(dailyRecommend, radio, hiddenModules, loading, loadFailed, viewModel::refresh, playSongs, playRadio)
                            2 -> ChartsTab(
                                charts = charts,
                                expandedChartId = expandedChartId,
                                expandedChartSongs = expandedChartSongs,
                                hiddenModules = hiddenModules,
                                loading = loading,
                                loadFailed = loadFailed,
                                onRetry = viewModel::refresh,
                                onPlay = playSongs,
                                onToggleChart = viewModel::toggleChart
                            )
                            3 -> PlaylistsTab(
                                userPlaylists = userPlaylists,
                                recent = recent,
                                expandedPlaylistId = expandedPlaylistId,
                                expandedPlaylistSongs = expandedPlaylistSongs,
                                hiddenModules = hiddenModules,
                                loading = loading,
                                loadFailed = loadFailed,
                                onRetry = viewModel::refresh,
                                onPlay = playSongs,
                                onPlayRecent = playRecentEntries,
                                onTogglePlaylist = viewModel::toggleUserPlaylist
                            )
                            else -> ExploreTab(
                                roam = radio,
                                dailyPodcasts = dailyPodcasts,
                                recommendPlaylists = recommendPlaylists,
                                loading = loading,
                                loadFailed = loadFailed,
                                onRetry = viewModel::refresh,
                                onPlay = playSongs,
                                onOpenPlaylist = viewModel::openPlaylist
                            )
                        }
                    }
                }
            }
        }

        if (fetchingSongId != null) {
            Box(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 24.dp)
                    .clip(RoundedCornerShape(20.dp))
                    .background(DarkSurface)
                    .padding(horizontal = 16.dp, vertical = 10.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(16.dp),
                        color = LocalAccentColor.current,
                        strokeWidth = 2.dp
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(
                        text = stringResource(R.string.discover_fetching_url),
                        color = TextPrimary,
                        fontSize = 13.sp
                    )
                }
            }
        }
    }

    if (showSettingsDialog) {
        DiscoverSettingsDialog(
            hiddenModules = hiddenModules,
            onToggle = viewModel::toggleModule,
            onOpenPlatformSwitch = {
                showSettingsDialog = false
                showPlatformDialog = true
            },
            onDismiss = { showSettingsDialog = false }
        )
    }

    if (showPlatformDialog) {
        PlatformSwitchDialog(
            sources = musicSources,
            activeSourceId = activeSourceId,
            sourceFor = viewModel::sourceFor,
            onSelect = viewModel::setActiveSource,
            onDismiss = { showPlatformDialog = false }
        )
    }
}

private val discoverTabLabels = listOf(
    R.string.discover_tab_recommend,
    R.string.discover_tab_daily,
    R.string.discover_tab_charts,
    R.string.discover_tab_playlists,
    R.string.discover_tab_explore
)

/** 视频源（B站/YouTube）发现页标签：热门 + 收藏夹 + 动态，不显示音乐向的推荐/每日/排行榜/歌单。 */
private val videoDiscoverTabLabels = listOf(
    R.string.discover_tab_hot,
    R.string.discover_tab_favorites,
    R.string.discover_tab_feed
)

@Composable
private fun SearchBar(
    query: String,
    onQueryChange: (String) -> Unit,
    searching: Boolean,
    onOpenSettings: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 16.dp, end = 8.dp, top = 10.dp, bottom = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        OutlinedTextField(
            value = query,
            onValueChange = onQueryChange,
            modifier = Modifier.weight(1f),
            placeholder = { Text(stringResource(R.string.discover_search_hint), color = TextHint) },
            leadingIcon = {
                Icon(Icons.Filled.Search, null, tint = TextSecondary)
            },
            trailingIcon = {
                if (query.isNotEmpty()) {
                    IconButton(onClick = { onQueryChange("") }) {
                        Icon(Icons.Filled.Close, stringResource(R.string.common_clear), tint = TextSecondary)
                    }
                } else if (searching) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(18.dp),
                        color = LocalAccentColor.current,
                        strokeWidth = 2.dp
                    )
                }
            },
            singleLine = true,
            shape = RoundedCornerShape(12.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = LocalAccentColor.current,
                unfocusedBorderColor = DarkSurfaceVariant,
                focusedContainerColor = DarkSurface,
                unfocusedContainerColor = DarkSurface,
                cursorColor = LocalAccentColor.current,
                focusedTextColor = TextPrimary,
                unfocusedTextColor = TextPrimary
            )
        )
        IconButton(onClick = onOpenSettings) {
            Icon(Icons.Filled.Settings, stringResource(R.string.discover_settings_title), tint = TextSecondary)
        }
    }
}

@Composable
private fun DiscoverTabRow(
    selectedIndex: Int,
    labels: List<Int>,
    onSelect: (Int) -> Unit
) {
    val accent = LocalAccentColor.current
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState())
            .padding(start = 12.dp, end = 4.dp, top = 4.dp, bottom = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        labels.forEachIndexed { i, labelRes ->
            val selected = i == selectedIndex
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(14.dp))
                    .background(if (selected) accent.copy(alpha = 0.18f) else Color.Transparent)
                    .appleClickable { onSelect(i) }
                    .padding(horizontal = 14.dp, vertical = 8.dp)
            ) {
                Text(
                    stringResource(labelRes),
                    color = if (selected) accent else TextSecondary,
                    fontSize = 14.sp,
                    fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Medium
                )
            }
        }
    }
}

private val searchTabLabels = listOf(
    R.string.discover_search_tab_songs,
    R.string.discover_search_tab_artists,
    R.string.discover_search_tab_playlists,
    R.string.discover_search_tab_albums,
    R.string.discover_search_tab_podcast
)

/** 视频源搜索标签：只有视频 + 博主，不提供歌曲/歌手/歌单。 */
private val videoSearchTabLabels = listOf(
    R.string.discover_search_tab_video,
    R.string.discover_search_tab_uploader
)

@Composable
private fun SearchResults(
    searching: Boolean,
    searchTab: Int,
    labels: List<Int>,
    onTabChange: (Int) -> Unit,
    songs: List<RemoteSong>,
    artists: List<RemoteArtist>,
    playlists: List<RemotePlaylist>,
    albums: List<RemoteAlbum>,
    podcasts: List<RemotePlaylist>,
    isVideo: Boolean,
    onPlay: (List<RemoteSong>, Int) -> Unit,
    onOpenArtist: (RemoteArtist) -> Unit,
    onOpenPlaylist: (RemotePlaylist) -> Unit,
    onOpenAlbum: (RemoteAlbum) -> Unit,
    onOpenPodcast: (RemotePlaylist) -> Unit
) {
    Column(Modifier.fillMaxSize()) {
        SearchTabRow(searchTab, labels, onTabChange)
        if (searching) {
            CenteredLoading()
        } else {
            when (searchTab) {
                1 -> SearchArtistList(
                    artists,
                    onOpenArtist,
                    emptyText = stringResource(if (isVideo) R.string.discover_no_uploader else R.string.discover_no_artist)
                )
                2 -> SearchPlaylistList(playlists, onOpenPlaylist)
                3 -> SearchAlbumList(albums, onOpenAlbum)
                4 -> SearchPodcastList(podcasts, onOpenPodcast)
                else -> SearchSongList(songs, onPlay)
            }
        }
    }
}

@Composable
private fun SearchTabRow(selectedIndex: Int, labels: List<Int>, onSelect: (Int) -> Unit) {
    val accent = LocalAccentColor.current
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState())
            .padding(horizontal = 12.dp, vertical = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        labels.forEachIndexed { i, labelRes ->
            val selected = i == selectedIndex
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(14.dp))
                    .background(if (selected) accent.copy(alpha = 0.18f) else Color.Transparent)
                    .appleClickable { onSelect(i) }
                    .padding(horizontal = 14.dp, vertical = 8.dp)
            ) {
                Text(
                    stringResource(labelRes),
                    color = if (selected) accent else TextSecondary,
                    fontSize = 14.sp,
                    fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Medium
                )
            }
        }
    }
}

@Composable
private fun SearchSongList(songs: List<RemoteSong>, onPlay: (List<RemoteSong>, Int) -> Unit) {
    if (songs.isEmpty()) {
        EmptyState(stringResource(R.string.discover_no_results))
        return
    }
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = androidx.compose.foundation.layout.PaddingValues(bottom = 24.dp)
    ) {
        itemsIndexed(songs, key = { _, it -> "${it.sourceId}:${it.id}" }) { index, song ->
            SongRow(song = song, onPlay = { onPlay(songs, index) })
        }
    }
}

@Composable
private fun SearchArtistList(artists: List<RemoteArtist>, onOpenArtist: (RemoteArtist) -> Unit, emptyText: String) {
    if (artists.isEmpty()) {
        EmptyState(emptyText)
        return
    }
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = androidx.compose.foundation.layout.PaddingValues(bottom = 24.dp)
    ) {
        itemsIndexed(artists, key = { _, it -> "artist:${it.id}" }) { _, artist ->
            ArtistRow(artist = artist, onClick = { onOpenArtist(artist) })
        }
    }
}

@Composable
private fun SearchPlaylistList(playlists: List<RemotePlaylist>, onOpenPlaylist: (RemotePlaylist) -> Unit) {
    if (playlists.isEmpty()) {
        EmptyState(stringResource(R.string.discover_no_results))
        return
    }
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = androidx.compose.foundation.layout.PaddingValues(bottom = 24.dp)
    ) {
        itemsIndexed(playlists, key = { _, it -> "spl:${it.id}" }) { _, playlist ->
            SearchPlaylistRow(name = playlist.name, coverUrl = playlist.coverUrl, onClick = { onOpenPlaylist(playlist) })
        }
    }
}

@Composable
private fun SearchAlbumList(albums: List<RemoteAlbum>, onOpenAlbum: (RemoteAlbum) -> Unit) {
    if (albums.isEmpty()) {
        EmptyState(stringResource(R.string.discover_no_results))
        return
    }
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = androidx.compose.foundation.layout.PaddingValues(bottom = 24.dp)
    ) {
        itemsIndexed(albums, key = { _, it -> "album:${it.id}" }) { _, album ->
            AlbumRow(album = album, onClick = { onOpenAlbum(album) })
        }
    }
}

@Composable
private fun SearchPodcastList(podcasts: List<RemotePlaylist>, onOpenPodcast: (RemotePlaylist) -> Unit) {
    if (podcasts.isEmpty()) {
        EmptyState(stringResource(R.string.discover_no_results))
        return
    }
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = androidx.compose.foundation.layout.PaddingValues(bottom = 24.dp)
    ) {
        itemsIndexed(podcasts, key = { _, it -> "podcast:${it.id}" }) { _, podcast ->
            SearchPlaylistRow(name = podcast.name, coverUrl = podcast.coverUrl, onClick = { onOpenPodcast(podcast) })
        }
    }
}

@Composable
private fun AlbumRow(album: RemoteAlbum, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .appleClickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Cover(coverUrl = album.coverUrl)
        Spacer(Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                album.name,
                color = TextPrimary,
                fontSize = 14.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            if (album.artist.isNotBlank()) {
                Text(
                    album.artist,
                    color = TextSecondary,
                    fontSize = 12.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
        Icon(Icons.Filled.ChevronRight, null, tint = TextHint, modifier = Modifier.size(18.dp))
    }
}

@Composable
private fun ArtistRow(artist: RemoteArtist, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .appleClickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Cover(coverUrl = artist.coverUrl)
        Spacer(Modifier.width(12.dp))
        Text(
            artist.name,
            color = TextPrimary,
            fontSize = 14.sp,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f)
        )
        Icon(Icons.Filled.ChevronRight, null, tint = TextHint, modifier = Modifier.size(18.dp))
    }
}

@Composable
private fun SearchPlaylistRow(name: String, coverUrl: String?, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .appleClickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Cover(coverUrl = coverUrl)
        Spacer(Modifier.width(12.dp))
        Text(
            name,
            color = TextPrimary,
            fontSize = 14.sp,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f)
        )
        Icon(Icons.Filled.ChevronRight, null, tint = TextHint, modifier = Modifier.size(18.dp))
    }
}

@Composable
private fun CenteredLoading() {
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        CircularProgressIndicator(color = LocalAccentColor.current)
    }
}

/** 推荐标签：热门推荐（视频源渲染为横滑视频架，音乐源为纵向列表）。 */
@Composable
private fun RecommendTab(
    recommendations: List<RemoteSong>,
    hiddenModules: Set<String>,
    loading: Boolean,
    loadFailed: Boolean,
    onRetry: () -> Unit,
    onPlay: (List<RemoteSong>, Int) -> Unit
) {
    if (DiscoverModule.RECOMMEND in hiddenModules) {
        EmptyTab()
        return
    }
    if (recommendations.isEmpty()) {
        ModuleEmptyState(loading, loadFailed, onRetry)
        return
    }
    if (isVideoSource(recommendations.first().sourceId)) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = androidx.compose.foundation.layout.PaddingValues(bottom = 24.dp)
        ) {
            item { VideoShelf(recommendations, onPlay) }
        }
        return
    }
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = androidx.compose.foundation.layout.PaddingValues(bottom = 24.dp)
    ) {
        itemsIndexed(recommendations, key = { _, it -> "rec:${it.sourceId}:${it.id}" }) { index, song ->
            SongRow(song = song, onPlay = { onPlay(recommendations, index) })
        }
    }
}

/** 每日标签：每日推荐 + 电台，整段拼接成队列。 */
@Composable
private fun DailyTab(
    dailyRecommend: List<RemoteSong>,
    radio: List<RemoteSong>,
    hiddenModules: Set<String>,
    loading: Boolean,
    loadFailed: Boolean,
    onRetry: () -> Unit,
    onPlay: (List<RemoteSong>, Int) -> Unit,
    onPlayRadio: (List<RemoteSong>) -> Unit
) {
    val daily = if (DiscoverModule.DAILY in hiddenModules) emptyList() else dailyRecommend
    val radioList = if (DiscoverModule.RADIO in hiddenModules) emptyList() else radio
    if (daily.isEmpty() && radioList.isEmpty()) {
        // 两个子模块都被用户隐藏 → 空态；否则是加载失败/未登录/暂无数据
        if (DiscoverModule.DAILY in hiddenModules && DiscoverModule.RADIO in hiddenModules) EmptyTab()
        else ModuleEmptyState(loading, loadFailed, onRetry)
        return
    }
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = androidx.compose.foundation.layout.PaddingValues(bottom = 24.dp)
    ) {
        // 电台：长椭圆按钮，点进去自动进入电台播放（不再在下方展开歌曲）
        if (radioList.isNotEmpty()) {
            item { RadioPlayButton(onClick = { onPlayRadio(radioList) }) }
        }
        // 每日推荐：电台下方的歌曲列表
        if (daily.isNotEmpty()) {
            item { SectionHeader(stringResource(R.string.discover_daily_recommend)) }
            itemsIndexed(daily, key = { _, it -> "daily:${it.sourceId}:${it.id}" }) { index, song ->
                SongRow(song = song, onPlay = { onPlay(daily, index) })
            }
        }
    }
}

/** 电台入口：长椭圆按钮，点击后自动进入电台并从第一首开始播放。 */
@Composable
private fun RadioPlayButton(onClick: () -> Unit) {
    val accent = LocalAccentColor.current
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp)
            .clip(RoundedCornerShape(28.dp))
            .background(accent.copy(alpha = 0.18f))
            .appleClickable(onClick = onClick)
            .padding(vertical = 14.dp),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(Icons.Filled.PlayArrow, stringResource(R.string.discover_radio), tint = accent, modifier = Modifier.size(20.dp))
        Spacer(Modifier.width(6.dp))
        Text(stringResource(R.string.discover_radio), color = accent, fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
    }
}

/** 排行榜标签：榜单可点开/收起。 */
@Composable
private fun ChartsTab(
    charts: List<RemoteChart>,
    expandedChartId: String?,
    expandedChartSongs: List<RemoteSong>,
    hiddenModules: Set<String>,
    loading: Boolean,
    loadFailed: Boolean,
    onRetry: () -> Unit,
    onPlay: (List<RemoteSong>, Int) -> Unit,
    onToggleChart: (RemoteChart) -> Unit
) {
    if (DiscoverModule.CHARTS in hiddenModules) {
        EmptyTab()
        return
    }
    if (charts.isEmpty()) {
        ModuleEmptyState(loading, loadFailed, onRetry)
        return
    }
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = androidx.compose.foundation.layout.PaddingValues(bottom = 24.dp)
    ) {
        charts.forEach { chart ->
            val expanded = expandedChartId == chart.id
            item(key = "chart_header:${chart.id}") {
                ChartHeader(
                    name = chart.name,
                    expanded = expanded,
                    onClick = { onToggleChart(chart) }
                )
            }
            if (expanded) {
                itemsIndexed(expandedChartSongs, key = { _, it -> "chart:${chart.id}:${it.id}" }) { index, song ->
                    SongRow(song = song, onPlay = { onPlay(expandedChartSongs, index) })
                }
            }
        }
    }
}

/** 歌单标签：我的歌单（可折叠）+ 最近播放。 */
@Composable
private fun PlaylistsTab(
    userPlaylists: List<RemotePlaylist>,
    recent: List<PlayHistoryEntity>,
    expandedPlaylistId: String?,
    expandedPlaylistSongs: List<RemoteSong>,
    hiddenModules: Set<String>,
    loading: Boolean,
    loadFailed: Boolean,
    onRetry: () -> Unit,
    onPlay: (List<RemoteSong>, Int) -> Unit,
    onPlayRecent: (List<PlayHistoryEntity>, Int) -> Unit,
    onTogglePlaylist: (RemotePlaylist) -> Unit
) {
    val playlists = if (DiscoverModule.PLAYLISTS in hiddenModules) emptyList() else userPlaylists
    val recentList = if (DiscoverModule.RECENT in hiddenModules) emptyList() else recent
    Column(Modifier.fillMaxSize()) {
        if (playlists.isEmpty() && recentList.isEmpty()) {
            // 两个子模块都被用户隐藏 → 空态；否则是加载失败/未登录/暂无数据
            if (DiscoverModule.PLAYLISTS in hiddenModules && DiscoverModule.RECENT in hiddenModules) EmptyTab()
            else ModuleEmptyState(loading, loadFailed, onRetry)
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = androidx.compose.foundation.layout.PaddingValues(bottom = 24.dp)
            ) {
                if (playlists.isNotEmpty()) {
                    item { SectionHeader(stringResource(R.string.discover_user_playlists)) }
                    playlists.forEach { playlist ->
                        val expanded = expandedPlaylistId == playlist.id
                        item(key = "pl_header:${playlist.id}") {
                            PlaylistRow(
                                name = playlist.name,
                                coverUrl = playlist.coverUrl,
                                expanded = expanded,
                                onClick = { onTogglePlaylist(playlist) }
                            )
                        }
                        if (expanded) {
                            itemsIndexed(expandedPlaylistSongs, key = { _, it -> "pl:${playlist.id}:${it.id}" }) { index, song ->
                                SongRow(song = song, onPlay = { onPlay(expandedPlaylistSongs, index) })
                            }
                        }
                    }
                }
                if (recentList.isNotEmpty()) {
                    item { SectionHeader(stringResource(R.string.discover_recent)) }
                    itemsIndexed(recentList, key = { _, it -> "recent:${it.id}" }) { index, entry ->
                        RecentRow(entry = entry, onPlay = { onPlayRecent(recentList, index) })
                    }
                }
            }
        }
    }
}

/** 探索标签：漫游 + 每日播客 + 歌单推荐，每类最多 5 个。 */
@Composable
private fun ExploreTab(
    roam: List<RemoteSong>,
    dailyPodcasts: List<RemoteSong>,
    recommendPlaylists: List<RemotePlaylist>,
    loading: Boolean,
    loadFailed: Boolean,
    onRetry: () -> Unit,
    onPlay: (List<RemoteSong>, Int) -> Unit,
    onOpenPlaylist: (RemotePlaylist) -> Unit
) {
    val roamList = roam.take(5)
    if (roamList.isEmpty() && dailyPodcasts.isEmpty() && recommendPlaylists.isEmpty()) {
        ModuleEmptyState(loading, loadFailed, onRetry)
        return
    }
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = androidx.compose.foundation.layout.PaddingValues(bottom = 24.dp)
    ) {
        if (roamList.isNotEmpty()) {
            item { SectionHeader(stringResource(R.string.discover_explore_roam)) }
            itemsIndexed(roamList, key = { _, it -> "explore_roam:${it.sourceId}:${it.id}" }) { index, song ->
                SongRow(song = song, onPlay = { onPlay(roam, index) })
            }
        }
        if (dailyPodcasts.isNotEmpty()) {
            item { SectionHeader(stringResource(R.string.discover_explore_daily_podcast)) }
            itemsIndexed(dailyPodcasts, key = { _, it -> "explore_podcast:${it.sourceId}:${it.id}" }) { index, song ->
                SongRow(song = song, onPlay = { onPlay(dailyPodcasts, index) })
            }
        }
        if (recommendPlaylists.isNotEmpty()) {
            item { SectionHeader(stringResource(R.string.discover_explore_playlist_recommend)) }
            recommendPlaylists.forEach { playlist ->
                item(key = "explore_pl:${playlist.id}") {
                    SearchPlaylistRow(name = playlist.name, coverUrl = playlist.coverUrl, onClick = { onOpenPlaylist(playlist) })
                }
            }
        }
    }
}

/** 播客详情：电台节目列表（可播放）。 */
@Composable
private fun PodcastDetail(
    podcast: RemotePlaylist,
    songs: List<RemoteSong>,
    onBack: () -> Unit,
    onPlay: (List<RemoteSong>, Int) -> Unit
) {
    Column(Modifier.fillMaxSize()) {
        DetailHeader(title = podcast.name, coverUrl = podcast.coverUrl, onBack = onBack)
        if (songs.isEmpty()) {
            CenteredLoading()
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = androidx.compose.foundation.layout.PaddingValues(bottom = 24.dp)
            ) {
                itemsIndexed(songs, key = { _, it -> "${it.sourceId}:${it.id}" }) { index, song ->
                    SongRow(song = song, onPlay = { onPlay(songs, index) })
                }
            }
        }
    }
}

/** 视频源「热门」标签：最多 10 条热门视频，右上角可刷新随机重新分配。 */
@Composable
private fun VideoHotTab(
    hot: List<RemoteSong>,
    loading: Boolean,
    loadFailed: Boolean,
    onRefresh: () -> Unit,
    onPlay: (List<RemoteSong>, Int) -> Unit
) {
    if (hot.isEmpty()) {
        ModuleEmptyState(loading, loadFailed, onRefresh)
        return
    }
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = androidx.compose.foundation.layout.PaddingValues(bottom = 24.dp)
    ) {
        item(key = "video_hot_header") {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 16.dp, end = 8.dp, top = 8.dp, bottom = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    stringResource(R.string.discover_tab_hot),
                    color = TextPrimary,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.weight(1f)
                )
                IconButton(onClick = onRefresh) {
                    Icon(Icons.Filled.Refresh, stringResource(R.string.discover_refresh), tint = TextSecondary)
                }
            }
        }
        itemsIndexed(hot, key = { _, it -> "hot:${it.sourceId}:${it.id}" }) { index, song ->
            SongRow(song = song, onPlay = { onPlay(hot, index) })
        }
    }
}

/** 视频源「收藏夹」标签：折叠的收藏夹列表。 */
@Composable
private fun VideoFavoritesTab(
    userPlaylists: List<RemotePlaylist>,
    expandedPlaylistId: String?,
    expandedPlaylistSongs: List<RemoteSong>,
    loading: Boolean,
    loadFailed: Boolean,
    onRetry: () -> Unit,
    onPlay: (List<RemoteSong>, Int) -> Unit,
    onTogglePlaylist: (RemotePlaylist) -> Unit
) {
    if (userPlaylists.isEmpty()) {
        ModuleEmptyState(loading, loadFailed, onRetry)
        return
    }
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = androidx.compose.foundation.layout.PaddingValues(bottom = 24.dp)
    ) {
        userPlaylists.forEach { playlist ->
            val expanded = expandedPlaylistId == playlist.id
            item(key = "vfav_header:${playlist.id}") {
                PlaylistRow(
                    name = playlist.name,
                    coverUrl = playlist.coverUrl,
                    expanded = expanded,
                    onClick = { onTogglePlaylist(playlist) }
                )
            }
            if (expanded) {
                itemsIndexed(expandedPlaylistSongs, key = { _, it -> "vfav:${playlist.id}:${it.id}" }) { index, song ->
                    SongRow(song = song, onPlay = { onPlay(expandedPlaylistSongs, index) })
                }
            }
        }
    }
}

/** 视频源「动态」标签：动态流视频（视频源渲染为视频行）。 */
@Composable
private fun VideoFeedTab(
    feed: List<RemoteSong>,
    loading: Boolean,
    loadFailed: Boolean,
    onRetry: () -> Unit,
    onPlay: (List<RemoteSong>, Int) -> Unit
) {
    if (feed.isEmpty()) {
        ModuleEmptyState(loading, loadFailed, onRetry)
        return
    }
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = androidx.compose.foundation.layout.PaddingValues(bottom = 24.dp)
    ) {
        itemsIndexed(feed, key = { _, it -> "feed:${it.sourceId}:${it.id}" }) { index, song ->
            SongRow(song = song, onPlay = { onPlay(feed, index) })
        }
    }
}

@Composable
private fun PlatformSwitchDialog(
    sources: List<SourceConfig>,
    activeSourceId: String,
    sourceFor: (SourceConfig) -> MusicSource,
    onSelect: (String) -> Unit,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.discover_switch_platform_title), color = TextPrimary) },
        text = {
            Column {
                sources.forEach { config ->
                    val source = sourceFor(config)
                    val isLoggedIn by source.isLoggedIn.collectAsStateWithLifecycle()
                    val isActive = config.id == activeSourceId
                    val canSelect = isLoggedIn || !source.requiresLogin
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .appleClickable {
                                when {
                                    isActive -> onDismiss()
                                    canSelect -> { onSelect(config.id); onDismiss() }
                                    else -> Toast.makeText(
                                        context,
                                        context.getString(R.string.discover_login_required),
                                        Toast.LENGTH_SHORT
                                    ).show()
                                }
                            }
                            .padding(vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        val stateColor = when {
                            isActive -> LocalAccentColor.current
                            isLoggedIn -> SuccessGreen
                            else -> TextHint
                        }
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .clip(CircleShape)
                                .background(if (isActive || isLoggedIn) stateColor else TextHint.copy(alpha = 0.4f))
                        )
                        Spacer(Modifier.width(10.dp))
                        Text(
                            stringResource(config.displayNameRes),
                            color = TextPrimary,
                            fontSize = 14.sp,
                            fontWeight = if (isActive) FontWeight.SemiBold else FontWeight.Normal,
                            modifier = Modifier.weight(1f)
                        )
                        Text(
                            if (isLoggedIn) stringResource(R.string.profile_logged_in)
                            else if (!source.requiresLogin) stringResource(R.string.discover_anonymous)
                            else stringResource(R.string.profile_not_logged_in),
                            color = stateColor,
                            fontSize = 12.sp
                        )
                    }
                }
            }
        },
        confirmButton = {},
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.common_close), color = TextSecondary) }
        },
        containerColor = DarkSurface,
        shape = RoundedCornerShape(16.dp)
    )
}

/** 歌手/歌单详情：上方大封面 + 下方歌曲列表。 */
@Composable
private fun ArtistDetail(
    artist: RemoteArtist,
    songs: List<RemoteSong>,
    onBack: () -> Unit,
    onPlay: (List<RemoteSong>, Int) -> Unit
) {
    Column(Modifier.fillMaxSize()) {
        DetailHeader(title = artist.name, coverUrl = artist.coverUrl, onBack = onBack)
        if (songs.isEmpty()) {
            CenteredLoading()
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = androidx.compose.foundation.layout.PaddingValues(bottom = 24.dp)
            ) {
                itemsIndexed(songs, key = { _, it -> "${it.sourceId}:${it.id}" }) { index, song ->
                    SongRow(song = song, onPlay = { onPlay(songs, index) })
                }
            }
        }
    }
}

@Composable
private fun PlaylistDetail(
    playlist: RemotePlaylist,
    songs: List<RemoteSong>,
    onBack: () -> Unit,
    onPlay: (List<RemoteSong>, Int) -> Unit
) {
    Column(Modifier.fillMaxSize()) {
        DetailHeader(title = playlist.name, coverUrl = playlist.coverUrl, onBack = onBack)
        if (songs.isEmpty()) {
            CenteredLoading()
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = androidx.compose.foundation.layout.PaddingValues(bottom = 24.dp)
            ) {
                itemsIndexed(songs, key = { _, it -> "${it.sourceId}:${it.id}" }) { index, song ->
                    SongRow(song = song, onPlay = { onPlay(songs, index) })
                }
            }
        }
    }
}

@Composable
private fun AlbumDetail(
    album: RemoteAlbum,
    songs: List<RemoteSong>,
    onBack: () -> Unit,
    onPlay: (List<RemoteSong>, Int) -> Unit
) {
    Column(Modifier.fillMaxSize()) {
        DetailHeader(title = album.name, coverUrl = album.coverUrl, onBack = onBack)
        if (album.artist.isNotBlank()) {
            Text(
                album.artist,
                color = TextSecondary,
                fontSize = 14.sp,
                modifier = Modifier.padding(horizontal = 16.dp).padding(bottom = 8.dp)
            )
        }
        if (songs.isEmpty()) {
            CenteredLoading()
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = androidx.compose.foundation.layout.PaddingValues(bottom = 24.dp)
            ) {
                itemsIndexed(songs, key = { _, it -> "${it.sourceId}:${it.id}" }) { index, song ->
                    SongRow(song = song, onPlay = { onPlay(songs, index) })
                }
            }
        }
    }
}

@Composable
private fun DetailHeader(title: String, coverUrl: String?, onBack: () -> Unit) {
    Column(Modifier.fillMaxWidth()) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(220.dp)
        ) {
            BigCover(coverUrl)
            Box(
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .padding(8.dp)
                    .size(36.dp)
                    .clip(CircleShape)
                    .background(Color.Black.copy(alpha = 0.45f)),
                contentAlignment = Alignment.Center
            ) {
                IconButton(onClick = onBack, modifier = Modifier.size(36.dp)) {
                    Icon(Icons.Filled.ArrowBack, stringResource(R.string.common_back), tint = Color.White, modifier = Modifier.size(20.dp))
                }
            }
        }
        Text(
            title,
            color = TextPrimary,
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)
        )
    }
}

@Composable
private fun BigCover(coverUrl: String?) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(DarkSurface),
        contentAlignment = Alignment.Center
    ) {
        if (coverUrl.isNullOrBlank()) {
            Icon(Icons.Filled.MusicNote, null, tint = TextSecondary, modifier = Modifier.size(64.dp))
        } else {
            AsyncImage(
                model = coverUrl,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )
        }
    }
}

@Composable
private fun DiscoverSettingsDialog(
    hiddenModules: Set<String>,
    onToggle: (String) -> Unit,
    onOpenPlatformSwitch: () -> Unit,
    onDismiss: () -> Unit
) {
    val modules = listOf(
        DiscoverModule.RECOMMEND to R.string.discover_recommendations,
        DiscoverModule.DAILY to R.string.discover_daily_recommend,
        DiscoverModule.RADIO to R.string.discover_radio,
        DiscoverModule.CHARTS to R.string.discover_charts,
        DiscoverModule.PLAYLISTS to R.string.discover_user_playlists,
        DiscoverModule.RECENT to R.string.discover_recent
    )
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.discover_settings_title), color = TextPrimary) },
        text = {
            Column(Modifier.verticalScroll(rememberScrollState())) {
                // 「换平台」从标签栏迁入设置：点击关闭本对话框并打开平台选择。
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .appleClickable(onClick = onOpenPlatformSwitch)
                        .padding(vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        stringResource(R.string.discover_switch_platform),
                        color = TextPrimary,
                        fontSize = 14.sp,
                        modifier = Modifier.weight(1f)
                    )
                    Icon(Icons.Filled.ChevronRight, null, tint = TextHint, modifier = Modifier.size(18.dp))
                }
                modules.forEach { (key, labelRes) ->
                    val visible = key !in hiddenModules
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(stringResource(labelRes), color = TextPrimary, fontSize = 14.sp, modifier = Modifier.weight(1f))
                        Switch(
                            checked = visible,
                            onCheckedChange = { onToggle(key) },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = LocalAccentColor.current,
                                checkedTrackColor = LocalAccentColor.current.copy(alpha = 0.4f)
                            )
                        )
                    }
                }
            }
        },
        confirmButton = {},
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.common_close), color = TextSecondary) }
        },
        containerColor = DarkSurface,
        shape = RoundedCornerShape(16.dp)
    )
}

@Composable
private fun SectionHeader(title: String) {
    Text(
        title,
        color = TextPrimary,
        fontSize = 16.sp,
        fontWeight = FontWeight.Bold,
        modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp)
    )
}

@Composable
private fun EmptyTab() {
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Text(stringResource(R.string.discover_empty_tab), color = TextHint, fontSize = 14.sp)
    }
}

/** 模块空态：加载中显示进度，加载失败显示重试按钮，否则显示「暂无内容」。
 *  用于区分「用户主动隐藏」「断网失败」「确实无数据」三种情况，避免模块无端"消失"。 */
@Composable
private fun ModuleEmptyState(loading: Boolean, loadFailed: Boolean, onRetry: () -> Unit) {
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            when {
                loading -> {
                    CircularProgressIndicator(color = LocalAccentColor.current)
                    Spacer(Modifier.height(10.dp))
                    Text(stringResource(R.string.discover_loading), color = TextHint, fontSize = 14.sp)
                }
                loadFailed -> {
                    Text(stringResource(R.string.discover_load_failed), color = TextSecondary, fontSize = 14.sp)
                    Spacer(Modifier.height(10.dp))
                    TextButton(onClick = onRetry) {
                        Text(stringResource(R.string.discover_retry), color = LocalAccentColor.current)
                    }
                }
                else -> Text(stringResource(R.string.discover_empty_tab), color = TextHint, fontSize = 14.sp)
            }
        }
    }
}

@Composable
private fun ChartHeader(name: String, expanded: Boolean, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .appleClickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (expanded) {
            Box(
                modifier = Modifier
                    .width(3.dp)
                    .height(14.dp)
                    .clip(RoundedCornerShape(2.dp))
                    .background(LocalAccentColor.current)
            )
            Spacer(Modifier.width(8.dp))
        }
        Text(
            name,
            color = if (expanded) TextPrimary else TextSecondary,
            fontSize = 13.sp,
            fontWeight = FontWeight.Medium,
            modifier = Modifier.weight(1f)
        )
        val rotation by animateFloatAsState(if (expanded) 180f else 0f, label = "chartChevron")
        Icon(
            Icons.Filled.ExpandMore,
            contentDescription = null,
            tint = if (expanded) LocalAccentColor.current else TextHint,
            modifier = Modifier
                .size(18.dp)
                .rotate(rotation)
        )
    }
}

@Composable
private fun PlaylistRow(name: String, coverUrl: String?, expanded: Boolean, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .appleClickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Cover(coverUrl = coverUrl)
        Spacer(Modifier.width(12.dp))
        Text(
            name,
            color = TextPrimary,
            fontSize = 14.sp,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f)
        )
        val rotation by animateFloatAsState(if (expanded) 180f else 0f, label = "playlistChevron")
        Icon(
            Icons.Filled.ExpandMore,
            contentDescription = null,
            tint = if (expanded) LocalAccentColor.current else TextHint,
            modifier = Modifier
                .size(18.dp)
                .rotate(rotation)
        )
    }
}

/** 在线歌曲行：视频源渲染为视频行（封面+时长角标+上传者+发布时间），否则音乐行。 */
@Composable
private fun SongRow(song: RemoteSong, onPlay: () -> Unit) {
    if (isVideoSource(song.sourceId)) VideoRow(song, onPlay) else MusicSongRow(song, onPlay)
}

@Composable
private fun MusicSongRow(song: RemoteSong, onPlay: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .appleClickable(onClick = onPlay)
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Cover(coverUrl = song.coverUrl)
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(
                song.title,
                color = TextPrimary,
                fontSize = 14.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            if (song.artist.isNotBlank()) {
                Text(
                    song.artist,
                    color = TextSecondary,
                    fontSize = 12.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
        if (song.durationMs > 0) {
            Text(
                formatDuration(song.durationMs),
                color = TextHint,
                fontSize = 12.sp
            )
        }
    }
}

/** 视频行：封面（16:9 + 时长角标）+ 名称/上传者/发布时间。 */
@Composable
private fun VideoRow(song: RemoteSong, onPlay: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .appleClickable(onClick = onPlay)
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .width(112.dp)
                .height(63.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(DarkSurface),
            contentAlignment = Alignment.Center
        ) {
            if (song.coverUrl.isNullOrBlank()) {
                Icon(Icons.Filled.MusicNote, null, tint = TextSecondary, modifier = Modifier.size(24.dp))
            } else {
                AsyncImage(
                    model = song.coverUrl,
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
            }
            if (song.durationMs > 0) {
                Text(
                    formatDuration(song.durationMs),
                    color = Color.White,
                    fontSize = 10.sp,
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(4.dp)
                        .clip(RoundedCornerShape(4.dp))
                        .background(Color.Black.copy(alpha = 0.7f))
                        .padding(horizontal = 4.dp, vertical = 1.dp)
                )
            }
        }
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(
                song.title,
                color = TextPrimary,
                fontSize = 14.sp,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
            val publish = formatPublishTime(song.publishTime)
            Text(
                buildString {
                    append(song.artist)
                    if (publish.isNotBlank()) append(" · ").append(publish)
                }.ifBlank { stringResource(R.string.discover_unknown_uploader) },
                color = TextSecondary,
                fontSize = 12.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

/** 横滑视频架（推荐模块，视频源）。 */
@Composable
private fun VideoShelf(songs: List<RemoteSong>, onPlay: (List<RemoteSong>, Int) -> Unit) {
    LazyRow(
        modifier = Modifier.fillMaxWidth(),
        contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 16.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        itemsIndexed(songs, key = { _, it -> "shelf:${it.sourceId}:${it.id}" }) { index, song ->
            VideoCard(song = song, onClick = { onPlay(songs, index) })
        }
    }
}

@Composable
private fun VideoCard(song: RemoteSong, onClick: () -> Unit) {
    Column(
        modifier = Modifier
            .width(150.dp)
            .appleClickable(onClick = onClick)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(84.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(DarkSurface),
            contentAlignment = Alignment.Center
        ) {
            if (song.coverUrl.isNullOrBlank()) {
                Icon(Icons.Filled.MusicNote, null, tint = TextSecondary, modifier = Modifier.size(24.dp))
            } else {
                AsyncImage(
                    model = song.coverUrl,
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
            }
            if (song.durationMs > 0) {
                Text(
                    formatDuration(song.durationMs),
                    color = Color.White,
                    fontSize = 10.sp,
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(4.dp)
                        .clip(RoundedCornerShape(4.dp))
                        .background(Color.Black.copy(alpha = 0.7f))
                        .padding(horizontal = 4.dp, vertical = 1.dp)
                )
            }
        }
        Spacer(Modifier.height(6.dp))
        Text(
            song.title,
            color = TextPrimary,
            fontSize = 12.sp,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis
        )
        if (song.artist.isNotBlank()) {
            Text(
                song.artist,
                color = TextSecondary,
                fontSize = 11.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
private fun RecentRow(entry: PlayHistoryEntity, onPlay: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .appleClickable(onClick = onPlay)
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(48.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(DarkSurface),
            contentAlignment = Alignment.Center
        ) {
            Icon(Icons.Filled.MusicNote, null, tint = TextSecondary, modifier = Modifier.size(22.dp))
        }
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(
                entry.audioTitle,
                color = TextPrimary,
                fontSize = 14.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            if (entry.audioArtist.isNotBlank()) {
                Text(
                    entry.audioArtist,
                    color = TextSecondary,
                    fontSize = 12.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
        if (entry.durationMs > 0) {
            Text(formatDuration(entry.durationMs), color = TextHint, fontSize = 12.sp)
        }
    }
}

@Composable
private fun Cover(coverUrl: String?) {
    Box(
        modifier = Modifier
            .size(48.dp)
            .clip(RoundedCornerShape(10.dp))
            .background(DarkSurface),
        contentAlignment = Alignment.Center
    ) {
        if (coverUrl.isNullOrBlank()) {
            Icon(Icons.Filled.MusicNote, null, tint = TextSecondary, modifier = Modifier.size(22.dp))
        } else {
            AsyncImage(
                model = coverUrl,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )
        }
    }
}

@Composable
private fun EmptyState(text: String) {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(
                Icons.Filled.Search,
                null,
                tint = TextHint,
                modifier = Modifier.size(40.dp)
            )
            Spacer(Modifier.height(10.dp))
            Text(text, color = TextHint, fontSize = 14.sp)
        }
    }
}

private fun formatDuration(ms: Long): String {
    val totalSec = ms / 1000
    val min = totalSec / 60
    val sec = totalSec % 60
    return "%d:%02d".format(min, sec)
}

private fun isVideoSource(sourceId: String): Boolean = sourceId == "bilibili" || sourceId == "youtube"

/** 发布时间（毫秒时间戳 → "yyyy-MM-dd"；拿不到返回空串）。 */
private fun formatPublishTime(ms: Long): String {
    if (ms <= 0) return ""
    val c = java.util.Calendar.getInstance().apply { timeInMillis = ms }
    return "%04d-%02d-%02d".format(
        c.get(java.util.Calendar.YEAR),
        c.get(java.util.Calendar.MONTH) + 1,
        c.get(java.util.Calendar.DAY_OF_MONTH)
    )
}
