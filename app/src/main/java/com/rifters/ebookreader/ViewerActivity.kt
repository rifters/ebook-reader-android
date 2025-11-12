package com.rifters.ebookreader

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Typeface
import android.graphics.pdf.PdfRenderer
import android.os.Bundle
import android.os.ParcelFileDescriptor
import android.speech.tts.TextToSpeech
import android.text.InputType
import android.util.TypedValue
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.webkit.JavascriptInterface
import android.webkit.WebView
import android.widget.FrameLayout
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.viewpager2.widget.ViewPager2
import com.github.junrar.Archive
import com.github.junrar.rarfile.FileHeader
import com.rifters.ebookreader.adapter.ComicPageAdapter
import com.rifters.ebookreader.adapter.PdfPageAdapter
import com.rifters.ebookreader.databinding.ActivityViewerBinding
import com.rifters.ebookreader.pagination.PaginationManager
import com.rifters.ebookreader.pagination.PaginationPreferencesKey
import com.rifters.ebookreader.pagination.PaginationSnapshot
import com.rifters.ebookreader.model.Bookmark
import com.rifters.ebookreader.model.LayoutMode
import com.rifters.ebookreader.model.ReadingPreferences
import com.rifters.ebookreader.util.FileValidator
import com.rifters.ebookreader.util.PreferencesManager
import com.rifters.ebookreader.util.TtsReplacementProcessor
import com.rifters.ebookreader.util.TtsTextSplitter
import com.rifters.ebookreader.viewmodel.BookViewModel
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.slider.Slider
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.apache.commons.compress.archivers.zip.ZipFile as ApacheZipFile
import org.json.JSONArray
import org.json.JSONException
import org.json.JSONObject
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
    private var currentLayoutMode: LayoutMode = LayoutMode.SINGLE_COLUMN
    private val continuousScrollSyncRunnable = Runnable { syncContinuousScrollPosition() }
    
    // PDF variables
    private var pdfRenderer: PdfRenderer? = null
    private var pdfFileDescriptor: ParcelFileDescriptor? = null
    private var totalPdfPages: Int = 0
    
    // Comic book (CBZ/CBR) variables
    private var comicImages: MutableList<Bitmap> = mutableListOf()
    private var totalComicPages: Int = 0
    
    // EPUB variables
    private var epubContent: com.rifters.ebookreader.util.EpubParser.EpubContent? = null
    private var currentEpubChapter: Int = 0
    // EPUB pagination (per-chapter) - uses CSS columns in WebView to simulate pages
    private var epubPageCount: Int = 0
    private var epubCurrentPageInChapter: Int = 0
    // If non-null, requested page index to jump to after a chapter is loaded. Use -1 to indicate "last page".
    private var pendingEpubPageAfterLoad: Int? = null
    private var epubChapterPagePositions: MutableMap<Int, Int> = mutableMapOf()
    private var suppressPageSliderCallback: Boolean = false
    
    // TTS variables
    private var textToSpeech: TextToSpeech? = null
    private var isTtsInitialized = false
    private var isTtsPlaying = false
    private var currentTextContent: String = ""
    private var ttsChunks: List<TtsChunk> = emptyList()
    private var currentTtsChunkIndex: Int = 0
    private var ttsSavedPosition: Int = 0
    private var pendingTtsAutoContinue = false
    private var pendingTtsPlayAfterPreparation = false
    private var isTtsSelectionEnabled = false

    private data class TtsChunk(
        val text: String,
        val startPosition: Int,
        val paragraphIndex: Int
    )
    
    // Table of Contents
    private var tableOfContents: List<com.rifters.ebookreader.model.TableOfContentsItem> = emptyList()
    
    // Pagination support
    private val paginationManager by lazy { PaginationManager(this) }
    private var paginationPreferencesKey: PaginationPreferencesKey? = null
    private var paginationBookIdentifier: String? = null
    
    // ViewPager2 support for page-based navigation
    private var pdfPageAdapter: PdfPageAdapter? = null
    private var comicPageAdapter: ComicPageAdapter? = null
    private var isUsingViewPager = false
    
    // Night mode state (separate from theme for toggle functionality)
    private var isNightModeEnabled = false
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityViewerBinding.inflate(layoutInflater)
        setContentView(binding.root)
        
        bookViewModel = ViewModelProvider(this)[BookViewModel::class.java]
        preferencesManager = PreferencesManager(this)
        
        setupToolbar()
        setupBottomBar()
        setupPageSlider()
        setupTTS()
        
        // Load night mode state
        isNightModeEnabled = preferencesManager.isNightModeEnabled()
        
        // Apply saved theme on activity load
        val preferences = preferencesManager.getReadingPreferences()
        currentLayoutMode = preferences.layoutMode
        updatePaginationPreferences(preferences)
        applyThemeToUI(preferences)
        
        loadBookFromIntent()
    }
    
    private fun setupTTS() {
        textToSpeech = TextToSpeech(this, this)
    }
    
    override fun onInit(status: Int) {
        android.util.Log.d("ViewerActivity", "TTS onInit called with status: $status")
        
        if (status == TextToSpeech.SUCCESS) {
            val result = textToSpeech?.setLanguage(Locale.getDefault())
            android.util.Log.d("ViewerActivity", "TTS setLanguage result: $result")
            
            isTtsInitialized = result != TextToSpeech.LANG_MISSING_DATA && 
                              result != TextToSpeech.LANG_NOT_SUPPORTED
            if (!isTtsInitialized) {
                // Try falling back to English if default language is not available
                val englishResult = textToSpeech?.setLanguage(Locale.ENGLISH)
                isTtsInitialized = englishResult != TextToSpeech.LANG_MISSING_DATA && 
                                  englishResult != TextToSpeech.LANG_NOT_SUPPORTED
                
                if (!isTtsInitialized) {
                    runOnUiThread {
                        Toast.makeText(this, "TTS language not supported. Please install TTS data:\nSettings > Accessibility > Text-to-speech", Toast.LENGTH_LONG).show()
                    }
                } else {
                    runOnUiThread {
                        Toast.makeText(this, "Using English TTS (default language unavailable)", Toast.LENGTH_SHORT).show()
                    }
                }
            }
            
            if (isTtsInitialized) {
                // Set up utterance progress listener for text highlighting
                textToSpeech?.setOnUtteranceProgressListener(object : android.speech.tts.UtteranceProgressListener() {
                    override fun onStart(utteranceId: String?) {
                        android.util.Log.d("ViewerActivity", "TTS onStart utterance: $utteranceId")
                        runOnUiThread {
                            // TTS started speaking
                            isTtsPlaying = true
                            updateTtsButtons()
                        }
                    }
                    
                    override fun onDone(utteranceId: String?) {
                        android.util.Log.d("ViewerActivity", "TTS onDone utterance: $utteranceId")
                        runOnUiThread {
                            // Move to next chunk if still playing
                            if (isTtsPlaying) {
                                currentTtsChunkIndex++
                                if (currentTtsChunkIndex < ttsChunks.size) {
                                    // Continue to next chunk with a brief pause
                                    binding.root.postDelayed({
                                        if (isTtsPlaying) {
                                            speakCurrentChunk()
                                        }
                                    }, 300) // 300ms pause between chunks for natural reading
                                } else {
                                    // Finished all chunks in current chapter/content
                                    android.util.Log.d("ViewerActivity", "Finished all chunks, checking for next chapter")
                                    
                                    // For EPUB, try to continue to next chapter automatically
                                    if (epubContent != null && currentEpubChapter < epubContent!!.spine.size - 1) {
                                        val nextChapterIndex = currentEpubChapter + 1
                                        android.util.Log.d("ViewerActivity", "Auto-continuing to next chapter: $nextChapterIndex")
                                        Toast.makeText(this@ViewerActivity, "Continuing to next chapter...", Toast.LENGTH_SHORT).show()

                                        pendingTtsAutoContinue = true

                                        // Load next chapter and continue TTS
                                        renderEpubChapter(nextChapterIndex)

                                        // Wait for chapter to load, then resume TTS
                                        binding.root.postDelayed({
                                            if (isTtsPlaying) {
                                                playTTS()
                                            }
                                        }, 500) // Give chapter time to load
                                    } else {
                                        // No more chapters or not an EPUB
                                        isTtsPlaying = false
                                        updateTtsButtons()
                                        hideTtsProgress()
                                        removeTextHighlights()
                                        Toast.makeText(this@ViewerActivity, "Finished reading", Toast.LENGTH_SHORT).show()
                                    }
                                }
                            } else {
                                updateTtsButtons()
                            }
                        }
                    }
                    
                    @Deprecated("Deprecated in Java")
                    override fun onError(utteranceId: String?) {
                        android.util.Log.e("ViewerActivity", "TTS onError (deprecated) utterance: $utteranceId")
                        runOnUiThread {
                            isTtsPlaying = false
                            updateTtsButtons()
                            showTtsError("TTS error occurred. The TTS engine may not be installed or configured properly.")
                        }
                    }
                    
                    override fun onError(utteranceId: String?, errorCode: Int) {
                        android.util.Log.e("ViewerActivity", "TTS onError utterance: $utteranceId, errorCode: $errorCode")
                        runOnUiThread {
                            isTtsPlaying = false
                            updateTtsButtons()
                            val errorMsg = when (errorCode) {
                                TextToSpeech.ERROR_SYNTHESIS -> "TTS synthesis error. Try selecting a different TTS engine in Settings."
                                TextToSpeech.ERROR_SERVICE -> "TTS service error. The TTS engine may have crashed."
                                TextToSpeech.ERROR_OUTPUT -> "TTS output error. Check audio settings."
                                TextToSpeech.ERROR_NETWORK -> "TTS network error. Check internet connection."
                                TextToSpeech.ERROR_NETWORK_TIMEOUT -> "TTS network timeout. Try again."
                                TextToSpeech.ERROR_INVALID_REQUEST -> "TTS invalid request. The text may be too long or invalid."
                                TextToSpeech.ERROR_NOT_INSTALLED_YET -> "TTS engine not installed. Please install a TTS engine from Play Store."
                                else -> "TTS error (code: $errorCode). Try installing Google Text-to-speech from Play Store."
                            }
                            showTtsError(errorMsg)
                        }
                    }
                    
                    override fun onRangeStart(utteranceId: String?, start: Int, end: Int, frame: Int) {
                        // This method is called when TTS starts speaking a range of text
                        // We can use this to highlight the currently spoken text
                        runOnUiThread {
                            highlightSpokenText(start, end)
                        }
                    }
                })
                
                // Update UI to enable TTS buttons now that TTS is ready
                runOnUiThread {
                    updateTtsButtons()
                    android.util.Log.d("ViewerActivity", "TTS initialized successfully")
                }
            }
        } else if (status == TextToSpeech.ERROR) {
            android.util.Log.e("ViewerActivity", "TTS initialization failed with ERROR status")
            runOnUiThread {
                isTtsInitialized = false
                updateTtsButtons()
                showTtsError("TTS initialization failed. Please install a TTS engine:\n\n1. Open Play Store\n2. Search for 'Google Text-to-speech'\n3. Install and enable it")
            }
        } else {
            android.util.Log.e("ViewerActivity", "TTS initialization failed with unknown status: $status")
            runOnUiThread {
                isTtsInitialized = false
                updateTtsButtons()
                showTtsError("TTS initialization failed with unknown error. Please check device settings.")
            }
        }
    }
    
    private fun showTtsError(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_LONG).show()
    }
    
    @Suppress("UNUSED_PARAMETER")
    private fun highlightSpokenText(start: Int, end: Int) {
        // This is called by onRangeStart for fine-grained word-level highlighting
        // Currently not used as we're doing paragraph-level highlighting instead
        // Could be enhanced for word-by-word highlighting in the future
    }
    
    /**
     * Highlight the current TTS chunk being read in the WebView
     */
    private fun highlightCurrentChunk(scrollToCenter: Boolean = false) {
        if (binding.webView.visibility != View.VISIBLE || ttsChunks.isEmpty()) {
            return
        }
        
        if (currentTtsChunkIndex >= ttsChunks.size) {
            return
        }
        
        val paragraphIndex = ttsChunks[currentTtsChunkIndex].paragraphIndex
        val scrollCommand = if (scrollToCenter) {
            "target.scrollIntoView({ behavior: 'smooth', block: 'center' });"
        } else {
            ""
        }

        binding.webView.evaluateJavascript(
            """
            (function() {
                var nodes = document.querySelectorAll('[data-tts-chunk]');
                nodes.forEach(function(node) {
                    node.classList.remove('tts-highlight');
                });
                var target = document.querySelector('[data-tts-chunk="$paragraphIndex"]');
                if (target) {
                    target.classList.add('tts-highlight');
                    $scrollCommand
                }
            })();
            """.trimIndent(),
            null
        )
    }
    
    /**
     * Remove all TTS text highlights from the WebView
     */
    private fun removeTextHighlights() {
        if (binding.webView.visibility != View.VISIBLE) {
            return
        }
        
        binding.webView.evaluateJavascript(
            """
            (function() {
                var nodes = document.querySelectorAll('[data-tts-chunk].tts-highlight');
                nodes.forEach(function(node) {
                    node.classList.remove('tts-highlight');
                });
            })();
            """.trimIndent(),
            null
        )
    }

    private fun prepareTtsNodesInWebView() {
        if (binding.webView.visibility != View.VISIBLE) {
            return
        }

        binding.webView.evaluateJavascript(
            """
            (function() {
                try {
                    var styleId = 'tts-chunk-style';
                    if (!document.getElementById(styleId)) {
                        var style = document.createElement('style');
                        style.id = styleId;
                        style.innerHTML = '[data-tts-chunk]{cursor:pointer;transition:background-color 0.2s ease-in-out;} .tts-highlight{background-color: rgba(255, 213, 79, 0.35) !important;}';
                        document.head.appendChild(style);
                    }

                    var existing = document.querySelectorAll('[data-tts-chunk]');
                    existing.forEach(function(node) {
                        node.classList.remove('tts-highlight');
                        node.classList.remove('tts-chunk');
                        node.removeAttribute('data-tts-chunk');
                    });

                    var selectors = 'p, li, blockquote, h1, h2, h3, h4, h5, h6, pre, article, section';
                    var nodes = document.querySelectorAll(selectors);
                    var chunks = [];
                    var index = 0;

                    nodes.forEach(function(node) {
                        if (!node) { return; }
                        if (node.closest('[data-tts-ignore="true"]')) { return; }
                        var text = node.innerText || '';
                        text = text.replace(/\s+/g, ' ').trim();
                        if (!text) { return; }
                        node.setAttribute('data-tts-chunk', index);
                        node.classList.add('tts-chunk');
                        node.classList.remove('tts-highlight');
                        chunks.push({ index: index, text: text });
                        index++;
                    });

                    if (!window.__ebookTtsClickHandlerAttached) {
                        document.addEventListener('click', function(event) {
                            var target = event.target.closest('[data-tts-chunk]');
                            if (!target) { return; }
                            var idx = parseInt(target.getAttribute('data-tts-chunk'));
                            if (isNaN(idx)) { return; }
                            if (window.AndroidTtsBridge && AndroidTtsBridge.onChunkTapped) {
                                AndroidTtsBridge.onChunkTapped(idx);
                            }
                        }, true);
                        window.__ebookTtsClickHandlerAttached = true;
                    }

                    if (window.AndroidTtsBridge && AndroidTtsBridge.onChunksPrepared) {
                        AndroidTtsBridge.onChunksPrepared(JSON.stringify(chunks));
                    }
                } catch (e) {
                    console.error('prepareTtsNodesInWebView error', e);
                }
            })();
            """.trimIndent(),
            null
        )
    }

    private fun attachTtsBridge(webView: WebView) {
        try {
            webView.removeJavascriptInterface("AndroidTtsBridge")
        } catch (_: Throwable) {
            // Ignore if interface was not previously added
        }
        webView.addJavascriptInterface(TtsWebBridge(), "AndroidTtsBridge")
    }

    private fun onWebViewContentReady() {
        val preferences = preferencesManager.getReadingPreferences()
        currentLayoutMode = preferences.layoutMode
        updatePaginationPreferences(preferences)
        applyWebViewStyles(preferences)
        if (isNightModeEnabled) {
            applyNightModeToWebView()
        }
        prepareTtsNodesInWebView()
    }

    private fun setupGenericWebView() {
        binding.webView.settings.apply {
            javaScriptEnabled = true
            builtInZoomControls = true
            displayZoomControls = false
            loadWithOverviewMode = true
            useWideViewPort = true
            domStorageEnabled = true
        }
        attachTtsBridge(binding.webView)
        binding.webView.webViewClient = object : android.webkit.WebViewClient() {
            override fun onPageFinished(view: WebView?, url: String?) {
                super.onPageFinished(view, url)
                onWebViewContentReady()
            }
        }

        binding.webView.setOnScrollChangeListener { _, _, _, _, _ ->
            if (currentLayoutMode == LayoutMode.CONTINUOUS_SCROLL && epubContent != null) {
                binding.webView.removeCallbacks(continuousScrollSyncRunnable)
                binding.webView.postDelayed(continuousScrollSyncRunnable, 120)
            }
        }
    }

    private fun buildChunksFromParagraphs(paragraphs: List<Pair<Int, String>>): List<TtsChunk> {
        if (paragraphs.isEmpty()) {
            return emptyList()
        }

        val replacementsEnabled = preferencesManager.isTtsReplacementsEnabled()
        val replacementsJson = preferencesManager.getTtsReplacements()
        val chunks = mutableListOf<TtsChunk>()
        var position = 0

        paragraphs.forEach { (paragraphIndex, originalText) ->
            var processed = originalText
            if (replacementsEnabled) {
                processed = TtsReplacementProcessor.applyReplacements(processed, replacementsJson, true)
            }

            val segments = TtsTextSplitter.splitParagraphIntoChunks(processed)
            segments.forEach inner@ { segment ->
                val trimmed = segment.trim()
                if (trimmed.isEmpty()) {
                    return@inner
                }
                chunks.add(TtsChunk(trimmed, position, paragraphIndex))
                position += trimmed.length + 1
            }
        }

        return chunks
    }

    private fun findChunkIndexForPosition(position: Int): Int {
        if (ttsChunks.isEmpty()) {
            return 0
        }

        for (index in ttsChunks.indices) {
            val chunk = ttsChunks[index]
            val end = chunk.startPosition + chunk.text.length
            if (position in chunk.startPosition until end) {
                return index
            }
        }

        return ttsChunks.lastIndex
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
            R.id.action_view_highlights -> {
                showHighlights()
                true
            }
            R.id.action_go_to_page -> {
                showGoToPageDialog()
                true
            }
            R.id.action_table_of_contents -> {
                showTableOfContents()
                true
            }
            R.id.action_tts_play -> {
                toggleTTS()
                true
            }
            R.id.action_tts_controls -> {
                showTtsControls()
                true
            }
            R.id.action_toggle_night_mode -> {
                toggleNightMode()
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
        
        // Enable/disable based on TTS readiness and content availability
        ttsItem?.isEnabled = isTtsInitialized && currentTextContent.isNotEmpty()
        
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
            Toast.makeText(this, "TTS is initializing, please wait...", Toast.LENGTH_SHORT).show()
            return
        }
        
        if (isTtsPlaying) {
            pauseTTS()
        } else {
            playTTS()
        }
    }
    
    private fun updateTtsButtons() {
        // Update menu icon
        invalidateOptionsMenu()
        
        // Update bottom bar button
        binding.btnTtsPlay.isEnabled = isTtsInitialized && currentTextContent.isNotEmpty()
        
        if (isTtsPlaying) {
            binding.btnTtsPlay.setImageResource(android.R.drawable.ic_media_pause)
        } else {
            binding.btnTtsPlay.setImageResource(android.R.drawable.ic_media_play)
        }
        
        // Update alpha to show disabled state
        binding.btnTtsPlay.alpha = if (binding.btnTtsPlay.isEnabled) 1.0f else 0.5f
    }
    
    private fun playTTS() {
        android.util.Log.d("ViewerActivity", "playTTS called - textToSpeech: ${textToSpeech != null}, isTtsInitialized: $isTtsInitialized")
        pendingTtsAutoContinue = false
        pendingTtsPlayAfterPreparation = false

        if (textToSpeech == null) {
            android.util.Log.e("ViewerActivity", "TTS engine is null, attempting to reinitialize")
            setupTTS()
            Toast.makeText(this, "TTS engine not available. Initializing... Please try again in a moment.", Toast.LENGTH_LONG).show()
            return
        }

        if (!isTtsInitialized) {
            Toast.makeText(this, "TTS is initializing, please wait...", Toast.LENGTH_SHORT).show()
            binding.root.postDelayed({
                if (isTtsInitialized) {
                    playTTS()
                } else {
                    Toast.makeText(this, "TTS initialization is taking longer than expected.\n\nTo fix:\n1. Go to Settings > Accessibility > Text-to-speech\n2. Install 'Google Text-to-speech' from Play Store\n3. Set it as preferred engine", Toast.LENGTH_LONG).show()
                }
            }, 1500)
            return
        }

        if (currentTextContent.isEmpty()) {
            Toast.makeText(this, "No text content available for this format (image-based formats not supported)", Toast.LENGTH_SHORT).show()
            return
        }

        val usingWebView = binding.webView.visibility == View.VISIBLE
        isTtsSelectionEnabled = true

        if (usingWebView) {
            if (ttsChunks.isEmpty()) {
                pendingTtsPlayAfterPreparation = true
                Toast.makeText(this, "Preparing text for TTS...", Toast.LENGTH_SHORT).show()
                prepareTtsNodesInWebView()
                return
            }
        } else if (ttsChunks.isEmpty()) {
            val replacementsEnabled = preferencesManager.isTtsReplacementsEnabled()
            val replacementsJson = preferencesManager.getTtsReplacements()

            val textToSpeak = if (currentTextContent.contains("<")) {
                TtsTextSplitter.extractTextFromHtml(
                    currentTextContent,
                    applyReplacements = true,
                    replacementsJson = replacementsJson,
                    replacementsEnabled = replacementsEnabled
                )
            } else {
                if (replacementsEnabled) {
                    TtsReplacementProcessor.applyReplacements(currentTextContent, replacementsJson, true)
                } else {
                    currentTextContent
                }
            }

            if (textToSpeak.trim().isEmpty()) {
                Toast.makeText(this, "Could not extract readable text from content. The file may be empty or corrupted.", Toast.LENGTH_LONG).show()
                isTtsSelectionEnabled = false
                return
            }

            val paragraphChunks = TtsTextSplitter.splitIntoParagraphs(textToSpeak)
            var position = 0
            ttsChunks = paragraphChunks.mapIndexedNotNull { index, chunk ->
                val trimmed = chunk.text.trim()
                if (trimmed.isEmpty()) {
                    return@mapIndexedNotNull null
                }
                val ttsChunk = TtsChunk(trimmed, position, index)
                position += trimmed.length + 1
                ttsChunk
            }

            if (ttsChunks.isEmpty()) {
                Toast.makeText(this, "Could not split text for TTS reading", Toast.LENGTH_SHORT).show()
                isTtsSelectionEnabled = false
                return
            }
        }

        if (ttsSavedPosition > 0 && currentTtsChunkIndex == 0) {
            currentTtsChunkIndex = findChunkIndexForPosition(ttsSavedPosition)
            android.util.Log.d("ViewerActivity", "Resuming TTS from saved position $ttsSavedPosition (chunk $currentTtsChunkIndex)")
            Toast.makeText(this, "Resuming from saved position", Toast.LENGTH_SHORT).show()
            ttsSavedPosition = 0
        }

        if (currentTtsChunkIndex >= ttsChunks.size) {
            currentTtsChunkIndex = 0
        }

        android.util.Log.d("ViewerActivity", "Playing TTS chunk $currentTtsChunkIndex of ${ttsChunks.size}")
        speakCurrentChunk()
    }
    
    private fun speakCurrentChunk() {
        if (ttsChunks.isEmpty()) {
            Toast.makeText(this, "No text prepared for TTS", Toast.LENGTH_SHORT).show()
            return
        }

        if (currentTtsChunkIndex >= ttsChunks.size) {
            // Finished reading all chunks
            android.util.Log.d("ViewerActivity", "Finished reading all TTS chunks")
            isTtsPlaying = false
            isTtsSelectionEnabled = false
            updateTtsButtons()
            Toast.makeText(this, "Finished reading", Toast.LENGTH_SHORT).show()
            return
        }
        
        val chunk = ttsChunks[currentTtsChunkIndex]
        
        // Get TTS settings from preferences
        val ttsRate = preferencesManager.getTtsRate()
        val ttsPitch = preferencesManager.getTtsPitch()
        
        textToSpeech?.setSpeechRate(ttsRate)
        textToSpeech?.setPitch(ttsPitch)
        
        // Use Bundle for API 21+
        val params = android.os.Bundle()
        params.putString(TextToSpeech.Engine.KEY_PARAM_UTTERANCE_ID, "tts_chunk_$currentTtsChunkIndex")
        
        val result = textToSpeech?.speak(chunk.text, TextToSpeech.QUEUE_FLUSH, params, "tts_chunk_$currentTtsChunkIndex")
        
        android.util.Log.d("ViewerActivity", "TTS speak result for chunk $currentTtsChunkIndex: $result")
        
        when (result) {
            TextToSpeech.SUCCESS -> {
                android.util.Log.d("ViewerActivity", "TTS chunk $currentTtsChunkIndex started successfully")
                // Save current position
                saveTtsPosition(chunk.startPosition)
                updateTtsButtons()
                updateTtsProgress()
                // Highlight the text being read
                highlightCurrentChunk(scrollToCenter = true)
            }
            TextToSpeech.ERROR -> {
                android.util.Log.e("ViewerActivity", "TTS speak returned ERROR")
                showTtsError("TTS engine error. Please try:\n\n1. Go to Settings > Apps > All apps\n2. Find your TTS engine (e.g., Google Text-to-speech)\n3. Clear cache and data\n4. Restart this app\n\nOr install a different TTS engine from Play Store")
            }
            else -> {
                android.util.Log.e("ViewerActivity", "TTS speak returned unexpected result: $result")
                showTtsError("Failed to start TTS (code: $result).\n\nPlease check:\n1. TTS engine is installed and enabled\n2. Audio output is working\n3. Device settings > Accessibility > Text-to-speech")
            }
        }
    }
    
    private fun saveTtsPosition(position: Int) {
        ttsSavedPosition = position
        currentBook?.let { book ->
            lifecycleScope.launch(Dispatchers.IO) {
                val updatedBook = book.copy(
                    ttsPosition = position,
                    ttsLastPlayed = System.currentTimeMillis()
                )
                bookViewModel.updateBook(updatedBook)
                android.util.Log.d("ViewerActivity", "Saved TTS position: $position")
            }
        }
    }
    
    private fun pauseTTS() {
        textToSpeech?.stop()
        isTtsPlaying = false
        pendingTtsAutoContinue = false
        pendingTtsPlayAfterPreparation = false
        isTtsSelectionEnabled = false
        // Reset chunks so they'll be regenerated on next play
        ttsChunks = emptyList()
        currentTtsChunkIndex = 0
        updateTtsButtons()
        hideTtsProgress()
        removeTextHighlights()
        Toast.makeText(this, "TTS stopped", Toast.LENGTH_SHORT).show()
    }
    
    /**
     * Reset TTS state when content changes (e.g., chapter navigation)
     * This ensures TTS doesn't get stuck on old content
     */
    private fun resetTtsStateForNewContent() {
        // Stop any ongoing TTS playback unless we are auto-continuing to the next chapter
        if (pendingTtsAutoContinue) {
            textToSpeech?.stop()
        } else if (isTtsPlaying) {
            textToSpeech?.stop()
            isTtsPlaying = false
        }
        
        // Clear TTS chunks and reset position
        ttsChunks = emptyList()
        pendingTtsPlayAfterPreparation = false
        currentTtsChunkIndex = 0
        ttsSavedPosition = 0 // Reset saved position for new content
    isTtsSelectionEnabled = false
        
        // Remove any existing highlights
        removeTextHighlights()
        
        android.util.Log.d("ViewerActivity", "TTS state reset for new content (autoContinue=$pendingTtsAutoContinue)")
    }
    
    private fun updateTtsProgress() {
        if (ttsChunks.isEmpty()) {
            return
        }
        
        val currentChunk = ttsChunks[currentTtsChunkIndex]
        val progressPercentage = ((currentTtsChunkIndex + 1) * 100 / ttsChunks.size)
        
        // Show progress with text preview (first 50 chars)
        val preview = if (currentChunk.text.length > 50) {
            currentChunk.text.substring(0, 50) + "..."
        } else {
            currentChunk.text
        }
        
        val progressText = "🔊 $progressPercentage% • $preview"
        binding.pageIndicator.text = progressText
        binding.pageIndicator.visibility = View.VISIBLE
    }
    
    private fun hideTtsProgress() {
        // Only hide if showing TTS progress (starts with speaker emoji)
        if (binding.pageIndicator.text.toString().startsWith("🔊")) {
            binding.pageIndicator.visibility = View.GONE
        }
    }
    
    private fun showTtsControls() {
        val bottomSheet = TtsControlsBottomSheet.newInstance()
        bottomSheet.setOnSettingsChangedListener { rate, pitch ->
            // Apply settings in real-time if TTS is playing
            if (isTtsPlaying) {
                textToSpeech?.setSpeechRate(rate)
                textToSpeech?.setPitch(pitch)
            }
        }
        bottomSheet.show(supportFragmentManager, TtsControlsBottomSheet.TAG)
    }

    private inner class TtsWebBridge {
        @JavascriptInterface
        fun onChunksPrepared(payload: String) {
            runOnUiThread {
                try {
                    val array = JSONArray(payload)
                    val paragraphs = mutableListOf<Pair<Int, String>>()
                    for (i in 0 until array.length()) {
                        val entry = array.optJSONObject(i) ?: continue
                        val paragraphIndex = entry.optInt("index", -1)
                        if (paragraphIndex < 0) continue
                        val text = entry.optString("text", "").trim()
                        if (text.isEmpty()) continue
                        paragraphs.add(paragraphIndex to text)
                    }

                    if (paragraphs.isEmpty()) {
                        ttsChunks = emptyList()
                        isTtsSelectionEnabled = false
                        updateTtsButtons()
                        return@runOnUiThread
                    }

                    ttsChunks = buildChunksFromParagraphs(paragraphs)
                    if (ttsChunks.isEmpty()) {
                        isTtsSelectionEnabled = false
                        updateTtsButtons()
                        return@runOnUiThread
                    }

                    if (ttsSavedPosition > 0) {
                        currentTtsChunkIndex = findChunkIndexForPosition(ttsSavedPosition)
                    }

                    currentTtsChunkIndex = currentTtsChunkIndex.coerceIn(0, ttsChunks.lastIndex)
                    updateTtsButtons()

                    if (pendingTtsPlayAfterPreparation || (pendingTtsAutoContinue && isTtsPlaying)) {
                        pendingTtsPlayAfterPreparation = false
                        pendingTtsAutoContinue = false
                        speakCurrentChunk()
                    } else if (isTtsPlaying) {
                        speakCurrentChunk()
                    } else if (isTtsSelectionEnabled) {
                        highlightCurrentChunk()
                    }
                } catch (e: JSONException) {
                    android.util.Log.e("ViewerActivity", "Failed to parse TTS chunks from WebView", e)
                }
            }
        }

        @JavascriptInterface
        fun onChunkTapped(index: Int) {
            runOnUiThread {
                if (ttsChunks.isEmpty()) {
                    return@runOnUiThread
                }

                if (!isTtsSelectionEnabled) {
                    return@runOnUiThread
                }

                val targetIndex = ttsChunks.indexOfFirst { it.paragraphIndex == index }
                if (targetIndex == -1) {
                    return@runOnUiThread
                }

                currentTtsChunkIndex = targetIndex
                ttsSavedPosition = ttsChunks[targetIndex].startPosition
                updateTtsProgress()

                if (isTtsPlaying) {
                    textToSpeech?.stop()
                    speakCurrentChunk()
                } else {
                    highlightCurrentChunk()
                }
            }
        }
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
        
        binding.btnTtsPlay.setOnClickListener {
            toggleTTS()
        }
        
        binding.btnTtsPlay.setOnLongClickListener {
            showTtsControls()
            true
        }
    }

    private fun setupPageSlider() {
        binding.pageSlider.apply {
            addOnSliderTouchListener(object : Slider.OnSliderTouchListener {
                override fun onStartTrackingTouch(slider: Slider) {
                    if (epubPageCount > 0) {
                        showEpubPagePreview(slider.value.toInt())
                    }
                }

                override fun onStopTrackingTouch(slider: Slider) {
                    if (epubPageCount > 0) {
                        gotoEpubPage(slider.value.toInt() - 1)
                    }
                }
            })

            addOnChangeListener { _, value, fromUser ->
                if (!fromUser || suppressPageSliderCallback || epubPageCount <= 0) {
                    return@addOnChangeListener
                }
                showEpubPagePreview(value.toInt())
            }
        }
    }

    private fun showEpubPagePreview(pageNumber: Int) {
        if (epubPageCount <= 0) {
            return
        }

        val safeNumber = pageNumber.coerceIn(1, epubPageCount)
        binding.pageIndicator.visibility = View.VISIBLE
        binding.pageIndicator.text = getString(R.string.page_indicator, safeNumber, epubPageCount)
        binding.pageIndicator.removeCallbacks(hidePageIndicatorRunnable)
    }

    private fun updatePageSliderConfiguration() {
        val slider = binding.pageSlider
        if (epubContent == null || epubPageCount <= 1) {
            slider.visibility = View.GONE
            return
        }

        slider.visibility = View.VISIBLE
        suppressPageSliderCallback = true
        slider.valueFrom = 1f
        slider.valueTo = epubPageCount.toFloat()
        slider.stepSize = 1f
        slider.value = (epubCurrentPageInChapter + 1).coerceIn(1, epubPageCount).toFloat()
        slider.isEnabled = true
        suppressPageSliderCallback = false
    }

    private fun syncPageSliderValue() {
        val slider = binding.pageSlider
        if (slider.visibility != View.VISIBLE || epubPageCount <= 0) {
            return
        }

        suppressPageSliderCallback = true
        val maxPage = epubPageCount.coerceAtLeast(1)
        slider.value = (epubCurrentPageInChapter + 1).coerceIn(1, maxPage).toFloat()
        suppressPageSliderCallback = false
    }

    private fun persistEpubProgress() {
        val book = currentBook ?: return
        val totalChapters = epubContent?.spine?.size ?: return
        if (totalChapters <= 0) {
            return
        }

        epubChapterPagePositions[currentEpubChapter] = epubCurrentPageInChapter
        val serializedPositions = serializeEpubPositions()
        val pageFraction = if (epubPageCount > 0) {
            (epubCurrentPageInChapter + 1).toFloat() / epubPageCount
        } else {
            0f
        }
        val progress = (((currentEpubChapter.toFloat() + pageFraction) / totalChapters) * 100f)
            .coerceIn(0f, 100f)

        currentProgressPercent = progress
        currentBook = book.copy(
            currentPage = currentEpubChapter,
            epubCurrentPageInChapter = epubCurrentPageInChapter,
            epubChapterPagePositions = serializedPositions,
            progressPercentage = progress
        )

        bookViewModel.updateEpubProgress(
            bookId = book.id,
            currentChapter = currentEpubChapter,
            pageInChapter = epubCurrentPageInChapter,
            chapterPositions = serializedPositions,
            progressPercentage = progress
        )
    }
    
    /**
     * Setup ViewPager2 for page-based navigation (PDF, comic books).
     * Configures orientation based on layout mode and adds page change callback.
     */
    private fun setupViewPager() {
        // Configure orientation based on layout mode
        when (currentLayoutMode) {
            LayoutMode.CONTINUOUS_SCROLL -> {
                binding.viewPager.setVerticalMode()
            }
            else -> {
                binding.viewPager.setHorizontalMode()
            }
        }
        
        // Add page change callback for progress tracking
        binding.viewPager.registerOnPageChangeCallback(object : ViewPager2.OnPageChangeCallback() {
            override fun onPageSelected(position: Int) {
                super.onPageSelected(position)
                currentPage = position
                updatePageIndicator()
                updateProgress(position + 1, getCurrentTotalPages())
            }
        })
        
        // Handle page clicks to toggle UI visibility
        pdfPageAdapter?.setOnPageClickListener {
            toggleUIVisibility()
        }
        comicPageAdapter?.setOnPageClickListener {
            toggleUIVisibility()
        }
    }
    
    /**
     * Initialize PDF page adapter and load all pages for ViewPager2
     */
    private suspend fun initPdfViewPager() {
        withContext(Dispatchers.Main) {
            isUsingViewPager = true
            pdfPageAdapter = PdfPageAdapter()
            binding.viewPager.adapter = pdfPageAdapter
            
            // Initially set placeholder pages
            val placeholders = List<Bitmap?>(totalPdfPages) { null }
            pdfPageAdapter?.setPages(placeholders)
            
            // Show ViewPager and hide other views
            binding.viewPager.visibility = View.VISIBLE
            binding.pdfImageView.visibility = View.GONE
            binding.webView.visibility = View.GONE
            binding.scrollView.visibility = View.GONE
            
            setupViewPager()
        }
        
        // Load pages in background
        withContext(Dispatchers.IO) {
            loadPdfPagesForViewPager()
        }
    }
    
    /**
     * Load PDF pages for ViewPager2 display
     */
    private suspend fun loadPdfPagesForViewPager() {
        val renderer = pdfRenderer ?: return
        
        // Load current page and nearby pages first for faster display
        val pagesToLoadFirst = listOf(
            currentPage,
            currentPage - 1,
            currentPage + 1,
            currentPage - 2,
            currentPage + 2
        ).filter { it in 0 until totalPdfPages }.distinct()
        
        for (pageIndex in pagesToLoadFirst) {
            loadPdfPageBitmap(renderer, pageIndex)
        }
        
        // Then load remaining pages
        for (pageIndex in 0 until totalPdfPages) {
            if (pageIndex !in pagesToLoadFirst) {
                loadPdfPageBitmap(renderer, pageIndex)
            }
        }
    }
    
    /**
     * Load a single PDF page bitmap
     */
    private suspend fun loadPdfPageBitmap(renderer: PdfRenderer, pageIndex: Int) {
        try {
            val page = renderer.openPage(pageIndex)
            
            val bitmap = Bitmap.createBitmap(
                page.width * 2,
                page.height * 2,
                Bitmap.Config.ARGB_8888
            )
            
            page.render(bitmap, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
            page.close()
            
            withContext(Dispatchers.Main) {
                pdfPageAdapter?.updatePage(pageIndex, bitmap)
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
    
    /**
     * Initialize comic page adapter for ViewPager2
     */
    private suspend fun initComicViewPager() {
        withContext(Dispatchers.Main) {
            isUsingViewPager = true
            comicPageAdapter = ComicPageAdapter()
            comicPageAdapter?.setPages(comicImages)
            binding.viewPager.adapter = comicPageAdapter
            
            // Show ViewPager and hide other views
            binding.viewPager.visibility = View.VISIBLE
            binding.pdfImageView.visibility = View.GONE
            binding.webView.visibility = View.GONE
            binding.scrollView.visibility = View.GONE
            
            setupViewPager()
            
            // Set current page
            binding.viewPager.setCurrentItem(currentPage, false)
        }
    }
    
    /**
     * Get current total pages based on active format
     */
    private fun getCurrentTotalPages(): Int {
        return when {
            pdfRenderer != null -> totalPdfPages
            comicImages.isNotEmpty() -> totalComicPages
            epubContent != null -> epubContent!!.spine.size
            else -> 1
        }
    }
    
    /**
     * Update page indicator display
     */
    private fun updatePageIndicator() {
        val totalPages = getCurrentTotalPages()
        if (totalPages > 1) {
            binding.pageIndicator.text = getString(
                R.string.page_indicator,
                currentPage + 1,
                totalPages
            )
            binding.pageIndicator.visibility = View.VISIBLE
            
            // Auto-hide after 2 seconds
            binding.pageIndicator.removeCallbacks(hidePageIndicatorRunnable)
            binding.pageIndicator.postDelayed(hidePageIndicatorRunnable, 2000)
        }
    }
    
    private val hidePageIndicatorRunnable = Runnable {
        binding.pageIndicator.visibility = View.GONE
    }
    
    /**
     * Toggle UI visibility (toolbar and bottom bar)
     */
    private fun toggleUIVisibility() {
        val appBarLayout = binding.appBarLayout
        val bottomBar = binding.bottomAppBar
        
        if (appBarLayout.visibility == View.VISIBLE) {
            appBarLayout.visibility = View.GONE
            bottomBar.visibility = View.GONE
        } else {
            appBarLayout.visibility = View.VISIBLE
            bottomBar.visibility = View.VISIBLE
        }
    }
    
    private fun loadBookFromIntent() {
        val bookId = intent.getLongExtra("book_id", -1L)
        val bookPath = intent.getStringExtra("book_path")
        val bookTitle = intent.getStringExtra("book_title")
        
        supportActionBar?.title = bookTitle

        paginationBookIdentifier = when {
            bookId != -1L -> "book:$bookId"
            !bookPath.isNullOrBlank() -> "path:${File(bookPath).absolutePath}"
            else -> null
        }
        paginationManager.clear()
        
        if (bookPath != null) {
            lifecycleScope.launch {
                currentBook = if (bookId != -1L) {
                    bookViewModel.getBookById(bookId)
                } else {
                    null
                }
                
                currentPage = currentBook?.currentPage ?: 0
                epubCurrentPageInChapter = currentBook?.epubCurrentPageInChapter ?: 0
                epubChapterPagePositions = deserializeEpubPositions(currentBook?.epubChapterPagePositions)
                currentProgressPercent = currentBook?.progressPercentage ?: 0f
                ttsSavedPosition = currentBook?.ttsPosition ?: 0
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
            binding.pageSlider.visibility = View.GONE
            renderPaginationStatus(null)
        }
        
        try {
            val file = File(filePath)
            
            // Validate file existence and basic properties
            val validationResult = FileValidator.validateFile(file, this@ViewerActivity)
            if (validationResult is FileValidator.ValidationResult.Invalid) {
                withContext(Dispatchers.Main) {
                    Toast.makeText(
                        this@ViewerActivity,
                        validationResult.errorMessage,
                        Toast.LENGTH_LONG
                    ).show()
                    finish()
                }
                return
            }
            
            // Determine file type and load accordingly
            val extension = file.extension.lowercase()
            when (extension) {
                "pdf" -> {
                    if (!FileValidator.validatePdfFile(file)) {
                        withContext(Dispatchers.Main) {
                            Toast.makeText(
                                this@ViewerActivity,
                                getString(R.string.error_pdf_damaged),
                                Toast.LENGTH_LONG
                            ).show()
                            finish()
                        }
                        return
                    }
                    loadPdf(file)
                }
                "epub" -> {
                    if (!FileValidator.validateEpubFile(file)) {
                        withContext(Dispatchers.Main) {
                            Toast.makeText(
                                this@ViewerActivity,
                                getString(R.string.error_epub_invalid),
                                Toast.LENGTH_LONG
                            ).show()
                            finish()
                        }
                        return
                    }
                    loadEpub(file)
                }
                "mobi" -> {
                    if (!FileValidator.validateMobiFile(file)) {
                        withContext(Dispatchers.Main) {
                            Toast.makeText(
                                this@ViewerActivity,
                                getString(R.string.error_mobi_invalid),
                                Toast.LENGTH_LONG
                            ).show()
                            finish()
                        }
                        return
                    }
                    loadMobi(file)
                }
                "fb2" -> {
                    loadFb2(file)
                }
                "md" -> {
                    loadMarkdown(file)
                }
                "html", "htm", "xhtml", "xml", "mhtml" -> {
                    loadHtml(file)
                }
                "azw", "azw3" -> {
                    loadAzw(file)
                }
                "docx" -> {
                    loadDocx(file)
                }
                "cbz" -> {
                    if (!FileValidator.validateCbzFile(file)) {
                        withContext(Dispatchers.Main) {
                            Toast.makeText(
                                this@ViewerActivity,
                                getString(R.string.error_cbz_invalid),
                                Toast.LENGTH_LONG
                            ).show()
                            finish()
                        }
                        return
                    }
                    loadCbz(file)
                }
                "cbr" -> {
                    loadCbr(file)
                }
                "cb7" -> {
                    loadCb7(file)
                }
                "cbt" -> {
                    loadCbt(file)
                }
                "txt" -> {
                    loadText(file)
                }
                else -> {
                    withContext(Dispatchers.Main) {
                        Toast.makeText(
                            this@ViewerActivity,
                            getString(R.string.error_unsupported_format),
                            Toast.LENGTH_LONG
                        ).show()
                        finish()
                    }
                }
            }
        } catch (e: OutOfMemoryError) {
            withContext(Dispatchers.Main) {
                Toast.makeText(
                    this@ViewerActivity,
                    getString(R.string.error_file_too_large, "100MB"),
                    Toast.LENGTH_LONG
                ).show()
                finish()
            }
        } catch (e: Exception) {
            e.printStackTrace()
            withContext(Dispatchers.Main) {
                val errorMsg = FileValidator.getErrorMessage(
                    e, 
                    this@ViewerActivity,
                    File(filePath).extension
                )
                Toast.makeText(this@ViewerActivity, errorMsg, Toast.LENGTH_LONG).show()
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
                
                if (totalPdfPages == 0) {
                    withContext(Dispatchers.Main) {
                        binding.loadingProgressBar.visibility = View.GONE
                        Toast.makeText(
                            this@ViewerActivity,
                            getString(R.string.error_file_empty),
                            Toast.LENGTH_LONG
                        ).show()
                        finish()
                    }
                    return@withContext
                }
                
                withContext(Dispatchers.Main) {
                    binding.loadingProgressBar.visibility = View.GONE
                    
                    // TTS is not supported for PDF (image-based format)
                    currentTextContent = ""
                    updateTtsButtons()
                    
                    // Apply theme background
                    val preferences = preferencesManager.getReadingPreferences()
                    applyThemeToUI(preferences)
                }
                
                // Initialize ViewPager2 for page-based navigation
                initPdfViewPager()
                
                // Set current page after ViewPager is ready
                withContext(Dispatchers.Main) {
                    binding.viewPager.setCurrentItem(currentPage, false)
                    updatePageIndicator()
                }
            } catch (e: SecurityException) {
                e.printStackTrace()
                withContext(Dispatchers.Main) {
                    binding.loadingProgressBar.visibility = View.GONE
                    Toast.makeText(
                        this@ViewerActivity,
                        getString(R.string.error_no_read_permission),
                        Toast.LENGTH_LONG
                    ).show()
                    finish()
                }
            } catch (e: Exception) {
                e.printStackTrace()
                withContext(Dispatchers.Main) {
                    binding.loadingProgressBar.visibility = View.GONE
                    val errorMsg = FileValidator.getErrorMessage(e, this@ViewerActivity, "pdf")
                    Toast.makeText(this@ViewerActivity, errorMsg, Toast.LENGTH_LONG).show()
                    finish()
                }
            }
        }
    }
    
    private fun renderPdfPage(pageIndex: Int) {
        lifecycleScope.launch(Dispatchers.IO) {
            try {
                val renderer = pdfRenderer ?: return@launch
                if (pageIndex < 0 || pageIndex >= totalPdfPages) return@launch
                
                val page = renderer.openPage(pageIndex)
                
                // Create a bitmap to render the page
                val bitmap = Bitmap.createBitmap(
                    page.width * 2,
                    page.height * 2,
                    Bitmap.Config.ARGB_8888
                )
                
                page.render(bitmap, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
                page.close()
                
                withContext(Dispatchers.Main) {
                    // Apply page flip animation
                    binding.pdfImageView.animate()
                        .alpha(0f)
                        .setDuration(150)
                        .withEndAction {
                            binding.apply {
                                pdfImageView.visibility = View.VISIBLE
                                pdfImageView.setImageBitmap(bitmap)
                                webView.visibility = View.GONE
                                scrollView.visibility = View.GONE
                                textView.visibility = View.GONE
                            }
                            
                            binding.pdfImageView.animate()
                                .alpha(1f)
                                .setDuration(150)
                                .start()
                        }
                        .start()
                    
                    currentPage = pageIndex
                    updateProgress(pageIndex, totalPdfPages)
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
    
    private suspend fun loadEpub(file: File) {
        withContext(Dispatchers.IO) {
            try {
                // Parse EPUB structure
                val parser = com.rifters.ebookreader.util.EpubParser(file)
                epubContent = parser.parse()
                
                if (epubContent == null) {
                    withContext(Dispatchers.Main) {
                        binding.loadingProgressBar.visibility = View.GONE
                        Toast.makeText(
                            this@ViewerActivity,
                            getString(R.string.error_epub_invalid),
                            Toast.LENGTH_LONG
                        ).show()
                        finish()
                    }
                    return@withContext
                }
                
                // Extract TOC
                tableOfContents = epubContent!!.toc
                
                // Load the first chapter or resume from saved position
                val chapterToLoad = if (currentPage >= 0 && currentPage < epubContent!!.spine.size) {
                    currentPage
                } else {
                    0
                }
                
                withContext(Dispatchers.Main) {
                    ensurePaginationInitializedForEpub()
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
                            domStorageEnabled = true
                        }

                        attachTtsBridge(webView)
                        
                        webView.webViewClient = object : android.webkit.WebViewClient() {
                            override fun onPageFinished(view: WebView?, url: String?) {
                                super.onPageFinished(view, url)
                                onWebViewContentReady()
                            }
                        }
                    }
                    
                    renderEpubChapter(chapterToLoad)
                }
            } catch (e: java.util.zip.ZipException) {
                e.printStackTrace()
                withContext(Dispatchers.Main) {
                    binding.loadingProgressBar.visibility = View.GONE
                    Toast.makeText(
                        this@ViewerActivity,
                        getString(R.string.error_epub_invalid),
                        Toast.LENGTH_LONG
                    ).show()
                    finish()
                }
            } catch (e: Exception) {
                e.printStackTrace()
                withContext(Dispatchers.Main) {
                    binding.loadingProgressBar.visibility = View.GONE
                    val errorMsg = FileValidator.getErrorMessage(e, this@ViewerActivity, "epub")
                    Toast.makeText(this@ViewerActivity, errorMsg, Toast.LENGTH_LONG).show()
                    finish()
                }
            }
        }
    }
    
    private fun renderEpubChapter(chapterIndex: Int) {
        epubContent?.let { content ->
            if (chapterIndex < 0 || chapterIndex >= content.spine.size) {
                Toast.makeText(this, "Invalid chapter", Toast.LENGTH_SHORT).show()
                return
            }

            ensurePaginationInitializedForEpub()
            renderPaginationStatus(null)

            if (pendingEpubPageAfterLoad == null) {
                val savedPage = epubChapterPagePositions[chapterIndex]
                if (savedPage != null) {
                    pendingEpubPageAfterLoad = savedPage
                    epubCurrentPageInChapter = savedPage
                } else {
                    epubCurrentPageInChapter = 0
                }
            }
            
            lifecycleScope.launch(Dispatchers.IO) {
                try {
                    val parser = com.rifters.ebookreader.util.EpubParser(File(currentBook?.filePath ?: ""))
                    val chapterHtml = parser.getChapterContent(chapterIndex, content)
                    
                    if (chapterHtml.isNullOrEmpty()) {
                        withContext(Dispatchers.Main) {
                            Toast.makeText(
                                this@ViewerActivity,
                                "Could not load chapter",
                                Toast.LENGTH_SHORT
                            ).show()
                        }
                        return@launch
                    }
                    
                    withContext(Dispatchers.Main) {
                        // Apply fade animation
                        binding.webView.animate()
                            .alpha(0f)
                            .setDuration(150)
                            .withEndAction {
                                binding.webView.loadDataWithBaseURL(
                                    null,
                                    wrapEpubChapterHtml(chapterHtml),
                                    "text/html",
                                    "UTF-8",
                                    null
                                )
                                
                                // After loading HTML, initialize pagination (CSS columns) and compute pages.
                                // We post a short delay to allow WebView to finish rendering before measuring.
                                binding.webView.postDelayed({
                                    try {
                                        initEpubPagination()
                                    } catch (e: Exception) {
                                        e.printStackTrace()
                                    }
                                }, 300)

                                binding.webView.animate()
                                    .alpha(1f)
                                    .setDuration(150)
                                    .start()
                            }
                            .start()
                        
                        currentEpubChapter = chapterIndex
                        // Keep book-level currentPage (chapter index) for persistence as before
                        currentPage = chapterIndex
                        // Reset per-chapter pagination state; reuse saved value unless a pending jump already exists
                        if (pendingEpubPageAfterLoad == null) {
                            epubCurrentPageInChapter = epubChapterPagePositions[chapterIndex] ?: 0
                        }
                        epubPageCount = 0
                        currentTextContent = chapterHtml // Store for TTS
                        
                        // Reset TTS state when chapter changes
                        resetTtsStateForNewContent()
                        
                        // Update TTS button state now that content is loaded
                        updateTtsButtons()
                        
                        // Update progress
                        updateProgress(chapterIndex, content.spine.size)
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                    withContext(Dispatchers.Main) {
                        Toast.makeText(
                            this@ViewerActivity,
                            "Error loading chapter: ${e.message}",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                }
            }
        }
    }
    
    private fun wrapEpubChapterHtml(chapterHtml: String): String {
        return """
            <!DOCTYPE html>
            <html>
            <head>
                <meta charset="utf-8"/>
                <meta name="viewport" content="width=device-width, initial-scale=1.0, maximum-scale=3.0, user-scalable=yes">
                <style>
                    html, body {
                        margin: 0;
                        padding: 0;
                        overflow-x: hidden;
                    }
                    body {
                        padding: 16px;
                        font-size: 16px;
                        line-height: 1.6;
                        word-wrap: break-word;
                    }
                    img {
                        max-width: 100% !important;
                        height: auto !important;
                    }
                    [data-tts-chunk] {
                        cursor: pointer;
                        transition: background-color 0.2s ease-in-out;
                    }
                    .tts-highlight {
                        background-color: rgba(255, 213, 79, 0.35) !important;
                    }
                </style>
            </head>
            <body>
                $chapterHtml
            </body>
            </html>
        """.trimIndent()
    }

    /**
     * Initialize EPUB pagination based on the selected layout mode and compute page information
     * for the currently loaded chapter.
     */
    private fun initEpubPagination() {
        try {
            val layoutMode = currentLayoutMode
            val js = when (layoutMode) {
                LayoutMode.CONTINUOUS_SCROLL -> """
                    (function() {
                        try {
                            document.documentElement.style.overflowY = 'auto';
                            document.documentElement.style.overflowX = 'hidden';
                            document.body.style.margin = '0';
                            document.body.style.padding = '0';
                            document.body.style.webkitColumnGap = '0px';
                            document.body.style.columnGap = '0px';
                            document.body.style.webkitColumnWidth = 'auto';
                            document.body.style.columnWidth = 'auto';
                            document.body.style.webkitColumnCount = 'auto';
                            document.body.style.columnCount = 'auto';
                            var pageHeight = window.innerHeight || document.documentElement.clientHeight || 1;
                            var totalHeight = Math.max(document.body.scrollHeight, document.documentElement.scrollHeight, pageHeight);
                            var pageCount = Math.max(1, Math.ceil(totalHeight / pageHeight));
                            return JSON.stringify({ pageCount: pageCount });
                        } catch(e) {
                            return JSON.stringify({ pageCount: 1 });
                        }
                    })();
                """.trimIndent()
                LayoutMode.TWO_COLUMN -> """
                    (function() {
                        try {
                            document.documentElement.style.overflow = 'hidden';
                            var pageWidth = window.innerWidth || document.documentElement.clientWidth || 1;
                            var columnWidth = Math.max(Math.floor(pageWidth / 2), 240);
                            document.body.style.webkitColumnGap = '32px';
                            document.body.style.columnGap = '32px';
                            document.body.style.webkitColumnWidth = columnWidth + 'px';
                            document.body.style.columnWidth = columnWidth + 'px';
                            document.body.style.webkitColumnFill = 'auto';
                            document.body.style.columnFill = 'auto';
                            document.body.style.margin = '0';
                            document.body.style.padding = '0';
                            var totalWidth = Math.max(document.body.scrollWidth, document.documentElement.scrollWidth);
                            var pageCount = Math.max(1, Math.ceil(totalWidth / pageWidth));
                            return JSON.stringify({ pageCount: pageCount });
                        } catch(e) {
                            return JSON.stringify({ pageCount: 1 });
                        }
                    })();
                """.trimIndent()
                else -> """
                    (function() {
                        try {
                            document.documentElement.style.overflow = 'hidden';
                            var pageWidth = window.innerWidth || document.documentElement.clientWidth || 1;
                            document.body.style.webkitColumnGap = '0px';
                            document.body.style.columnGap = '0px';
                            document.body.style.webkitColumnWidth = pageWidth + 'px';
                            document.body.style.columnWidth = pageWidth + 'px';
                            document.body.style.webkitColumnFill = 'auto';
                            document.body.style.columnFill = 'auto';
                            document.body.style.margin = '0';
                            document.body.style.padding = '0';
                            var totalWidth = Math.max(document.body.scrollWidth, document.documentElement.scrollWidth);
                            var pageCount = Math.max(1, Math.ceil(totalWidth / pageWidth));
                            return JSON.stringify({ pageCount: pageCount });
                        } catch(e) {
                            return JSON.stringify({ pageCount: 1 });
                        }
                    })();
                """.trimIndent()
            }

            binding.webView.evaluateJavascript(js) { result ->
                try {
                    val count = parsePageCount(result)
                    epubPageCount = count

                    ensurePaginationInitializedForEpub()
                    paginationManager.updateChapterPageCount(currentEpubChapter, epubPageCount)

                    if (pendingEpubPageAfterLoad != null) {
                        val requested = pendingEpubPageAfterLoad!!
                        val target = if (requested == -1) epubPageCount - 1 else requested
                        epubCurrentPageInChapter = target.coerceIn(0, maxOf(0, epubPageCount - 1))
                        pendingEpubPageAfterLoad = null
                    } else {
                        if (epubCurrentPageInChapter < 0) epubCurrentPageInChapter = 0
                        if (epubCurrentPageInChapter >= epubPageCount) epubCurrentPageInChapter = epubPageCount - 1
                    }

                    updatePageSliderConfiguration()
                    if (epubPageCount > 0) {
                        gotoEpubPage(epubCurrentPageInChapter)
                    }

                    if (layoutMode == LayoutMode.CONTINUOUS_SCROLL) {
                        binding.webView.postDelayed(continuousScrollSyncRunnable, 160)
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                    epubPageCount = 0
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
            epubPageCount = 0
        }
    }

    private fun parsePageCount(result: String?): Int {
        if (result.isNullOrBlank()) {
            return 1
        }

        return try {
            val trimmed = result.trim()
            if (trimmed.startsWith("{")) {
                val json = JSONObject(trimmed)
                json.optInt("pageCount", 1).coerceAtLeast(1)
            } else {
                trimmed.replace("\"", "").toIntOrNull()?.coerceAtLeast(1) ?: 1
            }
        } catch (e: Exception) {
            android.util.Log.w("ViewerActivity", "Failed to parse page count: $result", e)
            1
        }
    }

    private fun syncContinuousScrollPosition() {
        if (currentLayoutMode != LayoutMode.CONTINUOUS_SCROLL || epubContent == null) {
            return
        }

        binding.webView.evaluateJavascript(
            """
            (function() {
                try {
                    var pageHeight = window.innerHeight || document.documentElement.clientHeight || 1;
                    var scrollY = window.scrollY || document.documentElement.scrollTop || 0;
                    var totalHeight = Math.max(document.body.scrollHeight, document.documentElement.scrollHeight, pageHeight);
                    var pageCount = Math.max(1, Math.ceil(totalHeight / pageHeight));
                    var currentPage = Math.floor(scrollY / pageHeight);
                    currentPage = Math.max(0, Math.min(pageCount - 1, currentPage));
                    return JSON.stringify({ pageCount: pageCount, currentPage: currentPage });
                } catch (e) {
                    return JSON.stringify({ pageCount: 1, currentPage: 0 });
                }
            })();
            """.trimIndent()
        ) { result ->
            try {
                if (result.isNullOrBlank()) {
                    return@evaluateJavascript
                }
                val data = JSONObject(result)
                val newCount = data.optInt("pageCount", 1).coerceAtLeast(1)
                val newPage = data.optInt("currentPage", 0).coerceIn(0, newCount - 1)
                val countChanged = newCount != epubPageCount
                val pageChanged = newPage != epubCurrentPageInChapter
                if (!countChanged && !pageChanged) {
                    return@evaluateJavascript
                }

                epubPageCount = newCount
                epubCurrentPageInChapter = newPage
                ensurePaginationInitializedForEpub()
                paginationManager.updateChapterPageCount(currentEpubChapter, epubPageCount)
                updatePageSliderConfiguration()
                updatePageIndicator(epubCurrentPageInChapter + 1, epubPageCount)
                syncPageSliderValue()
                persistEpubProgress()
                updatePaginationSnapshot(tableOfContents.getOrNull(currentEpubChapter)?.title)
            } catch (e: Exception) {
                android.util.Log.w("ViewerActivity", "Failed to sync continuous scroll position", e)
            }
        }
    }

    /** Scroll the WebView horizontally to the given page index (0-based) within the current chapter. */
    private fun gotoEpubPage(pageIndex: Int) {
        val safeIndex = when {
            epubPageCount <= 0 -> 0
            pageIndex < 0 -> 0
            pageIndex >= epubPageCount -> epubPageCount - 1
            else -> pageIndex
        }

        val js = when (currentLayoutMode) {
            LayoutMode.CONTINUOUS_SCROLL -> """
                (function() {
                    try {
                        var page = $safeIndex;
                        var viewport = window.innerHeight || document.documentElement.clientHeight || 1;
                        var y = page * viewport;
                        window.scrollTo(0, y);
                        return true;
                    } catch(e) { return false; }
                })();
            """.trimIndent()
            else -> """
                (function() {
                    try {
                        var page = $safeIndex;
                        var viewport = window.innerWidth || document.documentElement.clientWidth || 1;
                        var x = page * viewport;
                        window.scrollTo(x, 0);
                        return true;
                    } catch(e) { return false; }
                })();
            """.trimIndent()
        }

        binding.webView.evaluateJavascript(js) { _ ->
            epubCurrentPageInChapter = safeIndex

            // Update page indicator using per-chapter pages
            updatePageIndicator(epubCurrentPageInChapter + 1, epubPageCount)
            syncPageSliderValue()
            persistEpubProgress()
            updatePaginationSnapshot(tableOfContents.getOrNull(currentEpubChapter)?.title)
            if (currentLayoutMode == LayoutMode.CONTINUOUS_SCROLL) {
                binding.webView.postDelayed(continuousScrollSyncRunnable, 160)
            }
            prepareTtsNodesInWebView()
        }
    }
    
    private fun parseEpubToc(zipFile: java.util.zip.ZipFile) {
        // This method is now deprecated - TOC is parsed by EpubParser
        // Keeping for compatibility but it does nothing
    }

    private fun deserializeEpubPositions(serialized: String?): MutableMap<Int, Int> {
        if (serialized.isNullOrBlank()) {
            return mutableMapOf()
        }

        return try {
            val json = JSONObject(serialized)
            val map = mutableMapOf<Int, Int>()
            val keys = json.keys()
            while (keys.hasNext()) {
                val key = keys.next()
                val chapter = key.toIntOrNull()
                if (chapter != null) {
                    map[chapter] = json.optInt(key, 0)
                }
            }
            map
        } catch (e: JSONException) {
            e.printStackTrace()
            mutableMapOf()
        }
    }

    private fun serializeEpubPositions(): String {
        val json = JSONObject()
        epubChapterPagePositions.forEach { (chapter, page) ->
            json.put(chapter.toString(), page)
        }
        return json.toString()
    }

    private fun updatePaginationPreferences(preferences: ReadingPreferences) {
        val newKey = PaginationPreferencesKey.from(preferences)
        if (newKey == paginationPreferencesKey) {
            return
        }
        paginationPreferencesKey = newKey
        if (epubContent != null) {
            ensurePaginationInitializedForEpub()
            updatePaginationSnapshot()
        } else if (pdfRenderer != null && totalPdfPages > 0) {
            updateStaticPaginationSnapshot(currentPage, totalPdfPages)
        } else if (totalComicPages > 0) {
            updateStaticPaginationSnapshot(currentPage, totalComicPages)
        } else {
            renderPaginationStatus(null)
        }
    }

    private fun ensurePaginationInitializedForEpub() {
        val content = epubContent ?: return
        if (content.spine.isEmpty()) {
            return
        }

        val identifier = paginationBookIdentifier ?: return
        val key = paginationPreferencesKey
            ?: PaginationPreferencesKey.from(preferencesManager.getReadingPreferences()).also {
                paginationPreferencesKey = it
            }

        paginationManager.initialize(identifier, key, content.spine.size)
        paginationManager.updateTableOfContents(tableOfContents)
    }

    private fun updatePaginationSnapshot(explicitTitle: String? = null): PaginationSnapshot? {
        val content = epubContent ?: return null
        if (content.spine.isEmpty()) {
            return null
        }

        if (paginationBookIdentifier == null) {
            return null
        }

        ensurePaginationInitializedForEpub()

        val chapterIndex = currentEpubChapter.coerceIn(0, content.spine.size - 1)
        val pageIndex = epubCurrentPageInChapter.coerceAtLeast(0)
        val title = explicitTitle ?: tableOfContents.getOrNull(chapterIndex)?.title
        val snapshot = paginationManager.updatePosition(chapterIndex, pageIndex, title)
        renderPaginationStatus(snapshot)
        return snapshot
    }

    private fun updateStaticPaginationSnapshot(currentPageIndex: Int, totalPages: Int) {
        if (totalPages <= 0) {
            renderPaginationStatus(null)
            return
        }

        val identifier = paginationBookIdentifier ?: return
        val key = paginationPreferencesKey
            ?: PaginationPreferencesKey.from(preferencesManager.getReadingPreferences()).also {
                paginationPreferencesKey = it
            }

        paginationManager.initialize(identifier, key, 1)
        paginationManager.updateTableOfContents(emptyList())
        paginationManager.updateChapterPageCount(0, totalPages)

        val safeIndex = currentPageIndex.coerceIn(0, totalPages - 1)
        val title = currentBook?.title ?: supportActionBar?.title?.toString()
        val snapshot = paginationManager.updatePosition(0, safeIndex, title)
        renderPaginationStatus(snapshot)
    }

    private fun renderPaginationStatus(snapshot: PaginationSnapshot?) {
        val statusBar = binding.paginationStatusBar
        if (snapshot == null) {
            statusBar.visibility = View.GONE
            return
        }

        statusBar.visibility = View.VISIBLE

        val rawTitle = snapshot.chapterTitle?.trim()?.takeIf { it.isNotEmpty() }
        val isSingleSection = snapshot.bookPageCount <= snapshot.chapterPageCount || snapshot.bookPageCount <= 0

        val chapterLabel = when {
            rawTitle != null && isSingleSection -> rawTitle
            rawTitle != null -> getString(
                R.string.pagination_chapter_with_title,
                snapshot.chapterDisplayIndex,
                rawTitle
            )
            else -> getString(
                R.string.pagination_chapter_number_only,
                snapshot.chapterDisplayIndex
            )
        }

        binding.paginationChapterText.text = chapterLabel
        binding.paginationChapterText.visibility = if (chapterLabel.isNotEmpty()) View.VISIBLE else View.GONE

        val chapterPageTotal = snapshot.chapterPageCount.coerceAtLeast(snapshot.chapterDisplayPage)
        val bookPageTotal = snapshot.bookPageCount.coerceAtLeast(snapshot.bookDisplayPage)

        binding.paginationPagesText.text = if (!isSingleSection && bookPageTotal > 0) {
            getString(
                R.string.pagination_pages_dual,
                snapshot.chapterDisplayPage,
                chapterPageTotal,
                snapshot.bookDisplayPage,
                bookPageTotal
            )
        } else {
            getString(
                R.string.pagination_pages_single,
                snapshot.chapterDisplayPage,
                chapterPageTotal
            )
        }
    }
    
    private suspend fun loadMobi(file: File) {
        withContext(Dispatchers.IO) {
            try {
                // Basic MOBI/PDB format reading
                val content = extractMobiContent(file)
                
                if (content.contains("Error extracting MOBI content")) {
                    withContext(Dispatchers.Main) {
                        binding.loadingProgressBar.visibility = View.GONE
                        Toast.makeText(
                            this@ViewerActivity,
                            getString(R.string.error_mobi_invalid),
                            Toast.LENGTH_LONG
                        ).show()
                        finish()
                    }
                    return@withContext
                }
                
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
                        
                        setupGenericWebView()

                        webView.loadDataWithBaseURL(null, htmlContent, "text/html", "UTF-8", null)
                        currentTextContent = content
                        
                        // Update TTS button state now that content is loaded
                        updateTtsButtons()
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
                withContext(Dispatchers.Main) {
                    binding.loadingProgressBar.visibility = View.GONE
                    val errorMsg = FileValidator.getErrorMessage(e, this@ViewerActivity, "mobi")
                    Toast.makeText(this@ViewerActivity, errorMsg, Toast.LENGTH_LONG).show()
                    finish()
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
            
            // Try to extract readable text from the file
            val buffer = ByteArray(4096)
            var bytesRead: Int
            val textContent = StringBuilder()
            
            raf.seek(0)
            while (raf.read(buffer).also { bytesRead = it } != -1) {
                val text = String(buffer, 0, bytesRead, Charsets.ISO_8859_1)
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
                comicImages.clear()
                
                // Use Apache Commons Compress for better ZIP handling
                val zipFile = ApacheZipFile(file)
                val entries = zipFile.entries.toList()
                    .filter { !it.isDirectory && it.name.lowercase().let { name -> 
                        name.endsWith(".jpg") || name.endsWith(".jpeg") || 
                        name.endsWith(".png") || name.endsWith(".gif") || 
                        name.endsWith(".bmp") || name.endsWith(".webp")
                    }}
                    .sortedBy { it.name }
                
                if (entries.isEmpty()) {
                    zipFile.close()
                    withContext(Dispatchers.Main) {
                        binding.loadingProgressBar.visibility = View.GONE
                        Toast.makeText(
                            this@ViewerActivity,
                            getString(R.string.error_cbz_invalid),
                            Toast.LENGTH_LONG
                        ).show()
                        finish()
                    }
                    return@withContext
                }
                
                // Load all images
                for (entry in entries) {
                    try {
                        val inputStream = zipFile.getInputStream(entry)
                        val bitmap = BitmapFactory.decodeStream(inputStream)
                        if (bitmap != null) {
                            comicImages.add(bitmap)
                        }
                        inputStream.close()
                    } catch (e: Exception) {
                        e.printStackTrace()
                        // Continue with other images
                    }
                }
                
                zipFile.close()
                totalComicPages = comicImages.size
                
                if (totalComicPages == 0) {
                    withContext(Dispatchers.Main) {
                        binding.loadingProgressBar.visibility = View.GONE
                        Toast.makeText(
                            this@ViewerActivity,
                            getString(R.string.error_cbz_invalid),
                            Toast.LENGTH_LONG
                        ).show()
                        finish()
                    }
                    return@withContext
                }
                
                withContext(Dispatchers.Main) {
                    binding.loadingProgressBar.visibility = View.GONE
                    
                    // TTS is not supported for comic books (image-based format)
                    currentTextContent = ""
                    updateTtsButtons()
                }
                
                // Initialize ViewPager2 for comic page navigation
                initComicViewPager()
            } catch (e: java.util.zip.ZipException) {
                e.printStackTrace()
                withContext(Dispatchers.Main) {
                    binding.loadingProgressBar.visibility = View.GONE
                    Toast.makeText(
                        this@ViewerActivity,
                        getString(R.string.error_cbz_invalid),
                        Toast.LENGTH_LONG
                    ).show()
                    finish()
                }
            } catch (e: OutOfMemoryError) {
                comicImages.forEach { it.recycle() }
                comicImages.clear()
                withContext(Dispatchers.Main) {
                    binding.loadingProgressBar.visibility = View.GONE
                    Toast.makeText(
                        this@ViewerActivity,
                        getString(R.string.error_file_too_large, "100MB"),
                        Toast.LENGTH_LONG
                    ).show()
                    finish()
                }
            } catch (e: Exception) {
                e.printStackTrace()
                withContext(Dispatchers.Main) {
                    binding.loadingProgressBar.visibility = View.GONE
                    val errorMsg = FileValidator.getErrorMessage(e, this@ViewerActivity, "cbz")
                    Toast.makeText(this@ViewerActivity, errorMsg, Toast.LENGTH_LONG).show()
                    finish()
                }
            }
        }
    }
    
    private suspend fun loadCbr(file: File) {
        withContext(Dispatchers.IO) {
            try {
                comicImages.clear()
                
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
                
                if (fileHeaders.isEmpty()) {
                    archive.close()
                    withContext(Dispatchers.Main) {
                        binding.loadingProgressBar.visibility = View.GONE
                        Toast.makeText(
                            this@ViewerActivity,
                            getString(R.string.error_cbr_invalid),
                            Toast.LENGTH_LONG
                        ).show()
                        finish()
                    }
                    return@withContext
                }
                
                // Sort by name
                fileHeaders.sortBy { it.fileName }
                
                // Extract and decode images
                for (header in fileHeaders) {
                    try {
                        val outputStream = ByteArrayOutputStream()
                        archive.extractFile(header, outputStream)
                        val bytes = outputStream.toByteArray()
                        val bitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
                        if (bitmap != null) {
                            comicImages.add(bitmap)
                        }
                        outputStream.close()
                    } catch (e: Exception) {
                        e.printStackTrace()
                        // Continue with other images
                    }
                }
                
                archive.close()
                totalComicPages = comicImages.size
                
                if (totalComicPages == 0) {
                    withContext(Dispatchers.Main) {
                        binding.loadingProgressBar.visibility = View.GONE
                        Toast.makeText(
                            this@ViewerActivity,
                            getString(R.string.error_cbr_invalid),
                            Toast.LENGTH_LONG
                        ).show()
                        finish()
                    }
                    return@withContext
                }
                
                withContext(Dispatchers.Main) {
                    binding.loadingProgressBar.visibility = View.GONE
                    
                    // TTS is not supported for comic books (image-based format)
                    currentTextContent = ""
                    updateTtsButtons()
                }
                
                // Initialize ViewPager2 for comic page navigation
                initComicViewPager()
            } catch (e: com.github.junrar.exception.RarException) {
                e.printStackTrace()
                withContext(Dispatchers.Main) {
                    binding.loadingProgressBar.visibility = View.GONE
                    Toast.makeText(
                        this@ViewerActivity,
                        getString(R.string.error_cbr_invalid),
                        Toast.LENGTH_LONG
                    ).show()
                    finish()
                }
            } catch (e: OutOfMemoryError) {
                comicImages.forEach { it.recycle() }
                comicImages.clear()
                withContext(Dispatchers.Main) {
                    binding.loadingProgressBar.visibility = View.GONE
                    Toast.makeText(
                        this@ViewerActivity,
                        getString(R.string.error_file_too_large, "100MB"),
                        Toast.LENGTH_LONG
                    ).show()
                    finish()
                }
            } catch (e: Exception) {
                e.printStackTrace()
                withContext(Dispatchers.Main) {
                    binding.loadingProgressBar.visibility = View.GONE
                    val errorMsg = FileValidator.getErrorMessage(e, this@ViewerActivity, "cbr")
                    Toast.makeText(this@ViewerActivity, errorMsg, Toast.LENGTH_LONG).show()
                    finish()
                }
            }
        }
    }
    
    private fun renderComicPage(pageIndex: Int) {
        if (pageIndex < 0 || pageIndex >= totalComicPages) return
        
        val bitmap = comicImages[pageIndex]
        
        // Apply page flip animation
        binding.pdfImageView.animate()
            .alpha(0f)
            .setDuration(150)
            .withEndAction {
                binding.apply {
                    pdfImageView.visibility = View.VISIBLE
                    pdfImageView.setImageBitmap(bitmap)
                    webView.visibility = View.GONE
                    scrollView.visibility = View.GONE
                    textView.visibility = View.GONE
                }
                
                binding.pdfImageView.animate()
                    .alpha(1f)
                    .setDuration(150)
                    .start()
            }
            .start()
        
        currentPage = pageIndex
        updateProgress(pageIndex, totalComicPages)
    }
    
    private suspend fun loadText(file: File) {
        val content = withContext(Dispatchers.IO) {
            file.readText()
        }
        
        withContext(Dispatchers.Main) {
            binding.apply {
                loadingProgressBar.visibility = View.GONE
                // pdfView.visibility = View.GONE
                pdfImageView.visibility = View.GONE
                webView.visibility = View.GONE
                scrollView.visibility = View.VISIBLE
                textView.visibility = View.VISIBLE
                
                textView.text = content
                currentTextContent = content // Store for TTS
                
                // Update TTS button state now that content is loaded
                updateTtsButtons()
            }
            
            // Apply reading preferences
            val preferences = preferencesManager.getReadingPreferences()
            applyReadingPreferences(preferences)
        }
    }
    
    private suspend fun loadFb2(file: File) {
        val fb2Content = withContext(Dispatchers.IO) {
            com.rifters.ebookreader.util.Fb2Parser.parse(file)
        }
        
        if (fb2Content == null) {
            withContext(Dispatchers.Main) {
                Toast.makeText(
                    this@ViewerActivity,
                    "Error loading FB2 file",
                    Toast.LENGTH_LONG
                ).show()
                finish()
            }
            return
        }
        
        withContext(Dispatchers.Main) {
            binding.apply {
                loadingProgressBar.visibility = View.GONE
                pdfImageView.visibility = View.GONE
                webView.visibility = View.VISIBLE
                scrollView.visibility = View.GONE
                textView.visibility = View.GONE
                
                setupGenericWebView()

                webView.loadDataWithBaseURL(null, fb2Content.htmlContent, "text/html", "UTF-8", null)
                currentTextContent = fb2Content.htmlContent
                
                // Update TTS button state now that content is loaded
                updateTtsButtons()
            }
            
            // Update book metadata if available
            currentBook?.let { book ->
                val updatedBook = book.copy(
                    genre = fb2Content.metadata.genre ?: book.genre,
                    publisher = fb2Content.metadata.publisher ?: book.publisher,
                    publishedYear = fb2Content.metadata.publishYear?.toIntOrNull() ?: book.publishedYear,
                    language = fb2Content.metadata.language ?: book.language,
                    isbn = fb2Content.metadata.isbn ?: book.isbn
                )
                bookViewModel.updateBook(updatedBook)
            }
            
            val preferences = preferencesManager.getReadingPreferences()
            applyThemeToUI(preferences)
            applyWebViewStyles(preferences)
        }
    }
    
    private suspend fun loadMarkdown(file: File) {
        val htmlContent = withContext(Dispatchers.IO) {
            com.rifters.ebookreader.util.MarkdownParser.parseToHtml(file)
        }
        
        withContext(Dispatchers.Main) {
            binding.apply {
                loadingProgressBar.visibility = View.GONE
                pdfImageView.visibility = View.GONE
                webView.visibility = View.VISIBLE
                scrollView.visibility = View.GONE
                textView.visibility = View.GONE
                
                setupGenericWebView()

                webView.loadDataWithBaseURL(null, htmlContent, "text/html", "UTF-8", null)
                currentTextContent = htmlContent
                
                // Update TTS button state now that content is loaded
                updateTtsButtons()
            }
            
            val preferences = preferencesManager.getReadingPreferences()
            applyThemeToUI(preferences)
            applyWebViewStyles(preferences)
        }
    }
    
    private suspend fun loadHtml(file: File) {
        val htmlContent = withContext(Dispatchers.IO) {
            file.readText(Charsets.UTF_8)
        }
        
        withContext(Dispatchers.Main) {
            binding.apply {
                loadingProgressBar.visibility = View.GONE
                pdfImageView.visibility = View.GONE
                webView.visibility = View.VISIBLE
                scrollView.visibility = View.GONE
                textView.visibility = View.GONE
                
                setupGenericWebView()

                webView.loadDataWithBaseURL(null, htmlContent, "text/html", "UTF-8", null)
                currentTextContent = htmlContent
                
                // Update TTS button state now that content is loaded
                updateTtsButtons()
            }
            
            val preferences = preferencesManager.getReadingPreferences()
            applyThemeToUI(preferences)
            applyWebViewStyles(preferences)
        }
    }
    
    private suspend fun loadAzw(file: File) {
        val azwContent = withContext(Dispatchers.IO) {
            com.rifters.ebookreader.util.AzwParser.parse(file)
        }
        
        if (azwContent == null) {
            withContext(Dispatchers.Main) {
                Toast.makeText(
                    this@ViewerActivity,
                    "Error loading AZW/AZW3 file",
                    Toast.LENGTH_LONG
                ).show()
                finish()
            }
            return
        }
        
        withContext(Dispatchers.Main) {
            binding.apply {
                loadingProgressBar.visibility = View.GONE
                pdfImageView.visibility = View.GONE
                webView.visibility = View.GONE
                scrollView.visibility = View.VISIBLE
                textView.visibility = View.VISIBLE
                
                textView.text = azwContent.content
                currentTextContent = azwContent.content
                
                // Update TTS button state now that content is loaded
                updateTtsButtons()
            }
            
            val preferences = preferencesManager.getReadingPreferences()
            applyReadingPreferences(preferences)
        }
    }
    
    private suspend fun loadDocx(file: File) {
        val docxContent = withContext(Dispatchers.IO) {
            com.rifters.ebookreader.util.DocxParser.parse(file)
        }
        
        if (docxContent == null) {
            withContext(Dispatchers.Main) {
                Toast.makeText(
                    this@ViewerActivity,
                    "Error loading DOCX file",
                    Toast.LENGTH_LONG
                ).show()
                finish()
            }
            return
        }
        
        withContext(Dispatchers.Main) {
            binding.apply {
                loadingProgressBar.visibility = View.GONE
                pdfImageView.visibility = View.GONE
                webView.visibility = View.VISIBLE
                scrollView.visibility = View.GONE
                textView.visibility = View.GONE
                
                setupGenericWebView()

                webView.loadDataWithBaseURL(null, docxContent.htmlContent, "text/html", "UTF-8", null)
                currentTextContent = docxContent.htmlContent
                
                // Update TTS button state now that content is loaded
                updateTtsButtons()
            }
            
            // Update book metadata if available
            currentBook?.let { book ->
                val updatedBook = book.copy(
                    author = docxContent.metadata.author ?: book.author,
                    publisher = docxContent.metadata.subject ?: book.publisher
                )
                bookViewModel.updateBook(updatedBook)
            }
            
            val preferences = preferencesManager.getReadingPreferences()
            applyThemeToUI(preferences)
            applyWebViewStyles(preferences)
        }
    }
    
    private suspend fun loadCb7(file: File) {
        withContext(Dispatchers.IO) {
            try {
                comicImages.clear()
                
                // Use Apache Commons Compress for 7z support
                val sevenZFile = org.apache.commons.compress.archivers.sevenz.SevenZFile(file)
                val imageList = mutableListOf<Bitmap>()
                
                var entry = sevenZFile.nextEntry
                while (entry != null) {
                    if (!entry.isDirectory) {
                        val fileName = entry.name.lowercase()
                        if (fileName.endsWith(".jpg") || fileName.endsWith(".jpeg") || 
                            fileName.endsWith(".png") || fileName.endsWith(".gif") || 
                            fileName.endsWith(".bmp") || fileName.endsWith(".webp")) {
                            
                            val content = ByteArray(entry.size.toInt())
                            sevenZFile.read(content)
                            
                            val bitmap = BitmapFactory.decodeByteArray(content, 0, content.size)
                            if (bitmap != null) {
                                imageList.add(bitmap)
                            }
                        }
                    }
                    entry = sevenZFile.nextEntry
                }
                
                sevenZFile.close()
                
                if (imageList.isEmpty()) {
                    withContext(Dispatchers.Main) {
                        binding.loadingProgressBar.visibility = View.GONE
                        Toast.makeText(
                            this@ViewerActivity,
                            "No images found in CB7 file",
                            Toast.LENGTH_LONG
                        ).show()
                        finish()
                    }
                    return@withContext
                }
                
                comicImages.addAll(imageList)
                totalComicPages = comicImages.size
                
                withContext(Dispatchers.Main) {
                    binding.loadingProgressBar.visibility = View.GONE
                    
                    // TTS is not supported for comic books (image-based format)
                    currentTextContent = ""
                    updateTtsButtons()
                }
                
                // Initialize ViewPager2 for comic page navigation
                initComicViewPager()
            } catch (e: Exception) {
                e.printStackTrace()
                withContext(Dispatchers.Main) {
                    binding.loadingProgressBar.visibility = View.GONE
                    Toast.makeText(
                        this@ViewerActivity,
                        "Error loading CB7 file: ${e.message}",
                        Toast.LENGTH_LONG
                    ).show()
                    finish()
                }
            }
        }
    }
    
    private suspend fun loadCbt(file: File) {
        withContext(Dispatchers.IO) {
            try {
                comicImages.clear()
                
                // Use Apache Commons Compress for TAR support
                val tarInput = org.apache.commons.compress.archivers.tar.TarArchiveInputStream(
                    java.io.FileInputStream(file)
                )
                val imageList = mutableListOf<Bitmap>()
                
                var entry = tarInput.nextTarEntry
                while (entry != null) {
                    if (!entry.isDirectory) {
                        val fileName = entry.name.lowercase()
                        if (fileName.endsWith(".jpg") || fileName.endsWith(".jpeg") || 
                            fileName.endsWith(".png") || fileName.endsWith(".gif") || 
                            fileName.endsWith(".bmp") || fileName.endsWith(".webp")) {
                            
                            val content = ByteArray(entry.size.toInt())
                            tarInput.read(content)
                            
                            val bitmap = BitmapFactory.decodeByteArray(content, 0, content.size)
                            if (bitmap != null) {
                                imageList.add(bitmap)
                            }
                        }
                    }
                    entry = tarInput.nextTarEntry
                }
                
                tarInput.close()
                
                if (imageList.isEmpty()) {
                    withContext(Dispatchers.Main) {
                        binding.loadingProgressBar.visibility = View.GONE
                        Toast.makeText(
                            this@ViewerActivity,
                            "No images found in CBT file",
                            Toast.LENGTH_LONG
                        ).show()
                        finish()
                    }
                    return@withContext
                }
                
                comicImages.addAll(imageList)
                totalComicPages = comicImages.size
                
                withContext(Dispatchers.Main) {
                    binding.loadingProgressBar.visibility = View.GONE
                    
                    // TTS is not supported for comic books (image-based format)
                    currentTextContent = ""
                    updateTtsButtons()
                }
                
                // Initialize ViewPager2 for comic page navigation
                initComicViewPager()
            } catch (e: Exception) {
                e.printStackTrace()
                withContext(Dispatchers.Main) {
                    binding.loadingProgressBar.visibility = View.GONE
                    Toast.makeText(
                        this@ViewerActivity,
                        "Error loading CBT file: ${e.message}",
                        Toast.LENGTH_LONG
                    ).show()
                    finish()
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
            
            // Update page indicator
            updatePageIndicator(boundedPage + 1, totalPages)

            if (epubContent == null && totalPages > 0) {
                updateStaticPaginationSnapshot(boundedPage, totalPages)
            }
        }
    }
    
    private fun updatePageIndicator(currentPage: Int, totalPages: Int) {
        if (totalPages > 0) {
            binding.pageIndicator.visibility = View.VISIBLE
            binding.pageIndicator.text = getString(R.string.page_indicator, currentPage, totalPages)
            
            // Auto-hide after 2 seconds
            binding.pageIndicator.removeCallbacks(hidePageIndicatorRunnable)
            binding.pageIndicator.postDelayed(hidePageIndicatorRunnable, 2000)
        } else {
            binding.pageIndicator.visibility = View.GONE
        }
    }
    
    private val hidePageIndicatorRunnable = Runnable {
        binding.pageIndicator.animate()
            .alpha(0f)
            .setDuration(300)
            .withEndAction {
                binding.pageIndicator.visibility = View.GONE
                binding.pageIndicator.alpha = 1f
            }
            .start()
    }
    
    private fun previousPage() {
        if (isUsingViewPager && binding.viewPager.visibility == View.VISIBLE) {
            // Use ViewPager2 navigation
            val current = binding.viewPager.currentItem
            if (current > 0) {
                binding.viewPager.currentItem = current - 1
            } else {
                Toast.makeText(this, "Already at first page", Toast.LENGTH_SHORT).show()
            }
        } else if (pdfRenderer != null && totalPdfPages > 0) {
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
        } else if (epubContent != null) {
            // If EPUB pagination is initialized use page-based navigation within chapter
            if (epubPageCount > 0) {
                if (epubCurrentPageInChapter > 0) {
                    epubCurrentPageInChapter--
                    gotoEpubPage(epubCurrentPageInChapter)
                } else {
                    // At first page of chapter, try to go to previous chapter's last page
                    if (currentEpubChapter > 0) {
                        // Request that after the previous chapter loads we jump to its last page
                        pendingEpubPageAfterLoad = -1
                        renderEpubChapter(currentEpubChapter - 1)
                    } else {
                        Toast.makeText(this, "Already at first chapter", Toast.LENGTH_SHORT).show()
                    }
                }
            } else {
                // Fallback to chapter navigation
                if (currentEpubChapter > 0) {
                    renderEpubChapter(currentEpubChapter - 1)
                } else {
                    Toast.makeText(this, "Already at first chapter", Toast.LENGTH_SHORT).show()
                }
            }
        } else {
            Toast.makeText(this, "Page navigation not available for this format", Toast.LENGTH_SHORT).show()
        }
    }
    
    private fun nextPage() {
        if (isUsingViewPager && binding.viewPager.visibility == View.VISIBLE) {
            // Use ViewPager2 navigation
            val current = binding.viewPager.currentItem
            val total = binding.viewPager.adapter?.itemCount ?: 0
            if (current < total - 1) {
                binding.viewPager.currentItem = current + 1
            } else {
                Toast.makeText(this, "Already at last page", Toast.LENGTH_SHORT).show()
            }
        } else if (pdfRenderer != null && totalPdfPages > 0) {
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
        } else if (epubContent != null) {
            // If EPUB pagination is initialized use page-based navigation within chapter
            if (epubPageCount > 0) {
                if (epubCurrentPageInChapter < epubPageCount - 1) {
                    epubCurrentPageInChapter++
                    gotoEpubPage(epubCurrentPageInChapter)
                } else {
                    // At last page of chapter, advance to next chapter and stay at its first page
                    if (currentEpubChapter < epubContent!!.spine.size - 1) {
                        renderEpubChapter(currentEpubChapter + 1)
                    } else {
                        Toast.makeText(this, "Already at last chapter", Toast.LENGTH_SHORT).show()
                    }
                }
            } else {
                // Fallback to chapter navigation
                if (currentEpubChapter < epubContent!!.spine.size - 1) {
                    renderEpubChapter(currentEpubChapter + 1)
                } else {
                    Toast.makeText(this, "Already at last chapter", Toast.LENGTH_SHORT).show()
                }
            }
        } else {
            Toast.makeText(this, "Page navigation not available for this format", Toast.LENGTH_SHORT).show()
        }
    }
    
    private fun bookmarkCurrentPage() {
        currentBook?.let { book ->
            lifecycleScope.launch {
                try {
                    val bookmarkPage = when {
                        epubContent != null -> currentEpubChapter
                        else -> currentPage
                    }
                    val bookmarkPosition = when {
                        epubContent != null -> epubCurrentPageInChapter
                        else -> 0
                    }
                    val bookmark = com.rifters.ebookreader.model.Bookmark(
                        bookId = book.id,
                        page = bookmarkPage,
                        position = bookmarkPosition,
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
    
    private fun showHighlights() {
        currentBook?.let { book ->
            val bottomSheet = HighlightsBottomSheet.newInstance(book.id)
            bottomSheet.setOnHighlightSelectedListener { highlight ->
                navigateToHighlight(highlight)
            }
            bottomSheet.show(supportFragmentManager, HighlightsBottomSheet.TAG)
        } ?: run {
            Toast.makeText(this, "No book loaded", Toast.LENGTH_SHORT).show()
        }
    }

    private fun showGoToPageDialog() {
        if (epubContent == null) {
            Toast.makeText(this, getString(R.string.epub_only_action), Toast.LENGTH_SHORT).show()
            return
        }

        if (epubPageCount <= 0) {
            Toast.makeText(this, getString(R.string.epub_pagination_not_ready), Toast.LENGTH_SHORT).show()
            return
        }

        val density = resources.displayMetrics.density
        val horizontalPadding = (24 * density).toInt()
        val verticalPadding = (12 * density).toInt()

        val container = FrameLayout(this).apply {
            setPadding(horizontalPadding, verticalPadding, horizontalPadding, 0)
        }

        val inputLayout = TextInputLayout(this).apply {
            hint = getString(R.string.page_number_hint)
        }

        val editText = TextInputEditText(inputLayout.context).apply {
            inputType = InputType.TYPE_CLASS_NUMBER
            val currentDisplayPage = (epubCurrentPageInChapter + 1).coerceAtLeast(1)
            setText(currentDisplayPage.toString())
            setSelection(text?.length ?: 0)
        }

        inputLayout.addView(editText)
        container.addView(inputLayout)

        MaterialAlertDialogBuilder(this)
            .setTitle(R.string.go_to_page)
            .setView(container)
            .setPositiveButton(R.string.go_to_page) { _, _ ->
                val pageValue = editText.text?.toString()?.trim()?.toIntOrNull()
                if (pageValue == null || pageValue < 1 || pageValue > epubPageCount) {
                    Toast.makeText(
                        this,
                        getString(R.string.invalid_page_number, epubPageCount),
                        Toast.LENGTH_SHORT
                    ).show()
                } else {
                    gotoEpubPage(pageValue - 1)
                }
            }
            .setNegativeButton(R.string.cancel, null)
            .show()
    }
    
    private fun navigateToHighlight(highlight: com.rifters.ebookreader.model.Highlight) {
        if (pdfRenderer != null && totalPdfPages > 0) {
            // For PDF files, navigate to the highlighted page
            if (highlight.page in 0 until totalPdfPages) {
                currentPage = highlight.page
                renderPdfPage(currentPage)
                Toast.makeText(
                    this,
                    getString(R.string.page_format, highlight.page + 1),
                    Toast.LENGTH_SHORT
                ).show()
            } else {
                Toast.makeText(this, "Invalid page number", Toast.LENGTH_SHORT).show()
            }
        } else if (totalComicPages > 0) {
            // For comic books (CBZ/CBR), navigate to the highlighted page
            if (highlight.page in 0 until totalComicPages) {
                currentPage = highlight.page
                renderComicPage(currentPage)
                Toast.makeText(
                    this,
                    getString(R.string.page_format, highlight.page + 1),
                    Toast.LENGTH_SHORT
                ).show()
            } else {
                Toast.makeText(this, "Invalid page number", Toast.LENGTH_SHORT).show()
            }
        } else if (epubContent != null) {
            val chapterIndex = highlight.page
            if (chapterIndex in 0 until epubContent!!.spine.size) {
                pendingEpubPageAfterLoad = highlight.position
                renderEpubChapter(chapterIndex)
                if (highlight.position >= 0) {
                    Toast.makeText(
                        this,
                        getString(R.string.page_format, highlight.position + 1),
                        Toast.LENGTH_SHORT
                    ).show()
                } else {
                    Toast.makeText(
                        this,
                        getString(R.string.chapter_format, chapterIndex + 1),
                        Toast.LENGTH_SHORT
                    ).show()
                }
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
    
    private fun navigateToBookmark(bookmark: Bookmark) {
        if (isUsingViewPager && binding.viewPager.visibility == View.VISIBLE) {
            // Use ViewPager2 navigation for PDFs and comic books
            if (bookmark.page in 0 until (binding.viewPager.adapter?.itemCount ?: 0)) {
                binding.viewPager.setCurrentItem(bookmark.page, true)
                Toast.makeText(
                    this,
                    getString(R.string.page_format, bookmark.page + 1),
                    Toast.LENGTH_SHORT
                ).show()
            } else {
                Toast.makeText(this, "Invalid page number", Toast.LENGTH_SHORT).show()
            }
        } else if (pdfRenderer != null && totalPdfPages > 0) {
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
        } else if (epubContent != null) {
            val chapterIndex = bookmark.page
            if (chapterIndex in 0 until epubContent!!.spine.size) {
                pendingEpubPageAfterLoad = bookmark.position
                renderEpubChapter(chapterIndex)
                if (bookmark.position >= 0) {
                    Toast.makeText(
                        this,
                        getString(R.string.page_format, bookmark.position + 1),
                        Toast.LENGTH_SHORT
                    ).show()
                } else {
                    Toast.makeText(
                        this,
                        getString(R.string.chapter_format, chapterIndex + 1),
                        Toast.LENGTH_SHORT
                    ).show()
                }
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
    
    private fun showTableOfContents() {
        if (tableOfContents.isEmpty()) {
            Toast.makeText(this, R.string.no_table_of_contents, Toast.LENGTH_SHORT).show()
            return
        }
        
        val bottomSheet = TocBottomSheet.newInstance(tableOfContents)
        bottomSheet.setOnTocItemSelectedListener { tocItem ->
            navigateToTocItem(tocItem)
        }
        bottomSheet.show(supportFragmentManager, TocBottomSheet.TAG)
    }
    
    private fun navigateToTocItem(tocItem: com.rifters.ebookreader.model.TableOfContentsItem) {
        // Navigation depends on the book format
        if (pdfRenderer != null && totalPdfPages > 0) {
            // For PDF files, navigate to the page
            if (tocItem.page in 0 until totalPdfPages) {
                currentPage = tocItem.page
                renderPdfPage(currentPage)
                Toast.makeText(
                    this,
                    getString(R.string.page_format, tocItem.page + 1),
                    Toast.LENGTH_SHORT
                ).show()
            }
        } else if (epubContent != null) {
            // For EPUB files, try to find the chapter that matches the TOC href
            val href = tocItem.href
            
            // Find the spine item that matches this href
            var chapterIndex = -1
            epubContent?.spine?.forEachIndexed { index, spineItem ->
                val manifestItem = epubContent?.manifest?.get(spineItem.idref)
                if (manifestItem != null) {
                    val fullHref = epubContent!!.opfBasePath + manifestItem.href
                    // Check if the TOC href matches or is contained in the manifest href
                    if (href.contains(manifestItem.href) || fullHref.contains(href)) {
                        chapterIndex = index
                        return@forEachIndexed
                    }
                }
            }
            
            if (chapterIndex >= 0) {
                renderEpubChapter(chapterIndex)
                Toast.makeText(this, tocItem.title, Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(this, "Chapter not found", Toast.LENGTH_SHORT).show()
            }
        }
    }
    
    private fun toggleNightMode() {
        isNightModeEnabled = !isNightModeEnabled
        
        // Save night mode state
        preferencesManager.setNightModeEnabled(isNightModeEnabled)
        
        // Apply night mode
        applyNightMode()
        
        val message = if (isNightModeEnabled) {
            R.string.night_mode_on
        } else {
            R.string.night_mode_off
        }
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
    }
    
    private fun applyNightMode() {
        if (isNightModeEnabled) {
            applyNightModeColors()
        } else {
            // Restore original theme
            val preferences = preferencesManager.getReadingPreferences()
            applyThemeToUI(preferences)
            applyReadingPreferences(preferences)
        }
    }
    
    private fun applyNightModeColors() {
        val nightBackgroundColor = 0xFF1C1C1C.toInt() // Dark gray
        val nightTextColor = 0xFFE0E0E0.toInt() // Light gray
        
        // Apply to content container
        binding.contentContainer.setBackgroundColor(nightBackgroundColor)
        
        // Apply to TextView (for TXT files)
        if (binding.textView.visibility == View.VISIBLE) {
            binding.textView.setTextColor(nightTextColor)
            binding.textView.setBackgroundColor(nightBackgroundColor)
        }
        
        // Apply to WebView (for EPUB/MOBI files)
        if (binding.webView.visibility == View.VISIBLE) {
            applyNightModeToWebView()
        }
        
        // Apply to PDF ImageView (affects background behind the image)
        if (binding.pdfImageView.visibility == View.VISIBLE) {
            binding.pdfImageView.setBackgroundColor(nightBackgroundColor)
        }
    }
    
    private fun applyNightModeToWebView() {
        val css = """
            body { 
                background-color: #1C1C1C !important; 
                color: #E0E0E0 !important; 
            }
            * {
                background-color: transparent !important;
                color: #E0E0E0 !important;
            }
            p, div, span, h1, h2, h3, h4, h5, h6 {
                color: #E0E0E0 !important;
            }
            a {
                color: #82B1FF !important;
            }
        """.trimIndent()
        
        binding.webView.evaluateJavascript(
            """
            (function() {
                var existingStyle = document.getElementById('night-mode-style');
                if (existingStyle) {
                    existingStyle.remove();
                }
                var style = document.createElement('style');
                style.id = 'night-mode-style';
                style.innerHTML = `$css`;
                document.head.appendChild(style);
            })();
            """.trimIndent(),
            null
        )
    }
    
    private fun lookupWord(word: String) {
        if (word.isBlank()) {
            return
        }
        
        try {
            // Try to use Android's built-in dictionary/define intent
            val intent = android.content.Intent(android.content.Intent.ACTION_DEFINE)
            intent.putExtra(android.content.Intent.EXTRA_TEXT, word)
            
            // Check if there's an app that can handle this intent
            if (intent.resolveActivity(packageManager) != null) {
                startActivity(intent)
            } else {
                // Fallback: Use web search for definition
                val searchIntent = android.content.Intent(android.content.Intent.ACTION_WEB_SEARCH)
                searchIntent.putExtra(android.app.SearchManager.QUERY, "define $word")
                
                if (searchIntent.resolveActivity(packageManager) != null) {
                    startActivity(searchIntent)
                } else {
                    Toast.makeText(this, R.string.no_dictionary_app, Toast.LENGTH_SHORT).show()
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
            Toast.makeText(this, R.string.no_dictionary_app, Toast.LENGTH_SHORT).show()
        }
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
        updatePaginationPreferences(preferences)
        // Apply theme to all UI elements
        applyThemeToUI(preferences)
        
        // Update layout mode and ViewPager orientation if using ViewPager2
        val oldLayoutMode = currentLayoutMode
        currentLayoutMode = preferences.layoutMode
        
        if (isUsingViewPager && binding.viewPager.visibility == View.VISIBLE) {
            // Update ViewPager2 orientation based on layout mode
            when (preferences.layoutMode) {
                LayoutMode.CONTINUOUS_SCROLL -> {
                    binding.viewPager.setVerticalMode()
                }
                else -> {
                    binding.viewPager.setHorizontalMode()
                }
            }
        }
        
        // Apply to TextView (for TXT files)
        if (binding.textView.visibility == View.VISIBLE) {
            binding.textView.apply {
                setTextColor(preferences.theme.textColor)
                setTextSize(TypedValue.COMPLEX_UNIT_SP, preferences.fontSize.toFloat())
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
            if (epubContent != null) {
                binding.webView.postDelayed({ initEpubPagination() }, 150)
            }
        }
        
        // Apply to ScrollView container
        binding.scrollView.setPadding(0, 0, 0, 0)
    }
    
    private fun applyWebViewStyles(preferences: ReadingPreferences) {
        val backgroundColor = String.format("#%06X", 0xFFFFFF and preferences.theme.backgroundColor)
        val textColor = String.format("#%06X", 0xFFFFFF and preferences.theme.textColor)
        val layoutCss = when (preferences.layoutMode) {
            LayoutMode.CONTINUOUS_SCROLL -> """
                html, body {
                    overflow-y: auto;
                    overflow-x: hidden;
                }
            """.trimIndent()
            else -> """
                html, body {
                    overflow-y: hidden;
                    overflow-x: hidden;
                }
            """.trimIndent()
        }
        
        val css = """
            $layoutCss
            body {
                font-family: ${preferences.fontFamily} !important;
                font-size: ${preferences.fontSize}px !important;
                line-height: ${preferences.lineSpacing} !important;
                color: $textColor !important;
                background-color: $backgroundColor !important;
                padding: ${preferences.marginVertical}px ${preferences.marginHorizontal}px !important;
                margin: 0 !important;
            }
            * {
                color: $textColor !important;
                background-color: transparent !important;
            }
            p, div, span, h1, h2, h3, h4, h5, h6 {
                color: $textColor !important;
            }
            [data-tts-chunk] {
                cursor: pointer !important;
                transition: background-color 0.2s ease-in-out !important;
            }
            .tts-highlight {
                background-color: rgba(255, 213, 79, 0.35) !important;
            }
        """.trimIndent()
        
        binding.webView.evaluateJavascript(
            """
            (function() {
                var existingStyle = document.getElementById('reading-preferences-style');
                if (existingStyle) {
                    existingStyle.remove();
                }
                var style = document.createElement('style');
                style.id = 'reading-preferences-style';
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

        binding.webView.removeCallbacks(continuousScrollSyncRunnable)
        
        // Close PDF renderer
        try {
            pdfRenderer?.close()
            pdfFileDescriptor?.close()
        } catch (e: Exception) {
            e.printStackTrace()
        }
        
        // Clean up comic book images
        comicImages.forEach { it.recycle() }
        comicImages.clear()
        
        super.onDestroy()
    }
}
