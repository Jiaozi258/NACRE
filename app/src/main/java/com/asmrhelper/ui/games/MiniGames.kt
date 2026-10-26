package com.asmrhelper.ui.games

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.asmrhelper.R
import com.asmrhelper.ui.theme.DarkSurface
import com.asmrhelper.ui.theme.DarkSurfaceVariant
import com.asmrhelper.ui.theme.ErrorRed
import com.asmrhelper.ui.theme.LocalAccentColor
import com.asmrhelper.ui.theme.TextHint
import com.asmrhelper.ui.theme.TextPrimary
import com.asmrhelper.ui.theme.TextSecondary
import kotlin.math.abs
import kotlin.random.Random

/** ASMR 界面下方小工具里的小游戏类型。 */
enum class MiniGame { GAME_2048, MINESWEEPER }

/** 全屏小游戏浮层（覆盖在 ASMR 播放页上，音频继续播放）。 */
@Composable
fun MiniGameOverlay(game: MiniGame, onClose: () -> Unit) {
    when (game) {
        MiniGame.GAME_2048 -> Game2048Screen(onClose)
        MiniGame.MINESWEEPER -> MinesweeperScreen(onClose)
    }
}

// ────────────────────────────── 2048 ──────────────────────────────

private enum class Direction { UP, DOWN, LEFT, RIGHT }

private fun newBoard2048(): List<List<Int>> {
    val board = MutableList(4) { MutableList(4) { 0 } }
    spawnTile(board)
    spawnTile(board)
    return board.map { it.toList() }
}

private fun spawnTile(board: MutableList<MutableList<Int>>) {
    val empty = mutableListOf<Pair<Int, Int>>()
    for (r in 0 until 4) for (c in 0 until 4) if (board[r][c] == 0) empty.add(r to c)
    if (empty.isEmpty()) return
    val (r, c) = empty[Random.nextInt(empty.size)]
    board[r][c] = if (Random.nextFloat() < 0.9f) 2 else 4
}

private fun slideRow(row: List<Int>): Pair<List<Int>, Int> {
    val filtered = row.filter { it != 0 }.toMutableList()
    var score = 0
    var i = 0
    while (i < filtered.size - 1) {
        if (filtered[i] == filtered[i + 1]) {
            val merged = filtered[i] * 2
            filtered[i] = merged
            filtered.removeAt(i + 1)
            score += merged
        }
        i++
    }
    while (filtered.size < 4) filtered.add(0)
    return filtered.toList() to score
}

private fun transpose(g: List<List<Int>>): List<List<Int>> =
    (0 until 4).map { c -> (0 until 4).map { r -> g[r][c] } }

private fun reverseRows(g: List<List<Int>>): List<List<Int>> = g.map { it.reversed() }

private fun move2048(g: List<List<Int>>, dir: Direction): Pair<List<List<Int>>, Int> {
    var grid = g
    var score = 0
    fun process(rows: List<List<Int>>): List<List<Int>> = rows.map { row ->
        val (slid, gained) = slideRow(row)
        score += gained
        slid
    }
    when (dir) {
        Direction.LEFT -> grid = process(grid)
        Direction.RIGHT -> grid = reverseRows(process(reverseRows(grid)))
        Direction.UP -> grid = transpose(process(transpose(grid)))
        Direction.DOWN -> grid = transpose(reverseRows(process(reverseRows(transpose(grid)))))
    }
    return grid to score
}

private fun isGameOver2048(g: List<List<Int>>): Boolean {
    for (r in 0 until 4) for (c in 0 until 4) {
        if (g[r][c] == 0) return false
        if (c < 3 && g[r][c] == g[r][c + 1]) return false
        if (r < 3 && g[r][c] == g[r + 1][c]) return false
    }
    return true
}

