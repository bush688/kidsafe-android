package com.kidsafe.parent

import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithText
import org.junit.Rule
import org.junit.Test

class ParentAppTest {
    @get:Rule
    val rule = createAndroidComposeRule<MainActivity>()

    @Test
    fun hasSyncButton() {
        rule.onNodeWithText("同步到儿童端").assertExists()
    }
}