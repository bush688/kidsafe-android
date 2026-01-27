package com.kidsafe.probe.modules.impl

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.WaterDrop
import androidx.compose.runtime.Composable
import com.kidsafe.probe.R
import com.kidsafe.probe.dplevel.DpLevelModule
import com.kidsafe.probe.modules.FeatureModule
import com.kidsafe.probe.modules.ModuleDescriptor
import com.kidsafe.probe.modules.ModuleHost

object DpLevelModule : FeatureModule {
    override val descriptor = ModuleDescriptor(
        id = "dp_level",
        titleRes = R.string.feature_dp_level,
        icon = Icons.Default.WaterDrop,
    )

    @Composable
    override fun Content(host: ModuleHost, state: Any?) {
        DpLevelModule(
            onCopy = { text ->
                host.copyToClipboard("dp_level_calc", text)
                host.showMessage(host.context.getString(R.string.copied))
            }
        )
    }
}

