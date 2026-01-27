package com.kidsafe.probe.modules.impl

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Straighten
import androidx.compose.runtime.Composable
import com.kidsafe.probe.R
import com.kidsafe.probe.probe.ProbeCalcModule
import com.kidsafe.probe.modules.FeatureModule
import com.kidsafe.probe.modules.ModuleDescriptor
import com.kidsafe.probe.modules.ModuleHost

object ProbeModule : FeatureModule {
    override val descriptor = ModuleDescriptor(
        id = "probe",
        titleRes = R.string.feature_probe,
        icon = Icons.Default.Straighten,
    )

    @Composable
    override fun Content(host: ModuleHost, state: Any?) {
        ProbeCalcModule(
            onCopy = { text ->
                host.copyToClipboard("probe_calc", text)
                host.showMessage(host.context.getString(R.string.copied))
            }
        )
    }
}

