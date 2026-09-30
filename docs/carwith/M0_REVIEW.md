# CarWith M0 接入审查与实验记录

关联 [Project #1](https://github.com/users/xurunxin/projects/1)、[路线图 #2](https://github.com/xurunxin/DiPlay/issues/2)、[设备基线 #3](https://github.com/xurunxin/DiPlay/issues/3)、[许可/认证 #4](https://github.com/xurunxin/DiPlay/issues/4)。核验日期：2026-09-30。

## 结论与当前证据

> 以下是同日较早的无设备快照。随后已接入真实手机、启动隔离 AVD 并完成 UI 基准；当前入口和授权阻塞见 [真机与模拟器核查](EMULATOR_FINDINGS.md)。用户确认当前手机没有可见 CarLife 入口、只有亿连；G0 仍未通过。

**G0 = Blocked，不能解锁 M1。** 尚无目标手机/车机的软件矩阵、CarWith 入口与兼容插件信息、授权参考接收端、真实重复连接和连续画面证据。DarkFlame 上执行 `adb devices`，仅返回空设备列表；这只说明本机没有可供此次 ADB 验证的设备，不能据此判定 CarWith 不兼容。没有运行真实 CarWith 会话，也没有形成产品 No-Go 结论。

本 PR 交付 M0 静态来源审查、准入/停止条件、台架实验流程、结构化记录和完整性检查器。#3/#4 仍需目标设备和接入资料，不能自动关闭。M1–M3、M4、R1 的完整验收没有完成；不宣称已实现 CarWith、CarLife 或 ICCOA 后端。

直接从 GitHub GraphQL 读取 xurunxin 的 Projects：只有此一个 Project，`hasNextPage=false`。读取 Project 说明、17 个字段、3 个视图、14 个项目项（#2–#15）、每项字段与关联 Issue 全文，所有连接均无后续页。Status/Stage/Priority/Area/Readiness 与依赖按当前线上数据核对，未从历史记录推断。

实际视图配置：`View 1` 和 `CarWith · 全部任务` 无分组、过滤或排序；`CarWith · 执行看板` 按 Status 垂直分组，无过滤或排序。项目说明建议的 Stage 分组、Priority 排序、Ready 筛选尚未配置，本次不修改看板。

| 工作 | 当前依赖与准入 | 本次结果 |
| --- | --- | --- |
| #3/#4 M0 | Ready，真实入口、固定版本、许可/认证审查 | 静态审查与实验准备完成；G0 受阻 |
| #5/#6 M1 | G0 明确通过；#6 依赖 #5 | Blocked，未开始协议实施 |
| #7 M2 架构 | #6 连续 CarWith 视频 G1 | Blocked |
| #8/#9 M2 输入/音频 | #7 | Blocked |
| #10 M3 生命周期 | #8/#9 | Blocked |
| #11 M3 验证/试用 | #10/#4 | Blocked；不发布版本 |
| #12–#14 M4 发现/USB/HUD | #11，各自设备验证 | Deferred |
| #15 R1 ICCOA | 独立研究，无硬前置；需要官方/授权材料 | Deferred，未获得 SDK/接入条款 |

G1 要求自有接收原型显示目标 CarWith 连续画面；独立 CarLife App 成功不能替代。G2 要求视频、单指输入、音乐/导航音频与 CarPlay 回归。G3 要求 60 分钟及 20 次连接循环、恢复、脱敏诊断与设备矩阵记录。研究 Issue 关闭或单元测试通过都不能替代这些证据。

## 来源、版本与逐组件准入

本仓库基线 `1e9d97f97cd15975999c999bff1b181c83d824f1`。下列远端 SHA 是本次读取的版本锚点，不表示本仓库所有衍生文件与该版本逐字一致；导入任何组件前必须逐文件复核来源、修改与 NOTICE。

| 组件与来源 | 固定版本/许可证据 | 实施、试用和商业分发条件 |
| --- | --- | --- |
| [DiPlay](https://github.com/shihabal3amri/DiPlay/tree/11dc9581df5323170e8033efb6f6b318857a5c81) / 本仓库接收器 | 上游 SHA `11dc9581df5323170e8033efb6f6b318857a5c81`；本地 LICENSE 为 GPL-3.0，现有 THIRD_PARTY_NOTICES 记录来源 | 保留来源、许可证及修改说明；分发时提供对应源代码。品牌与认证身份不随代码许可授予 |
| [xcertplay](https://github.com/shilapi/xcertplay/tree/3753867f0dd0e5c03490b987fb9df49b8ac96472) | master SHA `3753867f0dd0e5c03490b987fb9df49b8ac96472`；本仓库保留 GPL-3.0 与上游 README | 按 GPL 条件处理衍生代码；参考版本不是 CarWith 兼容证明 |
| [DiAuto](https://github.com/shihabal3amri/DiAuto/tree/71e65588dee081043a3b9d079752bf5ad17c2db9) UI/网站 | SHA `71e65588dee081043a3b9d079752bf5ad17c2db9`；本地相关文件及 `docs/licenses/DiAuto-AGPL-3.0.txt` | 保留 AGPL 声明及对应源代码义务，部署网络交互版本须审查适用义务 |
| [历史 CarLife C++ 源码](https://github.com/674809/carlife/tree/f9522db31a5244b89a2f3f555b2e0ad882b48674/CarLife-Vehicle-Lib/LibSource) | SHA `f9522db31a5244b89a2f3f555b2e0ad882b48674`；根 LICENSE 是 Apache-2.0，`include/CCarLifeLib.h` 有 Baidu Apache-2.0 声明 | 可作互操作研究参考；导入前检查每个源文件、生成 protobuf、运行库及分发二进制的单独许可/NOTICE。根许可不足以批准整包；本次未复制源码或二进制 |
| CarLife 历史 protobuf/认证接口 | 同一固定树含 AuthenRequest/Response/Result proto；Request 含 randomValue | 证明历史接口有认证消息；不能证明目标 CarWith 的身份、算法、令牌或量产准入条件。需目标版本规范/授权实验核验 |
| BYD HUD 图像 | 本地 notices：BYDMate PolyForm Noncommercial 1.0.0，源头未独立确立 | 与代码许可分开；商业包不得因 GPL/Apache 代码可用就携带此资源，需独立授权或移除。M4 后续另审 |
| Apple 图标、MFi 运行身份 | 本地 notices 与 BUILD.md：图标为 Apple 资产；现有试用身份来自固件研究、未获新 MFi provisioning | 不授予新用途或再分发许可。CarLife 会话不得使用 Apple 身份；真实身份不进 Git/CI；本次不使用、复制或读取身份文件 |
| ICCOA / 目标 CarWith 与插件 | [官方入口](https://www.iccoa.cn/)，尚无目标 SDK、版本资料、渠道身份及分发授权 | 仅列 R1 未决项；源码公开可下载不等于手机插件、SDK 或商业认证开放 |

现有 Kotlin/AndroidX/Compose、Bouncy Castle、JmDNS 等运行依赖的组件清单与许可证见 [THIRD_PARTY_NOTICES](../THIRD_PARTY_NOTICES.md) 和版本目录；新 CarLife 依赖尚未选择。本表是工程准入清单，最终许可适用性仍需权利方/维护者审查。

## 准入与停止条件

实现：先取得目标设备使用授权、准确 CarWith/插件入口和版本、可合法使用的参考接收器。确认连接方向、IP/端口、协商、协议版本、认证/渠道身份与试验范围。只在自有/获授权设备且停车或台架进行。不得伪造包名/渠道身份、提取或复用未经授权的证书/私钥、绕过车辆安全限制。权限或合法参考实现缺失就记录 Blocked；明确入口不支持可记录可复现 No-Go。

试用：除 G0/G1/G2/G3 设备证据外，需要单独确认插件、SDK、运行身份和 APK/资源试用分发许可，保留源代码/通知与回退办法。商业分发：另确认商用 SDK/接入/认证合同、所有资源权利、商标与对应源代码义务。当前两类分发均未获批准；本任务只提交草稿 PR，不发布 APK。

Apple 路径保持现有边界；未来 CarLifeSession 和其 CI 必须不依赖 `DIPLAY_AUTH_ASSETS_DIR`。当前测试生成合成身份，`scripts/check_public_tree.py` 检查已跟踪凭据容器与私钥块；它不是任意个人信息或所有秘密的检测器。新证据提交前需人工审查，不把此脚本通过等同于已完全脱敏。

## 最小真机下一步

1. 在私有本地记录手机型号、地区/ROM、HyperOS/Android、CarWith/插件版本与官方安装来源；车机型号/固件/Android、显示分辨率、触控、网络角色。插件确实不存在时写明“不适用”及查证来源，不用猜测版本。
2. 提供获授权参考接收端的名称、版本、来源和使用条件；确认当前身份/渠道/认证流程。仅批准的测试身份保存在 Git 外；不要求在聊天或 PR 中上传凭据。
3. 停车/台架，选择同一 LAN 或获支持热点，记录双方角色与连接方向。本地使用已知 IP；公开报告用 phone-A/headunit-A 等代号，删除 IP/MAC/SSID、序列号、路线、账号与令牌。
4. 从 **CarWith 的真实入口** 连接参考接收端，录制经过审查的界面/连续画面证据，记录步骤、提示、握手/视频阶段与失败点。断开后重复至少一次，确保相同固定组合可复现；两次是本次模板的最低重复性规则，不是通用兼容认证。
5. 单独运行独立 CarLife App 作对照，标记不同来源；不能拿该结果替代 CarWith。记录音视频与输入支持/不支持/未测试，不提前宣称熄屏、小窗、USB 或 HUD 能力。
6. 填写 `m0-record.json` 的本地副本；每次运行包含 `source: CarWith`、`continuous_video: true/false`、date、steps、evidence_reference、observed_limitations。证据引用可为有权限限制的审核报告链接，不要把原始抓包、手机画面或凭据直接放入仓库。
7. 失败写 Blocked 或 No-Go、原因与最小下次实验。成功写 Go，并提供 authorization_review、license_review、authentication_conditions 与独立 App 对照结论；维护者核验实际证据后明确签署 G0，才更新 Readiness 和开始 #5。

## 记录检查器

```powershell
python -m unittest discover -s scripts/tests -v
python scripts/check_carwith_m0.py docs/carwith/m0-record.json
python scripts/check_public_tree.py
```

退出码：0 = Go 记录字段完整，可送人工审查；1 = 格式/必需字段错误；2 = 有效 Blocked/No-Go，G0 不开放。检查器不访问网络/设备、不验证链接、证据真实性、法律许可或视频，不自动更新 Project、不关闭 Issues、不宣布 G0 通过。合成测试记录只测试规则，不是实机证据。

本次默认记录明确 Blocked，设备字段留空。资料补齐后最小下一步是步骤 1–4 的授权台架实验，而不是继续猜测协议或重构生产路径。

## 只读设备版本采集

`collect_carwith_baseline.py` 是 #3 的可独立实施工具。参考 Android 官方 [ADB](https://developer.android.com/tools/adb) 与 [dumpsys](https://developer.android.com/tools/dumpsys) 指南：显式选择设备，使用 getprop 白名单和指定包的 package dump，输出仅保留版本字段。没有调用安装、授权、Root、连接网络、抓包或账号/认证读取命令。ADB 只用于台架准备，不作为日常投屏前提。

```powershell
python scripts/collect_carwith_baseline.py --serial DEVICE_SELECTOR --role phone --package ACTUAL.CARWITH.PACKAGE --package ACTUAL.PLUGIN.PACKAGE --output C:\local\phone-baseline.json
python scripts/collect_carwith_baseline.py --serial DEVICE_SELECTOR --role head_unit --package com.shihab.diplay --output C:\local\headunit-baseline.json
```

必须替换占位符，包名从目标设备已知配置确认；不猜 CarWith 包名。设备选择器仅用于调用，不保存进 JSON。原始 dumpsys 与 stderr 不保存/回显；不读取序列号、IP/MAC/SSID、账号、路线或认证资产。输出仍需人工审查，型号/版本和所选包名可能暴露设备组合。输出文件用排他创建，不覆盖已有文件；请保存到 Git 外。

退出 0 只表示元数据采集完成；退出 2 表示设备未授权/不存在、超时、缺应用版本或输出文件不可用。空属性保存为 null，不伪造值。HyperOS/地区 ROM、安装来源、显示/网络角色、CarWith 入口、授权和实际投屏仍需人工填写到 M0 记录；采集文件不是 Go 证明。

本机无设备路径已实跑：返回 2 且没有产生输出文件。12 个合成测试覆盖记录门禁与采集器的指定设备、未授权/离线拒绝、版本缺失、输入校验、错误脱敏和超时；真实成功采集路径仍待目标设备验收。
