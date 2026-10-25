# 「高级感」UI 打磨设计提案

日期：2026-10-25
状态：**已批准并实现**（P0/P1 全部 + P2 部分落地；余项见文末「进度」）
前提：B站 + YouTube 视频音频已上线；正常/沉浸双播放页已就位；主题/字型/强调色系统已有底子（`AppColors` 语义色 + `ThemePreset` 14 强调色 + Apple 风字型 + `appleClickable`/`pressScale`/`DynamicIsland`）。

## 0. 目标

不是重做，而是把已有的「零件」统一成一套有质感、可复用的系统，再逐页套用。核心一句话：**同一套令牌（token）、同一套动效曲线、同一套组件**，消除各页 `12.dp / 14.dp / 16.dp` 与 `0.45f / 0.6f / 0.7f` 的随意值。

## 1. 设计令牌（唯一事实来源）

新增 `ui/theme/Tokens.kt`，集中定义，各页引用而非硬编码。

| 令牌 | 值 | 说明 |
|---|---|---|
| 圆角 `Radius.sm / md / lg / xl` | 10 / 14 / 20 / 28 dp | 卡片/输入框/面板/全屏弹层 |
| 间距 `Spacing.xs/sm/md/lg/xl` | 4/8/12/16/24 dp | 统一留白节奏 |
| 描边 `Stroke.hairline` | 1 dp `textPrimary.copy(alpha=0.08f)` | 深色卡片的白描边（磨砂感） |
| 阴影 `Elevation.card` | 0 dp（深色以描边替代阴影） | 深色 UI 阴影易显脏，改用描边+层次 |
| 按压 `Press.scale` | 0.96f（已有 `appleClickable`） | 统一点按回弹 |
| 磨砂 `Glass.card` | `surface.copy(alpha=0.45f)` + hairline 描边 | 悬浮条/底部栏 |
| 时长 | `Motion.quick 160ms / standard 240ms / slow 360ms` | 微交互/常规/页面级 |
| 曲线 | `spring(MediumBouncy)` 已有；常规用 `tween` + `FastOutSlowInEasing` | 回弹只给「可点按」元素 |

## 2. 四个方向落地清单

### A. 全局统一质感（先做，收益最高）

1. **Token 化**：把各页散落的 `RoundedCornerShape(10/12/14/16/20.dp)`、`padding(16.dp)`、`alpha` 收口到 `Tokens.kt`；`MusicSourceSettingsScreen`、`ProfileScreen`、`SettingsScreen` 的卡片圆角/描边统一为 `Radius.md` + `Stroke.hairline`。
2. **卡片统一**：所有 `Card`（账号、发现设置、设置项）加一层 `surfaceVariant` 到 `surface` 的微妙层次 + hairline 描边，去掉生硬纯色块感。
3. **文字层级**：标题一律 `SemiBold`（已有字型），正文 `bodyMedium`；次要信息统一 `TextSecondary` 12sp，占位/说明统一 `TextHint` 12sp。消除各页 `13.sp` 与 `12.sp` 混用。
4. **强调色一致性**：所有「可点按高亮」统一 `LocalAccentColor.current`（现已基本统一，扫一遍遗漏项：如进度条、开关、选中态）。

### B. 播放页视觉

1. **沉浸模式**：
   - 背景 `AudioReactiveBackground` 双色调：封面取色改为 `vibrant` + `darkMuted`（已有），把次色呼吸振幅从 `0.5f` 级微调到更克制的 `0.35f`，避免抢封面。
   - 悬浮条 `FoliaCommandBar`：磨砂统一 `Glass.card`（`surface.copy(alpha=0.45f)` + hairline），进度条改细横条 2dp + 拖动圆点 10dp（白色 + 淡光晕）。
   - 逐词歌词：当前字发光改用 accent 色 halo（`graphicsLayer` 外发光），非当前字降到 `TextSecondary`，KTV 式对比更高级。
2. **正常模式**（`MusicNormalScreen`）：
   - 顶部封面加「取色晕影」：封面下方叠一层取色渐变到页面底色，避免封面边缘生硬切线。
   - 音效器面板（上滑页）：耳机品牌 chip / 混响 chip 统一 `Radius.md` + accent 选中态；分区块标题用 `SectionTitle` 统一 16sp SemiBold。
   - 歌曲信息 / 歌曲笔记：输入框统一 `OutlinedTextField` 圆角 10dp、focused accent，笔记计数「n/400」用 `TextHint` 对齐右侧。

