package com.kidsafe.child.rules

object TimeRules {
    fun inWindow(minutes: Int, tw: TimeWindow): Boolean = minutes in tw.startMinutes..tw.endMinutes
}