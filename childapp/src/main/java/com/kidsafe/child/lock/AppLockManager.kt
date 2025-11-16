package com.kidsafe.child.lock

import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager

data class LockRule(val category: String?, val minAge: Int?, val whitelist: Set<String>, val blacklist: Set<String>)

class AppLockManager(private val pm: PackageManager) {
    fun isAllowed(packageName: String, rule: LockRule, age: Int): Boolean {
        if (rule.blacklist.contains(packageName)) return false
        if (rule.whitelist.contains(packageName)) return true
        if (rule.minAge != null && age < rule.minAge) return false
        val info = pm.getApplicationInfo(packageName, 0)
        val cat = category(info)
        if (rule.category != null && rule.category != cat) return false
        return true
    }

    private fun category(info: ApplicationInfo): String {
        return if ((info.flags and ApplicationInfo.FLAG_SYSTEM) != 0) "system" else "user"
    }
}