package com.asmrhelper.data.music.bilibili

import android.content.Context
import com.asmrhelper.R
import com.asmrhelper.domain.model.music.RemoteChart
import com.asmrhelper.domain.model.music.RemoteLyric
import com.asmrhelper.domain.model.music.RemotePlaylist
import com.asmrhelper.domain.model.music.RemoteSong
import com.asmrhelper.domain.music.MusicSource
import com.asmrhelper.domain.music.WebLoginConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject

/**
 * B站视频音频源。把「视频」映射为 [RemoteSong]（title=视频名、artist=上传者、
 * coverUrl=封面、publishTime=发布时间、id=bvid）。搜索/取流走 WBI 签名接口。
 * 登录走 WebView 登录页（扫码 + 账号密码都在网页内），鉴权键 `SESSDATA`。
 */
class BilibiliMusicSource(
    private val client: OkHttpClient,
    private val context: Context,
    persistedCookie: String = "",
    persistedNickname: String = "",
    private val onLoginChanged: (cookie: String, nickname: String, loginTime: Long) -> Unit = { _, _, _ -> }
) : MusicSource {

    override val id: String = "bilibili"
    override val displayNameRes: Int = R.string.source_bilibili

    private var cookie: String = persistedCookie
    private var nickname: String = persistedNickname

    private val _isLoggedIn = MutableStateFlow(cookie.contains("SESSDATA"))
    override val isLoggedIn: StateFlow<Boolean> = _isLoggedIn.asStateFlow()

    override val supportsAccountLogin: Boolean = false
    override val webLogin: WebLoginConfig =
        WebLoginConfig("https://passport.bilibili.com/login", "https://www.bilibili.com", setOf("SESSDATA"))

    /** WBI mixinKey 内存缓存（nav 拉取一次，失败下次重试）。 */
    @Volatile private var cachedMixinKey: String? = null

    /** B站风控 buvid3/buvid4（与视频链接提取共用同一指纹，避免反复换指纹被风控）。 */
    private val biliPrefs by lazy { context.getSharedPreferences("bili_anti_risk", Context.MODE_PRIVATE) }
    @Volatile private var cachedBuvid3: String? = biliPrefs.getString("buvid3", null)
    @Volatile private var cachedBuvid4: String? = biliPrefs.getString("buvid4", null)

    override fun search(keyword: String, page: Int): Flow<List<RemoteSong>> = flow {
        val params = mapOf(
            "search_type" to "video",
            "keyword" to keyword,
            "page" to page.toString(),
            "page_size" to "30"
        )
        emit(BilibiliJson.parseSearch(wbiGet("x/web-interface/wbi/search/type", params)))
    }

    /** 热门视频（`x/web-interface/popular`）作为「推荐」。 */
    override fun getRecommendations(): Flow<List<RemoteSong>> = flow {
        emit(BilibiliJson.parsePopular(get("x/web-interface/popular", "ps=20&pn=1")))
    }

    /** 热门视频也挂到排行榜 tab（单一「热门」榜）。 */
    override fun getCharts(): Flow<List<RemoteChart>> = flow {
        val videos = BilibiliJson.parsePopular(get("x/web-interface/popular", "ps=20&pn=1"))
        emit(
            if (videos.isEmpty()) emptyList()
            else listOf(RemoteChart(id = "popular", name = context.getString(R.string.bili_popular), songs = videos))
        )
    }

    override fun getSongUrl(songId: String, quality: String): Flow<String> = flow {
        val cid = fetchViewCid(songId)
        if (cid < 0) {
            emit("")
            return@flow
        }
        val params = mapOf(
            "bvid" to songId,
            "cid" to cid.toString(),
            "fnval" to "16",
            "fnver" to "0",
            "fourk" to "1",
            "qn" to "0"
        )
        emit(BilibiliJson.parsePlayUrl(wbiGet("x/player/wbi/playurl", params)))
    }

    override fun getLyric(songId: String): Flow<RemoteLyric> = flowOf(RemoteLyric())

    override fun getCoverUrl(songId: String): Flow<String> = flow {
        emit(BilibiliJson.parseView(get("x/web-interface/view", "bvid=$songId"))?.coverUrl ?: "")
    }

    override fun getSongDetail(songId: String): Flow<RemoteSong> = flow {
        BilibiliJson.parseView(get("x/web-interface/view", "bvid=$songId"))?.let { emit(it) }
    }

    override fun getPlaylist(playlistId: String): Flow<RemotePlaylist> = flowOf(RemotePlaylist(playlistId, ""))

    // ── 登录 ───────────────────────────────────────────

    override fun loginAccount(username: String, password: String): Flow<Result<Unit>> = flow {
        emit(Result.failure(IllegalStateException(context.getString(R.string.login_bili_use_web))))
    }

    override fun importLogin(cookie: String, nickname: String) {
        this.cookie = cookie
        this.nickname = nickname
        _isLoggedIn.value = cookie.contains("SESSDATA")
        onLoginChanged(cookie, nickname, System.currentTimeMillis())
    }

    /** 网页登录无昵称：nav 返回 `data.uname` 按需拉取。 */
    override suspend fun resolveAccountNickname(): String {
        if (nickname.isNotBlank()) return nickname
        val n = BilibiliJson.parseNickname(get("x/web-interface/nav", ""))
        if (n.isBlank()) return ""
        nickname = n
        onLoginChanged(cookie, n, System.currentTimeMillis())
        return n
    }

    override fun logout() {
        cookie = ""
        nickname = ""
        _isLoggedIn.value = false
        onLoginChanged("", "", 0L)
    }

    // ── HTTP ───────────────────────────────────────────

    /** WBI 签名 GET。 */
    private suspend fun wbiGet(path: String, params: Map<String, String>): JSONObject {
        val mixin = mixinKey() ?: return JSONObject()
        return get(path, BilibiliWbi.sign(mixin, params))
    }

    private suspend fun get(path: String, query: String): JSONObject = withContext(Dispatchers.IO) {
        try {
            val url = if (query.isBlank()) "$BASE/$path" else "$BASE/$path?$query"
            val builder = Request.Builder()
                .url(url)
                .header("User-Agent", PC_UA)
                .header("Referer", refererFor(query))
                .header("X-Bili-Api-Source", "pc")
                .header("X-Requested-With", "XMLHttpRequest")
            val ck = buildCookie()
            if (ck.isNotBlank()) builder.header("Cookie", ck)
            client.newCall(builder.build()).execute().use { resp ->
                if (resp.isSuccessful) JSONObject(resp.body?.string() ?: "{}") else JSONObject()
            }
        } catch (_: Exception) {
            JSONObject()
        }
    }

    /** 拉取 mixinKey（nav），失败返回 null（下次重试）。 */
    private suspend fun mixinKey(): String? {
        cachedMixinKey?.let { return it }
        val keys = BilibiliJson.parseWbiKeys(get("x/web-interface/nav", "")) ?: return null
        return BilibiliWbi.mixinKey(keys.first, keys.second).also { cachedMixinKey = it }
    }

    private suspend fun fetchViewCid(bvid: String): Long =
        BilibiliJson.parseViewCid(get("x/web-interface/view", "bvid=$bvid"))

    private fun refererFor(query: String): String {
        val bvid = Regex("bvid=([a-zA-Z0-9]+)").find(query)?.groupValues?.get(1)
        return if (bvid != null) "https://www.bilibili.com/video/$bvid/" else "https://www.bilibili.com"
    }

    /** Cookie：风控 buvid + 登录 SESSDATA。 */
    private fun buildCookie(): String {
        val bili = buvidCookie()
        val login = cookie.trim()
        return when {
            bili.isBlank() -> login
            login.isBlank() -> bili
            else -> "$bili; $login"
        }
    }

    private fun buvidCookie(): String {
        var b3 = cachedBuvid3
        var b4 = cachedBuvid4
        if (b3.isNullOrEmpty()) {
            val (nb3, nb4) = obtainBuvid()
            b3 = nb3; b4 = nb4
            cachedBuvid3 = b3; cachedBuvid4 = b4
            biliPrefs.edit().putString("buvid3", b3).putString("buvid4", b4).apply()
        }
        if (b3.isNullOrEmpty()) return ""
        val nut = System.currentTimeMillis() / 1000
        return buildString {
            append("buvid3=").append(b3)
            if (!b4.isNullOrEmpty()) append("; buvid4=").append(b4)
            append("; b_nut=").append(nut)
        }
    }

    private fun obtainBuvid(): Pair<String?, String?> {
        return try {
            val req = Request.Builder()
                .url("$BASE/x/frontend/finger/spi")
                .header("User-Agent", PC_UA)
                .header("Referer", "https://www.bilibili.com")
                .get().build()
            client.newCall(req).execute().use { resp ->
                if (!resp.isSuccessful) return null to null
                val data = JSONObject(resp.body?.string() ?: "{}").optJSONObject("data")
                data?.optString("b_3", "")?.takeIf { it.isNotEmpty() } to
                    data?.optString("b_4", "")?.takeIf { it.isNotEmpty() }
            }
        } catch (_: Exception) {
            null to null
        }
    }

    companion object {
        private const val BASE = "https://api.bilibili.com"
        private const val PC_UA =
            "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36"
    }
}
