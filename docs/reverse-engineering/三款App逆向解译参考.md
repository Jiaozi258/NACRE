# 三款 App 逆向解译参考（供 NACRE 实现借鉴）

> 来源：`E:\My_Documents\github\other_projects` 下的三个 APK，通过 DEX 字符串扫描 + 源码拉取（NeriPlayer 开源）精确到函数级还原。
> 日期：2026-10-25

---

## 0. 结论速览

| App | 本质 | 对 NACRE 的借鉴点 |
|-----|------|------------------|
| 只音 (`zhiyin.apk.1`, 41MB) | 配置驱动的 4 音源聚合抓取器 + Aria 下载器 | 免费下歌的「源选择 + 远程配置 + 直接下载」思路 |
| 音理音理 (`yinliyinli.1`, 14MB) | 开源 **NeriPlayer**：InnerTube + yt-dlp ejs 求解器 + NewPipe + FFmpeg，B站 WBI 签名 + web ticket | **可完整复制**；B站取流 + YouTube 音频直链已精确到函数级 |
| Salt Player (`salt_player.1`, 12MB) | Compose Multiplatform + BASS/BASS_FX 音频 DSP | 播放页布局、歌词滚动、"V3 混响"/"MAX Audio" = BASS_FX 效果器 |

**两个核心问题答案：**

1. **音理音理（NeriPlayer）能否在 NACRE 复制？** 能。B站取流走官方 `wbi/playurl` + WBI 签名（纯算法，无 JNI）；YouTube 走 InnerTube `player` 接口 + `n`/`sig` 反混淆（yt-dlp ejs 求解器 + NewPipe 兜底）。两端点都是公开 HTTP 接口 + 可移植算法。

2. **「拿到音频直链」 vs 「下载视频再抽音频」哪个更快更稳？** **音频直链更快更稳。** YouTube 直接返回 `adaptiveFormats` 里的纯音频轨（itag 139/140/141/249/250/251）；B站 `wbi/playurl` 直接下发 `dash.audio`/`dolby.audio`/`flac.audio` 纯音频。无需 FFmpeg 抽流、无需下载整段视频，省带宽省时间。

---

## 1. 只音（zhiyin.apk）——免费下歌的实现方式

**架构**：`com.zhiyin.*` 多内容聚合器（music/comic/novel/video/toolbox 五合一）。

**音乐源**：酷狗(kg) / 酷我(kw) / QQ(qq) / 网易云(wyy) 四源。

**核心机制**：
- **配置驱动**：抓取逻辑不是写死在客户端，而是由远程后端 `6wm.top`（`c-nd2-1`、`c-nd3-1`、`t-nd2-1`、`t-nd3-1`）和 `v2.apikk.top` 下发接口/参数/路径规则，客户端按配置去拼请求。
- **下载器**：`com/arialyy/aria`（Aria 下载框架）负责多任务下载。
- **风控特征**：DEX 里存在字符串加密（doubleReflector/JNI）、ProGuard/R8 混淆。

**给 NACRE 的启示**：NACRE 若要「免费下歌」，本质是**音源接口选择器 + 一个可靠的下载队列**，接口可从服务端热更新，避免客户端硬编码失效。⚠️ 注意：此类「免费下歌」接口通常绕过正版授权，属于灰色地带，NACRE 若上线应评估合规风险。

---

## 2. 音理音理（yinliyinli）== NeriPlayer（GPLv3，开源可读）

源码：`github.com/cwuom/NeriPlayer`（本仓库已 clone 到 `E:\My_Documents\github\other_projects\NeriPlayer`）。

**技术栈**：InnerTube API（`youtubei/v1/`）、yt-dlp ejs 生成的 JS 密码学求解器（Rhino `org.mozilla.javascript.*` + Android `JavaScriptSandbox`）、NewPipe Extractor 兜底、FFmpeg（`libffmpegJNI.so`）、B站 WBI 签名 + web ticket、网易云 weapi、酷狗 KRC 歌词。

---

### 2.1 B站 WBI 签名（精确到函数级）

关键文件：`modules/platform/src/main/java/moe/ouom/neriplayer/platform/bilibili/api/client/BiliClient.kt`

#### 常量（BiliClient.kt:95-194）

