package com.asmrhelper.data.music.youtube

import com.asmrhelper.domain.model.music.RemoteSong
import org.json.JSONArray
import org.json.JSONObject

/**
 * YouTube InnerTube 响应的纯解析（org.json，异常一律返回空/空串/0）。
 * 搜索与趋势都包裹在 `videoRenderer` 里，故用递归收集统一解析。
 */
object YoutubeJson {

    private const val SOURCE_ID = "youtube"

    /** 搜索 → 视频列表。 */
    fun parseSearch(json: JSONObject): List<RemoteSong> = parseVideos(json)

    /** 趋势/首页 → 视频列表。 */
    fun parseTrending(json: JSONObject): List<RemoteSong> = parseVideos(json)

    /** 递归收集所有 `videoRenderer`（结构通用，搜索/趋势共用）。 */
    private fun parseVideos(json: JSONObject): List<RemoteSong> {
        val out = mutableListOf<RemoteSong>()
        collectVideoRenderers(json) { v ->
            val videoId = v.optString("videoId", "")
            if (videoId.isNotBlank()) {
                out.add(
                    RemoteSong(
                        id = videoId,
                        title = titleText(v).ifBlank { videoId },
                        artist = runText(v.optJSONObject("ownerText")),
                        durationMs = parseLength(v.optJSONObject("lengthText")),
                        coverUrl = lastThumb(v.optJSONObject("thumbnail")),
                        sourceId = SOURCE_ID,
                        publishTime = 0L
                    )
                )
            }
        }
        return out
    }

    /** player → 音频流 URL（adaptiveFormats 优先 itag 251/140/139）。 */
    fun parsePlayUrl(json: JSONObject): String {
        val formats = json.optJSONObject("streamingData")?.optJSONArray("adaptiveFormats") ?: return ""
        for (target in listOf("251", "140", "139")) {
            for (i in 0 until formats.length()) {
                val f = formats.optJSONObject(i) ?: continue
                if (f.optInt("itag", -1).toString() == target) {
                    val url = f.optString("url", "")
                    if (url.isNotBlank()) return url
                }
            }
        }
        return ""
    }

    /** player → 单个视频详情。 */
    fun parsePlayer(json: JSONObject): RemoteSong? {
        val vd = json.optJSONObject("videoDetails") ?: return null
        val videoId = vd.optString("videoId", "")
        if (videoId.isBlank()) return null
        return RemoteSong(
            id = videoId,
            title = vd.optString("title", "").ifBlank { videoId },
            artist = vd.optString("author", ""),
            durationMs = vd.optLong("lengthSeconds", 0L) * 1000,
            coverUrl = lastThumb(vd.optJSONObject("thumbnail")),
            sourceId = SOURCE_ID,
            publishTime = 0L
        )
    }

    // ── 字段解析 ──────────────────────────────────────────

    private fun titleText(v: JSONObject): String {
        val runs = v.optJSONObject("title")?.optJSONArray("runs")
        if (runs != null && runs.length() > 0) {
            val t = runs.optJSONObject(0)?.optString("text", "")
            if (t != null && t.isNotBlank()) return t
        }
        return v.optJSONObject("title")?.optString("simpleText", "") ?: ""
    }

    private fun runText(o: JSONObject?): String {
        if (o == null) return ""
        val runs = o.optJSONArray("runs")
        if (runs != null && runs.length() > 0) {
            val t = runs.optJSONObject(0)?.optString("text", "")
            if (t != null && t.isNotBlank()) return t
        }
        return o.optString("simpleText", "")
    }

    private fun lastThumb(o: JSONObject?): String {
        val arr = o?.optJSONArray("thumbnails") ?: return ""
        val n = arr.length()
        if (n == 0) return ""
        return arr.optJSONObject(n - 1)?.optString("url", "") ?: ""
    }

    /** 时长 `lengthText.simpleText`："MM:SS" 或 "H:MM:SS"。 */
    private fun parseLength(o: JSONObject?): Long {
        val s = o?.optString("simpleText", "") ?: return 0L
        if (s.isBlank()) return 0L
        val parts = s.split(":").mapNotNull { it.toIntOrNull() }
        return when (parts.size) {
            3 -> (parts[0] * 3600L + parts[1] * 60L + parts[2]) * 1000
            2 -> (parts[0] * 60L + parts[1]) * 1000
            else -> 0L
        }
    }

    private fun collectVideoRenderers(json: JSONObject, onVideo: (JSONObject) -> Unit) {
        fun walk(o: Any) {
            when (o) {
                is JSONObject -> {
                    val vr = o.optJSONObject("videoRenderer")
                    if (vr != null && vr.has("videoId")) onVideo(vr)
                    val keys = o.keys()
                    while (keys.hasNext()) walk(o.get(keys.next()))
                }
                is JSONArray -> for (i in 0 until o.length()) walk(o.get(i))
            }
        }
        walk(json)
    }
}
