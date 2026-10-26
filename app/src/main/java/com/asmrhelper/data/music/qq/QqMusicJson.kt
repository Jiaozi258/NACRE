package com.asmrhelper.data.music.qq

import com.asmrhelper.domain.model.music.RemoteAlbum
import com.asmrhelper.domain.model.music.RemoteChart
import com.asmrhelper.domain.model.music.RemoteLyric
import com.asmrhelper.domain.model.music.RemoteLyricLine
import com.asmrhelper.domain.model.music.RemoteSong
import java.util.Base64
import org.json.JSONArray
import org.json.JSONObject

/** QQ 音乐接口响应的纯解析（org.json，异常一律返回空/空串，不抛）。 */
object QqMusicJson {

    private val LYRIC_TIME = Regex("""\[(\d+):(\d+(?:\.\d+)?)\]""")

    fun parseSearch(json: String): List<RemoteSong> = try {
        val items = JSONObject(json).optJSONObject("req_0")?.optJSONObject("data")
            ?.optJSONObject("body")?.optJSONObject("item_song")?.optJSONArray("items")
            ?: return emptyList()
        val songs = mutableListOf<RemoteSong>()
        for (i in 0 until items.length()) {
            val s = items.optJSONObject(i) ?: continue
            songs.add(parseSong(s))
        }
        songs
    } catch (_: Exception) {
        emptyList()
    }

    fun parseSongUrl(json: String): String = try {
        val data = JSONObject(json).optJSONObject("req_0")?.optJSONObject("data") ?: return ""
        val info = data.optJSONArray("midurlinfo")?.optJSONObject(0) ?: return ""
        val purl = info.optString("purl")
        val sip = data.optJSONArray("sip")
        if (info.optInt("result") == 0 && purl.isNotBlank() && sip != null && sip.length() > 0) {
            https(sip.optString(0) + purl)
        } else {
            ""
        }
    } catch (_: Exception) {
        ""
    }

    /** 搜索专辑（`do_search_v2` search_type=2 → body.item_album.items[]，字段做多重兼容）。 */
    fun parseAlbumSearch(json: String): List<RemoteAlbum> = try {
        val body = JSONObject(json).optJSONObject("req_0")?.optJSONObject("data")
            ?.optJSONObject("body") ?: return emptyList()
        val items = body.optJSONObject("item_album")?.optJSONArray("items")
            ?: body.optJSONObject("album")?.optJSONArray("items")
            ?: body.optJSONArray("album")
            ?: return emptyList()
        val list = mutableListOf<RemoteAlbum>()
        for (i in 0 until items.length()) {
            val a = items.optJSONObject(i) ?: continue
            val mid = a.optString("mid").ifBlank { a.optString("albumMID").ifBlank { a.optString("album_mid") } }
            if (mid.isBlank()) continue
            val name = a.optString("name").ifBlank { a.optString("albumName").ifBlank { a.optString("album_name") } }
            val artist = a.optString("singerName").ifBlank {
                a.optJSONArray("singer")?.let { arr ->
                    (0 until arr.length())
                        .mapNotNull { j -> arr.optJSONObject(j)?.optString("name")?.takeIf { it.isNotBlank() } }
                        .joinToString(" / ")
                } ?: ""
            }
            val cover = a.optString("pic").ifBlank { a.optString("picUrl").ifBlank { a.optString("albumPic") } }
                .ifBlank { "https://y.gtimg.cn/music/photo_new/T002R300x300M000$mid.jpg" }
            list.add(RemoteAlbum(id = mid, name = name, artist = artist, coverUrl = cover))
        }
        list
    } catch (_: Exception) {
        emptyList()
    }

    /** 专辑歌曲（`music.musichallAlbum.AlbumInfoServer.GetAlbumDetail`，多个可能路径）。 */
    fun parseAlbumSongs(json: String): List<RemoteSong> = try {
        val data = JSONObject(json).optJSONObject("req_0")?.optJSONObject("data") ?: return emptyList()
        val arr = data.optJSONArray("songList")
            ?: data.optJSONObject("data")?.optJSONArray("songList")
            ?: data.optJSONObject("data")?.optJSONArray("getSongList")
            ?: data.optJSONObject("getAlbumDetail")?.optJSONObject("data")?.optJSONArray("songList")
            ?: data.optJSONObject("getAlbumDetail")?.optJSONObject("data")?.optJSONArray("getSongList")
            ?: return emptyList()
        val songs = mutableListOf<RemoteSong>()
        for (i in 0 until arr.length()) {
            arr.optJSONObject(i)?.let { songs.add(parseSong(it)) }
        }
        songs
    } catch (_: Exception) {
        emptyList()
    }


