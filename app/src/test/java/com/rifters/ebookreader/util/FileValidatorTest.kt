package com.rifters.ebookreader.util

import android.content.Context
import com.rifters.ebookreader.R
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mock
import org.mockito.Mockito.*
import org.mockito.junit.MockitoJUnitRunner
import java.io.File
import java.io.IOException

@RunWith(MockitoJUnitRunner.Silent::class)
class FileValidatorTest {

    @Mock
    private lateinit var mockContext: Context

    @Mock
    private lateinit var mockFile: File

    private fun setupMockContext() {
        // Setup mock context to return test strings
        `when`(mockContext.getString(R.string.error_file_not_found))
            .thenReturn("File not found")
        `when`(mockContext.getString(R.string.error_file_empty))
            .thenReturn("File is empty")
        `when`(mockContext.getString(R.string.error_no_read_permission))
            .thenReturn("Cannot read file")
        `when`(mockContext.getString(R.string.error_opening_file))
            .thenReturn("Error opening file")
        `when`(mockContext.getString(eq(R.string.error_file_too_large), anyString()))
            .thenReturn("File is too large")
        `when`(mockContext.getString(eq(R.string.error_invalid_format), anyString()))
            .thenReturn("Invalid file format")
        `when`(mockContext.getString(R.string.error_pdf_damaged))
            .thenReturn("PDF file is damaged")
        `when`(mockContext.getString(R.string.error_epub_invalid))
            .thenReturn("EPUB file is invalid")
        `when`(mockContext.getString(R.string.error_storage_full))
            .thenReturn("Not enough storage space")
    }

    @Test
    fun testValidateFile_FileNotExists() {
        setupMockContext()
        `when`(mockFile.exists()).thenReturn(false)

        val result = FileValidator.validateFile(mockFile, mockContext)

        assertTrue(result is FileValidator.ValidationResult.Invalid)
        assertEquals("File not found", (result as FileValidator.ValidationResult.Invalid).errorMessage)
    }

    @Test
    fun testValidateFile_NotAFile() {
        setupMockContext()
        `when`(mockFile.exists()).thenReturn(true)
        `when`(mockFile.isFile()).thenReturn(false)

        val result = FileValidator.validateFile(mockFile, mockContext)

        assertTrue(result is FileValidator.ValidationResult.Invalid)
    }

    @Test
    fun testValidateFile_NotReadable() {
        setupMockContext()
        `when`(mockFile.exists()).thenReturn(true)
        `when`(mockFile.isFile()).thenReturn(true)
        `when`(mockFile.canRead()).thenReturn(false)

        val result = FileValidator.validateFile(mockFile, mockContext)

        assertTrue(result is FileValidator.ValidationResult.Invalid)
        assertEquals("Cannot read file", (result as FileValidator.ValidationResult.Invalid).errorMessage)
    }

    @Test
    fun testValidateFile_EmptyFile() {
        setupMockContext()
        `when`(mockFile.exists()).thenReturn(true)
        `when`(mockFile.isFile()).thenReturn(true)
        `when`(mockFile.canRead()).thenReturn(true)
        `when`(mockFile.length()).thenReturn(0L)

        val result = FileValidator.validateFile(mockFile, mockContext)

        assertTrue(result is FileValidator.ValidationResult.Invalid)
        assertEquals("File is empty", (result as FileValidator.ValidationResult.Invalid).errorMessage)
    }

    @Test
    fun testValidateFile_TooSmall() {
        setupMockContext()
        `when`(mockFile.exists()).thenReturn(true)
        `when`(mockFile.isFile()).thenReturn(true)
        `when`(mockFile.canRead()).thenReturn(true)
        `when`(mockFile.length()).thenReturn(512L) // Less than 1KB

        val result = FileValidator.validateFile(mockFile, mockContext)

        assertTrue(result is FileValidator.ValidationResult.Invalid)
        assertEquals("File is empty", (result as FileValidator.ValidationResult.Invalid).errorMessage)
    }

