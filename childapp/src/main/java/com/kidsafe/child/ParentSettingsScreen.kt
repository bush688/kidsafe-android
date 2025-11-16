package com.kidsafe.child

import androidx.biometric.BiometricManager
import androidx.biometric.BiometricPrompt
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.fragment.app.FragmentActivity
import com.kidsafe.child.rules.ScreenTimeRule
import com.kidsafe.child.lock.LockConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

@Composable
fun ParentSettingsScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    val activity = context as FragmentActivity
    val authed = remember { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        val bm = BiometricManager.from(context)
        if (bm.canAuthenticate(BiometricManager.Authenticators.BIOMETRIC_STRONG or BiometricManager.Authenticators.DEVICE_CREDENTIAL) == BiometricManager.BIOMETRIC_SUCCESS) {
            val prompt = BiometricPrompt(activity, activity.mainExecutor, object : BiometricPrompt.AuthenticationCallback() {})
            val info = BiometricPrompt.PromptInfo.Builder().setTitle("家长身份验证").setAllowedAuthenticators(BiometricManager.Authenticators.BIOMETRIC_STRONG or BiometricManager.Authenticators.DEVICE_CREDENTIAL).build()
            prompt.authenticate(info)
            authed.value = true
        } else {
            authed.value = true
        }
    }
    if (!authed.value) return
    val daily = remember { mutableStateOf("60") }
    val cats = remember { mutableStateOf("") }
    val white = remember { mutableStateOf("") }
    val black = remember { mutableStateOf("") }
    LaunchedEffect(Unit) {
        val db = SecureDatabase.get(context)
        val rule = withContext(Dispatchers.IO) { db.screenTimeRuleDao().get() } ?: ScreenTimeRule()
        val cfg = withContext(Dispatchers.IO) { db.lockConfigDao().get() } ?: LockConfig()
        daily.value = rule.dailyLimitMinutes.toString()
        cats.value = cfg.allowedCategories
        white.value = cfg.whitelist
        black.value = cfg.blacklist
    }
    Column(modifier = Modifier.fillMaxSize().padding(24.dp)) {
        Text(text = "家长控制中心", style = MaterialTheme.typography.headlineLarge)
        OutlinedTextField(value = daily.value, onValueChange = { daily.value = it }, label = { Text("每日总时长(分钟)") }, modifier = Modifier.padding(top = 16.dp))
        OutlinedTextField(value = cats.value, onValueChange = { cats.value = it }, label = { Text("允许类别(逗号分隔)") }, modifier = Modifier.padding(top = 16.dp))
        OutlinedTextField(value = white.value, onValueChange = { white.value = it }, label = { Text("白名单包名(逗号分隔)") }, modifier = Modifier.padding(top = 16.dp))
        OutlinedTextField(value = black.value, onValueChange = { black.value = it }, label = { Text("黑名单包名(逗号分隔)") }, modifier = Modifier.padding(top = 16.dp))
        Button(onClick = {
            val db = SecureDatabase.get(context)
            val d = daily.value.toIntOrNull() ?: 60
            kotlinx.coroutines.GlobalScope.launch(Dispatchers.IO) {
                db.screenTimeRuleDao().upsert(ScreenTimeRule(dailyLimitMinutes = d))
                db.lockConfigDao().upsert(LockConfig(allowedCategories = cats.value, whitelist = white.value, blacklist = black.value))
            }
        }, modifier = Modifier.padding(top = 24.dp)) { Text("保存") }
        Button(onClick = onBack, modifier = Modifier.padding(top = 12.dp)) { Text("返回") }
        UsageReport()
    }
}

@Composable
fun UsageReport() {
    val context = LocalContext.current
    val list = remember { mutableStateOf<List<com.kidsafe.child.db.AppUsage>>(emptyList()) }
    LaunchedEffect(Unit) {
        val db = SecureDatabase.get(context)
        val l = withContext(Dispatchers.IO) { db.appUsageDao().latest(100) }
        list.value = l
    }
    Column(modifier = Modifier.padding(top = 24.dp)) {
        Text(text = "使用报告", style = MaterialTheme.typography.headlineLarge)
        list.value.forEach { e ->
            Text(text = e.packageName + " " + java.text.SimpleDateFormat("HH:mm:ss").format(java.util.Date(e.time)), modifier = Modifier.padding(top = 8.dp))
        }
    }
}