### C. 动效与转场

1. **点按统一**：列表行 / 卡片 / 按钮全部 `appleClickable`（已有），替换残留的裸 `clickable`（`DiscoverScreen`、`ProfileScreen` 大量仍用裸 `clickable`）。
2. **面板展开**：音效器面板（`VerticalPager` 上滑）已用分页，补「松手吸附」阻尼提示；歌单/榜单展开（`ChartHeader`/`PlaylistRow` chevron）加 `animateContentSize`，chevron 用 `animateFloatAsState` 旋转 90°。
3. **列表入场**：`LazyColumn` 首屏推荐/搜索结果用 `animateItemPlacement`（Compose 1.7 有 `animateItem`）做轻量 stagger 淡入（仅首屏一次，不干扰滚动）。
4. **页面级**：三页 `HorizontalPager` 导航与详情页（歌手/歌单详情）转场统一为 `slideInHorizontally + fadeIn`（快 240ms），返回反向；避免系统默认瞬间跳变。

### D. 发现页 / 列表

1. **歌曲行**：音乐行统一 `Radius.md` 圆角封面 48dp；视频行封面 112×63 → 统一 `Radius.sm`；时长角标统一（黑 0.7f + 白字 10sp，`Radius.sm`）。
2. **视频架**：`VideoCard` 封面高度 84dp → 按 16:9 固定（150 宽 → 84.4 高，取 84dp 已合理）；标题 2 行 12sp、上传者 11sp `TextSecondary`；卡片整体加 hairline 描边，hover 无（移动端）。
3. **榜单/歌单**：`ChartHeader`/`PlaylistRow` 展开态加 accent 左侧竖条（3dp）标记当前展开项；行高统一，chevron 旋转动效。
4. **换平台对话框**：`PlatformSwitchDialog` 每个平台行加「当前」accent 圆点（选中态），已登录用 `successGreen` 圆点 + 「已登录」，匿名用 `TextHint` 「匿名」；去掉纯文字堆叠。

## 3. 实施顺序与验收

- **P0（令牌 + 全局统一质感）**：`Tokens.kt` + 全局替换卡片/圆角/文字层级。验收：各页圆角/间距/描边一致，无散落 magic number。
- **P1（发现页/列表 + 播放页）**：A/B/D 逐页套令牌 + 行/卡片/架/面板质感。验收：真机截图对比「统一、克制、有层次」。
- **P2（动效）**：点按/展开/列表入场/页面转场。验收：无跳变、回弹只在可点按元素、转场流畅 60fps。
- 每阶段 commit+push；真机验收（OPPO 设备）逐阶段确认后再进下一阶段。

## 4. 明确不做（避免过度设计）

- 不引入新第三方动画库（现有 Compose 动画足够）。
- 不做 GPU shader 级特效（Folia 的 Pixi/Three 无法移植，按「概念复刻」已实现双色调/可视化）。
- 不改信息架构/交互流程，只改视觉与动效。

## 5. 进度

- **P0 ✅**（`5262aae`）：`Tokens.kt` 设计令牌 + 全局卡片统一（Settings / Profile / MusicSourceSettings 卡片圆角 `Radius.md` + `HairlineBorder`）。
- **P1 ✅**（`d234c09`）：
  - 发现页 D3/D4：换平台对话框状态圆点（accent / 已登录绿 / 匿名灰）、ChartHeader/PlaylistRow accent 竖条 + 动画 chevron。
  - 播放页 B1/B2：沉浸模式次色呼吸 0.44→0.35 更克制、悬浮条 `Glass.card` 磨砂、进度条细横条 2dp + 白圆点淡光晕；正常模式封面取色晕影、音效器 chip 统一 `Radius.md`、分区块标题 16sp、笔记输入框 10dp 圆角 + 计数右对齐。
- **P2 部分 ✅**（`7f6be78`）：C1 点按统一 —— DiscoverScreen / ProfileScreen 残留裸 `clickable` 全部替换 `appleClickable`。
- **暂缓（待真机验收）**：
  - C2 子列表展开动画（榜单/歌单展开的歌曲子列表目前硬显隐；chevron 旋转已就位，`animateContentSize` 因 LazyColumn 子项性能取舍暂缓）。
  - C3 列表入场 stagger（`animateItem` 仅首屏一次，Foundation 1.7 可做，待设备确认不干扰滚动）。
  - C4 页面级转场（HorizontalPager 导航 + 详情页 slide+fade，改动面大，待设备确认）。
