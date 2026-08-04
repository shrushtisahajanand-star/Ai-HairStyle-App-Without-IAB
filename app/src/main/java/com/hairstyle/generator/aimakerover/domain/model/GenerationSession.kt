package com.hairstyle.generator.aimakerover.domain.model

import android.net.Uri

data class GenerationSession(
    val originalImageUri: Uri? = null,
    val compressedImagePath: String? = null,
    val selectedHairstyle: Hairstyle? = null,
    val selectedColor: HairColor? = null,
    val resultImageUrl: String? = null,
    val resultImagePath: String? = null,
    val generationTimeMs: Long = 0L
)

data class LoadingStage(
    val label: String,
    val infoTitle: String,
    val infoSubtitle: String,
    val targetPercent: Int
)

object LoadingStages {
    val stages = listOf(
        LoadingStage("Preparing Image", "Face Shape Analysis", "Matching hairstyle with jawline", 0),
        LoadingStage("Uploading", "Hair Density", "Analyzing hair volume and texture", 15),
        LoadingStage("AI Processing", "Hairline Detection", "Mapping natural hairline boundaries", 32),
        LoadingStage("Analyzing Face", "Texture Mapping", "Applying realistic hair texture", 54),
        LoadingStage("Generating Hairstyle", "Color Blending", "Blending color with natural tones", 71),
        LoadingStage("Applying Hair Texture", "Face Shape Analysis", "Refining style to face shape", 86),
        LoadingStage("Rendering", "Texture Mapping", "Rendering final high-quality output", 95),
        LoadingStage("Finalizing", "Color Blending", "Polishing the final result", 100)
    )
}
