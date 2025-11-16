package com.kidsafe.child

import com.kidsafe.child.analytics.UsageAggregator
import com.kidsafe.child.db.AppUsage
import org.junit.Assert.assertEquals
import org.junit.Test

class UsageAggregatorByPackageTest {
    @Test
    fun aggregatePerPackage() {
        val p1 = "a"
        val p2 = "b"
        val events = listOf(
            AppUsage(packageName = p1, type = android.app.usage.UsageEvents.Event.MOVE_TO_FOREGROUND, time = 0),
            AppUsage(packageName = p1, type = android.app.usage.UsageEvents.Event.MOVE_TO_BACKGROUND, time = 120000),
            AppUsage(packageName = p2, type = android.app.usage.UsageEvents.Event.MOVE_TO_FOREGROUND, time = 120000),
            AppUsage(packageName = p2, type = android.app.usage.UsageEvents.Event.MOVE_TO_BACKGROUND, time = 480000)
        )
        val map = UsageAggregator.aggregateByPackage(events)
        assertEquals(2, map[p1])
        assertEquals(6, map[p2])
    }
}