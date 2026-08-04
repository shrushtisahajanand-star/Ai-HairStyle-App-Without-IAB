package com.hairstyle.generator.aimakerover.domain.model

enum class Hairstyle(
    val displayName: String,
    val apiValue: String,
    val description: String
) {
    BUZZ_CUT("Buzz Cut", "BuzzCut", "Clean, ultra-short buzz cut with even texture."),
    CREW_CUT("Crew Cut", "CrewCut", "Classic crew cut with tapered sides."),
    FADE("Fade", "Fade", "Smooth gradient fade from skin to full length."),
    SKIN_FADE("Skin Fade", "SkinFade", "Sharp skin fade with crisp transition."),
    HIGH_FADE("High Fade", "HighFade", "High fade starting above the temples."),
    LOW_FADE("Low Fade", "LowFade", "Subtle low fade near the ears."),
    MID_FADE("Mid Fade", "MidFade", "Balanced mid-level fade."),
    FRENCH_CROP("French Crop", "FrenchCrop", "Textured French crop with fringe."),
    POMPADOUR("Pompadour", "Pompadour", "Voluminous pompadour with swept-back style."),
    QUIFF("Quiff", "Quiff", "Modern quiff with height and texture."),
    SLICK_BACK("Slick Back", "SlickBack", "Polished slick-back finish."),
    UNDERCUT("Undercut", "Undercut", "Disconnected undercut with contrast."),
    CURTAINS("Curtains", "Curtains", "Center-parted curtain bangs."),
    SIDE_PART("Side Part", "SidePart", "Classic side part with clean lines."),
    COMB_OVER("Comb Over", "CombOver", "Neat comb-over with volume."),
    MOHAWK("Mohawk", "Mohawk", "Bold mohawk with shaved sides."),
    AFRO("Afro", "Afro", "Full natural afro texture."),
    CURLY("Curly", "Curly", "Defined curly hair with bounce."),
    LONG_HAIR("Long Hair", "LongHair", "Flowing long hairstyle."),
    WOLF_CUT("Wolf Cut", "WolfCut", "Trendy wolf cut with layers."),
    MULLET("Mullet", "Mullet", "Business in front, party in back."),
    BALD("Bald", "Bald", "Clean bald look with smooth finish.");

    companion object {
        fun fromApiValue(value: String): Hairstyle? =
            entries.find { it.apiValue.equals(value, ignoreCase = true) }
    }
}
