package com.rifters.ebookreader.util

import android.util.Log
import com.rifters.ebookreader.model.TableOfContentsItem
import org.xmlpull.v1.XmlPullParser
import org.xmlpull.v1.XmlPullParserFactory
import java.io.File
import java.io.StringReader
import java.util.zip.ZipFile

/**
 * EPUB Parser utility for parsing EPUB structure, metadata, spine, and table of contents.
 * 
 * EPUB structure:
 * - META-INF/container.xml: Points to the OPF file
 * - OPF file (content.opf): Contains metadata, manifest, spine
 * - NCX file (toc.ncx) or NAV file (nav.xhtml): Table of contents
 */
class EpubParser(private val epubFile: File) {
    
    private val TAG = "EpubParser"
    private var zipFile: ZipFile? = null
    
    data class EpubContent(
        val title: String = "",
        val author: String = "",
        val spine: List<SpineItem> = emptyList(),
        val manifest: Map<String, ManifestItem> = emptyMap(),
        val toc: List<TableOfContentsItem> = emptyList(),
        val opfBasePath: String = ""
    )
    
    data class SpineItem(
        val idref: String,
        val linear: Boolean = true
    )
    
    data class ManifestItem(
        val id: String,
        val href: String,
        val mediaType: String
    )
    
    /**
     * Parse the EPUB file and extract all relevant information
     */
    fun parse(): EpubContent? {
        try {
            zipFile = ZipFile(epubFile)
            
            // Step 1: Find the OPF file location from container.xml
            val opfPath = findOpfPath() ?: run {
                Log.e(TAG, "Could not find OPF file path")
                return null
            }
            
            // Get the base path of the OPF file
            val opfBasePath = if (opfPath.contains("/")) {
                opfPath.substring(0, opfPath.lastIndexOf("/") + 1)
            } else {
                ""
            }
            
            // Step 2: Parse the OPF file
            val opfContent = readFileFromZip(opfPath) ?: run {
                Log.e(TAG, "Could not read OPF file")
                return null
            }
            
            val (title, author, manifest, spine) = parseOpf(opfContent)
            
            // Step 3: Find and parse the TOC (NCX or NAV file)
            val tocFile = findTocFile(manifest, opfBasePath)
            val toc = if (tocFile != null) {
                parseToc(tocFile, opfBasePath)
            } else {
                Log.w(TAG, "No TOC file found")
                emptyList()
            }
            
            return EpubContent(
                title = title,
                author = author,
                spine = spine,
                manifest = manifest,
                toc = toc,
                opfBasePath = opfBasePath
            )
        } catch (e: Exception) {
            Log.e(TAG, "Error parsing EPUB", e)
            return null
        } finally {
            zipFile?.close()
        }
    }
    
