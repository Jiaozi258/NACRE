package com.asmrhelper.domain.model

/** 「一起听」房间会话状态（本地保存昵称 / 房间号 / 是否房主）。 */
data class ListenTogetherState(
    val nickname: String = "",
    val roomId: String? = null,
    val isHost: Boolean = false
) {
    val inRoom: Boolean get() = !roomId.isNullOrBlank()
}
