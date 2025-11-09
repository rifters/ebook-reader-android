package com.rifters.ebookreader.util

import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4
import java.io.File

@RunWith(JUnit4::class)
class EpubParserTest {

    @Test
    fun testParseEpubStructure() {
        // Try to find the test file in the root directory (for local testing)
        var testFile = File("Fight. Level. Survive._ An Isekai LitRPG Adventure_nodrm.epub")
        
        // If not found, try in parent directory
        if (!testFile.exists()) {
            testFile = File("../Fight. Level. Survive._ An Isekai LitRPG Adventure_nodrm.epub")
        }
        
        // If still not found, try in repository root
        if (!testFile.exists()) {
            testFile = File("../../Fight. Level. Survive._ An Isekai LitRPG Adventure_nodrm.epub")
        }
        
        // Skip test if file doesn't exist (for CI environments)
        if (!testFile.exists()) {
            println("Test EPUB file not found, skipping test")
            return
        }
        
        val parser = EpubParser(testFile)
        val content = parser.parse()
        
        assertNotNull("EPUB content should not be null", content)
        assertNotNull("Title should not be null", content?.title)
        assertNotNull("Spine should not be null", content?.spine)
        assertTrue("Spine should have items", content?.spine?.isNotEmpty() == true)
        assertNotNull("Manifest should not be null", content?.manifest)
        assertTrue("Manifest should have items", content?.manifest?.isNotEmpty() == true)
        
        println("EPUB parsed successfully:")
        println("  Title: ${content?.title}")
        println("  Author: ${content?.author}")
        println("  Spine items: ${content?.spine?.size}")
        println("  Manifest items: ${content?.manifest?.size}")
    }

    @Test
    fun testGetChapterContent() {
        var testFile = File("Fight. Level. Survive._ An Isekai LitRPG Adventure_nodrm.epub")
        if (!testFile.exists()) {
            testFile = File("../Fight. Level. Survive._ An Isekai LitRPG Adventure_nodrm.epub")
        }
        if (!testFile.exists()) {
            testFile = File("../../Fight. Level. Survive._ An Isekai LitRPG Adventure_nodrm.epub")
        }
        
        // Skip test if file doesn't exist (for CI environments)
        if (!testFile.exists()) {
            println("Test EPUB file not found, skipping test")
            return
        }
        
        val parser = EpubParser(testFile)
        val content = parser.parse()
        
        assertNotNull("EPUB content should not be null", content)
        
        // Test reading the first chapter
        val chapterHtml = parser.getChapterContent(0, content!!)
        
        assertNotNull("Chapter content should not be null", chapterHtml)
        assertTrue("Chapter content should not be empty", chapterHtml?.isNotEmpty() == true)
        assertTrue("Chapter content should be HTML", chapterHtml?.contains("<html>") == true || chapterHtml?.contains("<?xml") == true)
        
        println("First chapter loaded successfully:")
        println("  Length: ${chapterHtml?.length} characters")
        println("  First 100 chars: ${chapterHtml?.take(100)}")
    }

    @Test
    fun testGetChapterContentForAllChapters() {
        var testFile = File("Fight. Level. Survive._ An Isekai LitRPG Adventure_nodrm.epub")
        if (!testFile.exists()) {
            testFile = File("../Fight. Level. Survive._ An Isekai LitRPG Adventure_nodrm.epub")
        }
        if (!testFile.exists()) {
            testFile = File("../../Fight. Level. Survive._ An Isekai LitRPG Adventure_nodrm.epub")
        }
        
        // Skip test if file doesn't exist (for CI environments)
        if (!testFile.exists()) {
            println("Test EPUB file not found, skipping test")
            return
        }
        
        val parser = EpubParser(testFile)
        val content = parser.parse()
        
        assertNotNull("EPUB content should not be null", content)
        
        // Test reading multiple chapters
        val chaptersToTest = minOf(5, content!!.spine.size)
        var successCount = 0
        var failCount = 0
        
        for (i in 0 until chaptersToTest) {
            val chapterHtml = parser.getChapterContent(i, content)
            if (chapterHtml != null && chapterHtml.isNotEmpty()) {
                successCount++
            } else {
                failCount++
                println("Warning: Chapter $i failed to load")
            }
        }
        
        println("Chapter loading results:")
        println("  Tested: $chaptersToTest chapters")
        println("  Success: $successCount")
        println("  Failed: $failCount")
        
        assertTrue("At least one chapter should load successfully", successCount > 0)
        assertTrue("Most chapters should load successfully", successCount >= chaptersToTest * 0.8)
    }

    @Test
    fun testParseSecondEpubFile() {
        var testFile = File("Fight. Level. Survive. 2_ An Isekai LitRPG Adventure_nodrm.epub")
        if (!testFile.exists()) {
            testFile = File("../Fight. Level. Survive. 2_ An Isekai LitRPG Adventure_nodrm.epub")
        }
        if (!testFile.exists()) {
            testFile = File("../../Fight. Level. Survive. 2_ An Isekai LitRPG Adventure_nodrm.epub")
        }
        
        // Skip test if file doesn't exist (for CI environments)
        if (!testFile.exists()) {
            println("Test EPUB file not found, skipping test")
            return
        }
        
        val parser = EpubParser(testFile)
        val content = parser.parse()
        
        assertNotNull("EPUB content should not be null", content)
        assertNotNull("Title should not be null", content?.title)
        assertNotNull("Spine should not be null", content?.spine)
        assertTrue("Spine should have items", content?.spine?.isNotEmpty() == true)
        
        // Test reading the first chapter
        val chapterHtml = parser.getChapterContent(0, content!!)
        
        assertNotNull("Chapter content should not be null", chapterHtml)
        assertTrue("Chapter content should not be empty", chapterHtml?.isNotEmpty() == true)
        
        println("Second EPUB parsed and first chapter loaded successfully:")
        println("  Title: ${content.title}")
        println("  Chapter length: ${chapterHtml?.length} characters")
    }

    @Test
    fun testInvalidChapterIndex() {
        var testFile = File("Fight. Level. Survive._ An Isekai LitRPG Adventure_nodrm.epub")
        if (!testFile.exists()) {
            testFile = File("../Fight. Level. Survive._ An Isekai LitRPG Adventure_nodrm.epub")
        }
        if (!testFile.exists()) {
            testFile = File("../../Fight. Level. Survive._ An Isekai LitRPG Adventure_nodrm.epub")
        }
        
        // Skip test if file doesn't exist (for CI environments)
        if (!testFile.exists()) {
            println("Test EPUB file not found, skipping test")
            return
        }
        
        val parser = EpubParser(testFile)
        val content = parser.parse()
        
        assertNotNull("EPUB content should not be null", content)
        
        // Test with invalid chapter indices
        val negativeChapter = parser.getChapterContent(-1, content!!)
        assertNull("Negative index should return null", negativeChapter)
        
        val tooLargeChapter = parser.getChapterContent(9999, content)
        assertNull("Out of bounds index should return null", tooLargeChapter)
        
        println("Invalid index handling works correctly")
    }
}