    @Test
    fun testValidateFile_TooLarge() {
        setupMockContext()
        `when`(mockFile.exists()).thenReturn(true)
        `when`(mockFile.isFile()).thenReturn(true)
        `when`(mockFile.canRead()).thenReturn(true)
        `when`(mockFile.length()).thenReturn(600L * 1024 * 1024) // 600MB

        val result = FileValidator.validateFile(mockFile, mockContext)

        assertTrue(result is FileValidator.ValidationResult.Invalid)
        assertEquals("File is too large", (result as FileValidator.ValidationResult.Invalid).errorMessage)
    }

    @Test
    fun testValidateFile_ValidFile() {
        setupMockContext()
        `when`(mockFile.exists()).thenReturn(true)
        `when`(mockFile.isFile()).thenReturn(true)
        `when`(mockFile.canRead()).thenReturn(true)
        `when`(mockFile.length()).thenReturn(10L * 1024 * 1024) // 10MB

        val result = FileValidator.validateFile(mockFile, mockContext)

        assertTrue(result is FileValidator.ValidationResult.Valid)
    }

    @Test
    fun testHasEnoughStorage_Sufficient() {
        `when`(mockFile.usableSpace).thenReturn(100L * 1024 * 1024) // 100MB available

        val result = FileValidator.hasEnoughStorage(50L * 1024 * 1024, mockFile) // Need 50MB

        assertTrue(result)
    }

    @Test
    fun testHasEnoughStorage_Insufficient() {
        `when`(mockFile.usableSpace).thenReturn(30L * 1024 * 1024) // 30MB available

        val result = FileValidator.hasEnoughStorage(50L * 1024 * 1024, mockFile) // Need 50MB

        assertFalse(result)
    }

    @Test
    fun testGetErrorMessage_IOException() {
        setupMockContext()
        val exception = IOException("Test IO error")

        val result = FileValidator.getErrorMessage(exception, mockContext)

        assertEquals("Error opening file", result)
    }

    @Test
    fun testGetErrorMessage_SecurityException() {
        setupMockContext()
        val exception = SecurityException("Test security error")

        val result = FileValidator.getErrorMessage(exception, mockContext)

        assertEquals("Cannot read file", result)
    }

    @Test
    fun testGetErrorMessage_OutOfMemoryError() {
        setupMockContext()
        val error = OutOfMemoryError("Test OOM error")

        val result = FileValidator.getErrorMessage(error, mockContext)

        assertEquals("File is too large", result)
    }

    @Test
    fun testGetErrorMessage_WithFileType_PDF() {
        setupMockContext()
        val exception = Exception("Generic error")

        val result = FileValidator.getErrorMessage(exception, mockContext, "pdf")

        assertEquals("PDF file is damaged", result)
    }

    @Test
    fun testGetErrorMessage_WithFileType_EPUB() {
        setupMockContext()
        val exception = Exception("Generic error")

        val result = FileValidator.getErrorMessage(exception, mockContext, "epub")

        assertEquals("EPUB file is invalid", result)
    }

    @Test
    fun testGetErrorMessage_IOException_NoSpace() {
        setupMockContext()
        val exception = IOException("ENOSPC: No space left on device")

        val result = FileValidator.getErrorMessage(exception, mockContext)

        assertEquals("Not enough storage space", result)
    }

    @Test
    fun testGetErrorMessage_IOException_NoPermission() {
        setupMockContext()
        val exception = IOException("EACCES: Permission denied")

        val result = FileValidator.getErrorMessage(exception, mockContext)

        assertEquals("Cannot read file", result)
    }

    @Test
    fun testGetErrorMessage_IOException_NotFound() {
        setupMockContext()
        val exception = IOException("ENOENT: No such file or directory")

        val result = FileValidator.getErrorMessage(exception, mockContext)

        assertEquals("File not found", result)
    }
}
