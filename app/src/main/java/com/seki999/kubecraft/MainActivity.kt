package com.seki999.kubecraft

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.compose.BackHandler
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.foundation.*
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.Alignment
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.*
import com.seki999.kubecraft.ui.*
import com.seki999.kubecraft.engine.*

class MainActivity: ComponentActivity() {
 override fun onCreate(savedInstanceState: Bundle?) { super.onCreate(savedInstanceState); setContent { KubeCraft() } }
}
@OptIn(ExperimentalMaterial3Api::class)
@Composable fun KubeCraft(vm: GameViewModel = viewModel()) {
 val ui by vm.ui.collectAsStateWithLifecycle()
 val settings by vm.settings.collectAsStateWithLifecycle()
 val density = LocalDensity.current
 val animatedXp by androidx.compose.animation.core.animateIntAsState(ui.save.progress.xp,animationSpec = androidx.compose.animation.core.tween(if(settings.animations) (if(settings.slow) 1600 else 800) else 0),label = "经验奖励")
 CompositionLocalProvider(LocalDensity provides Density(density.density,density.fontScale*settings.fontScale)) {
 MaterialTheme(colorScheme = if(settings.dark) darkColorScheme(primary = Color(0xFF65E4C5),secondary = Color(0xFF8DB6FF),background = Color(0xFF081422),surface = Color(0xFF122337)) else lightColorScheme()) {
 var page by rememberSaveable { mutableStateOf("首页") }
 var panel by rememberSaveable { mutableStateOf("任务") }
 var sheet by remember { mutableStateOf(false) }
 BackHandler(page != "首页") { page = "首页" }
 Scaffold(topBar = { TopAppBar(title = { Column { Text("KUBECRAFT",style = MaterialTheme.typography.titleLarge); Text("Kubernetes 云端帝国 · 离线实验室",style = MaterialTheme.typography.labelSmall) } },navigationIcon = { if(page != "首页") TextButton(onClick = { page = "首页" }) { Text("返回") } }) }, bottomBar = { if(page != "城市") NavigationBar { listOf("首页","课程","图鉴","设置").forEach { name -> NavigationBarItem(selected = page == name,onClick = { page = name },icon = { Text(when(name) { "首页" -> "⌂"; "课程" -> "◈"; "图鉴" -> "▤"; else -> "⚙" }) },label = { Text(name) }) } } }) { padding ->
 Surface(Modifier.fillMaxSize().padding(padding)) {
 SelectionContainer {
 if(!ui.loaded) Box(Modifier.fillMaxSize(),contentAlignment = Alignment.Center) { Text(ui.error ?: "正在读取离线课程…") }
 else when(page) {
 "首页" -> Column(Modifier.verticalScroll(rememberScrollState()).padding(24.dp),verticalArrangement = Arrangement.spacedBy(18.dp)) {
  Text("把集群，建成一座城。",style = MaterialTheme.typography.headlineLarge)
  Text("用真实状态变化学习调度、副本与自愈。每一次操作，都能在城市里看见。",color = MaterialTheme.colorScheme.onSurfaceVariant)
  Card { Column(Modifier.padding(20.dp),verticalArrangement = Arrangement.spacedBy(10.dp)) { Text("指挥官档案",style = MaterialTheme.typography.titleMedium); Text("${animatedXp} XP  ·  ${ui.save.progress.best.size} / 50 关"); LinearProgressIndicator(progress = { ui.save.progress.best.size/50f },modifier = Modifier.fillMaxWidth()); Text("50 关离线课程 · 高级功能使用受控教学子集。") } }
  Button(onClick = { page = "课程" },modifier = Modifier.fillMaxWidth()) { Text("开始剧情课程") }
  OutlinedButton(onClick = { vm.start(0); page = "城市" },modifier = Modifier.fillMaxWidth()) { Text("自由建设 / 终端训练") }
  if(ui.save.session.cluster.nodes.isNotEmpty() || ui.save.session.levelId > 0) OutlinedButton(onClick = { page = "城市" },modifier = Modifier.fillMaxWidth()) { Text("继续保存的城市") }
  Text("Maya · 技术导师     Leo · 业务伙伴\nK8-Bot · 运维机器人",style = MaterialTheme.typography.bodyMedium)
 }
 "课程" -> LazyColumn(contentPadding = PaddingValues(16.dp),verticalArrangement = Arrangement.spacedBy(10.dp)) {
  items(vm.levels,key = { it.id }) { level ->
   if((level.id-1)%5 == 0) Text("第 ${level.chapterId} 章",style = MaterialTheme.typography.headlineSmall,modifier = Modifier.padding(vertical = 10.dp))
   val unlocked = ui.save.progress.unlocked(level)
   Card(onClick = { if(unlocked && level.implemented) { vm.start(level.id); panel = "任务"; page = "城市" } },enabled = unlocked && level.implemented,modifier = Modifier.fillMaxWidth()) { Column(Modifier.padding(16.dp)) { Text("%02d  %s".format(level.id,level.title),style = MaterialTheme.typography.titleMedium); Text(if(!level.implemented) "尚未开发 · 元数据已收录" else if(!unlocked) "完成前一关解锁" else "最高 ${ui.save.progress.best[level.id] ?: 0} 分 · 点击进入"); Text(level.description,style = MaterialTheme.typography.bodySmall) } }
  }
 }
 "图鉴" -> LazyColumn(contentPadding = PaddingValues(16.dp),verticalArrangement = Arrangement.spacedBy(10.dp)) { items(vm.knowledge) { k -> Card { Column(Modifier.fillMaxWidth().padding(16.dp)) { Text(k.term,style = MaterialTheme.typography.titleMedium); Text(k.text); if(k.id in ui.save.progress.learned) Text("✓ 已学习",color = MaterialTheme.colorScheme.primary) } } } }
 "设置" -> Column(Modifier.verticalScroll(rememberScrollState()).padding(24.dp),verticalArrangement = Arrangement.spacedBy(20.dp)) {
  Text("学习偏好",style = MaterialTheme.typography.headlineMedium)
  Toggle("深色主题",settings.dark) { vm.settings(settings.copy(dark = it)) }
  Toggle("状态动画",settings.animations) { vm.settings(settings.copy(animations = it)) }
  Toggle("慢速动画",settings.slow) { vm.settings(settings.copy(slow = it)) }
  Text("字体大小 ${"%.0f".format(settings.fontScale*100)}%")
  Slider(value = settings.fontScale,onValueChange = { vm.settings(settings.copy(fontScale = it)) },valueRange = 0.85f..1.4f,steps = 10)
  Text("版本 0.1.1 · Android 10+\n完全离线，无账号、无真实云资源费用。\n终端与 YAML 使用受控教学子集；Helm、Terraform、OCI、OKE 为离线教学模型。")
  ui.error?.let { Text(it,color = MaterialTheme.colorScheme.error) }
 }
 "城市" -> BoxWithConstraints(Modifier.fillMaxSize()) {
  val wide = maxWidth >= 840.dp
  if(wide) Row { CityCanvas(ui.save.session.cluster,settings,Modifier.weight(1f).fillMaxHeight()); GamePanel(vm,panel,{ panel = it },Modifier.width(380.dp).fillMaxHeight()) }
  else Column {
   CityCanvas(ui.save.session.cluster,settings,Modifier.weight(1f).fillMaxWidth())
   Row(Modifier.horizontalScroll(rememberScrollState()).padding(8.dp),horizontalArrangement = Arrangement.spacedBy(8.dp)) { listOf("任务","终端","YAML","拓扑","状态").forEach { p -> FilledTonalButton(onClick = { panel = p; sheet = true }) { Text(p) } } }
   Text("Tick ${ui.save.session.cluster.tick} · ${ui.save.session.cluster.pods.count { it.ready }} Ready / ${ui.save.session.cluster.pods.size} Pods",modifier = Modifier.padding(horizontal = 16.dp))
   Button(onClick = { vm.action(GameAction.Tick) },modifier = Modifier.padding(8.dp).fillMaxWidth()) { Text("推进 1 tick") }
  }
  if(sheet && !wide) ModalBottomSheet(onDismissRequest = { sheet = false },modifier = Modifier.fillMaxHeight(0.88f)) { GamePanel(vm,panel,{ panel = it },Modifier.fillMaxSize()) }
 }
 }
 }
 }
 }
 }
 }
}
@Composable fun Toggle(label: String,value: Boolean,change: (Boolean)->Unit) { Row(Modifier.fillMaxWidth(),verticalAlignment = Alignment.CenterVertically) { Text(label,Modifier.weight(1f)); Switch(value,change) } }

