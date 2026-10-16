package com.seki999.kubecraft
import com.seki999.kubecraft.data.*
import com.seki999.kubecraft.engine.*
import kotlinx.serialization.encodeToString
import org.junit.Assert.*
import org.junit.Test
class ProgressTest {
 @Test fun fullProgressAndSnapshotSurviveSerialization() {
  val save = Save(progress = Progress(best = mapOf(1 to 93),completions = mapOf(1 to 2),xp = 220,learned = setOf("k001"),achievements = setOf("chapter-1-lesson-1")),session = Session(levelId = 2,cluster = Cluster(nodes = listOf(Node("worker-1")),pods = listOf(Pod("hello"))),actions = 3,hints = 1,understood = true,claimed = true))
  assertEquals(save,Content.json.decodeFromString<Save>(Content.json.encodeToString(save)))
 }
 @Test fun oldSaveReceivesDefaultsWithoutDestructiveReset() {
  val old = """{"progress":{"best":{"1":91},"xp":110},"session":{"levelId":2,"cluster":{"nodes":[{"name":"worker-1"}]}}}"""
  val loaded = Content.json.decodeFromString<Save>(old)
  assertEquals(91,loaded.progress.best[1]); assertEquals(110,loaded.progress.xp)
  assertEquals("worker-1",loaded.session.cluster.nodes.single().name)
  assertFalse(loaded.session.claimed); assertTrue(loaded.session.cluster.advanced.routes.isEmpty())
 }
}
