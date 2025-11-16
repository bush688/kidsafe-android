package com.kidsafe.child.lock

import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager

class AppLockManager(private val pm: PackageManager) {
    fun isAllowedWithConfig(packageName: String, config: LockConfig): Boolean {
        val blacklist = toSet(config.blacklist)
        val whitelist = toSet(config.whitelist)
        if (blacklist.contains(packageName)) return false
        if (whitelist.contains(packageName)) return true
        val info = try { pm.getApplicationInfo(packageName, 0) } catch (e: Exception) { null }
        val cat = if (info != null) category(info) else "user"
        val allowedCats = toSet(config.allowedCategories)
        if (allowedCats.isNotEmpty() && !allowedCats.contains(cat)) return false
        return true
    }

    private fun toSet(s: String): Set<String> = if (s.isBlank()) emptySet() else s.split(',').map { it.trim() }.filter { it.isNotEmpty() }.toSet()

    private fun category(info: ApplicationInfo): String {
        return when (info.category) {
            ApplicationInfo.CATEGORY_GAME -> "game"
            ApplicationInfo.CATEGORY_PRODUCTIVITY -> "productivity"
            ApplicationInfo.CATEGORY_SOCIAL -> "social"
            ApplicationInfo.CATEGORY_NEWS -> "news"
            ApplicationInfo.CATEGORY_AUDIO -> "audio"
            ApplicationInfo.CATEGORY_VIDEO -> "video"
            ApplicationInfo.CATEGORY_IMAGE -> "image"
            ApplicationInfo.CATEGORY_MAPS -> "maps"
            ApplicationInfo.CATEGORY_TEST -> "test"
            else -> if ((info.flags and ApplicationInfo.FLAG_SYSTEM) != 0) "system" else "user"
        }
    }
}