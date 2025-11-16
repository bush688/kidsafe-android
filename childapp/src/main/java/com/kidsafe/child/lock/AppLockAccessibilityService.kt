package com.kidsafe.child.lock

import android.accessibilityservice.AccessibilityService
import android.content.Intent
import android.view.accessibility.AccessibilityEvent
import com.kidsafe.child.LockOverlayActivity
import com.kidsafe.child.SecureDatabase
import java.util.Calendar

class AppLockAccessibilityService : AccessibilityService() {
    override fun onAccessibilityEvent(event: AccessibilityEvent) {
        val pkg = event.packageName?.toString() ?: return
        if (event.eventType == AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED || event.eventType == AccessibilityEvent.TYPE_WINDOW_CONTENT_CHANGED) {
            val db = SecureDatabase.get(this)
            val cfg = db.lockConfigDao().get() ?: com.kidsafe.child.lock.LockConfig()
            val tw = db.timeWindowDao().get() ?: com.kidsafe.child.rules.TimeWindow()
            val allowed = AppLockManager(packageManager).isAllowedWithConfig(pkg, cfg)
            val now = Calendar.getInstance()
            val minutes = now.get(Calendar.HOUR_OF_DAY) * 60 + now.get(Calendar.MINUTE)
            val inWindow = minutes in tw.startMinutes..tw.endMinutes
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