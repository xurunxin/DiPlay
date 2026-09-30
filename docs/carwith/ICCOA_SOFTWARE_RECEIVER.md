# ICCOA Carlink 纯软件车机接收器：技术可行性与最小 PoC

核查日期：2026-09-30。产品目标：**DiPlay 作为安卓车机应用，直接接收 CarPlay 与原生 CarWith；摆脱外置连接盒。** 本文研究 ICCOA，不改变 [Project #1](https://github.com/users/xurunxin/projects/1) 的实施依赖，不申请账号、同意条款、修改手机安全设置或宣布 G0 通过。当前没有取得 SDK 或规范正文，没有运行 ICCOA 会话。

## 核心结论与证据等级

**原生 ICCOA 软件接收器在架构上有官方依据；当前还不足以证明普通后装 APK 可以在目标车机上完成认证。** 推荐先评估“官方 Carlink SDK 嵌入 DiPlay”，最终产物仍是 DiPlay APK。现成商业接收 APP 可以是有授权的可选对照，不能是产品依赖或最终解决方案。完全独立实现协议也是候选，但目前缺规范、认证条件及许可，不能编造端口、UUID、握手或可信身份。

| 已读一手来源 | 已证实 | 不能据此推断 |
| --- | --- | --- |
| [SDK个人信息政策](https://www.iccoa.cn/suit_1.html)，页面更新2023-09-08 | SDK面向车机应用，嵌入应用并在其进程运行；无线功能涉及蓝牙、P2P/AP连接与绑定后发现 | 特定报文、BLE/Classic选择、SSID规则、MAC使用方式或硬件身份材料 |
| [SDK隐私安全说明](https://www.iccoa.cn/suit_2.html) | 有车端SDK和应用集成路径；应用需遵守准入资源规范、PRD及认证测试；发送给经过校验的车机应用 | SDK是否纯Java、ABI、普通APK/系统签名要求、离线/联网认证、专用芯片要求 |
| [官方CarWith公告](https://cdn.iccoa.cn/news/47.html)，2022-06-01 | 小米CarWith支持ICCOA Carlink；当时已有车端SDK及认证流程 | 2026目标设备版本矩阵、SDK兼容性、个人开发许可 |
| [互联技术要求介绍](https://www.iccoa.cn/tech/68.html)，2024-05-16 | 标准涉及投屏/桌面融合、有无线接口及性能 | 已获得协议正文或可以凭简介重建协议 |

在这些已读公开材料中**未发现外置连接盒或专用认证芯片的强制要求**。这是有边界的检索结论，不是已证明完全没有特定硬件要求；被引用但未取得的软硬件资源/认证规范可能仍含限制。蓝牙/Wi-Fi芯片、解码器等车机自身能力与“另买外置盒”是不同问题。

## 当前手机的精确兼容风险

[官方应用案例页](https://www.iccoa.cn/site/iccoaCase)由公开前端模块渲染。已核读页面路由与手机列表模块 `index-BELMxyRc.js`：版本提示属于车联列表，不是数字钥匙分支。其小米手机提示分别列出：

| 能力版本 | 页面当前的手机条件 | 当前 Redmi K30 Pro |
| --- | --- | --- |
| 经典投屏1.0 / 融合桌面全屏1.5 | MIUI13 / HyperOS1.0及以上；Android13及以上 | MIUI14满足系统代际提示，但Android12不满足此处Android13条件 |
| 融合桌面小窗1.6 / 镜像2.0 | HyperOS2.0及以上；Android15及以上；CarWith3.6.0及以上 | Android12、MIUI14、CarWith3.2.0均不满足该组当前提示 |

这是**手机能力提示，不是车机SDK最低Android版本**。3.6.0条件不能倒套到1.0；2022公告的历史上线范围也不能代替现在的逐型号确认。当前手机没有可见CarLife入口、有亿连入口及ICCOA标识，但尚未验证原生ICCOA兼容性。应核实该型号/ROM/CarWith3.2是否支持历史经典投屏及匹配哪版车端SDK；不能直接下“绝不兼容”结论，也不能以刷机/隐藏开关来补证据。若目标是2.0镜像，应另选官方确认支持的手机，不能把经典投屏的应用生态承诺成任意手机界面完整镜像。

## 公开规范、SDK、参考源码和许可

实际访问记录：新站已发布标准/产品文档/接入指南列表返回401；标准68页面的下载按钮调用 `POST /callback/download.jsp`、参数articleId=68，本次按同一公开方式请求返回code150001、“请先登陆再进行下载”。未提交凭据或绕过认证。规范简介和隐私政策不是API手册或线上的认证合同。

[在线申请](https://www.iccoa.cn/join/apply.html)要求单位名，说明邮箱用于成员索取技术资料；[实际联盟章程地址](https://www.iccoa.cn/about/11.html)提到符合条件的自然人和组织，以及运营会费。`/site/alliedMember`是联盟成员页面，不能当作章程引用。会费不是SDK报价；自然人可入会也不自动赋予SDK/再分发权限。个人项目、独立后装APK是否准入、是否需会员、开发/认证/发行费用，仍待具体答复。本轮不建议立刻付费或入会。

公开候选只读核查结果：

| 来源 | 许可/用途 | 本项目使用边界 |
| --- | --- | --- |
| [linuxhunter/iccoa2](https://github.com/linuxhunter/iccoa2)、[carkey_server](https://github.com/linuxhunter/carkey_server) | BLE/APDU、数字钥匙；前者未见许可证，后者README明确CarKey | 不是Carlink媒体投屏参考实现，不复用其认证协议 |
| [frisky1985/yuleDKCS](https://github.com/frisky1985/yuleDKCS) | Apache-2.0，数字钥匙系统 | 有许可也不等于投屏协议，不复用其安全SE要求来推定Carlink硬件要求 |
| [lvalen91/Carlink](https://github.com/lvalen91/Carlink) | API识别为Unlicense；README要求CPC200-CCPA连接盒，源码有UsbAdapterPlayer/USB协议层 | 是host-dongle架构；USB消息出现ICCOA名称也不能证明公开了手机侧原生协议，更不能作去盒方案 |
| [hyksosss/ICCOA-tool](https://github.com/hyksosss/ICCOA-tool) | 依赖已有com.ucarhu.demo的窗口调试启动器，要求DUMP权限 | 不是独立接收端，未安装/授予权限；非官方重发布不能证明SDK许可、身份或ABI |
| CarWithPlus等搜索结果 | 未取得足够实现和许可证据 | 未认定为独立投屏接收端，不导入 |

GitHub关键词检索和公开Maven的iccoa查询未核得可信许可的独立ICCOA投屏实现；Maven carlink查询超时，不能当作“无工件”证据。检索无结果不证明全球不存在方案。

DiPlay已有GPL源代码及第三方许可约束。SDK即使可以技术集成，也须核实嵌入、源码/二进制再分发、签名、修改及GPL兼容条件；不能认为动态链接自动解决许可问题。[GNU官方FAQ](https://www.gnu.org/licenses/gpl-faq.html#GPLIncompatibleLibs)说明不兼容库的链接许可需相应权利人授权。此处是待确认的工程准入项，不替SDK条款作法律结论；未复制厂商SDK或认证资产。

## 分层技术可行性

下表的模块边界是设计提案，不是已实现的ICCOA协议。确认/推测/未知分别标记，普通Android API可提供能力不意味着CarWith必然接受其协议。

| 层 | 已知与公开通用能力 | 仍缺的ICCOA合同 | 可自行完成的工作 |
| --- | --- | --- | --- |
| 发现 | 官方确认蓝牙、P2P/AP及绑定后发现；Android公开扫描/广播/P2P API | Classic或BLE、服务UUID/广播字段、手机主动/车机主动、发现时序及版本差异 | 只读能力/权限探测；拿规范后写有界发现状态机；不猜UUID或伪造品牌广播 |
| 配对/无线链路 | Android有系统配对与Wi-Fi Direct；车机可包含无线芯片 | 角色、频段、SSID/凭据交换、绑定标识、网络选择、车机并发联网条件 | 按公开API管理自身资源、明确请求必要权限；先以隔离fixture验证，不改全机网络 |
| 认证/版本协商 | 官方提到经过校验的车机应用、认证与准入；隐私政策泛述TLS等保护 | 包名/签名白名单、证书/密钥、provisioning、信任根、激活联网、软件还是硬件密钥、握手格式 | 设计私有身份提供接口/失败分类；取得规范与测试身份后实现；现有Apple MFi材料完全不能替代 |
| 传输/复用 | Android socket、Network、JNI均可在应用使用 | 端口、TCP/UDP选择、多通道/封包、加密边界、流控/心跳/重连；泛述TLS不等于所有媒体均走TLS | 按真实SDK回调/协议实现取消、超时、资源释放；用自己定义的fixture合同测试，清楚标记非ICCOA |
| 视频 | Android MediaCodec→Surface；DiPlay已有H.264/HEVC及有界解码队列 | codec/profile/level、CSD格式、帧边界/PTS、关键帧请求、SDK直接Surface还是交付压缩帧 | 独立fixture验证解码；只在格式确认后适配，不预设H.264是ICCOA强制格式 |
| 反控 | Android本地触控可由应用接收 | pointer/down/move/up/cancel、坐标/旋转、键盘/返回、焦点及ACK | 对纯坐标转换/状态语义测试；调用SDK合法输入API，不注入手机全局触控或绕过MIUI限制 |
| 音频/麦克风 | AudioTrack、AudioRecord、焦点/路由API；已有CarPlay媒体末端 | PCM/AAC/Opus等协商、音乐/导航/通话混音、HFP还是IP通道、麦克风格式、echo/延迟/焦点 | 先验证本地fixture播放；获准后再开启麦克风；不能直接复用AirPlay RTP包或猜通话路径 |
| 窗口/生命周期 | 普通应用Surface、前台服务等可用 | 小窗/镜像/OEM权限、驾驶限制、后台要求、多屏、车厂服务/签名 | 最小前台单Surface；依据已证实能力扩展；不把接入成功等同全部系统级能力 |

官方个人信息政策还提到导航GPS及行车限制相关档位/车速数据；SDK版本中哪些数据必需、普通APK如何通过车厂服务取得，以及缺失时的安全策略，仍需确认，不能虚构停车状态来解锁功能。无需外置盒与无需OEM权限也是两个命题。

SDK路线中，未知协议层可以由合法SDK承担，DiPlay仍负责宿主UI、媒体/窗口、权限、生命周期与诊断。全自写路线必须先取得足以独立实现且允许使用的线协议规范及测试身份；协议“开放联盟”名称不能代替公开可实现的报文。

## DiPlay现有组件核查

- [WifiP2pGroupManager](../../shared/src/main/java/com/shilapi/xcertplay/network/WifiP2pGroupManager.kt) / LocalOnlyHotspotManager已有资源生命周期和权限诊断；[WirelessHotspotManager](../../shared/src/main/java/com/shilapi/xcertplay/network/WirelessHotspotManager.kt)的security、持久记录、onCarPlayConfirmed仍绑定iAP2/CarPlay。不能直接用CarPlay SSID/身份宣称Carlink发现。ICCOA端需独立状态和凭据命名空间，且先由SDK说明决定链路由谁管理。
- [AndroidMediaSink](../../shared/src/main/java/com/shilapi/xcertplay/media/AndroidMediaSink.kt)可接Surface并使用MediaCodec，音频经AudioTrack；[MediaCodecSupport](../../shared/src/main/java/com/shilapi/xcertplay/media/MediaCodecSupport.kt)处理现有AVC/HEVC CSD/NALU。[VideoDecodeQueue](../../shared/src/main/java/com/shilapi/xcertplay/media/VideoDecodeQueue.kt)限制60帧/8MiB并触发重同步，decoder对250ms过期帧恢复。可在格式确认后复用末端，不能盲套CarPlay帧封装、当前本地生成PTS或关键帧命令；SDK若直接渲染Surface则绕过帧级sink。
- [VideoStats](../../shared/src/main/java/com/shilapi/xcertplay/media/VideoStats.kt)已有接收/输出计数，但输出是在releaseOutputBuffer后计数，不能等同物理屏幕已显示；touch2frame是触控至帧到达的代理值，不是完整端到端延迟。[Android渲染回调](https://developer.android.com/reference/android/media/MediaCodec.OnFrameRenderedListener)可提供Surface渲染时间，但可能延迟/批量通知，也不代表玻璃到玻璃时延。
- CarPlayController/CarPlayHostActivity输入与DiPlaySessionService耦合AirPlay和CarPlayBackgroundSession。真实SDK输入/生命周期合同取得前只做隔离实验，避免本轮大规模抽取。
- 当前没有ProjectionBackend接口；#7明确需G1后再大规模实施。建议将未来IccoaSdkAdapter与原生IccoaProtocolAdapter视为同一接收边界的不同实现选择，并支持SurfaceOwned / EncodedFrames两种媒体所有权；这些只是候选边界，不提前把推测写成生产接口。

## ARM车机与x86_64 AVD

真实车机应现场核对OS和进程ABI；64位SoC不保证系统/应用进程为64位。SDK至少需明确arm64-v8a/armeabi-v7a支持和minSdk，x86_64另列验证，不把ARM .so在AVD无法加载当作协议No-Go。没有SDK包时无法核实JNI、SDK是否自带decoder、Surface回调或系统服务依赖。

[Android Wi-Fi P2P](https://developer.android.com/develop/connectivity/wifi/wifip2p)与[蓝牙权限](https://developer.android.com/develop/connectivity/bluetooth/bt-permissions)提供公开API，但SDK需要何种角色、权限、OEM能力仍未确认。当前manifest已有BLUETOOTH_CONNECT，不能以此假设新发现路径已有SCAN/ADVERTISE；不无条件追加或授予全部权限。API33+ Nearby Wi-Fi与旧版定位条件需按实际调用核查，不能统一要求改手机安全设置。

[当前模拟器网络说明](https://developer.android.com/studio/run/emulator-networking)列出P2P和API31+ Classic/BLE蓝牙能力，部分能力要求Emulator36.5+。本机37.1不应一概宣称不支持蓝牙/P2P；但虚拟功能表和feature声明不能证明真实手机可经宿主无线链路连接该AVD、也不能证明SDK支持。未验证实机射频映射、认证和发现。因此AVD优先承担ABI加载、生命周期、fixture媒体与模拟peer测试；真实ARM车机是无线链路与性能验收的重要环境。

解码能力探测使用 [VideoCapabilities](https://developer.android.com/reference/android/media/MediaCodecInfo.VideoCapabilities) 的size/rate/profile声明，API29+可报告 [isHardwareAccelerated/isSoftwareOnly](https://developer.android.com/reference/android/media/MediaCodecInfo)。厂商声明不是实测吞吐保证；旧API不能仅凭codec名称猜硬解。记录实际codec名、输出尺寸/裁剪、频率与持续运行结果。

## 最小PoC顺序与验收

1. **P0本地软件基础（不依赖ICCOA身份）**：隔离debug实验页，仅输出ABI/OS、系统功能、权限当前状态与codec声明；使用有来源/许可的确定性视频fixture验证Surface解码、停止/重启和分层计时。先候选720p30 AVC单流；这是实验输入，不是声称ICCOA实际视频参数。只读探测不扫描/配对/建组，不读取真实MAC、账号或手机媒体。
2. **P1 SDK加载**：取得合法SDK/API与许可后检查AAR/jni ABI、minSdk、依赖、所需权限及签名/provisioning；以DiPlay内嵌SDK的debug实验模块加载、初始化/释放。获准使用真实ARM目标；x86不支持时fixture模块仍可留在AVD。成功标准是实际SDK加载及资源释放，不叫连接成功。
3. **P2真实链路**：明确兼容手机/SDK版本和合法测试身份；按官方流程记录原生CarWith发现、配对/链路、认证、能力协商各阶段，至少两次从干净状态重复。未认证不播放“连接成功”，不伪造车厂身份/签名或使用独立CarLife客户端。
4. **P3视频最小闭环**：先前台单Surface，不做小窗/全镜像承诺；拿真实codec/config/PTS或SDK Surface合同，记录首帧、接收/解码/提交/渲染计数，测持续10分钟与重连。SDK直接Surface时明确可观察范围，UI gfxinfo不能充当视频FPS。
5. **P4交互音频与回归**：依合同加入触控取消/旋转/焦点、音乐/导航/通话与用户许可后的麦克风；验证停启/后台/断网，回归现有CarPlay。多屏、小窗、镜像各自单独准入。实验成功也不自动改原路线图关卡。
6. **P5真实低端车机性能**：有真实视频链路后，在实际ARM/625级目标测首帧、持续FPS、丢弃/恢复、CPU/PSS、网络/解码/渲染时间和长跑；测法区分本地收到帧至Surface与端到端。后者需要时钟关联或可控视觉事件与外部测量，不能拿touch2frame或UI帧时间代替。

现有七组[UI基准](EMULATOR_FINDINGS.md)仅是UI；AVD内存请求1GB被提高至2560MB，x86/WHPX/SwiftShader未校准625。当前没有视频、输入、音频或端到端性能数字，本轮没有重启AVD或追加UI测量。

## 可独立实施的软件基础模块（提案，尚未编写）

| 内容/候选位置 | 依赖与实际收益 | 验证与边界 |
| --- | --- | --- |
| debug ReceiverEnvironmentProbe（mobile debug实验源集） | Android公开PackageManager/Build/MediaCodecList；不需SDK，明确真实车机ABI/解码声明/权限缺项 | 不读设备唯一ID/SSID/密码，不自动开蓝牙/Wi-Fi或授予权限；输出未知而非伪造支持 |
| debug DecoderFixtureRunner | 已有媒体末端、确定性fixture及许可；验证实际解码/Surface，而非更多UI压力 | 不抽取生产ProjectionBackend，不借Apple身份；记录输入hash/codec/config/rate、首帧/恢复与真实计时范围 |
| debug ReceiverMetrics与实验生命周期台架 | 单调时钟、独立计数，测试队列超时/释放和PID/PSS采样 | 将queued、decoded、Surface callback和物理呈现区分；不把模拟fixture叫CarWith视频 |

模块可以依上述顺序独立推进，具体产品后端签名仍等SDK合同；本轮只交付技术研究提案，没有擅自扩展生产实现或额外安装软件。

## 最小下一步与精确缺项

建议首先取得**可嵌入DiPlay的Carlink车端测试SDK/API**或**允许独立实现的完整规范**。所需答复清单：当前手机历史协议版本/型号矩阵、普通后装Android APK可否接入、minSdk与ARM/x86 ABI、发现/配对角色、认证身份/签名/provisioning与是否存在硬件密钥要求、媒体/输入/音频API、测试许可/GPL兼容再分发条件、开发/认证/发行费用。没有必要先买连接器或付会员费。

可将上述清单作为具体技术询问草案；本轮没有发送消息、申请或接受条款。若资料允许软件嵌入且P2可复现，就按SDK路线做DiPlay内的最小闭环；若可获完整开放规范与身份，评估独立协议实现；若最终仅OEM系统应用可用，则针对目标车机签名/系统集成单独评估，不能从当前401提前判死纯软件目标。