```kotlin
BASE_PLAY_URL  = "https://api.bilibili.com/x/player/wbi/playurl"   // 取流
NAV_URL        = "https://api.bilibili.com/x/web-interface/nav"    // 拿 mixin key 的 img/sub
WEB_TICKET_URL = "https://api.bilibili.com/bapis/bilibili.api.ticket.v1.Ticket/GenWebTicket" // 兜底
VIEW_URL       = "https://api.bilibili.com/x/web-interface/wbi/view"
SEARCH_TYPE_URL= "https://api.bilibili.com/x/web-interface/wbi/search/type"
WEB_TICKET_KEY = "XgwSnGZ1p"                                       // web ticket HMAC key
```

#### WBI 混排索引表 `MIXIN_INDEX`（BiliClient.kt:153-158，64 元素）

```kotlin
intArrayOf(
  46,47,18,2,53,8,23,32,15,50,10,31,58,3,45,35,
  27,43,5,49,33,9,42,19,29,28,14,39,12,38,41,13,
  37,48,7,16,24,55,40,61,26,17,0,1,60,51,30,4,
  22,25,54,21,56,62,6,63,57,20,34,52,59,11,36,44
)
```

#### 取流参数 `PlayOptions`（BiliPlaybackModels.kt:5-28）

```kotlin
FNVAL_DASH  = 1 shl 4   // = 16
FNVAL_DOLBY = 1 shl 8   // = 256
// 默认 fnval = 16 | 256 = 272，确保同时下发普通音轨 + 杜比音轨
data class PlayOptions(
  qn: Int? = null,
  fnval: Int = FNVAL_DASH or FNVAL_DOLBY,
  fnver: Int = 0,
  fourk: Int = 0,
  platform: String = "pc",      // pc 需 Referer；html5 无 Referer 校验但仅 MP4
  highQuality: Int? = null,
  tryLook: Int? = null,
  ...
)
```

#### `signWbiUrl(base, paramsIn)`（BiliClient.kt:1157-1177）——完整算法

```kotlin
private suspend fun signWbiUrl(base: String, paramsIn: Map<String, String>): HttpUrl {
    val mixinKey = getOrRefreshMixinKey()                    // 1. 拿 mixin key（10min 缓存）
    val params = paramsIn.mapValues { (_, v) -> filterValue(v) }.toMutableMap() // 2. 过滤特殊字符
    val wts = (System.currentTimeMillis() / 1000L).toString()
    params["wts"] = wts                                     // 3. 加 wts = 当前 epoch 秒
    val sorted = params.toSortedMap()                        // 4. 按键名升序
    val query = sorted.entries.joinToString("&") { (k, v) -> "${urlEncode(k)}=${urlEncode(v)}" }
    val wRid = md5(query + mixinKey)                         // 5. w_rid = md5(拼接串 + mixinKey)
    val httpUrlBuilder = base.toHttpUrl().newBuilder()
    sorted.forEach { (k, v) -> httpUrlBuilder.addQueryParameter(k, v) }
    httpUrlBuilder.addQueryParameter("w_rid", wRid)
    return httpUrlBuilder.build()
}

private fun filterValue(v: String): String = v.replace(Regex("""[!'()*]"""), "") // 过滤 ! ' ( ) *
private fun urlEncode(v: String): String = URLEncoder.encode(v, "UTF-8").replace("+", "%20")
private fun md5(s: String): String { ... MessageDigest("MD5") ... 小写 hex }
```

**四步 WBI 签名总结**：值过滤 `[!'()*]` → 加 `wts` → 键升序拼 `k=v&k=v` → `w_rid = md5(query + mixinKey)` 追加到 URL。

#### mixin key 获取（BiliClient.kt:1200-1273）

```kotlin
// 主路径：GET NAV_URL，从 data.wbi_img.img_url + sub_url 取文件名拼接
private suspend fun fetchMixinKeyFromNav(): String {
    // data.wbi_img.img_url / sub_url，例如 https://i0.hdslb.com/.../xxxx.png
    ensureValidMixin(imgUrl, subUrl)
}

// 兜底路径：web ticket（HMAC-SHA256）
private suspend fun fetchMixinKeyFromTicket(): String {
    val ts = System.currentTimeMillis() / 1000L
    val hexSign = webTicketHmacSha256Hex("ts$ts")   // HMAC-SHA256("ts<ts>", key="XgwSnGZ1p")
    // GET/POST WEB_TICKET_URL?key_id=ec02&hexsign=<hexSign>&context[ts]=<ts>[&csrf=<bili_jct>]
    // 响应 data.nav.img + data.nav.sub
    ensureValidMixin(imgUrl, subUrl)
}

// 核心：imgKey/subKey 取 URL 最后一段、去扩展名 → 拼接 → 按 MIXIN_INDEX 重排 → 取前 32 位
private fun ensureValidMixin(imgUrl: String, subUrl: String): String {
    val imgKey = imgUrl.substringAfterLast('/').substringBefore('.')
    val subKey = subUrl.substringAfterLast('/').substringBefore('.')
    val raw = imgKey + subKey
    val mixed = StringBuilder()
    for (idx in MIXIN_INDEX) if (idx < raw.length) mixed.append(raw[idx])
    return if (mixed.length >= 32) mixed.substring(0, 32) else mixed.toString()
}
```

