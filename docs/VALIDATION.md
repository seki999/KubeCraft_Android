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
| `app/build/outputs/apk/debug/app-debug.apk` | 27571440 | `d56972699445e4bfb5440e7922f4de9ada8e51663fc6b79e56604d8c4f3e60a5` |
| `app/build/outputs/apk/release/app-release-unsigned.apk` | 20627439 | `a52eca125fe21401d4169de2cfaa45da7f85f4c6e04012956cd79bb9a3729203` |

Debug 包使用开发签名，可安装体验；Release 未签名，正式分发前必须使用自己的发布密钥签名。两个产物均来自最后一次成功构建。

## 验证限制

50 关回放证明当前内容通过实际引擎与命令状态转换可通关，不代表完整 Kubernetes 一致性或设备体验验收。关键简化规则和逐关限制在 SIMULATION_RULES.md / IMPLEMENTATION_STATUS.md 公开记录。
