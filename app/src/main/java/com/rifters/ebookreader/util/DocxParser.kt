package com.rifters.ebookreader.util

import android.util.Log
import org.xmlpull.v1.XmlPullParser
import org.xmlpull.v1.XmlPullParserFactory
import java.io.File
import java.io.StringReader
import java.util.zip.ZipFile

/**
 * Simple parser for DOCX (Microsoft Word) documents
 * Extracts text from document.xml without external dependencies
 */
object DocxParser {
    
    private const val TAG = "DocxParser"
    
    data class DocxContent(
        val title: String,
        val htmlContent: String,
        val metadata: DocxMetadata
    )
    
    data class DocxMetadata(
        val author: String? = null,
        val subject: String? = null,
        val keywords: String? = null,
        val created: String? = null,
        val modified: String? = null
    )
    
    /**
     * Parse DOCX file and convert to HTML
     * DOCX is a ZIP file containing XML documents
     */
    fun parse(file: File): DocxContent? {
        return try {
            val zipFile = ZipFile(file)
            
            // Extract main document text from word/document.xml
            val documentEntry = zipFile.getEntry("word/document.xml")
            if (documentEntry == null) {
                zipFile.close()
                return null
            }
            
            val documentXml = zipFile.getInputStream(documentEntry).bufferedReader().use { it.readText() }
            
            // Extract metadata from docProps/core.xml if available
            var author: String? = null
            var subject: String? = null
            var title: String? = null
            
            val corePropsEntry = zipFile.getEntry("docProps/core.xml")
            if (corePropsEntry != null) {
                val coreXml = zipFile.getInputStream(corePropsEntry).bufferedReader().use { it.readText() }
                val metadata = parseCoreProperties(coreXml)
                author = metadata["creator"]
                subject = metadata["subject"]
                title = metadata["title"]
            }
            
            zipFile.close()
            
            // Parse the document XML and extract text
            val html = parseDocumentXml(documentXml)
            
            DocxContent(
                title = title ?: file.nameWithoutExtension,
                htmlContent = html,
                metadata = DocxMetadata(
                    author = author,
                    subject = subject,
                    keywords = null,
                    created = null,
                    modified = null
                )
            )
        } catch (e: Exception) {
            Log.e(TAG, "Error parsing DOCX file", e)
            null
        }
    }
    
    private fun parseCoreProperties(xml: String): Map<String, String> {
        val properties = mutableMapOf<String, String>()
        
        try {
            val factory = XmlPullParserFactory.newInstance()
            val parser = factory.newPullParser()
            parser.setInput(StringReader(xml))
            
            var eventType = parser.eventType
            var currentTag = ""
            
            while (eventType != XmlPullParser.END_DOCUMENT) {
                when (eventType) {
                    XmlPullParser.START_TAG -> {
                        currentTag = parser.name
                    }
                    XmlPullParser.TEXT -> {
                        val text = parser.text?.trim() ?: ""
                        if (text.isNotEmpty()) {
                            when (currentTag) {
                                "creator", "dc:creator" -> properties["creator"] = text
                                "title", "dc:title" -> properties["title"] = text
                                "subject", "dc:subject" -> properties["subject"] = text
                                "description", "dc:description" -> properties["description"] = text
                            }
                        }
                    }
                }
                eventType = parser.next()
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error parsing core properties", e)
        }
        
        return properties
    }
    
    private fun parseDocumentXml(xml: String): String {
        val html = StringBuilder()
        html.append("<html><head><meta charset='UTF-8'>")
        html.append("<style>")
        html.append("body { font-family: Arial, sans-serif; line-height: 1.6; padding: 16px; }")
        html.append("p { margin: 0.5em 0; }")
        html.append("</style>")
        html.append("</head><body>")
        
        try {
            val factory = XmlPullParserFactory.newInstance()
            val parser = factory.newPullParser()
            parser.setInput(StringReader(xml))
            
            var eventType = parser.eventType
            var inParagraph = false
            val paragraphText = StringBuilder()
            
            while (eventType != XmlPullParser.END_DOCUMENT) {
                when (eventType) {
                    XmlPullParser.START_TAG -> {
                        when (parser.name) {
                            "w:p" -> {
                                inParagraph = true
                                paragraphText.clear()
                            }
                            "w:t" -> {
                                // Text element - get the text content
                            }
                        }
                    }
                    XmlPullParser.TEXT -> {
                        if (inParagraph) {
                            val text = parser.text?.trim() ?: ""
                            if (text.isNotEmpty()) {
                                paragraphText.append(escapeHtml(text))
                                paragraphText.append(" ")
                            }
                        }
                    }
                    XmlPullParser.END_TAG -> {
                        when (parser.name) {
                            "w:p" -> {
                                if (paragraphText.isNotEmpty()) {
                                    html.append("<p>")
                                    html.append(paragraphText.toString().trim())
                                    html.append("</p>")
                                } else {
                                    html.append("<br/>")
                                }
                                inParagraph = false
                            }
                        }
                    }
                }
                eventType = parser.next()
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error parsing document XML", e)
        }
        
        html.append("</body></html>")
        return html.toString()
    }
    
    private fun escapeHtml(text: String): String {
        return text
            .replace("&", "&amp;")
            .replace("<", "&lt;")
            .replace(">", "&gt;")
            .replace("\"", "&quot;")
            .replace("'", "&#39;")
    }
}
