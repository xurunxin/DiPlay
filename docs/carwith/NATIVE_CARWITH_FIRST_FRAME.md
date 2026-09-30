# 原生 CarWith 首帧：实机基线与 DiPlay 接入条件

更新：2026-09-30。目标是 **DiPlay 安卓车机应用直接接收原生 CarWith / ICCOA Carlink，摆脱外置盒**；CarLife仅为兼容回退。原[Project #1](https://github.com/users/xurunxin/projects/1)字段和Issues未关闭或改写，CarLife画面不能作为原生验收。

## 目前已证明什么

用户单独批准安装运行经过哈希/签名/静态分析的 `com.ucarhu.demo v1.2.2`。它已在K30Pro/Android12/ARM64正常运行；用户确认 **HyperOS3手机的原生CarWith应用投屏、可听音频和触控操作正常**。只读工具观察到接收端前台/存活、四个本地SDK服务、RTSP200保活、持续推进的目标Surface呈现、目标音频播放器started和触控/UIBC发送活动。当前车辆显示名称为用户报告的“奇瑞风云2”。

这证明此设备/样本/身份组合存在无需外置盒的软件接收路径。**DiPlay自身仍未接入真实SDK，未完成自己的发现、鉴权或首帧。** 完整版本、链路模式、独立认证回调、延迟/掉帧和625级性能仍未验证。详细区分用户确认、工具证据、身份/车名兼容假设及执行边界见[实机基线](NATIVE_CARWITH_DEVICE_BASELINE.md)和[脱敏记录](evidence/native-third-party-baseline.json)。

## 实际材料与环境

- 指定checkout `X:\Projects\Code\DiPlay`、既有分支 `work/carwith-m0-evidence`，保留用户改动；未新克隆，未发现checkout/祖先AGENTS.md或`.agents/skills`。
- ADB实际可见且已授权的K30Pro为本次接收端；其原CarWith3.2并非本次发送应用。发送端由用户确认为HyperOS3/CarWith，未确认具体型号、OS/App版本或ADB连接，不能把旧手机版本当作成功发送端版本。
- 两附件完整材料化并完成[静态分析](ICCOA_APK_STATIC_ANALYSIS.md)。接收APK内有 `UCarAdapter.init/startAdvertise/startCast`、本地BLE/P2P/AP服务和ARM媒体库；另一`com.heytap.opluscarlink 14.1.8`不是该接收SDK升级版。未将第三方二进制/源码/身份材料提交仓库。
- 官方历史列表包含K30Pro，支持其历史兼容调查，但本次K30Pro作为接收端成功不验证其Android12/CarWith3.2发送能力。旧案例Android版本条件同样不能替代实测。
- 官方车端SDK应用进程内架构与公开标准列表匿名code401的既有观察见[软件接收研究](ICCOA_SOFTWARE_RECEIVER.md)；资料受限不证明必须连接盒。未注册、联系厂商、接受条款或绕过认证。
- 接收APK的媒体库只有ARM32/ARM64；x86_64 USB库不能证明x86媒体SDK可用。API34 AVD现已启动，并经用户单独批准安装运行接收APK；选择ARM64 ABI后进程存活，停在位置权限弹窗，SDK初始化及真实无线连接尚未验证。未更改CPU亲和性或系统网络/安全设置。

## 逐阶段状态

| 阶段 | 第三方样本基线 | DiPlay状态 / 最小条件 |
| --- | --- | --- |
| 加载/初始化 | ARM64应用运行、SDK服务和媒体呈现已观察；未单独保留初始化回调 | 未接入；需要可集成SDK/依赖和真实配置/状态合同，或足够的独立互操作规范 |
| 原生发现/配对 | 用户连接成功，未单独捕获发现/配对时序 | 未执行；使用实际API/字段合同与正常系统配对流程，不猜UUID/身份 |
| 身份认证 | 用户确认端到端成功，工具观察保活；没有独立认证回调证据 | 未执行；明确CCD/合法测试身份来源与认证合同，不能复制样本身份 |
| 会话/视频 | RTSP200保活、目标Surface有效呈现并推进；用户确认投屏 | 未执行；真实SDK单Surface或压缩帧合同、协商和生命周期 |
| 首帧/输入/音频 | 用户确认画面、正常触控、可听音频；相关工具状态支持 | DiPlay首帧/输入/音频未实现验收，须真实重复连接验证 |
| 持续性能 | 只有K30Pro短呈现窗口约33.45ms中位间隔 | 非稳定FPS/掉帧/延迟或625级验收；待DiPlay视频链路后测 |

## 下一步实施顺序

1. 核实独立SDK及许可、初始化示例/状态码、ABI/后装APK要求，以及CCD配置/合法测试身份来源。方法签名已见，完整合同仍未知。APK可下载或运行不意味着可将其SDK/身份再分发进GPL项目。
2. **优先调查兼容列表**：当前“奇瑞风云2”可能关联显示名称，也可能关联厂商/产品ID、协议能力或认证身份。先取得允许自定义字段与获准测试身份的合同，使用合法单变量对照；不改当前连接配置、不克隆车辆身份、不把换车名当已证实解法。
3. 在DiPlay隔离debug后端按真实合同实现 `init → startAdvertise → 正常配对/认证回调 → startCast(Surface)`；状态机与超时、Surface生命周期、disconnect/deInit、音频焦点和触控映射分别处理。不导入未知许可二进制，不让第三方样本成功直接改变DiPlay成功状态。
4. 先在ARM接收台架复现DiPlay自己的第一帧，并重复连接/输入/可听音频，再做断线恢复、持续视频与625级性能。x86只有获准媒体ABI/真实无线映射后才进入同类验收。

**当前关键缺项从“是否有可运行软件接收端”收敛为“如何合法、独立地接入DiPlay，以及哪些配置/身份/兼容限制必须满足”。** 已有合成AVC与UI基准仍只是基础证据；原CarLife回退记录和Project状态不因本次第三方样本成功自动完成。

## Android 9 / API28 接收端验收基线

用户明确要求兼容Android 9车机。`mobile/common/shared/automotive`当前均为`minSdk=28`；接入不得无声提高此值。更低系统仅评估，不降低最低版本或承诺支持。API34 AVD与Android12接收实机的结果均不能代替API28验收。

| 检查面 | 已审查证据与实施要求 | API28验收 |
| --- | --- | --- |
| SDK / Java API | 第三方样本Manifest为min24/target30；已见`startAdvertisingSet`、`MediaCodec.setCallback/setOutputSurface`等API28以前的接口，但这不是完整DEX可达性证明。真实SDK必须提供最低版本及版本分支，测试所有启动、连接、退出路径；反射/非SDK接口单独审查 | ARM车机冷启动、初始化和退出无链接、缺方法/类或权限错误；不能靠提高minSdk通过 |
| native / ABI | 两个ARM ABI的`libovmsink.so/libusbio.so`Android ELF note均标记API27、NDK r21b。用现有NDK28.2的API28 `liblog/libm/libdl/libc`导出表对照，各库强未定义符号未发现缺项；未校验符号版本、运行时`dlopen/dlsym`或所有代码路径。min24声明、构建标记及符号检查均不是运行通过 | 分别核实目标车机ARM32/ARM64 ABI及完整依赖，实际加载、首帧、断开重连；不把x86 USB库当作x86媒体SDK |
| BLE / Wi-Fi权限 | API28使用旧`BLUETOOTH/BLUETOOTH_ADMIN`及扫描所需位置权限；不能只实现API31+Nearby Devices或API33+Nearby Wi-Fi请求。定位开关、拒绝权限、硬件广播能力及非SDK限制须显式处理 | 经用户授权正常发现/配对；拒绝权限可恢复，不自动改变安全设置或申请系统签名权限 |
| P2P / AP | 现有`WifiP2pGroupManager.start`主动要求API29+，不能原样复用到28；这是本项目可控凭据实现的限制，不是Android9缺少P2P。原生SDK自己的`initialize/createGroup/requestGroupInfo`路径须按合同验证；LocalOnlyHotspot/车机热点仅在协议支持时选用 | 普通应用权限下验证真实GO/客户端或协议允许的AP模式，记录厂商限制；不得绕过隐藏API或假造连接状态 |
| 视频 / 音频 | 已有`AndroidMediaSink`按API29保护`isSoftwareOnly`、按API30保护低延迟特性；API28走普通MediaCodec与AudioTrack路径。SDK硬解profile/level、分辨率、音频格式及采样率必须实测，不从Android版本推断硬解能力 | 先协商双方支持的AVC/音频参数，取得首帧、可听音频和触控响应；HEVC或特定参数不可用时只使用真实协议允许的降级 |
| 前台服务 / 生命周期 | `common`已声明`FOREGROUND_SERVICE`，`DiPlaySessionService`在28使用双参数`startForeground`，29+才传服务类型。原生后端接入仍须核实自己的服务、通知、Surface重建、音频焦点和资源释放 | 前后台切换、Surface销毁重建、断线恢复、用户停止和车机休眠恢复；API28后台麦克风限制不以强授权限绕过 |

本机目前只有API34镜像。Google官方镜像目录确有`system-images;android-28;google_apis;x86_64`（压缩下载1,102,721,597字节），可作为申请新增安装后的Java/API与生命周期检查台架；另需解压及独立AVD数据空间。当前未安装、未接受新条款。镜像是否提供ARM转译必须安装后另验，不能继承API34结论；API28模拟器也不能替代真实无线电或ARM车机硬解验收。最终需要至少一个API28 ARM车机/台架完成原生发现→认证→首帧及音频、触控、生命周期验证。625级性能只在实际视频链路建立后测量。

当前K30→API34 AVD测试：APK按ARM64安装启动成功，进程存活；前台位置权限问题已提出并待用户答复，未授予麦克风/后台位置等额外权限。真实硬件的BLE/HCI与P2P接入尚未建立，不能归因于旧手机不支持。此前HyperOS3→K30连接由用户主动断开，不记为稳定性失败。

依据：[Android 9行为变化](https://developer.android.com/about/versions/pie/android-9.0-changes-all)、[API28前台服务要求](https://developer.android.com/about/versions/pie/android-9.0-changes-28)、[蓝牙权限](https://developer.android.com/develop/connectivity/bluetooth/bt-permissions)、[Wi-Fi Direct](https://developer.android.com/develop/connectivity/wifi/wifi-direct)、[NDK最低API说明](https://developer.android.com/ndk/guides/sdk-versions)、[Google官方镜像目录](https://dl.google.com/android/repository/sys-img/google_apis/sys-img2-4.xml)。

## 已实现的独立接收条件预检

`shared/.../carwith/NativeCarWithPreflight.kt`提供API28/31/33标准BLE/Wi-Fi发现权限分支及只读USB状态评估，`common/.../NativeCarWithDiagnostics.kt`在现有诊断导出中采集平台与USB接口条件。它区分USB host未报告、服务不可用/查询受限、等待设备、未观察到AOA模式、AOA数据接口缺失、设备授权缺失与AOA数据接口存在。普通厂商USB、ADB-only接口、跨接口拼凑的bulk IN/OUT和AOA音频专用PID均不算数据候选；输出只包含状态与数量，不含USB名称、路径、序列号或身份配置。它不扫描、授权、打开设备或发送AOA模式切换请求。接口存在仅表示可继续验证传输，不能证明CarWith兼容、认证或首帧。

这些权限是标准发现API的预检条件，不是未知SDK的完整权限合同；USB状态与无线权限独立，位置权限缺失不会把USB接口评估判成失败。API28/31/33的Robolectric框架级测试覆盖对应Android版本分支及厂商服务异常；这些测试不替代实机验收；纯逻辑测试覆盖USB接口和设备授权边界。`WifiP2pGroupManager`目前只由CarPlay控制器调用，API28已有LocalOnlyHotspot选择分支，因此没有改动该类或将其称为原生接收后端。

USB原生候选的静态证据：样本有`UCarAdapter.enableUsbDeviceDetection`、`IShareLinkManager.enableUsbDeviceScanning`、`MDevice.isWired`及`com.ucar.connect.aoa.UsbNative.nativeBulkRead/nativeBulkWrite`；K30 CarWith声明AOA附接/分离Activity。官方Emulator37.1.11有`-usb-passthrough`选项，但当前AVD未报告USB host特征，也未配置真实透传。下一步是正常USB枚举/应用授权→SDK有线初始化→认证→首帧；AOA是传输候选，不自动等同ICCOA协议成功。未强启手机隐藏Activity、发送AOA控制请求或改变Windows驱动。依据：[ICCOA有线/无线要求](https://www.iccoa.cn/tech/68.html)、[Android USB host](https://developer.android.com/develop/connectivity/usb/host)、[AOA规范](https://source.android.com/docs/core/interaction/accessories/aoa)。


## 后续平板失败诊断

K30 Pro 向 Android15 平板接收端的本次尝试已取得独立媒体证据：1920×1152@30 的 AVC 解码器 configure 完成后，在 start 阶段报 NO_MEMORY，并触发10005。尺寸/能力不匹配是待验证假设；720p单变量验证尚未执行，USB不保证修复。现有Demo尺寸来自可见窗口计算，未确认应用内720p设置入口。详见[脱敏解码器失败记录与最小验证方案](NATIVE_CARWITH_CODEC_FAILURE.md)。本记录不改变此前 HyperOS3→K30 第三方接收端成功基线，也不宣称 DiPlay 原生链路通过。
