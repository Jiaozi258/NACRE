package com.asmrhelper.data.music.bilibili

import android.content.Context
import com.asmrhelper.R
import com.asmrhelper.domain.model.music.RemoteArtist
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
    override val requiresLogin: Boolean = false
    override val webLogin: WebLoginConfig =
        WebLoginConfig("https://passport.bilibili.com/login", "https://www.bilibili.com", setOf("SESSDATA"))

    /** WBI mixinKey 内存缓存（nav 拉取一次，失败下次重试）。 */
    @Volatile private var cachedMixinKey: String? = null

    /** B站风控指纹 Cookie（服务端下发，持久化复用，避免反复换指纹被风控）。 */
    private val biliPrefs by lazy { context.getSharedPreferences("bili_anti_risk", Context.MODE_PRIVATE) }
    @Volatile private var cachedFingerprint: Map<String, String>? = loadFingerprint()

    override fun search(keyword: String, page: Int): Flow<List<RemoteSong>> = flow {
        val params = mapOf(
            "search_type" to "video",
            "keyword" to keyword,
            "page" to page.toString(),
            "page_size" to "30"
        )
        emit(BilibiliJson.parseSearch(wbiGet("x/web-interface/wbi/search/type", params)))
    }

    /** 搜索博主/UP主（`search_type=bili_user`）。 */
    override fun searchArtists(keyword: String): Flow<List<RemoteArtist>> = flow {
        val params = mapOf(
            "search_type" to "bili_user",
            "keyword" to keyword,
            "page" to "1"
        )
        emit(BilibiliJson.parseUserSearch(wbiGet("x/web-interface/wbi/search/type", params)))
    }

    /** 热门视频（`x/web-interface/popular`）作为「热门」。拉 50 条做池子随机抽 10 条，刷新即随机重新分配。 */
    override fun getRecommendations(): Flow<List<RemoteSong>> = flow {
        val pool = BilibiliJson.parsePopular(get("x/web-interface/popular", "ps=50&pn=1"))
        emit(pool.shuffled().take(10))
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
        // fnval 只保留 DASH(16) + Dolby(256) = 272：匿名请求带 4048（8K/Dolby Vision/Hi-Res 等
        // 大会员专用格式）会被 B站风控拦成 HTTP 412，与 NeriPlayer 取流实现保持一致。
        val params = mapOf(
            "bvid" to songId,
            "cid" to cid.toString(),
            "fnval" to "272",
            "fnver" to "0",
            "fourk" to "0",
            "otype" to "json",
            "platform" to "pc"
        )
        emit(BilibiliJson.parsePlayUrl(wbiGet("x/player/wbi/playurl", params)))
    }

    override fun getLyric(songId: String): Flow<RemoteLyric> = flowOf(RemoteLyric())

    override fun getCoverUrl(songId: String): Flow<String> = flow {
        emit(BilibiliJson.parseView(wbiGet("x/web-interface/wbi/view", mapOf("bvid" to songId)))?.coverUrl ?: "")
    }

    override fun getSongDetail(songId: String): Flow<RemoteSong> = flow {
        BilibiliJson.parseView(wbiGet("x/web-interface/wbi/view", mapOf("bvid" to songId)))?.let { emit(it) }
    }

    override fun getPlaylist(playlistId: String): Flow<RemotePlaylist> = flow {
        // 收藏夹（media_id）内视频；普通歌单/榜单无此概念，返回空。
        // 收藏夹接口已强制要求 WBI 签名，否则返回 -352 风控、打不开。
        val songs = BilibiliJson.parseFavMedia(
            wbiGet("x/v3/fav/resource/list", mapOf("media_id" to playlistId, "pn" to "1", "ps" to "50"))
        )
        emit(RemotePlaylist(playlistId, "", songs = songs))
    }

    /** 当前登录用户收藏夹列表（需登录）；匿名/失败返回空。 */
    override fun getUserPlaylists(): Flow<List<RemotePlaylist>> = flow {
        val mid = resolveUid()
        if (mid <= 0) {
            emit(emptyList())
            return@flow
        }
        // 收藏夹列表接口同样需要 WBI 签名，否则返回 -352 风控、列表为空。
        emit(BilibiliJson.parseFavFolders(wbiGet("x/v3/fav/folder/created/list-all", mapOf("up_mid" to mid.toString()))))
    }

    /** 动态（feed）视频（需登录）；匿名/失败返回空。最多展示 25 条。 */
    override fun getRadio(): Flow<List<RemoteSong>> = flow {
        emit(BilibiliJson.parseDynamicFeed(get("x/polymer/web-dynamic/v1/feed/all", "type=video&page=1")).take(25))
    }

    /** 每日推荐：B站无对应匿名接口，复用热门视频作为兜底。 */
    override fun getDailyRecommendSongs(): Flow<List<RemoteSong>> = flow {
        emit(BilibiliJson.parsePopular(get("x/web-interface/popular", "ps=20&pn=1")))
    }

    /** UP主（博主）的视频列表：空间视频 `x/space/wbi/arc/search`。 */
    override fun getArtistSongs(artistId: String): Flow<List<RemoteSong>> = flow {
        val params = mapOf(
            "mid" to artistId,
            "ps" to "30",
            "pn" to "1",
            "order" to "pubdate"
        )
        emit(BilibiliJson.parseSpaceVideos(wbiGet("x/space/wbi/arc/search", params)))
    }

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

    /** WBI 签名 GET。失败（空结果或业务码非 0，常见 412/风控）时刷新 mixinKey 与
     *  指纹后重试一次：WBI key 会周期性轮换、buvid 也可能被标记，重试能自愈大部分情况。 */
    private suspend fun wbiGet(path: String, params: Map<String, String>): JSONObject {
        var mixin = mixinKey() ?: return JSONObject()
        var result = get(path, BilibiliWbi.sign(mixin, params))
        if (result.length() == 0 || result.optInt("code", -1) != 0) {
            cachedMixinKey = null
            rotateFingerprint()
            mixin = mixinKey() ?: return result
            result = get(path, BilibiliWbi.sign(mixin, params))
        }
        return result
    }

    private suspend fun get(path: String, query: String): JSONObject = withContext(Dispatchers.IO) {
        try {
            val url = if (query.isBlank()) "$BASE/$path" else "$BASE/$path?$query"
            val builder = Request.Builder()
                .url(url)
                .header("User-Agent", BilibiliFingerprint.WEB_UA)
                .header("Referer", refererFor(query))
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

    /** 取分 P cid：必须走 WBI 签名版 `x/web-interface/wbi/view`。无签名版
     *  `x/web-interface/view` 匿名会被风控拦成 HTML（非 JSON），导致 cid 拿不到。 */
    private suspend fun fetchViewCid(bvid: String): Long =
        BilibiliJson.parseViewCid(wbiGet("x/web-interface/wbi/view", mapOf("bvid" to bvid)))

    /** 当前登录用户 UID（nav `data.mid`），未登录返回 0。 */
    private suspend fun resolveUid(): Long =
        BilibiliJson.parseUid(get("x/web-interface/nav", ""))

    private fun refererFor(query: String): String = "https://www.bilibili.com"

    /** Cookie：风控指纹 + 登录 SESSDATA。 */
    private fun buildCookie(): String {
        val bili = fingerprintCookie()
        val login = cookie.trim()
        return when {
            bili.isBlank() -> login
            login.isBlank() -> bili
            else -> "$bili; $login"
        }
    }

    /** 返回 B站风控指纹 Cookie 头（缓存优先，为空才走 finger/spi 拉取）。 */
    private fun fingerprintCookie(): String {
        val fp = cachedFingerprint ?: fetchAndCacheFingerprint()
        return if (fp.isNullOrEmpty()) "" else BilibiliFingerprint.toCookieHeader(fp)
    }

    private fun loadFingerprint(): Map<String, String>? {
        val fp = linkedMapOf<String, String>()
        for (k in BilibiliFingerprint.FINGERPRINT_KEYS) {
            val v = biliPrefs.getString(k, null)
            if (v.isNullOrBlank()) return null
            fp[k] = v
        }
        // 注意：服务端下发的真实 buvid3 本身就以 "infoc" 结尾（如 `72E4...infoc`），
        // 不能再按结尾 "infoc" 判定为「旧版本地假指纹」，否则每次启动都会误删真指纹、
        // 反复换指纹反而触发风控。缺字段才返回 null 触发重取。
        return fp
    }

    private fun fetchAndCacheFingerprint(): Map<String, String> {
        val fetched = BilibiliFingerprint.fetch()
        if (fetched.isNotEmpty()) {
            cachedFingerprint = fetched
            val editor = biliPrefs.edit()
            fetched.forEach { (k, v) -> editor.putString(k, v) }
            editor.apply()
        }
        return fetched
    }

    /** 被风控/412 拦截后轮换一次指纹，下次请求用新指纹自愈。 */
    private fun rotateFingerprint() {
        cachedFingerprint = null
        val editor = biliPrefs.edit()
        BilibiliFingerprint.FINGERPRINT_KEYS.forEach { editor.remove(it) }
        editor.apply()
    }

    companion object {
        private const val BASE = "https://api.bilibili.com"
    }
}
