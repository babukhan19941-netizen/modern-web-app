package com.binunpacker.app

/**
 * Represents one piece of embedded data found inside the selected .bin file.
 */
data class ExtractedEntry(
    val index: Int,
    val typeName: String,
    val extension: String,
    val offset: Long,
    val length: Long
)
