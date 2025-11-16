package com.kidsafe.child

import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithText
import org.junit.Rule
import org.junit.Test

class KidsGridTest {
    @get:Rule
    val rule = createAndroidComposeRule<MainActivity>()

    @Test
    fun gridHasItems() {
        rule.onNodeWithText("学习").assertExists()
        rule.onNodeWithText("游戏").assertExists()
        rule.onNodeWithText("视频").assertExists()
    }
}