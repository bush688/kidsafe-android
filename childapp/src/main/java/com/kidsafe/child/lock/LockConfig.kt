package com.kidsafe.child.lock

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "lock_config")
data class LockConfig(
    @PrimaryKey val id: Int = 1,
    val minAge: Int = 0,
    val allowedCategories: String = "",
    val whitelist: String = "",
    val blacklist: String = ""
)