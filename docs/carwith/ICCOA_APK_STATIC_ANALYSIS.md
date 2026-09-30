# ICCOA 车端 APK：两份附件的静态分析

日期：2026-09-30。目标仍为 DiPlay 安卓车机应用原生接收 CarWith / ICCOA Carlink，CarLife 仅回退。本次只读 APK 的 ZIP、Manifest、DEX 元数据/调用目标和 ELF 依赖；**没有安装或运行任何样本，没有发现/配对/认证/首帧成功证据**。

后续运行更新：本文为安装前静态快照。用户之后单独批准运行，K30Pro上的样本已成功接收HyperOS3原生CarWith，音频/触控由用户确认，工具取得有限会话/呈现证据。当前状态与车辆名称兼容假设见[实机基线](NATIVE_CARWITH_DEVICE_BASELINE.md)；本文的“未安装/运行”和阶段未执行结论仅指静态分析时点，不能作为当前运行状态。

## 来源、身份和可复核性

用户指定[车友社区条目](https://www.bydmax.com/apps/3292.html)，页面标题“ICCOA Carlink 车机端”，日期 2023-11-03；随后提供[夸克分享](https://pan.quark.cn/s/b2db8990060c#/list/share)，并直接上传两个 APK。页面是第三方社区，不能据此证明开发者身份、完整发布链或 SDK 授权。论坛 2026 修改版和此样本不是同一版本。

| 项目 | 附件 A | 附件 B |
| --- | --- | --- |
| 实际上传文件名 | `CarLink_14.1.8.apk` | `【视频演示使用的版本】ICCOA+Carlink_v1.2.2.apk` |
| 文件大小 | 11,579,172 字节 | 4,598,259 字节 |
| 包名 | `com.heytap.opluscarlink` | `com.ucarhu.demo` |
| versionName / versionCode | `14.1.8` / `1401008` | `v1.2.2` / `2023` |
| minSdk / targetSdk / compileSdk | 28 / 34 / 34 | 24 / 30 / 33 |
| DEX | 2 | 2 |
| APK 内原生库 | 无 `lib/` 条目 | 详见下表 |
| 已验证签名方案 | v3 | v2、v3 |
| 签名证书主体 | OPPO / ColorOS / AndroidTeam | Android / Android；不能凭这个名称证明官方发行身份 |

附件 A SHA256：`c7d3879fed17d300a24b83bf85d5c6abc5bda038040a41cbed44355aef815000`。

附件 B SHA256：`eb15e5a62cea12c1ecb8bf0e20f8c5369fb6eaa34820720130c3b6073053a3e3`；与正常公开分享下载的 `【视频演示使用的版本】ICCOA Carlink_v1.2.2.apk` **完整文件哈希相同**，无需重复分析。

公开签名证书 SHA256 指纹分别为 A `64aafaf1d5bc9155a9e417a849e4f8eda1d0d1341667c28ed7c443c76f820b9a`、B `a40da80a59d170caa950cf15c18c454d47a39b26989d8b640ecd745ba71bf5dc`。签名验证证明字节与所附签名一致，不证明发行者可信或允许 SDK 再分发。未取得官方对照指纹。

两附件由当前 Library 助手材料化至本机私有分析目录，实际字节数、完整 SHA256 和 Library 身份扩展属性均核验。Windows Python 缺少 `os.setxattr`；使用已有 WSL 的同一官方助手直接写入持久任务目录，保留身份属性。APK、DEX/SO 工作副本、Library IDs 和原始分析数据均留在仓库外；本文件只包含原创描述、公开方法名和非秘密指纹，不包含第三方反编译源码、身份资产或授权元数据值。

## B 确实包含车端接收结构

下述是实际类/方法及调用元数据证据，不是对线协议的完整复现。

| 层 | 静态证据 | 尚不能证明的内容 |
| --- | --- | --- |
| 接入入口 | `com.ucarhu.demo.UCarDemoActivity.A0` 构建 `UCarConfig.Builder`，调用 `com.ucar.vehiclesdk.UCarAdapter.getInstance().init(Context, UCarConfig, ICarInitCallback)` | 初始化结果、SDK 独立发布版本及合法集成许可 |
| 发现/进度 | `UCarAdapter.startAdvertise/stopAdvertise`；`ICarConnectListener.onConnectStateChanged/onConnectingProgress/onPinCode` | CarWith 当前版本识别及每个状态整数的完整合同 |
| 本地连接服务 | `UCarConnectProxy` 的 `const-class` 指向 `com.share.connect.ShareLinkService`，并调用 `Context.bindService`；该服务在同 APK Manifest 中，`exported=false` | 所有路径均无外部服务依赖；本结论只证明这条绑定指向本地服务 |
| BLE | APK 内 `BluetoothLeService`、`IBluetoothLe` 和 `BluetoothLeObserver`；调用 `BluetoothLeAdvertiser.startAdvertisingSet`，有 PIN、发现和地址通知回调 | 广播字段、UUID、版本/角色、时序及完整互操作要求 |
| Wi-Fi | APK 内 `WifiP2pService` 和 `WifiApService`；调用 `WifiP2pManager.initialize/createGroup/requestGroupInfo`，SoftAP 服务使用反射和 `WifiManager.setWifiEnabled` | 普通后装 APK 在实际系统能否满足所有网络操作权限 |
| 认证 | `ShareLinkObserver.onAuthenticationOk`；`UCarProto.CarCertificate/CertIndex` 类型；混淆类 `a.b.a.h.b` 调用 Android `KeyStore` 与 `Signature.initSign/initVerify` | 车端身份如何取得、是否有测试身份/激活、服务端信任链；未读取任何密钥或认证资产内容 |
| 会话/媒体控制 | `UCarProto.GetPortRequest/GetPortResponse`、`GetUCarConfigRequest/Response`、`Heartbeat` 等消息类型 | 消息封装、通道绑定、协商和验证完整合同；不能从类名猜端口或跳过认证 |
| 视频 | `UCarAdapter.startCast(Surface,int,int)`；`com.ucarsink.sink.natives.SinkNative` 加载 `ovmsink`，有 `start/stop/pause/resume` native 方法和 `onNativeCastVideoInitialized/onNativeVideoDataReceived` 回调 | 真实首帧、分辨率、PTS 和 Surface 生命周期行为 |
| 解码/音频 | 混淆类 `a.c.d.h.j` 调用 `MediaCodec.createDecoderByType/configure/setCallback/setOutputSurface`；`a.c.d.h.g` 内部类使用音频 MediaCodec/AudioTrack，`a.c.d.i.a` 使用 AudioRecord | 实际 codec、采样参数、音频路由、延迟及稳定性 |
| 输入 | `UCarAdapter.sendTouchEvent/sendKeyEvent/sendMicRecordData`；`SinkNative.onUibcEvent/onUibcEncrypted` | 手机对触控、加密输入和音频回传的真实接受情况 |

`libovmsink.so` 内实际存在 `RTSP/1.0`、`OPTIONS/SETUP/PLAY/TEARDOWN`、`RTP/AVP`、`wfd_video_formats/wfd_audio_codecs/wfd_uibc_capability` 标记。这支持“包含 WFD/RTSP/RTP/UIBC 媒体实现”的判断；**不等于标准 Miracast 接收器已经能接受 CarWith**，BLE发现、身份、附加消息及协商仍须真实合同。未导出整段原生字符串、证书材料或第三方实现。

### ABI 和车机绑定

| 原生库 | arm64-v8a | armeabi-v7a | x86 | x86_64 |
| --- | --- | --- | --- | --- |
| `libovmsink.so` | 有 | 有 | 无 | 无 |
| `libusbio.so` | 有 | 有 | 有 | 有 |

`aapt native-code` 会汇总报告四种 ABI；**不能因此把媒体 SDK 认作支持 x86_64**。`SinkNative` 确实调用 `System.loadLibrary("ovmsink")`；没有 x86 媒体库。原生 ARM 接收台架是此样本最直接的运行验证目标，x86 AVD 需要另有对应 ABI 的合法 SDK 或另行核实翻译层，不把 advertised ABI 当已通过。

ARM64 `libovmsink.so` 的 `DT_NEEDED` 只有 `liblog.so/libm.so/libdl.so/libc.so`，有 `JNI_OnLoad/JNI_OnUnload`。该列表未见比亚迪专有库或外置 CPC 盒依赖；结合本地连接服务和车端 SDK，支持纯软件接收结构。动态加载、反射或运行时身份依赖仍可能存在，不能声称已证明任何 Android 车机都能运行。

Manifest 声明 `BLUETOOTH_PRIVILEGED`、`LOCAL_MAC_ADDRESS`、`NETWORK_SETTINGS`、`NETWORK_STACK`、`TETHER_PRIVILEGED`、`OVERRIDE_WIFI_CONFIG`、`MANAGE_USB` 等权限，以及 BLE/GPS 功能；未声明 `sharedUserId`。声明不是已获得权限，也不是证明每项权限在所有连接模式都必需。SoftAP 反射路径和 P2P 路径需分开实测，不绕过权限、复制平台签名或改系统安全设置。

ZIP 中有 `assets/ccd.data`；`UCarConfig.Builder.setCcdFilePath` 及连接代理 `getCcdFilePath` 调用表明配置文件与接入相关。只记录文件名和接口，**未读取/提取该资产内容**，不能把它视为可复制的测试身份，也不能仅凭名称确定内容。

## A 不是 B 的新版接收端

A 没有 B 的 `com.ucar.vehiclesdk`、`com.share.connect`、`com.ucarsink` 类族或 `libovmsink.so`，没有 APK 内原生库。Manifest 查询 `com.oplus.ocar`、OPPO账户和配件生态，声明 `com.oplus.permission.safe.CAR_LINK`、`OPLUS_COMPONENT_SAFE`、`ACCESSORY_FRAMEWORK` 等厂商权限，包含 `CarService`、`car.OplusSdkService`、`ipc.InnerIpcService` 和 `CarAppProvider`。DEX 中 `commonlayer.utils.a`、`carcontrol.view.StatementActivity` 等实际引用 `com.oplus.ocar`，`c8.d` 引用 `com.heytap.accessory`；`CarLinkApplication` 初始化车辆控制 SDK 和账户管理。

这些证据倾向于 **OPPO 手机侧连接/车辆生态管理应用**，而不是独立安卓车机 ICCOA 媒体接收端。尚未运行其外部厂商服务，保留推断界限；不能仅凭文件名将 A 用作 DiPlay 的车端 SDK。它也不是 Xiaomi CarWith，不应用它替换手机端来计算验收成功。

## 对 DiPlay 原生首帧的最小路线

1. **材料阶段已有进展**：找到真实内嵌车端接收结构及其 API 边界；“完全没有车端实现线索”的旧结论已过时。尚未取得独立 SDK 发行包、初始化示例/状态语义、许可与合法测试身份合同。APK 可见接口不赋予复制其类、SO 或身份进公开工程的权利。
2. **优先 ARM 受控台架**：确认原样本的合法本地测试依据、来源风险和单独安装批准后，才可以在隔离且受授权的 ARM Android 设备试运行；先测库加载与初始化，再分别核实 P2P/SoftAP 必要权限。当前用户只授权静态分析，本轮不执行此步。已有 K30 Pro 是 CarWith 手机侧，未改装为接收端。
3. **首次运行分层取证**：按实际 `ICarInitCallback`、发现/连接/PIN回调和 `onAuthenticationOk` 的合同记录阶段；由手机正常 CarWith UI 发起连接，走正常配对/认证。身份或权限不足就在对应阶段记录原始失败码，不绕过，不用第三方配置资产克隆身份。需要 SDK 方提供 CCD 的定义及合法生成/测试方式。
4. **DiPlay 集成走授权 SDK**：在本项目隔离 debug 后端中封装真实 `init → startAdvertise → 连接状态 → startCast(Surface) → disconnect/deInit` 生命周期，配置屏幕/FPS和P2P/SoftAP能力；保留音频焦点、反控和断线清理边界。方法顺序只是待合同验证的接入方案，不是已执行连接。若走独立实现，则还需足够的发现/认证/通道规范进行合法互操作开发，不直接复制反编译代码。
5. **第一帧通过后才测625级负载**：记录SDK媒体协商、首帧时间、持续帧/掉帧/音频/输入，再在真实 ARM 或可解释的低资源台架测试。既有x86合成解码/UI基准继续只作为基础证据。

当前最早未经验证的运行阶段仍是 **ARM SDK加载/初始化及权限条件**，不是已观测的认证失败。公开样本没有证明“必须外置连接盒”，也没有证明无需身份或平台集成。下一步所需是合法测试/集成依据和 ARM 接收环境、SDK接口与配置说明；不是再买连接器，也不是继续测无视频链路的UI。

## 实际验证工具与结果

已安装 Android SDK 36.0.0 `apksigner verify --verbose --print-certs`：两包验证通过；`aapt dump badging/xmltree`：身份、组件和权限解析成功。NDK28.2 `llvm-readelf -d`、`llvm-nm -D`：ARM64依赖和JNI导出读取成功；`dexdump -d` 输出在内存中筛选为调用目标元数据，不保留原始反编译输出。ZIP结构及DEX类/方法元数据使用本机Python静态读取；没有安装分析软件、运行APK、加载SO或访问第三方扫描站。

**本轮连接结果：发现未执行、配对未执行、认证未执行、媒体协商未执行、原生首帧未取得。** 静态分析不能替代 G0。
