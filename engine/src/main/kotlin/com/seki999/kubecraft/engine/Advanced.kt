package com.seki999.kubecraft.engine
import kotlinx.serialization.Serializable

@Serializable data class Ingress(val name: String, val host: String, val service: String, val namespace: String = "default")
@Serializable data class StatefulSet(val name: String, val replicas: Int, val image: String = "nginx:1", val claim: String)
@Serializable data class HelmRelease(val name: String, val replicas: Int, val revision: Int = 1)
@Serializable data class CloudPlan(val region: String, val workers: Int, val applied: Boolean = false)
@Serializable data class AdvancedState(val ingresses: List<Ingress> = emptyList(), val statefulSets: List<StatefulSet> = emptyList(), val releases: List<HelmRelease> = emptyList(), val plan: CloudPlan? = null, val routes: List<String> = emptyList(), val data: Map<String,String> = emptyMap(), val oke: Boolean = false, val region: String = "", val nodePool: Int = 0)
sealed interface Operation {
 data class NamespaceCreate(val name: String): Operation
 data class Label(val pod: String, val key: String, val value: String): Operation
 data class ServiceApply(val service: Service): Operation
 data class Connect(val service: String, val namespace: String = "default"): Operation
 data class IngressApply(val ingress: Ingress): Operation
 data class Ping(val pod: String): Operation
 data class Request(val host: String): Operation
 data class Config(val name: String, val key: String, val value: String): Operation
 data class SecretCreate(val name: String, val key: String, val value: String): Operation
 data class Mount(val pod: String, val kind: String, val name: String): Operation
 data class Volume(val name: String, val capacity: Int): Operation
 data class Claim(val name: String, val capacity: Int): Operation
 data class Write(val claim: String, val value: String): Operation
 data class Stateful(val set: StatefulSet): Operation
 data class Resources(val pod: String, val cpu: Int, val memory: Int, val limitMemory: Int): Operation
 data class SelectNode(val pod: String, val key: String, val value: String): Operation
 data class Priority(val pod: String, val value: Int): Operation
 data class MemoryLoad(val pod: String, val memory: Int): Operation
 data class Probe(val pod: String, val liveness: Boolean): Operation
 data class Health(val pod: String, val healthy: Boolean): Operation
 data class Helm(val name: String, val replicas: Int): Operation
 data class Plan(val region: String, val workers: Int): Operation
 data object ApplyPlan: Operation
 data class Region(val region: String): Operation
 data object Oke: Operation
 data class Pool(val count: Int): Operation
}
object AdvancedEngine {
 private fun valid(name: String) { require(name.matches(Regex("[a-z0-9][a-z0-9-]{0,62}"))) { "名称必须使用小写字母、数字与连字符" } }
 fun reduce(state: Cluster,op: Operation): Cluster {
  var s = state
  fun event(reason: String,message: String) { s = s.copy(events = (s.events+Event(s.tick,reason,message)).takeLast(100)) }
  fun pod(name: String,change: (Pod)->Pod) { require(s.pods.any { it.name == name }) { "Pod 不存在" }; s = s.copy(pods = s.pods.map { if(it.name == name) change(it) else it }) }
  when(op) {
   is Operation.NamespaceCreate -> { valid(op.name); require(s.namespaces.none { it.name == op.name }); s = s.copy(namespaces = s.namespaces+Namespace(op.name)); event("NamespaceCreated",op.name) }
   is Operation.Label -> { valid(op.key); pod(op.pod) { it.copy(labels = it.labels+(op.key to op.value)) }; event("LabelChanged",op.pod) }
   is Operation.ServiceApply -> { valid(op.service.name); require(s.namespaces.any { it.name == op.service.namespace }); require(op.service.type in listOf("ClusterIP","NodePort","LoadBalancer")); s = s.copy(services = s.services.filterNot { it.name == op.service.name && it.namespace == op.service.namespace }+op.service.copy(endpoints = emptyList())); event("ServiceConfigured",op.service.name) }
   is Operation.Connect -> { val service = s.services.firstOrNull { it.name == op.service && it.namespace == op.namespace } ?: error("DNS NXDOMAIN"); require(service.endpoints.isNotEmpty()) { "Service 无就绪端点" }; val dns = "${service.name}.${service.namespace}.svc.cluster.local"; s = s.copy(advanced = s.advanced.copy(routes = (s.advanced.routes+dns).distinct())); event("RequestSucceeded",dns) }
   is Operation.IngressApply -> { valid(op.ingress.name); require(op.ingress.host.contains('.')); require(s.services.any { it.name == op.ingress.service && it.namespace == op.ingress.namespace }); s = s.copy(advanced = s.advanced.copy(ingresses = s.advanced.ingresses.filterNot { it.name == op.ingress.name }+op.ingress)); event("IngressConfigured",op.ingress.host) }
   is Operation.Ping -> { require(s.pods.any { it.name == op.pod && it.ready }); s = s.copy(advanced = s.advanced.copy(routes = (s.advanced.routes+"pod/${op.pod}").distinct())); event("PodReachable",op.pod) }
   is Operation.Request -> { val ingress = s.advanced.ingresses.first { it.host == op.host }; require(s.services.any { it.name == ingress.service && it.namespace == ingress.namespace && it.endpoints.isNotEmpty() }); s = s.copy(advanced = s.advanced.copy(routes = (s.advanced.routes+op.host).distinct())); event("Http200",op.host) }
   is Operation.Config -> { valid(op.name); val prev = s.configMaps.find { it.name == op.name }; s = s.copy(configMaps = s.configMaps.filterNot { it.name == op.name } + ConfigMap(op.name,(prev?.data ?: emptyMap())+(op.key to op.value))); event("ConfigMapCreated",op.name) }
   is Operation.SecretCreate -> { valid(op.name); s = s.copy(secrets = s.secrets.filterNot { it.name == op.name } + Secret(op.name,mapOf(op.key to op.value))); event("SecretCreated",op.name) }
   is Operation.Mount -> {
    when(op.kind) { "configmap" -> { require(s.configMaps.any { it.name == op.name }); pod(op.pod) { it.copy(config = op.name) } }; "secret" -> { require(s.secrets.any { it.name == op.name }); pod(op.pod) { it.copy(secret = op.name) } }; "pvc" -> { require(s.claims.any { it.name == op.name }); pod(op.pod) { it.copy(claim = op.name,ready = false) } }; else -> error("支持 configmap/secret/pvc") }; event("Mounted","${op.pod}: ${op.kind}/${op.name}")
   }
   is Operation.Volume -> { valid(op.name); require(op.capacity > 0); require(s.volumes.none { it.name == op.name }); s = s.copy(volumes = s.volumes+PersistentVolume(op.name,op.capacity)); event("VolumeCreated",op.name) }
   is Operation.Claim -> { valid(op.name); require(op.capacity > 0); require(s.claims.none { it.name == op.name }); s = s.copy(claims = s.claims+PersistentVolumeClaim(op.name,op.capacity)); event("ClaimCreated",op.name) }
   is Operation.Write -> { require(s.claims.any { it.name == op.claim && it.volume != null }); require(s.pods.any { it.claim == op.claim && it.ready }); s = s.copy(advanced = s.advanced.copy(data = s.advanced.data+(op.claim to op.value))); event("VolumeWritten",op.claim) }
   is Operation.Stateful -> { valid(op.set.name); require(op.set.replicas in 1..5); require(s.advanced.statefulSets.none { it.name == op.set.name }); require(s.claims.any { it.name == op.set.claim && it.volume != null }); s = s.copy(advanced = s.advanced.copy(statefulSets = s.advanced.statefulSets+op.set)); event("StatefulSetCreated",op.set.name) }
   is Operation.Resources -> { require(op.cpu > 0 && op.memory > 0 && op.limitMemory >= op.memory); pod(op.pod) { require(it.node == null) { "此教学子集只允许修改尚未调度的 Pod requests" }; it.copy(cpu = op.cpu,memory = op.memory,limitMemory = op.limitMemory) }; event("ResourcesConfigured",op.pod) }
   is Operation.SelectNode -> { pod(op.pod) { require(it.node == null); it.copy(nodeSelector = mapOf(op.key to op.value)) }; event("SelectorConfigured",op.pod) }
   is Operation.Priority -> { pod(op.pod) { require(it.node == null); it.copy(priority = op.value) }; event("PriorityConfigured",op.pod) }
   is Operation.MemoryLoad -> { require(op.memory >= 0); pod(op.pod) { it.copy(usageMemory = op.memory) }; event("MemoryLoad",op.pod) }
   is Operation.Probe -> { pod(op.pod) { if(op.liveness) it.copy(livenessProbe = true) else it.copy(readinessProbe = true) }; event("ProbeConfigured",op.pod) }
   is Operation.Health -> { pod(op.pod) { it.copy(healthy = op.healthy) }; event("HealthChanged",op.pod) }
   is Operation.Helm -> {
    valid(op.name); require(op.replicas in 1..10)
    val prev = s.advanced.releases.find { it.name == op.name }
    require(prev != null || s.deployments.none { it.name == op.name }) { "同名 Deployment 不属于此 Release" }
    val d = s.deployments.find { it.name == op.name } ?: Deployment(op.name)
    s = s.copy(deployments = s.deployments.filterNot { it.name == op.name }+d.copy(replicas = op.replicas),advanced = s.advanced.copy(releases = s.advanced.releases.filterNot { it.name == op.name }+HelmRelease(op.name,op.replicas,(prev?.revision ?: 0)+1))); event("HelmReconciled",op.name)
   }
   is Operation.Plan -> { require(op.region in regions && op.workers in 1..5); s = s.copy(advanced = s.advanced.copy(plan = CloudPlan(op.region,op.workers))); event("TerraformPlan","+ ${op.workers} simulated workers in ${op.region}") }
   Operation.ApplyPlan -> { val plan = s.advanced.plan ?: error("先运行 terraform plan"); require(!plan.applied); val nodes = (1..plan.workers).map { Node("tf-worker-$it") }; require(nodes.none { n -> s.nodes.any { it.name == n.name } }); s = s.copy(nodes = s.nodes+nodes,advanced = s.advanced.copy(plan = plan.copy(applied = true))); event("TerraformApplied","仅本地教学资源") }
   is Operation.Region -> { require(op.region in regions); s = s.copy(advanced = s.advanced.copy(region = op.region)); event("RegionSelected",op.region) }
   Operation.Oke -> { require(s.advanced.region.isNotBlank()); s = s.copy(advanced = s.advanced.copy(oke = true)); event("OkeCreated","离线控制平面模型") }
   is Operation.Pool -> { require(s.advanced.oke && op.count in 1..5); val desired = (1..op.count).map { Node("oke-worker-$it",labels = mapOf("zone" to "a","pool" to "oke")) }; require(s.pods.none { it.node?.startsWith("oke-worker-") == true && desired.none { n -> n.name == it.node } }) { "先迁移待删除节点上的 Pod" }; s = s.copy(nodes = s.nodes.filterNot { it.name.startsWith("oke-worker-") }+desired,advanced = s.advanced.copy(nodePool = op.count)); event("NodePoolScaled",op.count.toString()) }
  }
  return s
 }
 val regions = setOf("ap-tokyo-1","ap-osaka-1","us-ashburn-1")
 fun reconcile(state: Cluster): Cluster {
  var s = state
  val used = s.claims.mapNotNull { it.volume }.toMutableSet()
  s = s.copy(claims = s.claims.map { c -> if(c.volume != null) c else { val v = s.volumes.firstOrNull { it.name !in used && it.capacity >= c.capacity }; v?.let { used.add(it.name) }; c.copy(volume = v?.name) } })
  for(set in s.advanced.statefulSets) for(i in 0 until set.replicas) {
   val name = "${set.name}-$i"
   if(s.pods.none { it.name == name }) s = s.copy(pods = s.pods+Pod(name,image = set.image,labels = mapOf("app" to set.name),owner = "stateful:${set.name}",claim = set.claim,born = s.tick))
  }
  // Simplified OOM and probes: liveness restarts once and models successful recovery.
  s = s.copy(pods = s.pods.map { p ->
   when {
    p.usageMemory > p.limitMemory -> p.copy(ready = false,phase = "OOMKilled",logs = listOf("[${s.tick}] OOMKilled: usage ${p.usageMemory}Mi > limit ${p.limitMemory}Mi"))
    !p.healthy && p.livenessProbe -> p.copy(healthy = true,ready = false,phase = "Starting",born = s.tick,restarts = p.restarts+1,logs = listOf("[${s.tick}] Liveness failed, container restarted"))
    else -> p
   }
  })
  return s
 }
}
