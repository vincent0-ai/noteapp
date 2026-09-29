package com.example.echowithin.ui.theme

import androidx.compose.ui.graphics.Color

data class NoteColor(
    val id: String,
    val name: String,
    val lightColor: Color,
    val darkColor: Color,
    val accentColor: Color
)

object NoteColorPalette {
    val options = listOf(
        NoteColor(
            id = "default",
            name = "Default",
            lightColor = Color.Transparent,
            darkColor = Color.Transparent,
            accentColor = BrandOrange
        ),
        NoteColor(
            id = "amber",
            name = "Warm Amber",
            lightColor = Color(0xFFFFF8EC),
            darkColor = Color(0xFF281E12),
            accentColor = Color(0xFFD97706)
        ),
        NoteColor(
            id = "sage",
            name = "Sage Green",
            lightColor = Color(0xFFF1F8F3),
            darkColor = Color(0xFF14261B),
            accentColor = Color(0xFF16A34A)
        ),
        NoteColor(
            id = "sky",
            name = "Slate Blue",
            lightColor = Color(0xFFF0F7FF),
            darkColor = Color(0xFF142436),
            accentColor = Color(0xFF0284C7)
        ),
        NoteColor(
            id = "rose",
            name = "Dusty Rose",
            lightColor = Color(0xFFFFF1F2),
            darkColor = Color(0xFF2E151A),
            accentColor = Color(0xFFE11D48)
        ),
        NoteColor(
            id = "lavender",
            name = "Soft Lavender",
            lightColor = Color(0xFFF8F4FD),
            darkColor = Color(0xFF231633),
            accentColor = Color(0xFF9333EA)
        )
    )

    fun getBackgroundColor(id: String?, isDark: Boolean): Color {
        val color = options.find { it.id.equals(id, ignoreCase = true) } ?: return Color.Transparent
        return if (isDark) color.darkColor else color.lightColor
    }

    fun getAccentColor(id: String?): Color {
        val color = options.find { it.id.equals(id, ignoreCase = true) } ?: return BrandOrange
        return color.accentColor
    }
}
