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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Repeat
import androidx.compose.material.icons.filled.RepeatOne
import androidx.compose.material.icons.filled.Shuffle
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.SkipPrevious
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.SwapVert
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
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
import com.asmrhelper.domain.model.LoopMode
import com.asmrhelper.domain.model.PlaybackMode
import com.asmrhelper.player.HeadphonePreset
import com.asmrhelper.player.HeadphonePresets
import com.asmrhelper.player.SceneEffect
import com.asmrhelper.ui.components.ModeToggleButton
import com.asmrhelper.ui.theme.DarkSurface
import com.asmrhelper.ui.theme.DarkSurfaceVariant
import com.asmrhelper.ui.theme.LocalAccentColor
import com.asmrhelper.ui.theme.TextHint
import com.asmrhelper.ui.theme.TextPrimary
import com.asmrhelper.ui.theme.TextSecondary
import com.asmrhelper.util.WordLyricLine
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
    onToggleMusicStyle: () -> Unit = {}
) {
    val viewModel: PlayViewModel = hiltViewModel()
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val lyrics by viewModel.lyrics.collectAsStateWithLifecycle()
    val lyricsFontSize by viewModel.lyricsFontSize.collectAsStateWithLifecycle()
    val lyricsLineSpacing by viewModel.lyricsLineSpacing.collectAsStateWithLifecycle()
    val lyricsAlignment by viewModel.lyricsAlignment.collectAsStateWithLifecycle()
    val lyricOffsetMs by viewModel.lyricOffsetMs.collectAsStateWithLifecycle()
    val currentFavorite by viewModel.currentFavorite.collectAsStateWithLifecycle()
    val eqCurrentPreset by viewModel.eqCurrentPreset.collectAsStateWithLifecycle()
    val currentSceneEffect by viewModel.currentSceneEffect.collectAsStateWithLifecycle()
    val songNotes by viewModel.songNotes.collectAsStateWithLifecycle()

    val currentAudio = state.playerState.currentAudio
    val coverUrl = currentAudio?.coverUrl
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

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(DarkSurface)
    ) {
        VerticalPager(state = pagerState, modifier = Modifier.fillMaxSize()) { page ->
            when (page) {
                0 -> FullScreenLyrics(
                    lines = lyrics,
                    progressMs = lyricProgress,
                    fontSize = lyricsFontSize,
                    lineSpacing = lyricsLineSpacing,
                    alignment = lyricsAlignment,
                    onBack = backToDefault
                )
                1 -> NormalModeContent(
                    title = title,
                    artist = currentAudio?.artist,
                    coverUrl = coverUrl,
                    coverTitle = currentAudio?.title,
                    lines = lyrics,
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
                    onToggleMusicStyle = onToggleMusicStyle
                )
                2 -> EqualizerPanel(
                    audio = currentAudio,
                    eqCurrentPreset = eqCurrentPreset,
                    currentSceneEffect = currentSceneEffect,
                    songKey = songKey,
                    savedNote = songKey?.let { songNotes[it] } ?: "",
                    onApplyHeadphonePreset = { viewModel.applyEqPreset(it.toEqPreset()) },
                    onResetEq = { viewModel.resetEq() },
                    onApplySceneEffect = { viewModel.applySceneEffect(it) },
                    onSetSongNote = { note -> songKey?.let { k -> viewModel.setSongNote(k, note) } },
                    onToggleMusicStyle = onToggleMusicStyle,
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
}

/** 第 0 页：全屏可拖动滚动歌词，点击或下滑返回默认页。 */
@Composable
private fun FullScreenLyrics(
    lines: List<WordLyricLine>,
    progressMs: Long,
    fontSize: Int,
    lineSpacing: Float,
    alignment: Int,
    onBack: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding()
    ) {
        ScrollingLyrics(
            lines = lines,
            progressMs = progressMs,
            fontSize = fontSize,
            lineSpacing = lineSpacing,
            alignment = alignment,
            draggable = true,
            modifier = Modifier.fillMaxSize()
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
    onToggleMusicStyle: () -> Unit
) {
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

        // 中上：封面
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(0.42f),
            contentAlignment = Alignment.Center
        ) {
            AlbumCover(coverUrl, coverTitle)
        }

        // 中下上二分之一：滚动歌词（默认不可拖动）
        ScrollingLyrics(
            lines = lines,
            progressMs = progressMs,
            fontSize = lyricsFontSize,
            lineSpacing = lyricsLineSpacing,
            alignment = lyricsAlignment,
            draggable = false,
            modifier = Modifier
                .fillMaxWidth()
                .weight(0.34f)
        )

        // 中下下二分之一：控制组件
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
            onToggleMusicStyle = onToggleMusicStyle,
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 16.dp)
        )
    }
}

