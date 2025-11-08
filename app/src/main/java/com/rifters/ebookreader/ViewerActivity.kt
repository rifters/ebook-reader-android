package com.rifters.ebookreader

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Typeface
import android.graphics.pdf.PdfRenderer
import android.os.Bundle
import android.os.ParcelFileDescriptor
import android.speech.tts.TextToSpeech
import android.util.TypedValue
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.webkit.WebView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import com.github.junrar.Archive
import com.github.junrar.rarfile.FileHeader
import com.rifters.ebookreader.databinding.ActivityViewerBinding
import com.rifters.ebookreader.model.Bookmark
import com.rifters.ebookreader.model.ReadingPreferences
import com.rifters.ebookreader.util.BitmapCache
import com.rifters.ebookreader.util.PreferencesManager
import com.rifters.ebookreader.viewmodel.BookViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.apache.commons.compress.archivers.zip.ZipFile as ApacheZipFile
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.RandomAccessFile
import java.util.Locale
import java.util.zip.ZipFile

class ViewerActivity : AppCompatActivity(), TextToSpeech.OnInitListener {
    
    private lateinit var binding: ActivityViewerBinding
    private lateinit var bookViewModel: BookViewModel
    private lateinit var preferencesManager: PreferencesManager
    private var currentBook: Book? = null
    private var currentPage: Int = 0
    private var currentProgressPercent: Float = 0f
    
    // PDF variables
    private var pdfRenderer: PdfRenderer? = null
    private var pdfFileDescriptor: ParcelFileDescriptor? = null
    private var totalPdfPages: Int = 0
    
    // Comic book (CBZ/CBR) variables
    private var comicImagePaths: MutableList<Pair<String, Any>> = mutableListOf() // Pair of (path, source)
    private var comicArchiveFile: File? = null
    private var totalComicPages: Int = 0
    
    // Bitmap cache for page caching
    private val bitmapCache = BitmapCache.getInstance()
    
    // TTS variables
    private var textToSpeech: TextToSpeech? = null
    private var isTtsInitialized = false
    private var isTtsPlaying = false
    private var currentTextContent: String = ""
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityViewerBinding.inflate(layoutInflater)
        setContentView(binding.root)
        
        bookViewModel = ViewModelProvider(this)[BookViewModel::class.java]
        preferencesManager = PreferencesManager(this)
        
        setupToolbar()
        setupBottomBar()
        setupTTS()
        
        // Apply saved theme on activity load
        val preferences = preferencesManager.getReadingPreferences()
        applyThemeToUI(preferences)
        
