# KubeCraft — Kubernetes 云端帝国

Kotlin / Jetpack Compose Android 离线 Kubernetes 教育游戏。包名 `com.seki999.kubecraft`，Android 10（API 29）以上。无需登录、集群、Docker、云账号或联网权限。

已实现 10 章 50 关独立 JSON 场景、自由城市、模拟 kubectl、受控 YAML、拓扑、技术图鉴、Room 存档、DataStore 设置、评分与经验奖励。所有关卡通过实际集群状态判断完成；高级功能是公开边界的教学子集。**尚未完成真机/平板体验、升级安装、性能和正式签名验收**，逐关状态见 [IMPLEMENTATION_STATUS](docs/IMPLEMENTATION_STATUS.md)。

## 构建

需要 JDK 17、Android SDK Platform 35、SDK Build Tools、网络可用的首次依赖下载。设置 `ANDROID_HOME`（或在不提交的 `local.properties` 中配置 sdk.dir）。仓库包含 Gradle Wrapper，无需全局 Gradle。

```powershell
.\gradlew.bat testDebugUnitTest
.\gradlew.bat :engine:test
.\gradlew.bat assembleDebug
.\gradlew.bat assembleRelease
.\gradlew.bat assembleDebugAndroidTest
# 已连接授权手机或已启动模拟器时：
.\gradlew.bat connectedDebugAndroidTest
```

`testDebugUnitTest` 运行 Android 模块的存档 JVM 测试；独立纯 Kotlin 引擎使用 `:engine:test`，必须同时运行，不能用 NO-SOURCE 代替引擎验证。

工具链固定为 AGP 8.7.3、Gradle 8.9、Kotlin 2.0.20、Compose BOM 2024.09.00、Room 2.6.1。AGP/Gradle 对照来自 [Android 官方兼容表](https://developer.android.com/build/releases/about-agp)。选择本机已安装且相互兼容的稳定工具，不使用动态版本。

## 内容验证

```powershell
python -m pip install -r scripts/requirements.txt
python scripts/validate_levels.py
```

校验 50 个 JSON 的 Schema、ID、依赖、知识索引与评分权重。JVM 测试额外验证严格反序列化、调度约束、控制器恢复、更新/回滚、端点、HPA、探针、存储和 50 关通关回放。

## APK 与安装

- 可安装调试包：`app/build/outputs/apk/debug/app-debug.apk`
- Release 未签名包：`app/build/outputs/apk/release/app-release-unsigned.apk`
- 设备测试包：`app/build/outputs/apk/androidTest/debug/app-debug-androidTest.apk`

```powershell
adb install -r app/build/outputs/apk/debug/app-debug.apk
```

Release 使用自己的发布 keystore 通过 Android Studio “Generate Signed Bundle / APK” 签名后才能分发安装。私钥不应提交到仓库。后续升级必须使用同一签名及更高 versionCode，不能卸载旧 App 后声称验证了升级保档。

## 开始游戏

1. 首页选择“开始剧情课程”，第 1 关可直接进入，后续按前置关卡解锁。
2. 手机在城市底部打开“任务”阅读 NPC 与目标；“终端”输入 `help` 查看完整受控命令。
3. 第 1 关输入 `kubectl create node worker-1`，回到任务面板查看状态目标。
4. 点击“推进 1 tick”观察 Pod 启动、控制器补建与滚动更新。查询命令不会推进时间。
5. 目标满足后可结算；英文术语复盘、资源/操作效率和提示使用影响最终分数。每次会话只领奖一次。
6. 首页“继续保存的城市”恢复快照。选择一个关卡会开始该关新会话；历史分数保留。

自由城市中的“建设 Worker”和故障按钮是具体集群操作，其余主要通过终端/YAML。`sim` 是本游戏教学扩展，不是真实 kubectl。第 41–42 关在 YAML 编辑器操作；高级关卡提示给出离线语法。输入任何命令都不会启动 Android shell。

## 支持边界

- 调度：Ready、CPU/内存 requests、nodeSelector、等待队列 Priority；无生产级抢占/驱逐。
- 控制器：Deployment/ReplicaSet、副本自愈、固定 maxSurge=1 更新与镜像回滚。
- 网络：selector/Ready/Namespace 端点、逻辑 DNS 与 Ingress；无真实网络流量。
- 存储与配置：ConfigMap/Secret 引用、静态 PV/PVC、独立数据、简化 StatefulSet。
- 运维：模拟 CPU/HPA、内存 OOM、readiness/liveness；无真实 metrics-server 或容器进程。
- YAML：只接受编辑器公开示例的单容器 Deployment 结构与字段顺序，拒绝未知字段。
- Helm/Terraform/OCI/OKE：内置 Chart、Worker 计划和区域/控制平面/节点池教学状态。无 HCL、provider、OCI API、实际资源或费用。

## 文档与测试结果

- [游戏设计](docs/GAME_DESIGN.md)
- [架构与存档兼容](docs/ARCHITECTURE.md)
- [模拟规则与边界](docs/SIMULATION_RULES.md)
- [内容编写](docs/CONTENT_AUTHORING.md)
- [逐关状态](docs/IMPLEMENTATION_STATUS.md)
- [实际验证记录](docs/VALIDATION.md)

引擎测试报告：`engine/build/reports/tests/test/index.html`；App JVM 测试报告：`app/build/reports/tests/testDebugUnitTest/index.html`。设备测试已编写，不应把测试 APK 编译成功当作设备测试通过。

## 0.1.1 交互更新

任务、提示、复盘、图鉴、拓扑、状态与结算文本支持长按选择复制；答案、终端和 YAML 输入框使用系统复制/粘贴菜单。默认浅色主题、动画开启、慢速动画开启、字体 140%。只改变尚未保存的设置默认值，保留用户已有偏好。
