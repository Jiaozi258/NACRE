package com.asmrhelper.domain.repository

import com.asmrhelper.data.local.db.entity.PlayStatEntity
import kotlinx.coroutines.flow.Flow

interface PlayStatRepository {
    fun observe(songKey: String): Flow<PlayStatEntity?>
    fun getAll(): Flow<List<PlayStatEntity>>
    suspend fun recordListen(songKey: String, title: String, artist: String, now: Long)
}
