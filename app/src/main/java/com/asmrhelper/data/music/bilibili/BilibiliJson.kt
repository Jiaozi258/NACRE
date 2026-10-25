package com.asmrhelper.data.music.bilibili

import com.asmrhelper.domain.model.music.RemotePlaylist
import com.asmrhelper.domain.model.music.RemoteSong
import org.json.JSONObject

/**
 * B站接口响应的纯解析（org.json，异常一律返回空/空串/0，不抛）。
 * 字段形状按公开 B站 Web API 已知结构 best-effort 解析。
 */
object BilibiliJson {

    private const val SOURCE_ID = "bilibili"

    /** 取 `data.wbi_img` 的 img_url / sub_url（用于计算 WBI mixinKey）。 */
    fun parseWbiKeys(json: JSONObject): Pair<String, String>? {
        val wbi = json.optJSONObject("data")?.optJSONObject("wbi_img") ?: return null
        val img = wbi.optString("img_url", "")
        val sub = wbi.optString("sub_url", "")
        return if (img.isBlank() || sub.isBlank()) null else img to sub
    }

    /** 当前登录用户昵称（`data.uname`，未登录为空）。 */
    fun parseNickname(json: JSONObject): String =
        json.optJSONObject("data")?.optString("uname", "") ?: ""

    /** 搜索 `data.result[]` → 视频。 */
    fun parseSearch(json: JSONObject): List<RemoteSong> {
        val arr = json.optJSONObject("data")?.optJSONArray("result") ?: return emptyList()
        val out = mutableListOf<RemoteSong>()
        for (i in 0 until arr.length()) {
            val o = arr.optJSONObject(i) ?: continue
            val bvid = o.optString("bvid", "")
            if (bvid.isBlank()) continue
            out.add(
                RemoteSong(
                    id = bvid,
                    title = o.optString("title", "").ifBlank { bvid },
                    artist = o.optString("author", ""),
                    durationMs = parseDurationMs(o),
                    coverUrl = https(o.optString("pic", "")),
                    sourceId = SOURCE_ID,
                    publishTime = o.optLong("pubdate", 0L) * 1000
                )
            )
        }
        return out
    }

    /** 热门 `data.list[]` → 视频。 */
    fun parsePopular(json: JSONObject): List<RemoteSong> {
        val arr = json.optJSONObject("data")?.optJSONArray("list") ?: return emptyList()
        val out = mutableListOf<RemoteSong>()
        for (i in 0 until arr.length()) {
            val o = arr.optJSONObject(i) ?: continue
            val bvid = o.optString("bvid", "")
            if (bvid.isBlank()) continue
            out.add(
                RemoteSong(
                    id = bvid,
                    title = o.optString("title", "").ifBlank { bvid },
                    artist = o.optJSONObject("owner")?.optString("name", "") ?: "",
                    durationMs = o.optLong("duration", 0L) * 1000,
                    coverUrl = https(o.optString("pic", "")),
                    sourceId = SOURCE_ID,
                    publishTime = o.optLong("pubdate", 0L) * 1000
                )
            )
        }
        return out
    }

    /** `x/player/wbi/playurl` 的音频流地址（优先 dash.audio，回退非 DASH 的 durl）。 */
    fun parsePlayUrl(json: JSONObject): String {
        val data = json.optJSONObject("data") ?: return ""
        // 优先 DASH 音频流（纯音频 m4s）
        val audioArr = data.optJSONObject("dash")?.optJSONArray("audio")
        if (audioArr != null && audioArr.length() > 0) {
            val first = audioArr.optJSONObject(0)
            var url = first?.optString("baseUrl", "") ?: ""
            if (url.isBlank()) {
                val backups = first?.optJSONArray("backupUrl")
                if (backups != null && backups.length() > 0) url = backups.optString(0, "")
            }
            if (url.isNotBlank()) return https(url)
        }
        // 回退：非 DASH 的 durl（音视频混流，ExoPlayer 会取其音频轨）
        val durl = data.optJSONArray("durl")
        val firstDurl = durl?.optJSONObject(0)
        var durlUrl = firstDurl?.optString("url", "") ?: ""
        if (durlUrl.isBlank()) {
            val backups = firstDurl?.optJSONArray("backup_url")
            if (backups != null && backups.length() > 0) durlUrl = backups.optString(0, "")
        }
        return https(durlUrl)
    }

    /** nav `data.mid`（当前登录用户 UID，未登录为 0）。 */
    fun parseUid(json: JSONObject): Long =
        json.optJSONObject("data")?.optLong("mid", 0L) ?: 0L

    /** 收藏夹列表 `data.list[]`（`x/v3/fav/folder/created/list-all`）→ RemotePlaylist。 */
    fun parseFavFolders(json: JSONObject): List<RemotePlaylist> {
        val arr = json.optJSONObject("data")?.optJSONArray("list") ?: return emptyList()
        val out = mutableListOf<RemotePlaylist>()
        for (i in 0 until arr.length()) {
            val o = arr.optJSONObject(i) ?: continue
            val id = o.optLong("id", -1L)
            if (id < 0) continue
            out.add(
                RemotePlaylist(
                    id = id.toString(),
                    name = o.optString("title", "").ifBlank { id.toString() },
                    coverUrl = https(o.optString("cover", "")).takeIf { it.isNotBlank() }
                )
            )
        }
        return out
    }

