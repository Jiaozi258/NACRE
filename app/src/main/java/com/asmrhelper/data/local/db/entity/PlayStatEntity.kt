package com.asmrhelper.data.local.db.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * 每首歌（含视频音频）的播放统计：以稳定歌曲键为主键，聚合收听次数与首次/最近收听时间。
 * 与 [com.asmrhelper.data.local.db.entity.PlayHistoryEntity]（逐条播放日志）不同，这里是聚合计数。
 */
@Entity(tableName = "play_stat")
data class PlayStatEntity(
    @PrimaryKey val songKey: String,
    val title: String,
    val artist: String,
    val listenCount: Int,
    val firstListenAt: Long,
    val lastListenAt: Long
)
