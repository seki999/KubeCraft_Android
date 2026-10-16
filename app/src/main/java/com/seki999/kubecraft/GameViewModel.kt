package com.seki999.kubecraft
import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.seki999.kubecraft.data.*
import com.seki999.kubecraft.engine.*
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.*

data class GameUi(val loaded: Boolean = false, val save: Save = Save(), val output: String = "离线模拟终端。输入 help 查看命令。", val error: String? = null, val result: Int? = null)
class GameViewModel(app: Application): AndroidViewModel(app) {
 private val repository = GameRepository(app)
 private val engine = SimulationEngine()
 private val mutable = MutableStateFlow(GameUi())
 val ui = mutable.asStateFlow()
 val levels get() = repository.levels
 val knowledge get() = repository.knowledge
 val settings = repository.settings.stateIn(viewModelScope, SharingStarted.Eagerly, Settings())
 private val writes = Channel<Save>(Channel.UNLIMITED)
 init {
  viewModelScope.launch { for(save in writes) try { repository.save(save) } catch(e: Exception) { mutable.update { it.copy(error = "存档失败：${e.message}") } } }
  viewModelScope.launch { try { val saved = withContext(Dispatchers.IO) { repository.load().also { repository.levels } }; mutable.value = GameUi(loaded = true,save = saved) } catch(e: Exception) { mutable.value = GameUi(error = "读取存档或课程失败：${e.message}") } }
 }
 private fun commit(save: Save) { mutable.update { it.copy(save = save, error = null) }; writes.trySend(save) }
 fun start(id: Int) {
  val level = levels.find { it.id == id }
  if(level != null && (!level.implemented || !ui.value.save.progress.unlocked(level))) return
  val initial = level?.simulation?.initialState?.copy(seed = level.simulation.seed) ?: Cluster(nodes = listOf(Node("worker-1")))
  var state = initial
  repeat(3) { state = engine.reduce(state,GameAction.Tick) }
  commit(ui.value.save.copy(session = Session(levelId = id,cluster = state)))
  mutable.update { it.copy(result = null,output = "会话开始；输入 help 查看离线命令。") }
 }
 fun action(action: GameAction) {
  try {
   val old = ui.value.save
   val session = old.session
   val level = levels.find { it.id == session.levelId }
   require(level == null || action.javaClass.simpleName in level.allowedActions) { "此关不允许此操作" }
   val next = engine.reduce(session.cluster,action)
   commit(old.copy(session = session.copy(cluster = next, actions = session.actions + if(action == GameAction.Tick) 0 else 1)))
  } catch(e: Exception) { mutable.update { it.copy(output = it.output + "\n操作失败：${e.message}") } }
 }
 fun command(line: String) {
  val result = if(line.trim() == "help") CommandResult(Terminal.help) else Terminal.parse(line,ui.value.save.session.cluster)
  mutable.update { it.copy(output = (it.output + "\n❯ $line\n" + result.output).takeLast(16000)) }
  result.action?.let { action(it) }
 }
 fun yaml(text: String) { try { action(Manifest.parse(text)) } catch(e: Exception) { mutable.update { it.copy(output = it.output + "\nYAML：${e.message}") } } }
 fun hint() { commit(ui.value.save.let { it.copy(session = it.session.copy(hints = it.session.hints+1)) }) }
 fun answer(text: String) { val level = levels.find { it.id == ui.value.save.session.levelId } ?: return; understood(text.trim().replace(" ","").equals(level.knowledgeAnswer.replace(" ",""),ignoreCase = true)) }
 private fun understood(value: Boolean) { commit(ui.value.save.let { it.copy(session = it.session.copy(understood = value)) }) }
 fun claim() {
  val old = ui.value.save; val s = old.session; val l = levels.find { it.id == s.levelId } ?: return
  if(!l.completed(s.cluster) || s.claimed) return
  val score = l.points(s.cluster,s.actions,s.hints,s.understood)
  val p = old.progress
  commit(old.copy(session = s.copy(claimed = true),progress = p.copy(best = p.best + (l.id to maxOf(p.best[l.id] ?: 0,score)),completions = p.completions + (l.id to ((p.completions[l.id] ?: 0)+1)),xp = p.xp + l.rewards.xp, learned = p.learned + l.knowledgeIds,achievements = p.achievements + l.rewards.achievement)))
  mutable.update { it.copy(result = score) }
 }
 fun dismissResult() { mutable.update { it.copy(result = null) } }
 fun settings(s: Settings) { viewModelScope.launch { repository.settings(s) } }
}
