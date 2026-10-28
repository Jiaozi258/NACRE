package com.asmrhelper.ui.games

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
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
import kotlinx.coroutines.delay
import kotlin.random.Random

private const val PLAYER = 0
private const val AI = 1

private enum class BrItem(val labelRes: Int) {
    MAGNIFIER(R.string.game_br_item_0),
    CIGARETTE(R.string.game_br_item_1),
    BEER(R.string.game_br_item_2),
    SHOTGUN(R.string.game_br_item_3),
    HANDCUFFS(R.string.game_br_item_4),
    ADRENALINE(R.string.game_br_item_5),
    CONVERTER(R.string.game_br_item_6),
    PHONE(R.string.game_br_item_7),
    BOMB(R.string.game_br_item_8),
    MEDICINE(R.string.game_br_item_9)
}

private enum class BrPhase { SETUP, DEALING, PLAYING, OVER }

private data class BrConfig(
    val hpMin: Int = 2,
    val hpMax: Int = 6,
    val bulletMin: Int = 2,
    val bulletMax: Int = 8,
    val liveMin: Int = 1,
    val liveMax: Int = 6
)

private data class BrState(
    val phase: BrPhase = BrPhase.SETUP,
    val config: BrConfig = BrConfig(),
    val playerHp: Int = 0,
    val aiHp: Int = 0,
    val maxHp: Int = 0,
    val chamber: List<Boolean> = emptyList(), // true = 实弹
    val currentTurn: Int = PLAYER,
    val playerItems: List<BrItem> = emptyList(),
    val aiItems: List<BrItem> = emptyList(),
    val itemDeck: List<BrItem> = emptyList(),
    val shotgun: Boolean = false,
    val handcuffed: Boolean = false,
    val bombOwner: Int = -1,
    val message: String = "",
    val winner: Int = -1,
    val round: Int = 1,
    val dealtHeart: Int? = null,
    val dealtBullets: Int? = null,
    val dealtLive: Int? = null
)

private const val RULES_TEXT = """《恶魔轮盘》简明规则

【目标】
互射把对方血量打到 0 即获胜。

【开局流程】
1. 随机抽三张牌定本局参数：
   ♥ 血量 —— 双方各有多少血
   ♣ 子弹数 —— 弹膛装几颗子弹
   ♦ 实弹数 —— 其中几颗是实弹（其余为空弹）
2. 黑桃牌判定每颗子弹：双数为实弹、单数为空弹，洗牌装膛（数量公开、顺序未知）。
3. 猜拳/骰子随机决定谁先手。

【你的回合】
- 可先用任意张道具牌，然后必须开一枪（射对手或射自己）。
- 射自己 + 空弹：你继续行动一回合。
- 射对手 + 空弹：轮到对方。
- 实弹命中：造成 1 点伤害（散弹 ×2），然后轮到对方。
- 弹膛打空自动重新装弹（按你设定的范围重新随机），并各抽 1~3 张道具。

【道具牌 · 全局牌堆共 37 张，用完即止】
- 放大镜 A ×5：查看第一发是实弹还是空弹
- 烟 2 ×5：回 1 点血
- 啤酒 3 ×5：退掉第一发子弹
- 散弹 4 ×5：下一发伤害 ×2
- 手铐 5 ×4：对方跳过下个回合
- 肾上腺素 6 ×4：偷对方一张道具
- 转换器 7 ×4：第一发空变实 / 实变空
- 小灵通 8 ×2：查看第 N 发（N=2~8）
- 定时炸弹 9 ×1：25% 概率在持有者回合开始时爆炸扣 2 血，否则转交对方
- 过期药 10 ×2：50% 回 2 血，50% 扣 1 血
- 每人同种道具最多持 2 张；每回合（含重新装弹）各随机抽 1~3 张。"""

