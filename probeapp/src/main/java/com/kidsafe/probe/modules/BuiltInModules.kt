package com.kidsafe.probe.modules

import com.kidsafe.probe.modules.impl.DpLevelModule
import com.kidsafe.probe.modules.impl.CableDropModule
import com.kidsafe.probe.modules.impl.ProbeModule
import com.kidsafe.probe.modules.impl.RtdModule
import com.kidsafe.probe.modules.impl.SupportModule
import com.kidsafe.probe.modules.impl.ThermocoupleModule

object BuiltInModules {
    fun all(): List<FeatureModule> = listOf(
        ThermocoupleModule,
        RtdModule,
        ProbeModule,
        DpLevelModule,
        CableDropModule,
        SupportModule,
    )
}
