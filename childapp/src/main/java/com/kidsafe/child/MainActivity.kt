package com.kidsafe.child

import android.os.Bundle
import android.speech.tts.TextToSpeech
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.biometric.BiometricManager
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.icons.Icons
import androidx.compose.material3.icons.filled.Lock
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import java.util.Locale

class MainActivity : ComponentActivity() {
    private var tts: TextToSpeech? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        tts = TextToSpeech(this) { tts?.language = Locale.getDefault() }
        setContent {
            KidSafeTheme {
                Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
                    val navController = rememberNavController()
                    NavHost(navController = navController, startDestination = "home") {
                        composable("home") { HomeScreen(onParent = { navController.navigate("parent") }, speak = { s -> tts?.speak(s, TextToSpeech.QUEUE_FLUSH, null, "kidsafe") }) }
                        composable("parent") { ParentSettingsScreen(onBack = { navController.popBackStack() }) }
                    }
                }
            }
        }
    }

    override fun onDestroy() {
        tts?.stop()
        tts?.shutdown()
        super.onDestroy()
    }
}

@Composable
fun HomeScreen(onParent: () -> Unit, speak: (String) -> Unit) {
    val visible = remember { mutableStateOf(true) }
    LaunchedEffect(Unit) { speak("点击家长按钮进入设置") }
    Column(modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.primaryContainer).padding(24.dp), verticalArrangement = Arrangement.Center, horizontalAlignment = Alignment.CenterHorizontally) {
        AnimatedVisibility(visible = visible.value) {
            Button(onClick = onParent, modifier = Modifier.fillMaxWidth().padding(8.dp), contentPadding = PaddingValues(24.dp)) {
                Icon(Icons.Default.Lock, contentDescription = null, modifier = Modifier.size(48.dp))
                Text(text = "家长", fontSize = 28.sp, modifier = Modifier.padding(start = 16.dp))
            }
        }
    }
}