@Composable
internal fun BuckshotRouletteScreen(onClose: () -> Unit) {
    var difficulty by remember { mutableStateOf(AiDifficulty.CASUAL) }
    var state by remember { mutableStateOf(initialBrState()) }
    var showRules by remember { mutableStateOf(false) }
    val accent = LocalAccentColor.current

    fun backToSetup() { state = initialBrState() }
    fun restartSame() { state = initialBrState(config = state.config).copy(phase = BrPhase.DEALING) }
    fun updateConfig(c: BrConfig) { state = state.copy(config = c) }

    // 发牌流程
    LaunchedEffect(state.phase) {
        if (state.phase == BrPhase.DEALING) {
            val cfg = state.config
            delay(500)
            val hp = Random.nextInt(cfg.hpMin, cfg.hpMax + 1)
            state = state.copy(dealtHeart = hp)
            delay(600)
            val bullets = Random.nextInt(cfg.bulletMin, cfg.bulletMax + 1)
            state = state.copy(dealtBullets = bullets)
            delay(600)
            val live = minOf(Random.nextInt(cfg.liveMin, cfg.liveMax + 1), bullets)
            state = state.copy(dealtLive = live)
            delay(600)
            var s = state.copy(
                maxHp = hp, playerHp = hp, aiHp = hp,
                chamber = buildChamber(bullets, live),
                itemDeck = newItemDeck(),
                message = "♠ 子弹已装膛"
            )
            state = s
            delay(600)
            s = drawItems(s, PLAYER, Random.nextInt(1, 4))
            s = drawItems(s, AI, Random.nextInt(1, 4))
            s = s.copy(message = "🎴 双方抽取道具牌")
            state = s
            delay(600)
            val first = Random.nextInt(2)
            s = s.copy(
                currentTurn = first,
                phase = BrPhase.PLAYING,
                round = 1,
                message = if (first == PLAYER) "🎲 你猜拳获胜，先手" else "🎲 人机先手"
            )
            state = s
        }
    }

    // 人机回合
    LaunchedEffect(state.phase, state.currentTurn, state.winner) {
        if (state.phase == BrPhase.PLAYING && state.currentTurn == AI && state.winner == -1) {
            delay(2000)
            state = aiUseItems(state, difficulty)
            var shots = 0
            while (state.winner == -1 && state.currentTurn == AI && shots < 6) {
                delay(500)
                state = brShoot(state, AI, aiChooseTarget(state, difficulty))
                shots++
            }
            if (state.winner == -1 && state.currentTurn == AI) {
                delay(500)
                state = brShoot(state, AI, PLAYER)
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(DarkSurface)
            .statusBarsPadding()
            .padding(horizontal = 20.dp)
    ) {
        GameTopBar(
            title = stringResource(R.string.game_br_title),
            scoreLabel = if (state.phase == BrPhase.PLAYING || state.phase == BrPhase.OVER) {
                stringResource(R.string.game_round, state.round)
            } else "",
            onRestart = { backToSetup() },
            onClose = onClose
        )

        when (state.phase) {
            BrPhase.SETUP -> {
                DifficultySelector(difficulty) { difficulty = it }
                Spacer(Modifier.height(8.dp))
                SetupContent(
                    config = state.config,
                    onConfigChange = ::updateConfig,
                    onStart = { state = state.copy(phase = BrPhase.DEALING) },
                    onShowRules = { showRules = true }
                )
            }
            BrPhase.DEALING -> DealingContent(state)
            BrPhase.PLAYING -> PlayingContent(
                state = state,
                onUseItem = { item -> state = brUseItem(state, PLAYER, item) },
                onShoot = { target -> state = brShoot(state, PLAYER, target) }
            )
            BrPhase.OVER -> OverContent(
                state = state,
                onPlayAgain = { restartSame() },
                onAdjust = { backToSetup() }
            )
        }
    }

    if (showRules) {
        RulesDialog(onDismiss = { showRules = false })
    }
}

// ────────────────────────────── 阶段界面 ──────────────────────────────

@Composable
private fun ColumnScope.SetupContent(
    config: BrConfig,
    onConfigChange: (BrConfig) -> Unit,
    onStart: () -> Unit,
    onShowRules: () -> Unit
) {
    val accent = LocalAccentColor.current

    Text(stringResource(R.string.game_br_setup_hint), color = TextSecondary, fontSize = 13.sp)

    Spacer(Modifier.height(10.dp))

    RangeStepper(
        label = stringResource(R.string.game_br_hp),
        min = config.hpMin, max = config.hpMax,
        hardMin = 1, hardMax = 16,
        onMinChange = { onConfigChange(config.copy(hpMin = it)) },
        onMaxChange = { onConfigChange(config.copy(hpMax = it)) }
    )
    RangeStepper(
        label = stringResource(R.string.game_br_bullets),
        min = config.bulletMin, max = config.bulletMax,
        hardMin = 2, hardMax = 16,
        onMinChange = { onConfigChange(config.copy(bulletMin = it)) },
        onMaxChange = { onConfigChange(config.copy(bulletMax = it)) }
    )
    RangeStepper(
        label = stringResource(R.string.game_br_live),
        min = config.liveMin, max = config.liveMax,
        hardMin = 1, hardMax = 16,
        onMinChange = { onConfigChange(config.copy(liveMin = it)) },
        onMaxChange = { onConfigChange(config.copy(liveMax = it)) }
    )

    Spacer(Modifier.weight(1f))

    Button(
        onClick = onStart,
        colors = ButtonDefaults.buttonColors(containerColor = accent),
        modifier = Modifier
            .fillMaxWidth()
            .height(50.dp)
    ) {
        Text(stringResource(R.string.game_br_start), color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Bold)
    }
    TextButton(onClick = onShowRules, modifier = Modifier.align(Alignment.CenterHorizontally)) {
        Text(stringResource(R.string.game_br_rules), color = TextSecondary, fontSize = 14.sp)
    }
    Spacer(Modifier.height(16.dp))
}

@Composable
private fun ColumnScope.DealingContent(state: BrState) {
    Spacer(Modifier.height(20.dp))
    Text("正在发牌…", color = TextSecondary, fontSize = 14.sp, modifier = Modifier.align(Alignment.CenterHorizontally))
    Spacer(Modifier.height(20.dp))

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        DealCard(
            suit = "♥",
            value = state.dealtHeart,
            label = stringResource(R.string.game_br_hp),
            modifier = Modifier.weight(1f)
        )
        DealCard(
            suit = "♣",
            value = state.dealtBullets,
            label = stringResource(R.string.game_br_bullets),
            modifier = Modifier.weight(1f)
        )
        DealCard(
            suit = "♦",
            value = state.dealtLive,
            label = stringResource(R.string.game_br_live),
            modifier = Modifier.weight(1f)
        )
    }

    Spacer(Modifier.height(24.dp))
    if (state.message.isNotEmpty()) {
        Text(state.message, color = TextPrimary, fontSize = 15.sp, modifier = Modifier.align(Alignment.CenterHorizontally))
    }
    Spacer(Modifier.weight(1f))
}

@Composable
private fun DealCard(suit: String, value: Int?, label: String, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .background(DarkSurfaceVariant)
            .padding(vertical = 18.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(suit, color = Color(0xFFEF5350), fontSize = 30.sp)
        Spacer(Modifier.height(4.dp))
        Text(
            value?.toString() ?: "?",
            color = if (value != null) TextPrimary else TextHint,
            fontSize = 22.sp,
            fontWeight = FontWeight.Bold
        )
        Spacer(Modifier.height(2.dp))
        Text(label, color = TextSecondary, fontSize = 12.sp)
    }
}

@Composable
private fun ColumnScope.PlayingContent(
    state: BrState,
    onUseItem: (BrItem) -> Unit,
    onShoot: (Int) -> Unit
) {
    val accent = LocalAccentColor.current
    val canAct = state.winner == -1 && state.currentTurn == PLAYER && state.chamber.isNotEmpty()

    Spacer(Modifier.height(8.dp))

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Column {
            Row(verticalAlignment = Alignment.CenterVertically) {
                TurnDot(active = state.currentTurn == PLAYER)
                Spacer(Modifier.width(5.dp))
                Text(stringResource(R.string.game_you), color = TextHint, fontSize = 12.sp)
            }
            Text("❤️ ${state.playerHp.coerceAtLeast(0)}", color = TextPrimary, fontSize = 22.sp, fontWeight = FontWeight.Bold)
        }
        Text(
            text = if (state.currentTurn == PLAYER) stringResource(R.string.game_your_turn) else stringResource(R.string.game_ai_turn),
            color = accent,
            fontSize = 13.sp,
            fontWeight = FontWeight.SemiBold
        )
        Column(horizontalAlignment = Alignment.End) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(stringResource(R.string.game_ai), color = TextHint, fontSize = 12.sp)
                Spacer(Modifier.width(5.dp))
                TurnDot(active = state.currentTurn == AI)
            }
            Text("❤️ ${state.aiHp.coerceAtLeast(0)}", color = TextPrimary, fontSize = 22.sp, fontWeight = FontWeight.Bold)
        }
    }

    Spacer(Modifier.height(10.dp))

    val live = state.chamber.count { it }
    val blank = state.chamber.size - live
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text("${stringResource(R.string.game_br_bullets)} ${state.chamber.size}", color = TextSecondary, fontSize = 13.sp)
        Text("${stringResource(R.string.game_br_live)} $live · ${stringResource(R.string.game_br_blank)} $blank", color = TextSecondary, fontSize = 13.sp)
    }

    Spacer(Modifier.height(6.dp))
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        state.chamber.forEach { _ ->
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(6.dp))
                    .background(Color(0xFF2A2A31))
                    .padding(horizontal = 8.dp, vertical = 6.dp)
            ) {
                Text("♠", color = Color(0xFFB0B0BC), fontSize = 13.sp)
            }
        }
    }

    if (state.shotgun) {
        Text("🔫 散弹已上膛：下一发伤害 ×2", color = accent, fontSize = 12.sp, modifier = Modifier.padding(top = 6.dp))
    }
    if (state.bombOwner >= 0) {
        val holder = if (state.bombOwner == PLAYER) stringResource(R.string.game_you) else stringResource(R.string.game_ai)
        Text("💣 定时炸弹在$holder 处", color = ErrorRed, fontSize = 12.sp, modifier = Modifier.padding(top = 4.dp))
    }
    Text(
        text = "${stringResource(R.string.game_br_deck_left, state.itemDeck.size)} · ${stringResource(R.string.game_ai)} ${stringResource(R.string.game_br_items, state.aiItems.size)}",
        color = TextHint,
        fontSize = 12.sp,
        modifier = Modifier.padding(top = 4.dp)
    )
    if (state.message.isNotEmpty()) {
        Text(state.message, color = TextPrimary, fontSize = 13.sp, modifier = Modifier.padding(top = 6.dp))
    }

    Spacer(Modifier.weight(1f))

    Text(stringResource(R.string.game_br_tap_item), color = TextHint, fontSize = 12.sp)
    Spacer(Modifier.height(6.dp))
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        if (state.playerItems.isEmpty()) {
            Text("—", color = TextHint, fontSize = 13.sp, modifier = Modifier.padding(vertical = 8.dp))
        } else {
            state.playerItems.forEach { item ->
                BrItemChip(item, enabled = canAct, onClick = { onUseItem(item) })
            }
        }
    }

    Spacer(Modifier.height(12.dp))

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Button(
            onClick = { onShoot(AI) },
            enabled = canAct,
            colors = ButtonDefaults.buttonColors(containerColor = accent),
            modifier = Modifier.weight(1f)
        ) {
            Text(stringResource(R.string.game_br_shoot_opponent), color = Color.White, fontSize = 14.sp)
        }
        Button(
            onClick = { onShoot(PLAYER) },
            enabled = canAct,
            colors = ButtonDefaults.buttonColors(containerColor = DarkSurfaceVariant),
            modifier = Modifier.weight(1f)
        ) {
            Text(stringResource(R.string.game_br_shoot_self), color = TextPrimary, fontSize = 14.sp)
        }
    }
}

