package com.rifters.ebookreader

import android.content.Intent
import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.rifters.ebookreader.databinding.ActivityCollectionBooksBinding
import com.rifters.ebookreader.viewmodel.BookViewModel
import com.rifters.ebookreader.viewmodel.CollectionViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class CollectionBooksActivity : AppCompatActivity() {
    
    private lateinit var binding: ActivityCollectionBooksBinding
    private lateinit var collectionViewModel: CollectionViewModel
    private lateinit var bookViewModel: BookViewModel
    private lateinit var bookAdapter: BookAdapter
    private var collectionId: Long = -1
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityCollectionBooksBinding.inflate(layoutInflater)
        setContentView(binding.root)
        
        collectionId = intent.getLongExtra("collection_id", -1)
        val collectionName = intent.getStringExtra("collection_name") ?: getString(R.string.collections)
        
        setupToolbar(collectionName)
        setupRecyclerView()
        setupViewModel()
        setupFab()
        loadCollectionBooks()
    }
    
    private fun setupToolbar(collectionName: String) {
        setSupportActionBar(binding.toolbar)
        supportActionBar?.title = collectionName
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        binding.toolbar.setNavigationOnClickListener {
            finish()
        }
    }
    
    private fun setupRecyclerView() {
        bookAdapter = BookAdapter(
            onBookClick = { book ->
                openBook(book)
            }
        )
        
        binding.recyclerView.apply {
            adapter = bookAdapter
            layoutManager = LinearLayoutManager(this@CollectionBooksActivity)
        }
    }
    
    private fun setupViewModel() {
        collectionViewModel = ViewModelProvider(this)[CollectionViewModel::class.java]
        bookViewModel = ViewModelProvider(this)[BookViewModel::class.java]
    }
    
    private fun setupFab() {
        binding.fabAddBook.setOnClickListener {
            showAddBookDialog()
        }
    }
    
    private fun showAddBookDialog() {
        if (collectionId == -1L) {
            return
        }
        
        lifecycleScope.launch(Dispatchers.IO) {
            // Get all books
            val allBooks = bookViewModel.getAllBooksSync()
            
            // Get books already in this collection
            val collectionWithBooks = collectionViewModel.getCollectionWithBooks(collectionId)
            val booksInCollection = collectionWithBooks?.books?.map { it.id } ?: emptyList()
            
            // Filter to get books not in collection
            val availableBooks = allBooks.filter { it.id !in booksInCollection }
            
            withContext(Dispatchers.Main) {
                if (availableBooks.isEmpty()) {
                    android.widget.Toast.makeText(
                        this@CollectionBooksActivity,
                        "All books are already in this collection",
                        android.widget.Toast.LENGTH_SHORT
                    ).show()
                    return@withContext
                }
                
                val bookTitles = availableBooks.map { "${it.title} by ${it.author}" }.toTypedArray()
                val checkedItems = BooleanArray(availableBooks.size) { false }
                
                AlertDialog.Builder(this@CollectionBooksActivity)
                    .setTitle(R.string.select_books_to_add)
                    .setMultiChoiceItems(bookTitles, checkedItems) { _, which, isChecked ->
                        checkedItems[which] = isChecked
                    }
                    .setPositiveButton(android.R.string.ok) { _, _ ->
                        // Add selected books to collection
                        lifecycleScope.launch(Dispatchers.IO) {
                            availableBooks.forEachIndexed { index, book ->
                                if (checkedItems[index]) {
                                    collectionViewModel.addBookToCollection(book.id, collectionId)
                                }
                            }
                            
                            withContext(Dispatchers.Main) {
                                android.widget.Toast.makeText(
                                    this@CollectionBooksActivity,
                                    "Books added to collection",
                                    android.widget.Toast.LENGTH_SHORT
                                ).show()
                                loadCollectionBooks()
                            }
                        }
                    }
                    .setNegativeButton(android.R.string.cancel, null)
                    .show()
            }
        }
    }
    
    private fun loadCollectionBooks() {
        if (collectionId == -1L) {
            finish()
            return
        }
        
        lifecycleScope.launch(Dispatchers.IO) {
            val collectionWithBooks = collectionViewModel.getCollectionWithBooks(collectionId)
            
            withContext(Dispatchers.Main) {
                collectionWithBooks?.let {
                    bookAdapter.submitList(it.books)
                    
                    if (it.books.isEmpty()) {
                        binding.emptyTextView.visibility = View.VISIBLE
                        binding.recyclerView.visibility = View.GONE
                    } else {
                        binding.emptyTextView.visibility = View.GONE
                        binding.recyclerView.visibility = View.VISIBLE
                    }
                }
            }
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
    
    override fun onResume() {
        super.onResume()
        // Reload books when returning to this activity
        loadCollectionBooks()
    }
}
