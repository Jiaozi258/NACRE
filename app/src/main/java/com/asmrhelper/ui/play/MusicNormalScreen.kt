package com.asmrhelper.ui.play

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.pager.VerticalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.ContentCut
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.HighQuality
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Notes
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.PlaylistPlay
import androidx.compose.material.icons.filled.QueueMusic
import androidx.compose.material.icons.filled.RateReview
import androidx.compose.material.icons.filled.Repeat
import androidx.compose.material.icons.filled.RepeatOne
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Shuffle
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.SkipPrevious
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.Subtitles
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import com.asmrhelper.R
import com.asmrhelper.data.local.db.entity.PlayStatEntity
import com.asmrhelper.data.music.isRemoteSong
import com.asmrhelper.domain.model.LoopMode
import com.asmrhelper.domain.model.PlaybackBackgroundMode
import com.asmrhelper.domain.model.PlaybackMode
import com.asmrhelper.domain.model.PlaybackQuality
import com.asmrhelper.player.HeadphonePreset
import com.asmrhelper.player.HeadphonePresets
import com.asmrhelper.player.V3ReverbPreset
import com.asmrhelper.ui.components.FrostedAlertDialog
import com.asmrhelper.ui.components.ModeToggleButton
import com.asmrhelper.ui.play.folia.AudioReactiveBackground
import com.asmrhelper.ui.play.folia.rememberCoverPalette
import com.asmrhelper.ui.playlist.PlaylistViewModel
import com.asmrhelper.ui.theme.DarkSurface
import com.asmrhelper.ui.theme.DarkSurfaceVariant
import com.asmrhelper.ui.theme.FrostedSurface
import com.asmrhelper.ui.theme.LocalAccentColor
import com.asmrhelper.ui.theme.Radius
import com.asmrhelper.ui.theme.TextHint
import com.asmrhelper.ui.theme.TextPrimary
import com.asmrhelper.ui.theme.TextSecondary
import com.asmrhelper.util.LyricCaptionMode
import com.asmrhelper.util.LyricMode
import com.asmrhelper.util.ShareHelper
import com.asmrhelper.util.WordLyricLine
import com.asmrhelper.util.captionFor
import com.asmrhelper.util.currentLineIndex
import kotlinx.coroutines.launch

/**
 * MUSIC 正常模式播放页。
 *
 * 以 [VerticalPager] 三层纵向滑动组织：
 * - 第 0 页（上滑返回 / 下滑进入）：全屏可拖动滚动歌词；
 * - 第 1 页（默认）：中上封面 + 中下滚动歌词（自动跟随，不可拖动）+ 控制组件；
 * - 第 2 页（上滑进入）：音效器面板（耳机适配 + 混响）+ 切回沉浸 + 歌曲信息 + 歌曲笔记。
 * 左上角保留 ASMR↔MUSIC 模式切换（三页均可见）。
 */
