package com.asmrhelper.ui.play.folia

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.media.MediaMetadataRetriever
import androidx.compose.runtime.Composable
import androidx.compose.runtime.produceState
import androidx.compose.ui.platform.LocalContext
import androidx.core.graphics.drawable.toBitmap
import androidx.palette.graphics.Palette
import coil.Coil
import coil.request.ImageRequest
import com.asmrhelper.util.opaqueArgb
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * 从封面（在线 URL 或本地内嵌图的 Bitmap）提取双色调主/次色（ARGB 编码为 Long）。
 * 主色取 vibrant（缺则 dominant），次色取 darkMuted（缺则 muted，再缺则主色）；
 * 空封面或加载失败时分别回退 [fallbackPrimary]/[fallbackSecondary]。
 */
@Composable
fun rememberCoverPalette(
    model: Any?,
    fallbackPrimary: Long,
    fallbackSecondary: Long
): Pair<Long, Long> {
    val context = LocalContext.current
    val loader = Coil.imageLoader(context)
    val fallback = fallbackPrimary to fallbackSecondary
    val state = produceState(initialValue = fallback, model, loader) {
        if (model == null) {
            value = fallback
            return@produceState
        }
        value = withContext(Dispatchers.IO) {
            try {
                val request = ImageRequest.Builder(context)
                    .data(model)
                    .allowHardware(false)
                    .build()
                val bitmap = loader.execute(request).drawable?.toBitmap()
                val palette = bitmap?.let { Palette.from(it).generate() }
                val primary = palette?.vibrantSwatch ?: palette?.dominantSwatch
                val secondary = palette?.darkMutedSwatch ?: palette?.mutedSwatch ?: primary
                val p = primary?.rgb?.let { opaqueArgb(it) } ?: fallbackPrimary
                val s = secondary?.rgb?.let { opaqueArgb(it) } ?: fallbackSecondary
                p to s
            } catch (_: Exception) {
                fallback
            }
        }
    }
    return state.value
}

/**
 * 本地音频文件的内嵌封面图（MediaMetadataRetriever.embeddedPicture）。
 * 非本地/无内嵌图返回 null。用于让背景模糊封面与小封面（通知栏/锁屏）保持一致，
 * 避免本地歌曲的背景只剩主题色、与真实封面"匹配不上"。
 */
@Composable
fun rememberEmbeddedArt(filePath: String?): Bitmap? {
    if (filePath.isNullOrBlank()) return null
    val state = produceState<Bitmap?>(initialValue = null, filePath) {
        value = withContext(Dispatchers.IO) {
            runCatching {
                val mmr = MediaMetadataRetriever()
                try {
                    mmr.setDataSource(filePath)
                    mmr.embeddedPicture?.let { BitmapFactory.decodeByteArray(it, 0, it.size) }
                } finally {
                    runCatching { mmr.release() }
                }
            }.getOrNull()
        }
    }
    return state.value
}
