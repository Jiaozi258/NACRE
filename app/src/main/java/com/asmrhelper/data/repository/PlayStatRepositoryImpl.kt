package com.asmrhelper.data.repository

import com.asmrhelper.data.local.db.dao.PlayStatDao
import com.asmrhelper.data.local.db.entity.PlayStatEntity
import com.asmrhelper.domain.repository.PlayStatRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class PlayStatRepositoryImpl @Inject constructor(
    private val dao: PlayStatDao
) : PlayStatRepository {
    override fun observe(songKey: String): Flow<PlayStatEntity?> = dao.observe(songKey)
    override fun getAll(): Flow<List<PlayStatEntity>> = dao.getAll()
    override suspend fun recordListen(songKey: String, title: String, artist: String, now: Long) {
        val updated = dao.increment(songKey, title, artist, now)
        if (updated == 0) {
            dao.insert(PlayStatEntity(songKey, title, artist, 1, now, now))
        }
    }
}
