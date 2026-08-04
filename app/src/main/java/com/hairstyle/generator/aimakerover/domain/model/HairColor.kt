package com.hairstyle.generator.aimakerover.domain.model

enum class HairColor(
    val displayName: String,
    val apiValue: String,
    val colorHex: Long
) {
    BLACK("Black", "black", 0xFF1A1A1A),
    BROWN("Brown", "brown", 0xFF5C3A21),
    DARK_BROWN("Dark Brown", "darkBrown", 0xFF3B2314),
    LIGHT_BROWN("Light Brown", "lightBrown", 0xFF8B5E3C),
    GOLDEN("Golden", "golden", 0xFFD4A017),
    BLONDE("Blonde", "blonde", 0xFFE8C872),
    ASH_BLONDE("Ash Blonde", "ashBlonde", 0xFFC9B896),
    SILVER("Silver", "silver", 0xFFC0C0C0),
    GRAY("Gray", "gray", 0xFF9E9E9E),
    WHITE("White", "white", 0xFFF5F5F5),
    BLUE("Blue", "blue", 0xFF2196F3),
    RED("Red", "red", 0xFFE53935),
    PURPLE("Purple", "purple", 0xFF9C27B0),
    PINK("Pink", "pink", 0xFFE91E8C),
    GREEN("Green", "green", 0xFF4CAF50);

    companion object {
        fun fromApiValue(value: String): HairColor? =
            entries.find { it.apiValue.equals(value, ignoreCase = true) }
    }
}
