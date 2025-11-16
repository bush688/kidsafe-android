package com.kidsafe.child

import android.app.usage.UsageEvents
import android.app.usage.UsageStatsManager
import android.content.Context
import android.provider.Settings
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

object UsageMonitor {
    fun hasAccess(context: Context): Boolean {
        return Settings.Secure.getInt(context.contentResolver, "usage_access", 0) == 1
    }

    suspend fun collectToday(context: Context): List<AppUsageEvent> {
        return withContext(Dispatchers.IO) {
            val manager = context.getSystemService(Context.USAGE_STATS_SERVICE) as UsageStatsManager
            val now = System.currentTimeMillis()
            val start = now - 24 * 60 * 60 * 1000
            val events = manager.queryEvents(start, now)
            val list = mutableListOf<AppUsageEvent>()
            val e = UsageEvents.Event()
            while (events.hasNextEvent()) {
                events.getNextEvent(e)
                if (e.eventType == UsageEvents.Event.MOVE_TO_FOREGROUND || e.eventType == UsageEvents.Event.MOVE_TO_BACKGROUND) {
                    list.add(AppUsageEvent(e.packageName, e.eventType, e.timeStamp))
                }
            }
            list
        }
    }
}

data class AppUsageEvent(val packageName: String, val type: Int, val time: Long)