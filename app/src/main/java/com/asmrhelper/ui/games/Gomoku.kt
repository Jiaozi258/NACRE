package com.asmrhelper.ui.games

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.asmrhelper.R
import com.asmrhelper.ui.theme.DarkSurface
import com.asmrhelper.ui.theme.DarkSurfaceVariant
import com.asmrhelper.ui.theme.LocalAccentColor
import com.asmrhelper.ui.theme.TextHint
import com.asmrhelper.ui.theme.TextPrimary
import com.asmrhelper.ui.theme.TextSecondary
import kotlinx.coroutines.delay

private const val BOARD_SIZE = 15
private const val EMPTY = 0
private const val BLACK = 1 // 玩家（先手）
private const val WHITE = 2 // 人机
private const val DRAW = 3
private const val NONE = 0

private val DIRS = listOf(1 to 0, 0 to 1, 1 to 1, 1 to -1)

@Composable
internal fun GomokuScreen(onClose: () -> Unit) {
    var difficulty by remember { mutableStateOf(AiDifficulty.CASUAL) }
    var board by remember { mutableStateOf(newGomokuBoard()) }
    var currentTurn by remember { mutableIntStateOf(BLACK) }
    var winner by remember { mutableIntStateOf(NONE) }
    val accent = LocalAccentColor.current

    fun reset() {
        board = newGomokuBoard()
        currentTurn = BLACK
        winner = NONE
    }

    fun setDifficulty(d: AiDifficulty) {
        difficulty = d
        reset()
    }

    fun place(r: Int, c: Int, player: Int) {
        if (winner != NONE || board[r][c] != EMPTY) return
        val next = placeStone(board, r, c, player)
        board = next
        when {
            hasFive(next, r, c, player) -> winner = player
            isFull(next) -> winner = DRAW
            else -> currentTurn = if (player == BLACK) WHITE else BLACK
        }
    }

    LaunchedEffect(currentTurn, winner) {
        if (currentTurn == WHITE && winner == NONE) {
            delay(420)
            val move = chooseAiMove(board, difficulty, WHITE)
            if (move != null) place(move.first, move.second, WHITE)
        }
    }

    val liveBoard = rememberUpdatedState(board)
    val liveTurn = rememberUpdatedState(currentTurn)
    val liveWinner = rememberUpdatedState(winner)

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(DarkSurface)
            .statusBarsPadding()
            .padding(horizontal = 20.dp)
    ) {
        GameTopBar(
            title = stringResource(R.string.game_gomoku_title),
            scoreLabel = "",
            onRestart = { reset() },
            onClose = onClose
        )
        DifficultySelector(difficulty, ::setDifficulty)

        Spacer(Modifier.height(6.dp))

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 4.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = stringResource(R.string.game_gomoku_you),
                color = if (currentTurn == BLACK && winner == NONE) accent else TextPrimary,
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold
            )
            Text(
                text = when (winner) {
                    BLACK -> stringResource(R.string.game_win)
                    WHITE -> stringResource(R.string.game_lose)
                    DRAW -> stringResource(R.string.game_draw)
                    else -> if (currentTurn == BLACK) stringResource(R.string.game_your_turn) else stringResource(R.string.game_ai_turn)
                },
                color = if (winner == BLACK) accent else TextSecondary,
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = stringResource(R.string.game_gomoku_ai),
                color = if (currentTurn == WHITE && winner == NONE) accent else TextPrimary,
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold
            )
        }

        Spacer(Modifier.height(10.dp))

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(1f)
                .clip(RoundedCornerShape(12.dp))
                .background(DarkSurfaceVariant)
                .pointerInput(Unit) {
                    val sz = size
                    detectTapGestures { offset ->
                        if (liveWinner.value != NONE || liveTurn.value != BLACK) return@detectTapGestures
                        val cell = sz.width / BOARD_SIZE
                        val c = (offset.x / cell).toInt().coerceIn(0, BOARD_SIZE - 1)
                        val r = (offset.y / cell).toInt().coerceIn(0, BOARD_SIZE - 1)
                        if (liveBoard.value[r][c] == EMPTY) {
                            place(r, c, BLACK)
                        }
                    }
                }
        ) {
            Canvas(Modifier.fillMaxSize()) {
                val cell = size.width / BOARD_SIZE
                val pad = cell / 2f
                val lineColor = Color(0xFF56565F)
                for (i in 0 until BOARD_SIZE) {
                    val p = pad + i * cell
                    drawLine(lineColor, Offset(p, pad), Offset(p, size.height - pad), 1.2f)
                    drawLine(lineColor, Offset(pad, p), Offset(size.width - pad, p), 1.2f)
                }
                for (r in 0 until BOARD_SIZE) for (c in 0 until BOARD_SIZE) {
                    val v = board[r][c]
                    if (v == EMPTY) continue
                    val cx = pad + c * cell
                    val cy = pad + r * cell
                    val radius = cell * 0.42f
                    if (v == BLACK) {
                        drawCircle(Color(0xFF14141A), radius, Offset(cx, cy))
                    } else {
                        drawCircle(Color(0xFFF2F2F2), radius, Offset(cx, cy))
                        drawCircle(Color(0xFF9A9AA5), radius, Offset(cx, cy), style = Stroke(1.5f))
                    }
                }
            }
        }

        Spacer(Modifier.height(10.dp))
        Text(
            text = stringResource(R.string.game_gomoku_hint),
            color = TextHint,
            fontSize = 12.sp
        )
    }
}

