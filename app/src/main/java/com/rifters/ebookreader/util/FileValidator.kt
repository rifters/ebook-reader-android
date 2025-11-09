package com.rifters.ebookreader.util

import android.content.Context
import com.rifters.ebookreader.R
import java.io.File
import java.io.IOException

/**
 * Utility class for validating files and providing detailed error messages
 */
object FileValidator {
    
    // Maximum file size: 500 MB
    private const val MAX_FILE_SIZE_BYTES = 500 * 1024 * 1024L
    
    // Minimum file size: 1 KB (to detect empty files)
    private const val MIN_FILE_SIZE_BYTES = 1024L
    
    sealed class ValidationResult {
        object Valid : ValidationResult()
        data class Invalid(val errorMessage: String) : ValidationResult()
    }
    
    /**
     * Validates a file for reading
     */
    fun validateFile(file: File, context: Context, expectedExtension: String? = null): ValidationResult {
        // Check if file exists
        if (!file.exists()) {
            return ValidationResult.Invalid(context.getString(R.string.error_file_not_found))
        }
        
        // Check if it's a regular file
        if (!file.isFile) {
            return ValidationResult.Invalid(context.getString(R.string.error_opening_file))
        }
        
        // Check if file is readable
        if (!file.canRead()) {
            return ValidationResult.Invalid(context.getString(R.string.error_no_read_permission))
        }
        
        // Check file size
        val fileSize = file.length()
        if (fileSize == 0L) {
            return ValidationResult.Invalid(context.getString(R.string.error_file_empty))
        }
        
        if (fileSize < MIN_FILE_SIZE_BYTES) {
            return ValidationResult.Invalid(context.getString(R.string.error_file_empty))
        }
        
        if (fileSize > MAX_FILE_SIZE_BYTES) {
            val maxSizeMB = MAX_FILE_SIZE_BYTES / (1024 * 1024)
            return ValidationResult.Invalid(
                context.getString(R.string.error_file_too_large, "${maxSizeMB}MB")
            )
        }
        
        // Check file extension if provided
        if (expectedExtension != null) {
            val actualExtension = file.extension.lowercase()
            if (!actualExtension.equals(expectedExtension.lowercase(), ignoreCase = true)) {
                return ValidationResult.Invalid(
                    context.getString(R.string.error_invalid_format, expectedExtension.uppercase())
                )
            }
        }
        
        return ValidationResult.Valid
    }
    
    /**
     * Check if there's enough storage space for a file operation
     */
    fun hasEnoughStorage(requiredBytes: Long, targetDir: File): Boolean {
        return try {
            targetDir.usableSpace >= requiredBytes
        } catch (e: Exception) {
            false
        }
    }
    
    /**
     * Validates PDF file structure
     */
    fun validatePdfFile(file: File): Boolean {
        return try {
            file.inputStream().use { stream ->
                val header = ByteArray(5)
                val bytesRead = stream.read(header)
                bytesRead == 5 && 
                header[0] == '%'.code.toByte() && 
                header[1] == 'P'.code.toByte() && 
                header[2] == 'D'.code.toByte() && 
                header[3] == 'F'.code.toByte() && 
                header[4] == '-'.code.toByte()
            }
        } catch (e: Exception) {
            false
        }
    }
    
    /**
     * Validates EPUB file structure (checks for mimetype file)
     */
    fun validateEpubFile(file: File): Boolean {
        return try {
            java.util.zip.ZipFile(file).use { zipFile ->
                val mimetypeEntry = zipFile.getEntry("mimetype")
                if (mimetypeEntry != null) {
                    val mimetype = zipFile.getInputStream(mimetypeEntry).bufferedReader().use { it.readText() }
                    mimetype.trim() == "application/epub+zip"
                } else {
                    // Some EPUB files might not have mimetype, check for other required files
                    zipFile.getEntry("META-INF/container.xml") != null
                }
            }
        } catch (e: Exception) {
            false
        }
    }
    