/** 第 2 页：音效器面板（耳机适配 + 混响）+ 切回沉浸 + 歌曲信息 + 歌曲笔记。 */
@Composable
private fun EqualizerPanel(
    audio: com.asmrhelper.domain.model.Audio?,
    eqCurrentPreset: String,
    currentSceneEffect: SceneEffect,
    songKey: String?,
    savedNote: String,
    onApplyHeadphonePreset: (HeadphonePreset) -> Unit,
    onResetEq: () -> Unit,
    onApplySceneEffect: (SceneEffect) -> Unit,
    onSetSongNote: (String) -> Unit,
    onToggleMusicStyle: () -> Unit,
    onBack: () -> Unit
) {
    var selectedBrand by remember { mutableStateOf(HeadphonePresets.BRANDS.firstOrNull() ?: "") }
    var noteText by remember(songKey, savedNote) { mutableStateOf(savedNote) }
    // 切换歌曲后同步文本框
    LaunchedEffect(songKey, savedNote) { noteText = savedNote }
    var noteSaved by remember { mutableStateOf(false) }
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

        // ── 耳机适配 ──
        item { SectionTitle(stringResource(R.string.eq_headphone_presets)) }
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

        // ── 混响 ──
        item { SectionTitle(stringResource(R.string.eq_reverb)) }
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                SceneEffect.entries.forEach { scene ->
                    Chip(
                        text = stringResource(scene.labelRes),
                        selected = scene == currentSceneEffect
                    ) { onApplySceneEffect(scene) }
                }
            }
        }

        // ── 歌曲信息 ──
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
                InfoRow(stringResource(R.string.song_info_artist), audio?.artist)
                InfoRow(stringResource(R.string.song_info_album), audio?.album)
                InfoRow(stringResource(R.string.song_info_source), sourceText)
            }
        }

        // ── 歌曲笔记 ──
        item { SectionTitle(stringResource(R.string.song_note_title)) }
        item {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = noteText,
                    onValueChange = { if (it.length <= 400) { noteText = it; noteSaved = false } },
                    modifier = Modifier.fillMaxWidth(),
                    placeholder = { Text(stringResource(R.string.song_note_hint), color = TextHint) },
                    supportingText = { Text("${noteText.length}/400", color = TextHint, fontSize = 11.sp) },
                    minLines = 4,
                    maxLines = 6,
                    enabled = songKey != null,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = TextPrimary,
                        unfocusedTextColor = TextPrimary,
                        focusedBorderColor = accent,
                        unfocusedBorderColor = DarkSurfaceVariant,
                        cursorColor = accent
                    )
                )
                Button(
                    onClick = {
                        onSetSongNote(noteText)
                        noteSaved = true
                    },
                    enabled = songKey != null,
                    modifier = Modifier.align(Alignment.End),
                    colors = ButtonDefaults.buttonColors(containerColor = accent, contentColor = Color.White)
                ) {
                    Text(if (noteSaved) stringResource(R.string.common_done) else stringResource(R.string.common_save))
                }
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
        Text(text, color = TextPrimary, fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
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

@Composable
private fun Chip(text: String, selected: Boolean, onClick: () -> Unit) {
    val accent = LocalAccentColor.current
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(50))
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
    progressMs: Long,
    modifier: Modifier = Modifier,
    fontSize: Int = 16,
    lineSpacing: Float = 1.5f,
    alignment: Int = 0,
    draggable: Boolean = false
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
                Text(
                    text = line.text,
                    color = if (isCurrent) accent else TextSecondary,
                    fontSize = if (isCurrent) fontSize.sp else (fontSize - 2).coerceAtLeast(10).sp,
                    fontWeight = if (isCurrent) FontWeight.Bold else FontWeight.Normal,
                    textAlign = textAlign,
                    lineHeight = (fontSize * lineSpacing).sp,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 6.dp)
                )
            }
        }
    }
}

/** 正常模式底部控制组件：进度条 + 时间，收藏 / 上一曲 / 播放暂停 / 下一曲 / 切回沉浸 / 循环。 */
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
    onToggleMusicStyle: () -> Unit,
    modifier: Modifier = Modifier
) {
    val accent = LocalAccentColor.current
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
            )
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
            IconButton(onClick = onToggleMusicStyle) {
                Icon(
                    imageVector = Icons.Filled.SwapVert,
                    contentDescription = stringResource(R.string.play_toggle_style),
                    tint = TextSecondary
                )
            }
            IconButton(onClick = onCycleLoopMode) {
                Icon(loopIcon, contentDescription = null, tint = TextSecondary)
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
