# 原生 CarWith：平板接收端解码器启动失败

2026-09-30。本记录是第三方接收端的脱敏互操作性诊断，不是 DiPlay 原生接入成功或已确认的根因。没有收录 APK、反编译源码、原始日志、连接码、设备地址或身份材料。

## 本次设备和失败证据

发送端为用户操作的 K30 Pro / 旧版 CarWith；本次失败窗口发送端没有 ADB 证据，不能将旧日志当作同步发送端记录。接收端为小米型号 `23073RPBFC` / `xun`，HyperOS 2、Android 15 / API35。只读系统属性报告 SoC 为 **QTI SM6225**，不依据“骁龙 6 系”名称推断整个系列的解码上限。

接收应用为经用户批准安装运行的 `com.ucarhu.demo v1.2.2`。用户报告输入连接码后失败。接收端本次媒体记录如下，时间均为 UTC：

| 时间 / 项目 | 实际观察 |
| --- | --- |
| 13:08:25.411–.416 | `configureDecoder`；选择 `c2.qti.avc.decoder`，MIME `video/avc` |
| 配置输入 | **1920×1152、30fps、profile 数值 8**；level 未取得 |
| Surface | 已连接；不是仅凭异常推断 Surface 不存在 |
| 配置输出 | 宽高仍为1920×1152，同时记录 `max-width=1920`、`max-height=1079`；此值不是完整 MediaCodecList 能力报告 |
| 13:08:25.417 | 状态到达 `CONFIGURED`，随后算法速率参数更新为45；不等于实际视频已达到45fps |
| 13:08:25.419 | 状态进入 `STARTING` |
| 13:08:25.429 | `Codec reported err …/NO_MEMORY, actionCode 0, while in state 5/STARTING` |
| 13:08:25.433–.434 | `MediaCodec.CodecException`；可保留栈帧为 `MediaCodec.native_start` 和 `MediaCodec.start(MediaCodec.java:2576)`；SDK 返回10005 |

静态错误枚举将10005映射为 `ERROR_START_CAST_FAILED`，10009为 `ERROR_CONNECT_AUTH_ERROR`，10006为 `ERROR_CONNECT_TIMEOUT`。不同尝试的错误不能混合归因。本次10005有明确的 codec 启动异常，不能仅归结为未发现设备、密码输入问题或 Wi-Fi 延迟，也不能由 `NO_MEMORY` 推断整机内存耗尽。

## 应用内配置和 SDK 调用边界

只读检查当前前台页面仍为连接失败提示，操作为“重连 / 取消”，未操作这些按钮。Manifest 仅声明一个 `UCarDemoActivity`。在已检查的 Demo 类中，尺寸字段 `K/L/M/N` 的写入仅来自 `z0()`：读取窗口可见矩形，将长边/短边作为宽高，各自向下按16对齐，再复制到视频显示宽高。它读取真实 DisplayMetrics，但配置尺寸来自窗口可见矩形，不能简单称为直接使用物理屏幕完整分辨率。平板只读 `wm size` 输出物理尺寸1200×1920；没有执行 `wm size` 修改。

`UCarDemoActivity.A0()` 调用 `z0()`，然后向 `UCarConfig.Builder` 传递 screen/videoDisplay 尺寸，明确设置 `setFps(30)`、`setSupportLowLatencyDecodingMode(false)`。已见 SharedPreferences 调用位于返回字节数组的 `s0()` 路径；尺寸写入链没有读取偏好配置。**没有确认到现有 UI、Intent 或正常配置可把协商视频独立设置为1280×720的入口。**这不等于整个 SDK 不支持配置尺寸；Builder 的 screen/videoDisplay 设置方法已存在。

SDK 媒体边界的静态结构证据：

- `a.c.d.d.b.f(Surface,int,int,boolean)` 调用 VideoPlayerV2 的配置与启动路径；异常后返回失败，由适配层发出10005。
- `a.c.d.h.j.d(Surface,int,int,boolean)` 创建 AVC MediaFormat 并调用 `MediaCodec.configure`；包含低延迟参数路径。
- `a.c.d.h.j.t()` 设置异步回调并调用 `MediaCodec.start`，与本次系统栈一致。

这些是原创结构描述，不授予复制 SDK、修改/重打包第三方 APK、复制认证身份或公开分发二进制的许可。当前样本的低延迟配置明确为 false，不能优先假设启用低延迟导致失败；更深层运行时参数仍需合法可配置测试端验证。

## 解码能力与尚未确认的根因

只读厂商文件中，同名 AVC decoder 在通用 `/vendor/etc/media_codecs.xml` 声明4096×2176，芯片专用 `media_codecs_khaje_iot.xml` / `media_codecs_khaje_v0.xml` 声明1920×1088。多份 XML 的存在不证明哪份最终生效，也不等于实际可持续性能；普通 `dumpsys media.codec` 没有给出所需完整运行时能力。

**优先假设是视频尺寸与有效解码能力/资源配置不匹配。**请求高度1152超过当次输出格式记录的1079，也超过上述芯片专用 XML 的1088，但尚未用运行时能力查询和单变量测试证明因果。需保留其他 codec 资源、驱动或参数组合问题的可能性。

缺项为完整 SDK 异常栈、`CodecException.getDiagnosticInfo()/getErrorCode()`、视频 level、运行时 MediaCodecList 的 profile/level/尺寸/帧率支持。异常数字状态被首次隐私过滤遮盖；现存目标应用日志未重新取回该错误行，不填推测数字。被动采集已在13:15:36 UTC结束，没有要求再次复现、清日志、重启或修改系统设置。

## 最小下一步与 DiPlay 接入要求

1. 使用独立的第一方 debug 诊断构建，只查询实际 `MediaCodecList`、AVC `profileLevels`、`VideoCapabilities` 及 `isFormatSupported/areSizeAndRateSupported`。不含第三方 SDK/身份材料，不申请定位、录音、媒体文件或网络权限。**在新平板安装运行这个新 APK 需要明确批准，尚未执行。**能力查询通过也不是 codec 启动或首帧验收。
2. 若获得有许可、允许调整协商参数的原生接收测试端，将视频请求设为 **1280×720@30**，保持低延迟 false、其余连接参数一致；比较启动和首帧结果。当前 Demo 的正常设置路径未确认，不能通过系统改分辨率或修改第三方 APK冒充这项验证。独立本地解码 fixture 可隔离 codec 启动问题，但不证明 CarWith 协商或投屏成功。
3. DiPlay 接入应按实际 decoder、profile/level、尺寸对齐和帧率能力限制协商尺寸，保留720p / 1080p回退并区分能力查询、configure、start、首帧失败。API28兼容基线不变，API29/30的新能力属性需版本分支；不能将高分屏尺寸直接当作可解码视频尺寸，也不能用 XML 或 SoC 营销参数替代实测。

若有线和无线共用 CastManager / VideoPlayerV2，并继续请求同样的1920×1152视频，**USB也可能遇到同一解码启动失败**。这是条件推论，尚无本次原生USB首帧证据；不能宣称换USB必然修复。USB角色与AOA建链仍是独立待验项。

参考：[Android VideoCapabilities](https://developer.android.com/reference/android/media/MediaCodecInfo.VideoCapabilities)、[CodecCapabilities](https://developer.android.com/reference/android/media/MediaCodecInfo.CodecCapabilities)、[CodecException](https://developer.android.com/reference/android/media/MediaCodec.CodecException)。相关成功组合见[此前实机基线](NATIVE_CARWITH_DEVICE_BASELINE.md)；DiPlay接入状态见[原生实施计划](NATIVE_CARWITH_FIRST_FRAME.md)。
