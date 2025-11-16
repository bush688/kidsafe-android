package com.kidsafe.child

import android.content.Intent
import android.net.Uri
import android.provider.Settings
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp

@Composable
fun PermissionGuides() {
    val context = LocalContext.current
    val usageOk = remember { mutableStateOf(UsageMonitor.hasAccess(context)) }
    val overlayOk = remember { mutableStateOf(Settings.canDrawOverlays(context)) }
    Column(modifier = Modifier.padding(12.dp)) {
        if (!usageOk.value) {
            Button(onClick = { context.startActivity(Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS)) }, modifier = Modifier.fillMaxWidth().padding(8.dp)) { Text("开启使用情况访问") }
        }
        if (!overlayOk.value) {
            val pkg = context.packageName
            Button(onClick = { context.startActivity(Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, Uri.parse("package:$pkg"))) }, modifier = Modifier.fillMaxWidth().padding(8.dp)) { Text("允许显示在其他应用上层") }
        }
    }
}