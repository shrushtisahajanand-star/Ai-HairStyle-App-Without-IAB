package com.hairstyle.generator.aimakerover.data.remote

/**
 * Temporary switches for local/dev generate API.
 */
object GenerateApiFlags {
    /** Multipart `image` field — JPEG file from ImageCompressor. */
    const val SEND_IMAGE_FILE = true

    /**
     * When true, ignore API response image URL and use selected style image.
     * Set false to show API result URL in Result screen.
     */
    const val USE_DEFAULT_RESULT_IMAGE = false
}