@Composable
private fun ColumnScope.OverContent(state: BrState, onPlayAgain: () -> Unit, onAdjust: () -> Unit) {
    val accent = LocalAccentColor.current
    Spacer(Modifier.height(20.dp))
    Text(
        text = if (state.winner == PLAYER) stringResource(R.string.game_win) else stringResource(R.string.game_lose),
        color = if (state.winner == PLAYER) accent else ErrorRed,
        fontSize = 26.sp,
        fontWeight = FontWeight.Bold,
        modifier = Modifier.align(Alignment.CenterHorizontally)
    )
    Text(
        text = "❤️ ${state.playerHp.coerceAtLeast(0)}  ·  ❤️ ${state.aiHp.coerceAtLeast(0)}",
        color = TextSecondary,
        fontSize = 14.sp,
        modifier = Modifier
            .align(Alignment.CenterHorizontally)
            .padding(top = 6.dp)
    )
    Spacer(Modifier.weight(1f))
    Button(
        onClick = onPlayAgain,
        colors = ButtonDefaults.buttonColors(containerColor = accent),
        modifier = Modifier
            .fillMaxWidth()
            .height(50.dp)
    ) {
        Text(stringResource(R.string.game_br_play_again), color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Bold)
    }
    TextButton(onClick = onAdjust, modifier = Modifier.align(Alignment.CenterHorizontally)) {
        Text(stringResource(R.string.game_br_adjust), color = TextSecondary, fontSize = 14.sp)
    }
    Spacer(Modifier.height(16.dp))
}

