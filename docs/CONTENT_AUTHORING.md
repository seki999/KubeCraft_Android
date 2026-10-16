# 内容编写

主要入口是 `app/src/main/assets/levels/level-NNN.json`。50 个文件按固定 id 加载。`knowledge.json` 与 `chapters.json` 提供知识和章节索引，NPC 对话保存在每关 dialogues 中。

## 校验
```
python -m pip install -r scripts/requirements.txt
python scripts/validate_levels.py
.\gradlew.bat :engine:test
```

`content/level.schema.json` 使用 JSON Schema 2020-12，禁止未知字段；开发脚本同时检查数量、文件名、前置依赖、知识索引与评分总和。运行时 kotlinx.serialization 默认拒绝未知键，Level.validate 检查语义。Schema initialState 与当前 Kotlin Cluster 模型同步；扩展初始资源时需同步扩展 schema、Kotlin 模型和测试。

## 目标与交互
支持 nodes、readyPods、replicas、endpoints、image、event，以及 namespace、route、serviceType、configMounted、secretMounted、mounted、boundClaim、persisted、stateful、requests、priority、selected、hpa、load、probe、release、plan、region、oke、pool。目标由当前集群数据判断，event 只允许作为操作经历的辅助断言，不能替代最终状态断言。image 当前教学约定为 nginx:目标整数。planned 表示未实现目标，不能将 implemented 设为 true。当前 50 关均实现状态目标；planned 保留给未来内容草案，不能标成 implemented。

新增能力顺序：领域模型/操作 → reducer 因果关系 → 单元测试 → 命令或 UI 输入 → Objective 状态断言 → JSON → 通关回放与校验。不能只改 implemented 标记。关卡通过 allowedActions 限定写操作，查询不会改变集群。每个可玩关卡必须有一个初始不满足、经操作可满足的状态目标。knowledgeQuestion/knowledgeAnswer 提供短答复盘，不可替代实际状态任务。高级课程开发回放见 engine/src/test/resources/solutions.json；所有回放均由真实命令解析进入引擎。
