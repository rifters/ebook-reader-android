package com.rifters.ebookreader

import android.graphics.Bitmap
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.rifters.ebookreader.databinding.ItemBookBinding
import com.rifters.ebookreader.util.BookCoverGenerator
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.TimeUnit

class BookAdapter(
    private val onBookClick: (Book) -> Unit,
    private val onBookLongClick: ((Book) -> Unit)? = null,
    private val onBookMenuClick: ((Book) -> Unit)? = null
) : ListAdapter<Book, BookAdapter.BookViewHolder>(BookDiffCallback()) {
    
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): BookViewHolder {
        val binding = ItemBookBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return BookViewHolder(binding, onBookClick, onBookLongClick, onBookMenuClick)
    }
    
    override fun onBindViewHolder(holder: BookViewHolder, position: Int) {
        holder.bind(getItem(position))
    }
    
    class BookViewHolder(
        private val binding: ItemBookBinding,
        private val onBookClick: (Book) -> Unit,
        private val onBookLongClick: ((Book) -> Unit)?,
        private val onBookMenuClick: ((Book) -> Unit)?
    ) : RecyclerView.ViewHolder(binding.root) {
        
        fun bind(book: Book) {
            binding.apply {
                bookTitleTextView.text = book.title
                bookAuthorTextView.text = book.author
                bookProgressBar.progress = book.progressPercentage.toInt()
                
                val progressText = if (book.progressPercentage > 0) {
                    val lastReadText = formatLastRead(book.lastOpened)
                    "${book.progressPercentage.toInt()}% complete • $lastReadText"
                } else {
                    "Not started"
                }
                bookProgressTextView.text = progressText
                
                root.setOnClickListener {
                    onBookClick(book)
                }
                
                root.setOnLongClickListener {
                    onBookLongClick?.invoke(book)
                    true
                }
                
                menuButton.setOnClickListener {
                    onBookMenuClick?.invoke(book)
                }
                
                // Load cover image if available, otherwise generate default
                if (!book.coverImagePath.isNullOrEmpty()) {
                    val coverFile = java.io.File(book.coverImagePath)
                    if (coverFile.exists()) {
                        bookCoverImageView.setImageURI(android.net.Uri.fromFile(coverFile))
                    } else {
                        // Generate default cover with title and author
                        val defaultCover = BookCoverGenerator.generateDefaultCover(
                            root.context,
                            book.title,
                            book.author
                        )
                        bookCoverImageView.setImageBitmap(defaultCover)
                    }
                } else {
                    // Generate default cover with title and author
                    val defaultCover = BookCoverGenerator.generateDefaultCover(
                        root.context,
                        book.title,
                        book.author
                    )
                    bookCoverImageView.setImageBitmap(defaultCover)
                }
            }
        }
        
        private fun formatLastRead(timestamp: Long): String {
            if (timestamp == 0L) {
                return "Never"
            }
            
            val now = System.currentTimeMillis()
            val diff = now - timestamp
            
            return when {
                diff < TimeUnit.MINUTES.toMillis(1) -> "Just now"
                diff < TimeUnit.HOURS.toMillis(1) -> {
                    val minutes = TimeUnit.MILLISECONDS.toMinutes(diff)
                    "$minutes minute${if (minutes > 1) "s" else ""} ago"
                }
                diff < TimeUnit.DAYS.toMillis(1) -> {
                    val hours = TimeUnit.MILLISECONDS.toHours(diff)
                    "$hours hour${if (hours > 1) "s" else ""} ago"
                }
                diff < TimeUnit.DAYS.toMillis(7) -> {
                    val days = TimeUnit.MILLISECONDS.toDays(diff)
                    "$days day${if (days > 1) "s" else ""} ago"
                }
                else -> {
                    val dateFormat = SimpleDateFormat("MMM dd, yyyy", Locale.getDefault())
                    "Last read: ${dateFormat.format(Date(timestamp))}"
                }
            }
        }
    }
    
    class BookDiffCallback : DiffUtil.ItemCallback<Book>() {
        override fun areItemsTheSame(oldItem: Book, newItem: Book): Boolean {
            return oldItem.id == newItem.id
        }
        
        override fun areContentsTheSame(oldItem: Book, newItem: Book): Boolean {
            // Explicitly compare all fields that affect the UI display
            // This ensures content changes are properly detected and the UI updates accordingly
            return oldItem.title == newItem.title &&
                    oldItem.author == newItem.author &&
                    oldItem.filePath == newItem.filePath &&
                    oldItem.fileSize == newItem.fileSize &&
                    oldItem.mimeType == newItem.mimeType &&
                    oldItem.dateAdded == newItem.dateAdded &&
                    oldItem.lastOpened == newItem.lastOpened &&
                    oldItem.coverImagePath == newItem.coverImagePath &&
                    oldItem.totalPages == newItem.totalPages &&
                    oldItem.currentPage == newItem.currentPage &&
                    oldItem.isCompleted == newItem.isCompleted &&
                    oldItem.progressPercentage == newItem.progressPercentage
        }
    }
}
