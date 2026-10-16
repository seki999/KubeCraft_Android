package com.seki999.kubecraft.engine

object AdvancedTerminal {
 fun parse(line: String,s: Cluster): CommandResult? {
  val a = line.trim().split(Regex("\\s+"))
  fun arg(i: Int) = a.getOrNull(i) ?: error("缺少参数")
  fun flag(key: String,default: String? = null) = a.firstOrNull { it.startsWith("$key=") }?.substringAfter('=') ?: default ?: error("缺少 $key=")
  fun op(operation: Operation) = CommandResult("本地教学操作已提交",GameAction.Advanced(operation))
  if(a.firstOrNull() == "sim") return when(arg(1)) {
   "ping" -> op(Operation.Ping(arg(2)))
   "connect" -> op(Operation.Connect(arg(2),flag("--namespace","default")))
   "request" -> op(Operation.Request(arg(2)))
   "service" -> op(Operation.ServiceApply(Service(arg(2), mapOf("app" to flag("--app")),flag("--namespace","default"),flag("--type","ClusterIP"))))
   "ingress" -> op(Operation.IngressApply(Ingress(arg(2),flag("--host"),flag("--service"),flag("--namespace","default"))))
   "mount" -> op(Operation.Mount(arg(2),arg(3),arg(4)))
   "pv" -> op(Operation.Volume(arg(2),flag("--size").toInt()))
   "pvc" -> op(Operation.Claim(arg(2),flag("--size").toInt()))
   "write" -> op(Operation.Write(arg(2),arg(3)))
   "statefulset" -> op(Operation.Stateful(StatefulSet(arg(2),flag("--replicas","1").toInt(),claim = flag("--claim"))))
   "resources" -> op(Operation.Resources(arg(2),flag("--cpu").toInt(),flag("--memory").toInt(),flag("--limit").toInt()))
   "selector" -> op(Operation.SelectNode(arg(2),arg(3).substringBefore('='),arg(3).substringAfter('=')))
   "priority" -> op(Operation.Priority(arg(2),arg(3).toInt()))
   "memory" -> op(Operation.MemoryLoad(arg(2),arg(3).toInt()))
   "probe" -> { require(arg(3) in listOf("readiness","liveness")); op(Operation.Probe(arg(2),arg(3) == "liveness")) }
   "health" -> { require(arg(3) in listOf("true","false")); op(Operation.Health(arg(2),arg(3).toBooleanStrict())) }
   "load" -> CommandResult("模拟 CPU 负载已提交",GameAction.SetLoad(arg(2).toInt()))
   "node" -> { require(arg(3) in listOf("true","false")); CommandResult("模拟节点健康已提交",GameAction.SetNodeReady(arg(2),arg(3).toBooleanStrict())) }
   "oci-region" -> op(Operation.Region(arg(2)))
   "oke-create" -> op(Operation.Oke)
   "oke-pool" -> op(Operation.Pool(arg(2).toInt()))
   else -> error("未知 sim 教学命令，查看 help")
  }
  if(a.firstOrNull() == "helm") { require(arg(1) in listOf("install","upgrade")); require(arg(3) == "web-chart"); if(arg(1) == "upgrade") require(s.advanced.releases.any { it.name == arg(2) }) { "Release 不存在" }; return op(Operation.Helm(arg(2),flag("--replicas","1").toInt())) }
  if(a.firstOrNull() == "terraform") return when(arg(1)) { "plan" -> op(Operation.Plan(flag("--region"),flag("--workers").toInt())); "apply" -> op(Operation.ApplyPlan); else -> error("仅支持 plan / apply") }
  if(a.firstOrNull() != "kubectl") return null
  return when(a.getOrNull(1)) {
   "create" -> when(a.getOrNull(2)) {
    "namespace" -> op(Operation.NamespaceCreate(arg(3)))
    "configmap" -> { val kv=flag("--from-literal"); require('=' in kv); op(Operation.Config(arg(3),kv.substringBefore('='),kv.substringAfter('='))) }
    "secret" -> { require(arg(3) == "generic"); val kv=flag("--from-literal"); require('=' in kv); op(Operation.SecretCreate(arg(4),kv.substringBefore('='),kv.substringAfter('='))) }
    else -> null
   }
   "label" -> { require(arg(2) == "pod"); require('=' in arg(4)); op(Operation.Label(arg(3),arg(4).substringBefore('='),arg(4).substringAfter('='))) }
   "autoscale" -> { require(arg(2) == "deployment"); CommandResult("HPA 已提交",GameAction.Autoscale(Hpa(arg(3),flag("--min","1").toInt(),flag("--max","5").toInt(),flag("--cpu-percent","60").toInt()))) }
   "get" -> when(a.getOrNull(2)) {
    "namespaces" -> CommandResult(s.namespaces.joinToString("\n") { it.name })
    "configmaps" -> CommandResult(s.configMaps.joinToString("\n") { "${it.name} ${it.data}" })
    "secrets" -> CommandResult(s.secrets.joinToString("\n") { "${it.name}: ${it.data.size} keys (redacted)" })
    "pv" -> CommandResult(s.volumes.joinToString("\n") { "${it.name} ${it.capacity}Mi" })
    "pvc" -> CommandResult(s.claims.joinToString("\n") { "${it.name} ${it.volume ?: "Pending"}" })
    "ingresses" -> CommandResult(s.advanced.ingresses.joinToString("\n") { "${it.host} → ${it.service}" })
    "statefulsets" -> CommandResult(s.advanced.statefulSets.joinToString("\n") { "${it.name} ${it.replicas}" })
    "hpa" -> CommandResult(s.hpas.joinToString("\n") { "${it.deployment} target=${it.target}% min=${it.min} max=${it.max}" })
    else -> null
   }
   else -> null
  }
 }
 val help = """
高级离线教学子集（sim 为本游戏扩展，不是真实 kubectl）：
sim ping POD
sim connect SERVICE --namespace=default
sim service NAME --app=web --type=ClusterIP --namespace=default
sim ingress NAME --host=web.local --service=web
sim request web.local
kubectl create namespace team
kubectl label pod NAME app=web
kubectl create configmap settings --from-literal=mode=prod
kubectl create secret generic credentials --from-literal=token=demo
sim mount POD configmap|secret|pvc NAME
sim pv disk --size=1024
sim pvc data --size=512
sim write data hello
sim statefulset db --replicas=1 --claim=data
sim resources POD --cpu=250 --memory=128 --limit=512
sim selector POD zone=a
sim priority POD 100
sim memory POD 600
sim probe POD readiness|liveness
sim health POD true|false
sim load 90
sim node NODE true|false
kubectl autoscale deployment web --min=1 --max=5 --cpu-percent=60
helm install|upgrade portal web-chart --replicas=2
terraform plan --region=ap-tokyo-1 --workers=2
terraform apply
sim oci-region ap-tokyo-1
sim oke-create
sim oke-pool 2
只模拟内置 web-chart、节点计划及区域/控制平面/节点池；不解析真实 Chart/HCL，不调用任何云 API。
"""
}
