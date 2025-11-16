package com.kidsafe.child

import com.kidsafe.child.analytics.UsageAggregator
import com.kidsafe.child.db.AppUsage
import org.junit.Assert.assertEquals
import org.junit.Test

class UsageAggregatorTest {
    @Test
    fun aggregateSimpleSession() {
        val pkg = "com.example.app"
        val start = AppUsage(packageName = pkg, type = android.app.usage.UsageEvents.Event.MOVE_TO_FOREGROUND, time = 0)
        val end = AppUsage(packageName = pkg, type = android.app.usage.UsageEvents.Event.MOVE_TO_BACKGROUND, time = 600000)
        val minutes = UsageAggregator.aggregateMinutes(listOf(start, end))
        assertEquals(10, minutes)
    }
}