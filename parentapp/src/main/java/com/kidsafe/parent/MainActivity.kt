package com.kidsafe.parent

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { ParentTheme { Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) { ParentHome() } } }
    }
}

@Composable
fun ParentHome() {
    Column(modifier = Modifier.fillMaxSize().padding(24.dp)) {
        Text(text = "家长控制端", style = MaterialTheme.typography.headlineLarge)
    }
}

@Composable
fun ParentTheme(content: @Composable () -> Unit) {
    MaterialTheme(content = content)
}