@Composable
private fun TurnDot(active: Boolean) {
    Box(
        modifier = Modifier
            .size(8.dp)
            .clip(CircleShape)
            .background(if (active) LocalAccentColor.current else TextHint.copy(alpha = 0.35f))
    )
}

@Composable
private fun BrItemChip(item: BrItem, enabled: Boolean, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(10.dp))
            .background(if (enabled) LocalAccentColor.current.copy(alpha = 0.16f) else DarkSurfaceVariant)
            .clickable(enabled = enabled) { onClick() }
            .padding(horizontal = 12.dp, vertical = 8.dp)
    ) {
        Text(
            text = stringResource(item.labelRes),
            color = if (enabled) LocalAccentColor.current else TextSecondary,
            fontSize = 13.sp,
            fontWeight = FontWeight.Medium
        )
    }
}

@Composable
private fun RangeStepper(
    label: String,
    min: Int,
    max: Int,
    hardMin: Int,
    hardMax: Int,
    onMinChange: (Int) -> Unit,
    onMaxChange: (Int) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(label, color = TextPrimary, fontSize = 13.sp, modifier = Modifier.width(48.dp))
        // 最小值：[hardMin, max]
        Stepper(min, hardMin, max, onMinChange)
        Text("~", color = TextHint, fontSize = 14.sp, modifier = Modifier.padding(horizontal = 8.dp))
        // 最大值：[min, hardMax]
        Stepper(max, min, hardMax, onMaxChange)
    }
}