    /** 收藏夹内视频 `data.medias[]`（`x/v3/fav/resource/list`）→ RemoteSong。 */
    fun parseFavMedia(json: JSONObject): List<RemoteSong> {
        val arr = json.optJSONObject("data")?.optJSONArray("medias") ?: return emptyList()
        val out = mutableListOf<RemoteSong>()
        for (i in 0 until arr.length()) {
            val o = arr.optJSONObject(i) ?: continue
            val bvid = o.optString("bvid", "")
            if (bvid.isBlank()) continue
            out.add(
                RemoteSong(
                    id = bvid,
                    title = o.optString("title", "").ifBlank { bvid },
                    artist = o.optJSONObject("upper")?.optString("name", "") ?: "",
                    durationMs = o.optLong("duration", 0L) * 1000,
                    coverUrl = https(o.optString("cover", "")),
                    sourceId = SOURCE_ID,
                    publishTime = o.optLong("pubtime", 0L) * 1000
                )
            )
        }
        return out
    }

    /** 动态 feed `data.items[]`（`x/polymer/web-dynamic/v1/feed/all`，type=video）→ RemoteSong。 */
    fun parseDynamicFeed(json: JSONObject): List<RemoteSong> {
        val arr = json.optJSONObject("data")?.optJSONArray("items") ?: return emptyList()
        val out = mutableListOf<RemoteSong>()
        for (i in 0 until arr.length()) {
            val item = arr.optJSONObject(i) ?: continue
            val modules = item.optJSONObject("modules")
            val archive = modules?.optJSONObject("module_dynamic")?.optJSONObject("major")?.optJSONObject("archive")
                ?: continue
            val bvid = archive.optString("bvid", "")
            if (bvid.isBlank()) continue
            out.add(
                RemoteSong(
                    id = bvid,
                    title = archive.optString("title", "").ifBlank { bvid },
                    artist = modules?.optJSONObject("module_author")?.optString("name", "") ?: "",
                    durationMs = parseDurationText(archive.optString("duration_text", "")),
                    coverUrl = https(archive.optString("cover", "")),
                    sourceId = SOURCE_ID,
                    publishTime = (modules?.optJSONObject("module_author")?.optLong("pub_ts", 0L) ?: 0L) * 1000
                )
            )
        }
        return out
    }

    /** `x/web-interface/view` 的 cid（分 P 音频流需要）。 */
    fun parseViewCid(json: JSONObject): Long =
        json.optJSONObject("data")?.optLong("cid", -1L) ?: -1L

    /** `x/web-interface/view` → 单个视频详情。 */
    fun parseView(json: JSONObject): RemoteSong? {
        val data = json.optJSONObject("data") ?: return null
        val bvid = data.optString("bvid", "")
        if (bvid.isBlank()) return null
        return RemoteSong(
            id = bvid,
            title = data.optString("title", "").ifBlank { bvid },
            artist = data.optJSONObject("owner")?.optString("name", "") ?: "",
            durationMs = data.optLong("duration", 0L) * 1000,
            coverUrl = https(data.optString("pic", "")),
            sourceId = SOURCE_ID,
            publishTime = data.optLong("pubdate", 0L) * 1000
        )
    }

    fun parseCode(json: JSONObject): Int = json.optInt("code", -1)
    fun parseMessage(json: JSONObject): String = json.optString("message", "")

    /** 时长：搜索返回 "MM:SS"/"H:MM:SS" 字符串，热门/详情返回秒数。 */
    private fun parseDurationMs(o: JSONObject): Long {
        val s = o.optString("duration", "")
        if (s.isNotBlank() && s.contains(":")) {
            val parts = s.split(":").mapNotNull { it.toIntOrNull() }
            return when (parts.size) {
                3 -> (parts[0] * 3600L + parts[1] * 60L + parts[2]) * 1000
                2 -> (parts[0] * 60L + parts[1]) * 1000
                else -> 0L
            }
        }
        val secs = o.optLong("duration", 0L)
        return if (secs > 0) secs * 1000 else 0L
    }

    /** "MM:SS"/"H:MM:SS" 时长字符串 → 毫秒。 */
    private fun parseDurationText(s: String): Long {
        if (s.isBlank()) return 0L
        val parts = s.split(":").mapNotNull { it.toIntOrNull() }
        return when (parts.size) {
            3 -> (parts[0] * 3600L + parts[1] * 60L + parts[2]) * 1000
            2 -> (parts[0] * 60L + parts[1]) * 1000
            else -> 0L
        }
    }

    /** B站封面/流地址常返回 `//` 或 `http://`，统一转 `https://`。 */
    fun https(url: String): String = when {
        url.isBlank() -> ""
        url.startsWith("//") -> "https:$url"
        url.startsWith("http://") -> "https://" + url.removePrefix("http://")
        else -> url
    }
}
