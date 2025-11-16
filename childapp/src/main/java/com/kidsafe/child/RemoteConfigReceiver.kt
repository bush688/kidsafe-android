package com.kidsafe.child

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.kidsafe.child.lock.LockConfig
import com.kidsafe.child.rules.ScreenTimeRule
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch

class RemoteConfigReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val daily = intent.getIntExtra("daily", -1)
        val cats = intent.getStringExtra("cats") ?: ""
        val white = intent.getStringExtra("white") ?: ""
        val black = intent.getStringExtra("black") ?: ""
        val startMin = intent.getIntExtra("startMin", -1)
        val endMin = intent.getIntExtra("endMin", -1)
        val db = SecureDatabase.get(context)
        GlobalScope.launch(Dispatchers.IO) {
            if (daily > 0) db.screenTimeRuleDao().upsert(ScreenTimeRule(dailyLimitMinutes = daily))
            db.lockConfigDao().upsert(LockConfig(allowedCategories = cats, whitelist = white, blacklist = black))
            if (startMin >= 0 && endMin >= 0) db.timeWindowDao().upsert(com.kidsafe.child.rules.TimeWindow(startMinutes = startMin, endMinutes = endMin))
        }
    }
}