private fun newGomokuBoard(): List<List<Int>> = List(BOARD_SIZE) { List(BOARD_SIZE) { EMPTY } }

private fun placeStone(board: List<List<Int>>, r: Int, c: Int, player: Int): List<List<Int>> =
    board.mapIndexed { rr, row ->
        if (rr == r) row.mapIndexed { cc, v -> if (cc == c) player else v } else row
    }

private fun hasFive(board: List<List<Int>>, r: Int, c: Int, player: Int): Boolean {
    for ((dr, dc) in DIRS) {
        var count = 1
        var i = r + dr; var j = c + dc
        while (i in 0 until BOARD_SIZE && j in 0 until BOARD_SIZE && board[i][j] == player) { count++; i += dr; j += dc }
        i = r - dr; j = c - dc
        while (i in 0 until BOARD_SIZE && j in 0 until BOARD_SIZE && board[i][j] == player) { count++; i -= dr; j -= dc }
        if (count >= 5) return true
    }
    return false
}

private fun isFull(board: List<List<Int>>): Boolean = board.all { row -> row.all { it != EMPTY } }

private fun candidateMoves(board: List<List<Int>>): List<Pair<Int, Int>> {
    val hasStone = board.any { row -> row.any { it != EMPTY } }
    if (!hasStone) return listOf(BOARD_SIZE / 2 to BOARD_SIZE / 2)
    val set = LinkedHashSet<Pair<Int, Int>>()
    for (r in 0 until BOARD_SIZE) for (c in 0 until BOARD_SIZE) {
        if (board[r][c] == EMPTY) continue
        for (dr in -2..2) for (dc in -2..2) {
            val nr = r + dr
            val nc = c + dc
            if (nr in 0 until BOARD_SIZE && nc in 0 until BOARD_SIZE && board[nr][nc] == EMPTY) set.add(nr to nc)
        }
    }
    return set.toList()
}

private fun patternScore(count: Int, openEnds: Int): Int = when {
    count >= 5 -> 1_000_000
    openEnds == 2 -> when (count) { 4 -> 100_000; 3 -> 1_000; 2 -> 10; 1 -> 1; else -> 0 }
    openEnds == 1 -> when (count) { 4 -> 10_000; 3 -> 100; 2 -> 5; 1 -> 1; else -> 0 }
    else -> 0
}

private fun moveScore(board: List<List<Int>>, r: Int, c: Int, player: Int): Int {
    var total = 0
    for ((dr, dc) in DIRS) {
        var count = 1
        var openEnds = 0
        var i = r + dr; var j = c + dc
        while (i in 0 until BOARD_SIZE && j in 0 until BOARD_SIZE && board[i][j] == player) { count++; i += dr; j += dc }
        if (i in 0 until BOARD_SIZE && j in 0 until BOARD_SIZE && board[i][j] == EMPTY) openEnds++
        i = r - dr; j = c - dc
        while (i in 0 until BOARD_SIZE && j in 0 until BOARD_SIZE && board[i][j] == player) { count++; i -= dr; j -= dc }
        if (i in 0 until BOARD_SIZE && j in 0 until BOARD_SIZE && board[i][j] == EMPTY) openEnds++
        total += patternScore(count, openEnds)
    }
    return total
}

private fun moveTotal(board: List<List<Int>>, r: Int, c: Int, player: Int): Int {
    val opp = if (player == BLACK) WHITE else BLACK
    return moveScore(board, r, c, player) + moveScore(board, r, c, opp)
}

private fun chooseAiMove(board: List<List<Int>>, difficulty: AiDifficulty, ai: Int): Pair<Int, Int>? {
    val candidates = candidateMoves(board)
    if (candidates.isEmpty()) return null
    return when (difficulty) {
        AiDifficulty.CASUAL -> candidates.random()
        AiDifficulty.CHALLENGE -> candidates.maxByOrNull { moveTotal(board, it.first, it.second, ai) } ?: candidates.first()
        AiDifficulty.DUEL -> duelMove(board, ai)
    }
}

private fun duelMove(board: List<List<Int>>, ai: Int): Pair<Int, Int> {
    val candidates = candidateMoves(board)
        .sortedByDescending { moveTotal(board, it.first, it.second, ai) }
        .take(10)
    val opp = if (ai == BLACK) WHITE else BLACK
    var best = candidates.first()
    var bestValue = Int.MIN_VALUE
    for (m in candidates) {
        val nextBoard = placeStone(board, m.first, m.second, ai)
        val oppCandidates = candidateMoves(nextBoard)
            .sortedByDescending { moveTotal(nextBoard, it.first, it.second, opp) }
            .take(8)
        var oppBest = 0
        for (om in oppCandidates) oppBest = maxOf(oppBest, moveTotal(nextBoard, om.first, om.second, opp))
        val value = moveTotal(board, m.first, m.second, ai) - oppBest
        if (value > bestValue) { bestValue = value; best = m }
    }
    return best
}
