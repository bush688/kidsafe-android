package com.kidsafe.child

import com.kidsafe.child.rules.TimeRules
import com.kidsafe.child.rules.TimeWindow
import org.junit.Assert.assertTrue
import org.junit.Assert.assertFalse
import org.junit.Test

class TimeRulesTest {
    @Test
    fun withinWindow() {
        val tw = TimeWindow(startMinutes = 480, endMinutes = 1200)
        assertTrue(TimeRules.inWindow(600, tw))
        assertFalse(TimeRules.inWindow(300, tw))
        assertFalse(TimeRules.inWindow(1300, tw))
    }
}