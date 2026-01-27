package com.kidsafe.probe

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.kidsafe.probe.core.ClipboardUtils
import com.kidsafe.probe.ui.ProbeAppTheme

class ThermoActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            ProbeAppTheme {
                val context = androidx.compose.ui.platform.LocalContext.current
                var crashText by remember { mutableStateOf(CrashReporter.readCrash(context)) }

                InstrumentCalculatorApp()

                if (crashText != null) {
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
}
