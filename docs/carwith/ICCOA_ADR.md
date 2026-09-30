# ADR：ICCOA Carlink 接入的研究准入条件

> 后续产品目标已明确为DiPlay内嵌软件接收器、摆脱外置盒。最新官方证据、兼容矩阵和分层PoC见 [纯软件接收器研究](ICCOA_SOFTWARE_RECEIVER.md)。商业接收APP只可选作授权对照，不是最终产品依赖。

日期：2026-09-30。状态：**研究结论 Blocked，待授权资料；不是产品 No-Go。** 对应 [R1 #15](https://github.com/xurunxin/DiPlay/issues/15)。用户要求优先跑通ICCOA原生CarWith，已明确执行优先级；未授权申请账号/接受条款。用户最新指令已将原生CarWith/ICCOA Carlink提升为执行主线，CarLife仅回退，当前步骤见 [原生首帧计划与实际阻塞](NATIVE_CARWITH_FIRST_FRAME.md)。本ADR不关闭#15，不宣称已取得原生首帧。

## 可追溯资料与决定

[联盟官方首页](https://www.iccoa.cn/)明确描述手机车机互联及小米发起方身份，提供 Carlink接入、产品文档和认证测试入口。具体取证、亿连对照与手机版本见 [真机/模拟器报告](EMULATOR_FINDINGS.md)。当前手机 CarWith 页面有 ICCOA 标识；版本 `3.2.0-20241009` / Android12 / MIUI14 的精确能力矩阵尚未得到官方确认，不能以历史“某系统版本以上”的门槛代替。

官方新站技术页请求的已发布文章列表返回401，未取得标准、SDK、样例、认证身份和许可。没有登录或申请。此结果只说明当前访问未认证；不推断联盟会员资格是一切测试前提。官方旧链接没有提供有效接入指南。

公开 GitHub 候选的只读核查作为补充，不作为 SDK 授权证明：

- [linuxhunter/iccoa2](https://github.com/linuxhunter/iccoa2) README只有“ICCOA 2.0 implementation on kotlin”；文件涉及 GetDkCertificate、Rke、BLE/APDU 等数字钥匙方向，未确认投屏接收端；仓库API许可证为空，树中未见LICENSE，不能作为已获集成许可的SDK。
- [linuxhunter/carkey_server](https://github.com/linuxhunter/carkey_server) README明确是 ICCOA/ICCE CarKey Server Demo，不能替代Carlink投屏。
- [hyksosss/ICCOA-tool](https://github.com/hyksosss/ICCOA-tool) README是Android10窗口观察/启动工具，依赖既有 `com.ucarhu.demo`、要求授予DUMP；不是独立接收端。本次没有安装或授予权限。
- `CarbitLink SDK` 仓库检索无结果只代表该次检索；不证明不存在商业/私有/其他名称的方案。

决定：ICCOA原生接入为当前主线；原生发现、认证、首帧先按真实SDK或规范实现。优先取得可嵌入DiPlay的合法SDK或完整规范/测试身份，在应用内做最小真实会话，再决定后端实现。亿连官方已有较直接的CarWith兼容说明，可能成为更短验证路径，但精确版本、包、ABI与许可仍缺；本次没有选定后端或更改Project。

## 能力等级必须分别证明

| 能力 | 必须取得/验证的合同 | 当前限制 |
| --- | --- | --- |
| 经典投屏/完整镜像 | 会话/认证、视频codec/config/帧边界/时间戳、输出Surface、坐标与反控、音频/麦克风通道、重连 | 全部待SDK/真实会话，不能从手机ICCOA标识或数字钥匙仓库推定 |
| 融合桌面 | 模板/应用清单、导航/媒体/电话元数据、焦点、生命周期、宿主UI与内容限制 | 与单一视频镜像合同不同，缺文档；不能承诺全部应用后台镜像 |
| 小窗/分屏 | 多窗口/多流、可变分辨率、焦点/触控变换、暂停/恢复及系统窗口权限 | 产品展示不说明第三方独立APK能获得相同权限；未请求overlay/系统签名权限 |
| 系统级后台/无感连接 | 蓝牙/Wi-Fi发现、网络路由/绑定、OEM服务/签名、前后台限制、音频焦点、权限和断连规则 | AVD声明Wi-Fi/蓝牙不等于真实链路可用；不改手机安全或全机网络来补假设 |

## 与当前代码的接口契合度

当前仓库没有 `ProjectionBackend` 接口；这是 #7 待G1后实施的架构项。不能把不存在的接口写成已经完成。现有锚点为：

- [AirPlaySessionListener / AirPlayMediaHandler](../../shared/src/main/java/com/shilapi/xcertplay/airplay/AirPlaySession.kt)含生命周期、首帧、错误和媒体设置回调，但参数携带AirPlaySession、RTSP/stream类型，不能直接实现ICCOA会话。
- [MediaSink / CarPlayMediaEngine](../../shared/src/main/java/com/shilapi/xcertplay/airplay/CarPlayMediaEngine.kt)提供视频codec/config/NALU、音频RTP与麦克风事件。可研究复用解码/渲染末端；前提是SDK输出格式已知。SDK若直接渲染Surface，不应假设能取得NALU；音频若为PCM或SDK自管，也不能沿用CarPlay RTP格式。
- [CarPlayController.sendTouch](../../shared/src/main/java/com/shilapi/xcertplay/orchestration/CarPlayController.kt)与 [CarPlayHostActivity.onHostTouch](../../common/src/main/java/com/shilapi/xcertplay/CarPlayHostActivity.kt)是CarPlay联系点。需独立验证ICCOA坐标、指针ID、按下/抬起/取消、旋转缩放、焦点与返回键语义，不直接套用AirPlayContact。
- [DiPlaySessionService](../../common/src/main/java/com/shilapi/xcertplay/DiPlaySessionService.kt)绑定CarPlayBackgroundSession和前台服务类型。未来复用需经实际会话与SDK生命周期验证，区分Android前台服务许可、SDK宿主签名要求和车厂系统级后台能力；不新增未经证实的权限。

若将来开独立实现Issues，候选接口边界应覆盖连接状态/失败原因、能力声明、媒体所有权（Surface还是帧）、输入、音频焦点、窗口/生命周期与脱敏诊断。方法签名应由真实SDK/协议证据决定，当前不先做大规模产品抽取，也不跨用Apple认证资产。

## 最小实验与 Go 条件

1. 由已有官方或授权供应商渠道取得目标版CarWith能力矩阵、车机SDK/样例/接收端、协议版本、Android/API/ABI和硬解要求、身份/签名方式、测试/集成/再分发许可及费用条件。需申请/同意条款/付款或对外联系时先获授权。
2. 核查包来源、许可证、ABI与所需权限；明确是否支持x86_64/WHPX/AVD及其Wi-Fi/蓝牙限制。安装与网络变更获准后才运行。若只支持ARM或实车链路，使用兼容且已授权的真实车机，不将模拟器失败判为产品失败。
3. 保持真实手机原生CarWith为发送端，按官方流程记录设备发现、身份认证、能力协商、首帧，至少两次干净会话。不得以独立手机客户端替代CarWith，未经认证/首帧不报告成功。
4. 按实际能力分别验证持续视频、触控、音频、焦点/窗口与断连；记录版本/链路/帧参数和脱敏证据。通过小窗不自动证明完整镜像或系统后台。
5. 资料和许可明确且最小连接可复现后，提交具体后端选择与准入结论，再经用户讨论开独立实现Issues。此准入不自动修改原CarLife路线的G0/G1规则。

当前所有产品能力与端到端性能均未测，缺项明确且没有认证恢复途径可以在本次权限内继续。仍可独立执行的UI/CPU测试已完成并保存；下一步是取得上述资料/合法接收端，不再重复请求手机入口操作。
