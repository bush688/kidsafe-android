package com.kidsafe.parent

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.Button
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import android.content.Intent
import java.time.LocalTime
import java.time.format.DateTimeFormatter

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { ParentTheme { Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) { ParentHome() } } }
    }
}

@Composable
fun ParentHome() {
    val daily = remember { mutableStateOf("60") }
    val cats = remember { mutableStateOf("") }
    val white = remember { mutableStateOf("") }
    val black = remember { mutableStateOf("") }
    val start = remember { mutableStateOf("08:00") }
    val end = remember { mutableStateOf("20:00") }
    Column(modifier = Modifier.fillMaxSize().padding(24.dp)) {
        Text(text = "家长控制端", style = MaterialTheme.typography.headlineLarge)
        OutlinedTextField(value = daily.value, onValueChange = { daily.value = it }, label = { Text("每日总时长(分钟)") }, modifier = Modifier.padding(top = 16.dp))
        OutlinedTextField(value = cats.value, onValueChange = { cats.value = it }, label = { Text("允许类别(逗号分隔)") }, modifier = Modifier.padding(top = 16.dp))
        OutlinedTextField(value = white.value, onValueChange = { white.value = it }, label = { Text("白名单包名(逗号分隔)") }, modifier = Modifier.padding(top = 16.dp))
        OutlinedTextField(value = black.value, onValueChange = { black.value = it }, label = { Text("黑名单包名(逗号分隔)") }, modifier = Modifier.padding(top = 16.dp))
        OutlinedTextField(value = start.value, onValueChange = { start.value = it }, label = { Text("可用开始时间(HH:mm)") }, modifier = Modifier.padding(top = 16.dp))
        OutlinedTextField(value = end.value, onValueChange = { end.value = it }, label = { Text("可用结束时间(HH:mm)") }, modifier = Modifier.padding(top = 16.dp))
        Button(onClick = {
            val i = Intent("com.kidsafe.action.UPDATE_CONFIG")
            i.setPackage("com.kidsafe.child")
            val d = daily.value.toIntOrNull() ?: 60
            i.putExtra("daily", d)
            i.putExtra("cats", cats.value)
            i.putExtra("white", white.value)
            i.putExtra("black", black.value)
            val fmt = DateTimeFormatter.ofPattern("HH:mm")
            val s = runCatching { LocalTime.parse(start.value, fmt) }.getOrNull() ?: LocalTime.of(8, 0)
            val e = runCatching { LocalTime.parse(end.value, fmt) }.getOrNull() ?: LocalTime.of(20, 0)
            i.putExtra("startMin", s.hour * 60 + s.minute)
            i.putExtra("endMin", e.hour * 60 + e.minute)
            getApplicationContext().sendBroadcast(i, "com.kidsafe.permission.MANAGE_CHILD")
        }, modifier = Modifier.padding(top = 24.dp)) { Text("同步到儿童端") }
    }
}

@Composable
fun ParentTheme(content: @Composable () -> Unit) {
    MaterialTheme(content = content)
}