@Composable
fun MusicNormalScreen(
    onToggleMode: () -> Unit = {},
    onToggleMusicStyle: () -> Unit = {},
    onNavigateToBackground: () -> Unit = {},
    onSearch: (String) -> Unit = {}
) {
    val viewModel: PlayViewModel = hiltViewModel()
    val playlistViewModel: PlaylistViewModel = hiltViewModel()
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val lyrics by viewModel.lyrics.collectAsStateWithLifecycle()
    val translatedLyrics by viewModel.translatedLyrics.collectAsStateWithLifecycle()
    val romanizedLyrics by viewModel.romanizedLyrics.collectAsStateWithLifecycle()
    val lyricsFontSize by viewModel.lyricsFontSize.collectAsStateWithLifecycle()
    val lyricsLineSpacing by viewModel.lyricsLineSpacing.collectAsStateWithLifecycle()
    val lyricsAlignment by viewModel.lyricsAlignment.collectAsStateWithLifecycle()
    val lyricOffsetMs by viewModel.lyricOffsetMs.collectAsStateWithLifecycle()
    val currentFavorite by viewModel.currentFavorite.collectAsStateWithLifecycle()
    val eqCurrentPreset by viewModel.eqCurrentPreset.collectAsStateWithLifecycle()
    val currentV3Reverb by viewModel.currentV3Reverb.collectAsStateWithLifecycle()
    val songNotes by viewModel.songNotes.collectAsStateWithLifecycle()
    val currentPlayStat by viewModel.currentPlayStat.collectAsStateWithLifecycle()
    val lyricsMode by viewModel.lyricsMode.collectAsStateWithLifecycle()
    val lyricCaptionMode by viewModel.lyricCaptionMode.collectAsStateWithLifecycle()
    val captionLines = when (lyricCaptionMode) {
        LyricCaptionMode.TRANSLATION -> translatedLyrics
        LyricCaptionMode.ROMANIZATION -> romanizedLyrics
        LyricCaptionMode.OFF -> emptyList()
    }
    val rawLyricsText by viewModel.rawLyricsText.collectAsStateWithLifecycle()
    val backgroundMode by viewModel.backgroundMode.collectAsStateWithLifecycle()
    val backgroundContrastMode by viewModel.backgroundContrastMode.collectAsStateWithLifecycle()
    val blurBackgroundImage by viewModel.blurBackgroundImage.collectAsStateWithLifecycle()
    val waveform by viewModel.waveformBytes.collectAsStateWithLifecycle()
    val fft by viewModel.fftMagnitudes.collectAsStateWithLifecycle()
    val playbackQuality by viewModel.playbackQuality.collectAsStateWithLifecycle()

    val currentAudio = state.playerState.currentAudio
    val context = LocalContext.current
    val coverUrl = currentAudio?.coverUrl
    val coverFilePath = currentAudio?.takeIf { !it.isRemoteSong() }?.filePath
    val progressMs = state.playerState.progressMs
    val durationMs = state.playerState.durationMs
    val lyricProgress = (progressMs + lyricOffsetMs).coerceAtLeast(0L)
    val title = state.displayTitle.ifEmpty { stringResource(R.string.play_not_playing) }

    // 歌曲笔记键：优先远程 sourceSongId，其次本地 id
    val songKey = currentAudio?.let {
        it.sourceSongId?.takeIf { s -> s.isNotBlank() } ?: it.id.toString()
    }

    val pagerState = rememberPagerState(initialPage = 1) { 3 }
    val scope = rememberCoroutineScope()
    val backToDefault: () -> Unit = { scope.launch { pagerState.animateScrollToPage(1) } }

    var showMenuDialog by remember { mutableStateOf(false) }
    var showQueueDialog by remember { mutableStateOf(false) }
    var showTimerDialog by remember { mutableStateOf(false) }
    var showSongInfoDialog by remember { mutableStateOf(false) }
    var showLyricOffsetDialog by remember { mutableStateOf(false) }
    var showLyricCaptionDialog by remember { mutableStateOf(false) }
    var showBackgroundDialog by remember { mutableStateOf(false) }
    var showQualityDialog by remember { mutableStateOf(false) }
    var showLyricsEditDialog by remember { mutableStateOf(false) }
    var showSongNoteDialog by remember { mutableStateOf(false) }
    var showPlaylistDialog by remember { mutableStateOf(false) }
    var showToolboxDialog by remember { mutableStateOf(false) }
    var showSliceDialog by remember { mutableStateOf(false) }
    var showEffectsDialog by remember { mutableStateOf(false) }
    var showBinauralDialog by remember { mutableStateOf(false) }
    var showNoiseDialog by remember { mutableStateOf(false) }
    var showAmbientDialog by remember { mutableStateOf(false) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(DarkSurface)
    ) {
        // 磨砂封面背景提升到页面容器层，让上滑/下滑面板（歌词页、音效页）也共享磨砂背景，
        // 而非露出纯色底。背景跟随「背景」菜单所选模式（含自定义图片）。
        AudioReactiveBackground(
            coverUrl = coverUrl,
            waveform = waveform,
            fft = fft,
            mode = backgroundMode,
            contrastMode = backgroundContrastMode,
            coverFilePath = coverFilePath,
            customImagePath = state.backgroundImagePath,
            blurCustomImage = blurBackgroundImage,
            modifier = Modifier.fillMaxSize()
        )
        VerticalPager(state = pagerState, modifier = Modifier.fillMaxSize()) { page ->
            when (page) {
                0 -> FullScreenLyrics(
                    lines = lyrics,
                    captionLines = captionLines,
                    progressMs = lyricProgress,
                    fontSize = lyricsFontSize,
                    lineSpacing = lyricsLineSpacing,
                    alignment = lyricsAlignment,
                    isPlaying = state.playerState.isPlaying,
                    progress = if (durationMs > 0) progressMs.toFloat() / durationMs else 0f,
                    durationMs = durationMs,
                    isFavorite = currentFavorite,
                    loopMode = state.playerState.loopMode,
                    onPlayPause = { viewModel.togglePlayPause() },
                    onPrevious = { viewModel.previous() },
                    onNext = { viewModel.next() },
                    onSeek = { viewModel.seekTo(it) },
                    onToggleFavorite = { viewModel.toggleFavorite() },
                    onCycleLoopMode = { viewModel.cycleLoopMode() },
                    onMenu = { showMenuDialog = true },
                    onQueue = { showQueueDialog = true },
                    onBack = backToDefault
                )
                1 -> NormalModeContent(
                    title = title,
                    artist = currentAudio?.artist,
                    coverUrl = coverUrl,
                    coverTitle = currentAudio?.title,
                    lines = lyrics,
                    captionLines = captionLines,
                    progressMs = lyricProgress,
                    lyricsFontSize = lyricsFontSize,
                    lyricsLineSpacing = lyricsLineSpacing,
                    lyricsAlignment = lyricsAlignment,
                    isPlaying = state.playerState.isPlaying,
                    progress = if (durationMs > 0) progressMs.toFloat() / durationMs else 0f,
                    durationMs = durationMs,
                    isFavorite = currentFavorite,
                    loopMode = state.playerState.loopMode,
                    onPlayPause = { viewModel.togglePlayPause() },
                    onPrevious = { viewModel.previous() },
                    onNext = { viewModel.next() },
                    onSeek = { viewModel.seekTo(it) },
                    onToggleFavorite = { viewModel.toggleFavorite() },
                    onCycleLoopMode = { viewModel.cycleLoopMode() },
                    onMenu = { showMenuDialog = true },
                    onQueue = { showQueueDialog = true }
                )
                2 -> EqualizerPanel(
                    audio = currentAudio,
                    eqCurrentPreset = eqCurrentPreset,
                    currentV3Reverb = currentV3Reverb,
                    playStat = currentPlayStat,
                    savedNote = songKey?.let { songNotes[it] } ?: "",
                    onApplyHeadphonePreset = { viewModel.applyEqPreset(it.toEqPreset()) },
                    onResetEq = { viewModel.resetEq() },
                    onApplyV3Reverb = { viewModel.applyV3Reverb(it) },
                    onOpenSongNote = { showSongNoteDialog = true },
                    onToggleMusicStyle = onToggleMusicStyle,
                    onSearch = onSearch,
                    onBack = backToDefault
                )
            }
        }

        // 左上角 ASMR↔MUSIC 模式切换（与沉浸模式同源，三页常显）
        ModeToggleButton(
            mode = PlaybackMode.MUSIC,
            onToggle = onToggleMode,
            modifier = Modifier
                .align(Alignment.TopStart)
                .statusBarsPadding()
                .padding(top = 6.dp, start = 16.dp)
        )
    }

    // ── 更多菜单（与沉浸模式一致 + 写歌评）─────────────────
    if (showMenuDialog) {
        FrostedAlertDialog(
            onDismissRequest = { showMenuDialog = false },
            title = { Text(title, color = TextPrimary, maxLines = 1, overflow = TextOverflow.Ellipsis) },
            text = {
                Column(
                    modifier = Modifier
                        .height(520.dp)
                        .verticalScroll(rememberScrollState())
                ) {
                    MenuRow(stringResource(R.string.menu_timer), "", Icons.Filled.Timer) { showMenuDialog = false; showTimerDialog = true }
                    MenuRow(stringResource(R.string.menu_song_info), "", Icons.Filled.Info) { showMenuDialog = false; showSongInfoDialog = true }
                    MenuRow(stringResource(R.string.menu_share), "", Icons.Filled.Share) {
                        showMenuDialog = false
                        currentAudio?.let { ShareHelper.share(context, it) }
                    }
                    MenuRow(stringResource(R.string.menu_lyric_offset), "", Icons.Filled.Schedule) { showMenuDialog = false; showLyricOffsetDialog = true }
                    MenuRow(
                        stringResource(R.string.lyric_caption_title),
                        stringResource(
                            when (lyricCaptionMode) {
                                LyricCaptionMode.TRANSLATION -> R.string.lyric_caption_translation
                                LyricCaptionMode.ROMANIZATION -> R.string.lyric_caption_romanization
                                LyricCaptionMode.OFF -> R.string.lyric_caption_off
                            }
                        ),
                        Icons.Filled.Subtitles
                    ) { showMenuDialog = false; showLyricCaptionDialog = true }
                    MenuRow(
                        stringResource(R.string.lyric_mode),
                        stringResource(if (lyricsMode == LyricMode.SCROLL) R.string.lyric_mode_scroll else R.string.lyric_mode_jump),
                        Icons.Filled.Notes
                    ) {
                        viewModel.setLyricsMode(if (lyricsMode == LyricMode.SCROLL) LyricMode.JUMP else LyricMode.SCROLL)
                    }
                    MenuRow(stringResource(R.string.menu_queue), "", Icons.Filled.QueueMusic) { showMenuDialog = false; showQueueDialog = true }
                    MenuRow(stringResource(R.string.menu_background), "", Icons.Filled.Image) { showMenuDialog = false; showBackgroundDialog = true }
                    MenuRow(stringResource(R.string.menu_quality), "", Icons.Filled.HighQuality) { showMenuDialog = false; showQualityDialog = true }
                    MenuRow(stringResource(R.string.menu_lyrics_edit), "", Icons.Filled.Edit) { showMenuDialog = false; showLyricsEditDialog = true }
                    MenuRow(stringResource(R.string.menu_song_note), "", Icons.Filled.RateReview) { showMenuDialog = false; showSongNoteDialog = true }
                    if (currentAudio?.isRemoteSong() != true) {
                        MenuRow(stringResource(R.string.menu_playlist), "", Icons.Filled.PlaylistPlay) { showMenuDialog = false; showPlaylistDialog = true }
                    }
                    MenuRow(stringResource(R.string.menu_toolbox), "", Icons.Filled.Build) { showMenuDialog = false; showToolboxDialog = true }
                    MenuRow(stringResource(R.string.menu_effects), "", Icons.Filled.GraphicEq) { showMenuDialog = false; showEffectsDialog = true }
                    MenuRow(stringResource(R.string.menu_slice), "", Icons.Filled.ContentCut) { showMenuDialog = false; showSliceDialog = true }
                }
            },
            confirmButton = {},
            dismissButton = {
                TextButton(onClick = { showMenuDialog = false }) { Text(stringResource(R.string.common_close), color = TextSecondary) }
            },
            containerColor = FrostedSurface,
            shape = RoundedCornerShape(16.dp)
        )
    }

    // ── 播放队列 ───────────────────────────────────────
    if (showQueueDialog) {
        val playlist = state.playerState.playlist
        val currentIndex = state.playerState.playlistIndex
        FrostedAlertDialog(
            onDismissRequest = { showQueueDialog = false },
            title = { Text(stringResource(R.string.menu_queue), color = TextPrimary) },
            text = {
                Column(
                    modifier = Modifier
                        .height(360.dp)
                        .verticalScroll(rememberScrollState())
                ) {
                    if (playlist.isEmpty()) {
                        Text(stringResource(R.string.queue_empty), color = TextHint, fontSize = 13.sp, modifier = Modifier.padding(vertical = 12.dp))
                    }
                    playlist.forEachIndexed { index, audio ->
                        val isCurrent = index == currentIndex
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { viewModel.playPlaylist(playlist.drop(index)) }
                                .padding(vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = if (isCurrent) "▶ ${audio.title}" else audio.title,
                                color = if (isCurrent) LocalAccentColor.current else TextPrimary,
                                fontSize = 14.sp,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.weight(1f)
                            )
                            audio.artist.takeIf { it.isNotBlank() }?.let {
                                Text(it, color = TextHint, fontSize = 11.sp, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.padding(start = 8.dp))
                            }
                        }
                        HorizontalDivider(color = DarkSurfaceVariant.copy(alpha = 0.3f))
                    }
                }
            },
            confirmButton = {},
            dismissButton = {
                TextButton(onClick = { showQueueDialog = false }) { Text(stringResource(R.string.common_close), color = TextSecondary) }
            },
            containerColor = FrostedSurface,
            shape = RoundedCornerShape(16.dp)
        )
    }

    // ── 定时 ───────────────────────────────────────────
    if (showTimerDialog) {
        var minutes by remember { mutableStateOf(30) }
        FrostedAlertDialog(
            onDismissRequest = { showTimerDialog = false },
            title = { Text(stringResource(R.string.play_sleep_timer), color = TextPrimary) },
            text = {
                Column {
                    Text(stringResource(R.string.play_timer_stop_after, minutes), color = TextSecondary, fontSize = 14.sp)
                    Slider(
                        value = minutes.toFloat(),
                        onValueChange = { minutes = it.toInt() },
                        valueRange = 5f..120f,
                        steps = 22,
                        colors = SliderDefaults.colors(thumbColor = LocalAccentColor.current, activeTrackColor = LocalAccentColor.current)
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = { viewModel.setTimerSeconds(minutes * 60); showTimerDialog = false }) {
                    Text(stringResource(R.string.common_start), color = LocalAccentColor.current)
                }
            },
            dismissButton = {
                TextButton(onClick = { viewModel.cancelTimer(); showTimerDialog = false }) {
                    Text(stringResource(R.string.play_cancel_timer), color = TextSecondary)
                }
            },
            containerColor = FrostedSurface,
            shape = RoundedCornerShape(16.dp)
        )
    }

    // ── 歌曲信息 ───────────────────────────────────────
    if (showSongInfoDialog) {
        val audio = currentAudio
        val publishTime = audio?.publishTime ?: 0L
        val sourceText = when (audio?.sourceId) {
            "netease" -> stringResource(R.string.source_netease)
            "qq" -> stringResource(R.string.source_qq)
            else -> stringResource(R.string.song_info_source_local)
        }
        FrostedAlertDialog(
            onDismissRequest = { showSongInfoDialog = false },
            title = { Text(stringResource(R.string.song_info_title), color = TextPrimary) },
            text = {
                Column {
                    SongInfoCoverRow(coverUrl = audio?.coverUrl, title = audio?.title)
                    ClickableSongInfoRow(
                        label = stringResource(R.string.song_info_artist),
                        value = audio?.artist?.takeIf { it.isNotBlank() } ?: stringResource(R.string.common_unknown),
                        onClick = audio?.artist?.takeIf { it.isNotBlank() }?.let { artist ->
                            { showSongInfoDialog = false; onSearch(artist) }
                        }
                    )
                    ClickableSongInfoRow(
                        label = stringResource(R.string.song_info_album),
                        value = audio?.album?.takeIf { it.isNotBlank() } ?: stringResource(R.string.common_unknown),
                        onClick = audio?.album?.takeIf { it.isNotBlank() }?.let { album ->
                            { showSongInfoDialog = false; onSearch(album) }
                        }
                    )
                    SongInfoRow(stringResource(R.string.song_info_source), sourceText)
                    SongInfoRow(stringResource(R.string.song_info_publish), if (publishTime > 0L) formatDate(publishTime) else stringResource(R.string.common_unknown))
                }
            },
            confirmButton = {},
            dismissButton = {
                TextButton(onClick = { showSongInfoDialog = false }) { Text(stringResource(R.string.common_close), color = TextSecondary) }
            },
            containerColor = FrostedSurface,
            shape = RoundedCornerShape(16.dp)
        )
    }

    // ── 歌词偏移 ───────────────────────────────────────
    if (showLyricOffsetDialog) {
        FrostedAlertDialog(
            onDismissRequest = { showLyricOffsetDialog = false },
            title = { Text(stringResource(R.string.lyric_offset_title), color = TextPrimary) },
            text = {
                Column {
                    Text(stringResource(R.string.lyric_offset_value, lyricOffsetMs), color = TextSecondary, fontSize = 13.sp)
                    Slider(
                        value = lyricOffsetMs.toFloat(),
                        onValueChange = { viewModel.setLyricOffsetMs(it.toInt()) },
                        valueRange = -2000f..2000f,
                        steps = 39,
                        colors = SliderDefaults.colors(thumbColor = LocalAccentColor.current, activeTrackColor = LocalAccentColor.current)
                    )
                }
            },
            confirmButton = {},
            dismissButton = {
                Row {
                    TextButton(onClick = { viewModel.setLyricOffsetMs(0); showLyricOffsetDialog = false }) {
                        Text(stringResource(R.string.lyric_offset_reset), color = TextSecondary)
                    }
                    TextButton(onClick = { showLyricOffsetDialog = false }) {
                        Text(stringResource(R.string.common_close), color = TextSecondary)
                    }
                }
            },
            containerColor = FrostedSurface,
            shape = RoundedCornerShape(16.dp)
        )
    }

    // ── 歌词副文本 ─────────────────────────────────────
    if (showLyricCaptionDialog) {
        FrostedAlertDialog(
            onDismissRequest = { showLyricCaptionDialog = false },
            title = { Text(stringResource(R.string.lyric_caption_title), color = TextPrimary) },
            text = {
                Column {
                    val options = listOf(
                        LyricCaptionMode.OFF to R.string.lyric_caption_off,
                        LyricCaptionMode.TRANSLATION to R.string.lyric_caption_translation,
                        LyricCaptionMode.ROMANIZATION to R.string.lyric_caption_romanization
                    )
                    options.forEach { (mode, labelRes) ->
                        val selected = lyricCaptionMode == mode
                        TextButton(onClick = { viewModel.setLyricCaptionMode(mode); showLyricCaptionDialog = false }) {
                            Text(
                                text = if (selected) "▶ ${stringResource(labelRes)}" else stringResource(labelRes),
                                color = if (selected) LocalAccentColor.current else TextSecondary,
                                fontSize = 15.sp
                            )
                        }
                    }
                }
            },
            confirmButton = {},
            dismissButton = {
                TextButton(onClick = { showLyricCaptionDialog = false }) { Text(stringResource(R.string.common_close), color = TextSecondary) }
            },
            containerColor = FrostedSurface,
            shape = RoundedCornerShape(16.dp)
        )
    }

    // ── 背景 ───────────────────────────────────────────
    if (showBackgroundDialog) {
        FrostedAlertDialog(
            onDismissRequest = { showBackgroundDialog = false },
            title = { Text(stringResource(R.string.menu_background), color = TextPrimary) },
            text = {
                Column {
                    listOf(
                        PlaybackBackgroundMode.BLURRED_COVER to stringResource(R.string.bg_blurred_cover),
                        PlaybackBackgroundMode.FOLIA_DUOTONE to stringResource(R.string.bg_folia_duotone),
                        PlaybackBackgroundMode.SYSTEM_SOLID to stringResource(R.string.bg_system_solid)
                    ).forEach { (mode, label) ->
                        val selected = backgroundMode == mode
                        TextButton(
                            onClick = { viewModel.setBackgroundMode(mode); showBackgroundDialog = false },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = if (selected) "▶ $label" else label,
                                color = if (selected) LocalAccentColor.current else TextSecondary,
                                fontSize = 14.sp,
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                    }
                    // 自定义图片：跳转背景图库上传/选用
                    val customSelected = backgroundMode == PlaybackBackgroundMode.CUSTOM_IMAGE
                    TextButton(
                        onClick = {
                            viewModel.setBackgroundMode(PlaybackBackgroundMode.CUSTOM_IMAGE)
                            showBackgroundDialog = false
                            onNavigateToBackground()
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = if (customSelected) "▶ ${stringResource(R.string.bg_custom_image)}" else stringResource(R.string.bg_custom_image),
                            color = if (customSelected) LocalAccentColor.current else TextSecondary,
                            fontSize = 14.sp,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
            },
            confirmButton = {},
            dismissButton = {
                TextButton(onClick = { showBackgroundDialog = false }) { Text(stringResource(R.string.common_close), color = TextSecondary) }
            },
            containerColor = FrostedSurface,
            shape = RoundedCornerShape(16.dp)
        )
    }

    // ── 音质 ───────────────────────────────────────────
    if (showQualityDialog) {
        FrostedAlertDialog(
            onDismissRequest = { showQualityDialog = false },
            title = { Text(stringResource(R.string.menu_quality), color = TextPrimary) },
            text = {
                Column {
                    Text(stringResource(R.string.quality_hint), color = TextHint, fontSize = 12.sp)
                    Spacer(Modifier.height(6.dp))
                    listOf(
                        PlaybackQuality.STANDARD to stringResource(R.string.quality_standard),
                        PlaybackQuality.HIGHER to stringResource(R.string.quality_higher),
                        PlaybackQuality.EXCELLENT to stringResource(R.string.quality_excellent),
                        PlaybackQuality.LOSSLESS to stringResource(R.string.quality_lossless)
                    ).forEach { (quality, label) ->
                        val selected = playbackQuality == quality
                        TextButton(
                            onClick = { viewModel.setPlaybackQuality(quality); showQualityDialog = false },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = if (selected) "▶ $label" else label,
                                color = if (selected) LocalAccentColor.current else TextSecondary,
                                fontSize = 14.sp,
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                    }
                }
            },
            confirmButton = {},
            dismissButton = {
                TextButton(onClick = { showQualityDialog = false }) { Text(stringResource(R.string.common_close), color = TextSecondary) }
            },
            containerColor = FrostedSurface,
            shape = RoundedCornerShape(16.dp)
        )
    }

    // ── 歌词编辑 ───────────────────────────────────────
    if (showLyricsEditDialog) {
        var lyricText by remember(showLyricsEditDialog) { mutableStateOf(rawLyricsText) }
        FrostedAlertDialog(
            onDismissRequest = { showLyricsEditDialog = false },
            title = { Text(stringResource(R.string.play_lyrics_title), color = TextPrimary) },
            text = {
                Column {
                    Text(stringResource(R.string.play_lyrics_format_hint), color = TextHint, fontSize = 12.sp)
                    Spacer(Modifier.height(8.dp))
                    OutlinedTextField(
                        value = lyricText,
                        onValueChange = { lyricText = it },
                        placeholder = { Text(stringResource(R.string.play_lyrics_example), color = TextHint) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(260.dp)
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = { viewModel.saveLyrics(lyricText); showLyricsEditDialog = false }) {
                    Text(stringResource(R.string.play_save_lyrics), color = LocalAccentColor.current)
                }
            },
            dismissButton = {
                TextButton(onClick = { showLyricsEditDialog = false }) { Text(stringResource(R.string.common_close), color = TextSecondary) }
            },
            containerColor = FrostedSurface,
            shape = RoundedCornerShape(16.dp)
        )
    }

    // ── 写歌评 ─────────────────────────────────────────
    if (showSongNoteDialog) {
        val key = songKey
        var note by remember(key) { mutableStateOf(key?.let { songNotes[it] } ?: "") }
        FrostedAlertDialog(
            onDismissRequest = { showSongNoteDialog = false },
            title = { Text(stringResource(R.string.menu_song_note), color = TextPrimary) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = note,
                        onValueChange = { if (it.length <= 400) note = it },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(Radius.sm),
                        placeholder = { Text(stringResource(R.string.song_note_hint), color = TextHint) },
                        minLines = 4,
                        maxLines = 6,
                        enabled = key != null,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = TextPrimary,
                            unfocusedTextColor = TextPrimary,
                            focusedBorderColor = LocalAccentColor.current,
                            unfocusedBorderColor = DarkSurfaceVariant,
                            cursorColor = LocalAccentColor.current
                        )
                    )
                    Text("${note.length}/400", color = TextHint, fontSize = 11.sp, modifier = Modifier.align(Alignment.End))
                }
            },
            confirmButton = {
                TextButton(
                    onClick = { key?.let { viewModel.setSongNote(it, note) }; showSongNoteDialog = false },
                    enabled = key != null
                ) { Text(stringResource(R.string.common_save), color = LocalAccentColor.current) }
            },
            dismissButton = {
                TextButton(onClick = { showSongNoteDialog = false }) { Text(stringResource(R.string.common_close), color = TextSecondary) }
            },
            containerColor = FrostedSurface,
            shape = RoundedCornerShape(16.dp)
        )
    }

    // ── 添加到歌单 ─────────────────────────────────────
    if (showPlaylistDialog) {
        val playlists by playlistViewModel.playlists.collectAsStateWithLifecycle()
        val audio = currentAudio
        FrostedAlertDialog(
            onDismissRequest = { showPlaylistDialog = false },
            title = { Text(stringResource(R.string.play_add_to_playlist), color = TextPrimary) },
            text = {
                Column(
                    modifier = Modifier
                        .height(260.dp)
                        .verticalScroll(rememberScrollState())
                ) {
                    if (playlists.isEmpty()) {
                        Text(stringResource(R.string.play_no_playlists), color = TextHint, fontSize = 13.sp, modifier = Modifier.padding(vertical = 12.dp))
                    }
                    playlists.forEach { pl ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    audio?.let { a ->
                                        if (a.isRemoteSong()) playlistViewModel.addRemoteAudioToPlaylist(pl.id, a)
                                        else playlistViewModel.addAudioToPlaylist(pl.id, a.id)
                                    }
                                    showPlaylistDialog = false
                                }
                                .padding(vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(pl.name, color = TextPrimary, fontSize = 14.sp, modifier = Modifier.weight(1f), maxLines = 1, overflow = TextOverflow.Ellipsis)
                        }
                        HorizontalDivider(color = DarkSurfaceVariant.copy(alpha = 0.3f))
                    }
                }
            },
            confirmButton = {},
            dismissButton = {
                TextButton(onClick = { showPlaylistDialog = false }) { Text(stringResource(R.string.common_close), color = TextSecondary) }
            },
            containerColor = FrostedSurface,
            shape = RoundedCornerShape(16.dp)
        )
    }

    // ── 工具箱（与沉浸模式一致）─────────────────────────
    if (showToolboxDialog) {
        MusicToolboxDialog(
            viewModel = viewModel,
            state = state,
            onOpenAmbient = { showToolboxDialog = false; showAmbientDialog = true },
            onOpenBinaural = { showToolboxDialog = false; showBinauralDialog = true },
            onOpenNoise = { showToolboxDialog = false; showNoiseDialog = true },
            onDismiss = { showToolboxDialog = false }
        )
    }

    // ── 音效器（与沉浸模式一致）─────────────────────────
    if (showEffectsDialog) {
        MusicEffectsDialog(viewModel = viewModel, onDismiss = { showEffectsDialog = false })
    }

    // ── 双耳节拍 ──────────────────────────────────────
    if (showBinauralDialog) {
        MusicBinauralDialog(viewModel = viewModel, state = state, onDismiss = { showBinauralDialog = false })
    }

    // ── 噪音生成 ──────────────────────────────────────
    if (showNoiseDialog) {
        MusicNoiseDialog(viewModel = viewModel, state = state, onDismiss = { showNoiseDialog = false })
    }

    // ── 环境音 ────────────────────────────────────────
    if (showAmbientDialog) {
        MusicAmbientDialog(viewModel = viewModel, state = state, onDismiss = { showAmbientDialog = false })
    }

    // ── 切片 ───────────────────────────────────────────
    if (showSliceDialog) {
        val duration = state.playerState.durationMs
        var startMs by remember(duration) { mutableStateOf(0f) }
        var endMs by remember(duration) { mutableStateOf(duration.toFloat()) }
        FrostedAlertDialog(
            onDismissRequest = { showSliceDialog = false },
            title = { Text(stringResource(R.string.play_slice_title), color = TextPrimary) },
            text = {
                Column {
                    Text(stringResource(R.string.play_slice_start, formatTime(startMs.toLong())), color = TextSecondary, fontSize = 13.sp)
                    Slider(
                        value = startMs,
                        onValueChange = { startMs = it.coerceAtMost(endMs) },
                        valueRange = 0f..(duration.takeIf { it > 0 } ?: 1L).toFloat(),
                        colors = SliderDefaults.colors(thumbColor = LocalAccentColor.current, activeTrackColor = LocalAccentColor.current)
                    )
                    Text(stringResource(R.string.play_slice_end, formatTime(endMs.toLong())), color = TextSecondary, fontSize = 13.sp)
                    Slider(
                        value = endMs,
                        onValueChange = { endMs = it.coerceAtLeast(startMs) },
                        valueRange = 0f..(duration.takeIf { it > 0 } ?: 1L).toFloat(),
                        colors = SliderDefaults.colors(thumbColor = LocalAccentColor.current, activeTrackColor = LocalAccentColor.current)
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = { viewModel.setSlice(startMs.toLong(), endMs.toLong()); showSliceDialog = false }) {
                    Text(stringResource(R.string.play_slice_start_action), color = LocalAccentColor.current)
                }
            },
            dismissButton = {
                TextButton(onClick = { viewModel.clearSlice(); showSliceDialog = false }) {
                    Text(stringResource(R.string.play_clear), color = TextSecondary)
                }
            },
            containerColor = FrostedSurface,
            shape = RoundedCornerShape(16.dp)
        )
    }
}

/** 第 0 页：全屏可拖动滚动歌词 + 底部控制栏（与默认页一致），点击歌词行跳转到该句。 */
@Composable
private fun FullScreenLyrics(
    lines: List<WordLyricLine>,
    captionLines: List<WordLyricLine>,
    progressMs: Long,
    fontSize: Int,
    lineSpacing: Float,
    alignment: Int,
    isPlaying: Boolean,
    progress: Float,
    durationMs: Long,
    isFavorite: Boolean,
    loopMode: LoopMode,
    onPlayPause: () -> Unit,
    onPrevious: () -> Unit,
    onNext: () -> Unit,
    onSeek: (Long) -> Unit,
    onToggleFavorite: () -> Unit,
    onCycleLoopMode: () -> Unit,
    onMenu: () -> Unit,
    onQueue: () -> Unit,
    onBack: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding()
    ) {
        ScrollingLyrics(
            lines = lines,
            captionLines = captionLines,
            progressMs = progressMs,
            fontSize = fontSize,
            lineSpacing = lineSpacing,
            alignment = alignment,
            draggable = true,
            onLineClick = onSeek,
            modifier = Modifier
                .fillMaxSize()
                .padding(bottom = 170.dp)
        )
        // 顶部居中提示 + 返回（下滑或点按）
        Column(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .padding(top = 4.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Row(
                modifier = Modifier
                    .clip(RoundedCornerShape(50))
                    .background(DarkSurfaceVariant.copy(alpha = 0.7f))
                    .clickable(onClick = onBack)
                    .padding(horizontal = 14.dp, vertical = 7.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(Icons.Filled.ArrowDownward, contentDescription = null, tint = TextSecondary, modifier = Modifier.size(16.dp))
                Spacer(Modifier.width(4.dp))
                Text(stringResource(R.string.play_lyrics_back), color = TextSecondary, fontSize = 12.sp)
            }
        }
        // 底部控制栏（与默认页一致，叠加渐隐遮罩保证可读）
        Box(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .background(Brush.verticalGradient(listOf(Color.Transparent, DarkSurface)))
        ) {
            NormalControls(
                isPlaying = isPlaying,
                progress = progress,
                durationMs = durationMs,
                isFavorite = isFavorite,
                loopMode = loopMode,
                onPlayPause = onPlayPause,
                onPrevious = onPrevious,
                onNext = onNext,
                onSeek = onSeek,
                onToggleFavorite = onToggleFavorite,
                onCycleLoopMode = onCycleLoopMode,
                onMenu = onMenu,
                onQueue = onQueue,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp)
                    .padding(top = 24.dp, bottom = 16.dp)
            )
        }
    }
}

/** 第 1 页：默认正常模式（封面 + 自动居中滚动歌词 + 控制组件）。 */
@Composable
private fun NormalModeContent(
    title: String,
    artist: String?,
    coverUrl: String?,
    coverTitle: String?,
    lines: List<WordLyricLine>,
    captionLines: List<WordLyricLine>,
    progressMs: Long,
    lyricsFontSize: Int,
    lyricsLineSpacing: Float,
    lyricsAlignment: Int,
    isPlaying: Boolean,
    progress: Float,
    durationMs: Long,
    isFavorite: Boolean,
    loopMode: LoopMode,
    onPlayPause: () -> Unit,
    onPrevious: () -> Unit,
    onNext: () -> Unit,
    onSeek: (Long) -> Unit,
    onToggleFavorite: () -> Unit,
    onCycleLoopMode: () -> Unit,
    onMenu: () -> Unit,
    onQueue: () -> Unit
) {
    val accent = LocalAccentColor.current
    Box(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .padding(horizontal = 20.dp)
        ) {
            // 顶部留白给左上角模式按钮，避免歌名与其重叠
            Spacer(Modifier.height(40.dp))

            Text(
                text = title,
                color = TextPrimary,
                fontSize = 18.sp,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )
            artist?.takeIf { it.isNotBlank() }?.let {
                Text(
                    text = it,
                    color = TextSecondary,
                    fontSize = 13.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    textAlign = TextAlign.Center,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 2.dp)
                )
            }

            // 中上：封面（下方叠一层取色晕影，渐变到页面底色，弱化封面边缘切线）
            val coverGlow = rememberCoverPalette(
                coverUrl,
                fallbackPrimary = accent.toArgb().toLong() and 0xFFFFFFFFL,
                fallbackSecondary = accent.toArgb().toLong() and 0xFFFFFFFFL
            ).first
            val glowColor = Color(coverGlow.toInt())
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(0.46f),
                contentAlignment = Alignment.Center
            ) {
                Box(
                    modifier = Modifier
                        .size(320.dp)
                        .background(
                            Brush.radialGradient(
                                colors = listOf(glowColor.copy(alpha = 0.35f), Color.Transparent)
                            )
                        )
                )
                AlbumCover(coverUrl, coverTitle)
            }

            // 中下上：滚动歌词（默认不可拖动，占比减小让位控制栏）
            ScrollingLyrics(
                lines = lines,
                captionLines = captionLines,
                progressMs = progressMs,
                fontSize = lyricsFontSize,
                lineSpacing = lyricsLineSpacing,
                alignment = lyricsAlignment,
                draggable = false,
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(0.26f)
            )

            // 中下下：控制组件
            NormalControls(
                isPlaying = isPlaying,
                progress = progress,
                durationMs = durationMs,
                isFavorite = isFavorite,
                loopMode = loopMode,
                onPlayPause = onPlayPause,
                onPrevious = onPrevious,
                onNext = onNext,
                onSeek = onSeek,
                onToggleFavorite = onToggleFavorite,
                onCycleLoopMode = onCycleLoopMode,
                onMenu = onMenu,
                onQueue = onQueue,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp)
            )
        }
    }
}

