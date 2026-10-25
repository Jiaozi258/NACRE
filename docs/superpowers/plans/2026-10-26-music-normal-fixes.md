# 正常模式播放页 + 发现页 + 图标 修复计划

日期：2026-10-26
状态：实施中
前提：B站/YouTube 视频音频已上线；正常/沉浸双播放页就位；设计令牌（`Tokens.kt`/`GlassSurface`/`HairlineBorder`/`appleClickable`）已建立。

## 0. 用户反馈（本次要修的全部问题）

1. **正常模式三页**：
   - 默认（中）页：控制栏简陋、多了一个「切回沉浸」按钮（SwapVert，下滑页已有）要删；进度条大横杠长竖条难看→改细；歌词占太多空间→减少；没有菜单按钮、没有播放队列按钮→加上；背景默认难看→封面取色/磨砂；菜单内容与沉浸一致 + 多加「写歌评」。
   - 上滑页：全是歌词、控制栏没了→加上并与默认一致；歌词无法点击跳转→可点击跳转。
   - 下滑页：耳机适配 + 混响统一放进「均衡器」；混响与沉浸「场景效果」没区别→实现独立 **V3 混响**（更真实立体）；歌曲信息歌手/专辑加小图 + 点击跳转；歌曲笔记不要直接展开→折叠成选项。
2. **发现页 bug**：切到 B站/YouTube 仍显示推荐/每日/排行榜/歌单——应只提供「收藏夹/动态」，或什么都不显示；搜索里只提供「视频/博主」。YouTube 加载失败（不好修先搁置）。
3. **UI 质感**：建议菜单、弹窗可在设置里调成「透明磨砂」；背景纯色 vs 透明磨砂可选。
4. **图标**：emoji 图标（如列表循环的橙色方块包白循环）难看，改单独白色循环图样，其他图标同理。

## 1. 分阶段实施

### Phase 1 — 正常模式默认页 + 控制栏（`MusicNormalScreen.kt`）
- 背景：整页封面取色晕影 + 磨砂（复用 `rememberCoverPalette` + 模糊封面）。
- `NormalControls`：删 SwapVert；进度条改细（复用 `FoliaCommandBar` 的 2dp 轨道 + 16dp 白色光晕圆点 + 10dp 白点）；加 `MoreVert` 菜单 + 播放队列按钮；循环图标改纯白循环（NONE 不再用 `Stop` 方块，用 `Repeat` 降透明）。
- 布局权重：歌词 `0.34` → `0.26`，封面 `0.42` → `0.46`，控制栏下移占满剩余。
- 菜单弹窗：与沉浸 `MusicPlayScreen` 的更多菜单一致 + 追加「写歌评」；加播放队列弹窗。
- 提交 + push。

### Phase 2 — 上滑页（全屏歌词）+ 下滑页（音效器）
- `FullScreenLyrics`：底部加与默认一致的控制栏；`ScrollingLyrics` 每行 `clickable` 点击跳转到该句时间。
- `EqualizerPanel`：耳机适配 + 混响合并进「均衡器」；歌曲笔记折叠成「查看/编辑歌评」；歌曲信息歌手/专辑加小图。
- 提交 + push。

### Phase 3 — V3 混响（`EnvironmentalReverb`）
- 新增独立混响控制器，用 `android.media.audiofx.EnvironmentalReverb`（区别于场景效果的 `PresetReverb`）；提供多个「V3 混响」预设（更真实立体）。接入正常模式下滑页均衡器区。
- 提交 + push。

### Phase 4 — 发现页平台 bug（`DiscoverViewModel` / `DiscoverScreen`）
- 按平台过滤模块：B站/YouTube 只显示「收藏夹/动态」；搜索只显示「视频/博主」。YouTube 加载失败先搁置。
- 提交 + push。

### Phase 5 — 透明磨砂设置 + 去 emoji
- `SettingsRepository` 加「透明磨砂」开关；`SettingsScreen` 外观与主题加开关。
- 菜单/弹窗背景在开启时用 `GlassSurface` + `HairlineBorder`。
- 去除菜单/循环标签的 emoji 前缀；循环图标纯白。
- 提交 + push。

## 2. 明确不做 / 暂缓

- YouTube 加载失败（国内不可访问，先搁置）。
- 歌手/专辑主页跳转若需大规模数据模型扩展，先出「小图展示」版本，跳转做搜索兜底。
