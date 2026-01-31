package com.kidsafe.probe.support

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.datastore.preferences.core.edit
import com.kidsafe.probe.R
import com.kidsafe.probe.ThermoActivity
import com.kidsafe.probe.probeDataStore
import kotlinx.coroutines.runBlocking
import org.junit.Rule
import org.junit.Test

class PrivacyConsentUiTest {
    @get:Rule
    val rule = createAndroidComposeRule<ThermoActivity>()

    @Test
    fun showsPrivacyDialogOnFirstLaunch() {
        val ctx = rule.activity.applicationContext
        runBlocking {
            ctx.probeDataStore.edit { it.clear() }
        }
        rule.activityRule.scenario.recreate()

        val title = rule.activity.getString(R.string.privacy_policy)
        rule.onNodeWithText(title).assertIsDisplayed()
    }
}

