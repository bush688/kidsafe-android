package com.kidsafe.child

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun UsageChart(data: Map<String, Int>) {
    if (data.isEmpty()) return
    val top = data.entries.take(5)
    val max = (top.maxOfOrNull { it.value } ?: 1).coerceAtLeast(1)
    Column(modifier = Modifier.fillMaxWidth().padding(top = 16.dp)) {
        Text(text = "Top 应用使用时长", style = MaterialTheme.typography.headlineLarge)
        top.forEach { e ->
            Row(modifier = Modifier.fillMaxWidth().padding(top = 8.dp)) {
                Column(modifier = Modifier.fillMaxWidth()) {
                    Text(text = e.key + " - " + e.value + "分钟", style = MaterialTheme.typography.bodyLarge)
                    LinearProgressIndicator(progress = e.value.toFloat() / max.toFloat(), modifier = Modifier.fillMaxWidth().padding(top = 4.dp))
                }
            }
        }
    }
}