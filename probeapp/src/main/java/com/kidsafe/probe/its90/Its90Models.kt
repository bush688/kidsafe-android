package com.kidsafe.probe.its90

enum class ThermocoupleType(
    val displayName: String,
    internal val tableId: Int,
) {
    K("K", 1),
    S("S", 2),
    R("R", 3),
    B("B", 4),
    N("N", 5),
    E("E", 6),
    J("J", 7),
    T("T", 8),
}

enum class RtdType(
    val displayName: String,
    internal val tableId: Int,
    val unitLabel: String,
) {
    Pt10("Pt10", 9, "Ω"),
    Pt100("Pt100", 10, "Ω"),
    Pt500("Pt500", 11, "Ω"),
    Pt800("Pt800", 12, "Ω"),
    Pt1000("Pt1000", 13, "Ω"),
    Cu10("Cu10", 14, "Ω"),
    Cu50("Cu50", 15, "Ω"),
    Cu100("Cu100", 16, "Ω"),
    BA1("BA1", 17, "Ω"),
    BA2("BA2", 18, "Ω"),
    G53("G53", 19, "Ω"),
}

enum class RtdWiring(val displayName: String) {
    WIRE_2("二线制"),
    WIRE_3("三线制"),
    WIRE_4("四线制"),
}

