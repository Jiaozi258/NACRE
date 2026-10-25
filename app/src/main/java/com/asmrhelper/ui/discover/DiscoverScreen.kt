package com.asmrhelper.ui.discover

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.MusicNote
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
import com.asmrhelper.ui.theme.TextHint
import com.asmrhelper.ui.theme.TextPrimary
import com.asmrhelper.ui.theme.TextSecondary

/** 发现页：搜索 + 四个标签页（推荐/每日/排行榜/歌单）+ 模块显示自定义。 */
@Composable
fun DiscoverScreen(
    modifier: Modifier = Modifier,
    onPlayRequested: () -> Unit = {},
    viewModel: DiscoverViewModel = hiltViewModel()
) {
    val query by viewModel.query.collectAsStateWithLifecycle()
    val searching by viewModel.searching.collectAsStateWithLifecycle()
    val searchResults by viewModel.searchResults.collectAsStateWithLifecycle()
    val artistResults by viewModel.artistResults.collectAsStateWithLifecycle()
    val playlistResults by viewModel.playlistResults.collectAsStateWithLifecycle()
    val searchTab by viewModel.searchTab.collectAsStateWithLifecycle()
    val artistDetail by viewModel.artistDetail.collectAsStateWithLifecycle()
    val artistDetailSongs by viewModel.artistDetailSongs.collectAsStateWithLifecycle()
    val playlistDetail by viewModel.playlistDetail.collectAsStateWithLifecycle()
    val playlistDetailSongs by viewModel.playlistDetailSongs.collectAsStateWithLifecycle()
    val musicSources by viewModel.musicSources.collectAsStateWithLifecycle()
    val activeSourceId by viewModel.activeSourceId.collectAsStateWithLifecycle()
    val recommendations by viewModel.recommendations.collectAsStateWithLifecycle()
    val charts by viewModel.charts.collectAsStateWithLifecycle()
    val recent by viewModel.recent.collectAsStateWithLifecycle()
    val dailyRecommend by viewModel.dailyRecommend.collectAsStateWithLifecycle()
    val radio by viewModel.radio.collectAsStateWithLifecycle()
    val userPlaylists by viewModel.userPlaylists.collectAsStateWithLifecycle()
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

    var selectedTab by remember { mutableIntStateOf(0) }
    var showSettingsDialog by remember { mutableStateOf(false) }
    var showPlatformDialog by remember { mutableStateOf(false) }

    LaunchedEffect(messageRes) {
        messageRes?.let {
            Toast.makeText(context, context.getString(it), Toast.LENGTH_SHORT).show()
            viewModel.consumeMessage()
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

            when {
                query.isNotBlank() -> {
                    val openedArtist = artistDetail
                    val openedPlaylist = playlistDetail
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
                        else -> SearchResults(
                            searching = searching,
                            searchTab = searchTab,
                            onTabChange = viewModel::setSearchTab,
                            songs = searchResults,
                            artists = artistResults,
                            playlists = playlistResults,
                            onPlay = playSongs,
                            onOpenArtist = viewModel::openArtist,
                            onOpenPlaylist = viewModel::openPlaylist
                        )
                    }
                }
                else -> {
                    DiscoverTabRow(selectedTab) { selectedTab = it }
                    when (selectedTab) {
                        0 -> RecommendTab(recommendations, hiddenModules, loading, loadFailed, viewModel::refresh, playSongs)
                        1 -> DailyTab(dailyRecommend, radio, hiddenModules, loading, loadFailed, viewModel::refresh, playSongs)
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
                        else -> PlaylistsTab(
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
                            onTogglePlaylist = viewModel::toggleUserPlaylist,
                            onOpenPlatformSwitch = { showPlatformDialog = true }
                        )
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
    R.string.discover_tab_playlists
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
private fun DiscoverTabRow(selectedIndex: Int, onSelect: (Int) -> Unit) {
    val accent = LocalAccentColor.current
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        discoverTabLabels.forEachIndexed { i, labelRes ->
            val selected = i == selectedIndex
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(14.dp))
                    .background(if (selected) accent.copy(alpha = 0.18f) else Color.Transparent)
                    .clickable { onSelect(i) }
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
    R.string.discover_search_tab_playlists
)

@Composable
private fun SearchResults(
    searching: Boolean,
    searchTab: Int,
    onTabChange: (Int) -> Unit,
    songs: List<RemoteSong>,
    artists: List<RemoteArtist>,
    playlists: List<RemotePlaylist>,
    onPlay: (List<RemoteSong>, Int) -> Unit,
    onOpenArtist: (RemoteArtist) -> Unit,
    onOpenPlaylist: (RemotePlaylist) -> Unit
) {
    Column(Modifier.fillMaxSize()) {
        SearchTabRow(searchTab, onTabChange)
        if (searching) {
            CenteredLoading()
        } else {
            when (searchTab) {
                1 -> SearchArtistList(artists, onOpenArtist)
                2 -> SearchPlaylistList(playlists, onOpenPlaylist)
                else -> SearchSongList(songs, onPlay)
            }
        }
    }
}

@Composable
private fun SearchTabRow(selectedIndex: Int, onSelect: (Int) -> Unit) {
    val accent = LocalAccentColor.current
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        searchTabLabels.forEachIndexed { i, labelRes ->
            val selected = i == selectedIndex
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(14.dp))
                    .background(if (selected) accent.copy(alpha = 0.18f) else Color.Transparent)
                    .clickable { onSelect(i) }
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
private fun SearchArtistList(artists: List<RemoteArtist>, onOpenArtist: (RemoteArtist) -> Unit) {
    if (artists.isEmpty()) {
        EmptyState(stringResource(R.string.discover_no_results))
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
private fun ArtistRow(artist: RemoteArtist, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
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
            .clickable(onClick = onClick)
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

/** 推荐标签：热门推荐。 */
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
    onPlay: (List<RemoteSong>, Int) -> Unit
) {
    val daily = if (DiscoverModule.DAILY in hiddenModules) emptyList() else dailyRecommend
    val radioList = if (DiscoverModule.RADIO in hiddenModules) emptyList() else radio
    val combined = daily + radioList
    if (combined.isEmpty()) {
        // 两个子模块都被用户隐藏 → 空态；否则是加载失败/未登录/暂无数据
        if (DiscoverModule.DAILY in hiddenModules && DiscoverModule.RADIO in hiddenModules) EmptyTab()
        else ModuleEmptyState(loading, loadFailed, onRetry)
        return
    }
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = androidx.compose.foundation.layout.PaddingValues(bottom = 24.dp)
    ) {
        if (daily.isNotEmpty()) {
            item { SectionHeader(stringResource(R.string.discover_daily_recommend)) }
            itemsIndexed(daily, key = { _, it -> "daily:${it.sourceId}:${it.id}" }) { index, song ->
                SongRow(song = song, onPlay = { onPlay(combined, index) })
            }
        }
        if (radioList.isNotEmpty()) {
            item { SectionHeader(stringResource(R.string.discover_radio)) }
            itemsIndexed(radioList, key = { _, it -> "radio:${it.sourceId}:${it.id}" }) { index, song ->
                SongRow(song = song, onPlay = { onPlay(combined, daily.size + index) })
            }
        }
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
    onTogglePlaylist: (RemotePlaylist) -> Unit,
    onOpenPlatformSwitch: () -> Unit
) {
    val playlists = if (DiscoverModule.PLAYLISTS in hiddenModules) emptyList() else userPlaylists
    val recentList = if (DiscoverModule.RECENT in hiddenModules) emptyList() else recent
    Column(Modifier.fillMaxSize()) {
        PlaylistsHeader(onOpenPlatformSwitch)
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

@Composable
private fun PlaylistsHeader(onOpenPlatformSwitch: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 16.dp, end = 8.dp, top = 6.dp, bottom = 2.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            stringResource(R.string.discover_tab_playlists),
            color = TextPrimary,
            fontSize = 16.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.weight(1f)
        )
        TextButton(onClick = onOpenPlatformSwitch) {
            Text(stringResource(R.string.discover_switch_platform), color = LocalAccentColor.current, fontSize = 13.sp)
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
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                when {
                                    isActive -> onDismiss()
                                    isLoggedIn -> { onSelect(config.id); onDismiss() }
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
                        Text(
                            stringResource(config.displayNameRes),
                            color = TextPrimary,
                            fontSize = 14.sp,
                            fontWeight = if (isActive) FontWeight.SemiBold else FontWeight.Normal,
                            modifier = Modifier.weight(1f)
                        )
                        Text(
                            if (isLoggedIn) stringResource(R.string.profile_logged_in) else stringResource(R.string.profile_not_logged_in),
                            color = if (isLoggedIn) LocalAccentColor.current else TextHint,
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
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            name,
            color = TextSecondary,
            fontSize = 13.sp,
            fontWeight = FontWeight.Medium,
            modifier = Modifier.weight(1f)
        )
        Text(
            text = if (expanded) "▾" else "▸",
            color = TextHint,
            fontSize = 13.sp
        )
    }
}

@Composable
private fun PlaylistRow(name: String, coverUrl: String?, expanded: Boolean, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
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
        Text(
            text = if (expanded) "▾" else "▸",
            color = TextHint,
            fontSize = 13.sp
        )
    }
}

@Composable
private fun SongRow(song: RemoteSong, onPlay: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onPlay)
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

@Composable
private fun RecentRow(entry: PlayHistoryEntity, onPlay: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onPlay)
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
