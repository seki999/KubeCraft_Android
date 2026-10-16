# 模拟规则与支持边界

## 时间与确定性
每个 GameAction 推进一个逻辑 tick。输入相同初始快照和操作序列会产生完全相同状态；seed 存入 Cluster，当前算法无随机分支，不读取系统时间。事件仅由 reducer 产生，保留最近 100 条。

## 调度与控制器
节点默认 2000m CPU、4096Mi 内存。调度只检查 Ready、requests 和 nodeSelector，按稳定节点顺序 first-fit 放置未绑定 Pod。已绑定 Pod 在节点故障时 Unknown / NotReady；当前没有真实 Kubernetes 的 eviction 延迟和重调度容忍机制。Pod 先 Starting，至少后续 tick 才 Running/Ready。

Deployment 根据 desired replicas 创建受管 Pod，独立 Pod 删除后不会恢复。ReplicaSet 是按 revision 投影的教学控制器模型，不是完整独立 API Server 对象。缩容移除多余 Pod。滚动更新使用最多一个额外副本；新副本 Ready 后再移除一个旧副本。空间不足会导致更新等待，不能伪装完成。回滚恢复历史镜像模板并增加 revision。当前没有更新失败超时、maxUnavailable 配置或完整历史 ReplicaSet 生命周期。

Service 只选取同 Namespace、labels 匹配且 Ready 的 Pod；状态变化后重建端点。NodePort、LoadBalancer 是教学类型，不开放端口。无 CNI、kube-proxy、真实 DNS、Ingress controller 或网络流量。

HPA 核心以 ceil(currentReplicas × simulatedCPU / target) 计算并限制 min/max，只在 Tick 采样；下一轮控制器处理新副本数。无真实 metrics-server、稳定窗口或生产级去抖。

## 终端与 YAML
`help` 显示全部实现语法。get、logs、describe、top 输出读取当前快照。create、run、scale、delete pod、expose、set image、rollout undo 产生 GameAction；create node 是教学扩展。无 shell 或进程执行，不解释管道、脚本、文件路径、真实网络地址。

YAML 编辑器只支持公开示例的单容器 Deployment 结构（apiVersion、kind、metadata.name、spec.replicas、template.spec.containers.name/image），限定缩进和字段顺序；拒绝未知、重复字段和多文档。它不是通用 YAML 解析器。无效输入不会提交 GameAction。

## 网络、命名空间与配置
`sim ping POD` 检查 Ready Pod 的逻辑可达性；不分配真实 IP。`sim connect SERVICE --namespace=team` 检查该 Namespace 中 Service 的当前 Ready 端点并记录逻辑 DNS 路径。Ingress host 路由必须指向有端点的 Service。`sim request` 成功状态来自这些条件，非固定文本。

Namespace 约束 Pod/Deployment 创建和 Service 端点选择；当前多数命令仍用单一名称定位，不支持同名跨 Namespace 管理。没有 NetworkPolicy、RBAC、CNI、DNS 服务器、实际 socket 或云负载均衡器。

ConfigMap/Secret 支持键值和 Pod 挂载引用；挂载必须引用存在的资源。Secret 查询仅返回键数。没有容器文件系统、环境变量注入或 Secret 加密；只输入演示凭据。日志/describe 不展示 Secret 内容，但输入命令仍会显示在当前终端历史中。

## 存储
PV 按容量 first-fit 与未绑定 PVC 一对一绑定；未 Bound 的 PVC 阻止引用 Pod Ready。写入必须有 Bound Claim 和 Ready 的挂载 Pod。数据存储于独立高级状态，不随 Pod 删除。StatefulSet 创建稳定名称 name-0 等，删除后恢复同名 Pod；教学子集共享一个预建 Claim，不实现每副本 PVC、StorageClass、访问模式或真实文件 IO。

## 资源、优先级与健康
CPU/内存 requests 约束调度。内存使用超过 limit 标记 OOMKilled，降低负载后可恢复；不实现真实 cgroup、CPU throttling 和 CrashLoopBackOff。Priority 仅影响等待队列顺序，不抢占运行 Pod。`sim resources/selector/priority` 只修改尚未绑定 Pod，属于教学扩展；生产通常更新模板或重建。

readiness probe + unhealthy 会撤销 Ready 与 Service 端点；liveness probe + unhealthy 增加 restarts 并以一次成功恢复演示重启。未模拟阈值、周期、超时与真实 HTTP 探针。

## Helm / Terraform / OCI / OKE
Helm 仅支持内置 web-chart 的 install/upgrade 与 replicas 参数；Release revision 与实际 Deployment desired 状态共同变化。不解释任意模板、hooks、依赖或外部仓库。

Terraform 仅支持三个内置区域、1–5 个模拟 Worker 的 plan/apply。Plan 存入引擎快照，apply 必须有未应用计划，创建实际模拟 Node。没有 HCL、provider、远端状态或真实资源。

OCI 区域选择是教学状态。OKE 创建依赖已选区域，节点池依赖 OKE 控制平面；节点池实际产生带 pool 标签的模拟 Worker，承载同一调度器中的 Pod。不能缩掉仍有 Pod 的池节点。没有真实云 API、IAM、VCN、计费和云实例。第 46–48 关是概念链的较小交互，49–50 是联合状态任务，全部应按“可游玩但受限”理解。