        loadBookFromIntent()
    }
    
    private fun setupTTS() {
        textToSpeech = TextToSpeech(this, this)
    }
    
    override fun onInit(status: Int) {
        if (status == TextToSpeech.SUCCESS) {
            val result = textToSpeech?.setLanguage(Locale.getDefault())
            isTtsInitialized = result != TextToSpeech.LANG_MISSING_DATA && 
                              result != TextToSpeech.LANG_NOT_SUPPORTED
            if (!isTtsInitialized) {
                Toast.makeText(this, "TTS language not supported", Toast.LENGTH_SHORT).show()
            }
        } else {
            Toast.makeText(this, "TTS initialization failed", Toast.LENGTH_SHORT).show()
        }
    }
    
    private fun setupToolbar() {
        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        
        binding.toolbar.setNavigationOnClickListener {
            finish()
        }
    }
    
    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.viewer_menu, menu)
        updateTtsMenuIcon(menu)
        return true
    }
    
    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.action_view_bookmarks -> {
                showBookmarks()
                true
            }
            R.id.action_tts_play -> {
                toggleTTS()
                true
            }
            R.id.action_customize_reading -> {
                showReadingSettings()
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }
    
    private fun updateTtsMenuIcon(menu: Menu) {
        val ttsItem = menu.findItem(R.id.action_tts_play)
        if (isTtsPlaying) {
            ttsItem?.title = getString(R.string.tts_pause)
            ttsItem?.setIcon(android.R.drawable.ic_media_pause)
        } else {
            ttsItem?.title = getString(R.string.tts_play)
            ttsItem?.setIcon(android.R.drawable.ic_media_play)
        }
    }
    
    private fun toggleTTS() {
        if (!isTtsInitialized) {
            Toast.makeText(this, "TTS not initialized", Toast.LENGTH_SHORT).show()
            return
        }
        
        if (isTtsPlaying) {
            pauseTTS()
        } else {
            playTTS()
        }
        invalidateOptionsMenu()
    }
    
    private fun playTTS() {
        if (currentTextContent.isNotEmpty()) {
            textToSpeech?.speak(currentTextContent, TextToSpeech.QUEUE_FLUSH, null, "tts_id")
            isTtsPlaying = true
            Toast.makeText(this, "TTS started", Toast.LENGTH_SHORT).show()
        } else {
            Toast.makeText(this, "No text content to read", Toast.LENGTH_SHORT).show()
        }
    }
    
    private fun pauseTTS() {
        textToSpeech?.stop()
        isTtsPlaying = false
        Toast.makeText(this, "TTS stopped", Toast.LENGTH_SHORT).show()
    }
    
    private fun setupBottomBar() {
        binding.btnPreviousChapter.setOnClickListener {
            previousPage()
        }
        
        binding.btnNextChapter.setOnClickListener {
            nextPage()
        }
        
        binding.btnBookmark.setOnClickListener {
            bookmarkCurrentPage()
        }
    }
    
    private fun loadBookFromIntent() {
        val bookId = intent.getLongExtra("book_id", -1L)
        val bookPath = intent.getStringExtra("book_path")
        val bookTitle = intent.getStringExtra("book_title")
        
        supportActionBar?.title = bookTitle
        
        if (bookPath != null) {
            lifecycleScope.launch {
                currentBook = if (bookId != -1L) {
                    bookViewModel.getBookById(bookId)
                } else {
                    null
                }
                
                currentPage = currentBook?.currentPage ?: 0
                currentProgressPercent = currentBook?.progressPercentage ?: 0f
                loadBook(bookPath)
            }
        } else {
            Toast.makeText(this, getString(R.string.error_opening_file), Toast.LENGTH_SHORT).show()
            finish()
        }
    }
    
    private suspend fun loadBook(filePath: String) {
        withContext(Dispatchers.Main) {
            binding.loadingProgressBar.visibility = View.VISIBLE
        }
        
        try {
            val file = File(filePath)
            if (!file.exists()) {
                withContext(Dispatchers.Main) {
                    Toast.makeText(
                        this@ViewerActivity,
                        getString(R.string.error_opening_file),
                        Toast.LENGTH_SHORT
                    ).show()
                    finish()
                }
                return
            }
            
            when {
                filePath.endsWith(".pdf", ignoreCase = true) -> {
                    loadPdf(file)
                }
                filePath.endsWith(".epub", ignoreCase = true) -> {
                    loadEpub(file)
                }
                filePath.endsWith(".mobi", ignoreCase = true) -> {
                    loadMobi(file)
                }
                filePath.endsWith(".cbz", ignoreCase = true) -> {
                    loadCbz(file)
                }
                filePath.endsWith(".cbr", ignoreCase = true) -> {
                    loadCbr(file)
                }
                filePath.endsWith(".txt", ignoreCase = true) -> {
                    loadText(file)
                }
                else -> {
                    withContext(Dispatchers.Main) {
                        Toast.makeText(
                            this@ViewerActivity,
                            getString(R.string.error_unsupported_format),
                            Toast.LENGTH_SHORT
                        ).show()
                        finish()
                    }
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
            withContext(Dispatchers.Main) {
                Toast.makeText(
                    this@ViewerActivity,
                    getString(R.string.error_opening_file),
                    Toast.LENGTH_SHORT
                ).show()
                finish()
            }
        }
    }
    
    private suspend fun loadPdf(file: File) {
        withContext(Dispatchers.IO) {
            try {
                pdfFileDescriptor = ParcelFileDescriptor.open(file, ParcelFileDescriptor.MODE_READ_ONLY)
                pdfRenderer = PdfRenderer(pdfFileDescriptor!!)
                totalPdfPages = pdfRenderer?.pageCount ?: 0
                
                withContext(Dispatchers.Main) {
                    binding.loadingProgressBar.visibility = View.GONE
                    binding.webView.visibility = View.GONE
                    binding.scrollView.visibility = View.GONE
                    binding.pdfImageView.visibility = View.VISIBLE
                    binding.pdfImageView.setImageBitmap(null)
                    
                    renderPdfPage(currentPage)
                    
                    // Apply theme background
                    val preferences = preferencesManager.getReadingPreferences()
                    applyThemeToUI(preferences)
                }
            } catch (e: Exception) {
                e.printStackTrace()
                withContext(Dispatchers.Main) {
                    binding.apply {
                        loadingProgressBar.visibility = View.GONE
                        pdfImageView.visibility = View.GONE
                        scrollView.visibility = View.VISIBLE
                        textView.visibility = View.VISIBLE
                        textView.text = "Error loading PDF: ${e.message}\n\n" +
                                "File: ${file.name}\n" +
                                "Size: ${file.length() / 1024} KB"
                    }
                }
            }
        }
    }
    
    private fun renderPdfPage(pageIndex: Int) {
        lifecycleScope.launch(Dispatchers.IO) {
            try {
                val renderer = pdfRenderer ?: return@launch
                if (pageIndex < 0 || pageIndex >= totalPdfPages) return@launch
                
                // Check cache first
                val cacheKey = "pdf_${currentBook?.id}_$pageIndex"
                val cachedBitmap = bitmapCache.getBitmap(cacheKey)
                
                if (cachedBitmap != null && !cachedBitmap.isRecycled) {
                    withContext(Dispatchers.Main) {
                        displayPdfBitmap(cachedBitmap, pageIndex)
                    }
                    return@launch
                }
                
                val page = renderer.openPage(pageIndex)
                
                // Create a bitmap with optimized resolution (1.5x instead of 2x)
                val bitmap = Bitmap.createBitmap(
                    (page.width * 1.5).toInt(),
                    (page.height * 1.5).toInt(),
                    Bitmap.Config.ARGB_8888
                )
                
                page.render(bitmap, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
                page.close()
                
                // Cache the bitmap
                bitmapCache.put(cacheKey, bitmap)
                
                withContext(Dispatchers.Main) {
                    displayPdfBitmap(bitmap, pageIndex)
                }
            } catch (e: Exception) {
                e.printStackTrace()
                withContext(Dispatchers.Main) {
                    Toast.makeText(
                        this@ViewerActivity,
                        "Error rendering page: ${e.message}",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }
        }
    }
    
    private fun displayPdfBitmap(bitmap: Bitmap, pageIndex: Int) {
        binding.apply {
            pdfImageView.visibility = View.VISIBLE
            pdfImageView.setImageBitmap(bitmap)
            webView.visibility = View.GONE
            scrollView.visibility = View.GONE
            textView.visibility = View.GONE
        }
        
        currentPage = pageIndex
        updateProgress(pageIndex, totalPdfPages)
    }
    
    private suspend fun loadEpub(file: File) {
        withContext(Dispatchers.IO) {
            try {
                val zipFile = ZipFile(file)
                val entries = zipFile.entries()
                
                // Find the first HTML/XHTML content file with size limit
                val maxContentSize = 5 * 1024 * 1024 // 5MB limit
                var contentHtml = ""
                while (entries.hasMoreElements()) {
                    val entry = entries.nextElement()
                    val name = entry.name.lowercase()
                    
                    if (name.endsWith(".html") || name.endsWith(".xhtml") || name.endsWith(".htm")) {
                        val inputStream = zipFile.getInputStream(entry)
                        
                        // Check size and limit if necessary
                        contentHtml = if (entry.size > maxContentSize) {
                            val limitedContent = inputStream.bufferedReader().use { reader ->
                                val buffer = CharArray(maxContentSize)
                                val charsRead = reader.read(buffer)
                                String(buffer, 0, charsRead)
                            }
                            "$limitedContent\n<p><em>[Content truncated - showing first 5MB]</em></p>"
                        } else {
                            inputStream.bufferedReader().use { it.readText() }
                        }
                        break
                    }
                }
                
                if (contentHtml.isEmpty()) {
                    contentHtml = """
                        <html>
                        <head><meta charset="utf-8"/></head>
                        <body style="padding: 16px; font-size: 16px; line-height: 1.6;">
                        <h1>EPUB Reader</h1>
                        <p>EPUB file loaded: ${file.name}</p>
                        <p>Basic EPUB support is now available.</p>
                        <p>This is a simple EPUB parser that extracts and displays HTML content.</p>
                        </body>
                        </html>
                    """.trimIndent()
                }
                
                zipFile.close()
                
                withContext(Dispatchers.Main) {
                    binding.apply {
                        loadingProgressBar.visibility = View.GONE
                        pdfImageView.visibility = View.GONE
                        webView.visibility = View.VISIBLE
                        scrollView.visibility = View.GONE
                        textView.visibility = View.GONE
                        
                        // Configure WebView for EPUB
                        webView.settings.apply {
                            javaScriptEnabled = true
                            builtInZoomControls = true
                            displayZoomControls = false
                            loadWithOverviewMode = true
                            useWideViewPort = true
                        }
                        
                        webView.webViewClient = object : android.webkit.WebViewClient() {
                            override fun onPageFinished(view: WebView?, url: String?) {
                                super.onPageFinished(view, url)
                                val preferences = preferencesManager.getReadingPreferences()
                                applyWebViewStyles(preferences)
                            }
                        }
                        
                        webView.loadDataWithBaseURL(null, contentHtml, "text/html", "UTF-8", null)
                        currentTextContent = contentHtml // Store for TTS
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
                withContext(Dispatchers.Main) {
                    binding.apply {
                        loadingProgressBar.visibility = View.GONE
                        webView.visibility = View.VISIBLE
                        scrollView.visibility = View.GONE
                        
                        webView.settings.javaScriptEnabled = true
                        pdfImageView.visibility = View.GONE
                        val errorHtml = """
                            <html>
                            <body style="padding: 16px;">
                            <h1>EPUB Error</h1>
                            <p>Error loading EPUB file: ${e.message}</p>
                            <p>File: ${file.name}</p>
                            </body>
                            </html>
                        """.trimIndent()
                        
                        webView.loadDataWithBaseURL(null, errorHtml, "text/html", "UTF-8", null)
                    }
                }
            }
        }
    }
    
    private suspend fun loadMobi(file: File) {
        withContext(Dispatchers.IO) {
            try {
                // Basic MOBI/PDB format reading
                val content = extractMobiContent(file)
                
                val htmlContent = """
                    <html>
                    <head><meta charset="utf-8"/></head>
                    <body style="padding: 16px; font-size: 16px; line-height: 1.6;">
                    <pre style="white-space: pre-wrap; word-wrap: break-word;">$content</pre>
                    </body>
                    </html>
                """.trimIndent()
                
                withContext(Dispatchers.Main) {
                    binding.apply {
                        loadingProgressBar.visibility = View.GONE
                        pdfImageView.visibility = View.GONE
                        webView.visibility = View.VISIBLE
                        scrollView.visibility = View.GONE
                        textView.visibility = View.GONE
                        
                        webView.settings.apply {
                            javaScriptEnabled = true
                            builtInZoomControls = true
                            displayZoomControls = false
                        }
                        
                        webView.webViewClient = object : android.webkit.WebViewClient() {
                            override fun onPageFinished(view: WebView?, url: String?) {
                                super.onPageFinished(view, url)
                                val preferences = preferencesManager.getReadingPreferences()
                                applyWebViewStyles(preferences)
                            }
                        }
                        
                        webView.loadDataWithBaseURL(null, htmlContent, "text/html", "UTF-8", null)
                        currentTextContent = content
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
                withContext(Dispatchers.Main) {
                    binding.apply {
                        loadingProgressBar.visibility = View.GONE
                        webView.visibility = View.VISIBLE
                        scrollView.visibility = View.GONE
                        pdfImageView.visibility = View.GONE
                        
                        val errorHtml = """
                            <html>
                            <body style="padding: 16px;">
                            <h1>MOBI Error</h1>
                            <p>Error loading MOBI file: ${e.message}</p>
                            <p>File: ${file.name}</p>
                            <p>Basic MOBI format support is available for simple text extraction.</p>
                            </body>
                            </html>
                        """.trimIndent()
                        
                        webView.loadDataWithBaseURL(null, errorHtml, "text/html", "UTF-8", null)
                    }
                }
            }
        }
    }
    
    private fun extractMobiContent(file: File): String {
        try {
            val raf = RandomAccessFile(file, "r")
            val headerBytes = ByteArray(78)
            raf.read(headerBytes)
            
            // Simple text extraction from MOBI/PDB format
            // This is a basic implementation that reads text records
            val sb = StringBuilder()
            sb.append("MOBI/PDB Book\n")
            sb.append("File: ${file.name}\n")
            sb.append("Size: ${file.length() / 1024} KB\n\n")
            
            // Limit reading to 5MB to avoid memory issues
            val maxBytes = 5 * 1024 * 1024 // 5MB limit
            val bytesToRead = minOf(file.length(), maxBytes.toLong()).toInt()
            
            // Try to extract readable text from the file
            val buffer = ByteArray(4096)
            var bytesRead: Int
            var totalRead = 0
            val textContent = StringBuilder()
            
            raf.seek(0)
            while (raf.read(buffer).also { bytesRead = it } != -1 && totalRead < bytesToRead) {
                val actualRead = minOf(bytesRead, bytesToRead - totalRead)
                totalRead += actualRead
                
                val text = String(buffer, 0, actualRead, Charsets.ISO_8859_1)
                // Filter out non-printable characters but keep readable text
                text.forEach { char ->
                    if (char.isLetterOrDigit() || char.isWhitespace() || char in ".,!?;:'\"-()[]{}") {
                        textContent.append(char)
                    }
                }
            }
            
            raf.close()
            
            val extracted = textContent.toString()
            if (extracted.length > 200) {
                sb.append(extracted)
                if (file.length() > maxBytes) {
                    sb.append("\n\n[Content truncated - showing first 5MB of ${file.length() / 1024 / 1024}MB]")
                }
            } else {
                sb.append("MOBI format detected.\n\n")
                sb.append("This is a basic MOBI reader implementation.\n")
                sb.append("For full MOBI support with proper formatting, consider using a dedicated reader.\n")
            }
            
            return sb.toString()
        } catch (e: Exception) {
            e.printStackTrace()
            return "Error extracting MOBI content: ${e.message}"
        }
    }
    
    private suspend fun loadCbz(file: File) {
        withContext(Dispatchers.IO) {
            try {
                comicImagePaths.clear()
                comicArchiveFile = file
                
                // Use Apache Commons Compress for better ZIP handling
                val zipFile = ApacheZipFile(file)
                val entries = zipFile.entries.toList()
                    .filter { !it.isDirectory && it.name.lowercase().let { name -> 
                        name.endsWith(".jpg") || name.endsWith(".jpeg") || 
                        name.endsWith(".png") || name.endsWith(".gif") || 
                        name.endsWith(".bmp") || name.endsWith(".webp")
                    }}
                    .sortedBy { it.name }
                
                // Store paths only, load images on demand
                for (entry in entries) {
                    comicImagePaths.add(Pair(entry.name, "cbz"))
                }
                
                zipFile.close()
                totalComicPages = comicImagePaths.size
                
                withContext(Dispatchers.Main) {
                    binding.apply {
                        loadingProgressBar.visibility = View.GONE
                        pdfImageView.visibility = View.VISIBLE
                        webView.visibility = View.GONE
                        scrollView.visibility = View.GONE
                        textView.visibility = View.GONE
                    }
                    
                    if (totalComicPages > 0) {
                        renderComicPage(currentPage)
                    } else {
                        Toast.makeText(
                            this@ViewerActivity,
                            "No images found in CBZ file",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
                withContext(Dispatchers.Main) {
                    binding.apply {
                        loadingProgressBar.visibility = View.GONE
                        scrollView.visibility = View.VISIBLE
                        textView.visibility = View.VISIBLE
                        pdfImageView.visibility = View.GONE
                        webView.visibility = View.GONE
                        
                        textView.text = "Error loading CBZ file: ${e.message}\n\n" +
                                "File: ${file.name}\n" +
                                "CBZ files should contain image files (JPG, PNG, etc.)"
                    }
                }
            }
        }
    }
    
    private suspend fun loadCbr(file: File) {
        withContext(Dispatchers.IO) {
            try {
                comicImagePaths.clear()
                comicArchiveFile = file
                
                // Use junrar library for RAR extraction
                val archive = Archive(file)
                val fileHeaders = mutableListOf<FileHeader>()
                
                // Collect all image file headers
                var fileHeader: FileHeader? = archive.nextFileHeader()
                while (fileHeader != null) {
                    if (!fileHeader.isDirectory) {
                        val fileName = fileHeader.fileName.lowercase()
                        if (fileName.endsWith(".jpg") || fileName.endsWith(".jpeg") || 
                            fileName.endsWith(".png") || fileName.endsWith(".gif") || 
                            fileName.endsWith(".bmp") || fileName.endsWith(".webp")) {
                            fileHeaders.add(fileHeader)
                        }
                    }
                    fileHeader = archive.nextFileHeader()
                }
                
                // Sort by name and store paths only
                fileHeaders.sortBy { it.fileName }
                for (header in fileHeaders) {
                    comicImagePaths.add(Pair(header.fileName, "cbr"))
                }
                
                archive.close()
                totalComicPages = comicImagePaths.size
                
                withContext(Dispatchers.Main) {
                    binding.apply {
                        loadingProgressBar.visibility = View.GONE
                        pdfImageView.visibility = View.VISIBLE
                        webView.visibility = View.GONE
                        scrollView.visibility = View.GONE
                        textView.visibility = View.GONE
                    }
                    
                    if (totalComicPages > 0) {
                        renderComicPage(currentPage)
                    } else {
                        Toast.makeText(
                            this@ViewerActivity,
                            "No images found in CBR file",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
                withContext(Dispatchers.Main) {
                    binding.apply {
                        loadingProgressBar.visibility = View.GONE
                        scrollView.visibility = View.VISIBLE
                        textView.visibility = View.VISIBLE
                        pdfImageView.visibility = View.GONE
                        webView.visibility = View.GONE
                        
                        textView.text = "Error loading CBR file: ${e.message}\n\n" +
                                "File: ${file.name}\n" +
                                "CBR files should contain image files (JPG, PNG, etc.)"
                    }
                }
            }
        }
    }
    
    private fun renderComicPage(pageIndex: Int) {
        if (pageIndex < 0 || pageIndex >= totalComicPages) return
        
        lifecycleScope.launch(Dispatchers.IO) {
            try {
                val imagePath = comicImagePaths[pageIndex]
                val cacheKey = "comic_${currentBook?.id}_$pageIndex"
                
                // Check cache first
                val cachedBitmap = bitmapCache.getBitmap(cacheKey)
                if (cachedBitmap != null && !cachedBitmap.isRecycled) {
                    withContext(Dispatchers.Main) {
                        displayComicBitmap(cachedBitmap, pageIndex)
                    }
                    return@launch
                }
                
                // Load bitmap on demand
                val bitmap = when (imagePath.second) {
                    "cbz" -> loadCbzImage(imagePath.first)
                    "cbr" -> loadCbrImage(imagePath.first)
                    else -> null
                }
                
                if (bitmap != null) {
                    // Optimize large images with subsampling
                    val optimizedBitmap = optimizeBitmap(bitmap)
                    
                    // Cache the bitmap
                    bitmapCache.put(cacheKey, optimizedBitmap)
                    
                    withContext(Dispatchers.Main) {
                        displayComicBitmap(optimizedBitmap, pageIndex)
                    }
                    
                    // Recycle original if different
                    if (bitmap != optimizedBitmap && !bitmap.isRecycled) {
                        bitmap.recycle()
                    }
                } else {
                    withContext(Dispatchers.Main) {
                        Toast.makeText(
                            this@ViewerActivity,
                            "Error loading page $pageIndex",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
                withContext(Dispatchers.Main) {
                    Toast.makeText(
                        this@ViewerActivity,
                        "Error loading page: ${e.message}",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }
        }
    }
    
    private fun loadCbzImage(imagePath: String): Bitmap? {
        return try {
            val file = comicArchiveFile ?: return null
            val zipFile = ApacheZipFile(file)
            val entry = zipFile.getEntry(imagePath)
            val inputStream = zipFile.getInputStream(entry)
            val bitmap = BitmapFactory.decodeStream(inputStream)
            inputStream.close()
            zipFile.close()
            bitmap
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }
    
    private fun loadCbrImage(imagePath: String): Bitmap? {
        return try {
            val file = comicArchiveFile ?: return null
            val archive = Archive(file)
            
            // Find the specific file header
            var fileHeader: FileHeader? = archive.nextFileHeader()
            var targetHeader: FileHeader? = null
            while (fileHeader != null) {
                if (fileHeader.fileName == imagePath) {
                    targetHeader = fileHeader
                    break
                }
                fileHeader = archive.nextFileHeader()
            }
            
            if (targetHeader != null) {
                val outputStream = ByteArrayOutputStream()
                archive.extractFile(targetHeader, outputStream)
                val bytes = outputStream.toByteArray()
                val bitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
                outputStream.close()
                archive.close()
                bitmap
            } else {
                archive.close()
                null
            }
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }
    
    private fun optimizeBitmap(bitmap: Bitmap): Bitmap {
        // If bitmap is too large, scale it down
        val maxDimension = 2048
        val width = bitmap.width
        val height = bitmap.height
        
        if (width <= maxDimension && height <= maxDimension) {
            return bitmap
        }
        
        val scale = minOf(
            maxDimension.toFloat() / width,
            maxDimension.toFloat() / height
        )
        
        val scaledWidth = (width * scale).toInt()
        val scaledHeight = (height * scale).toInt()
        
        return Bitmap.createScaledBitmap(bitmap, scaledWidth, scaledHeight, true)
    }
    
    private fun displayComicBitmap(bitmap: Bitmap, pageIndex: Int) {
        binding.apply {
            pdfImageView.visibility = View.VISIBLE
            pdfImageView.setImageBitmap(bitmap)
            webView.visibility = View.GONE
            scrollView.visibility = View.GONE
            textView.visibility = View.GONE
        }
        
        currentPage = pageIndex
        updateProgress(pageIndex, totalComicPages)
    }
    
    private suspend fun loadText(file: File) {
        withContext(Dispatchers.IO) {
            try {
                // For very large text files (>5MB), read with a limit to avoid OOM
                val maxSize = 5 * 1024 * 1024 // 5MB limit for text files
                val content = if (file.length() > maxSize) {
                    // Read only first 5MB and add a notice
                    val limitedContent = file.inputStream().bufferedReader().use { reader ->
                        val buffer = CharArray(maxSize)
                        val charsRead = reader.read(buffer)
                        String(buffer, 0, charsRead)
                    }
                    "$limitedContent\n\n[File truncated - showing first 5MB of ${file.length() / 1024 / 1024}MB]"
                } else {
                    file.readText()
                }
                
                withContext(Dispatchers.Main) {
                    binding.apply {
                        loadingProgressBar.visibility = View.GONE
                        pdfImageView.visibility = View.GONE
                        webView.visibility = View.GONE
                        scrollView.visibility = View.VISIBLE
                        textView.visibility = View.VISIBLE
                        
                        textView.text = content
                        currentTextContent = content // Store for TTS
                    }
                    
                    // Apply reading preferences
                    val preferences = preferencesManager.getReadingPreferences()
                    applyReadingPreferences(preferences)
                }
            } catch (e: Exception) {
                e.printStackTrace()
                withContext(Dispatchers.Main) {
                    binding.apply {
                        loadingProgressBar.visibility = View.GONE
                        scrollView.visibility = View.VISIBLE
                        textView.visibility = View.VISIBLE
                        pdfImageView.visibility = View.GONE
                        webView.visibility = View.GONE
                        
                        textView.text = "Error loading text file: ${e.message}\n\n" +
                                "File: ${file.name}\n" +
                                "Size: ${file.length() / 1024} KB"
                    }
                }
            }
        }
    }
    
    private fun updateProgress(page: Int, totalPages: Int) {
        currentBook?.let { book ->
            val boundedPage = if (totalPages > 0) {
                page.coerceAtLeast(0).coerceAtMost(totalPages - 1)
            } else {
                page
            }
            
            val progress = if (totalPages > 0) {
                (((boundedPage + 1).toFloat() / totalPages) * 100f).coerceIn(0f, 100f)
            } else {
                currentProgressPercent.coerceIn(0f, 100f)
            }
            
            currentProgressPercent = progress
            bookViewModel.updateProgress(book.id, boundedPage, progress)
        }
    }
    
    private fun previousPage() {
        if (pdfRenderer != null && totalPdfPages > 0) {
            if (currentPage > 0) {
                currentPage--
                renderPdfPage(currentPage)
            } else {
                Toast.makeText(this, "Already at first page", Toast.LENGTH_SHORT).show()
            }
        } else if (totalComicPages > 0) {
            if (currentPage > 0) {
                currentPage--
                renderComicPage(currentPage)
            } else {
                Toast.makeText(this, "Already at first page", Toast.LENGTH_SHORT).show()
            }
        } else {
            Toast.makeText(this, "Page navigation not available for this format", Toast.LENGTH_SHORT).show()
        }
    }
    
    private fun nextPage() {
        if (pdfRenderer != null && totalPdfPages > 0) {
            if (currentPage < totalPdfPages - 1) {
                currentPage++
                renderPdfPage(currentPage)
            } else {
                Toast.makeText(this, "Already at last page", Toast.LENGTH_SHORT).show()
            }
        } else if (totalComicPages > 0) {
            if (currentPage < totalComicPages - 1) {
                currentPage++
                renderComicPage(currentPage)
            } else {
                Toast.makeText(this, "Already at last page", Toast.LENGTH_SHORT).show()
            }
        } else {
            Toast.makeText(this, "Page navigation not available for this format", Toast.LENGTH_SHORT).show()
        }
    }
    
    private fun bookmarkCurrentPage() {
        currentBook?.let { book ->
            val dialog = AddNoteDialogFragment.newInstance()
            dialog.setOnNoteSavedListener { note ->
                lifecycleScope.launch {
                    try {
                        val bookmark = com.rifters.ebookreader.model.Bookmark(
                            bookId = book.id,
                            page = currentPage,
                            position = 0,
                            note = note,
                            timestamp = System.currentTimeMillis()
                        )
                        
                        val database = com.rifters.ebookreader.database.BookDatabase.getDatabase(this@ViewerActivity)
                        database.bookmarkDao().insertBookmark(bookmark)
                        
                        withContext(Dispatchers.Main) {
                            Toast.makeText(
                                this@ViewerActivity,
                                getString(R.string.bookmark_added),
                                Toast.LENGTH_SHORT
                            ).show()
                        }
                    } catch (e: Exception) {
                        e.printStackTrace()
                        withContext(Dispatchers.Main) {
                            Toast.makeText(
                                this@ViewerActivity,
                                "Error adding bookmark: ${e.message}",
                                Toast.LENGTH_SHORT
                            ).show()
                        }
                    }
                }
            }
            dialog.show(supportFragmentManager, AddNoteDialogFragment.TAG)
        } ?: run {
            Toast.makeText(this, "No book loaded", Toast.LENGTH_SHORT).show()
        }
    }
    
    private fun showBookmarks() {
        currentBook?.let { book ->
            val bottomSheet = BookmarksBottomSheet.newInstance(book.id)
            bottomSheet.setOnBookmarkSelectedListener { bookmark ->
                navigateToBookmark(bookmark)
            }
            bottomSheet.show(supportFragmentManager, BookmarksBottomSheet.TAG)
        } ?: run {
            Toast.makeText(this, "No book loaded", Toast.LENGTH_SHORT).show()
        }
    }
    
    private fun navigateToBookmark(bookmark: Bookmark) {
        if (pdfRenderer != null && totalPdfPages > 0) {
            // For PDF files, navigate to the bookmarked page
            if (bookmark.page in 0 until totalPdfPages) {
                currentPage = bookmark.page
                renderPdfPage(currentPage)
                Toast.makeText(
                    this,
                    getString(R.string.page_format, bookmark.page + 1),
                    Toast.LENGTH_SHORT
                ).show()
            } else {
                Toast.makeText(this, "Invalid page number", Toast.LENGTH_SHORT).show()
            }
        } else if (totalComicPages > 0) {
            // For comic books (CBZ/CBR), navigate to the bookmarked page
            if (bookmark.page in 0 until totalComicPages) {
                currentPage = bookmark.page
                renderComicPage(currentPage)
                Toast.makeText(
                    this,
                    getString(R.string.page_format, bookmark.page + 1),
                    Toast.LENGTH_SHORT
                ).show()
            } else {
                Toast.makeText(this, "Invalid page number", Toast.LENGTH_SHORT).show()
            }
        } else {
            Toast.makeText(
                this,
                "Page navigation not available for this format",
                Toast.LENGTH_SHORT
            ).show()
        }
    }
    
    private fun showReadingSettings() {
        val bottomSheet = ReadingSettingsBottomSheet.newInstance()
        bottomSheet.setOnSettingsAppliedListener { preferences ->
            applyReadingPreferences(preferences)
        }
        bottomSheet.show(supportFragmentManager, ReadingSettingsBottomSheet.TAG)
    }
    
    private fun applyThemeToUI(preferences: ReadingPreferences) {
        // Apply theme colors to all UI elements
        binding.contentContainer.setBackgroundColor(preferences.theme.backgroundColor)
        
        // Apply to toolbar (keep default colors for better visibility)
        // Toolbar colors are managed by the theme system
        
        // Apply to bottom bar (keep default colors for better visibility)
        // Bottom bar colors are managed by the theme system
    }
    
    private fun applyReadingPreferences(preferences: ReadingPreferences) {
        // Apply theme to all UI elements
        applyThemeToUI(preferences)
        
        // Apply to TextView (for TXT files)
        if (binding.textView.visibility == View.VISIBLE) {
            binding.textView.apply {
                setTextColor(preferences.theme.textColor)
                setLineSpacing(0f, preferences.lineSpacing)
                typeface = Typeface.create(preferences.fontFamily, Typeface.NORMAL)
                setPadding(
                    preferences.marginHorizontal,
                    preferences.marginVertical,
                    preferences.marginHorizontal,
                    preferences.marginVertical
                )
            }
        }
        
        // Apply to WebView (for EPUB files)
        if (binding.webView.visibility == View.VISIBLE) {
            applyWebViewStyles(preferences)
        }
        
        // Apply to ScrollView container
        binding.scrollView.setPadding(0, 0, 0, 0)
    }
    
    private fun applyWebViewStyles(preferences: ReadingPreferences) {
        val backgroundColor = String.format("#%06X", 0xFFFFFF and preferences.theme.backgroundColor)
        val textColor = String.format("#%06X", 0xFFFFFF and preferences.theme.textColor)
        
        val css = """
            <style>
                body {
                    font-family: ${preferences.fontFamily};
                    line-height: ${preferences.lineSpacing};
                    color: $textColor !important;
                    background-color: $backgroundColor !important;
                    padding: ${preferences.marginVertical}px ${preferences.marginHorizontal}px;
                    margin: 0;
                }
                * {
                    color: $textColor !important;
                    background-color: transparent !important;
                }
            </style>
        """.trimIndent()
        
        binding.webView.evaluateJavascript(
            """
            (function() {
                var style = document.createElement('style');
                style.innerHTML = `$css`;
                document.head.appendChild(style);
            })();
            """.trimIndent(),
            null
        )
    }
    
    override fun onPause() {
        super.onPause()
        // Save current position when leaving the activity
        currentBook?.let { book ->
            bookViewModel.updateProgress(book.id, currentPage, currentProgressPercent)
        }
    }
    
    override fun onDestroy() {
        // Shutdown TTS
        if (textToSpeech != null) {
            textToSpeech?.stop()
            textToSpeech?.shutdown()
        }
        
        // Close PDF renderer
        try {
            pdfRenderer?.close()
            pdfFileDescriptor?.close()
        } catch (e: Exception) {
            e.printStackTrace()
        }
        
        // Clear comic book references (bitmaps are managed by cache)
        comicImagePaths.clear()
        comicArchiveFile = null
        
        // Note: Don't clear the global cache here as it may be used by other instances
        // The cache will auto-evict when memory is needed
        
        super.onDestroy()
    }
}
