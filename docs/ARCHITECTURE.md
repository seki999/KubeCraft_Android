# 架构

## 模块
- `engine`：纯 Kotlin/JVM，Serializable 领域模型、GameAction、SimulationEngine、受控终端、课程目标和评分。无 Android Context、系统命令或 IO。
- `app`：Compose Material 3，GameViewModel 暴露 StateFlow；GameRepository 隔离 assets、Room 和 DataStore；Canvas 仅投影领域状态。

数据流：用户输入 → 受控命令解析 → GameAction → 纯 reducer → Session → StateFlow → Compose；成功操作进入串行存档队列。数据库通过单个主存档记录原子保存进度和会话，避免奖励/快照分离提交。数据库异常显示错误，不删除旧存档。进程初始化读档失败时停留错误状态，不用空数据覆盖存档。

## 持久化
Room v1 saves 表内有版本化 JSON，包含 best、completions、xp、learned、achievements 和完整 Session/Cluster。内容资源更新与数据库独立；不存在 destructive migration。新增非必填字段需提供 Kotlin 默认值。变更数据库表时必须增加显式 Migration 并测试；变更存档语义时必须增加 save schema migration。JVM 测试覆盖完整 v1 往返和缺省新字段的旧存档兼容；设备数据库重开测试已编写，但无设备尚未运行。尚无跨版本升级 APK 的设备验收。

DataStore 保存界面偏好。每次有效状态变更即排队持久化；已提交数据库事务在重启后恢复。极端强杀时未落盘队列仍可能丢失末尾操作，不能宣称严格零丢失。

## 构建
Gradle 8.9 / AGP 8.7.3 / Kotlin 2.0.20 / Compose BOM 2024.09.00 / JDK 17 / compileSdk 35 / minSdk 29。固定版本可重复解析，首次构建需要下载工具依赖；App 正常使用离线。
