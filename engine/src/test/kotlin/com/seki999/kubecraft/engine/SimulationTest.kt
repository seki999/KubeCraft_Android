package com.seki999.kubecraft.engine
import org.junit.Assert.*
import org.junit.Test
import java.io.File
import kotlinx.serialization.json.*

class SimulationTest {
 private val engine = SimulationEngine()
 private fun ticks(state: Cluster, n: Int = 4): Cluster = (1..n).fold(state) { s,_ -> engine.reduce(s,GameAction.Tick) }
 @Test fun deploymentSchedulesAndHeals() {
  var s = ticks(engine.reduce(Cluster(nodes = listOf(Node("n"))),GameAction.Deploy(Deployment("web",2))))
  assertEquals(2,s.pods.count { it.ready }); assertEquals(1,s.replicaSets.size)
  val deleted = s.pods.first().name
  s = ticks(engine.reduce(s,GameAction.DeletePod(deleted)))
  assertEquals(2,s.pods.count { it.ready }); assertFalse(s.pods.any { it.name == deleted })
 }
 @Test fun capacityReadyAndSelectorAreEnforced() {
  val p = Pod("p",cpu = 250,nodeSelector = mapOf("zone" to "b"))
  var s = engine.reduce(Cluster(nodes = listOf(Node("small",cpu = 100),Node("off",ready = false))),GameAction.CreatePod(p))
  assertNull(s.pods.single().node)
  s = ticks(s.copy(nodes = s.nodes+Node("ok",labels = mapOf("zone" to "b"))))
  assertEquals("ok",s.pods.single().node); assertTrue(s.pods.single().ready)
 }
 @Test fun serviceOnlyRoutesReadyMatchingPods() {
  var s = engine.reduce(Cluster(nodes = listOf(Node("n"))),GameAction.Deploy(Deployment("web")))
  s = engine.reduce(s,GameAction.Expose("web"))
  s = ticks(s); assertEquals(1,s.services.single().endpoints.size)
  s = engine.reduce(s,GameAction.SetNodeReady("n",false)); assertTrue(s.services.single().endpoints.isEmpty())
 }
 @Test fun rollingUpdateAndRollbackConverge() {
  var s = ticks(engine.reduce(Cluster(nodes = listOf(Node("n"))),GameAction.Deploy(Deployment("web",2))))
  s = engine.reduce(s,GameAction.UpdateImage("web","nginx:2"))
  assertTrue(s.pods.any { it.image == "nginx:1" && it.ready })
  s = ticks(s,12); assertEquals(2,s.pods.size); assertTrue(s.pods.all { it.image == "nginx:2" && it.ready })
  s = ticks(engine.reduce(s,GameAction.Rollback("web")),12)
  assertTrue(s.pods.all { it.image == "nginx:1" && it.ready })
 }
 @Test fun loadDrivesHpaWithinBounds() {
  var s = ticks(engine.reduce(Cluster(nodes = listOf(Node("n"))),GameAction.Deploy(Deployment("web",2))))
  s = engine.reduce(s,GameAction.Autoscale(Hpa("web",1,4,50)))
  s = engine.reduce(s,GameAction.SetLoad(100)); s = ticks(s)
  assertEquals(4,s.deployments.single().replicas)
 }
 @Test fun deterministicReplayAndNoShell() {
  val initial = Cluster(seed = 42,nodes = listOf(Node("n")))
  assertEquals(ticks(initial),ticks(initial))
  listOf("rm -rf /","kubectl get pods; whoami","sh","kubectl scale deployment/web --replicas=-1").forEach { line ->
   val r = Terminal.parse(line,initial)
   if(r.action == null) assertTrue(r.output.startsWith("错误")) else try { engine.reduce(initial,r.action); fail("Invalid action") } catch(_: IllegalArgumentException) { }
  }
 }
 @Test fun allFiftyLevelsStrictlyParseAndFirstTenCanBeWon() {
  val files = File("app/src/main/assets/levels").listFiles()!!.sortedBy { it.name }
  assertEquals(50,files.size)
  val levels = files.map { Content.parse(it.readText()) }
  assertEquals((1..50).toList(),levels.map { it.id })
  for(l in levels.take(10)) {
   var s = ticks(l.simulation.initialState.copy(seed = l.simulation.seed),3)
   val actions = when(l.id) {
    1 -> listOf(GameAction.AddNode("worker-1"))
    2 -> listOf(GameAction.CreatePod(Pod("hello")))
    3 -> listOf(GameAction.AddNode("worker-1"))
    4 -> listOf(GameAction.AddNode("worker-2"))
    5 -> listOf(GameAction.Deploy(Deployment("web")))
    6 -> listOf(GameAction.Scale("web",2))
    7 -> listOf(GameAction.DeletePod(s.pods.first().name))
    8 -> listOf(GameAction.Scale("web",1))
    9 -> listOf(GameAction.UpdateImage("web","nginx:2"))
    else -> listOf(GameAction.Rollback("web"))
   }
   assertFalse("Level ${l.id} should require work",l.completed(s))
   actions.forEach { s = engine.reduce(s,it) }; s = ticks(s,15)
   assertTrue("Level ${l.id}",l.completed(s))
   assertTrue(l.points(s,1,0,true) in 60..100)
  }
 }
 @Test fun advancedFortyLevelsHavePlayableStateAssertions() {
  val recipes = Content.json.parseToJsonElement(File("engine/src/test/resources/solutions.json").readText()).jsonObject
  for(id in 11..50) {
   val l = Content.parse(File("app/src/main/assets/levels/level-%03d.json".format(id)).readText())
   var state = ticks(l.simulation.initialState.copy(seed = l.simulation.seed),3)
   assertFalse("$id initial state must need an action",l.completed(state))
   for(item in recipes[id.toString()]!!.jsonArray) {
    val command = item.jsonPrimitive.content
    val action = when(command) {
     "@yaml" -> Manifest.parse(Manifest.example)
     "@yaml3" -> Manifest.parse(Manifest.example.replace("replicas: 2","replicas: 3"))
     else -> {
      val result = Terminal.parse(command,state)
      assertFalse("$id $command: ${result.output}",result.output.startsWith("错误"))
      result.action
     }
    }
    if(action != null) state = engine.reduce(state,action)
    state = ticks(state,4)
   }
   state = ticks(state,20)
   assertTrue("$id ${l.title}: ${l.objectives.filterNot { it.met(state) }}",l.completed(state))
  }
 }
 @Test fun storageSurvivesPodDeletionAndStatefulIdentityRestores() {
  var s = Cluster(nodes = listOf(Node("n")),volumes = listOf(PersistentVolume("disk",1024)),claims = listOf(PersistentVolumeClaim("data",512)))
  s = ticks(s)
  s = ticks(engine.reduce(s,GameAction.Advanced(Operation.Stateful(StatefulSet("db",1,claim = "data")))))
  s = engine.reduce(s,GameAction.Advanced(Operation.Write("data","saved")))
  s = ticks(engine.reduce(s,GameAction.DeletePod("db-0")))
  assertEquals("saved",s.advanced.data["data"])
  assertEquals("db-0",s.pods.single().name); assertTrue(s.pods.single().ready)
 }
 @Test fun readinessRemovesEndpointsAndLivenessRestarts() {
  var s = ticks(Cluster(nodes = listOf(Node("n")),pods = listOf(Pod("hello")),services = listOf(Service("web",mapOf("app" to "web")))))
  s = engine.reduce(s,GameAction.Advanced(Operation.Probe("hello",false)))
  s = engine.reduce(s,GameAction.Advanced(Operation.Health("hello",false)))
  assertTrue(s.services.single().endpoints.isEmpty())
  s = ticks(engine.reduce(s,GameAction.Advanced(Operation.Probe("hello",true))))
  assertEquals(1,s.pods.single().restarts); assertEquals(1,s.services.single().endpoints.size)
 }
 @Test fun namespacesConstrainServiceSelection() {
  val s = ticks(Cluster(nodes = listOf(Node("n")),namespaces = listOf(Namespace("default"),Namespace("team")),pods = listOf(Pod("a"),Pod("b",namespace = "team")),services = listOf(Service("web",mapOf("app" to "web"),"team"))))
  assertEquals(listOf("b"),s.services.single().endpoints)
 }
 @Test fun unboundClaimsBlockAndInsufficientCapacityCannotBind() {
  val s = ticks(Cluster(nodes = listOf(Node("n")),pods = listOf(Pod("hello",claim = "data")),claims = listOf(PersistentVolumeClaim("data",1024)),volumes = listOf(PersistentVolume("small",100))))
  assertNull(s.claims.single().volume); assertFalse(s.pods.single().ready)
 }
 @Test fun routeGoalRequiresCurrentlyHealthyBackend() {
  var s = ticks(Cluster(nodes = listOf(Node("n")),pods = listOf(Pod("hello")),services = listOf(Service("web",mapOf("app" to "web")))))
  s = engine.reduce(s,GameAction.Advanced(Operation.Connect("web")))
  val goal = Objective("route","web.default.svc.cluster.local",text = "connected")
  assertTrue(goal.met(s))
  s = engine.reduce(s,GameAction.SetNodeReady("n",false))
  assertFalse(goal.met(s))
 }
 @Test fun serializationRoundTrip() {
  val s = ticks(engine.reduce(Cluster(nodes = listOf(Node("n"))),GameAction.Deploy(Deployment("web"))))
  val encoded = Content.json.encodeToString(Cluster.serializer(),s)
  assertEquals(s,Content.json.decodeFromString<Cluster>(encoded))
 }
 @Test fun yamlRejectsUnknownKeys() {
  assertTrue(Manifest.parse(Manifest.example) is GameAction.Deploy)
  try { Manifest.parse(Manifest.example+"\nexec: whoami"); fail() } catch(_: IllegalArgumentException) { }
 }
}