    /**
     * Find the path to the OPF file from META-INF/container.xml
     */
    private fun findOpfPath(): String? {
        val containerXml = readFileFromZip("META-INF/container.xml") ?: return null
        
        try {
            val factory = XmlPullParserFactory.newInstance()
            val parser = factory.newPullParser()
            parser.setInput(StringReader(containerXml))
            
            var eventType = parser.eventType
            while (eventType != XmlPullParser.END_DOCUMENT) {
                if (eventType == XmlPullParser.START_TAG && parser.name == "rootfile") {
                    val fullPath = parser.getAttributeValue(null, "full-path")
                    if (fullPath != null) {
                        return fullPath
                    }
                }
                eventType = parser.next()
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error parsing container.xml", e)
        }
        
        return null
    }
    
    /**
     * Parse the OPF file to extract metadata, manifest, and spine
     */
    private fun parseOpf(opfContent: String): OpfData {
        var title = ""
        var author = ""
        val manifest = mutableMapOf<String, ManifestItem>()
        val spine = mutableListOf<SpineItem>()
        
        try {
            val factory = XmlPullParserFactory.newInstance()
            val parser = factory.newPullParser()
            parser.setInput(StringReader(opfContent))
            
            var eventType = parser.eventType
            var currentSection = ""
            
            while (eventType != XmlPullParser.END_DOCUMENT) {
                when (eventType) {
                    XmlPullParser.START_TAG -> {
                        when (parser.name) {
                            "metadata" -> currentSection = "metadata"
                            "manifest" -> currentSection = "manifest"
                            "spine" -> currentSection = "spine"
                            "dc:title", "title" -> {
                                if (currentSection == "metadata" && parser.next() == XmlPullParser.TEXT) {
                                    title = parser.text ?: ""
                                }
                            }
                            "dc:creator", "creator" -> {
                                if (currentSection == "metadata" && parser.next() == XmlPullParser.TEXT) {
                                    author = parser.text ?: ""
                                }
                            }
                            "item" -> {
                                if (currentSection == "manifest") {
                                    val id = parser.getAttributeValue(null, "id") ?: ""
                                    val href = parser.getAttributeValue(null, "href") ?: ""
                                    val mediaType = parser.getAttributeValue(null, "media-type") ?: ""
                                    
                                    if (id.isNotEmpty() && href.isNotEmpty()) {
                                        manifest[id] = ManifestItem(id, href, mediaType)
                                    }
                                }
                            }
                            "itemref" -> {
                                if (currentSection == "spine") {
                                    val idref = parser.getAttributeValue(null, "idref") ?: ""
                                    val linear = parser.getAttributeValue(null, "linear") != "no"
                                    
                                    if (idref.isNotEmpty()) {
                                        spine.add(SpineItem(idref, linear))
                                    }
                                }
                            }
                        }
                    }
                    XmlPullParser.END_TAG -> {
                        when (parser.name) {
                            "metadata", "manifest", "spine" -> currentSection = ""
                        }
                    }
                }
                eventType = parser.next()
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error parsing OPF", e)
        }
        
        return OpfData(title, author, manifest, spine)
    }
    
    /**
     * Find the TOC file (NCX or NAV) from the manifest
     */
    private fun findTocFile(manifest: Map<String, ManifestItem>, basePath: String): String? {
        // Look for NCX file
        val ncxItem = manifest.values.find { 
            it.mediaType == "application/x-dtbncx+xml" || it.href.endsWith(".ncx")
        }
        if (ncxItem != null) {
            return basePath + ncxItem.href
        }
        
        // Look for NAV file (EPUB 3)
        val navItem = manifest.values.find { 
            it.mediaType == "application/xhtml+xml" && 
            (it.href.contains("nav") || it.href.contains("toc"))
        }
        if (navItem != null) {
            return basePath + navItem.href
        }
        
        return null
    }
    
    /**
     * Parse the TOC file (NCX or NAV format)
     */
    private fun parseToc(tocPath: String, basePath: String): List<TableOfContentsItem> {
        val tocContent = readFileFromZip(tocPath) ?: return emptyList()
        
        return if (tocPath.endsWith(".ncx")) {
            parseNcxToc(tocContent, basePath)
        } else {
            parseNavToc(tocContent, basePath)
        }
    }
    
    /**
     * Parse NCX (EPUB 2) table of contents
     */
    private fun parseNcxToc(ncxContent: String, basePath: String): List<TableOfContentsItem> {
        val tocItems = mutableListOf<TableOfContentsItem>()
        
        try {
            val factory = XmlPullParserFactory.newInstance()
            val parser = factory.newPullParser()
            parser.setInput(StringReader(ncxContent))
            
            var eventType = parser.eventType
            var currentNavPoint: NavPoint? = null
            var inNavLabel = false
            var inText = false
            
            while (eventType != XmlPullParser.END_DOCUMENT) {
                when (eventType) {
                    XmlPullParser.START_TAG -> {
                        when (parser.name) {
                            "navPoint" -> {
                                val id = parser.getAttributeValue(null, "id") ?: ""
                                val playOrder = parser.getAttributeValue(null, "playOrder")?.toIntOrNull() ?: tocItems.size
                                currentNavPoint = NavPoint(id, playOrder)
                            }
                            "navLabel" -> inNavLabel = true
                            "text" -> {
                                if (inNavLabel) {
                                    inText = true
                                }
                            }
                            "content" -> {
                                val src = parser.getAttributeValue(null, "src") ?: ""
                                currentNavPoint?.href = src
                            }
                        }
                    }
                    XmlPullParser.TEXT -> {
                        if (inText && currentNavPoint != null) {
                            currentNavPoint.title = parser.text ?: ""
                        }
                    }
                    XmlPullParser.END_TAG -> {
                        when (parser.name) {
                            "navPoint" -> {
                                currentNavPoint?.let { navPoint ->
                                    tocItems.add(
                                        TableOfContentsItem(
                                            title = navPoint.title,
                                            href = basePath + navPoint.href,
                                            page = navPoint.playOrder,
                                            level = 0
                                        )
                                    )
                                }
                                currentNavPoint = null
                            }
                            "navLabel" -> inNavLabel = false
                            "text" -> inText = false
                        }
                    }
                }
                eventType = parser.next()
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error parsing NCX TOC", e)
        }
        
        return tocItems
    }
    
    /**
     * Parse NAV (EPUB 3) table of contents
     */
    private fun parseNavToc(navContent: String, basePath: String): List<TableOfContentsItem> {
        val tocItems = mutableListOf<TableOfContentsItem>()
        
        try {
            val factory = XmlPullParserFactory.newInstance()
            val parser = factory.newPullParser()
            parser.setInput(StringReader(navContent))
            
            var eventType = parser.eventType
            var inTocNav = false
            var inAnchor = false
            var currentHref = ""
            var currentTitle = ""
            var itemCount = 0
            
            while (eventType != XmlPullParser.END_DOCUMENT) {
                when (eventType) {
                    XmlPullParser.START_TAG -> {
                        when (parser.name.lowercase()) {
                            "nav" -> {
                                val navType = parser.getAttributeValue(null, "epub:type") 
                                    ?: parser.getAttributeValue(null, "type")
                                if (navType == "toc") {
                                    inTocNav = true
                                }
                            }
                            "a" -> {
                                if (inTocNav) {
                                    inAnchor = true
                                    currentHref = parser.getAttributeValue(null, "href") ?: ""
                                }
                            }
                        }
                    }
                    XmlPullParser.TEXT -> {
                        if (inAnchor) {
                            currentTitle = parser.text?.trim() ?: ""
                        }
                    }
                    XmlPullParser.END_TAG -> {
                        when (parser.name.lowercase()) {
                            "nav" -> inTocNav = false
                            "a" -> {
                                if (inAnchor && currentTitle.isNotEmpty() && currentHref.isNotEmpty()) {
                                    tocItems.add(
                                        TableOfContentsItem(
                                            title = currentTitle,
                                            href = basePath + currentHref,
                                            page = itemCount++,
                                            level = 0
                                        )
                                    )
                                }
                                inAnchor = false
                                currentHref = ""
                                currentTitle = ""
                            }
                        }
                    }
                }
                eventType = parser.next()
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error parsing NAV TOC", e)
        }
        
        return tocItems
    }
    
    /**
     * Get chapter content by spine index
     */
    fun getChapterContent(spineIndex: Int, epubContent: EpubContent): String? {
        if (spineIndex < 0 || spineIndex >= epubContent.spine.size) {
            return null
        }
        
        val spineItem = epubContent.spine[spineIndex]
        val manifestItem = epubContent.manifest[spineItem.idref] ?: return null
        val href = epubContent.opfBasePath + manifestItem.href
        
        return readFileFromZip(href)
    }
    
    /**
     * Read a file from the ZIP archive
     */
    private fun readFileFromZip(path: String): String? {
        try {
            val entry = zipFile?.getEntry(path) ?: return null
            val inputStream = zipFile?.getInputStream(entry) ?: return null
            return inputStream.bufferedReader().use { it.readText() }
        } catch (e: Exception) {
            Log.e(TAG, "Error reading file from ZIP: $path", e)
            return null
        }
    }
    
    private data class OpfData(
        val title: String,
        val author: String,
        val manifest: Map<String, ManifestItem>,
        val spine: List<SpineItem>
    )
    
    private data class NavPoint(
        val id: String,
        val playOrder: Int,
        var title: String = "",
        var href: String = ""
    )
}
