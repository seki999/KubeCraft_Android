package com.seki999.kubecraft
import androidx.room.Room
import androidx.test.platform.app.InstrumentationRegistry
import com.seki999.kubecraft.data.*
import com.seki999.kubecraft.engine.*
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.encodeToString
import org.junit.Assert.*
import org.junit.Test
class SaveDatabaseTest {
 @Test fun sqliteReopenRetainsProgressAndSession() = runBlocking {
  val context = InstrumentationRegistry.getInstrumentation().targetContext
  val name = "kubecraft-instrumentation-test.db"
  context.deleteDatabase(name)
  val saved = Save(progress = Progress(best = mapOf(1 to 95),xp = 110),session = Session(levelId = 2,cluster = Cluster(nodes = listOf(Node("worker-1")))))
  val db = Room.databaseBuilder(context,GameDatabase::class.java,name).build()
  try { db.saves().put(SaveRow("main",Content.json.encodeToString(saved))) } finally { db.close() }
  val reopened = Room.databaseBuilder(context,GameDatabase::class.java,name).build()
  try { assertEquals(saved,Content.json.decodeFromString<Save>(reopened.saves().load("main")!!.payload)) } finally { reopened.close(); context.deleteDatabase(name) }
 }
}