/** 第 2 页：音效器面板（均衡器：耳机适配 + 混响统一入口）+ 切回沉浸 + 歌曲信息 + 歌曲笔记（折叠）。 */
@Composable
private fun EqualizerPanel(
    audio: com.asmrhelper.domain.model.Audio?,
    eqCurrentPreset: String,
    currentV3Reverb: V3ReverbPreset,
    playStat: PlayStatEntity?,
    savedNote: String,
    onApplyHeadphonePreset: (HeadphonePreset) -> Unit,
    onResetEq: () -> Unit,
    onApplyV3Reverb: (V3ReverbPreset) -> Unit,
    onOpenSongNote: () -> Unit,
    onToggleMusicStyle: () -> Unit,
    onSearch: (String) -> Unit,
    onBack: () -> Unit
) {
    var selectedBrand by remember { mutableStateOf(HeadphonePresets.BRANDS.firstOrNull() ?: "") }
    val accent = LocalAccentColor.current

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding(),
        contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 48.dp, bottom = 32.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // 切回沉浸模式（主入口）
        item {
            Button(
                onClick = onToggleMusicStyle,
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(containerColor = accent, contentColor = Color.White)
            ) {
                Text(stringResource(R.string.play_switch_immersive), fontWeight = FontWeight.SemiBold)
            }
        }

        // ── 均衡器（耳机适配 + 混响统一入口）──
        item { SectionTitle(stringResource(R.string.category_equalizer)) }
        // 耳机适配
        item { SubTitle(stringResource(R.string.eq_headphone_presets)) }
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                HeadphonePresets.BRANDS.forEach { brand ->
                    Chip(text = brand, selected = brand == selectedBrand) { selectedBrand = brand }
                }
            }
        }
        items(HeadphonePresets.byBrand(selectedBrand), key = { it.model }) { preset ->
            val selected = eqCurrentPreset == preset.model
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(if (selected) accent.copy(alpha = 0.16f) else DarkSurfaceVariant)
                    .clickable { onApplyHeadphonePreset(preset) }
                    .padding(horizontal = 14.dp, vertical = 11.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(preset.model, color = if (selected) accent else TextPrimary, fontSize = 14.sp)
            }
        }
        item {
            Text(
                text = stringResource(R.string.settings_eq_reset),
                color = TextSecondary,
                fontSize = 13.sp,
                modifier = Modifier
                    .clip(RoundedCornerShape(10.dp))
                    .clickable { onResetEq() }
                    .padding(horizontal = 4.dp, vertical = 6.dp)
            )
        }
        // 混响（V3，独立于沉浸模式场景效果）
        item { SubTitle(stringResource(R.string.eq_reverb)) }
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                V3ReverbPreset.entries.forEach { preset ->
                    Chip(
                        text = stringResource(preset.labelRes),
                        selected = preset == currentV3Reverb
                    ) { onApplyV3Reverb(preset) }
                }
            }
        }

        // ── 歌曲信息（歌手/专辑带小图占位）──
        item { SectionTitle(stringResource(R.string.song_info_title)) }
        item {
            val sourceText = when {
                audio == null -> stringResource(R.string.song_info_source_local)
                audio.sourceId == "netease" -> stringResource(R.string.source_netease)
                audio.sourceId == "qq" -> stringResource(R.string.source_qq)
                audio.sourceId.isNotBlank() -> audio.sourceId
                else -> stringResource(R.string.song_info_source_local)
            }
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                // 歌手无头像数据（首字占位）；专辑用封面图。点击歌手/专辑跳到发现页搜索。
                SongInfoAvatarRow(
                    label = stringResource(R.string.song_info_artist),
                    value = audio?.artist,
                    imageUrl = null,
                    onClick = audio?.artist?.takeIf { it.isNotBlank() }?.let { artist -> { onSearch(artist) } }
                )
                SongInfoAvatarRow(
                    label = stringResource(R.string.song_info_album),
                    value = audio?.album,
                    imageUrl = audio?.coverUrl,
                    onClick = audio?.album?.takeIf { it.isNotBlank() }?.let { album -> { onSearch(album) } }
                )
                InfoRow(stringResource(R.string.song_info_source), sourceText)
                InfoRow(
                    stringResource(R.string.song_info_listen_count),
                    playStat?.takeIf { it.listenCount > 0 }?.listenCount?.toString()
                )
                InfoRow(
                    stringResource(R.string.song_info_first_listen),
                    playStat?.takeIf { it.firstListenAt > 0 }?.let { formatDateTime(it.firstListenAt) }
                )
            }
        }

        // ── 歌曲笔记（折叠，点击打开弹窗编辑）──
        item { SectionTitle(stringResource(R.string.song_note_title)) }
        item {
            val preview = savedNote.takeIf { it.isNotBlank() }?.let { n -> if (n.length > 24) n.take(24) + "…" else n }
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(Radius.md))
                    .background(DarkSurfaceVariant)
                    .clickable { onOpenSongNote() }
                    .padding(horizontal = 14.dp, vertical = 13.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = preview ?: stringResource(R.string.song_note_open_hint),
                    color = if (preview != null) TextPrimary else TextHint,
                    fontSize = 13.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                )
                Text("›", color = TextSecondary, fontSize = 18.sp, fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
private fun SectionTitle(text: String) {
    val accent = LocalAccentColor.current
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
            modifier = Modifier
                .size(width = 3.dp, height = 14.dp)
                .clip(RoundedCornerShape(2.dp))
                .background(accent)
        )
        Spacer(Modifier.width(8.dp))
        Text(text, color = TextPrimary, fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
    }
}

