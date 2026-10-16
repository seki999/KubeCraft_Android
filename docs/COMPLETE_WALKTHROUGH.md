# KubeCraft Android · 50 关完整通关秘籍

> 适用仓库：[seki999/KubeCraft_Android](https://github.com/seki999/KubeCraft_Android) · 基于当前 `app/src/main/assets/levels/level-001.json` 至 `level-050.json` 的任务、提示与答案整理（2026-10-17）。
>
> **重要说明**：这是离线 Kubernetes **教学模拟器**，命令属于应用内受控语法；`sim`、`kubectl create node`、简化的 Helm/Terraform/OKE 操作不能直接照搬到真实生产集群。关卡虽声明可玩，仓库仍未完成真机/平板和发布验收；本秘籍给出关卡数据定义的标准操作路径，不代表已经逐关完成真机点击验收。

## 目录

- [统一过关流程](#统一过关流程)
- [50 关逐关攻略](#50-关逐关攻略)
- [通关与评分机制](#通关与评分机制)
- [卡关排查](#卡关排查)
- [命令与文档来源](#命令与文档来源)

## 统一过关流程

1. 首页点 **「开始剧情课程」**，选择已经解锁的关卡；手机进入城市后打开底部 **「任务」**；平板可在右侧面板切换。
2. 查看任务前面的 `○`，明确目标（Ready Pod 数量、镜像、Service endpoint、特定事件等）；注意：每关从独立初始集群快照启动，**不要机械重复前一关的操作**。
3. 打开 **「终端」**，输入下文的一行命令并点击 **「执行」**；多行攻略须 **逐行执行**。终端先输入 `help` 可以查看游戏真正支持的命令。
4. 需要等待调度、启动、自愈、滚动更新、HPA、探针恢复、卷绑定时，点击 **「推进 1 tick」**，必要时多点几次。查询 `get/logs/describe/top` 不会自动推进 tick。事件和 Pod 详情可从 **「状态」** 或终端观察。
5. 回 **「任务」** 面板检查全部目标出现 `✓`；答知识复盘题（**英文**，答案逐关列出），点击 **「验证答案」**；然后点击 **「验证任务并结算」**。
6. 结算后在对话框或任务面板进入下一关。**只回答知识问题不会通关**；只达成目标但未点击结算，也不会领取通关奖励。

**约定**：下面的每段代码都输入在游戏内部终端，不是 Android shell / PowerShell。`<实际Pod名>` 属于说明占位符，务必用查询结果替换；`tick` 是按钮不是终端命令；如遇到「此关不允许此操作」，请核对是否选对关卡及命令。

## 50 关逐关攻略

### 第 1 章：Container 与 Pod（1–5 关）

#### 第 01 关 · 点亮第一座工人站

- **操作**：在「终端」依次执行：

```text
kubectl create node worker-1
```
- **成功判定**：至少 1 个 Ready Worker。
- **知识复盘答案**：`Ready`。
- **注意**：执行后视状态推进 tick，并到「任务」检查目标。

#### 第 02 关 · 第一个容器信使

- **操作**：在「终端」依次执行：

```text
kubectl run hello --image=nginx:1
```
- **成功判定**：hello Pod Ready。
- **知识复盘答案**：`Pod`。
- **注意**：创建 Pod 后需推进 tick，直到 Starting → Running / Ready。

#### 第 03 关 · 拥挤的站台

- **操作**：在「终端」依次执行：

```text
kubectl create node worker-1
```
- **成功判定**：Pending 的 hello 在 Ready 节点上运行。
- **知识复盘答案**：`FailedScheduling`。
- **注意**：执行后视状态推进 tick，并到「任务」检查目标。

#### 第 04 关 · 调度员的选择

- **操作**：在「终端」依次执行：

```text
kubectl create node worker-2
```
- **成功判定**：cargo 在健康 Worker 上 Ready。
- **知识复盘答案**：`Scheduler`。
- **注意**：执行后视状态推进 tick，并到「任务」检查目标。

#### 第 05 关 · 不会消失的订单

- **操作**：在「终端」依次执行：

```text
kubectl create deployment web --image=nginx:1
```
- **成功判定**：web Deployment 至少 1 个 Ready Pod。
- **知识复盘答案**：`Deployment`。
- **注意**：执行后视状态推进 tick，并到「任务」检查目标。

### 第 2 章：Deployment、副本与自愈（6–10 关）

#### 第 06 关 · 副本工坊

- **操作**：在「终端」依次执行：

```text
kubectl scale deployment/web --replicas=2
```
- **成功判定**：web 期望且 Ready 副本均为 2。
- **知识复盘答案**：`replicas`。
- **注意**：执行后视状态推进 tick，并到「任务」检查目标。

#### 第 07 关 · 自愈警报

- **操作**：在「终端」依次执行：

```text
kubectl get pods
kubectl delete pod <上一步查询到的实际 web Pod 名称>
```
- **成功判定**：记录 Deleted 事件、控制器恢复 2 个 Ready 副本。
- **知识复盘答案**：`ReplicaSet`。
- **注意**：先查看存在的 Pod 的真实名称，不可把“实际Pod名称”字样直接当参数；删除后继续推进 tick 等控制器补建。

#### 第 08 关 · 节能夜班

- **操作**：在「终端」依次执行：

```text
kubectl scale deployment/web --replicas=1
```
- **成功判定**：web 期望且 Ready 副本均为 1。
- **知识复盘答案**：`replicas`。
- **注意**：执行后视状态推进 tick，并到「任务」检查目标。

#### 第 09 关 · 不停机换代

- **操作**：在「终端」依次执行：

```text
kubectl set image deployment/web web=nginx:2
```
- **成功判定**：所有 web Pod Ready 且镜像 nginx:2。
- **知识复盘答案**：`revision`。
- **注意**：滚动更新最多额外创建一个 Pod；空间不足则更新会等待，继续检查节点容量。

#### 第 10 关 · 回到可靠版本

- **操作**：在「终端」依次执行：

```text
kubectl rollout undo deployment/web
```
- **成功判定**：发生 Rollback 事件且 web 回到 nginx:1。
- **知识复盘答案**：`rollout undo`。
- **注意**：必须发生真实 Rollback 事件且最终镜像为 nginx:1；需等待旧/新 Pod 交接。

### 第 3 章：网络、Service 与 Ingress（11–15 关）

#### 第 11 关 · 跨节点的信使

- **操作**：在「终端」依次执行：

```text
sim ping hello
```
- **成功判定**：记录成功的 pod/hello 路由。
- **知识复盘答案**：`Ready`。
- **注意**：执行后视状态推进 tick，并到「任务」检查目标。

#### 第 12 关 · 稳定的访问入口

- **操作**：在「终端」依次执行：

```text
kubectl expose deployment web
```
- **成功判定**：web Service 有 2 个 Ready endpoints。
- **知识复盘答案**：`selector`。
- **注意**：Service endpoint 只包含同 Namespace、标签匹配且 Ready 的 Pod。

#### 第 13 关 · 街区大门 NodePort

- **操作**：在「终端」依次执行：

```text
kubectl expose deployment web --type=NodePort
```
- **成功判定**：web Service 类型 NodePort 且有 Ready endpoint。
- **知识复盘答案**：`NodePort`。
- **注意**：执行后视状态推进 tick，并到「任务」检查目标。

#### 第 14 关 · 负载均衡灯塔

- **操作**：在「终端」依次执行：

```text
kubectl expose deployment web --type=LoadBalancer
```
- **成功判定**：LoadBalancer Service 有 Ready endpoint。
- **知识复盘答案**：`LoadBalancer`。
- **注意**：执行后视状态推进 tick，并到「任务」检查目标。

#### 第 15 关 · 域名路由 Ingress

- **操作**：在「终端」依次执行：

```text
sim ingress gateway --host=web.local --service=web
sim request web.local
```
- **成功判定**：web.local 请求成功并记录路由。
- **知识复盘答案**：`Ingress`。
- **注意**：执行 request 前确保 Ingress 指向的 web Service 已有 Ready endpoint。

### 第 4 章：Namespace、标签与 DNS（16–20 关）

#### 第 16 关 · 新的命名空间

- **操作**：在「终端」依次执行：

```text
kubectl create namespace team
kubectl create deployment web --image=nginx:1 --namespace=team
```
- **成功判定**：team Namespace 存在、team 的 web Ready。
- **知识复盘答案**：`Namespace`。
- **注意**：执行后视状态推进 tick，并到「任务」检查目标。

#### 第 17 关 · 给信使贴标签

- **操作**：在「终端」依次执行：

```text
kubectl label pod hello app=web
```
- **成功判定**：web Service 出现至少 1 个 Ready endpoint。
- **知识复盘答案**：`Labels`。
- **注意**：执行后视状态推进 tick，并到「任务」检查目标。

#### 第 18 关 · 选择器的误会

- **操作**：在「终端」依次执行：

```text
sim service web --app=web
```
- **成功判定**：web Service 有 2 个 Ready endpoints。
- **知识复盘答案**：`Selector`。
- **注意**：执行后视状态推进 tick，并到「任务」检查目标。

#### 第 19 关 · 城市的电话簿

- **操作**：在「终端」依次执行：

```text
sim connect web --namespace=team
```
- **成功判定**：记录 web.team.svc.cluster.local 成功路由。
- **知识复盘答案**：`DNS`。
- **注意**：逻辑 DNS 取决于相应 Namespace 的 Service 和 Ready 后端。

#### 第 20 关 · 分区之间的合作

- **操作**：在「终端」依次执行：

```text
sim service web --app=web --namespace=payments
sim connect web --namespace=payments
```
- **成功判定**：记录 web.payments.svc.cluster.local 成功路由。
- **知识复盘答案**：`Namespace`。
- **注意**：先创建/修正 payments Service，再 connect；若无可用端点，先检查 web Pod 状态。

### 第 5 章：ConfigMap、Secret 与存储（21–25 关）

#### 第 21 关 · 把配置移出镜像

- **操作**：在「终端」依次执行：

```text
kubectl create configmap settings --from-literal=mode=prod
sim mount hello configmap settings
```
- **成功判定**：hello 挂载 settings ConfigMap。
- **知识复盘答案**：`ConfigMap`。
- **注意**：执行后视状态推进 tick，并到「任务」检查目标。

#### 第 22 关 · 凭据保险箱

- **操作**：在「终端」依次执行：

```text
kubectl create secret generic credentials --from-literal=token=demo
sim mount hello secret credentials
```
- **成功判定**：hello 挂载 credentials Secret。
- **知识复盘答案**：`Secret`。
- **注意**：执行后视状态推进 tick，并到「任务」检查目标。

#### 第 23 关 · 跨越 Pod 生命周期

- **操作**：在「终端」依次执行：

```text
sim mount hello pvc data
sim write data hello
```
- **成功判定**：hello 挂载 data 且 data 中持久记录 hello。
- **知识复盘答案**：`Volume`。
- **注意**：持久写入要求 data PVC 已 Bound、hello Ready 并挂载该卷。

#### 第 24 关 · 等待供应的磁盘

- **操作**：在「终端」依次执行：

```text
sim pv disk --size=1024
```
- **成功判定**：data PVC Bound 且 hello 恢复 Ready。
- **知识复盘答案**：`PVC`。
- **注意**：PV 创建后推进 tick，让容量匹配与 Pod 就绪完成。

#### 第 25 关 · 有名字的数据库

- **操作**：在「终端」依次执行：

```text
sim statefulset db --replicas=1 --claim=data
```
- **成功判定**：db-0 Ready 并挂载持久卷。
- **知识复盘答案**：`StatefulSet`。
- **注意**：执行后视状态推进 tick，并到「任务」检查目标。

### 第 6 章：调度、requests 与优先级（26–30 关）

#### 第 26 关 · 过大的资源订单

- **操作**：在「终端」依次执行：

```text
sim resources hello --cpu=250 --memory=128 --limit=512
```
- **成功判定**：hello 按 250m CPU requests 成功调度并 Ready。
- **知识复盘答案**：`requests`。
- **注意**：执行后视状态推进 tick，并到「任务」检查目标。

#### 第 27 关 · 内存红线

- **操作**：在「终端」依次执行：

```text
kubectl logs hello
sim memory hello 64
```
- **成功判定**：hello 内存负载降低并恢复 Ready。
- **知识复盘答案**：`limits`。
- **注意**：执行后视状态推进 tick，并到「任务」检查目标。

#### 第 28 关 · 紧急订单优先

- **操作**：在「终端」依次执行：

```text
sim priority urgent 100
kubectl create node worker-1
```
- **成功判定**：urgent 按优先级运行，batch 等待容量。
- **知识复盘答案**：`priority`。
- **注意**：此关是等待队列优先级，不是 Kubernetes 生产级的抢占；注意 urgent/batch 的模拟状态。

#### 第 29 关 · 寻找正确的分区

- **操作**：在「终端」依次执行：

```text
sim selector hello zone=a
```
- **成功判定**：hello 调度在 zone=a 的节点并 Ready。
- **知识复盘答案**：`nodeSelector`。
- **注意**：执行后视状态推进 tick，并到「任务」检查目标。

#### 第 30 关 · 调度诊断联合演习

- **操作**：在「终端」依次执行：

```text
kubectl get events
kubectl create node worker-2
```
- **成功判定**：9 个 web 副本全部 Ready。
- **知识复盘答案**：`FailedScheduling`。
- **注意**：目标是九个全部 Ready；有 Pending 时检查节点可用 CPU/内存。

### 第 7 章：扩缩容、HPA 与容量（31–35 关）

#### 第 31 关 · 迎接流量高峰

- **操作**：在「终端」依次执行：

```text
kubectl scale deployment/web --replicas=5
```
- **成功判定**：5 个 web 副本 Ready。
- **知识复盘答案**：`replicas`。
- **注意**：执行后视状态推进 tick，并到「任务」检查目标。

#### 第 32 关 · 自动伸缩的节拍

- **操作**：在「终端」依次执行：

```text
kubectl autoscale deployment web --min=1 --max=5 --cpu-percent=60
sim load 90
```
- **成功判定**：HPA 管理 web，至少 3 副本 Ready。
- **知识复盘答案**：`HPA`。
- **注意**：HPA 在 tick 才采样，之后还要等待新 Pod 启动并 Ready。

#### 第 33 关 · 度量与缩容

- **操作**：在「终端」依次执行：

```text
sim load 15
```
- **成功判定**：模拟 CPU 15%，HPA 缩到 1 个 Ready 副本。
- **知识复盘答案**：`Metrics`。
- **注意**：执行后视状态推进 tick，并到「任务」检查目标。

#### 第 34 关 · 容量不是无限的

- **操作**：在「终端」依次执行：

```text
kubectl create node worker-2
```
- **成功判定**：10 个 web 副本 Ready。
- **知识复盘答案**：`Node`。
- **注意**：执行后视状态推进 tick，并到「任务」检查目标。

#### 第 35 关 · 有边界的弹性

- **操作**：在「终端」依次执行：

```text
kubectl autoscale deployment web --min=2 --max=6 --cpu-percent=50
sim load 100
```
- **成功判定**：模拟 CPU 100%，HPA 达到 6 个 Ready 副本。
- **知识复盘答案**：`maxReplicas`。
- **注意**：CPU 100% 的模拟状态和至少 6 个 Ready 副本要同时成立。

### 第 8 章：排错、日志与健康探针（36–40 关）

#### 第 36 关 · 命令行值班

- **操作**：在「终端」依次执行：

```text
kubectl create deployment web --image=nginx:1
kubectl expose deployment web
```
- **成功判定**：命令创建的 Service 有 Ready endpoint。
- **知识复盘答案**：`kubectl`。
- **注意**：执行后视状态推进 tick，并到「任务」检查目标。

#### 第 37 关 · 事件里的线索

- **操作**：在「终端」依次执行：

```text
kubectl get events
kubectl create node worker-1
```
- **成功判定**：hello 从 Pending 恢复 Ready。
- **知识复盘答案**：`Events`。
- **注意**：执行后视状态推进 tick，并到「任务」检查目标。

#### 第 38 关 · 日志追踪

- **操作**：在「终端」依次执行：

```text
kubectl logs hello
sim memory hello 100
```
- **成功判定**：hello 内存状态恢复 Ready。
- **知识复盘答案**：`Logs`。
- **注意**：执行后视状态推进 tick，并到「任务」检查目标。

#### 第 39 关 · 健康不等于就绪

- **操作**：在「终端」依次执行：

```text
sim probe hello readiness
sim health hello true
```
- **成功判定**：hello readiness 健康 Ready，web Service endpoint 恢复。
- **知识复盘答案**：`readiness`。
- **注意**：仅配置 readiness probe 不足以通关，还要健康恢复并让 Service endpoint 出现。

#### 第 40 关 · 机器人自愈巡航

- **操作**：在「终端」依次执行：

```text
sim probe hello liveness
```
- **成功判定**：liveness 探针触发恢复、hello Ready。
- **知识复盘答案**：`liveness`。
- **注意**：推进 tick 检查 liveness 的重启及恢复状态。

### 第 9 章：YAML、Helm 与 Terraform（41–45 关）

#### 第 41 关 · 第一份声明式蓝图

- **操作**：进入「YAML」 →  使用默认示例  → 「校验并创建 Deployment」
- **成功判定**：web 期望及 Ready 副本均为 2。
- **知识复盘答案**：`YAML`。
- **注意**：此关必须用 **YAML 面板**，默认固定结构；不是将这些文字输入终端。

#### 第 42 关 · 修订蓝图的副本数

- **操作**：进入「YAML」 →  将 replicas: 2 改为 replicas: 3  → 「校验并创建 Deployment」
- **成功判定**：web 期望及 Ready 副本均为 3。
- **知识复盘答案**：`replicas`。
- **注意**：编辑器仅支持固定顺序/缩进的 Deployment 示例；不要引入其他 YAML 字段。

#### 第 43 关 · 打包应用工坊

- **操作**：在「终端」依次执行：

```text
helm install portal web-chart --replicas=2
```
- **成功判定**：portal Release revision 1，2 个 Ready 副本。
- **知识复盘答案**：`Helm`。
- **注意**：Release 和 Deployment 两边状态都要满足，通常需要推进 tick。

#### 第 44 关 · 升级应用包

- **操作**：在「终端」依次执行：

```text
helm upgrade portal web-chart --replicas=3
```
- **成功判定**：portal Release revision 至少 2，3 个 Ready 副本。
- **知识复盘答案**：`revision`。
- **注意**：等待升级后的三个 Pod 全部 Ready。

#### 第 45 关 · 先计划，再供应

- **操作**：在「终端」依次执行：

```text
terraform plan --region=ap-tokyo-1 --workers=2
terraform apply
```
- **成功判定**：东京区域 2 个 Worker 计划应用，web Ready。
- **知识复盘答案**：`plan`。
- **注意**：plan 和 apply 是两条独立命令；等待 web Pod 调度。

### 第 10 章：OCI、OKE 与综合演习（46–50 关）

#### 第 46 关 · 选择云端区域

- **操作**：在「终端」依次执行：

```text
sim oci-region ap-tokyo-1
```
- **成功判定**：OCI 教学区域为 ap-tokyo-1。
- **知识复盘答案**：`Region`。
- **注意**：执行后视状态推进 tick，并到「任务」检查目标。

#### 第 47 关 · 托管控制平面

- **操作**：在「终端」依次执行：

```text
sim oke-create
```
- **成功判定**：OKE 教学控制平面存在。
- **知识复盘答案**：`OKE`。
- **注意**：如果控制平面尚不能创建，请确认关卡初始化中的 OCI 区域状态；该关标准提示是 sim oke-create。

#### 第 48 关 · 节点池的工人们

- **操作**：在「终端」依次执行：

```text
sim oke-pool 2
```
- **成功判定**：OKE 节点池 2 个 Ready Worker。
- **知识复盘答案**：`Node Pool`。
- **注意**：节点池依赖模拟 OKE 控制平面；若失败请查看初始状态和终端错误。

#### 第 49 关 · 门户上线联合演习

- **操作**：在「终端」依次执行：

```text
sim oke-pool 2
kubectl create deployment web --image=nginx:1
kubectl scale deployment/web --replicas=3
kubectl expose deployment web --type=LoadBalancer
sim ingress gateway --host=web.local --service=web
sim request web.local
```
- **成功判定**：节点池至少 2 Worker、3 个 web Ready、LoadBalancer 有 endpoint、web.local 访问成功。
- **知识复盘答案**：`Service`。
- **注意**：按顺序执行；在 sim request 之前让 Service 出现 Ready endpoint。

#### 第 50 关 · 云端帝国毕业演习

- **操作**：在「终端」依次执行：

```text
kubectl create configmap settings --from-literal=mode=prod
sim mount vault configmap settings
sim pv disk --size=1024
sim pvc data --size=512
sim mount vault pvc data
sim write data graduate
kubectl autoscale deployment web --min=2 --max=5 --cpu-percent=60
sim load 90
```
- **成功判定**：vault 挂载 settings 和 data、data 持久化 graduate、web HPA 至少 3 Ready。
- **知识复盘答案**：`PVC`。
- **注意**：严格按顺序执行；先准备卷和挂载，再写入；HPA 需多次推进 tick。

## 通关与评分机制

- 源码的 `level.completed(cluster)` 以任务目标是否满足判断通关；任务按钮仅在所有目标满足且当前会话未领奖时可用。
- 每次会话只领取一次奖励；重复挑战允许更新历史最高分。后续关卡以前一关是否通关为解锁条件。
- 每关评分 JSON 指定：完成 60、资源效率 15、操作效率 10、知识问答 10、未使用提示 5（合计 100），并有 `idealActions` 最佳操作数参考。输入知识答案要点 **「验证答案」**；使用 **「请求提示」** 会影响未使用提示分。
- `Tick` 由 ViewModel 不计入玩家 `actions` 统计，但会推进集群；正常查询不会改变模拟状态。不要为了省操作而跳过必须的创建或修复动作。
- 游戏内定义的“Ready”“Service route”“Storage write”等是模拟器里的状态断言，**不是实际 Kubernetes 的可用性测试**。

## 卡关排查

| 现象 | 处理 |
|---|---|
| 命令执行后目标还是 `○` | 先推进数次 tick，再查看「任务」「状态」和 `kubectl get pods`。某些目标不仅要求命令执行，还要求 Ready。 |
| `Pending`、`FailedScheduling` | 检查 Worker Ready、CPU/内存 requests、nodeSelector；依据关卡提示增设节点或调整资源。 |
| Pod 是 `Starting` | 再推进 tick；在此模型中 Pod 至少经历后续 tick 才 Running / Ready。 |
| `kubectl delete pod` 失败 | 先用 `kubectl get pods` 查真实生成的 Pod 名，不能复制示例占位符。 |
| Service 没有 endpoints | 检查 web/hello 的 `app` 标签、Service selector、Namespace、Pod Ready。 |
| Ingress / `sim connect` 失败 | 先确保目标 Service 存在且具有 Ready endpoints，随后重新发起请求。 |
| PVC 不 Bound / Pod 等待存储 | 检查 PV 容量是否满足 PVC，并推进 tick；写入前 Pod 必须 Ready 且已挂载。 |
| HPA 不伸缩 | 检查 `sim load`、HPA min/max、target CPU、节点容量，连续推进 tick。 |
| 更新/回滚卡住 | 检查可用 Worker 资源；固定 maxSurge=1，缺少空间时不会假装滚动完成。 |
| YAML 被拒绝 | 回到示例清单修改；编辑器不是通用 YAML 解析器，不支持额外字段、任意顺序和多文档。 |
| `此关不允许此操作` | 每关有 `allowedActions` 白名单；检查当前关卡与关卡 JSON，不要使用其他关的命令。 |
| 任务全 ✓ 仍不能下一关 | 回「任务」点击「验证任务并结算」；只做知识问答或离开城市不等于领奖。 |
| 游戏重开进度不对 | 用「继续保存的城市」恢复会话；重新点击关卡会开始该关的新会话，但最高分历史保留。 |
| 文档命令与设备行为不一致 | 以安装 APK 对应源码版本、当前终端 `help`、具体错误与关卡 JSON 为准；设备侧完整验收仍待完成。 |

## 命令与文档来源

- [关卡数据（50 个 JSON）](../app/src/main/assets/levels)
- [游戏操作说明](../README.md)
- [模拟器规则与限制](SIMULATION_RULES.md)
- [逐关实现状态](IMPLEMENTATION_STATUS.md)
- [游戏内容编辑规范](CONTENT_AUTHORING.md)
- [架构和存档](ARCHITECTURE.md)

维护建议：修改关卡提示、`objectives`、`knowledgeAnswer` 后同步本秘籍，并运行 `python scripts/validate_levels.py`、`./gradlew :engine:test`，再做真机通关回归。
