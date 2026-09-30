# 原生 CarWith 首帧：当前阻塞与最短执行路径

更新：2026-09-30。用户最新优先级为 **DiPlay 安卓车机应用直接接收原生 CarWith / ICCOA Carlink，摆脱外置盒**。CarLife 仅是兼容回退，暂停扩展其主线。此前“CarLife PoC 优先”的决定已被本次指令覆盖；原 [Project #1](https://github.com/users/xurunxin/projects/1) 字段和 Issues 未被关闭或改写。CarLife 画面不能作为原生链路验收。

本轮新增：两个用户附件已完成静态分析，其中 `com.ucarhu.demo v1.2.2` 实际内嵌车端 SDK 和 ARM 媒体接收库；另一包是不同的 OPPO 生态应用。详见[APK 静态证据与最小 ARM PoC](ICCOA_APK_STATIC_ANALYSIS.md)。这更新了材料缺口判断，不代表原生 G0 通过。

## 实际核查

- 指定 checkout `X:\Projects\Code\DiPlay`，分支 `work/carwith-m0-evidence`；核查开始时干净，HEAD `0f8cebdf6d58ddbd6340f307d2c6309680609e92`。没有新克隆或覆盖用户改动。未发现 checkout/祖先的 AGENTS.md 或 `.agents/skills`。
- ADB 实际只见已授权 Redmi K30 Pro（Android 12），当前安装的 CarWith `3.2.0-20241009` / code `103002000`。package dump 另列系统旧版 `1.0.18`，不代表另一台手机。新 HyperOS 3 手机未确认接入，型号/App 版本仍未知。
- [官方历史手机列表](https://www.iccoa.cn/carlinkphonelist.html)明确包含 Redmi K30 Pro。新版案例页的 Android13/15 条件不能排除其历史经典投屏能力；当前 Android12/MIUI14/CarWith3.2 组合仍待真实验证，既非确认兼容，也非 No-Go。
- 初次核查未找到车端 SDK。随后已完整读取用户上传的两个 APK：`com.ucarhu.demo v1.2.2` 可见 `UCarAdapter.init/startAdvertise/startCast`、本地 BLE/P2P/AP 服务与 ARM `libovmsink.so`；另一包 `com.heytap.opluscarlink 14.1.8` 没有该接收 SDK。已有真实实现线索，仍无独立授权 SDK、配置/状态合同或合法测试身份说明。APK留在私有分析目录，未安装、运行或复制进项目。
- 官方公开前端使用 `category=standard/prd/guides&status=published`。本轮匿名读取 `/prod-api/system/article/list` 三类均 HTTP200、JSON **code401**，未取得文档正文。旧技术标准页是范围简介，非线协议。没有注册、联系厂商、登录或绕过认证；访问受限不证明技术必须外置盒，也不证明所有测试必须会员。
- 宿主可见 Intel AX200 Wi-Fi（Disconnected）及有线网；未修改网络。未验证真实手机与 AVD 的射频/蓝牙/P2P 映射。本轮没有启动 AVD 或继续通用性能测试。

## 当前停在哪一层

| 阶段 | 实际结果 | 进入下一步的最小条件 |
| --- | --- | --- |
| SDK加载/初始化 | 未执行：APK中已见车端 API 和 ARM 库，尚非已授权可集成 SDK | ARM受控台架、合法测试/集成依据、配置与状态说明；未知APK运行另需批准 |
| 原生发现 | **未进入**，已见 `startAdvertise`、BLE/P2P/AP结构；没有真实扫描结果 | 完成真实初始化与权限验证，并获得SDK合同或完整服务/广播字段、角色、版本及时序规范 |
| 系统配对/无线链路 | 未执行 | 明确由SDK或宿主管理、蓝牙/P2P/AP角色；在所需操作获准后走官方流程 |
| 身份认证/握手 | 未执行，没有认证失败码 | SDK实际认证合同；若要求测试身份、签名、证书或激活，提供合法provisioning材料。当前不能假定它必需专用硬件或可以省略 |
| 媒体协商 | 未执行 | 真实SDK Surface/压缩帧回调、codec/config/PTS合同 |
| 首帧 | **未取得任何原生 CarWith 帧** | 经上述真实链路收到的视频交给SDK Surface或DiPlay媒体末端；记录实际回调/渲染证据 |

已有 debug AVC fixture 只证明合成视频解码与取消恢复，不属于本表的发现、认证或首帧进展。本轮不新增猜测的UUID、端口、广播身份、认证协议、占位后端或更多基准来替代这些缺项。

## 可合法参考什么

[官方 SDK 政策](https://www.iccoa.cn/suit_1.html)与[安全说明](https://www.iccoa.cn/suit_2.html)明确车端 SDK 嵌入应用并在应用进程运行。这支持 DiPlay 内嵌软件接收器方向，但不确定普通后装 APK 的签名/权限、ABI、身份和许可。

公开源码候选仍须分清：数字钥匙仓库不是投屏；依赖 CPC200-CCPA 的 Carlink 工程是 host-dongle；`ICCOA-tool` 只是启动已有接收APP的工具。既有 GPL 工程不能因为一个未知许可的 APK 可下载就获得 SDK 集成权。没有把这些候选安装、提取身份或导入 DiPlay。详见 [分层接入审查](ICCOA_SOFTWARE_RECEIVER.md)。本轮公开仓库查询 `iccoa is:public` 得到8项；新增候选 [Hyggec/iccoavoice](https://github.com/Hyggec/iccoavoice) 的README说明它是语音测试网页，不是可验证的发现/认证接收实现，且未识别许可证。检索未找到可信可集成实现，不等于不存在可授权方案。

## 取得材料后立即执行的首帧计划

1. **先要一套可用车端测试 SDK**（AAR/JAR/so及依赖）和**最小初始化/发现示例、对应测试许可**。不必一开始就取得所有协议正文；SDK可以承担未知的线协议。完整独立实现则另需发现、握手与媒体规范。未明确重分发权时 SDK/身份只留私有工作区，不进入公开 PR。
2. 核实 SDK minSdk、进程 ABI、后装APK/系统签名要求与必要权限，确认它支持 K30 Pro 的历史协议版本。x86_64 可用则在现有 AVD 做加载实验；只有 ARM 时，使用支持该 ABI 的授权真实安卓车机，不把加载失败当协议不兼容。ARM64 出现在 AVD 的 advertised ABI 列表也不证明真实 ARM 验收。
3. 在 DiPlay 隔离 debug 接入页使用真实 SDK API 初始化、开始发现、记录 SDK 原始阶段码的最小白名单；SDK需身份或额外权限时在该阶段停下，明确所需材料/批准。先用已接入的 K30 Pro；不要求用户为尚不存在的接收端重复找手机入口。
4. 手机实际发现 DiPlay 后，按SDK规定配对与认证；记录“发现”“链路建立”“认证成功”各自证据，不能把TCP连接当认证。没有认证成功不进入媒体成功状态。
5. 按真实合同绑定单Surface或压缩帧适配器，拿到**第一帧**及首帧时间；两次重复原生连接后再延伸持续视频/输入/音频/重连，最后回归CarPlay。CarLife模式和本地fixture均不得计入原生结果。

**当前最小条件已缩小为合法SDK/样本测试依据、ARM接收台架、初始化配置与状态说明。** 样本可见API不足以确认CCD/身份生成方式和普通APK权限；先按[静态分析中的ARM PoC](ICCOA_APK_STATIC_ANALYSIS.md)核实，再接入DiPlay。无需购买连接盒，不能复制样本身份。用户未授权申请账号、接受条款、联系厂商、安装未知接收包或改手机安全设置；本任务未做这些动作。