    /** Android 默认禁明文 HTTP；QQ 流地址返回 `http://`（CDN 支持 https），统一转 https。 */
    private fun https(url: String): String =
        if (url.startsWith("http://")) "https://" + url.substring(7) else url

    fun parseLyric(json: String): RemoteLyric = try {
        val lyric = JSONObject(json).optJSONObject("req_0")?.optJSONObject("data")
            ?.optString("lyric") ?: return RemoteLyric()
        val raw = String(Base64.getDecoder().decode(lyric), Charsets.UTF_8)
        val lines = mutableListOf<RemoteLyricLine>()
        for (line in raw.split('\n')) {
            val m = LYRIC_TIME.find(line) ?: continue
            val min = m.groupValues[1].toLong()
            val sec = m.groupValues[2].toDouble()
            val timeMs = min * 60_000L + (sec * 1000).toLong()
            val text = line.substring(m.range.last + 1).trim()
            if (text.isNotEmpty()) lines.add(RemoteLyricLine(timeMs, text))
        }
        RemoteLyric(lines = lines, rawText = raw)
    } catch (_: Exception) {
        RemoteLyric()
    }

    // ── 内部 ────────────────────────────────────────────

    /** 榜单：`musicToplist.ToplistInfo.GetAll` → `req_0.data.group[].toplist[]`。 */
    fun parseCharts(json: String): List<RemoteChart> = try {
        val groups = JSONObject(json).optJSONObject("req_0")?.optJSONObject("data")
            ?.optJSONArray("group") ?: return emptyList()
        val charts = mutableListOf<RemoteChart>()
        for (i in 0 until groups.length()) {
            val toplists = groups.optJSONObject(i)?.optJSONArray("toplist") ?: continue
            for (j in 0 until toplists.length()) {
                val t = toplists.optJSONObject(j) ?: continue
                val songs = mutableListOf<RemoteSong>()
                t.optJSONArray("songList")?.let { list ->
                    for (k in 0 until list.length()) {
                        list.optJSONObject(k)?.let { songs.add(parseSong(it)) }
                    }
                }
                charts.add(RemoteChart(id = t.optString("topId"), name = t.optString("title"), songs = songs))
            }
        }
        charts
    } catch (_: Exception) {
        emptyList()
    }

    /** 推荐：`req_0.data.songlist[]`（登录接口形状 best-effort，异常/缺失返回空）。 */
    fun parseRecommendations(json: String): List<RemoteSong> = try {
        val items = JSONObject(json).optJSONObject("req_0")?.optJSONObject("data")
            ?.optJSONArray("songlist") ?: return emptyList()
        val songs = mutableListOf<RemoteSong>()
        for (i in 0 until items.length()) {
            items.optJSONObject(i)?.let { songs.add(parseSong(it)) }
        }
        songs
    } catch (_: Exception) {
        emptyList()
    }

    /** 从 QQ Cookie 提取纯数字 uin（`uin=o<号码>` 去 `o` 前缀），无则空串。 */
    fun uin(cookie: String): String {
        val m = Regex("""(?:^|;\s*)uin=([^;]+)""").find(cookie) ?: return ""
        return m.groupValues[1].trim().removePrefix("o")
    }

    private fun parseSong(s: JSONObject): RemoteSong {
        val artists = s.optJSONArray("singer")?.let { a ->
            val names = mutableListOf<String>()
            for (j in 0 until a.length()) {
                a.optJSONObject(j)?.optString("name")?.takeIf { it.isNotBlank() }?.let(names::add)
            }
            names.joinToString(" / ")
        } ?: ""
        val album = s.optJSONObject("album")
        val albumMid = album?.optString("mid") ?: ""
        val cover = albumMid.takeIf { it.isNotBlank() }
            ?.let { "https://y.gtimg.cn/music/photo_new/T002R300x300M000$it.jpg" }
        return RemoteSong(
            id = s.optString("mid").ifBlank { s.optString("songMid") },
            title = s.optString("title").ifBlank { s.optString("name").ifBlank { s.optString("songName") } },
            artist = artists,
            album = album?.optString("name") ?: "",
            durationMs = s.optLong("interval", 0L) * 1000L,
            coverUrl = cover,
            sourceId = "qq"
        )
    }
}
