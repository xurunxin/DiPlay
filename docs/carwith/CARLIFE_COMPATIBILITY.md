# CarWith 的 CarLife 兼容路线：新证据与当前 PoC

更新：2026-09-30。执行计划仍为 [Project #1](https://github.com/users/xurunxin/projects/1)，不是改成 ICCOA 项目。产品目标仍是 DiPlay 在安卓车机上直接接收 CarPlay / CarWith，摆脱外置连接盒。

## 已修正的结论

用户提供的新 HyperOS 3 手机截图，已由父级任务实际查看，显示 CarWith 的 **CarLife连接** 设置及弹窗：标题“下载CarLife组件”，正文“如要通过CarLife协议使用CarWith功能，请保证手机内存在CarLife组件”。只记录相关文字，不提交带通知栏的截图。

这证明该手机的官方 UI 提供 **CarLife 承载 CarWith 功能** 的兼容路径。此前旧 Redmi K30 Pro / Android 12 / CarWith 3.2 未见 CarLife 入口的结论只适用于该组合，不能推广到新手机或所有 CarWith。新手机的型号、Android/CarWith 版本、组件包名/版本/安装来源仍待核实；不能把 HyperOS 3 自动换算为已测 Android 版本。

截图不证明组件已安装、车端身份有效、握手成功或收到视频。`com.baidu.carlife.xiaomi` 仅是未核实的候选包名，未据此下载/安装 APK。普通独立 CarLife 应用也不能替代截图要求的组件。

当前优先 PoC 恢复为原 CarLife 兼容路线；ICCOA 保留 [独立研究](ICCOA_SOFTWARE_RECEIVER.md)，取得 ICCOA SDK 不再作为 CarLife 后端的前置条件。两种接入方式分别验证，不互相代替。

## 一手资料与历史参考的边界

| 来源 | 已确认 | 尚未证明 |
| --- | --- | --- |
| [小米 CarWith 第三方共享说明](https://privacy.mi.com/CarWith-share/zh_CN/)，页面版本 v20221028 | Apollo 通过 IPC 用于显示 CarWith 音视频，列出加密显示的车机渠道号、车机蓝牙地址等特征 | 新手机组件身份、当前车端协议/鉴权格式、是否接受历史调试渠道 |
| [百度官方接入流程](https://carlife.baidu.com/carlife/caroem/start) | 按手机厂商或车企/Tier1 类型申请，分配文档或 SDK；集成资料包含接入指南、HMI 与测试规范，之后自测与设备验收 | 个人项目授权范围、SDK ABI、渠道申请费用、当代 CarWith 组件兼容性；本任务没有提交申请或接受条款 |
| [历史 Android 车端示例](https://github.com/674809/carlife/tree/f9522db31a5244b89a2f3f555b2e0ad882b48674/CarLife-Android-Vehicle)，README 更新 2017-11-09 | 提供 Android 车端工程，连接/视频/音频/触控/协议分层；根目录与已检查库文件有 Apache-2.0 声明；README 描述 H.264、USB Host、蓝牙条件 | 仓库是历史镜像，不能当成最新百度发布或当前认证；各二进制/资源的完整授权仍需逐项检查。旧调试渠道不能自动用于 CarWith 或量产 |

历史工程可供研究软件接收架构，但有旧 Gradle/SDK、jcenter、配置与渠道依赖。没有将其中的二进制、签名、调试身份或协议实现引入 DiPlay，也没有运行它或改手机。公开历史示例说明软件车端方向值得验证，不保证普通后装 APK、模拟器或所有手机无需任何硬件能力/系统权限。USB Host 和真实无线链路需各自建立台架；模拟器启动、网络 TCP 连通都不算 CarWith 握手。

## 原 G0 仍缺什么

[Issue #3](https://github.com/xurunxin/DiPlay/issues/3) 要求固定手机/车端组合、合法参考接收端、原生 CarWith 操作步骤、实际失败阶段及可复现记录。Go 需要重复成功与连续视频；Blocked/No-Go 报告不能解锁下游。[Issue #4](https://github.com/xurunxin/DiPlay/issues/4) 的来源/许可/身份审查同样必须完成。[Issue #5](https://github.com/xurunxin/DiPlay/issues/5) 明确在 G0 通过后启动。

截至本轮，ADB 实际可见的仍是旧 Redmi K30 Pro / Android 12；没有把新手机截图的数据填到旧手机记录中。旧组合保留 [原记录](m0-record.json)，新组合单独记录为 [Blocked](hyperos3-m0-record.json)。缺项：

1. 新手机型号、ROM/Android/CarWith 版本；从官方 UI 获得组件的确切来源、版本和授权安装状态。
2. 可合法使用、与此组件匹配的车端源码/SDK或参考接收台架，及其许可、测试渠道和认证条件。不要猜端口/消息 ID 或复用第三方身份。
3. 固定有线/无线网络角色与物理链路，实际观察发现、配对、握手、连续视频；至少两次重复记录及失败阶段。视频、输入、音频分别验收。

因此没有开始 G0 强制门后的 CarLifeSession、也没有宣布 G0 通过。当前可独立完成的是 [Issue #11](https://github.com/xurunxin/DiPlay/issues/11) 中不依赖会话结论的台架与媒体测试基础。

## 本轮实现与验证路径

新增 debug 专用 `ReceiverBenchActivity`，探测公开 Android 能力并运行自产 AVC 测试图案，经 MediaExtractor → MediaCodec → Surface 输出。它不使用车端渠道、身份或手机协议，不启动现有 CarPlay 会话；不读手机私人内容、不扫描/修改网络。只保存在自身 app 私有目录的一份合成测试报告。入口由 `android.permission.DUMP` 限制，release 构建不包含它及测试视频。

报告区分输入数、解码输出数、Surface 回调数和 EOS；有超时、取消及页面离开资源释放。未限速解码吞吐不等于屏幕稳定帧率，回调缺失不能当成投屏掉帧。队列到 Surface 时间戳延迟不等于端到端延迟。具体命令、fixture 来源和实测见 [台架说明](RECEIVER_BENCH.md)。这验证媒体基础而不是 G0 或 CarWith 支持。

## 最小下一步

用户可先接入新手机并确认电脑的调试授权，再在官方 CarWith 下载入口查看组件的官方来源/包信息；遇到安装、权限、登录或条款提示时停在提示页，本任务不代替用户同意。后续安装与网络/USB 接线操作按具体所需另行确认。提供合法匹配的车端开发材料或可授权参考台架后，先做固定组合 G0；通过后才按原计划实现 CarLifeSession 和真实视频链路。不能为赶进度用普通 CarLife 成功、模拟图案或 ICCOA 研究充当原生 CarWith 的成功证据。
