package com.kidsafe.child

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.icons.Icons
import androidx.compose.material3.icons.filled.School
import androidx.compose.material3.icons.filled.SportsEsports
import androidx.compose.material3.icons.filled.Videocam
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

data class KidItem(val title: String, val icon: androidx.compose.ui.graphics.vector.ImageVector)

@Composable
fun KidsHomeGrid(onAction: (String) -> Unit, speak: (String) -> Unit) {
    val items = listOf(
        KidItem("学习", Icons.Default.School),
        KidItem("游戏", Icons.Default.SportsEsports),
        KidItem("视频", Icons.Default.Videocam)
    )
    LazyVerticalGrid(columns = GridCells.Fixed(2), modifier = Modifier.fillMaxWidth().padding(top = 12.dp), verticalArrangement = Arrangement.spacedBy(12.dp), horizontalArrangement = Arrangement.spacedBy(12.dp), contentPadding = PaddingValues(4.dp)) {
        items(items) { it -> KidTile(it, onAction, speak) }
    }
}

@Composable
fun KidTile(item: KidItem, onAction: (String) -> Unit, speak: (String) -> Unit) {
    val pressed = remember { mutableStateOf(false) }
    val scale = animateFloatAsState(if (pressed.value) 1.06f else 1f, label = "scale")
    Card(onClick = { onAction(item.title) }, modifier = Modifier.fillMaxWidth().aspectRatio(1f).scale(scale.value).pointerInput(Unit) { detectTapGestures(onPress = { pressed.value = true; tryAwaitRelease(); pressed.value = false }, onLongPress = { speak(item.title) }) }, colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer)) {
        Box(modifier = Modifier.fillMaxWidth().background(MaterialTheme.colorScheme.secondaryContainer), contentAlignment = Alignment.Center) {
            Icon(item.icon, contentDescription = null, modifier = Modifier.padding(12.dp).scale(1.4f))
        }
        Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
            Text(text = item.title, fontSize = 24.sp, modifier = Modifier.padding(8.dp))
        }
    }
}