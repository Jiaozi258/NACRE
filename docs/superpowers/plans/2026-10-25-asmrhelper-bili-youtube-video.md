# 音理音理（NeriPlayer）功能：B站 + YouTube 视频音频

日期：2026-10-25
目标：给 NACRE 加入 B站 / YouTube「视频音频」能力——发现页切换平台、浏览/搜索视频（封面 + 名称/上传者/发布时间/时长）、点按播放音频+封面（后台播放，不放视频）、账号并入「管理个人账号」，并保留/改进现有视频链接提取。

## 已拍板决策（用户）
- 平台：B站（WBI 取流）+ YouTube（InnerTube 音频直链）。
- B站登录：扫码 + 账号密码都要。
- YouTube：先匿名取流，登录之后再加。
- 播放形态：音频 + 封面，不放视频；支持后台播放。
- 发现页在 B / Y 之间切换。
- 歌曲笔记（歌评 ≤400 字）已在正常模式完成。
- 约束：软件内不出现任何其他软件名字（不用「音理音理/NeriPlayer/Salt/Folia」等，用中性词「视频音频」等）。

## 架构决策
- B站 / YouTube 作为 **`MusicSource` 实现**（`BilibiliMusicSource`、`YoutubeMusicSource`），把「视频」映射为 `RemoteSong`：
  - `title`=视频名、`artist`=上传者、`coverUrl`=封面、`durationMs`=时长、`publishTime`=发布时间、`id`=平台视频 id（B站 bvid / YouTube videoId）、`sourceId`=`"bilibili"`/`"youtube"`。
  - 复用现有：搜索、队列、播放、收藏、历史、登录态、账号管理、发现页模块、`playSongs`。
  - 音乐专属方法（歌词/歌手/歌单/榜单/每日推荐）默认返回空。
- 视频专属 UI：发现页在活动源为 bilibili/youtube 时，搜索结果与推荐用「视频行」（封面 + 名称/上传者/发布时间/时长），并加横滑热门视频架。
- 现有 `VideoAudioExtractor`（链接提取）保留不动；B站取流统一改用 WBI 签名实现，YouTube 改直连 InnerTube（不再只靠 Invidious 代理）。

## 里程碑

### A. B站 WBI + `BilibiliMusicSource`
1. `data/music/bilibili/BilibiliWbi.kt`：mixin key 计算（MIXIN_KEY_ENC_TAB 64 表 + MD5）+ `signParams(params)`。
2. `data/music/bilibili/BilibiliJson.kt`：org.json 纯解析（搜索/热门/播放地址/详情，异常返回空）。
3. `data/music/bilibili/BilibiliMusicSource.kt`：`search`（wbi/search/type）、`getRecommendations`（popular）、`getSongUrl`（view 取 cid + wbi/playurl 取 dash.audio）、`getCoverUrl`/`getSongDetail`（view）、登录（WebView，`SESSDATA`）、logout、resolveAccountNickname。
4. `SourcePresets` 加 `BILIBILI`；`MusicSourceManager.sourceFor` 加 `"bilibili"` 分支。
5. 字符串（`source_bilibili`、视频行标签等，中/英）。

### B. YouTube InnerTube + `YoutubeMusicSource`
1. `data/music/youtube/YoutubeInnerTube.kt`：ANDROID client context + player/search/browse，取 `streamingData.adaptiveFormats` 音频（优先 itag 140/251，ANDROID 直链免 decipher）。
2. `data/music/youtube/YoutubeJson.kt` + `YoutubeMusicSource`：`search`、`getRecommendations`（trending）、`getSongUrl`、`getCoverUrl`、匿名（无登录）。
3. 注册源 + 字符串。

### C. 发现页 B/Y 切换 + 视频行 + 播放
1. 发现页平台切换入口扩展到 4 源（网易云/QQ/B站/YouTube）。
2. 视频源时：搜索结果显示「视频行」（封面 + 名称/上传者/发布时间/时长）；推荐模块渲染横滑视频架。
3. 播放：`playSongs` 走现有队列（`getSongUrl` 返回音频流 + 封面）。

### D. 账号管理 + 视频链接提取收敛
1. 「管理个人账号」自动纳入 B/Y（复用 `MusicSourceSettingsScreen` 遍历 `sources`）。
2. 确认 `VideoAudioExtractor` 与 B站 WBI/YouTube InnerTube 不冲突；必要时统一切换到 WBI/InnerTube 取流。

## 验收
- B站/YouTube 匿名可搜视频、浏览热门、点按后台播放音频+封面。
- B站 WebView 登录后带 `SESSDATA`，登录态持久化，账号管理可退出。
- 字符串中/英 parity（`StringResourcesTest` 守卫）。
- 构建 + 单测全绿，每大步 commit+push。
