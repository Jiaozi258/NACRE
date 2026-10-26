package com.asmrhelper.util

/** 从 Set-Cookie 或 Cookie 串中挑出指定 name，返回可直接放进 `Cookie` 头的 `name=value` 串。 */
object Cookies {

    fun select(raw: String?, keep: Set<String>): String {
        if (raw.isNullOrBlank()) return ""
        val result = linkedMapOf<String, String>()
        raw.split(';').forEach { part ->
            val kv = part.trim().split('=', limit = 2)
            if (kv.size == 2 && kv[0].isNotBlank() && kv[0] in keep) {
                result[kv[0]] = kv[1]
            }
        }
        return result.entries.joinToString("; ") { "${it.key}=${it.value}" }
    }

    /** 精确判断 Cookie 串中是否存在指定 name（按 `name=` 边界匹配，避免 `uin` 误匹配 `wxuin`）。 */
    fun hasKey(raw: String?, key: String): Boolean {
        if (raw.isNullOrBlank()) return false
        return raw.split(';').any { part ->
            part.trim().substringBefore('=').trim() == key
        }
    }
}
