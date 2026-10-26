package com.asmrhelper.data.repository

import android.content.Context
import com.asmrhelper.domain.model.ListenTogetherState
import com.asmrhelper.domain.repository.ListenTogetherRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.random.Random

@Singleton
class ListenTogetherRepositoryImpl @Inject constructor(
    @ApplicationContext private val context: Context
) : ListenTogetherRepository {

    private val prefs = context.getSharedPreferences("listen_together", Context.MODE_PRIVATE)
    private val _state = MutableStateFlow(
        ListenTogetherState(
            nickname = prefs.getString("nickname", "") ?: "",
            roomId = prefs.getString("room_id", null),
            isHost = prefs.getBoolean("is_host", false)
        )
    )
    override val state: StateFlow<ListenTogetherState> = _state.asStateFlow()

    override suspend fun setNickname(nickname: String) {
        val trimmed = nickname.trim()
        prefs.edit().putString("nickname", trimmed).apply()
        _state.value = _state.value.copy(nickname = trimmed)
    }

    override suspend fun createRoom(): String {
        val roomId = generateRoomId()
        prefs.edit()
            .putString("room_id", roomId)
            .putBoolean("is_host", true)
            .apply()
        _state.value = _state.value.copy(roomId = roomId, isHost = true)
        return roomId
    }

    override suspend fun joinRoom(roomId: String): Boolean {
        val normalized = normalizeRoomId(roomId) ?: return false
        prefs.edit()
            .putString("room_id", normalized)
            .putBoolean("is_host", false)
            .apply()
        _state.value = _state.value.copy(roomId = normalized, isHost = false)
        return true
    }

    override suspend fun leaveRoom() {
        prefs.edit().remove("room_id").putBoolean("is_host", false).apply()
        _state.value = _state.value.copy(roomId = null, isHost = false)
    }

    override fun inviteText(): String {
        val roomId = _state.value.roomId ?: return ""
        return "一起听房间号：$roomId"
    }

    override fun parseRoomId(text: String): String? {
        if (text.isBlank()) return null
        // 优先匹配「房间号：ABC123」格式，避免误取昵称等自由文本。
        val marked = Regex("房间号[：:]\\s*([A-Za-z0-9]{4,8})").find(text)?.groupValues?.getOrNull(1)
        if (marked != null) return normalizeRoomId(marked)
        // 兜底：整段文本中第一个形如 6 位大写字母数字的片段（用户可能只复制了房间号）。
        val bare = Regex("\\b[A-Za-z0-9]{6}\\b").find(text)?.value
        return bare?.let { normalizeRoomId(it) }
    }

    /** 去除易混淆字符并统一大写；仅当结果仍为 6 位字母数字时视为有效房间号。 */
    private fun normalizeRoomId(raw: String): String? {
        val cleaned = raw.uppercase().filter { it.isLetterOrDigit() }
        return cleaned.takeIf { it.length in 4..8 && it.all { c -> c.isLetterOrDigit() } }
    }

    /** 生成 6 位不含易混淆字符（I/O/0/1）的房间号。 */
    private fun generateRoomId(): String {
        val charset = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789"
        return buildString {
            repeat(6) { append(charset[Random.nextInt(charset.length)]) }
        }
    }
}