@Composable
private fun Stepper(value: Int, lo: Int, hi: Int, onChange: (Int) -> Unit) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        StepButton("−", value > lo) { onChange(value - 1) }
        Text(
            text = value.toString(),
            color = LocalAccentColor.current,
            fontSize = 14.sp,
            fontWeight = FontWeight.SemiBold,
            textAlign = TextAlign.Center,
            modifier = Modifier.width(34.dp)
        )
        StepButton("+", value < hi) { onChange(value + 1) }
    }
}

@Composable
private fun StepButton(text: String, enabled: Boolean, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .size(28.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(if (enabled) DarkSurfaceVariant else DarkSurfaceVariant.copy(alpha = 0.4f))
            .clickable(enabled = enabled) { onClick() },
        contentAlignment = Alignment.Center
    ) {
        Text(text, color = if (enabled) TextPrimary else TextHint, fontSize = 16.sp, fontWeight = FontWeight.Bold)
    }
}

@Composable
private fun RulesDialog(onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.game_br_rules), color = TextPrimary) },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
            ) {
                Text(RULES_TEXT, color = TextPrimary, fontSize = 13.sp, lineHeight = 20.sp)
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.common_close), color = LocalAccentColor.current, fontSize = 14.sp)
            }
        },
        containerColor = DarkSurface,
        shape = RoundedCornerShape(16.dp)
    )
}

// ────────────────────────────── 游戏逻辑 ──────────────────────────────

private fun initialBrState(config: BrConfig = BrConfig()): BrState =
    BrState(phase = BrPhase.SETUP, config = config)

private fun newItemDeck(): List<BrItem> {
    val deck = mutableListOf<BrItem>()
    fun add(item: BrItem, count: Int) = repeat(count) { deck.add(item) }
    add(BrItem.MAGNIFIER, 5)
    add(BrItem.CIGARETTE, 5)
    add(BrItem.BEER, 5)
    add(BrItem.SHOTGUN, 5)
    add(BrItem.HANDCUFFS, 4)
    add(BrItem.ADRENALINE, 4)
    add(BrItem.CONVERTER, 4)
    add(BrItem.PHONE, 2)
    add(BrItem.BOMB, 1)
    add(BrItem.MEDICINE, 2)
    return deck.shuffled()
}

private fun buildChamber(bullets: Int, live: Int): List<Boolean> =
    (List(live) { true } + List(bullets - live) { false }).shuffled()