@Composable
private fun InfoRow(label: String, value: String?) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(label, color = TextSecondary, fontSize = 13.sp)
        Text(value?.takeIf { it.isNotBlank() } ?: "—", color = TextPrimary, fontSize = 13.sp, textAlign = TextAlign.End)
    }
}

/** 分区小标题（低于 SectionTitle 一级的次级标签，如均衡器下的「耳机适配」「混响」）。 */
@Composable
private fun SubTitle(text: String) {
    Text(text, color = TextSecondary, fontSize = 12.sp, fontWeight = FontWeight.Medium)
}

/** 歌曲信息行（歌手/专辑）：左侧小图（有图显示封面、无图用首字头像），右侧值，可点跳搜索。 */
@Composable
private fun SongInfoAvatarRow(label: String, value: String?, imageUrl: String? = null, onClick: (() -> Unit)? = null) {
    val accent = LocalAccentColor.current
    val rowModifier = if (onClick != null) {
        Modifier.fillMaxWidth().clickable(onClick = onClick).padding(vertical = 6.dp)
    } else {
        Modifier.fillMaxWidth().padding(vertical = 6.dp)
    }
    Row(
        modifier = rowModifier,
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (imageUrl != null) {
            AsyncImage(
                model = imageUrl,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .size(28.dp)
                    .clip(CircleShape)
            )
        } else {
            Box(
                modifier = Modifier
                    .size(28.dp)
                    .clip(CircleShape)
                    .background(accent.copy(alpha = 0.18f)),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = value?.trim()?.firstOrNull()?.toString() ?: "?",
                    color = accent,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }
        Spacer(Modifier.width(10.dp))
        Text(label, color = TextSecondary, fontSize = 13.sp)
        Spacer(Modifier.weight(1f))
        Text(
            text = value?.takeIf { it.isNotBlank() } ?: "—",
            color = TextPrimary,
            fontSize = 13.sp,
            textAlign = TextAlign.End,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        if (onClick != null) {
            Spacer(Modifier.width(4.dp))
            Text("›", color = TextSecondary, fontSize = 16.sp, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
private fun Chip(text: String, selected: Boolean, onClick: () -> Unit) {
    val accent = LocalAccentColor.current
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(Radius.md))
            .background(if (selected) accent else DarkSurfaceVariant)
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 7.dp)
    ) {
        Text(text, color = if (selected) Color.White else TextSecondary, fontSize = 13.sp)
    }
}

/** 专辑封面：圆角方片；无封面时显示音符占位。 */
@Composable
private fun AlbumCover(coverUrl: String?, title: String?) {
    val accent = LocalAccentColor.current
    Box(
        modifier = Modifier
            .size(210.dp)
            .clip(RoundedCornerShape(20.dp))
            .background(DarkSurfaceVariant),
        contentAlignment = Alignment.Center
    ) {
        if (!coverUrl.isNullOrBlank()) {
            AsyncImage(
                model = coverUrl,
                contentDescription = title,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )
        } else {
            Icon(
                imageVector = Icons.Filled.MusicNote,
                contentDescription = null,
                tint = accent.copy(alpha = 0.5f),
                modifier = Modifier.size(64.dp)
            )
        }
    }
}

/**
 * 滚动歌词：LazyColumn 按当前句自动居中（`animateScrollToItem`），
 * 当前句高亮、其余句弱化。`draggable` 为 false 时禁用用户手动拖动（默认正常模式）。
 */
@Composable
private fun ScrollingLyrics(
    lines: List<WordLyricLine>,
    captionLines: List<WordLyricLine> = emptyList(),
    progressMs: Long,
    modifier: Modifier = Modifier,
    fontSize: Int = 16,
    lineSpacing: Float = 1.5f,
    alignment: Int = 0,
    draggable: Boolean = false,
    onLineClick: ((Long) -> Unit)? = null
) {
    val accent = LocalAccentColor.current
    val textAlign = when (alignment) {
        1 -> TextAlign.Left
        2 -> TextAlign.Right
        else -> TextAlign.Center
    }
    val currentIndex = remember(lines, progressMs) { lines.currentLineIndex(progressMs) }
    val listState = rememberLazyListState()

    if (lines.isEmpty()) {
        Box(modifier = modifier, contentAlignment = Alignment.Center) {
            Text(stringResource(R.string.no_lyrics), color = TextSecondary, fontSize = 15.sp)
        }
        return
    }

    BoxWithConstraints(modifier = modifier) {
        // 上下各留半屏减去单行高度，使滚动到当前句时它正好垂直居中
        val viewportH = maxHeight
        val lineH = (fontSize * lineSpacing).dp + 8.dp
        val verticalPad = ((viewportH - lineH) / 2).coerceAtLeast(0.dp)

        LaunchedEffect(currentIndex) {
            if (currentIndex in lines.indices) listState.animateScrollToItem(currentIndex)
        }

        LazyColumn(
            state = listState,
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(vertical = verticalPad),
            userScrollEnabled = draggable,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            itemsIndexed(lines, key = { i, _ -> i }) { index, line ->
                val isCurrent = index == currentIndex
                val caption = if (captionLines.isEmpty()) null else captionLines.captionFor(line)
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable(enabled = onLineClick != null) { onLineClick?.invoke(line.startMs) }
                        .padding(horizontal = 20.dp, vertical = 6.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = line.text,
                        color = if (isCurrent) accent else TextSecondary,
                        fontSize = if (isCurrent) fontSize.sp else (fontSize - 2).coerceAtLeast(10).sp,
                        fontWeight = if (isCurrent) FontWeight.Bold else FontWeight.Normal,
                        textAlign = textAlign,
                        lineHeight = (fontSize * lineSpacing).sp,
                        modifier = Modifier.fillMaxWidth()
                    )
                    if (!caption.isNullOrBlank()) {
                        Text(
                            text = caption,
                            color = if (isCurrent) TextSecondary else TextSecondary.copy(alpha = 0.6f),
                            fontSize = (fontSize - 4).coerceAtLeast(10).sp,
                            textAlign = textAlign,
                            lineHeight = (fontSize * lineSpacing).sp,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 2.dp)
                        )
                    }
                }
            }
        }
    }
}

/** 正常模式底部控制组件：细进度条 + 时间，收藏 / 上一曲 / 播放暂停 / 下一曲 / 队列 / 菜单 / 循环。 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun NormalControls(
    isPlaying: Boolean,
    progress: Float,
    durationMs: Long,
    isFavorite: Boolean,
    loopMode: LoopMode,
    onPlayPause: () -> Unit,
    onPrevious: () -> Unit,
    onNext: () -> Unit,
    onSeek: (Long) -> Unit,
    onToggleFavorite: () -> Unit,
    onCycleLoopMode: () -> Unit,
    onMenu: () -> Unit,
    onQueue: () -> Unit,
    modifier: Modifier = Modifier
) {
    val accent = LocalAccentColor.current
    // 四种播放模式各用不同图标区分：播完即停=Stop、单曲=RepeatOne、列表=Repeat、随机=Shuffle。
    // 颜色统一跟随旁边控制按钮（TextPrimary），不再用「灰/亮」表达状态。
    val loopIcon = when (loopMode) {
        LoopMode.NONE -> Icons.Filled.Stop
        LoopMode.SINGLE -> Icons.Filled.RepeatOne
        LoopMode.LIST -> Icons.Filled.Repeat
        LoopMode.SHUFFLE -> Icons.Filled.Shuffle
    }
    var dragValue by remember { mutableStateOf<Float?>(null) }
    val shownValue = dragValue ?: progress.coerceIn(0f, 1f)
    val currentMs = (shownValue * durationMs).toLong()

    Column(modifier = modifier) {
        // 细进度条：2dp 轨道 + 白色光晕圆点（复用沉浸模式 FoliaCommandBar 质感）
        Slider(
            value = shownValue,
            onValueChange = { dragValue = it },
            onValueChangeFinished = {
                dragValue?.let { onSeek((it * durationMs).toLong()) }
                dragValue = null
            },
            valueRange = 0f..1f,
            colors = SliderDefaults.colors(
                thumbColor = accent,
                activeTrackColor = accent,
                inactiveTrackColor = DarkSurfaceVariant.copy(alpha = 0.6f)
            ),
            thumb = {
                Box(
                    modifier = Modifier
                        .size(16.dp)
                        .background(
                            brush = Brush.radialGradient(
                                colors = listOf(accent.copy(alpha = 0.30f), Color.Transparent)
                            ),
                            shape = CircleShape
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Box(
                        Modifier
                            .size(10.dp)
                            .background(Color.White, CircleShape)
                    )
                }
            },
            track = { sliderState ->
                SliderDefaults.Track(
                    sliderState = sliderState,
                    modifier = Modifier.height(2.dp),
                    colors = SliderDefaults.colors(
                        thumbColor = accent,
                        activeTrackColor = accent,
                        inactiveTrackColor = DarkSurfaceVariant.copy(alpha = 0.6f)
                    )
                )
            }
        )
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 6.dp),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(formatTime(currentMs), color = TextHint, fontSize = 11.sp)
            Text(formatTime(durationMs), color = TextHint, fontSize = 11.sp)
        }
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 4.dp),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onToggleFavorite) {
                Icon(
                    imageVector = if (isFavorite) Icons.Filled.Favorite else Icons.Filled.FavoriteBorder,
                    contentDescription = stringResource(if (isFavorite) R.string.library_remove_favorite else R.string.library_add_favorite),
                    tint = if (isFavorite) accent else TextSecondary
                )
            }
            IconButton(onClick = onPrevious) {
                Icon(Icons.Filled.SkipPrevious, contentDescription = stringResource(R.string.play_prev_track), tint = TextPrimary)
            }
            IconButton(onClick = onPlayPause) {
                Icon(
                    imageVector = if (isPlaying) Icons.Filled.Pause else Icons.Filled.PlayArrow,
                    contentDescription = if (isPlaying) stringResource(R.string.play_pause) else stringResource(R.string.play_play),
                    tint = TextPrimary,
                    modifier = Modifier.size(44.dp)
                )
            }
            IconButton(onClick = onNext) {
                Icon(Icons.Filled.SkipNext, contentDescription = stringResource(R.string.play_next_track), tint = TextPrimary)
            }
            IconButton(onClick = onQueue) {
                Icon(Icons.Filled.QueueMusic, contentDescription = stringResource(R.string.menu_queue), tint = TextSecondary)
            }
            IconButton(onClick = onMenu) {
                Icon(Icons.Filled.MoreVert, contentDescription = stringResource(R.string.menu_title), tint = TextSecondary)
            }
            IconButton(onClick = onCycleLoopMode) {
                Icon(loopIcon, contentDescription = null, tint = TextPrimary)
            }
        }
    }
}

private fun formatTime(ms: Long): String {
    val totalSec = ms / 1000
    val min = totalSec / 60
    val sec = totalSec % 60
    return "%02d:%02d".format(min, sec)
}

private fun formatDate(ms: Long): String =
    java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.getDefault()).format(java.util.Date(ms))

private fun formatDateTime(ms: Long): String =
    java.text.SimpleDateFormat("yyyy-MM-dd HH:mm", java.util.Locale.getDefault()).format(java.util.Date(ms))

/** 菜单行：左标签 + 可选右值，整行可点。 */
@Composable
private fun MenuRow(label: String, value: String, icon: ImageVector? = null, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (icon != null) {
            Icon(icon, contentDescription = null, tint = TextSecondary, modifier = Modifier.size(20.dp))
            Spacer(Modifier.width(12.dp))
        }
        Text(label, color = TextPrimary, fontSize = 14.sp, modifier = Modifier.weight(1f))
        if (value.isNotEmpty()) Text(value, color = LocalAccentColor.current, fontSize = 13.sp)
    }
}

