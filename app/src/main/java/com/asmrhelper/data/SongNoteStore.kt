package com.asmrhelper.data

import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.json.JSONObject
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 歌曲笔记（歌评）存储：每首歌一条，最多 400 字。
 * 以歌曲稳定标识（sourceSongId 或本地 id）为键，序列化到单个 JSON 字符串，
 * 避免为每首歌生成一个 SharedPreferences key。
 */
@Singleton
class SongNoteStore @Inject constructor(
    @ApplicationContext context: Context
) {
    private val prefs = context.getSharedPreferences("song_notes", Context.MODE_PRIVATE)

    private val _notes = MutableStateFlow(load())
    val notes: StateFlow<Map<String, String>> = _notes.asStateFlow()

    private fun load(): Map<String, String> {
        val json = prefs.getString(KEY_NOTES, null) ?: return emptyMap()
        return runCatching {
            val obj = JSONObject(json)
            val map = mutableMapOf<String, String>()
            val keys = obj.keys()
            while (keys.hasNext()) {
                val k = keys.next()
                map[k] = obj.optString(k)
            }
            map
        }.getOrDefault(emptyMap())
    }

    fun setNote(key: String, note: String) {
        val trimmed = note.trim()
        val next = _notes.value.toMutableMap()
        if (trimmed.isEmpty()) next.remove(key) else next[key] = trimmed
        _notes.value = next

        val obj = JSONObject()
        next.forEach { (k, v) -> obj.put(k, v) }
        prefs.edit().putString(KEY_NOTES, obj.toString()).apply()
    }

    private companion object {
        const val KEY_NOTES = "notes_json"
    }
}