#### 取流（BiliClient.kt:978-1037 + 1463-1506）

`requestPlayUrl`：`signWbiUrl(BASE_PLAY_URL, params)` → GET → 解析：
- MP4：`data.durl[]`（每个含 `url` / `backup_url` / `length` / `size`）
- DASH：`data.dash.video[]`、`data.dash.audio[]`（`id`/`baseUrl`/`backupUrl`/`bandwidth`/`mimeType`/`codecs`）
- 杜比：`data.dash.dolby.audio[]`；Hi-Res：`data.dash.flac.audio`

`toAudioStreamInfos()` 把上面映射成统一音轨：

| 来源 | qualityTag | 默认 mimeType | bitrate |
|------|-----------|--------------|---------|
| `dash.audio[]` | `null` | `audio/mp4` | `bandwidth/1000` |
| `dash.dolby.audio[]` | `"dolby"` | `audio/eac3` | `bandwidth/1000` |
| `dash.flac.audio` | `"hires"` | `audio/flac` | `bandwidth/1000` |

```kotlin
private fun bitrateKbpsFromBandwidth(bandwidth: Long): Int = max(0, (bandwidth / 1000L).toInt())
```

`getAllAudioStreams(bvid, cid, opts)`（BiliClient.kt:247）：空音轨重试 3 次 → 仍空则 `buildHtml5FallbackOptions`（`fnval=0, platform="html5", highQuality=1`，拿 MP4 `durl` 兜底）。

#### 流地址优先级（BiliStreamUrls.kt:16-40）

```kotlin
// score: upos-*.bilivideo.* = 3  >  *.bilivideo.* = 2  >  *.mountaintoys.cn = 1
```

#### B站扫码登录（BiliQrLoginClient.kt）

```kotlin
GENERATE = "https://passport.bilibili.com/x/passport-login/web/qrcode/generate" // 拿 qrcode_key + url
POLL     = "https://passport.bilibili.com/x/passport-login/web/qrcode/poll"      // ?qrcode_key=... 轮询
// poll 返回 data.code：0=已确认(带 Set-Cookie)，86101=未扫，86090=已扫未确认
// 从 Set-Cookie 解析 SESSDATA / bili_jct / DedeUserID
```

---

### 2.2 YouTube 音频直链（精确到函数级）

关键文件（都在 `modules/platform/src/main/java/moe/ouom/neriplayer/platform/youtube/api/`）：

- `protocol/YouTubePlayerRequestComposer.kt` —— 组 InnerTube `player` 请求
- `protocol/YouTubePlayerResponseParsers.kt` —— 解析 `streamingData`，求解 URL
- `playback/YouTubePlayableAudioSelection.kt` —— 选最优音轨
- `challenge/YouTubeEjsChallengeSolver.kt` —— yt-dlp ejs 求解器（n/sig）
- `challenge/YouTubeStreamingCipherResolution.kt` —— n/sig 竞速求解（EJS vs NewPipe）

#### 2.2.1 请求构造（YouTubePlayerRequestComposer.kt）

**客户端 Profile**（`playerClientProfiles`）：VISION_OS(101)、ANDROID_VR(28)、WEB_REMIX(67)、TV_HTML5(7)、WEB_CREATOR(62)、TV_HTML5_LEGACY、ANDROID_MUSIC。优先级由 `resolveYouTubePlayerClientOrder` 决定，登录态优先带鉴权的 WEB 客户端。

**URL**（`resolvePlayerRequestUrl`）：

```
WEB_REMIX:  {YOUTUBE_MUSIC_ORIGIN}/youtubei/v1/player?prettyPrint=false&key={apiKey}
其他:       {YOUTUBE_WEB_ORIGIN}/youtubei/v1/player?prettyPrint=false&id={videoId}&key={apiKey}[&fields=...]
```

**关键 Header**（`buildPlayerRequestHeaders`）：

