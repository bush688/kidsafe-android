package com.kidsafe.child.lock

import android.accessibilityservice.AccessibilityService
import android.content.Intent
import android.view.accessibility.AccessibilityEvent
import com.kidsafe.child.LockOverlayActivity
import com.kidsafe.child.SecureDatabase
import java.util.Calendar
import com.kidsafe.child.rules.TimeRules

class AppLockAccessibilityService : AccessibilityService() {
    override fun onAccessibilityEvent(event: AccessibilityEvent) {
        val pkg = event.packageName?.toString() ?: return
        if (event.eventType == AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED || event.eventType == AccessibilityEvent.TYPE_WINDOW_CONTENT_CHANGED) {
            val db = SecureDatabase.get(this)
            val cfg = db.lockConfigDao().get() ?: com.kidsafe.child.lock.LockConfig()
            val profile = db.childProfileDao().get() ?: com.kidsafe.child.profile.ChildProfile()
            val tw = db.timeWindowDao().get() ?: com.kidsafe.child.rules.TimeWindow()
            val allowed = AppLockManager(packageManager).isAllowedWithConfig(pkg, cfg, profile.age)
            val now = Calendar.getInstance()
            val minutes = now.get(Calendar.HOUR_OF_DAY) * 60 + now.get(Calendar.MINUTE)
            val inWindow = TimeRules.inWindow(minutes, tw)
            if ((!allowed || !inWindow) && pkg != packageName) {
                val intent = Intent(this, LockOverlayActivity::class.java)
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                startActivity(intent)
                if (!inWindow) com.kidsafe.child.NotificationUtil.notifyWindowBlocked(this)
            }
        }
    }

    override fun onInterrupt() {}
}