package com.asmrhelper.ui.profile

import androidx.annotation.StringRes
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.asmrhelper.R
import com.asmrhelper.data.local.db.entity.PlayHistoryEntity
import com.asmrhelper.data.repository.PlayHistoryRepositoryImpl
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

/** 听歌统计时间范围。 */
enum class StatsRange(@StringRes val labelRes: Int) {
    LAST_WEEK(R.string.stats_range_last_week),
    LAST_MONTH(R.string.stats_range_last_month),
    LAST_QUARTER(R.string.stats_range_last_quarter),
    LAST_YEAR(R.string.stats_range_last_year)
}

/** 某时间范围内的听歌统计结果。 */
data class ListenStats(
    val totalListens: Int,
    val artistCount: Int,
    val albumCount: Int,
    val buckets: List<Int>
)

@HiltViewModel
class ListenStatsViewModel @Inject constructor(
    private val playHistoryRepository: PlayHistoryRepositoryImpl
) : ViewModel() {

    private val _range = MutableStateFlow(StatsRange.LAST_WEEK)
    val range: StateFlow<StatsRange> = _range.asStateFlow()

    fun setRange(range: StatsRange) {
        _range.value = range
    }

    val stats: StateFlow<ListenStats> = combine(
        playHistoryRepository.getAll(),
        _range
    ) { history, range -> compute(history, range) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), ListenStats(0, 0, 0, emptyList()))

    private fun compute(history: List<PlayHistoryEntity>, range: StatsRange): ListenStats {
        val now = System.currentTimeMillis()
        val rangeMs = range.durationMs()
        val cutoff = now - rangeMs
        val filtered = history.filter { it.playedAt >= cutoff }

        val totalListens = filtered.size
        // 「未知」不统计（空值同样视为未知）
        val artistCount = filtered.map { it.audioArtist.trim() }
            .filter { it.isNotEmpty() && it != UNKNOWN }
            .distinct().size
        val albumCount = filtered.map { it.album.trim() }
            .filter { it.isNotEmpty() && it != UNKNOWN }
            .distinct().size

        return ListenStats(totalListens, artistCount, albumCount, buildBuckets(filtered, range, now))
    }

    private fun buildBuckets(history: List<PlayHistoryEntity>, range: StatsRange, now: Long): List<Int> {
        val bucketCount = range.bucketCount()
        val bucketMs = range.bucketMs()
        val start = now - bucketCount * bucketMs
        val result = IntArray(bucketCount)
        for (e in history) {
            if (e.playedAt < start) continue
            val idx = ((e.playedAt - start) / bucketMs).toInt().coerceIn(0, bucketCount - 1)
            result[idx]++
        }
        return result.toList()
    }

    private fun StatsRange.durationMs(): Long = when (this) {
        StatsRange.LAST_WEEK -> 7L * DAY_MS
        StatsRange.LAST_MONTH -> 30L * DAY_MS
        StatsRange.LAST_QUARTER -> 90L * DAY_MS
        StatsRange.LAST_YEAR -> 365L * DAY_MS
    }

    private fun StatsRange.bucketCount(): Int = when (this) {
        StatsRange.LAST_WEEK -> 7
        StatsRange.LAST_MONTH -> 30
        StatsRange.LAST_QUARTER -> 13
        StatsRange.LAST_YEAR -> 52
    }

    private fun StatsRange.bucketMs(): Long = when (this) {
        StatsRange.LAST_WEEK -> DAY_MS
        StatsRange.LAST_MONTH -> DAY_MS
        StatsRange.LAST_QUARTER -> WEEK_MS
        StatsRange.LAST_YEAR -> WEEK_MS
    }

    companion object {
        private const val UNKNOWN = "未知"
        private const val DAY_MS = 24L * 3600 * 1000
        private const val WEEK_MS = 7L * DAY_MS
    }
}
