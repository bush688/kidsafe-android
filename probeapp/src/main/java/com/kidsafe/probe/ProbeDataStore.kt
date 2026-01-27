package com.kidsafe.probe

import android.content.Context
import androidx.datastore.preferences.preferencesDataStore

val Context.probeDataStore by preferencesDataStore(name = "probe_calc")

