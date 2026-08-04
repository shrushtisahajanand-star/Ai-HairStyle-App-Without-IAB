package com.hairstyle.generator.aimakerover.utils

class ApiException(
    override val message: String,
    val code: Int = -1,
    val canRetry: Boolean = true
) : Exception(message)