```kotlin
"User-Agent" / "Accept-Language" / "Content-Type: application/json"
"X-Goog-Visitor-Id"       = bootstrap.visitorData
"X-YouTube-Client-Name"   = profile.clientId
"X-YouTube-Client-Version"= clientVersion
"Origin" / "Referer"
"X-Goog-Api-Format-Version" = "2"   // 非 WEB_REMIX
// 鉴权客户端再加：Cookie、X-Goog-AuthUser、Authorization、X-Origin
```

**Body**（`buildPlayerRequestBody`）：`context.client`（`clientName/clientVersion/platform/hl/gl/utcOffsetMinutes/visitorData/...`）+ `context.request` + `context.user` + `videoId/contentCheckOk=true/racyCheckOk=true`（或 WEB_REMIX 的 `playerRequest`）。WEB_REMIX 额外塞 `playbackContext.contentPlaybackContext.signatureTimestamp` 和完整浏览器上下文，**目的是拿到浏览器端 direct 流，避免退回移动端限速直链**。

#### 2.2.2 响应解析（YouTubePlayerResponseParsers.kt `YouTubeMusicPlaybackParser`）

```
parsePlayableAudio(root)
  └─ collectAudioCandidates(root)
       └─ streamingData.adaptiveFormats[] + streamingData.formats[]
            └─ 过滤 mimeType 以 "audio/" 开头
  └─ 逐个候选 resolveFormatUrl(format, cipherResolver)
       ├─ 1. parseStreamingFormat：有 "url" 直接用；否则读 "signatureCipher"/"cipher"
       ├─ 2. parseCipherFormat：把 cipher 按 url-encoded 拆成 url / sp(默认"sig") / sig / s
       ├─ 3. resolveSignedFormatUrl：
       │     已有 sig/signature → 直接 append &{sp}={sig}
       │     只有加密 s → cipherResolver.resolveSignature(s) 解出 sig 再拼上
       └─ 4. resolveStreamingUrl：URL 带混淆的 n 参数 → cipherResolver.resolveStreamingUrl(url) 解 n
  └─ 返回第一个可用的 YouTubePlayableAudio
```

**音轨排序**（`selectCandidate`）：bitrate 降序 → sampleRate 降序 → mime 偏好（`audio/mp4|m4a|aac = 2`，`audio/webm = 1`）→ contentLength。画质档：LOW / MEDIUM(≥96k) / HIGH(≥128k) / VERY_HIGH。

**itag→码率表**（HLS 侧 `AUDIO_ITAG_BITRATES`）：

```kotlin
139=48k, 233=48k, 249=48k,
140=128k, 234=128k, 250=128k,
141=256k, 251=256k
```

#### 2.2.3 最优音轨选择（YouTubePlayableAudioSelection.kt）

- **direct 流优先于 HLS**；客户端偏好分（direct/hls）：VISION_OS 40/25、ANDROID_VR 35/22、WEB_REMIX 30/20、TV 20/5、WEB_CREATOR 15/0、ANDROID_MUSIC 10/0。
- `preferM4a` 时硬优先 `audio/mp4|m4a|aac`（可内嵌标签），没有 m4a 才回退 webm/opus。
- 画质达标（`satisfiesYouTubePlaybackQuality`）优先。

#### 2.2.4 n/sig 反混淆（YouTubeEjsChallengeSolver.kt + YouTubeStreamingCipherResolution.kt）

这是 YouTube 直链的核心难点。NeriPlayer 用**双引擎竞速**：

**引擎 A：yt-dlp ejs 求解器（Android JavaScriptSandbox）**
- 内置两个 asset：`youtube/yt.solver.lib.min.js`（lib）+ `youtube/yt.solver.core.min.js`（core），均由 yt-dlp 的 ejs 模板生成。
- `buildYouTubeEjsSandboxBootstrapScript`：`Object.assign(globalThis, lib)` + core。
- `buildYouTubeEjsPlayerSessionInitializeScript`：把 player.js 以 ArrayBuffer 传入，调用 `jsc({type:"player", player, requests:[], output_preprocessed:true})` 得到 `preprocessed_player`，再 `Function("_result", preprocessed)(_functions)` 提取出 `_functions.n` 和 `_functions.sig` 两个函数。
- `buildYouTubeEjsLoadedPlayerSolveScript`：对每个加密 `s` / 混淆 `n` 调用 `_solver(challenge)`，得到解出的 sig 和 n。
- 沙箱不可用（`JavaScriptSandbox.isSupported()==false`）时退到 WebView 兜底求解器 `YouTubeEjsWebViewFallbackSolver`。

