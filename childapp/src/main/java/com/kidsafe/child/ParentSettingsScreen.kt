package com.kidsafe.child

import androidx.biometric.BiometricManager
import androidx.biometric.BiometricPrompt
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.fragment.app.FragmentActivity

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
    Column(modifier = Modifier.fillMaxSize().padding(24.dp)) {
        Text(text = "家长控制中心", style = MaterialTheme.typography.headlineLarge)
        Button(onClick = onBack, modifier = Modifier.padding(top = 24.dp)) { Text("返回") }
    }
}