private fun drawItems(s: BrState, who: Int, count: Int): BrState {
    if (count <= 0 || s.itemDeck.isEmpty()) return s
    val deck = s.itemDeck.toMutableList()
    val hand = (if (who == PLAYER) s.playerItems else s.aiItems).toMutableList()
    var remaining = count
    var guard = 0
    val maxGuard = deck.size + 4
    while (remaining > 0 && deck.isNotEmpty() && guard < maxGuard) {
        val card = deck.removeAt(0)
        if (hand.count { it == card } >= 2) {
            deck.add(card) // 该道具已达每人上限，放回牌堆底部
        } else {
            hand.add(card)
            remaining--
        }
        guard++
    }
    return if (who == PLAYER) s.copy(itemDeck = deck.toList(), playerItems = hand.toList())
    else s.copy(itemDeck = deck.toList(), aiItems = hand.toList())
}

private fun removeOne(items: List<BrItem>, item: BrItem): List<BrItem> =
    items.toMutableList().apply { remove(item) }.toList()

private fun brUseItem(st: BrState, who: Int, item: BrItem): BrState {
    val opp = 1 - who
    var s = if (who == PLAYER) st.copy(playerItems = removeOne(st.playerItems, item))
    else st.copy(aiItems = removeOne(st.aiItems, item))
    s = when (item) {
        BrItem.MAGNIFIER -> {
            val first = s.chamber.firstOrNull()
            s.copy(message = when (first) { true -> "🔍 第一发是实弹"; false -> "🔍 第一发是空弹"; null -> "🔍 弹膛已空" })
        }
        BrItem.CIGARETTE -> brHeal(s, who, 1).copy(message = "🚬 回 1 点血")
        BrItem.BEER -> if (s.chamber.isNotEmpty()) s.copy(chamber = s.chamber.drop(1), message = "🍺 退掉第一发子弹") else s
        BrItem.SHOTGUN -> s.copy(shotgun = true, message = "🔫 散弹上膛，下一发伤害 ×2")
        BrItem.HANDCUFFS -> s.copy(handcuffed = true, message = "⛓️ 对方下个回合将被跳过")
        BrItem.ADRENALINE -> {
            val oppHand = if (who == PLAYER) s.aiItems else s.playerItems
            if (oppHand.isEmpty()) s.copy(message = "💉 对方没有道具可偷")
            else {
                val stolen = oppHand[Random.nextInt(oppHand.size)]
                if (who == PLAYER) s.copy(aiItems = removeOne(s.aiItems, stolen), playerItems = s.playerItems + stolen, message = "💉 偷到对方一张道具")
                else s.copy(playerItems = removeOne(s.playerItems, stolen), aiItems = s.aiItems + stolen, message = "💉 对方偷走你一张道具")
            }
        }
        BrItem.CONVERTER -> {
            if (s.chamber.isNotEmpty()) s.copy(chamber = s.chamber.toMutableList().apply { this[0] = !this[0] }.toList(), message = "🔁 第一发被调换")
            else s
        }
        BrItem.PHONE -> {
            val n = Random.nextInt(2, 9).coerceAtMost(s.chamber.size)
            if (n <= 0) s.copy(message = "📱 弹膛已空")
            else s.copy(message = "📱 第 $n 发是${if (s.chamber[n - 1]) "实弹" else "空弹"}")
        }
        BrItem.BOMB -> s.copy(bombOwner = opp, message = "💣 炸弹交给了对方")
        BrItem.MEDICINE -> {
            if (Random.nextFloat() < 0.5f) brHeal(s, who, 2).copy(message = "💊 过期药起效 +2")
            else brDamage(s, who, 1).copy(message = "💊 过期药失效 -1")
        }
    }
    return s
}

private fun brShoot(st: BrState, who: Int, target: Int): BrState {
    var s = if (st.chamber.isEmpty()) reloadIfNeeded(st) else st
    if (s.chamber.isEmpty()) return s
    val wasShotgun = s.shotgun
    val live = s.chamber.first()
    val dmg = if (wasShotgun) 2 else 1
    s = s.copy(chamber = s.chamber.drop(1), shotgun = false)
    if (live) {
        s = brDamage(s, target, dmg)
        s = s.copy(message = "💥 实弹！-$dmg")
    } else {
        s = s.copy(message = "💨 空弹")
    }
    s = checkWinner(s)
    if (s.winner != -1) return s
    val passTurn = live || target != who
    s = if (passTurn) brNextTurn(s, who) else reloadIfNeeded(s.copy(currentTurn = who))
    return s
}

