# DiPlay 原生 CarWith 视频能力选择与协商边界

2026-09-30。用户选择停止第三方 Demo 试验、取消待批独立诊断 APK，转为开发 DiPlay 自有分辨率方案。本次没有再操作测试设备，也没有安装 APK。平板“低于1080p可用”的反馈是该设备经验约束，不推广为某系列 SoC 的共同上限。

## 本次实现

- `CarWithVideoPolicy` 使用固定720p / 1080p的AVC候选，默认用户上限为720p。查询实际解码器的宽高对齐、尺寸/帧率以及 profile/level 格式支持；不会从物理屏幕、可见窗口、Surface大小或缩放比例派生视频请求。
- 未满足对齐的候选直接跳过，不把1080高度向上取整为1088或向下变成另一种宽高比。1080p不支持时尝试720p；请求60fps时可降到30fps。已知没有可用30fps组合时返回 `NO_SUPPORTED_MODE`，不会凭空宣称有支持。
- 能力查询缺失/失败时保留 **720p@30、UNVERIFIED_FALLBACK** 的保守预案，明确未验证。它不能通过协商守卫自动记录为已发布能力，仍须真实后端校验；空解码器列表与查询失败有所区分。
- 优先选择报告为硬件加速的decoder，避免选软件1080p替代可用硬件720p。API28没有硬件标志查询，保持未知并保留列表顺序；只在API29+调用 `isHardwareAccelerated`。厂商报告不等于性能保证。
- 用户在“设置 → CarWith 视频 → 视频分辨率上限”选择720p或1080p。偏好与CarPlay设置/身份独立，保存不会重连CarPlay。UI明确CarWith连接尚未启用；该上限当前供真实能力选择及诊断使用。
- 导出的原生接收诊断包含上限、本地选择、能力证据、候选decoder与profile/最大level，同时明确能力未发布、未协商、未配置decoder；不会把这些本地结果变成连接成功。

Android实现使用 `MediaCodecList.REGULAR_CODECS`，排除encoder，读取AVC `VideoCapabilities`，用 `areSizeAndRateSupported` 和包含 MIME、尺寸、fps、profile、level 的 `isFormatSupported` 共同筛选。平台profileLevels报告的最大level不是要求发送端一定用该level；最终视频的实际profile/level仍需再次检查。查询不创建/启动MediaCodec，不请求无线/录音/存储权限。

## 四个阶段必须分开

| 阶段 | 已实现边界 | 目前原生接入状态 |
| --- | --- | --- |
| 本地参数选择 | 实际decoder能力筛选、用户上限、保守预案 | 已实现并接入设置/诊断 |
| 向手机发布接收能力 | `CarWithVideoNegotiation.recordPublication`只校验并记录真实后端确认的模式子集 | **真实发布尚未实现**；没有SDK/协议数据包发送，也没有伪成功adapter |
| 最终协商结果 | `recordNegotiated`要求模式已发布，尺寸/fps符合上限，实际profile/level被对应decoder支持；否则要求重新协商 | **真实回调/发送端重协商尚未接入**；返回状态不是已发出的协议消息 |
| 实际解码配置 | `recordDecoderConfigured`只接受与协商参数和选定decoder一致的真实配置事件；重新发布、重协商或重置清除旧结果 | **原生配置/首帧尚未实现**；Surface缩放不算协商或解码成功 |

异常宽高比、1920×1152超出用户上限、未发布的旋转尺寸、无效宽高、fps改变、profile/level不支持均在配置前拒绝。策略支持显式横/竖屏候选并检查旋转后的实际尺寸；当前原生SDK的旋转能力合同未验证，UI/诊断默认横屏。不是把当前接收屏幕旋转当作协议能力。

## 有证据的 SDK 参数映射与缺项

已有合法静态研究仅确认接口结构，不复制或打包SDK二进制/源码/身份材料：

| 本地信息 | 已见接口 | 使用边界 |
| --- | --- | --- |
| 选定视频宽高 | `UCarConfig.Builder.setVideoDisplayWidth/Height` | 可作为待核实的配置映射；是否控制编码尺寸、手机是否接受、可否运行时修改仍需SDK合同及真实协商证据 |
| 接收端屏幕/坐标尺寸 | `setScreenWidth/Height` | 与视频参数分别保留；屏幕坐标与视频像素如何关联未确认，不盲目将高分屏尺寸写成视频请求 |
| 目标fps | `setFps` | setter存在；需要确认发布/最终接受结果，不能把设置值当实测帧率 |
| 低延迟 | `setSupportLowLatencyDecodingMode` | 已有Demo初始化设false；策略不擅自启用或要求API30能力 |
| 最终媒体配置 | `startCast(Surface)`及已见媒体层尺寸入参 | Surface本身不表达发送端协商结果；需要真实参数回调/码流参数绑定 |
| profile/level列表、模式列表、动态重协商 | 未确认可集成接口/协议合同 | 不杜撰字段、UUID或wire格式；须由合法SDK或有完整规范的自有后端实现 |

当前仓库原生CarWith路径只有预检、本地策略和校验边界，现有 `AndroidMediaSink` 是CarPlay媒体后端；没有将它改成伪装已连接的原生CarWith接收器。SDK的可集成/重分发许可、合法配置/认证身份来源、能力发布及真实协商回调仍是端到端阻塞。此策略不要求先取得这些材料才能单测，但不声称绕过了这些缺项。

## 验证与后续验收

纯策略测试覆盖720p/1080p、尺寸/对齐限制、60→30fps及无30fps支持、profile/level、异常宽高比、旋转、缺能力/厂商异常、软件与硬件优先级，以及发布/协商/配置各阶段和旧状态清除。Android框架测试覆盖API28/31/33真实能力对象的尺寸/帧率/profile检查、encoder过滤和偏好隔离。minSdk28保留；测试不代表API28车机实测或原生投屏成功。

后续取得可合法配置的接收SDK/完整协议与测试身份后，真实绑定四阶段，以720p30先验证能力发布、手机最终参数和DiPlay首帧，再按能力及用户上限提升至1080p。码流参数与协商不一致必须明确拒绝/请求重协商，不能通过配置小Surface掩盖。Wi-Fi与USB应共用此视频约束；USB能否建立原生连接及是否共用媒体路径仍需实测，不假设USB会修复codec问题。

参考：[VideoCapabilities](https://developer.android.com/reference/android/media/MediaCodecInfo.VideoCapabilities)、[CodecCapabilities](https://developer.android.com/reference/android/media/MediaCodecInfo.CodecCapabilities)、[硬件加速查询及API等级](https://developer.android.com/reference/android/media/MediaCodecInfo#isHardwareAccelerated())。此前实机异常仅作[独立失败证据](NATIVE_CARWITH_CODEC_FAILURE.md)。
