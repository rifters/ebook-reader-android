package com.rifters.ebookreader.util

import android.util.Log
import java.io.File

/**
 * Simple Markdown to HTML converter
 * Supports basic Markdown syntax
 */
object MarkdownParser {
    
    private const val TAG = "MarkdownParser"
    
    fun parseToHtml(file: File): String {
        return try {
            val markdown = file.readText(Charsets.UTF_8)
            convertMarkdownToHtml(markdown)
        } catch (e: Exception) {
            Log.e(TAG, "Error parsing Markdown file", e)
            "<html><body><p>Error loading Markdown file</p></body></html>"
        }
    }
    
    private fun convertMarkdownToHtml(markdown: String): String {
        val html = StringBuilder()
        html.append("<html><head>")
        html.append("<meta charset='UTF-8'>")
        html.append("<style>")
        html.append("body { padding: 16px; line-height: 1.6; }")
        html.append("pre { background: #f4f4f4; padding: 12px; border-radius: 4px; overflow-x: auto; }")
        html.append("code { background: #f4f4f4; padding: 2px 6px; border-radius: 3px; }")
        html.append("blockquote { border-left: 4px solid #ddd; margin-left: 0; padding-left: 16px; color: #666; }")
        html.append("table { border-collapse: collapse; width: 100%; }")
        html.append("th, td { border: 1px solid #ddd; padding: 8px; text-align: left; }")
        html.append("th { background: #f4f4f4; }")
        html.append("img { max-width: 100%; height: auto; }")
        html.append("</style>")
        html.append("</head><body>")
        
        val lines = markdown.split("\n")
        var inCodeBlock = false
        var inList = false
        var inBlockquote = false
        
        for (line in lines) {
            var processedLine = line
            
            // Code blocks
            if (line.trim().startsWith("```")) {
                if (inCodeBlock) {
                    html.append("</pre>")
                    inCodeBlock = false
                } else {
                    html.append("<pre><code>")
                    inCodeBlock = true
                }
                continue
            }
            
            if (inCodeBlock) {
                html.append(escapeHtml(line)).append("\n")
                continue
            }
            
            // Headers
            when {
                line.startsWith("# ") -> {
                    processedLine = "<h1>${escapeHtml(line.substring(2))}</h1>"
                }
                line.startsWith("## ") -> {
                    processedLine = "<h2>${escapeHtml(line.substring(3))}</h2>"
                }
                line.startsWith("### ") -> {
                    processedLine = "<h3>${escapeHtml(line.substring(4))}</h3>"
                }
                line.startsWith("#### ") -> {
                    processedLine = "<h4>${escapeHtml(line.substring(5))}</h4>"
                }
                line.startsWith("##### ") -> {
                    processedLine = "<h5>${escapeHtml(line.substring(6))}</h5>"
                }
                line.startsWith("###### ") -> {
                    processedLine = "<h6>${escapeHtml(line.substring(7))}</h6>"
                }
                // Lists
                line.trim().startsWith("- ") || line.trim().startsWith("* ") -> {
                    if (!inList) {
                        html.append("<ul>")
                        inList = true
                    }
                    processedLine = "<li>${processInlineMarkdown(line.trim().substring(2))}</li>"
                }
                line.trim().matches(Regex("^\\d+\\.\\s.*")) -> {
                    if (!inList) {
                        html.append("<ol>")
                        inList = true
                    }
                    val content = line.trim().substringAfter(". ")
                    processedLine = "<li>${processInlineMarkdown(content)}</li>"
                }
                // Blockquotes
                line.trim().startsWith("> ") -> {
                    if (!inBlockquote) {
                        html.append("<blockquote>")
                        inBlockquote = true
                    }
                    processedLine = processInlineMarkdown(line.trim().substring(2))
                }
                // Horizontal rule
                line.trim() == "---" || line.trim() == "***" -> {
                    processedLine = "<hr/>"
                }
                // Empty line - close lists and blockquotes
                line.trim().isEmpty() -> {
                    if (inList) {
                        html.append("</ul>")
                        inList = false
                    }
                    if (inBlockquote) {
                        html.append("</blockquote>")
                        inBlockquote = false
                    }
                    processedLine = "<br/>"
                }
                // Regular paragraph
                else -> {
                    if (inList) {
                        html.append("</ul>")
                        inList = false
                    }
                    if (inBlockquote) {
                        html.append("</blockquote>")
                        inBlockquote = false
                    }
                    if (line.isNotBlank()) {
                        processedLine = "<p>${processInlineMarkdown(line)}</p>"
                    }
                }
            }
            
            html.append(processedLine)
        }
        
        // Close any open tags
        if (inCodeBlock) html.append("</code></pre>")
        if (inList) html.append("</ul>")
        if (inBlockquote) html.append("</blockquote>")
        
        html.append("</body></html>")
        return html.toString()
    }
    
    private fun processInlineMarkdown(text: String): String {
        var result = escapeHtml(text)
        
        // Bold **text** or __text__
        result = result.replace(Regex("\\*\\*(.+?)\\*\\*"), "<strong>$1</strong>")
        result = result.replace(Regex("__(.+?)__"), "<strong>$1</strong>")
        
        // Italic *text* or _text_
        result = result.replace(Regex("\\*(.+?)\\*"), "<em>$1</em>")
        result = result.replace(Regex("_(.+?)_"), "<em>$1</em>")
        
        // Inline code `code`
        result = result.replace(Regex("`(.+?)`"), "<code>$1</code>")
        
        // Links [text](url)
        result = result.replace(Regex("\\[(.+?)\\]\\((.+?)\\)"), "<a href='$2'>$1</a>")
        
        // Images ![alt](url)
        result = result.replace(Regex("!\\[(.+?)\\]\\((.+?)\\)"), "<img src='$2' alt='$1'/>")
        
        return result
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
