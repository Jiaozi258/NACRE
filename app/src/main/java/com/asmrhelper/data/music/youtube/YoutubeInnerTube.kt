package com.asmrhelper.data.music.youtube

import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject

/**
 * YouTube InnerTube 客户端（ANDROID 端 context）。
 * ANDROID client 返回的 adaptiveFormats 音频 URL 是直链（itag 140/251 无需 decipher），
 * 因此不依赖 Invidious/Piped 代理。搜索/趋势/播放均匿名。
 */
object YoutubeInnerTube {

    private const val BASE = "https://www.youtube.com/youtubei/v1"
    // ANDROID 客户端公开 key（内置于官方 Android app，公开可查）。
    private const val API_KEY = "AIzaSyA8eiZmM1FaDVjRy-df2KTyQ_vz_yYM39w"
    private const val CLIENT_VERSION = "19.09.37"
    private const val ANDROID_UA =
        "com.google.android.youtube/$CLIENT_VERSION (Linux; U; Android 11) gzip"

    private val JSON_MEDIA = "application/json; charset=utf-8".toMediaType()

    private fun androidContext(): JSONObject = JSONObject().apply {
        put("client", JSONObject().apply {
            put("clientName", "ANDROID")
            put("clientVersion", CLIENT_VERSION)
            put("androidSdkVersion", 30)
            put("hl", "en")
            put("gl", "US")
        })
    }

    /** 搜索视频（`params` = 只搜视频过滤）。 */
    fun searchBody(query: String): JSONObject = JSONObject().apply {
        put("context", androidContext())
        put("query", query)
        put("params", "EgIQAQ%3D%3D")
    }

    /** 取单个视频的 streamingData（含音频流）。 */
    fun playerBody(videoId: String): JSONObject = JSONObject().apply {
        put("context", androidContext())
        put("videoId", videoId)
        put("contentCheckOk", true)
        put("racyCheckOk", true)
    }

    /** 趋势页（`FEtrending`）作为推荐。 */
    fun trendingBody(): JSONObject = JSONObject().apply {
        put("context", androidContext())
        put("browseId", "FEtrending")
    }

    /** POST 到 InnerTube（阻塞，调用方置于 IO 上下文）。 */
    fun post(client: OkHttpClient, action: String, body: JSONObject): JSONObject {
        return try {
            val req = Request.Builder()
                .url("$BASE/$action?key=$API_KEY")
                .header("User-Agent", ANDROID_UA)
                .header("Content-Type", "application/json")
                .header("X-YouTube-Client-Name", "3")
                .header("X-YouTube-Client-Version", CLIENT_VERSION)
                .post(body.toString().toRequestBody(JSON_MEDIA))
                .build()
            client.newCall(req).execute().use { resp ->
                if (resp.isSuccessful) JSONObject(resp.body?.string() ?: "{}") else JSONObject()
            }
        } catch (_: Exception) {
            JSONObject()
        }
    }
}
