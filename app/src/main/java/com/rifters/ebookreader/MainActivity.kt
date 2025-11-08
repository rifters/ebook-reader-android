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
import com.rifters.ebookreader.databinding.ActivityMainBinding
import com.rifters.ebookreader.viewmodel.BookViewModel
import com.rifters.ebookreader.viewmodel.CollectionViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream

class MainActivity : AppCompatActivity() {
    
    private lateinit var binding: ActivityMainBinding
    private lateinit var bookViewModel: BookViewModel
    private lateinit var collectionViewModel: CollectionViewModel
    private lateinit var bookAdapter: BookAdapter
    
    private val filePickerLauncher = registerForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let { handleSelectedFile(it) }
    }
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        
        setupToolbar()
        setupRecyclerView()
        setupViewModel()
        setupFab()
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
        
        bookViewModel.allBooks.observe(this) { books ->
            bookAdapter.submitList(books)
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
        return true
    }
    
    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
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
                    
                    withContext(Dispatchers.Main) {
                        AlertDialog.Builder(this@MainActivity)
                            .setTitle(R.string.select_collections)
                            .setMultiChoiceItems(collectionNames, checkedItems) { _, which, isChecked ->
                                checkedItems[which] = isChecked
                            }
                            .setPositiveButton(android.R.string.ok) { _, _ ->
                                // Add or remove book from collections based on selection
                                collections.forEachIndexed { index, collection ->
                                    if (checkedItems[index]) {
                                        collectionViewModel.addBookToCollection(book.id, collection.id)
                                    } else {
                                        collectionViewModel.removeBookFromCollection(book.id, collection.id)
                                    }
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