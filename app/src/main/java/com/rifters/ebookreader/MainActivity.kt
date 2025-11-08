package com.rifters.ebookreader

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.snackbar.Snackbar
import com.rifters.ebookreader.databinding.ActivityMainBinding
import com.rifters.ebookreader.util.PreferencesManager
import com.rifters.ebookreader.viewmodel.BookViewModel
import com.rifters.ebookreader.viewmodel.CollectionViewModel
import com.rifters.ebookreader.viewmodel.SyncViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream

class MainActivity : AppCompatActivity() {
    
    private lateinit var binding: ActivityMainBinding
    private lateinit var bookViewModel: BookViewModel
    private lateinit var collectionViewModel: CollectionViewModel
    private lateinit var syncViewModel: SyncViewModel
    private lateinit var bookAdapter: BookAdapter
    private lateinit var prefsManager: PreferencesManager
    private var syncMenuItem: MenuItem? = null
    
    private val filePickerLauncher = registerForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let { handleSelectedFile(it) }
    }
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        
        prefsManager = PreferencesManager(this)
        
        setupToolbar()
        setupRecyclerView()
        setupViewModel()
        setupFab()
        setupSyncObservers()
    }
    
    private fun setupToolbar() {
        setSupportActionBar(binding.toolbar)
        supportActionBar?.title = getString(R.string.app_name)
    }
    
    private fun setupRecyclerView() {
        bookAdapter = BookAdapter(
            onBookClick = { book ->
                openBook(book)
            },
            onBookLongClick = { book ->
                showAddToCollectionDialog(book)
            }
        )
        
        binding.recyclerView.apply {
            adapter = bookAdapter
            layoutManager = LinearLayoutManager(this@MainActivity)
        }
    }
    
    private fun setupViewModel() {
        bookViewModel = ViewModelProvider(this)[BookViewModel::class.java]
        collectionViewModel = ViewModelProvider(this)[CollectionViewModel::class.java]
        syncViewModel = ViewModelProvider(this)[SyncViewModel::class.java]
        
        bookViewModel.allBooks.observe(this) { books ->
            bookAdapter.submitList(books)
        }
    }
    
    private fun setupSyncObservers() {
        // Observe sync status
        syncViewModel.syncStatus.observe(this) { status ->
            when (status) {
                is SyncViewModel.SyncStatus.InProgress -> {
                    showSyncSnackbar(status.message)
                }
                is SyncViewModel.SyncStatus.Success -> {
                    showSyncSnackbar(status.message)
                }
                is SyncViewModel.SyncStatus.Error -> {
                    showSyncSnackbar(status.message)
                }
                is SyncViewModel.SyncStatus.PartialSuccess -> {
                    showSyncSnackbar(status.message)
                }
                else -> {}
            }
        }
        
        // Observe pending sync count to update menu icon
        syncViewModel.pendingSyncCount.observe(this) { count ->
            syncMenuItem?.let { menuItem ->
                if (count > 0 && prefsManager.isSyncEnabled()) {
                    menuItem.title = getString(R.string.pending_sync, count)
                } else {
                    menuItem.title = getString(R.string.sync)
                }
            }
        }
    }
    
    private fun setupFab() {
        binding.fabAddBook.setOnClickListener {
            openFilePicker()
        }
    }
    
    private fun openBook(book: Book) {
        val intent = Intent(this, ViewerActivity::class.java).apply {
            putExtra("book_id", book.id)
            putExtra("book_path", book.filePath)
            putExtra("book_title", book.title)
        }
        startActivity(intent)
    }
    
    private fun openFilePicker() {
        filePickerLauncher.launch("*/*")
    }
    
    private fun handleSelectedFile(uri: Uri) {
        lifecycleScope.launch(Dispatchers.IO) {
            try {
                val inputStream = contentResolver.openInputStream(uri)
                if (inputStream == null) {
                    withContext(Dispatchers.Main) {
                        Toast.makeText(
                            this@MainActivity,
                            "Failed to open file",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                    return@launch
                }
                
                // Get file name and create local copy
                val fileName = getFileName(uri) ?: "book_${System.currentTimeMillis()}"
                val storageDir = getExternalFilesDir(null) ?: filesDir
                val file = File(storageDir, fileName)
                
                // Copy file to app storage
                FileOutputStream(file).use { outputStream ->
                    inputStream.copyTo(outputStream)
                }
                inputStream.close()
                
                // Determine MIME type
                val mimeType = when {
                    fileName.endsWith(".pdf", ignoreCase = true) -> "application/pdf"
                    fileName.endsWith(".epub", ignoreCase = true) -> "application/epub+zip"
                    fileName.endsWith(".mobi", ignoreCase = true) -> "application/x-mobipocket-ebook"
                    fileName.endsWith(".cbz", ignoreCase = true) -> "application/vnd.comicbook+zip"
                    fileName.endsWith(".cbr", ignoreCase = true) -> "application/vnd.comicbook-rar"
                    fileName.endsWith(".txt", ignoreCase = true) -> "text/plain"
                    else -> contentResolver.getType(uri) ?: "application/octet-stream"
                }
                
                // Create book entry
                val book = Book(
                    title = fileName.substringBeforeLast('.'),
                    author = "Unknown",
                    filePath = file.absolutePath,
                    fileSize = file.length(),
                    mimeType = mimeType,
                    dateAdded = System.currentTimeMillis()
                )
                
                bookViewModel.insertBook(book)
                
                withContext(Dispatchers.Main) {
                    Toast.makeText(
                        this@MainActivity,
                        "Book added: ${book.title}",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            } catch (e: Exception) {
                e.printStackTrace()
                withContext(Dispatchers.Main) {
                    Toast.makeText(
                        this@MainActivity,
                        "Error adding book: ${e.message}",
                        Toast.LENGTH_LONG
                    ).show()
                }
            }
        }
    }
    
    private fun getFileName(uri: Uri): String? {
        var fileName: String? = null
        contentResolver.query(uri, null, null, null, null)?.use { cursor ->
            if (cursor.moveToFirst()) {
                val nameIndex = cursor.getColumnIndex(android.provider.OpenableColumns.DISPLAY_NAME)
                if (nameIndex >= 0) {
                    fileName = cursor.getString(nameIndex)
                }
            }
        }
        return fileName ?: uri.lastPathSegment
    }
    
    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.main_menu, menu)
        syncMenuItem = menu.findItem(R.id.action_sync)
        
        // Update sync menu visibility based on sync enabled status
        syncMenuItem?.isVisible = prefsManager.isSyncEnabled()
        
        return true
    }
    
    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.action_sync -> {
                handleSyncAction()
                true
            }
            R.id.action_collections -> {
                val intent = Intent(this, CollectionsActivity::class.java)
                startActivity(intent)
                true
            }
            R.id.action_download_network -> {
                val intent = Intent(this, NetworkBookActivity::class.java)
                startActivity(intent)
                true
            }
            R.id.action_settings -> {
                val intent = Intent(this, SettingsActivity::class.java)
                startActivity(intent)
                true
            }
            R.id.action_about -> {
                val intent = Intent(this, AboutActivity::class.java)
                startActivity(intent)
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }
    
    private fun handleSyncAction() {
        if (!prefsManager.isSyncEnabled()) {
            showSyncSnackbar(getString(R.string.sync_disabled))
            return
        }
        
        // Perform full sync
        syncViewModel.fullSync()
    }
    
    private fun showSyncSnackbar(message: String) {
        Snackbar.make(binding.root, message, Snackbar.LENGTH_SHORT).show()
    }
    
    override fun onResume() {
        super.onResume()
        // Update sync menu visibility when returning from settings
        invalidateOptionsMenu()
    }
    
    private fun showAddToCollectionDialog(book: Book) {
        lifecycleScope.launch(Dispatchers.IO) {
            val collections = collectionViewModel.allCollections.value ?: emptyList()
            
            withContext(Dispatchers.Main) {
                if (collections.isEmpty()) {
                    Toast.makeText(
                        this@MainActivity,
                        "No collections yet. Create one first from the Collections menu.",
                        Toast.LENGTH_LONG
                    ).show()
                    return@withContext
                }
                
                val collectionNames = collections.map { it.name }.toTypedArray()
                val checkedItems = BooleanArray(collections.size) { false }
                
                // Check which collections already contain this book
                lifecycleScope.launch(Dispatchers.IO) {
                    collections.forEachIndexed { index, collection ->
                        checkedItems[index] = collectionViewModel.isBookInCollection(book.id, collection.id)
                    }
                    
                    // Store initial state to compare later
                    val initialState = checkedItems.copyOf()
                    
                    withContext(Dispatchers.Main) {
                        AlertDialog.Builder(this@MainActivity)
                            .setTitle(R.string.select_collections)
                            .setMultiChoiceItems(collectionNames, checkedItems) { _, which, isChecked ->
                                checkedItems[which] = isChecked
                            }
                            .setPositiveButton(android.R.string.ok) { _, _ ->
                                // Only add or remove book when the state has actually changed
                                collections.forEachIndexed { index, collection ->
                                    val wasInCollection = initialState[index]
                                    val isNowInCollection = checkedItems[index]
                                    
                                    if (!wasInCollection && isNowInCollection) {
                                        // Book was not in collection, but now should be - add it
                                        collectionViewModel.addBookToCollection(book.id, collection.id)
                                    } else if (wasInCollection && !isNowInCollection) {
                                        // Book was in collection, but now should not be - remove it
                                        collectionViewModel.removeBookFromCollection(book.id, collection.id)
                                    }
                                    // If wasInCollection == isNowInCollection, no change needed
                                }
                                Toast.makeText(
                                    this@MainActivity,
                                    "Collections updated",
                                    Toast.LENGTH_SHORT
                                ).show()
                            }
                            .setNegativeButton(android.R.string.cancel, null)
                            .show()
                    }
                }
            }
        }
    }
}