/** 歌曲信息行：左标签固定宽度 + 右值。 */
@Composable
private fun SongInfoRow(label: String, value: String) {
    Row(modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp), verticalAlignment = Alignment.Top) {
        Text(label, color = TextHint, fontSize = 13.sp, modifier = Modifier.width(72.dp))
        Text(value, color = TextPrimary, fontSize = 14.sp, modifier = Modifier.weight(1f))
    }
}

/** 歌曲信息行（可点击，如歌手/专辑跳搜索）。 */
@Composable
private fun ClickableSongInfoRow(label: String, value: String, onClick: (() -> Unit)?) {
    val rowModifier = if (onClick != null) {
        Modifier.fillMaxWidth().clickable(onClick = onClick).padding(vertical = 6.dp)
    } else {
        Modifier.fillMaxWidth().padding(vertical = 6.dp)
    }
    Row(modifier = rowModifier, verticalAlignment = Alignment.Top) {
        Text(label, color = TextHint, fontSize = 13.sp, modifier = Modifier.width(72.dp))
        Text(value, color = TextPrimary, fontSize = 14.sp, modifier = Modifier.weight(1f))
        if (onClick != null) {
            Text("›", color = TextSecondary, fontSize = 16.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(start = 4.dp))
        }
    }
}

/** 歌曲信息封面行：左侧封面图 + 右侧标题。 */
@Composable
private fun SongInfoCoverRow(coverUrl: String?, title: String?) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (coverUrl != null) {
            AsyncImage(
                model = coverUrl,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .size(52.dp)
                    .clip(RoundedCornerShape(8.dp))
            )
            Spacer(Modifier.width(12.dp))
        }
        Text(
            text = title?.takeIf { it.isNotBlank() } ?: stringResource(R.string.common_unknown),
            color = TextPrimary,
            fontSize = 15.sp,
            fontWeight = FontWeight.SemiBold,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f)
        )
    }
}
