package com.asmrhelper.data.music.youtube

import android.content.Context
import com.asmrhelper.R
import com.asmrhelper.domain.model.music.RemoteChart
import com.asmrhelper.domain.model.music.RemoteLyric
import com.asmrhelper.domain.model.music.RemotePlaylist
import com.asmrhelper.domain.model.music.RemoteSong
import com.asmrhelper.domain.music.MusicSource
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import org.json.JSONObject

/**
 * YouTube 视频音频源（InnerTube ANDROID client，匿名）。把「视频」映射为
 * [RemoteSong]（title=视频名、artist=上传者、coverUrl=封面、durationMs=时长、
 * id=videoId）。登录后续再加。
 */
class YoutubeMusicSource(
    private val client: OkHttpClient,
    private val context: Context
) : MusicSource {

    override val id: String = "youtube"
    override val displayNameRes: Int = R.string.source_youtube
    override val isLoggedIn: StateFlow<Boolean> = MutableStateFlow(false)

    override fun search(keyword: String, page: Int): Flow<List<RemoteSong>> = flow {
        emit(YoutubeJson.parseSearch(post("search", YoutubeInnerTube.searchBody(keyword))))
    }

    /** 趋势（`FEtrending`）作为「推荐」。 */
    override fun getRecommendations(): Flow<List<RemoteSong>> = flow {
        emit(YoutubeJson.parseTrending(post("browse", YoutubeInnerTube.trendingBody())))
    }

    /** 趋势也挂到排行榜 tab。 */
    override fun getCharts(): Flow<List<RemoteChart>> = flow {
        val videos = YoutubeJson.parseTrending(post("browse", YoutubeInnerTube.trendingBody()))
        emit(
            if (videos.isEmpty()) emptyList()
            else listOf(RemoteChart("trending", context.getString(R.string.youtube_trending), videos))
        )
    }

    override fun getSongUrl(songId: String, quality: String): Flow<String> = flow {
        emit(YoutubeJson.parsePlayUrl(post("player", YoutubeInnerTube.playerBody(songId))))
    }

    override fun getLyric(songId: String): Flow<RemoteLyric> = flowOf(RemoteLyric())

    override fun getCoverUrl(songId: String): Flow<String> = flow {
        emit(YoutubeJson.parsePlayer(post("player", YoutubeInnerTube.playerBody(songId)))?.coverUrl ?: "")
    }

    override fun getSongDetail(songId: String): Flow<RemoteSong> = flow {
        YoutubeJson.parsePlayer(post("player", YoutubeInnerTube.playerBody(songId)))?.let { emit(it) }
    }

    override fun getPlaylist(playlistId: String): Flow<RemotePlaylist> = flowOf(RemotePlaylist(playlistId, ""))

    override fun loginAccount(username: String, password: String): Flow<Result<Unit>> = flow {
        emit(Result.failure(IllegalStateException(context.getString(R.string.login_youtube_not_supported))))
    }

    override fun importLogin(cookie: String, nickname: String) {}
    override fun logout() {}

    private suspend fun post(action: String, body: JSONObject): JSONObject =
        withContext(Dispatchers.IO) { YoutubeInnerTube.post(client, action, body) }
}
