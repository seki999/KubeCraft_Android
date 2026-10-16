package com.seki999.kubecraft.ui

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.seki999.kubecraft.engine.Cluster

/** Accessible topology projection; ownership and routing come only from the snapshot. */
@Composable fun Topology(s: Cluster) {
 Text("Kubernetes 拓扑",style = MaterialTheme.typography.headlineSmall)
 Text("实线关系：控制器归属 · 箭头关系：当前 Ready 端点")
 for(d in s.deployments) {
  Card(Modifier.fillMaxWidth()) { Column(Modifier.padding(12.dp),verticalArrangement = Arrangement.spacedBy(8.dp)) {
   Text("Deployment/${d.name}  ·  desired=${d.replicas}",style = MaterialTheme.typography.titleMedium)
   s.replicaSets.filter { it.deployment == d.name }.forEach { rs ->
    Text("└ ReplicaSet/${rs.name}  ·  revision=${rs.revision}")
    s.pods.filter { it.owner == d.name && it.revision == rs.revision }.forEach { p -> Text("    └ ${p.name}\n       ${p.phase} → Node/${p.node ?: "Pending"}",color = if(p.ready) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface) }
   }
  } }
 }
 s.advanced.statefulSets.forEach { set -> Text("StatefulSet/${set.name} → "+s.pods.filter { it.owner == "stateful:${set.name}" }.joinToString { "${it.name} (${it.phase})" }) }
 s.pods.filter { it.owner == null }.forEach { Text("Pod/${it.name} (${it.phase}) → Node/${it.node ?: "Pending"}") }
 s.services.forEach { service -> Card { Column(Modifier.fillMaxWidth().padding(12.dp)) { Text("Service/${service.namespace}/${service.name} · ${service.type}",style = MaterialTheme.typography.titleMedium); Text("selector=${service.selector}"); Text("→ ${service.endpoints.joinToString().ifEmpty { "无 Ready 端点" }}") } } }
 s.advanced.ingresses.forEach { Text("Ingress/${it.host} → Service/${it.namespace}/${it.service}") }
 s.claims.forEach { Text("PVC/${it.name} → PV/${it.volume ?: "等待绑定"}") }
 s.pods.filter { it.claim != null || it.config != null || it.secret != null }.forEach { Text("Pod/${it.name} → PVC/${it.claim ?: "—"} · ConfigMap/${it.config ?: "—"} · Secret/${it.secret ?: "—"}") }
}
