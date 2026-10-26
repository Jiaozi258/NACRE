package com.asmrhelper.data.local.db.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.asmrhelper.data.local.db.entity.PlayStatEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface PlayStatDao {
    @Query("SELECT * FROM play_stat WHERE songKey = :songKey")
    fun observe(songKey: String): Flow<PlayStatEntity?>

    @Query("SELECT * FROM play_stat")
    fun getAll(): Flow<List<PlayStatEntity>>

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insert(entity: PlayStatEntity)

    @Query("UPDATE play_stat SET listenCount = listenCount + 1, title = :title, artist = :artist, lastListenAt = :now WHERE songKey = :songKey")
    suspend fun increment(songKey: String, title: String, artist: String, now: Long): Int
}
