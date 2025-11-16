package com.kidsafe.child.analytics

import com.kidsafe.child.db.AppUsage

object UsageAggregator {
    fun aggregateMinutes(events: List<AppUsage>): Int {
        var minutes = 0
        val stack = mutableMapOf<String, Long>()
        events.asReversed().forEach { e ->
            if (e.type == android.app.usage.UsageEvents.Event.MOVE_TO_FOREGROUND) stack[e.packageName] = e.time
            if (e.type == android.app.usage.UsageEvents.Event.MOVE_TO_BACKGROUND) {
                val start = stack.remove(e.packageName)
                if (start != null && e.time > start) minutes += ((e.time - start) / 60000L).toInt()
            }
        }
        return minutes
    }

    fun aggregateByPackage(events: List<AppUsage>): Map<String, Int> {
        val stack = mutableMapOf<String, Long>()
        val acc = mutableMapOf<String, Int>()
        events.asReversed().forEach { e ->
            if (e.type == android.app.usage.UsageEvents.Event.MOVE_TO_FOREGROUND) stack[e.packageName] = e.time
            if (e.type == android.app.usage.UsageEvents.Event.MOVE_TO_BACKGROUND) {
                val start = stack.remove(e.packageName)
                if (start != null && e.time > start) {
                    val mins = ((e.time - start) / 60000L).toInt()
                    acc[e.packageName] = (acc[e.packageName] ?: 0) + mins
                }
            }
        }
        return acc.toList().sortedByDescending { it.second }.toMap()
    }
}