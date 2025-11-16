package com.kidsafe.child

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.kidsafe.child.db.AppUsage
import com.kidsafe.child.db.AppUsageDao
import com.kidsafe.child.rules.ScreenTimeRuleDao
import com.kidsafe.child.rules.TimeWindowDao
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import com.kidsafe.child.analytics.UsageAggregator
import com.kidsafe.child.rules.TimeRules

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
        val tw = db.timeWindowDao().get() ?: kotlin.run {
            val r = com.kidsafe.child.rules.TimeWindow()
            db.timeWindowDao().upsert(r)
            r
        }
        val cal = java.util.Calendar.getInstance()
        val minutes = cal.get(java.util.Calendar.HOUR_OF_DAY) * 60 + cal.get(java.util.Calendar.MINUTE)
        if (!TimeRules.inWindow(minutes, tw)) NotificationUtil.notifyWindowBlocked(applicationContext)
        return Result.success()
    }

    private suspend fun aggregateTodayMinutes(dao: AppUsageDao): Int {
        return withContext(Dispatchers.IO) { UsageAggregator.aggregateMinutes(dao.latest(10000)) }
    }
}