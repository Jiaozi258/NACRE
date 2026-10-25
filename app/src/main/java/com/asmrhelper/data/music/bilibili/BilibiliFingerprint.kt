package com.asmrhelper.data.music.bilibili

import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

/**
 * B站风控指纹。取流接口（`x/player/wbi/playurl`）对匿名请求的风控极严，
 * 本地随手生成的 buvid 不被 B站服务端认可，会被直接拦成 HTTP 412。
 *
 * 正确做法（与官方 Web 端一致）：先带移动端 UA 请求 `x/frontend/finger/spi`，
 * 由 B站服务端下发一套真实指纹 Cookie（buvid3/buvid4/buvid_fp/buvid_fp_plain/b_lsid），
 * 之后每个 API 请求都原样带上这套 Cookie。指纹 Cookie 持久化复用，避免频繁换指纹被标记。
 */
object BilibiliFingerprint {

    const val FINGERPRINT_URL = "https://api.bilibili.com/x/frontend/finger/spi"

    /** 指纹接口专用 UA：必须用移动端 UA，否则接口不返回有效 buvid3/buvid4。 */
    const val FINGERPRINT_UA =
        "Mozilla/5.0 (iPhone; CPU iPhone OS 13_2_3 like Mac OS X) " +
            "AppleWebKit/605.1.15 (KHTML, like Gecko) Version/13.0.3 " +
            "Mobile/15E148 Safari/604.1 Edg/114.0.0.0"

    /** Web API 通用 UA（较新的 Chrome 桌面端）。 */
    const val WEB_UA =
        "Mozilla/5.0 (Windows NT 10.0; Win64; x64) " +
            "AppleWebKit/537.36 (KHTML, like Gecko) " +
            "Chrome/124.0.0.0 Safari/537.36"

    private const val TIMEOUT_MS = 15_000

    /** 指纹 Cookie 的键，按此顺序持久化/校验。 */
    val FINGERPRINT_KEYS = listOf("buvid3", "buvid4", "buvid_fp", "buvid_fp_plain", "b_lsid")

    /** 请求 `x/frontend/finger/spi` 拉取服务端指纹，失败返回空 map（调用方自行兜底重试）。 */
    fun fetch(): Map<String, String> {
        return try {
            val conn = (URL(FINGERPRINT_URL).openConnection() as HttpURLConnection).apply {
                requestMethod = "GET"
                connectTimeout = TIMEOUT_MS
                readTimeout = TIMEOUT_MS
                setRequestProperty("User-Agent", FINGERPRINT_UA)
            }
            val code = conn.responseCode
            val body = if (code in 200..299) {
                conn.inputStream?.bufferedReader()?.use { it.readText() }.orEmpty()
            } else {
                ""
            }
            conn.disconnect()
            if (body.isBlank()) return emptyMap()
            val data = JSONObject(body).optJSONObject("data") ?: JSONObject()
            linkedMapOf(
                "buvid3" to data.optString("b_3", data.optString("buvid3", "")),
                "buvid4" to data.optString("b_4", data.optString("buvid4", "")),
                "buvid_fp" to data.optString("buvid_fp", ""),
                "buvid_fp_plain" to data.optString("buvid_fp_plain", ""),
                "b_lsid" to data.optString("b_lsid", "")
            ).filterValues { it.isNotBlank() }
        } catch (_: Exception) {
            emptyMap()
        }
    }

    /** 把指纹 map 拼成 Cookie 头（`buvid3=…; buvid4=…; …`）。 */
    fun toCookieHeader(fingerprint: Map<String, String>): String =
        fingerprint.entries.joinToString("; ") { "${it.key}=${it.value}" }
}
