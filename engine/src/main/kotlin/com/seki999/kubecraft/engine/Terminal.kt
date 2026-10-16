package com.seki999.kubecraft.engine

data class CommandResult(val output: String, val action: GameAction? = null)
/** Deliberately restricted grammar. Never executes an OS command. */
object Terminal {
 fun parse(line: String, s: Cluster): CommandResult { return try {
  require(!line.contains(Regex("[;|&`$]"))) { "不支持 shell 表达式" }
  AdvancedTerminal.parse(line,s)?.let { return it }
  val a = line.trim().split(Regex("\\s+")); require(a.first() == "kubectl") { "只支持离线 kubectl 子集；输入 help 查看范围" }
  fun arg(i: Int) = a.getOrNull(i) ?: error("缺少参数")
  fun flag(prefix: String) = a.firstOrNull { it.startsWith("$prefix=") }?.substringAfter('=') ?: error("缺少 $prefix= 参数")
  when(arg(1)) {
   "get" -> CommandResult(when(arg(2)) {
    "nodes" -> "NAME  READY  CPU(m)\n" + s.nodes.joinToString("\n") { "${it.name}  ${it.ready}  ${it.cpu}" }
    "pods" -> "NAME  STATUS  READY  NODE  IMAGE\n" + s.pods.joinToString("\n") { "${it.name}  ${it.phase}  ${it.ready}  ${it.node ?: "<none>"}  ${it.image}" }
    "deployments" -> s.deployments.joinToString("\n") { "${it.name} desired=${it.replicas} image=${it.image}" }
    "replicasets" -> s.replicaSets.joinToString("\n") { "${it.name} desired=${it.desired}" }
    "services" -> s.services.joinToString("\n") { "${it.name} ${it.type} endpoints=${it.endpoints.joinToString()}" }
    "events" -> s.events.joinToString("\n") { "${it.tick} ${it.reason} ${it.message}" }
    else -> error("支持 nodes/pods/deployments/replicasets/services/events")
   })
   "create" -> when(arg(2)) {
    "node" -> CommandResult("模拟添加 Worker", GameAction.AddNode(arg(3)))
    "deployment" -> CommandResult("Deployment 已提交", GameAction.Deploy(Deployment(arg(3), image = flag("--image"), namespace = a.firstOrNull { it.startsWith("--namespace=") }?.substringAfter('=') ?: "default") ))
    else -> error("支持 create node / deployment")
   }
   "run" -> CommandResult("Pod 已提交",GameAction.CreatePod(Pod(arg(2), image = flag("--image"), namespace = a.firstOrNull { it.startsWith("--namespace=") }?.substringAfter('=') ?: "default")))
   "scale" -> CommandResult("副本数已提交", GameAction.Scale(arg(2).removePrefix("deployment/"), flag("--replicas").toInt()))
   "delete" -> { require(arg(2) == "pod"); CommandResult("Pod 删除已提交",GameAction.DeletePod(arg(3))) }
   "expose" -> { require(arg(2) == "deployment"); CommandResult("Service 已提交",GameAction.Expose(arg(3),a.firstOrNull { it.startsWith("--type=") }?.substringAfter('=') ?: "ClusterIP")) }
   "set" -> { require(arg(2) == "image"); CommandResult("更新已提交", GameAction.UpdateImage(arg(3).removePrefix("deployment/"),arg(4).substringAfter('='))) }
   "rollout" -> { require(arg(2) == "undo"); CommandResult("回滚已提交", GameAction.Rollback(arg(3).removePrefix("deployment/"))) }
   "logs" -> CommandResult(s.pods.first { it.name == arg(2) }.logs.joinToString("\n").ifBlank { "容器尚未就绪" })
   "describe" -> CommandResult(s.pods.first { it.name == arg(3) }.let { "${it.name} ${it.phase} requests=${it.cpu}m/${it.memory}Mi limit=${it.limitMemory}Mi node=${it.node} config=${it.config} claim=${it.claim} restarts=${it.restarts}" })
   "top" -> CommandResult("模拟 CPU utilization: ${s.metrics.cpuPercent}%")
   else -> error("不支持此命令。help 可查看离线子集")
  }
 } catch(e: Exception) { CommandResult("错误：${e.message}") } }
 val help = AdvancedTerminal.help + "\n" + """kubectl get nodes|pods|deployments|replicasets|services|events
kubectl create node worker-1
kubectl run hello --image=nginx:1
kubectl create deployment web --image=nginx:1
kubectl scale deployment/web --replicas=3
kubectl delete pod NAME
kubectl expose deployment web --type=ClusterIP
kubectl set image deployment/web web=nginx:2
kubectl rollout undo deployment/web
kubectl logs NAME
kubectl describe pod NAME
kubectl top pods
模拟时钟通过‘推进 1 tick’运行。节点添加为教学扩展。"""
}
object Manifest {
 /** Flat Deployment teaching subset; rejects unsupported YAML instead of silently ignoring it. */
 fun parse(text: String): GameAction {
  val fields = text.lines().filter { it.isNotBlank() && !it.trim().startsWith("#") }
  require(fields.none { '\t' in it }) { "YAML 不支持 Tab 缩进" }
  val patterns = listOf(
   Regex("apiVersion: apps/v1\\s*"), Regex("kind: Deployment\\s*"), Regex("metadata:\\s*"),
   Regex("  name: ([a-z0-9](?:[a-z0-9-]{0,61}[a-z0-9])?)\\s*"), Regex("spec:\\s*"),
   Regex("  replicas: ([0-9]+)\\s*"), Regex("  template:\\s*"), Regex("    spec:\\s*"),
   Regex("      containers:\\s*"), Regex("        - name: ([a-z0-9](?:[a-z0-9-]{0,61}[a-z0-9])?)\\s*"),
   Regex("          image: ([a-zA-Z0-9_./:@-]+)\\s*")
  )
  require(fields.size == patterns.size && fields.zip(patterns).all { (line,pattern) -> pattern.matches(line) }) { "只支持示例的单容器 Deployment 结构、字段顺序和缩进；未知/重复字段已拒绝" }
  val name = patterns[3].matchEntire(fields[3])!!.groupValues[1]
  val replicas = patterns[5].matchEntire(fields[5])!!.groupValues[1].toInt(); require(replicas in 0..30)
  val image = patterns[10].matchEntire(fields[10])!!.groupValues[1]
  return GameAction.Deploy(Deployment(name, replicas, image))
 }
 val example = """apiVersion: apps/v1
kind: Deployment
metadata:
  name: web
spec:
  replicas: 2
  template:
    spec:
      containers:
        - name: web
          image: nginx:1"""
}
