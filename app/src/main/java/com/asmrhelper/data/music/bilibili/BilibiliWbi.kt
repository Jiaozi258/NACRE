package com.asmrhelper.data.music.bilibili

import java.security.MessageDigest
import java.net.URLEncoder

/**
 * B站 WBI 签名。参考公开 wbi-sign 算法：
 * 1. `/x/web-interface/nav` 返回 `data.wbi_img.img_url` / `sub_url`，取文件名（去扩展名）拼成 rawKey；
 * 2. 按 MIXIN_KEY_ENC_TAB 重排 rawKey 后取前 32 位 = mixinKey；
 * 3. 参数加 `wts`、按 key 排序、值过滤 `!'()*` 后 encodeURIComponent 拼接，
 *    再 `md5(query + mixinKey)` 得 `w_rid`。
 */
object BilibiliWbi {

    private val MIXIN_KEY_ENC_TAB = intArrayOf(
        46, 47, 18, 2, 53, 8, 23, 32, 15, 50, 10, 31, 58, 3, 45, 35,
        27, 43, 5, 49, 33, 9, 42, 19, 29, 28, 14, 39, 12, 38, 41, 13,
        37, 48, 7, 16, 24, 55, 40, 61, 26, 17, 0, 1, 60, 51, 30, 4,
        22, 25, 54, 21, 56, 59, 6, 63, 57, 62, 11, 36, 20, 34, 44, 52
    )

    /** 由 img_url / sub_url 计算 mixinKey（前 32 位）。 */
    fun mixinKey(imgUrl: String, subUrl: String): String {
        val rawKey = extractKey(imgUrl) + extractKey(subUrl)
        val sb = StringBuilder(32)
        for (i in 0 until 32) {
            val idx = MIXIN_KEY_ENC_TAB[i]
            if (idx in rawKey.indices) sb.append(rawKey[idx])
        }
        return sb.toString()
    }

    /** 对参数签名，返回拼好的查询串（含 `wts` 与 `w_rid`，不含前导 `?`）。 */
    fun sign(mixinKey: String, params: Map<String, String>, wts: Long = System.currentTimeMillis() / 1000): String {
        val filtered = linkedMapOf<String, String>()
        params.forEach { (k, v) -> filtered[k] = filterChars(v) }
        filtered["wts"] = wts.toString()
        val query = filtered.keys.sorted().joinToString("&") { k ->
            "${encode(k)}=${encode(filtered.getValue(k))}"
        }
        val wRid = md5(query + mixinKey)
        return "$query&w_rid=$wRid"
    }

    private fun extractKey(url: String): String =
        url.substringAfterLast('/').substringBefore('.')

    private fun filterChars(s: String): String = s.replace(Regex("[!'()*]"), "")

    /** encodeURIComponent 等价：空格 → %20，`~` 保持不编码。 */
    private fun encode(s: String): String =
        URLEncoder.encode(s, "UTF-8").replace("+", "%20").replace("%7E", "~")

    private fun md5(s: String): String =
        MessageDigest.getInstance("MD5")
            .digest(s.toByteArray(Charsets.UTF_8))
            .joinToString("") { b -> "%02x".format(b.toInt() and 0xff) }
}
