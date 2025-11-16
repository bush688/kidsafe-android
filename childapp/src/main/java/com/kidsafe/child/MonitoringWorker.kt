package com.kidsafe.child

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.kidsafe.child.db.AppUsage
import com.kidsafe.child.db.AppUsageDao
import com.kidsafe.child.rules.ScreenTimeRuleDao
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class MonitoringWorker(appContext: Context, params: WorkerParameters) : CoroutineWorker(appContext, params) {
    override suspend fun doWork(): Result {
        val db = SecureDatabase.get(applicationContext)
        val events = UsageMonitor.collectToday(applicationContext)
        db.appUsageDao().insertAll(events.map { AppUsage.fromEvent(it) })
        val total = aggregateTodayMinutes(db.appUsageDao())
        val rule = db.screenTimeRuleDao().get() ?: kotlin.run {
            val r = com.kidsafe.child.rules.ScreenTimeRule(dailyLimitMinutes = 60)
            db.screenTimeRuleDao().upsert(r)
            r
        }
        if (total >= rule.dailyLimitMinutes) NotificationUtil.notifyTimeout(applicationContext, rule.dailyLimitMinutes)
        return Result.success()
    }

    private suspend fun aggregateTodayMinutes(dao: AppUsageDao): Int {
        return withContext(Dispatchers.IO) {
            val list = dao.latest(10000)
            var minutes = 0
            val stack = mutableMapOf<String, Long>()
            list.asReversed().forEach { e ->
                if (e.type == android.app.usage.UsageEvents.Event.MOVE_TO_FOREGROUND) stack[e.packageName] = e.time
                if (e.type == android.app.usage.UsageEvents.Event.MOVE_TO_BACKGROUND) {
                    val start = stack.remove(e.packageName)
                    if (start != null && e.time > start) minutes += ((e.time - start) / 60000L).toInt()
                }
            }
            minutes
        }
    }
}