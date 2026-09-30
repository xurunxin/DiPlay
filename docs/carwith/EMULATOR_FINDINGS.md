# CarWith 真机与模拟器核查（2026-09-30）

> 后续产品目标已明确为DiPlay内嵌软件接收器、摆脱外置盒。最新官方证据、兼容矩阵和分层PoC见 [纯软件接收器研究](ICCOA_SOFTWARE_RECEIVER.md)。商业接收APP只可选作授权对照，不是最终产品依赖。

[完整路线图 Project #1](https://github.com/users/xurunxin/projects/1) 的范围与依赖见 [M0_REVIEW](M0_REVIEW.md)。本报告更新同日较早的“无 ADB 设备”快照。**G0 仍为 Blocked；没有 CarWith 握手、视频流、反控或音频成功记录。** 本次实现的是只读设备采集、门禁证据检查、可复现 AVD 配置及实际 UI 基准工具，产品仍未实现 CarWith 后端。没有将完整路线图改为文档任务或替代协议。

## 手机和接收端可行性

用户接入的 Redmi K30 Pro 已通过 ADB 授权，Android 12 / API 31，MIUI V14.0.4.0.SJKCNXM；未识别为 HyperOS，地区未单独核实。实际活动包 `com.miui.carlink` 为 `3.2.0-20241009`（103002000），安装来源为小米应用商店。系统旧版本 1.0.18 不能当作插件版本；插件仍未独立确认。采集只读取明确属性与该包版本，不读取通信录、账号或其他个人内容。公开记录不包含设备序列号、原始 dumpsys 或手机截图。

用户明确确认：当前 CarWith 没有可见百度 CarLife / CarLife 连接入口，但有亿连连接；亿连页没有详细说明。手机 CarWith 页面也显示 ICCOA 标识，这只是方向证据，不能证明此版本与任意接收端兼容。包内存在 CarlifeDialogActivity 名称不构成入口或握手证据，未强行调用隐藏接口。一次普通 ADB 滑动被 MIUI 以 INJECT_EVENTS 拒绝，未绕过、授予权限或改变安全设置。

当前 DiPlay 只有 CarPlay 产品后端，没有合法可直接运行的 CarLife、亿连或 ICCOA 接收端。固定版本历史 CarLife 示例要求独立手机客户端、车机 USB host/H.264 解码等条件，生产渠道身份需官方取得；2017 示例不是当前 CarWith 授权和兼容证明。因此没有导入历史渠道身份、猜测私有协议或用独立 CarLife App 替代 CarWith。

| 路径 | CarWith 支持证据 | 接收端/SDK与授权 | 当前 AVD 条件 | 最短真实验证路径 |
| --- | --- | --- | --- | --- |
| 原 CarLife | 此手机版本没有可见入口，G0 前提不成立 | 历史源码可供审查；当前版本兼容、接收身份、授权未取得 | 缺当前可运行且授权的接收端；USB host 功能未声明 | 换用明确提供 CarWith CarLife 入口的目标设备，配合法接收端完成至少两次真实会话与身份核对 |
| 亿连 / CarbitLink | [官方产品页](https://www.carbit.com.cn/new/connect) 明确列出小米 CarWith；当前手机有亿连入口；精确版本矩阵未知 | 官方提供 APP/SDK 交付；[国内标准版](https://www.carbit.com.cn/10th/standard_apply) 是 Android 使用申请，非本次已获得的包；价格、测试/再分发许可、认证条件未知 | 官方公开页未确认 x86_64/API34/模拟器支持；未取得包与 ABI 清单 | 取得兼容本版 CarWith 的官方测试接收端与许可，核实 ABI/网络说明；获安装或网络变更授权后再按官方流程连接真实 CarWith |
| ICCOA Carlink（原计划 #15 R1） | [联盟首页](https://www.iccoa.cn/) 描述手机车机互联，小米是发起方；手机页面有 ICCOA 标识；精确版本支持仍待核验 | 官方有 [Carlink 接入](https://www.iccoa.cn/site/iccoaTech?id=3)、[产品文档](https://www.iccoa.cn/site/iccoaTech?id=2)、[认证测试](https://www.iccoa.cn/site/iccoaTech?id=4)；本次未取得 SDK/接收端/开发凭据；费用未知 | 公开首页未说明 x86_64 或模拟器支持；Wi-Fi/蓝牙功能声明不证明真实发现或认证可用 | 通过已有合法文档权限或授权供应商取得版本矩阵、测试 SDK/接收端、身份及连接说明；核实 ABI 和链路，再做真机发现、认证与首帧 |

本次官方资料访问记录：网页工具对亿连页面和部分 ICCOA 子页报 restricted URL，随后用普通 HTTPS 只读访问取得亿连官方产品和标准版申请页。ICCOA 新技术页只返回 JavaScript 加载壳；读取其公开页面模块后，按页面相同方式请求已发布 guides/prd/standard 文档列表，服务返回 `401`、“认证失败，无法访问系统资源”。未登录、申请、提交表单、接受条款或探查需认证文件。旧 `/about/13.html` 和 `/about/12.html` 分别返回会员权益/组织架构页面，并非有效接入资料。该访问结果**不证明会员资格是所有合法测试的前提，也不证明不存在可用方案**。

建议保持原 CarLife G0 为 Blocked，将原 #15 R1 的官方接入可行性研究与亿连授权接收端核验并列评估。亿连对 CarWith 的公开兼容证据更直接；ICCOA 适合研究系统级接入，需先拿到实际技术条件。目前没有证据足以选定产品后端。可先向已有授权供应商索取资料；对外联系、申请、费用、同意条款、安装新接收端与网络变更仍需明确授权。本次没有改 Project 范围。最小缺项为：当前 CarWith 版本矩阵、合法测试接收端/SDK、ABI/API支持、认证与链路说明、测试和集成许可。若模拟器不受支持，可用已授权且兼容的真实车机先证明链路。

独立R1能力边界、当前代码契合度与研究准入结论见 [ICCOA ADR](ICCOA_ADR.md)。

## 实际 AVD 配置与边界

用户已批准通过官方 sdkmanager 安装 Emulator 并创建测试 AVD。已安装 Emulator 37.1.11（15917651），WHPX 可用；新建隔离 `DiPlay_G0_API34`，使用已有 Android 34 / Google Play / x86_64 镜像 revision 14。原有其他 AVD 未修改。主机为 i5-13600KF（14核/20逻辑CPU），物理内存 68,523,835,392 bytes。AVD 为 headless、无音频、无快照、SwiftShader 软件渲染，实际屏幕 override 1280×720 / 240 dpi，配置60Hz。

| 配置 | 请求 vCPU/RAM | 实际 RAM | 宿主 CPU 限制 |
| --- | --- | --- | --- |
| baseline | 4 / 2048 MB | Emulator 提高至2560 MB；guest MemTotal约2.42 GiB | 未限制 |
| low_resource | 2 / 1024 MB | 同样提高至2560 MB；guest MemTotal约2.42 GiB | 未限制 |
| low_resource + affinity | 2 / 1024 MB | 同上 | 仅本任务 emulator 进程 affinity 0x3，即 Windows 逻辑CPU0、1；补测后已还原0xfffff |

两种 RAM 请求均在日志出现 `Increasing RAM size to 2560MB`；实际 `hardware-qemu.ini` 也是2560。因此本次**没有测成1GB内存配置**。没有改变全机电源、虚拟化、网络或安全设置。初次 userdata 启动日志64.747秒；后续低资源重启日志22.708秒；四核复测从启动器返回至轮询看到 boot_completed约24.8秒。这些使用不同初始化状态与测法，不是可比较的系统冷启动基准。

[高通官方骁龙625页面](https://www.qualcomm.com/smartphones/products/6-series/snapdragon-625-mobile-platform) 的公开检索规格为64位、最高2GHz、Adreno506与HEVC能力；本次直接页面抓取失败，未从该页面核验具体CPU微架构。x86_64/WHPX在现代Intel宿主执行，与ARM移动SoC的指令、IPC、频率、调度、内存带宽和热约束不同。SwiftShader也不代表Adreno506，模拟器的MediaCodec后端不能代表真实硬解；本次更未运行解码fixture。核数/RAM/affinity不构成芯片校准，没有骁龙625实机对照，所有结果只用于有限资源压力探索。

实际安装并启动的是本仓库源码构建的 debug APK，`com.shihab.diplay.hudtest` / 0.2.6-hud-test / code25，SHA256 `893F8641CB8623B04283D348027A6E693803C05E93BF7DF521B455714124AFFC`。ZIP核查没有 offline-mfi 或 .pk8/.p7b/.pem/.key；仅安装到模拟器，没有安装手机应用。首页如实显示 Setup needs attention（缺CarPlay认证材料），未点击连接或授予额外权限。

## 测法和实测结果

基准工具只接受 `emulator-N`，拒绝物理手机选择器。每组5次 force-stop 后 `am start -W` 的 TotalTime；不清OS/磁盘缓存，称进程冷启动。随后约60秒在home/settings间切换并滑动；`gfxinfo reset/framestats`统计UI渲染帧与jank，每10秒记录PSS/RSS、PID和CPU。[Android官方 dumpsys 说明](https://developer.android.com/tools/dumpsys)是UI与内存诊断参考。UI总帧除以时间不是持续视频FPS；jank不是投屏掉帧；直方图不是端到端延迟。

首轮原始数据：

| 配置 | 启动中位数 / 样本p95 ms | UI帧 / jank | UI p95 / p99 ms | PSS KiB |
| --- | --- | --- | --- | --- |
| [四核](performance/baseline-ui.json) | 467 / 598 | 2031 / 7.39% | 32 / 150 | 79071–94203 |
| [两核](performance/low-resource-ui.json) | 601 / 663 | 2072 / 7.48% | 32 / 150 | 79138–92760 |
| [两核+affinity](performance/low-resource-affinity-ui.json) | 628 / 653 | 2034 / 7.23% | 32 / 150 | 79144–91904 |

首轮 `dumpsys cpuinfo` 没有测试窗口内的应用数据，CPU留null，未填零。补测新增 `/proc/<app pid>/stat` utime+stime 增量 / `getconf CLK_TCK` / 单调时钟间隔，100%表示一个guest CPU；第一样本没有前值，留null。保留独立dumpsys历史窗口，不能与增量结果混用。

| CPU补测配置 | 启动中位数 / 样本p95 ms | UI帧 / jank | CPU单核百分比区间 | PSS KiB |
| --- | --- | --- | --- | --- |
| [四核，紧接重启](performance/baseline-cpu-ui.json) | 1572 / 2988 | 1980 / 6.97% | 40.38–43.31 | 77375–89781 |
| [四核，后续稳定复测](performance/baseline-warm-cpu-ui.json) | 793 / 890 | 2020 / 6.58% | 40.26–44.81 | 76737–90177 |
| [两核](performance/low-resource-cpu-ui.json) | 557 / 747 | 2052 / 6.63% | 34.67–37.57 | 79995–93912 |
| [两核+affinity](performance/low-resource-affinity-cpu-ui.json) | 531 / 558 | 2045 / 7.04% | 31.79–42.36 | 79860–93600 |

补测四核背景启动干扰明显，稳定后仍与首轮有差异。未随机化顺序、隔离宿主负载或校准P/E核心，因此不能推断限制CPU使应用更快、给出芯片性能排序或稳定吞吐承诺。n=5时nearest-rank p95就是最大样本。每组60秒观察到PID变化0；这不是长时间稳定性/ANR完整证明。首轮与补测共约7分钟UI压力，不满足路线图60分钟与20次投屏重连验收。

**未测项：** CarWith发现、握手、连续视频稳定FPS/掉帧、编解码延迟、反控、音频、端到端延迟与真实低端车机长跑。JSON中的projection_fps/projection_latency_ms始终null。

## 复现（仅已创建的测试 AVD）

在有Python和现有Android SDK的PowerShell中执行；选定AVD需已有相同镜像/配置。脚本不安装软件，不停止其他模拟器。启动后等待boot_completed=1；为减少启动后台负载，先观察系统稳定并记录预热时间。两配置轮流运行时只显式停止本任务AVD，保留userdata，记录运行顺序。

```powershell
$env:PATH=(Join-Path $env:ANDROID_HOME 'platform-tools')+';'+$env:PATH
# 路径替换为自己已授权的隔离 AVD / 日志目录
scripts/start_emulator_profile.ps1 -Profile baseline -AvdHome <avd-directory> -LogDirectory <log-directory>
adb -s emulator-5580 shell getprop sys.boot_completed
adb -s emulator-5580 shell wm size 1280x720
adb -s emulator-5580 shell wm density 240
adb -s emulator-5580 install -r mobile/build/outputs/apk/debug/mobile-debug.apk
python scripts/benchmark_emulator_ui.py --serial emulator-5580 --profile baseline --duration 60 --output <new-baseline.json>
# 确认该 serial 仍是自己的测试 AVD 后再停止它
adb -s emulator-5580 emu kill
scripts/start_emulator_profile.ps1 -Profile low_resource -AvdHome <avd-directory> -LogDirectory <new-log-directory>
# 等启动完成，重新核实显示/实际CPU/RAM，再运行
python scripts/benchmark_emulator_ui.py --serial emulator-5580 --profile low_resource --duration 60 --output <new-low.json>
# 第三组可在启动时加 -LimitHostCpu，仅限制新启动的 emulator 进程
# 基准命令加 --host-affinity 0x3 记录标签；标签不代替实际进程affinity核验
# 完成后停止自己测试AVD，或还原进程原affinity；不要限制其他进程
```

公开记录仅包含经过筛选的模拟器指标。脚本不会覆盖已有JSON；每次用新文件名。每条ADB命令有30秒超时，单次UI阶段duration限制30–600秒。完整任务最长仍取决于多条ADB命令累计时间，不承诺全流程严格60秒。此段是原 UI 测试快照；测试实例的后续启动/停止以最新台架记录为准，不代表一直运行。

## 当前验收状态

#3已获得旧手机元数据与模拟器UI环境；旧 K30 Pro 当时没有可见 CarLife 入口，新 HyperOS 3 手机现已提供官方组件入口证据（见 [更新](CARLIFE_COMPATIBILITY.md)）。当前授权匹配接收端、组件信息与连续视频仍缺，不能关闭；#4静态来源审查已完成，当前候选SDK/认证/测试与集成许可仍缺。#5需要G0，#6需要#5且真实流，#7需G1后再大规模抽取，#8/#9依赖#7，#10依赖#8/#9；这些后端工程项不能以UI基准解锁。#11的独立测试基础设施已推进，发行/硬件验收未完成；#12–14保持Deferred。#15已补充官方ICCOA访问与候选条件调查，未实现协议。没有合并或部署。
