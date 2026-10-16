# 实际验证记录

日期：2026-10-17（Asia/Tokyo）。环境为 Windows / JDK 17 / Android SDK 35。仓库开始时为空。

| 检查 | 结果 |
|---|---|
| `:engine:test` | 15 项通过，0 失败；包括前十关与后四十关的状态通关回放 |
| `testDebugUnitTest` | 2 项通过；完整存档往返、旧存档默认字段兼容 |
| `python scripts/validate_levels.py` | 50 关 JSON Schema、编号、知识索引、依赖、评分权重通过 |
| `assembleDebug` | 成功 |
| `assembleRelease` | 成功，生成未签名 Release 包 |
| `assembleDebugAndroidTest` | 成功，仅证明设备测试代码可编译 |
| `lintDebug` | 成功：0 errors，11 warnings（10 条依赖版本提示、1 条 KAPT→KSP 建议） |
| `connectedDebugAndroidTest` | 已尝试，失败：`DeviceException: No connected devices!` |
| Debug `apksigner verify` | 通过 |

## 未运行的设备验收

`adb devices` 未列出设备；SDK 内无 emulator.exe 与系统镜像。没有声称应用已在设备成功启动，也没有声称设备 UI/SQLite 重开/升级保档测试通过。设备测试源码包含首页/章节导航、终端通关第 1 关、设置访问、SQLite 关闭重开。连接设备后按 README 运行。

## 当前 APK

| 文件 | 字节 | SHA-256 |
|---|---|---|
| `app/build/outputs/apk/debug/app-debug.apk` | 27278132 | `1fefae75d6cf1a73aab35f2515a77fbc6280ef2080bfdd28e3a438d36fcbcad8` |
| `app/build/outputs/apk/release/app-release-unsigned.apk` | 20627439 | `31f9bad2915068df651d930c9bbde37069bc067fb5f3e8f5f5a0012d09fb42d7` |

Debug 包使用开发签名，可安装体验；Release 未签名，正式分发前必须使用自己的发布密钥签名。两个产物均来自最后一次成功构建。

## 验证限制

50 关回放证明当前内容通过实际引擎与命令状态转换可通关，不代表完整 Kubernetes 一致性或设备体验验收。关键简化规则和逐关限制在 SIMULATION_RULES.md / IMPLEMENTATION_STATUS.md 公开记录。

## 0.1.1 更新验证

默认浅色、动画开启、慢速开启、字体 140%；DataStore 已存偏好保留。任务、提示、复盘、拓扑、状态、图鉴和结算使用 Compose SelectionContainer 支持长按选择复制，输入框保留原生粘贴功能。

`testDebugUnitTest`（2 项）、`assembleDebug`、`assembleRelease`、`assembleDebugAndroidTest` 全部成功；Debug 签名验证通过，APK 包版本为 0.1.1 / versionCode 2。引擎没有变更，未重复运行引擎测试。当前 adb 无连接设备，选择手柄、复制菜单与粘贴的设备交互尚未验收；前文 lintDebug 与 connectedDebugAndroidTest 的记录属于 0.1.0。表中 APK 哈希已更新为 0.1.1。
