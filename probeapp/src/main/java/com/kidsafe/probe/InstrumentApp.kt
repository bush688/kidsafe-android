package com.kidsafe.probe

import android.content.Context
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.HelpOutline
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.saveable.rememberSaveableStateHolder
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.kidsafe.probe.core.ClipboardUtils
import com.kidsafe.probe.modules.FeatureModule
import com.kidsafe.probe.modules.ModuleHost
import com.kidsafe.probe.modules.ModuleRegistry
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun InstrumentCalculatorApp() {
    val context = androidx.compose.ui.platform.LocalContext.current
    val appContext = remember(context) { context.applicationContext }
    val modules = remember(appContext) { ModuleRegistry.loadModules(appContext).distinctBy { it.descriptor.id } }

    var selectedModuleId by rememberSaveable { mutableStateOf<String?>(null) }
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    val stateHolder = rememberSaveableStateHolder()
    val moduleStates = remember { mutableStateMapOf<String, Any?>() }
    val moduleErrors = remember { mutableStateMapOf<String, String>() }

    var showHelp by rememberSaveable { mutableStateOf(false) }

    val selected = remember(selectedModuleId, modules) { modules.firstOrNull { it.descriptor.id == selectedModuleId } }

    val host = remember(appContext) {
        object : ModuleHost {
            override val context: Context = appContext

            override suspend fun showMessage(message: String) {
                snackbarHostState.showSnackbar(message)
            }

            override fun copyToClipboard(label: String, text: String) {
                ClipboardUtils.copy(appContext, label, text)
            }

            override fun requestBack() {
                selectedModuleId = null
            }
        }
    }

    BackHandler(enabled = selectedModuleId != null) {
        selectedModuleId = null
    }

    if (selected != null) {
        val stateKey = selected.descriptor.id
        if (!moduleStates.containsKey(stateKey)) {
            moduleStates[stateKey] = selected.createState()
        }
        val moduleState = moduleStates[stateKey]

        LaunchedEffect(stateKey) {
            runCatching { selected.onEnter(host, moduleState) }
                .onFailure { moduleErrors[stateKey] = it.stackTraceToString() }
        }
        DisposableEffect(stateKey) {
            onDispose {
                scope.launch {
                    runCatching { selected.onExit(host, moduleState) }
                        .onFailure { moduleErrors[stateKey] = it.stackTraceToString() }
                }
            }
        }
    }

    val title = selected?.let { context.getString(it.descriptor.titleRes) } ?: context.getString(R.string.app_name)

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(text = title) },
                navigationIcon = {
                    if (selectedModuleId != null) {
                        IconButton(onClick = { selectedModuleId = null }) {
                            Icon(Icons.Default.ArrowBack, contentDescription = context.getString(R.string.back))
                        }
                    }
                },
                actions = {
                    IconButton(onClick = { showHelp = true }) {
                        Icon(Icons.Default.HelpOutline, contentDescription = context.getString(R.string.help))
                    }
                }
            )
        },
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
    ) { innerPadding ->
        AnimatedContent(
            targetState = selectedModuleId,
            transitionSpec = { fadeIn(tween(200)) togetherWith fadeOut(tween(200)) },
            label = "home_to_module",
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
        ) { id ->
            if (id == null) {
                HomePanel(
                    modules = modules,
                    onOpen = { selectedModuleId = it },
                )
            } else {
                val module = modules.firstOrNull { it.descriptor.id == id }
                if (module == null) {
                    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
                        Text(text = context.getString(R.string.invalid_module))
                    }
                } else {
                    stateHolder.SaveableStateProvider(id) {
                        val errorText = moduleErrors[id]
                        val scrollModifier = when {
                            errorText != null -> Modifier.verticalScroll(rememberScrollState())
                            module.useHostScroll -> Modifier.verticalScroll(rememberScrollState())
                            else -> Modifier
                        }
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .then(scrollModifier)
                                .padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp),
                        ) {
                            if (errorText != null) {
                                ModuleErrorPanel(
                                    moduleId = id,
                                    errorText = errorText,
                                    onCopy = { host.copyToClipboard("crash", errorText) },
                                    onBack = { selectedModuleId = null },
                                )
                            } else {
                                module.Content(host, moduleStates[id])
                            }
                        }
                    }
                }
            }
        }
    }

    if (showHelp) {
        AlertDialog(
            onDismissRequest = { showHelp = false },
            title = { Text(text = context.getString(R.string.help_title)) },
            text = {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .verticalScroll(rememberScrollState())
                        .padding(vertical = 4.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(text = context.getString(R.string.help_text))
                    Text(text = context.getString(R.string.copyright_maker))
                    Text(text = context.getString(R.string.contact_email))
                }
            },
            confirmButton = {
                TextButton(onClick = { showHelp = false }) {
                    Text(text = context.getString(R.string.close))
                }
            }
        )
    }
}

@Composable
private fun HomePanel(
    modules: List<FeatureModule>,
    onOpen: (String) -> Unit,
) {
    val context = androidx.compose.ui.platform.LocalContext.current
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text(text = context.getString(R.string.home_title), style = MaterialTheme.typography.titleLarge)
        Text(text = context.getString(R.string.home_hint), color = MaterialTheme.colorScheme.onSurfaceVariant)

        LazyVerticalGrid(
            columns = GridCells.Adaptive(minSize = 160.dp),
            contentPadding = PaddingValues(top = 8.dp, bottom = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier.fillMaxSize(),
        ) {
            items(modules, key = { it.descriptor.id }) { module ->
                val title = context.getString(module.descriptor.titleRes)
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onOpen(module.descriptor.id) }
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                    ) {
                        Icon(
                            imageVector = module.descriptor.icon,
                            contentDescription = title,
                            tint = MaterialTheme.colorScheme.primary,
                        )
                        Text(text = title, style = MaterialTheme.typography.titleMedium)
                    }
                }
            }
        }
    }
}

@Composable
private fun ModuleErrorPanel(
    moduleId: String,
    errorText: String,
    onCopy: () -> Unit,
    onBack: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text(text = "模块异常：$moduleId", style = MaterialTheme.typography.titleMedium)
        Text(text = errorText, style = MaterialTheme.typography.bodySmall)
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            TextButton(onClick = onCopy) { Text(text = "复制错误") }
            TextButton(onClick = onBack) { Text(text = "返回") }
        }
    }
}
