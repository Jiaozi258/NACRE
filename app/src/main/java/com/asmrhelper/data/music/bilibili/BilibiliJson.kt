package com.asmrhelper.data.music.bilibili

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

    /** `x/player/wbi/playurl` 的音频流地址（dash.audio 首个 baseUrl/backupUrl）。 */
    fun parsePlayUrl(json: JSONObject): String {
        val audioArr = json.optJSONObject("data")?.optJSONObject("dash")?.optJSONArray("audio")
            ?: return ""
        val first = audioArr.optJSONObject(0) ?: return ""
        var url = first.optString("baseUrl", "")
        if (url.isBlank()) {
            val backups = first.optJSONArray("backupUrl")
            if (backups != null && backups.length() > 0) url = backups.optString(0, "")
        }
        return https(url)
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

    /** B站封面/流地址常返回 `//` 或 `http://`，统一转 `https://`。 */
    fun https(url: String): String = when {
        url.isBlank() -> ""
        url.startsWith("//") -> "https:$url"
        url.startsWith("http://") -> "https://" + url.removePrefix("http://")
        else -> url
    }
}
