package com.asmrhelper.domain.model.music

/** 某在线平台的登录态（cookie + 昵称 + 登录时间），持久化到 SharedPreferences。
 *  [loginTime] 为登录成功时的毫秒时间戳，仅真实登录写入，0 表示无（未登录/已退出）。 */
data class MusicLogin(
    val cookie: String = "",
    val nickname: String = "",
    val loginTime: Long = 0L
)
