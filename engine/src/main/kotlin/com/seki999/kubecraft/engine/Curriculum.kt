package com.seki999.kubecraft.engine
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

@Serializable data class Dialogue(val speaker: String, val text: String)
@Serializable data class Simulation(val seed: Long, val initialState: Cluster)
@Serializable data class Objective(val type: String, val target: String = "", val value: Int = 1, val text: String, val expected: String = "") {
 fun met(s: Cluster): Boolean = when(type) {
  "nodes" -> s.nodes.count { it.ready } >= value
  "readyPods" -> s.pods.count { it.ready && (expected.isEmpty() || it.namespace == expected) && (target.isEmpty() || it.owner == target || it.name == target) } >= value
  "replicas" -> s.deployments.any { it.name == target && it.replicas == value } && s.pods.count { it.owner == target && it.ready } == value
  "endpoints" -> s.services.any { it.name == target && it.endpoints.size >= value }
  "image" -> s.deployments.any { it.name == target && it.image == "nginx:$value" } && s.pods.filter { it.owner == target }.let { it.isNotEmpty() && it.all { p -> p.ready && p.image == "nginx:$value" } }
  "event" -> s.events.count { it.reason == target } >= value
  "namespace" -> s.namespaces.any { it.name == target }
  "serviceType" -> s.services.any { it.name == target && it.type == expected && it.endpoints.isNotEmpty() }
  "route" -> target in s.advanced.routes && when {
   target.startsWith("pod/") -> s.pods.any { it.name == target.removePrefix("pod/") && it.ready }
   target.endsWith(".svc.cluster.local") -> target.split('.').let { parts -> s.services.any { it.name == parts[0] && it.namespace == parts[1] && it.endpoints.isNotEmpty() } }
   else -> s.advanced.ingresses.any { ingress -> ingress.host == target && s.services.any { it.name == ingress.service && it.namespace == ingress.namespace && it.endpoints.isNotEmpty() } }
  }
  "configMounted" -> s.pods.any { it.name == target && it.ready && it.config == expected && s.configMaps.any { c -> c.name == expected && c.data.isNotEmpty() } }
  "secretMounted" -> s.pods.any { it.name == target && it.ready && it.secret == expected && s.secrets.any { c -> c.name == expected && c.data.isNotEmpty() } }
  "mounted" -> s.pods.any { it.name == target && it.ready && it.claim == expected }
  "boundClaim" -> s.claims.any { it.name == target && it.volume != null }
  "persisted" -> s.advanced.data[target] == expected
  "stateful" -> s.advanced.statefulSets.any { it.name == target } && s.pods.count { it.owner == "stateful:$target" && it.ready } >= value
  "requests" -> s.pods.any { it.name == target && it.ready && it.cpu == value }
  "priority" -> s.pods.any { it.name == target && it.ready && it.priority >= value } && s.pods.any { it.name != target && it.node == null }
  "selected" -> s.pods.any { it.name == target && it.ready && it.nodeSelector["zone"] == expected }
  "hpa" -> s.hpas.any { it.deployment == target } && s.deployments.any { it.name == target && it.replicas >= value } && s.pods.count { it.owner == target && it.ready } >= value
  "load" -> s.metrics.cpuPercent == value
  "probe" -> s.pods.any { it.name == target && it.ready && if(expected == "liveness") it.livenessProbe && it.restarts >= value else it.readinessProbe && it.healthy }
  "release" -> s.advanced.releases.any { it.name == target && it.replicas == value && it.revision >= expected.toInt() } && s.pods.count { it.owner == target && it.ready } == value
  "plan" -> s.advanced.plan?.let { it.applied && it.workers == value && it.region == expected } == true
  "region" -> s.advanced.region == target
  "oke" -> s.advanced.oke
  "pool" -> s.advanced.oke && s.advanced.nodePool == value && s.nodes.count { it.name.startsWith("oke-worker-") && it.ready } == value
  else -> false
 }
}
@Serializable data class ScoreRules(val completion: Int = 60, val resource: Int = 15, val operations: Int = 10, val knowledge: Int = 10, val noHints: Int = 5, val idealActions: Int = 5)
@Serializable data class Rewards(val xp: Int = 100, val achievement: String)
@Serializable data class Level(val schemaVersion: Int, val id: Int, val chapterId: Int, val title: String, val description: String, val prerequisites: List<Int>, val knowledgeIds: List<String>, val story: String, val dialogues: List<Dialogue>, val simulation: Simulation, val allowedActions: List<String>, val objectives: List<Objective>, val hints: List<String>, val review: String, val score: ScoreRules, val rewards: Rewards, val implemented: Boolean = false, val knowledgeQuestion: String = "", val knowledgeAnswer: String = "") {
 fun validate() {
  require(schemaVersion == 1 && id in 1..50 && chapterId == (id-1)/5+1)
  require(title.isNotBlank() && story.isNotBlank() && review.isNotBlank())
  require(prerequisites.all { it in 1 until id } && knowledgeIds.isNotEmpty() && dialogues.isNotEmpty() && hints.isNotEmpty() && objectives.isNotEmpty())
  require(objectives.all { it.type in setOf("nodes","readyPods","replicas","endpoints","image","event","planned","namespace","serviceType","route","configMounted","secretMounted","mounted","boundClaim","persisted","stateful","requests","priority","selected","hpa","load","probe","release","plan","region","oke","pool") && it.text.isNotBlank() })
  require(!implemented || objectives.none { it.type == "planned" })
  require(!implemented || (knowledgeQuestion.isNotBlank() && knowledgeAnswer.isNotBlank()))
  require(allowedActions.distinct().size == allowedActions.size && allowedActions.all { it in setOf("AddNode","SetNodeReady","CreatePod","Deploy","Scale","DeletePod","UpdateImage","Rollback","Expose","SetLoad","Autoscale","Advanced","Tick") })
  val c = simulation.initialState
  require(c.nodes.map { it.name }.distinct().size == c.nodes.size)
  require(c.pods.map { "${it.namespace}/${it.name}" }.distinct().size == c.pods.size)
  require(c.nodes.all { it.cpu > 0 && it.memory > 0 } && c.pods.all { it.cpu > 0 && it.memory > 0 && it.limitMemory > 0 })
  require(c.deployments.all { it.replicas in 0..30 && it.cpu > 0 && it.memory > 0 })
  require(score.completion + score.resource + score.operations + score.knowledge + score.noHints == 100)
 }
 fun completed(s: Cluster) = implemented && objectives.all { it.met(s) }
 fun points(s: Cluster, actions: Int, hints: Int, understood: Boolean): Int {
  if(!completed(s)) return 0
  val used = s.pods.sumOf { it.cpu }.toDouble()
  val capacity = s.nodes.sumOf { it.cpu }.coerceAtLeast(1)
  return score.completion + (score.resource * (used/capacity * 4).coerceIn(0.0,1.0)).toInt() + (score.operations - (actions-score.idealActions).coerceAtLeast(0)).coerceAtLeast(0) + (if(understood) score.knowledge else 0) + (if(hints == 0) score.noHints else 0)
 }
}
object Content { val json = Json { encodeDefaults = true }; fun parse(text: String) = json.decodeFromString<Level>(text).also { it.validate() } }
