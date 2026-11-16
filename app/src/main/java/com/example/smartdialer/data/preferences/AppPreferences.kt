package com.example.smartdialer.data.preferences

enum class ThemeMode {
    SYSTEM,
    LIGHT,
    DARK
}

enum class AccentColorOption {
    OCEAN_BLUE,
    EMERALD_GREEN,
    ROYAL_PURPLE,
    AMBER_ORANGE,
    DYNAMIC
}

enum class FontSizeScale(val multiplier: Float) {
    SMALL(0.88f),
    NORMAL(1.0f),
    LARGE(1.14f),
    EXTRA_LARGE(1.26f)
}

enum class AppLanguage(val tag: String?) {
    SYSTEM(null),
    ENGLISH("en"),
    BENGALI("bn")
}

enum class CallRecordingScope {
    ALL_CALLS,
    UNKNOWN_CONTACTS_ONLY
}

data class UserPreferences(
    val themeMode: ThemeMode = ThemeMode.SYSTEM,
    val accentColor: AccentColorOption = AccentColorOption.OCEAN_BLUE,
    val fontSizeScale: FontSizeScale = FontSizeScale.NORMAL,
    val vibrationEnabled: Boolean = true,
    val language: AppLanguage = AppLanguage.SYSTEM,
    val preferredSimSlot: Int = -1,
    val autoCallRecordingEnabled: Boolean = false,
    val callRecordingScope: CallRecordingScope = CallRecordingScope.ALL_CALLS
)