**引擎 B：NewPipe Extractor**
- `YoutubeJavaScriptPlayerManager.deobfuscateSignature(videoId, encryptedSignature)` 解 sig。
- `YoutubeJavaScriptPlayerManager.getUrlWithThrottlingParameterDeobfuscated(videoId, url)` 解 n。

**竞速**（`raceChallenges`，12s 超时，`awaitFirstChallengeSuccess` 谁先成功用谁）：
- sig：NewPipe + EJS 并行；n：NewPipe + EJS 并行。
- 解出的 token 用正则 `^[A-Za-z0-9._~+\-/=]+$` 校验，拒绝 `undefined/null/nan/true/false/[object object]` 这类 JS 假成功值。
- 每个 player.js 版本 NewPipe 失败 1 次即记黑名单（`NewPipeFallbackTracker`），下次跳过 NewPipe 直走 EJS。

**n 参数处理**（`resolveStreamingUrl`）：抽 URL 里的 `n` → 解出合法 n → `replaceStreamQueryParameter(url, "n", solvedN)`；解不出则**丢弃该候选**（否则会被 googlevideo 限速）。

---

### 2.3 NACRE 可直接照搬的实现清单

1. **B站取流**：实现 `signWbiUrl` + `fetchMixinKeyFromNav`（含 `MIXIN_INDEX` 表和 `WEB_TICKET_KEY`），请求 `wbi/playurl`，直接取 `dash.audio/dolby.audio/flac.audio` 的 `baseUrl` 就是音频直链。纯 Kotlin，无 JNI 依赖。
2. **B站搜索/登录**：`wbi/search/type`（同样的 WBI 签名）+ 扫码登录 `generate`/`poll` 两接口。
3. **YouTube 音频直链**：InnerTube `player` 接口（WEB_REMIX 客户端最稳，拿浏览器 direct 流）+ 一个 n/sig 求解器。**建议直接复用 NewPipe Extractor 作为 n/sig 求解器**（成熟、纯 JVM），比自建 ejs/JavaScriptSandbox 成本低得多；ejs 方案仅作兜底。
4. **音源选择器**：仿 NeriPlayer 的「多 client 依优先级 + 画质档位」选择策略。

---

## 3. Salt Player——播放页布局 / 歌词滚动 / DSP

**技术栈**：Compose Multiplatform；音频引擎 BASS + BASS_FX（Un4seen）；自定义 `.so`：`libsalt-bassfx.so` / `libsalt-bassplayer.so`；jaudiotagger 分支（`com.xuncorp.guqin.media.tag`）做标签；`libepic.so`（me.weishu.epic ART hook）/ `libexp82464.so` = 反盗版。

**播放页布局**：Compose 全屏播放页（封面大图 + 上下滑切歌 + 控制栏），NACRE 用 Jetpack Compose 可等价实现。

**歌词安全/实用滚动**：
- 采用**整列表滚动 + 当前行高亮/居中**，而不是逐字/逐行 translate 动画；`LazyColumn` + 当前行锚定，用 `animateScrollToItem` 平滑滚到当前行。
- 歌词解析做**逐行时间戳 + 空行合并 + 双向（双语/谐音）副文本**对齐（NACRE 已实现网易云翻译/谐音副文本，可复用该思路）。

**"SALT PLAYER V4" 功能对应关系**（由 BASS_FX 符号表确认）：

| 宣传名 | BASS_FX 实际效果器 |
|--------|-------------------|
| V3 混响 | `DX8_REVERB` / `BFX_ECHO`（回声/混响） |
| MAX Audio 声学调音 | `DX8_PARAMEQ`（参数均衡器）/ `BFX_BQF`（双二阶滤波） |

NACRE 若要做「混响/调音」，可引入 BASS_FX（或 Android `Equalizer`/`PresetReverb`/`EnvironmentalReverb` 原生音效），对应同样的 DSP 链路。

---

## 附：逆向方法论（本次用到的可复用手段）

- **无 JADX 也能解 APK**：APK 即 ZIP → `unzip` 抽 DEX；DEX 字符串是 MUTF-8，用 Python `dex_strings.py`（正则 `[ -~]{6,}`）扫出 URL/端点/类名；二进制 AXML manifest 的字符串池是 UTF-16，用 `strings -e l` 恢复包名。
- **native `.so`**：`nm`/`strings` 导符号，确认 BASS_FX 的 `DX8_*`/`BFX_*`、FFmpeg `libffmpegJNI`、epic hook 等。
- **灰产类 App 共性**：配置驱动抓取 + 字符串加密（doubleReflector/JNI）+ 混淆（ProGuard/R8）。
