package com.binunpacker.app

import java.io.File
import java.io.FileOutputStream

/**
 * Generic ".bin" content scanner.
 *
 * A ".bin" file has no single universal format - it can be firmware, a disk
 * image, a resource pack, etc. This class does not assume any specific
 * container format. Instead it scans the raw bytes for well-known file
 * "magic number" signatures (the same technique forensic/carving tools like
 * binwalk use) and treats every signature match as the start of one embedded
 * item. Each item ends where the next detected item begins (or at end of
 * file for the last one).
 */
object BinExtractor {

    private data class Signature(
        val name: String,
        val extension: String,
        // relative offset (from the match position) where the magic bytes start
        val magicOffset: Int,
        val magic: ByteArray
    )

    private val SIGNATURES = listOf(
        Signature("PNG Image", "png", 0, byteArrayOf(0x89.toByte(), 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A)),
        Signature("JPEG Image", "jpg", 0, byteArrayOf(0xFF.toByte(), 0xD8.toByte(), 0xFF.toByte())),
        Signature("GIF Image", "gif", 0, byteArrayOf(0x47, 0x49, 0x46, 0x38)),
        Signature("BMP Image", "bmp", 0, byteArrayOf(0x42, 0x4D)),
        Signature("PDF Document", "pdf", 0, byteArrayOf(0x25, 0x50, 0x44, 0x46)),
        Signature("ZIP/APK/JAR Archive", "zip", 0, byteArrayOf(0x50, 0x4B, 0x03, 0x04)),
        Signature("RAR Archive (v5)", "rar", 0, byteArrayOf(0x52, 0x61, 0x72, 0x21, 0x1A, 0x07, 0x01, 0x00)),
        Signature("RAR Archive (v4)", "rar", 0, byteArrayOf(0x52, 0x61, 0x72, 0x21, 0x1A, 0x07, 0x00)),
        Signature("7-Zip Archive", "7z", 0, byteArrayOf(0x37, 0x7A, 0xBC.toByte(), 0xAF.toByte(), 0x27, 0x1C)),
        Signature("GZIP Archive", "gz", 0, byteArrayOf(0x1F, 0x8B.toByte(), 0x08)),
        Signature("BZIP2 Archive", "bz2", 0, byteArrayOf(0x42, 0x5A, 0x68)),
        Signature("ELF Binary", "elf", 0, byteArrayOf(0x7F, 0x45, 0x4C, 0x46)),
        Signature("OGG Audio", "ogg", 0, byteArrayOf(0x4F, 0x67, 0x67, 0x53)),
        Signature("MP4/MOV Video", "mp4", 4, "ftyp".toByteArray(Charsets.US_ASCII)),
        Signature("WAV Audio", "wav", 8, "WAVE".toByteArray(Charsets.US_ASCII)),
        Signature("ID3/MP3 Audio", "mp3", 0, byteArrayOf(0x49, 0x44, 0x33))
    )

    data class ScanResult(
        val entries: List<ExtractedEntry>,
        val totalSize: Long
    )

    /** Scan the given bytes and return the list of detected embedded items. */
    fun scan(data: ByteArray): ScanResult {
        val size = data.size
        // position -> matched signature, keep only the first signature found at a position
        val hits = sortedMapOf<Int, Signature>()

        for (sig in SIGNATURES) {
            val magic = sig.magic
            val magicOffset = sig.magicOffset
            if (magic.isEmpty() || size < magic.size + magicOffset) continue
            var i = 0
            val lastPossible = size - magic.size - magicOffset
            while (i <= lastPossible) {
                var matched = true
                var j = 0
                while (j < magic.size) {
                    if (data[i + magicOffset + j] != magic[j]) {
                        matched = false
                        break
                    }
                    j++
                }
                if (matched) {
                    val start = i
                    if (!hits.containsKey(start)) {
                        hits[start] = sig
                    }
                }
                i++
            }
        }

        val positions = hits.keys.toList()
        val entries = mutableListOf<ExtractedEntry>()

        if (positions.isEmpty()) {
            // Nothing recognized - report the whole file as one raw/unknown blob
            entries.add(ExtractedEntry(0, "Unknown / Raw Data", "bin", 0L, size.toLong()))
        } else {
            // If the file doesn't start at a signature, keep the leading bytes as a raw header chunk
            if (positions[0] != 0) {
                entries.add(ExtractedEntry(0, "Header / Unknown Data", "bin", 0L, positions[0].toLong()))
            }
            for (idx in positions.indices) {
                val start = positions[idx]
                val end = if (idx + 1 < positions.size) positions[idx + 1] else size
                val sig = hits[start]!!
                entries.add(
                    ExtractedEntry(
                        index = entries.size,
                        typeName = sig.name,
                        extension = sig.extension,
                        offset = start.toLong(),
                        length = (end - start).toLong()
                    )
                )
            }
        }

        return ScanResult(entries, size.toLong())
    }

    /**
     * Write every entry out to [outputDir] as a separate file named
     * "item_<index>_<offset>.<ext>". Returns the number of files written.
     */
    fun saveAll(data: ByteArray, entries: List<ExtractedEntry>, outputDir: File): Int {
        if (!outputDir.exists()) outputDir.mkdirs()
        var count = 0
        for (entry in entries) {
            val outFile = File(outputDir, "item_${entry.index}_${entry.offset}.${entry.extension}")
            FileOutputStream(outFile).use { fos ->
                fos.write(data, entry.offset.toInt(), entry.length.toInt())
            }
            count++
        }
        return count
    }
}
