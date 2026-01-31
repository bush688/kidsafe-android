package com.kidsafe.probe

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.kidsafe.probe.core.ClipboardUtils
import com.kidsafe.probe.support.AssetTextRepository
import com.kidsafe.probe.support.MarkdownContent
import com.kidsafe.probe.support.SupportPrefsStore
import com.kidsafe.probe.support.UpdateRepository
import com.kidsafe.probe.ui.ProbeAppTheme
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class ThermoActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            ProbeAppTheme {
                val context = androidx.compose.ui.platform.LocalContext.current
                val appContext = remember(context) { context.applicationContext }
                val prefs = remember(appContext) { SupportPrefsStore(appContext) }
                val consent by prefs.privacyConsent.collectAsState(initial = null)
                val assets = remember(appContext) { AssetTextRepository(appContext) }
                val updateRepo = remember(appContext) { UpdateRepository(assets) }
                val accepted = consent?.accepted == true
                var privacyMd by remember { mutableStateOf<String?>(null) }
                var forceUpdateUrl by remember { mutableStateOf<String?>(null) }
                var optionalUpdateUrl by remember { mutableStateOf<String?>(null) }
                var optionalUpdateSummary by remember { mutableStateOf<String?>(null) }
                var showOptionalUpdate by remember { mutableStateOf(false) }
                val scope = androidx.compose.runtime.rememberCoroutineScope()
                var crashText by remember { mutableStateOf(CrashReporter.readCrash(context)) }

                LaunchedEffect(Unit) {
                    privacyMd = assets.loadLocalizedText("privacy.md")
                }

                LaunchedEffect(accepted) {
                    if (!accepted) return@LaunchedEffect
                    val cfg = updateRepo.loadConfig() ?: return@LaunchedEffect
                    if (BuildConfig.VERSION_CODE < cfg.minSupportedVersionCode) {
                        forceUpdateUrl = cfg.storeUrl
                        return@LaunchedEffect
                    }
                    if (BuildConfig.VERSION_CODE < cfg.latestVersionCode) {
                        optionalUpdateUrl = cfg.storeUrl
                        optionalUpdateSummary = cfg.summary
                        delay(600)
                        showOptionalUpdate = true
                    }
                }

                if (accepted && forceUpdateUrl == null) {
                    InstrumentCalculatorApp()
                }

                if (!accepted) {
                    AlertDialog(
                        onDismissRequest = {},
                        title = { Text(text = context.getString(R.string.privacy_policy)) },
                        text = {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .verticalScroll(rememberScrollState())
                                    .padding(vertical = 4.dp),
                            ) {
                                val md = privacyMd
                                if (md.isNullOrBlank()) {
                                    Text(text = context.getString(R.string.loading))
                                } else {
                                    MarkdownContent(markdown = md)
                                }
                                Text(
                                    text = context.getString(R.string.privacy_required_hint),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.padding(top = 8.dp)
                                )
                            }
                        },
                        confirmButton = {
                            TextButton(onClick = {
                                val policyId = "privacy_v1"
                                scope.launch { prefs.acceptPrivacy(policyId) }
                            }) {
                                Text(text = context.getString(R.string.agree))
                            }
                        },
                        dismissButton = {
                            TextButton(onClick = { finishAffinity() }) {
                                Text(text = context.getString(R.string.decline))
                            }
                        }
                    )
                }

                val forcedUrl = forceUpdateUrl
                if (accepted && forcedUrl != null) {
                    AlertDialog(
                        onDismissRequest = {},
                        title = { Text(text = context.getString(R.string.update_required_title)) },
                        text = { Text(text = context.getString(R.string.update_required_desc)) },
                        confirmButton = {
                            TextButton(onClick = { openUrl(forcedUrl) }) { Text(text = context.getString(R.string.open_store)) }
                        },
                        dismissButton = {
                            TextButton(onClick = { finishAffinity() }) { Text(text = context.getString(R.string.exit)) }
                        }
                    )
                }

                val optionalUrl = optionalUpdateUrl
                if (accepted && forcedUrl == null && optionalUrl != null && showOptionalUpdate) {
                    AlertDialog(
                        onDismissRequest = { showOptionalUpdate = false },
                        title = { Text(text = context.getString(R.string.update_available_title)) },
                        text = {
                            Column(verticalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(8.dp)) {
                                Text(text = optionalUpdateSummary ?: "")
                                Text(
                                    text = context.getString(R.string.update_optional_desc),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        },
                        confirmButton = {
                            TextButton(onClick = { openUrl(optionalUrl) }) { Text(text = context.getString(R.string.open_store)) }
                        },
                        dismissButton = {
                            TextButton(onClick = { showOptionalUpdate = false }) { Text(text = context.getString(R.string.later)) }
                        }
                    )
                }

                if (accepted && crashText != null) {
                    AlertDialog(
                        onDismissRequest = {},
                        title = { Text(text = "上次崩溃信息") },
                        text = {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .verticalScroll(rememberScrollState())
                                    .padding(vertical = 4.dp),
                            ) {
                                Text(text = crashText.orEmpty())
                            }
                        },
                        confirmButton = {
                            TextButton(onClick = { ClipboardUtils.copy(context, "crash", crashText.orEmpty()) }) {
                                Text(text = "复制")
                            }
                        },
                        dismissButton = {
                            TextButton(onClick = {
                                CrashReporter.clearCrash(context)
                                crashText = null
                            }) {
                                Text(text = "关闭")
                            }
                        }
                    )
                }
            }
        }
    }

    private fun openUrl(url: String) {
        runCatching {
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
            startActivity(intent)
        }
    }
}