private fun brNextTurn(s: BrState, fromWho: Int): BrState {
    var st = s
    val to = 1 - fromWho
    if (st.bombOwner == to) {
        if (Random.nextFloat() < 0.25f) {
            st = brDamage(st, to, 2).copy(bombOwner = -1, message = "💥 炸弹爆炸！-2")
        } else {
            st = st.copy(bombOwner = fromWho, message = "⏳ 炸弹没炸，转交给了对方")
        }
        st = checkWinner(st)
        if (st.winner != -1) return st
    }
    if (st.handcuffed) {
        st = st.copy(handcuffed = false, currentTurn = fromWho, message = "⛓️ 被手铐铐住，跳过回合")
    } else {
        st = st.copy(currentTurn = to)
    }
    return reloadIfNeeded(st)
}

private fun reloadIfNeeded(s: BrState): BrState {
    if (s.winner != -1 || s.chamber.isNotEmpty()) return s
    val bullets = Random.nextInt(s.config.bulletMin, s.config.bulletMax + 1)
    val live = minOf(Random.nextInt(s.config.liveMin, s.config.liveMax + 1), bullets)
    var st = s.copy(chamber = buildChamber(bullets, live), round = s.round + 1)
    st = drawItems(st, PLAYER, Random.nextInt(1, 4))
    st = drawItems(st, AI, Random.nextInt(1, 4))
    return st.copy(message = "🔄 重新装弹")
}

private fun brDamage(s: BrState, who: Int, amount: Int): BrState =
    if (who == PLAYER) s.copy(playerHp = s.playerHp - amount) else s.copy(aiHp = s.aiHp - amount)

private fun brHeal(s: BrState, who: Int, amount: Int): BrState =
    if (who == PLAYER) s.copy(playerHp = (s.playerHp + amount).coerceAtMost(s.maxHp))
    else s.copy(aiHp = (s.aiHp + amount).coerceAtMost(s.maxHp))

private fun checkWinner(s: BrState): BrState = when {
    s.playerHp <= 0 -> s.copy(winner = AI, phase = BrPhase.OVER)
    s.aiHp <= 0 -> s.copy(winner = PLAYER, phase = BrPhase.OVER)
    else -> s
}

// ────────────────────────────── 人机 AI ──────────────────────────────

private fun aiUseItems(s: BrState, difficulty: AiDifficulty): BrState {
    var st = s
    if (st.aiItems.isEmpty()) return st
    val maxUses = when (difficulty) {
        AiDifficulty.CASUAL -> if (Random.nextFloat() < 0.35f) 1 else 0
        AiDifficulty.CHALLENGE -> 2
        AiDifficulty.DUEL -> 3
    }
    var used = 0
    for (item in aiItemOrder(st, difficulty)) {
        if (used >= maxUses) break
        if (item !in st.aiItems) continue
        st = brUseItem(st, AI, item)
        used++
    }
    return st
}

private fun aiItemOrder(s: BrState, difficulty: AiDifficulty): List<BrItem> {
    if (difficulty == AiDifficulty.CASUAL) return BrItem.entries.shuffled()
    val live = s.chamber.count { it }
    val blank = s.chamber.size - live
    val lowHp = s.aiHp < s.maxHp
    val order = mutableListOf<BrItem>()
    if (live > blank) { order += BrItem.CONVERTER; order += BrItem.BEER }
    if (live > 0) order += BrItem.SHOTGUN
    if (lowHp) { order += BrItem.MEDICINE; order += BrItem.CIGARETTE }
    order += BrItem.MAGNIFIER
    order += BrItem.PHONE
    order += BrItem.HANDCUFFS
    if (s.playerItems.isNotEmpty()) order += BrItem.ADRENALINE
    order += BrItem.BOMB
    order += BrItem.entries.filterNot { it in order }
    return order
}

private fun aiChooseTarget(s: BrState, difficulty: AiDifficulty): Int {
    val size = s.chamber.size
    if (size == 0) return PLAYER
    val live = s.chamber.count { it }
    val pLive = live.toDouble() / size
    return when (difficulty) {
        AiDifficulty.CASUAL -> if (Random.nextFloat() < 0.8f) PLAYER else AI
        AiDifficulty.CHALLENGE -> if (pLive >= 0.5) PLAYER else AI
        AiDifficulty.DUEL -> {
            if (s.playerHp <= 1) PLAYER
            else if (s.aiHp <= 1 && pLive < 0.5) AI
            else if (pLive >= 0.5) PLAYER
            else AI
        }
    }
}
