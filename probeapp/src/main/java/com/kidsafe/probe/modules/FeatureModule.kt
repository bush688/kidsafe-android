package com.kidsafe.probe.modules

import android.content.Context
import androidx.annotation.StringRes
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.vector.ImageVector

@Immutable
data class ModuleDescriptor(
    val id: String,
    @StringRes val titleRes: Int,
    val icon: ImageVector,
)

interface ModuleHost {
    val context: Context

    suspend fun showMessage(message: String)

    fun copyToClipboard(label: String, text: String)

    fun requestBack()
}

interface FeatureModule {
    val descriptor: ModuleDescriptor

    val useHostScroll: Boolean get() = true

    fun createState(): Any? = null

    suspend fun onEnter(host: ModuleHost, state: Any?) {}

    suspend fun onExit(host: ModuleHost, state: Any?) {}

    @Composable
    fun Content(host: ModuleHost, state: Any?)
}
