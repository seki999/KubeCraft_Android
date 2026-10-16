package com.seki999.kubecraft.data

import android.content.Context
import androidx.room.*
import androidx.datastore.preferences.core.*
import androidx.datastore.preferences.preferencesDataStore
import com.seki999.kubecraft.engine.*
import kotlinx.coroutines.flow.*
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString

@Entity(tableName = "saves") data class SaveRow(@PrimaryKey val slot: String, val payload: String)
@Dao interface SaveDao {
 @Query("SELECT * FROM saves WHERE slot = :slot") suspend fun load(slot: String): SaveRow?
 @Insert(onConflict = OnConflictStrategy.REPLACE) suspend fun put(row: SaveRow)
}
@Database(entities = [SaveRow::class], version = 1, exportSchema = false) abstract class GameDatabase: RoomDatabase() { abstract fun saves(): SaveDao }
@Serializable data class Progress(val best: Map<Int,Int> = emptyMap(), val completions: Map<Int,Int> = emptyMap(), val xp: Int = 0, val learned: Set<String> = emptySet(), val achievements: Set<String> = emptySet()) {
 fun unlocked(level: Level) = level.prerequisites.all { (best[it] ?: 0) >= 60 }
}
@Serializable data class Session(val schemaVersion: Int = 1, val levelId: Int = 0, val cluster: Cluster = Cluster(), val actions: Int = 0, val hints: Int = 0, val understood: Boolean = false, val claimed: Boolean = false)
@Serializable data class Save(val schemaVersion: Int = 1, val progress: Progress = Progress(), val session: Session = Session())
@Serializable data class Knowledge(val id: String, val chapterId: Int, val term: String, val text: String)
private val Context.settingsStore by preferencesDataStore("settings")
data class Settings(val dark: Boolean = false, val animations: Boolean = true, val fontScale: Float = 1.4f, val slow: Boolean = true)
class GameRepository(private val context: Context) {
 private val db = Room.databaseBuilder(context, GameDatabase::class.java, "kubecraft.db").build()
 val levels: List<Level> by lazy { (1..50).map { Content.parse(context.assets.open("levels/level-%03d.json".format(it)).bufferedReader().use { it.readText() }) } }
 val knowledge: List<Knowledge> by lazy { Content.json.decodeFromString(context.assets.open("knowledge.json").bufferedReader().use { it.readText() }) }
 suspend fun load(): Save = db.saves().load("main")?.let { Content.json.decodeFromString<Save>(it.payload).also { save -> require(save.schemaVersion == 1 && save.session.schemaVersion == 1) { "不支持的存档版本；旧存档已保留" } } } ?: Save()
 suspend fun save(save: Save) { db.saves().put(SaveRow("main",Content.json.encodeToString(save))) }
 val settings = context.settingsStore.data.map {
  val defaults = Settings()
  Settings(
   dark = it[booleanPreferencesKey("dark")] ?: defaults.dark,
   animations = it[booleanPreferencesKey("animations")] ?: defaults.animations,
   fontScale = it[floatPreferencesKey("fontScale")] ?: defaults.fontScale,
   slow = it[booleanPreferencesKey("slow")] ?: defaults.slow
  )
 }
 suspend fun settings(s: Settings) { context.settingsStore.edit { it[booleanPreferencesKey("dark")] = s.dark; it[booleanPreferencesKey("animations")] = s.animations; it[floatPreferencesKey("fontScale")] = s.fontScale; it[booleanPreferencesKey("slow")] = s.slow } }
}
