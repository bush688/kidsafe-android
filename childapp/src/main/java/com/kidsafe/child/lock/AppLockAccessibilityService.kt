package com.kidsafe.child.lock

import android.accessibilityservice.AccessibilityService
import android.content.Intent
import android.view.accessibility.AccessibilityEvent
import com.kidsafe.child.LockOverlayActivity
import com.kidsafe.child.SecureDatabase

class AppLockAccessibilityService : AccessibilityService() {
    override fun onAccessibilityEvent(event: AccessibilityEvent) {
        val pkg = event.packageName?.toString() ?: return
        if (event.eventType == AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED || event.eventType == AccessibilityEvent.TYPE_WINDOW_CONTENT_CHANGED) {
            val db = SecureDatabase.get(this)
            val cfg = db.lockConfigDao().get() ?: com.kidsafe.child.lock.LockConfig()
            val allowed = AppLockManager(packageManager).isAllowedWithConfig(pkg, cfg)
            if (!allowed && pkg != packageName) {
                val intent = Intent(this, LockOverlayActivity::class.java)
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                startActivity(intent)
            }
        }
    }

    override fun onInterrupt() {}
}