package com.seki999.kubecraft.ui

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.*
import androidx.compose.ui.text.*
import com.seki999.kubecraft.data.Settings
import com.seki999.kubecraft.engine.Cluster

@OptIn(ExperimentalTextApi::class)
@Composable fun CityCanvas(s: Cluster,settings: Settings,modifier: Modifier = Modifier) {
 val text = rememberTextMeasurer()
 val count by animateFloatAsState(s.pods.size.toFloat(),animationSpec = tween(if(!settings.animations) 0 else if(settings.slow) 1500 else 600),label = "pod creation")
 val pulse by animateFloatAsState(s.tick.toFloat(),animationSpec = tween(if(!settings.animations) 0 else if(settings.slow) 1800 else 900),label = "traffic")
 val primary = MaterialTheme.colorScheme.primary
 Column(modifier.padding(12.dp)) {
  Text("CLUSTER CITY",style = MaterialTheme.typography.labelLarge,color = primary)
  Text("${s.nodes.size} Worker · ${s.deployments.size} Deployment · ${s.services.size} Service",style = MaterialTheme.typography.bodySmall)
  BoxWithConstraints(Modifier.fillMaxWidth().weight(1f)) {
  val viewportHeight = maxHeight
  Box(Modifier.verticalScroll(rememberScrollState())) {
  Canvas(Modifier.fillMaxWidth().height(maxOf(viewportHeight,(((s.nodes.size+1)/2).coerceAtLeast(1)*190).dp)).semantics { contentDescription = "集群城市：${s.nodes.size} 节点，${s.pods.count { it.ready }} Ready Pod" }) {
   val cols = if(size.width > 800.dp.toPx()) 3 else 2
   val rows = ((s.nodes.size+cols-1)/cols).coerceAtLeast(1)
   val cellW = size.width/cols; val cellH = (size.height/(rows+0.4f)).coerceAtMost(220.dp.toPx())
   fun label(t: String,x: Float,y: Float,color: Color = Color(0xFFC7DAEF)) { drawText(text.measure(t,style = TextStyle(color = color,fontSize = 11.sp)),topLeft = Offset(x,y)) }
   for(i in -8..14) { val y = i*50f; drawLine(Color(0x1825BDAE),Offset(0f,y),Offset(size.width,y+size.width*0.35f)); drawLine(Color(0x1825BDAE),Offset(size.width,y),Offset(0f,y+size.width*0.35f)) }
   s.nodes.forEachIndexed { index,n ->
    val x = (index%cols)*cellW + cellW/2; val y = (index/cols)*cellH+cellH/2+20
    val w = cellW*0.42f; val h = cellH*0.24f
    val path = Path().apply { moveTo(x,y-h); lineTo(x+w,y); lineTo(x,y+h); lineTo(x-w,y); close() }
    val color = if(n.ready) Color(0xFF24567C) else Color(0xFF922D46)
    drawPath(path,color); drawPath(path,if(n.ready) primary else Color.Red,style = Stroke(2f))
    drawLine(color,Offset(x-w,y),Offset(x-w,y+25),8f); drawLine(color,Offset(x+w,y),Offset(x+w,y+25),8f)
    label(n.name,x-w,y+h+30,if(n.ready) primary else Color(0xFFFF7D8D))
    val pods = s.pods.filter { it.node == n.name }
    pods.forEachIndexed { pi,p ->
     val px = x-w*0.65f+(pi%4)*w*0.4f; val py = y-15-(pi/4)*28
     val latest = s.deployments.find { it.name == p.owner }?.revision ?: 1
     val c = if(p.ready && latest > 1 && p.revision == latest) Color(0xFF8DB6FF) else if(p.ready) Color(0xFF68E7C4) else if(p.phase == "Unknown") Color(0xFFFF6F86) else Color(0xFFFFD076)
     val radius = (6.dp.toPx() + if(count < s.pods.size && p == s.pods.lastOrNull()) (count%1)*5 else 0f)
     drawRoundRect(c,Offset(px,py),Size(radius*1.5f,radius*1.5f),cornerRadius = androidx.compose.ui.geometry.CornerRadius(3f))
     if(p.ready && s.services.any { p.name in it.endpoints }) { drawLine(primary.copy(alpha = 0.3f),Offset(size.width/2,4f),Offset(px,py),2f); val f = pulse%1; drawCircle(primary,4f,Offset(size.width/2+(px-size.width/2)*f,4+(py-4)*f)) }
    }
    label("${pods.count { it.ready }}/${pods.size} Ready",x-w,y+h+50)
   }
   val pending = s.pods.count { it.node == null }
   if(s.nodes.isEmpty()) label("尚无 Worker · 在终端建设第一座节点",20f,size.height/2)
   if(pending>0) label("Pending $pending · 查看 Events 查找原因",8f,size.height-35,Color(0xFFFFD076))
  }
  }
  }
  Text("■ Ready   ■ Starting / Pending   ■ Node fault",style = MaterialTheme.typography.labelSmall)
 }
}
