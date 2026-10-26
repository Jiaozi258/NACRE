package com.asmrhelper.domain.repository

import com.asmrhelper.domain.model.ListenTogetherState
import kotlinx.coroutines.flow.StateFlow

/** 「一起听」房间会话（昵称 / 房间号 / 房主身份）的持久化存取。 */
interface ListenTogetherRepository {
    val state: StateFlow<ListenTogetherState>

    /** 更新昵称（创建/加入房间时也一并保存）。 */
    suspend fun setNickname(nickname: String)

    /** 生成并进入一个新房间，返回房间号；同时记录为房主。 */
    suspend fun createRoom(): String

    /** 加入指定房间；房间号格式不合法时返回 false。 */
    suspend fun joinRoom(roomId: String): Boolean

    /** 离开当前房间。 */
    suspend fun leaveRoom()

    /** 生成可复制/分享的邀请文本（含房间号）。 */
    fun inviteText(): String

    /** 从任意文本中解析出房间号（支持直接复制邀请文本或仅复制房间号）。 */
    fun parseRoomId(text: String): String?
}