    /**
     * Validates CBZ file (ZIP archive with images)
     */
    fun validateCbzFile(file: File): Boolean {
        return try {
            java.util.zip.ZipFile(file).use { zipFile ->
                zipFile.entries().asSequence().any { entry ->
                    !entry.isDirectory && isImageFile(entry.name)
                }
            }
        } catch (e: Exception) {
            false
        }
    }
    
    /**
     * Validates MOBI file structure (PalmDB format)
     */
    fun validateMobiFile(file: File): Boolean {
        return try {
            file.inputStream().use { stream ->
                val header = ByteArray(68)
                val bytesRead = stream.read(header)
                // Check for PalmDB header or MOBI signature
                bytesRead >= 68 && (
                    // Check for "BOOKMOBI" signature at offset 60
                    (header.size > 67 && 
                     header[60] == 'B'.code.toByte() &&
                     header[61] == 'O'.code.toByte() &&
                     header[62] == 'O'.code.toByte() &&
                     header[63] == 'K'.code.toByte()) ||
                    // Or check for "TEXtREAd" signature (Palm DOC)
                    (header.size > 67 && 
                     header[60] == 'T'.code.toByte() &&
                     header[61] == 'E'.code.toByte() &&
                     header[62] == 'X'.code.toByte() &&
                     header[63] == 't'.code.toByte())
                )
            }
        } catch (e: Exception) {
            false
        }
    }
    
    /**
     * Check if filename represents an image file
     */
    private fun isImageFile(filename: String): Boolean {
        val extension = filename.substringAfterLast('.', "").lowercase()
        return extension in setOf("jpg", "jpeg", "png", "gif", "bmp", "webp")
    }
    
    /**
     * Gets a user-friendly error message for an exception
     */
    fun getErrorMessage(exception: Throwable, context: Context, fileType: String = ""): String {
        return when (exception) {
            is IOException -> {
                when {
                    exception.message?.contains("ENOSPC") == true -> 
                        context.getString(R.string.error_storage_full)
                    exception.message?.contains("EACCES") == true -> 
                        context.getString(R.string.error_no_read_permission)
                    exception.message?.contains("ENOENT") == true -> 
                        context.getString(R.string.error_file_not_found)
                    else -> context.getString(R.string.error_opening_file)
                }
            }
            is SecurityException -> context.getString(R.string.error_no_read_permission)
            is OutOfMemoryError -> context.getString(R.string.error_file_too_large, "100MB")
            else -> when (fileType.lowercase()) {
                "pdf" -> context.getString(R.string.error_pdf_damaged)
                "epub" -> context.getString(R.string.error_epub_invalid)
                "mobi" -> context.getString(R.string.error_mobi_invalid)
                "cbz" -> context.getString(R.string.error_cbz_invalid)
                "cbr" -> context.getString(R.string.error_cbr_invalid)
                "fb2" -> "Error opening FictionBook file"
                "md" -> "Error opening Markdown file"
                "html", "htm", "xhtml", "xml", "mhtml" -> "Error opening HTML file"
                else -> context.getString(R.string.error_opening_file)
            }
        }
    }
    
    /**
     * Validates FB2 (FictionBook) file format
     */
    fun validateFb2File(file: File): Boolean {
        return try {
            val content = file.readText(Charsets.UTF_8).take(1000)
            content.contains("<?xml") && 
            (content.contains("<FictionBook") || content.contains("<fictionbook"))
        } catch (e: Exception) {
            false
        }
    }
    
    /**
     * Validates Markdown file format
     * Very permissive - any text file is technically valid Markdown
     */
    fun validateMarkdownFile(file: File): Boolean {
        return try {
            file.readText(Charsets.UTF_8).isNotEmpty()
        } catch (e: Exception) {
            false
        }
    }
    
    /**
     * Validates HTML/XML file format
     */
    fun validateHtmlFile(file: File): Boolean {
        return try {
            val content = file.readText(Charsets.UTF_8).take(1000).lowercase()
            content.contains("<html") || 
            content.contains("<!doctype") ||
            content.contains("<?xml")
        } catch (e: Exception) {
            false
        }
    }
}
