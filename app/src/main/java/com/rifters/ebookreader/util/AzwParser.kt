package com.rifters.ebookreader.util

import android.util.Log
import java.io.File
import java.io.RandomAccessFile
import java.nio.ByteBuffer
import java.nio.ByteOrder

/**
 * Parser for AZW3 and AZW (Kindle) formats
 * AZW3 is based on MOBI with KF8 format
 * AZW is essentially MOBI with DRM (we handle DRM-free only)
 */
object AzwParser {
    
    private const val TAG = "AzwParser"
    
    data class AzwContent(
        val title: String,
        val author: String,
        val content: String,
        val hasImages: Boolean = false
    )
    
    /**
     * Parse AZW/AZW3 file
     * Note: Only DRM-free files are supported
     */
    fun parse(file: File): AzwContent? {
        return try {
            RandomAccessFile(file, "r").use { raf ->
                // Read PalmDB header
                val header = ByteArray(78)
                raf.read(header)
                
                // Check for MOBI/AZW signature at offset 60
                val signature = String(header, 60, 8, Charsets.US_ASCII)
                if (!signature.startsWith("BOOKMOBI") && !signature.startsWith("TEXtREAd")) {
                    Log.e(TAG, "Invalid AZW/MOBI signature: $signature")
                    return null
                }
                
                // Parse PalmDB header to get record count
                val recordCount = ByteBuffer.wrap(header, 76, 2)
                    .order(ByteOrder.BIG_ENDIAN)
                    .short
                    .toInt()
                
                if (recordCount < 1) {
                    Log.e(TAG, "No records found in file")
                    return null
                }
                
                // Read record offsets
                val recordOffsets = mutableListOf<Int>()
                for (i in 0 until recordCount) {
                    val offsetBytes = ByteArray(4)
                    raf.read(offsetBytes)
                    val offset = ByteBuffer.wrap(offsetBytes)
                        .order(ByteOrder.BIG_ENDIAN)
                        .int
                    recordOffsets.add(offset)
                    raf.skipBytes(4) // Skip unique ID
                }
                
                // First record contains MOBI header
                if (recordOffsets.isEmpty()) {
                    return null
                }
                
                raf.seek(recordOffsets[0].toLong())
                val mobiHeader = ByteArray(256)
                raf.read(mobiHeader)
                
                // Extract title and author from EXTH header if present
                var title = file.nameWithoutExtension
                var author = "Unknown"
                
                // Look for EXTH header
                val exthOffset = findExthHeader(mobiHeader)
                if (exthOffset > 0) {
                    raf.seek(recordOffsets[0].toLong() + exthOffset)
                    val exthHeader = ByteArray(1024)
                    raf.read(exthHeader)
                    
                    val metadata = parseExthHeader(exthHeader)
                    title = metadata["title"] ?: title
                    author = metadata["author"] ?: author
                }
                
                // Extract text content from records
                val contentBuilder = StringBuilder()
                
                // Skip first record (header), read text records
                for (i in 1 until minOf(recordCount, 50)) { // Limit to first 50 records
                    if (i >= recordOffsets.size) break
                    
                    val recordOffset = recordOffsets[i]
                    val nextOffset = if (i + 1 < recordOffsets.size) {
                        recordOffsets[i + 1]
                    } else {
                        file.length().toInt()
                    }
                    
                    val recordSize = nextOffset - recordOffset
                    if (recordSize <= 0 || recordSize > 1024 * 1024) continue // Skip invalid/huge records
                    
                    raf.seek(recordOffset.toLong())
                    val recordData = ByteArray(recordSize)
                    raf.read(recordData)
                    
                    // Decompress if needed (PalmDOC compression)
                    val decompressed = try {
                        decompressPalmDoc(recordData)
                    } catch (e: Exception) {
                        recordData
                    }
                    
                    // Convert to text
                    val text = String(decompressed, Charsets.UTF_8)
                        .replace("\u0000", "") // Remove null characters
                        .trim()
                    
                    if (text.isNotEmpty()) {
                        contentBuilder.append(text)
                        contentBuilder.append("\n\n")
                    }
                }
                
                val content = contentBuilder.toString().trim()
                if (content.isEmpty()) {
                    return null
                }
                
                AzwContent(
                    title = title,
                    author = author,
                    content = content,
                    hasImages = false // TODO: Extract images
                )
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error parsing AZW file", e)
            null
        }
    }
    
    private fun findExthHeader(mobiHeader: ByteArray): Int {
        // EXTH header indicator is at offset 128-131 in MOBI header
        if (mobiHeader.size < 132) return -1
        
        val exthFlag = ByteBuffer.wrap(mobiHeader, 128, 4)
            .order(ByteOrder.BIG_ENDIAN)
            .int
        
        return if (exthFlag and 0x40 != 0) {
            // EXTH exists, typically starts after MOBI header
            232 // Typical MOBI header length
        } else {
            -1
        }
    }
    
    private fun parseExthHeader(exthData: ByteArray): Map<String, String> {
        val metadata = mutableMapOf<String, String>()
        
        try {
            // Check for EXTH signature
            val signature = String(exthData, 0, 4, Charsets.US_ASCII)
            if (signature != "EXTH") {
                return metadata
            }
            
            // Get record count
            val recordCount = ByteBuffer.wrap(exthData, 8, 4)
                .order(ByteOrder.BIG_ENDIAN)
                .int
            
            var offset = 12
            for (i in 0 until minOf(recordCount, 50)) { // Limit records
                if (offset + 8 > exthData.size) break
                
                val recordType = ByteBuffer.wrap(exthData, offset, 4)
                    .order(ByteOrder.BIG_ENDIAN)
                    .int
                val recordLength = ByteBuffer.wrap(exthData, offset + 4, 4)
                    .order(ByteOrder.BIG_ENDIAN)
                    .int
                
                if (recordLength < 8 || offset + recordLength > exthData.size) break
                
                val dataLength = recordLength - 8
                val data = String(exthData, offset + 8, dataLength, Charsets.UTF_8).trim()
                
                when (recordType) {
                    100 -> metadata["author"] = data
                    105 -> metadata["title"] = data
                    106 -> metadata["publisher"] = data
                    109 -> metadata["description"] = data
                }
                
                offset += recordLength
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error parsing EXTH header", e)
        }
        
        return metadata
    }
    
    private fun decompressPalmDoc(data: ByteArray): ByteArray {
        val output = mutableListOf<Byte>()
        var i = 0
        
        while (i < data.size) {
            val c = data[i].toInt() and 0xFF
            
            when {
                c == 0 -> {
                    // Null byte, skip
                    i++
                }
                c in 1..8 -> {
                    // Copy next c bytes literally
                    i++
                    for (j in 0 until c) {
                        if (i < data.size) {
                            output.add(data[i])
                            i++
                        }
                    }
                }
                c < 0x80 -> {
                    // Regular character
                    output.add(data[i])
                    i++
                }
                c in 0x80..0xBF -> {
                    // Two-byte sequence
                    if (i + 1 < data.size) {
                        val next = data[i + 1].toInt() and 0xFF
                        val distance = ((c shl 8) or next) and 0x3FFF
                        val length = (c shr 3) and 0x07
                        
                        // Copy from output history
                        val start = maxOf(0, output.size - distance)
                        for (j in 0 until (length + 3)) {
                            if (start + j < output.size) {
                                output.add(output[start + j])
                            }
                        }
                        i += 2
                    } else {
                        i++
                    }
                }
                c >= 0xC0 -> {
                    // Space + character
                    output.add(' '.code.toByte())
                    output.add((c xor 0x80).toByte())
                    i++
                }
                else -> {
                    output.add(data[i])
                    i++
                }
            }
        }
        
        return output.toByteArray()
    }
}
