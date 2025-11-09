package com.rifters.ebookreader.util

import android.util.Log
import org.xmlpull.v1.XmlPullParser
import org.xmlpull.v1.XmlPullParserFactory
import java.io.File
import java.io.StringReader

/**
 * Parser for FictionBook 2 (.fb2) format
 * FB2 is an XML-based e-book format
 */
object Fb2Parser {
    
    private const val TAG = "Fb2Parser"
    
    data class Fb2Content(
        val title: String,
        val author: String,
        val htmlContent: String,
        val metadata: Fb2Metadata
    )
    
    data class Fb2Metadata(
        val genre: String? = null,
        val publisher: String? = null,
        val publishYear: String? = null,
        val language: String? = null,
        val isbn: String? = null
    )
    
    fun parse(file: File): Fb2Content? {
        try {
            val xmlContent = file.readText(Charsets.UTF_8)
            
            val factory = XmlPullParserFactory.newInstance()
            factory.isNamespaceAware = true
            val parser = factory.newPullParser()
            parser.setInput(StringReader(xmlContent))
            
            var title = ""
            var author = ""
            val contentBuilder = StringBuilder()
            var genre: String? = null
            var publisher: String? = null
            var publishYear: String? = null
            var language: String? = null
            var isbn: String? = null
            
            var eventType = parser.eventType
            var inBody = false
            var inTitle = false
            var inAuthor = false
            var inGenre = false
            var inPublisher = false
            var inYear = false
            var inLang = false
            var inIsbn = false
            var currentTag = ""
            
            contentBuilder.append("<html><head><meta charset='UTF-8'></head><body>")
            
            while (eventType != XmlPullParser.END_DOCUMENT) {
                when (eventType) {
                    XmlPullParser.START_TAG -> {
                        currentTag = parser.name
                        when (currentTag) {
                            "body" -> inBody = true
                            "book-title" -> inTitle = true
                            "author" -> inAuthor = true
                            "genre" -> inGenre = true
                            "publisher" -> inPublisher = true
                            "year" -> inYear = true
                            "lang" -> inLang = true
                            "isbn" -> inIsbn = true
                            "section" -> if (inBody) contentBuilder.append("<div class='section'>")
                            "title" -> if (inBody) contentBuilder.append("<h2>")
                            "p" -> if (inBody) contentBuilder.append("<p>")
                            "emphasis" -> if (inBody) contentBuilder.append("<em>")
                            "strong" -> if (inBody) contentBuilder.append("<strong>")
                            "subtitle" -> if (inBody) contentBuilder.append("<h3>")
                            "v" -> if (inBody) contentBuilder.append("<p class='verse'>")
                            "empty-line" -> if (inBody) contentBuilder.append("<br/>")
                        }
                    }
                    XmlPullParser.TEXT -> {
                        val text = parser.text
                        when {
                            inTitle && text.isNotBlank() -> title = text.trim()
                            inAuthor && text.isNotBlank() -> {
                                if (author.isNotEmpty()) author += " "
                                author += text.trim()
                            }
                            inGenre && text.isNotBlank() -> genre = text.trim()
                            inPublisher && text.isNotBlank() -> publisher = text.trim()
                            inYear && text.isNotBlank() -> publishYear = text.trim()
                            inLang && text.isNotBlank() -> language = text.trim()
                            inIsbn && text.isNotBlank() -> isbn = text.trim()
                            inBody && text.isNotBlank() -> {
                                contentBuilder.append(text)
                            }
                        }
                    }
                    XmlPullParser.END_TAG -> {
                        when (parser.name) {
                            "body" -> inBody = false
                            "book-title" -> inTitle = false
                            "author" -> inAuthor = false
                            "genre" -> inGenre = false
                            "publisher" -> inPublisher = false
                            "year" -> inYear = false
                            "lang" -> inLang = false
                            "isbn" -> inIsbn = false
                            "section" -> if (inBody) contentBuilder.append("</div>")
                            "title" -> if (inBody) contentBuilder.append("</h2>")
                            "p" -> if (inBody) contentBuilder.append("</p>")
                            "emphasis" -> if (inBody) contentBuilder.append("</em>")
                            "strong" -> if (inBody) contentBuilder.append("</strong>")
                            "subtitle" -> if (inBody) contentBuilder.append("</h3>")
                            "v" -> if (inBody) contentBuilder.append("</p>")
                        }
                    }
                }
                eventType = parser.next()
            }
            
            contentBuilder.append("</body></html>")
            
            val metadata = Fb2Metadata(
                genre = genre,
                publisher = publisher,
                publishYear = publishYear,
                language = language,
                isbn = isbn
            )
            
            return Fb2Content(
                title = title.ifEmpty { file.nameWithoutExtension },
                author = author.ifEmpty { "Unknown" },
                htmlContent = contentBuilder.toString(),
                metadata = metadata
            )
            
        } catch (e: Exception) {
            Log.e(TAG, "Error parsing FB2 file", e)
            return null
        }
    }
}
