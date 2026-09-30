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
- 接收APK的媒体库只有ARM32/ARM64；x86_64 USB库不能证明x86媒体SDK可用。此前AVD已关闭，本次未更改CPU亲和性或系统网络/安全设置。

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
