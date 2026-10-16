package com.seki999.kubecraft.engine

import kotlin.math.ceil

/** Pure reducer: no wall clock, IO, Android or process execution. */
class SimulationEngine {
 fun reduce(state: Cluster, action: GameAction): Cluster {
  fun valid(name: String) { require(name.matches(Regex("[a-z0-9](?:[a-z0-9-]{0,61}[a-z0-9])?"))) { "无效的 Kubernetes 教学对象名称" } }
  fun validImage(image: String) { require(image.matches(Regex("[a-zA-Z0-9_./:@-]+"))) { "镜像名不能为空或包含空格" } }
  var s = state.copy(tick = state.tick + 1)
  fun event(reason: String, message: String) { s = s.copy(events = (s.events + Event(s.tick, reason, message)).takeLast(100)) }
  when(action) {
   is GameAction.AddNode -> { valid(action.name); require(s.nodes.size < 32) { "教学集群最多 32 个节点" }; require(s.nodes.none { it.name == action.name }); s = s.copy(nodes = s.nodes + Node(action.name)); event("NodeReady", action.name) }
   is GameAction.SetNodeReady -> { require(s.nodes.any { it.name == action.name }); s = s.copy(nodes = s.nodes.map { if(it.name == action.name) it.copy(ready = action.ready) else it }); event("NodeStatus", "${action.name}: ${action.ready}") }
   is GameAction.CreatePod -> { valid(action.pod.name); validImage(action.pod.image); require(action.pod.cpu > 0 && action.pod.memory > 0 && action.pod.limitMemory >= action.pod.memory); require(s.pods.size < 256); require(s.namespaces.any { it.name == action.pod.namespace }); require(s.pods.none { it.name == action.pod.name && it.namespace == action.pod.namespace }); s = s.copy(pods = s.pods + action.pod.copy(born = s.tick, node = null, ready = false, phase = "Pending")); event("Created", action.pod.name) }
   is GameAction.Deploy -> { valid(action.deployment.name); validImage(action.deployment.image); require(action.deployment.cpu > 0 && action.deployment.memory > 0); require(s.deployments.size < 20); require(action.deployment.replicas in 0..30); require(s.deployments.none { it.name == action.deployment.name }); require(s.namespaces.any { it.name == action.deployment.namespace }); s = s.copy(deployments = s.deployments + action.deployment); event("DeploymentCreated", action.deployment.name) }
   is GameAction.Scale -> { require(action.replicas in 0..30); require(s.deployments.any { it.name == action.name }); s = s.copy(deployments = s.deployments.map { if(it.name == action.name) it.copy(replicas = action.replicas) else it }); event("Scaled", "${action.name} → ${action.replicas}") }
   is GameAction.DeletePod -> { require(s.pods.any { it.name == action.name }); s = s.copy(pods = s.pods.filterNot { it.name == action.name }); event("Deleted", action.name) }
   is GameAction.UpdateImage -> { validImage(action.image); require(s.deployments.any { it.name == action.name }); s = s.copy(deployments = s.deployments.map { if(it.name == action.name) it.copy(image = action.image, revision = it.revision + 1, history = it.history + it.image) else it }); event("RollingUpdate", action.image) }
   is GameAction.Rollback -> { val d = s.deployments.first { it.name == action.name }; require(d.history.isNotEmpty()); s = s.copy(deployments = s.deployments.map { if(it.name == d.name) it.copy(image = d.history.last(), revision = d.revision + 1, history = d.history.dropLast(1)) else it }); event("Rollback", d.name) }
   is GameAction.Expose -> { val d = s.deployments.first { it.name == action.name }; require(s.services.none { it.name == d.name }); require(action.type in listOf("ClusterIP", "NodePort", "LoadBalancer")); s = s.copy(services = s.services + Service(d.name, mapOf("app" to d.name), d.namespace, action.type)); event("ServiceCreated", d.name) }
   is GameAction.SetLoad -> { require(action.percent in 0..100); s = s.copy(metrics = Metrics(action.percent)) }
   is GameAction.Autoscale -> { require(s.deployments.any { it.name == action.hpa.deployment }); require(action.hpa.min in 1..action.hpa.max && action.hpa.max <= 30 && action.hpa.target in 1..100); s = s.copy(hpas = s.hpas.filterNot { it.deployment == action.hpa.deployment } + action.hpa) }
   is GameAction.Advanced -> { s = AdvancedEngine.reduce(s,action.operation) }
   GameAction.Tick -> Unit
  }
  s = AdvancedEngine.reconcile(s)
  // One replacement at a time; maxSurge=1 and no removal of healthy old Pods until replacement ready.
  for(d in s.deployments) {
   var owned = s.pods.filter { it.owner == d.name && it.namespace == d.namespace }
   val old = owned.filter { it.revision != d.revision }
   val retire = old.firstOrNull { !it.ready && owned.size > d.replicas } ?: old.firstOrNull { owned.count { p -> p.ready } > d.replicas }
   if(retire != null) {
    s = s.copy(pods = s.pods - retire); owned = s.pods.filter { it.owner == d.name && it.namespace == d.namespace }
   }
   val desired = if(old.isNotEmpty() && d.replicas > 0) d.replicas + 1 else d.replicas
   if(owned.size > desired) s = s.copy(pods = s.pods - owned.takeLast(owned.size - desired).toSet())
   repeat((desired - owned.size).coerceAtLeast(0)) {
    val id = s.serial + 1
    val p = Pod("${d.name}-${d.revision}-$id", d.image, mapOf("app" to d.name), d.namespace, d.name, d.revision, d.cpu, d.memory, d.nodeSelector, born = s.tick)
    s = s.copy(serial = id, pods = s.pods + p); event("SuccessfulCreate", p.name)
   }
  }
  // Schedule in stable node order; requests count all bound Pods.
  for(p in s.pods.sortedByDescending { it.priority }) {
   val existing = s.nodes.find { it.name == p.node }
   var next = p
   if(p.usageMemory > p.limitMemory) { next = p.copy(ready = false,phase = "OOMKilled") }
   else if(p.claim != null && s.claims.none { it.name == p.claim && it.volume != null }) { next = p.copy(ready = false,phase = "Pending") }
   else if(!p.healthy && p.readinessProbe) { next = p.copy(ready = false,phase = "Running") }
   else if(p.node == null) {
    val node = s.nodes.firstOrNull { n -> n.ready && p.nodeSelector.all { n.labels[it.key] == it.value } && s.pods.filter { it.node == n.name }.sumOf { it.cpu } + p.cpu <= n.cpu && s.pods.filter { it.node == n.name }.sumOf { it.memory } + p.memory <= n.memory }
    if(node != null) { next = p.copy(node = node.name, phase = "Starting"); event("Scheduled", "${p.name} → ${node.name}") }
    else event("FailedScheduling", "${p.name}: insufficient resources / selector / Ready nodes")
   } else if(existing?.ready != true) next = p.copy(ready = false, phase = "Unknown")
   else if(s.tick > p.born + 1) next = p.copy(ready = true, phase = "Running", logs = listOf("[${s.tick}] ${p.image}: application ready"))
   s = s.copy(pods = s.pods.map { if(it.name == p.name && it.namespace == p.namespace) next else it })
  }
  s = s.copy(replicaSets = s.deployments.flatMap { d -> (s.pods.filter { it.owner == d.name }.map { it.revision } + d.revision).distinct().map { r -> ReplicaSet("${d.name}-$r", d.name, r, if(r == d.revision) d.replicas else 0) } }, services = s.services.map { service -> service.copy(endpoints = s.pods.filter { it.namespace == service.namespace && it.ready && service.selector.all { (k,v) -> it.labels[k] == v } }.map { it.name }) })
  if(action == GameAction.Tick) for(h in s.hpas) s = s.copy(deployments = s.deployments.map { if(it.name == h.deployment) it.copy(replicas = ceil(it.replicas * s.metrics.cpuPercent.toDouble() / h.target).toInt().coerceIn(h.min,h.max)) else it })
  return s
 }
}
