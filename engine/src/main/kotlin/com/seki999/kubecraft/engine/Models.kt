package com.seki999.kubecraft.engine

import kotlinx.serialization.Serializable

@Serializable data class Node(val name: String, val cpu: Int = 2000, val memory: Int = 4096, val ready: Boolean = true, val labels: Map<String,String> = mapOf("zone" to "a"))
@Serializable data class Pod(val name: String, val image: String = "nginx:1", val labels: Map<String,String> = mapOf("app" to "web"), val namespace: String = "default", val owner: String? = null, val revision: Int = 1, val cpu: Int = 250, val memory: Int = 128, val nodeSelector: Map<String,String> = emptyMap(), val node: String? = null, val phase: String = "Pending", val ready: Boolean = false, val born: Long = 0, val logs: List<String> = emptyList(), val config: String? = null, val secret: String? = null, val claim: String? = null, val limitMemory: Int = 512, val usageMemory: Int = 64, val priority: Int = 0, val healthy: Boolean = true, val readinessProbe: Boolean = false, val livenessProbe: Boolean = false, val restarts: Int = 0)
@Serializable data class Deployment(val name: String, val replicas: Int = 1, val image: String = "nginx:1", val namespace: String = "default", val revision: Int = 1, val history: List<String> = emptyList(), val cpu: Int = 250, val memory: Int = 128, val nodeSelector: Map<String,String> = emptyMap())
@Serializable data class ReplicaSet(val name: String, val deployment: String, val revision: Int, val desired: Int)
@Serializable data class Service(val name: String, val selector: Map<String,String>, val namespace: String = "default", val type: String = "ClusterIP", val endpoints: List<String> = emptyList())
@Serializable data class Namespace(val name: String)
@Serializable data class ConfigMap(val name: String, val data: Map<String,String> = emptyMap())
@Serializable data class Secret(val name: String, val data: Map<String,String> = emptyMap())
@Serializable data class PersistentVolume(val name: String, val capacity: Int)
@Serializable data class PersistentVolumeClaim(val name: String, val capacity: Int, val volume: String? = null)
@Serializable data class Event(val tick: Long, val reason: String, val message: String)
@Serializable data class Metrics(val cpuPercent: Int = 30)
@Serializable data class Hpa(val deployment: String, val min: Int = 1, val max: Int = 5, val target: Int = 60)
@Serializable data class Cluster(val seed: Long = 1, val tick: Long = 0, val serial: Int = 0, val nodes: List<Node> = emptyList(), val pods: List<Pod> = emptyList(), val deployments: List<Deployment> = emptyList(), val replicaSets: List<ReplicaSet> = emptyList(), val services: List<Service> = emptyList(), val namespaces: List<Namespace> = listOf(Namespace("default")), val configMaps: List<ConfigMap> = emptyList(), val secrets: List<Secret> = emptyList(), val volumes: List<PersistentVolume> = emptyList(), val claims: List<PersistentVolumeClaim> = emptyList(), val events: List<Event> = emptyList(), val metrics: Metrics = Metrics(), val hpas: List<Hpa> = emptyList(), val advanced: AdvancedState = AdvancedState())

sealed interface GameAction {
 data class AddNode(val name: String): GameAction
 data class SetNodeReady(val name: String, val ready: Boolean): GameAction
 data class CreatePod(val pod: Pod): GameAction
 data class Deploy(val deployment: Deployment): GameAction
 data class Scale(val name: String, val replicas: Int): GameAction
 data class DeletePod(val name: String): GameAction
 data class UpdateImage(val name: String, val image: String): GameAction
 data class Rollback(val name: String): GameAction
 data class Expose(val name: String, val type: String = "ClusterIP"): GameAction
 data class SetLoad(val percent: Int): GameAction
 data class Autoscale(val hpa: Hpa): GameAction
 data class Advanced(val operation: Operation): GameAction
 data object Tick: GameAction
}
