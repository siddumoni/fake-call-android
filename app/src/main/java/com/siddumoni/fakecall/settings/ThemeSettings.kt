package com.siddumoni.fakecall.settings

enum class ThemeAppearance(
    val storageValue: String,
) {
    FOLLOW_SYSTEM("follow_system"),
    LIGHT("light"),
    DARK("dark"),
    ;

    companion object {
        fun fromStorage(value: String?): ThemeAppearance = entries.firstOrNull { it.storageValue == value } ?: FOLLOW_SYSTEM
    }
}

data class ThemeSettings(
    val appearance: ThemeAppearance = ThemeAppearance.FOLLOW_SYSTEM,
    val pureBlack: Boolean = false,
    val dynamicColor: Boolean = true,
) {
    companion object {
        val Default = ThemeSettings()
    }
}