@Composable fun GamePanel(vm: GameViewModel,tab: String,onTab: (String)->Unit,modifier: Modifier = Modifier) {
 val preferences by vm.settings.collectAsStateWithLifecycle()
 val ui by vm.ui.collectAsStateWithLifecycle(); val s = ui.save.session; val level = vm.levels.find { it.id == s.levelId }
 if(ui.result != null && s.claimed && level != null) {
  androidx.compose.ui.window.Dialog(onDismissRequest = { vm.dismissResult() }) {
   Surface(shape = RoundedCornerShape(24.dp)) {
    SelectionContainer {
    Column(Modifier.padding(24.dp).verticalScroll(rememberScrollState()),verticalArrangement = Arrangement.spacedBy(16.dp)) {
     Text("任务完成",style = MaterialTheme.typography.headlineMedium)
     val result = ui.result ?: 0
     var reveal by remember { mutableStateOf(false) }
     LaunchedEffect(result) { reveal = true }
     val displayed by androidx.compose.animation.core.animateIntAsState(if(reveal) result else 0,animationSpec = androidx.compose.animation.core.tween(if(preferences.animations) (if(preferences.slow) 1600 else 800) else 0),label = "结算")
     Text("★".repeat(if(result >= 90) 3 else if(result >= 75) 2 else 1),color = MaterialTheme.colorScheme.primary,style = MaterialTheme.typography.headlineLarge)
     Text("$displayed / 100 · +${level.rewards.xp} XP")
     Text(level.review)
     Text("本关最高 ${ui.save.progress.best[level.id]} 分 · 完成 ${ui.save.progress.completions[level.id]} 次")
     if(level.id < 50) Button(onClick = { vm.start(level.id+1) }) { Text("进入下一关") }
     OutlinedButton(onClick = { vm.dismissResult() }) { Text("返回城市") }
    }
    }
   }
  }
 }
 var answer by rememberSaveable(s.levelId) { mutableStateOf("") }
 var command by rememberSaveable { mutableStateOf("") }; var yaml by rememberSaveable { mutableStateOf(Manifest.example) }
 val panelScroll = rememberScrollState()
 LaunchedEffect(tab,ui.output) { if(tab == "终端") { withFrameNanos { }; panelScroll.scrollTo(panelScroll.maxValue) } }
 SelectionContainer {
 Column(modifier.imePadding().padding(horizontal = 12.dp)) {
  Row(Modifier.horizontalScroll(rememberScrollState())) { listOf("任务","终端","YAML","拓扑","状态").forEach { TextButton(onClick = { onTab(it) }) { Text(it) } } }
  Column(Modifier.weight(1f).verticalScroll(panelScroll),verticalArrangement = Arrangement.spacedBy(12.dp)) {
   when(tab) {
    "任务" -> {
     Text(level?.title ?: "自由建设",style = MaterialTheme.typography.headlineSmall)
     if(level != null) {
      level.dialogues.forEach { Text("${it.speaker}：${it.text}") }
      level.objectives.forEach { Text("${if(it.met(s.cluster)) "✓" else "○"} ${it.text}",color = if(it.met(s.cluster)) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface) }
      Text("打开终端输入命令；推进 tick 观察控制器与调度器。")
      OutlinedButton(onClick = { vm.hint() }) { Text("请求提示 (${s.hints})") }
      if(s.hints > 0) Text(level.hints.take(s.hints).joinToString("\n"))
      Text("知识复盘（10 分）· ${level.knowledgeQuestion}")
      OutlinedTextField(answer,{ answer = it },label = { Text("输入技术术语英文原文") },modifier = Modifier.fillMaxWidth())
      OutlinedButton(onClick = { vm.answer(answer) }) { Text(if(s.understood) "✓ 理解正确" else "验证答案") }
      Text(level.review)
      Button(onClick = { vm.claim() },enabled = level.completed(s.cluster) && !s.claimed) { Text(if(s.claimed) "本次奖励已领取" else "验证任务并结算") }
      if(s.claimed) { Text("${"★".repeat(if((ui.result ?: ui.save.progress.best[level.id] ?: 0) >= 90) 3 else if((ui.result ?: ui.save.progress.best[level.id] ?: 0) >= 75) 2 else 1)} ${ui.result ?: ui.save.progress.best[level.id]} 分 · +${level.rewards.xp} XP",style = MaterialTheme.typography.headlineSmall,color = MaterialTheme.colorScheme.primary); if(level.id < 50) Button(onClick = { vm.start(level.id+1) }) { Text("下一关") } }
     } else { Text("建立 Worker、运行 Pod、部署应用或暴露 Service。与剧情使用同一模拟引擎。"); Button(onClick = { vm.action(GameAction.AddNode("worker-${s.cluster.nodes.size+1}")) }) { Text("建设 Worker") }; Text(Terminal.help) }
    }
    "终端" -> { Text("模拟 kubectl · 永不执行系统 shell",style = MaterialTheme.typography.labelLarge); Text(ui.output,fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace,style = MaterialTheme.typography.bodySmall) }
    "拓扑" -> { com.seki999.kubecraft.ui.Topology(s.cluster) }
    "YAML" -> { Text("Deployment 教学子集 · 只支持示例字段，不连接真实集群"); OutlinedTextField(yaml,{ yaml = it },modifier = Modifier.fillMaxWidth().heightIn(min = 280.dp),label = { Text("YAML 清单") }); Button(onClick = { vm.yaml(yaml) }) { Text("校验并创建 Deployment") }; Text(ui.output.takeLast(1000)) }
    "状态" -> { Text("集群对象",style = MaterialTheme.typography.titleLarge); s.cluster.nodes.forEach { n -> Text("${n.name} · Ready=${n.ready}\nCPU ${s.cluster.pods.filter { it.node == n.name }.sumOf { it.cpu }}/${n.cpu}m"); if(level == null) OutlinedButton(onClick = { vm.action(GameAction.SetNodeReady(n.name,!n.ready)) }) { Text(if(n.ready) "模拟节点故障" else "恢复节点") } }; s.cluster.pods.forEach { Text("${it.name}\n${it.phase} · ${it.image} · ${it.node ?: "未调度"}") }; s.cluster.services.forEach { Text("Service ${it.name} → ${it.endpoints.joinToString()}") }; Text("Events",style = MaterialTheme.typography.titleMedium); s.cluster.events.takeLast(15).reversed().forEach { Text("[${it.tick}] ${it.reason} ${it.message}",style = MaterialTheme.typography.bodySmall) } }
   }
  }
  ui.error?.let { Text(it,color = MaterialTheme.colorScheme.error) }
  if(tab == "终端") Row(Modifier.fillMaxWidth().padding(vertical = 8.dp),verticalAlignment = Alignment.CenterVertically) { OutlinedTextField(command,{ command = it },modifier = Modifier.weight(1f),label = { Text("kubectl / help") },singleLine = true); TextButton(onClick = { vm.command(command); command = "" }) { Text("执行") } }
  OutlinedButton(onClick = { vm.action(GameAction.Tick) },modifier = Modifier.fillMaxWidth()) { Text("推进 1 tick · t=${s.cluster.tick}") }
 }
 }
}
