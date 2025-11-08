package com.rifters.ebookreader

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.rifters.ebookreader.databinding.ItemBookBinding
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.TimeUnit

class BookAdapter(
    private val onBookClick: (Book) -> Unit
) : ListAdapter<Book, BookAdapter.BookViewHolder>(BookDiffCallback()) {
    
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): BookViewHolder {
        val binding = ItemBookBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return BookViewHolder(binding, onBookClick)
    }
    
    override fun onBindViewHolder(holder: BookViewHolder, position: Int) {
        holder.bind(getItem(position))
    }
    
    class BookViewHolder(
        private val binding: ItemBookBinding,
        private val onBookClick: (Book) -> Unit
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
                
                // TODO: Load cover image if available
                // if (book.coverImagePath != null) {
                //     Glide.with(binding.root.context)
                //         .load(book.coverImagePath)
                //         .into(bookCoverImageView)
                // }
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
            return oldItem == newItem
        }
    }
}
