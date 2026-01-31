package com.kidsafe.probe.modules.impl

import android.content.Intent
import android.net.Uri
import android.util.Log
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.HelpOutline
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Policy
import androidx.compose.material.icons.filled.Update
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.kidsafe.probe.BuildConfig
import com.kidsafe.probe.R
import com.kidsafe.probe.modules.FeatureModule
import com.kidsafe.probe.modules.ModuleDescriptor
import com.kidsafe.probe.modules.ModuleHost
import com.kidsafe.probe.support.HelpViewModel
import com.kidsafe.probe.support.MarkdownContent
import com.kidsafe.probe.support.PrivacyViewModel
import com.kidsafe.probe.support.ReleaseNotesViewModel
import com.kidsafe.probe.support.UpdateViewModel
import com.kidsafe.probe.ui.SecondaryButton
import kotlinx.coroutines.launch

private enum class SupportTab { PRIVACY, RELEASE, HELP, FEEDBACK }

object SupportModule : FeatureModule {
    override val descriptor = ModuleDescriptor(
        id = "support",
        titleRes = R.string.feature_support,
        icon = Icons.AutoMirrored.Filled.HelpOutline,
    )

    override val useHostScroll: Boolean = false

    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    override fun Content(host: ModuleHost, state: Any?) {
        val context = host.context
        val appContext = remember(context) { context.applicationContext }
        val scope = rememberCoroutineScope()

        var tab by remember { mutableStateOf(SupportTab.PRIVACY) }

        val privacyVm: PrivacyViewModel = viewModel()
        val privacyState by privacyVm.state.collectAsState()

        val releaseVm: ReleaseNotesViewModel = viewModel()
        val releaseState by releaseVm.state.collectAsState()

        val helpVm: HelpViewModel = viewModel()
        val helpState by helpVm.state.collectAsState()

        val updateVm: UpdateViewModel = viewModel()
        val updateState by updateVm.state.collectAsState()

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            TabRow(selectedTabIndex = tab.ordinal) {
                Tab(
                    selected = tab == SupportTab.PRIVACY,
                    onClick = { tab = SupportTab.PRIVACY },
                    text = { Text(text = context.getString(R.string.support_tab_privacy)) },
                    icon = { Icon(Icons.Default.Policy, contentDescription = null) }
                )
                Tab(
                    selected = tab == SupportTab.RELEASE,
                    onClick = { tab = SupportTab.RELEASE },
                    text = { Text(text = context.getString(R.string.support_tab_release)) },
                    icon = { Icon(Icons.Default.Update, contentDescription = null) }
                )
                Tab(
                    selected = tab == SupportTab.HELP,
                    onClick = { tab = SupportTab.HELP },
                    text = { Text(text = context.getString(R.string.support_tab_help)) },
                    icon = { Icon(Icons.AutoMirrored.Filled.HelpOutline, contentDescription = null) }
                )
                Tab(
                    selected = tab == SupportTab.FEEDBACK,
                    onClick = { tab = SupportTab.FEEDBACK },
                    text = { Text(text = context.getString(R.string.support_tab_feedback)) },
                    icon = { Icon(Icons.Default.Email, contentDescription = null) }
                )
            }

            when (tab) {
                SupportTab.PRIVACY -> {
                    Card(modifier = Modifier.fillMaxWidth()) {
                        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                            Text(text = context.getString(R.string.privacy_policy), style = MaterialTheme.typography.titleMedium)
                            val consent = privacyState.consent
                            Text(
                                text = if (consent?.accepted == true) {
                                    context.getString(R.string.privacy_status_accepted, consent.acceptedAtIso ?: "—")
                                } else {
                                    context.getString(R.string.privacy_status_not_accepted)
                                },
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                                SecondaryButton(
                                    title = context.getString(R.string.withdraw_consent),
                                    onClick = {
                                        privacyVm.withdraw()
                                        scope.launch { host.showMessage(context.getString(R.string.withdrawn)) }
                                    },
                                    modifier = Modifier.weight(1f)
                                )
                            }
                            MarkdownContent(markdown = privacyState.markdown)
                        }
                    }
                }

                SupportTab.RELEASE -> {
                    val selected = releaseState.selected
                    Card(modifier = Modifier.fillMaxWidth()) {
                        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(text = context.getString(R.string.release_notes), style = MaterialTheme.typography.titleMedium)
                                if (selected != null) {
                                    SecondaryButton(title = context.getString(R.string.back), onClick = { releaseVm.select(null) })
                                }
                            }

                            val cfg = updateState.config
                            if (cfg != null) {
                                Text(
                                    text = context.getString(
                                        R.string.update_status,
                                        BuildConfig.VERSION_CODE,
                                        cfg.latestVersionCode,
                                        cfg.minSupportedVersionCode
                                    ),
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                if (cfg.latestVersionCode > BuildConfig.VERSION_CODE && cfg.summary.isNotBlank()) {
                                    Text(text = cfg.summary)
                                    SecondaryButton(
                                        title = context.getString(R.string.open_store),
                                        onClick = { openUrl(appContext, cfg.storeUrl) },
                                        modifier = Modifier.fillMaxWidth()
                                    )
                                }
                            }

                            if (selected == null) {
                                releaseState.notes.forEach { note ->
                                    Card(modifier = Modifier.fillMaxWidth()) {
                                        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                            Text(text = "${note.versionName}  ${note.title}", style = MaterialTheme.typography.titleSmall)
                                            Text(text = note.summary, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                            SecondaryButton(
                                                title = context.getString(R.string.view_details),
                                                onClick = { releaseVm.select(note) },
                                                modifier = Modifier.fillMaxWidth()
                                            )
                                        }
                                    }
                                }
                            } else {
                                Text(
                                    text = "${selected.versionName}  ${selected.date}",
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                MarkdownContent(markdown = selected.bodyMd)
                            }
                        }
                    }
                }

                SupportTab.HELP -> {
                    Card(modifier = Modifier.fillMaxWidth()) {
                        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(text = context.getString(R.string.help_center), style = MaterialTheme.typography.titleMedium)
                                if (helpState.selectedDoc != null) {
                                    SecondaryButton(title = context.getString(R.string.back), onClick = { helpVm.openDoc(null) })
                                }
                            }

                            if (helpState.selectedDoc == null) {
                                OutlinedTextField(
                                    value = helpState.query,
                                    onValueChange = { helpVm.setQuery(it) },
                                    modifier = Modifier.fillMaxWidth(),
                                    label = { Text(text = context.getString(R.string.search)) },
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Text),
                                    supportingText = { Text(text = context.getString(R.string.hint_help_search)) },
                                )

                                val results = helpState.searchResults
                                if (results.isNotEmpty()) {
                                    Text(text = context.getString(R.string.search_results), style = MaterialTheme.typography.titleSmall)
                                    results.forEach { doc ->
                                        SecondaryButton(
                                            title = doc.title,
                                            onClick = { helpVm.openDoc(doc) },
                                            modifier = Modifier.fillMaxWidth(),
                                        )
                                    }
                                } else {
                                    helpState.categories.forEach { cat ->
                                        Text(text = cat.title, style = MaterialTheme.typography.titleSmall)
                                        cat.items.forEach { doc ->
                                            SecondaryButton(
                                                title = doc.title,
                                                onClick = { helpVm.openDoc(doc) },
                                                modifier = Modifier.fillMaxWidth(),
                                            )
                                        }
                                    }
                                }
                            } else {
                                Text(text = helpState.selectedDoc?.title.orEmpty(), style = MaterialTheme.typography.titleSmall)
                                val md = helpState.selectedDocMarkdown
                                if (md != null) {
                                    MarkdownContent(markdown = md)
                                }
                            }
                        }
                    }
                }

                SupportTab.FEEDBACK -> {
                    Card(modifier = Modifier.fillMaxWidth()) {
                        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                            Text(text = context.getString(R.string.feedback), style = MaterialTheme.typography.titleMedium)
                            Text(
                                text = context.getString(R.string.feedback_hint, BuildConfig.VERSION_NAME, BuildConfig.VERSION_CODE),
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            SecondaryButton(
                                title = context.getString(R.string.send_email),
                                onClick = { sendEmail(appContext, "support@example.com", context.getString(R.string.feedback_email_subject, BuildConfig.VERSION_NAME)) },
                                modifier = Modifier.fillMaxWidth(),
                            )
                            SecondaryButton(
                                title = context.getString(R.string.copy_version_info),
                                onClick = {
                                    host.copyToClipboard("version", "versionName=${BuildConfig.VERSION_NAME}\nversionCode=${BuildConfig.VERSION_CODE}")
                                    scope.launch { host.showMessage(context.getString(R.string.copied)) }
                                },
                                modifier = Modifier.fillMaxWidth(),
                            )
                        }
                    }
                }
            }
        }
    }

    private fun openUrl(context: android.content.Context, url: String) {
        runCatching {
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url)).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            context.startActivity(intent)
        }.onFailure { e ->
            Log.e("SupportModule", "openUrl failed", e)
        }
    }

    private fun sendEmail(context: android.content.Context, to: String, subject: String) {
        runCatching {
            val uri = Uri.parse("mailto:$to")
            val intent = Intent(Intent.ACTION_SENDTO, uri)
                .putExtra(Intent.EXTRA_SUBJECT, subject)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            context.startActivity(intent)
        }.onFailure { e ->
            Log.e("SupportModule", "sendEmail failed", e)
        }
    }
}

