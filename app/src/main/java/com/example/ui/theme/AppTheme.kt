package com.example.ui.theme

enum class ThemeColor(val hex: Long, val displayName: String, val aliasName: String) {
    GREEN(0xFF10B981, "Verde Padrão", "Default"),
    BLUE(0xFF1976D2, "Azul", "DeepBlue"),
    DARK_GREEN(0xFF2E7D32, "Verde Escuro", "Nature"),
    BLACK(0xFF000000, "Preto", "Amoled"),
    PURPLE(0xFFBD93F9, "Roxo", "Dracula"),
    ORANGE(0xFFFF7043, "Laranja", "Sunset"),
    PINK(0xFFF000FF, "Rosa", "Cyberpunk"),
    RED(0xFFD32F2F, "Vermelho", "Red"),
    TEAL(0xFF009688, "Teal", "Teal"),
    INDIGO(0xFF3F51B5, "Índigo", "Indigo"),
    AMBER(0xFFFFC107, "Âmbar", "Amber"),
    DEEP_PURPLE(0xFF673AB7, "Roxo Escuro", "DeepPurple"),
    CYAN(0xFF00BCD4, "Ciano", "Cyan"),
    BROWN(0xFF795548, "Marrom", "Brown"),
    BLUE_GREY(0xFF607D8B, "Azul Cinza", "BlueGrey"),
    LIGHT_BLUE(0xFF03A9F4, "Azul Claro", "LightBlue")
}

data class CustomThemeData(
    val id: String,
    val name: String,
    val primaryColor: ThemeColor,
    val iconColor: ThemeColor,
    val profileColor: ThemeColor? = null,
    val isDark: Boolean
)

sealed class AppTheme(val id: String, val displayName: String) {
    object SYSTEM : AppTheme("SYSTEM", "Sistema")
    object LIGHT : AppTheme("LIGHT", "Claro")
    object DARK : AppTheme("DARK", "Escuro")
    object DEEP_BLUE : AppTheme("DEEP_BLUE", "Azul Profundo")
    object NATURE : AppTheme("NATURE", "Verde Floresta")
    object AMOLED : AppTheme("AMOLED", "Amoled (Escuro)")
    object DRACULA : AppTheme("DRACULA", "Drácula (Escuro)")
    object SUNSET : AppTheme("SUNSET", "Pôr do Sol (Claro)")
    object CYBERPUNK : AppTheme("CYBERPUNK", "Cyberpunk (Escuro)")
    
    data class Custom(val data: CustomThemeData) : AppTheme("CUSTOM_${data.id}", data.name)

    val name: String get() = id

    companion object {
        fun values(): List<AppTheme> = listOf(
            SYSTEM, LIGHT, DARK, DEEP_BLUE, NATURE, AMOLED, DRACULA, SUNSET, CYBERPUNK
        )
        fun valueOf(value: String): AppTheme {
            return values().find { it.id == value } ?: SYSTEM
        }
    }
}
