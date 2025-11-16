package com.kidsafe.child.profile

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "child_profile")
data class ChildProfile(
    @PrimaryKey val id: Int = 1,
    val age: Int = 8
)