@Composable
private fun Game2048Screen(onClose: () -> Unit) {
    var grid by remember { mutableStateOf(newBoard2048()) }
    var score by remember { mutableIntStateOf(0) }
    var gameOver by remember { mutableStateOf(false) }
    val accent = LocalAccentColor.current

    fun restart() {
        grid = newBoard2048()
        score = 0
        gameOver = false
    }

    fun handle(dir: Direction) {
        if (gameOver) return
        val (moved, gained) = move2048(grid, dir)
        if (moved != grid) {
            val next = moved.map { it.toMutableList() }.toMutableList()
            spawnTile(next)
            grid = next.map { it.toList() }
            score += gained
            gameOver = isGameOver2048(grid)
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(DarkSurface)
            .statusBarsPadding()
            .padding(horizontal = 20.dp)
    ) {
        GameTopBar(stringResource(R.string.mini_game_2048), scoreLabel = stringResource(R.string.mini_game_score, score), onRestart = { restart() }, onClose = onClose)
        Text(stringResource(R.string.mini_game_2048_hint), color = TextHint, fontSize = 12.sp)
        Spacer(Modifier.height(12.dp))

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(1f)
                .clip(RoundedCornerShape(12.dp))
                .background(DarkSurfaceVariant)
                .pointerInput(Unit) {
                    var tx = 0f
                    var ty = 0f
                    detectDragGestures(
                        onDragStart = { tx = 0f; ty = 0f },
                        onDrag = { _, drag -> tx += drag.x; ty += drag.y },
                        onDragEnd = {
                            when {
                                abs(tx) > abs(ty) && tx > 60f -> handle(Direction.RIGHT)
                                abs(tx) > abs(ty) && tx < -60f -> handle(Direction.LEFT)
                                ty > 60f -> handle(Direction.DOWN)
                                ty < -60f -> handle(Direction.UP)
                            }
                        }
                    )
                }
                .padding(6.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            grid.forEach { row ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    row.forEach { value ->
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxSize()
                                .clip(RoundedCornerShape(8.dp))
                                .background(tileColor2048(value)),
                            contentAlignment = Alignment.Center
                        ) {
                            if (value != 0) {
                                Text(
                                    text = value.toString(),
                                    color = if (value <= 4) Color(0xFF776E65) else Color.White,
                                    fontSize = if (value >= 1024) 18.sp else 24.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }
            }
        }

        Spacer(Modifier.height(12.dp))
        if (gameOver) {
            Text(
                text = stringResource(R.string.mini_game_lost),
                color = ErrorRed,
                fontSize = 16.sp,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.align(Alignment.CenterHorizontally)
            )
        }
    }
}

private fun tileColor2048(value: Int): Color = when (value) {
    0 -> Color(0xFF2A2A31)
    2 -> Color(0xFF3A3A42)
    4 -> Color(0xFF4A4A55)
    8 -> Color(0xFFF2B179)
    16 -> Color(0xFFF59563)
    32 -> Color(0xFFF67C5F)
    64 -> Color(0xFFF65E3B)
    128 -> Color(0xFFEDCF72)
    256 -> Color(0xFFEDCC61)
    512 -> Color(0xFFEDC850)
    1024 -> Color(0xFFEDC53F)
    else -> Color(0xFF3C3A32)
}

// ────────────────────────────── 扫雷 ──────────────────────────────

private data class Cell(
    val isMine: Boolean = false,
    val adjacent: Int = 0,
    val isRevealed: Boolean = false,
    val isFlagged: Boolean = false
)

private const val ROWS = 9
private const val COLS = 9
private const val MINES = 10

private fun newBoard(): List<List<Cell>> {
    val board = MutableList(ROWS) { r -> MutableList(COLS) { c -> Cell() } }
    val positions = (0 until ROWS * COLS).shuffled().take(MINES).toSet()
    for (idx in positions) {
        val r = idx / COLS
        val c = idx % COLS
        board[r][c] = board[r][c].copy(isMine = true)
    }
    for (r in 0 until ROWS) for (c in 0 until COLS) {
        if (!board[r][c].isMine) {
            var count = 0
            for (dr in -1..1) for (dc in -1..1) {
                val nr = r + dr
                val nc = c + dc
                if (dr == 0 && dc == 0) continue
                if (nr in 0 until ROWS && nc in 0 until COLS && board[nr][nc].isMine) count++
            }
            board[r][c] = board[r][c].copy(adjacent = count)
        }
    }
    return board.map { it.toList() }
}

private fun revealCell(grid: List<List<Cell>>, r: Int, c: Int): List<List<Cell>> {
    val cells = grid.map { row -> row.toMutableList() }.toMutableList()
    fun flood(rr: Int, cc: Int) {
        if (rr !in 0 until ROWS || cc !in 0 until COLS) return
        val cell = cells[rr][cc]
        if (cell.isRevealed || cell.isFlagged) return
        cells[rr][cc] = cell.copy(isRevealed = true)
        if (!cell.isMine && cell.adjacent == 0) {
            for (dr in -1..1) for (dc in -1..1) {
                if (dr == 0 && dc == 0) continue
                flood(rr + dr, cc + dc)
            }
        }
    }
    flood(r, c)
    return cells.map { it.toList() }
}

private fun hasWon(grid: List<List<Cell>>): Boolean =
    grid.flatten().all { it.isRevealed || it.isMine }

@Composable
private fun MinesweeperScreen(onClose: () -> Unit) {
    var grid by remember { mutableStateOf(newBoard()) }
    var gameOver by remember { mutableStateOf(false) }
    var won by remember { mutableStateOf(false) }
    var flagMode by remember { mutableStateOf(false) }
    val accent = LocalAccentColor.current

    fun restart() {
        grid = newBoard()
        gameOver = false
        won = false
    }

    fun tap(r: Int, c: Int) {
        if (gameOver || won) return
        val cell = grid[r][c]
        if (cell.isRevealed) return
        if (flagMode) {
            grid = grid.mapIndexed { rr, row ->
                row.mapIndexed { cc, ccCell ->
                    if (rr == r && cc == c && !ccCell.isRevealed) ccCell.copy(isFlagged = !ccCell.isFlagged) else ccCell
                }
            }
            return
        }
        if (cell.isFlagged) return
        if (cell.isMine) {
            // 踩雷：翻开所有雷，结束
            grid = grid.map { row -> row.map { it.copy(isRevealed = it.isMine || it.isRevealed) } }
            gameOver = true
            return
        }
        grid = revealCell(grid, r, c)
        won = hasWon(grid)
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(DarkSurface)
            .statusBarsPadding()
            .padding(horizontal = 20.dp)
    ) {
        GameTopBar(
            stringResource(R.string.mini_game_minesweeper),
            scoreLabel = "",
            onRestart = { restart() },
            onClose = onClose
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = if (flagMode) stringResource(R.string.mini_game_flag_mode) else stringResource(R.string.mini_game_reveal_mode),
                color = accent,
                fontSize = 13.sp
            )
            TextButton(onClick = { flagMode = !flagMode }) {
                Text(
                    text = if (flagMode) stringResource(R.string.mini_game_reveal_mode) else stringResource(R.string.mini_game_flag_mode),
                    color = TextSecondary,
                    fontSize = 13.sp
                )
            }
        }

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(1f)
                .clip(RoundedCornerShape(8.dp))
                .background(DarkSurfaceVariant)
                .padding(4.dp),
            verticalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            grid.forEachIndexed { r, row ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    horizontalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    row.forEachIndexed { c, cell ->
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxSize()
                                .clip(RoundedCornerShape(4.dp))
                                .background(
                                    when {
                                        cell.isRevealed -> Color(0xFF3A3A42)
                                        else -> Color(0xFF2A2A31)
                                    }
                                )
                                .clickable { tap(r, c) },
                            contentAlignment = Alignment.Center
                        ) {
                            when {
                                cell.isFlagged -> Text("🚩", fontSize = 12.sp)
                                cell.isRevealed && cell.isMine -> Text("💣", fontSize = 12.sp)
                                cell.isRevealed && cell.adjacent > 0 -> Text(
                                    cell.adjacent.toString(),
                                    color = numberColor(cell.adjacent),
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }
            }
        }

        Spacer(Modifier.height(12.dp))
        when {
            won -> Text(stringResource(R.string.mini_game_won), color = accent, fontSize = 16.sp, fontWeight = FontWeight.SemiBold, modifier = Modifier.align(Alignment.CenterHorizontally))
            gameOver -> Text(stringResource(R.string.mini_game_lost), color = ErrorRed, fontSize = 16.sp, fontWeight = FontWeight.SemiBold, modifier = Modifier.align(Alignment.CenterHorizontally))
        }
    }
}

private fun numberColor(n: Int): Color = when (n) {
    1 -> Color(0xFF4FC3F7)
    2 -> Color(0xFF81C784)
    3 -> Color(0xFFF06292)
    4 -> Color(0xFF9575CD)
    else -> Color(0xFFFFB74D)
}

// ────────────────────────────── 公共顶栏 ──────────────────────────────

@Composable
private fun GameTopBar(
    title: String,
    scoreLabel: String,
    onRestart: () -> Unit,
    onClose: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 8.dp, bottom = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = title,
            color = TextPrimary,
            fontSize = 18.sp,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.weight(1f)
        )
        if (scoreLabel.isNotEmpty()) {
            Text(scoreLabel, color = TextSecondary, fontSize = 14.sp)
            Spacer(Modifier.size(8.dp))
        }
        TextButton(onClick = onRestart) {
            Text(stringResource(R.string.mini_game_restart), color = LocalAccentColor.current, fontSize = 13.sp)
        }
        IconButton(onClick = onClose) {
            Icon(Icons.Filled.Close, contentDescription = stringResource(R.string.common_close), tint = TextSecondary)
        }
    }
}
